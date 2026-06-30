# DodgeGame (Dotch.) — FPS / Performance Audit

**Date:** 2026-06-30
**Scope:** Full back-end audit of why frame-rate drops (to ~30 FPS on boss levels) and concrete, low-risk fixes.
**Method:** Multi-agent code audit across every performance-relevant subsystem, then findings hand-verified against the actual source (`Game.java`, `Handler.java`, `Window.java`, `TrailPool.java`, `Trail.java`, `EnemyBossBullet.java`, `GamePalette.java`, `PageRenderer.java`, `PlayerProjectile.java`, `HUD.java`, `BossSwarm.java`, `AudioPlayer.java`, the IntelliJ run config, and the `.iml`/module files). Every claim below was checked at the cited `file:line`.

> Note on confidence: each finding is tagged **[verified]** (confirmed by reading the exact code) or **[estimated]** (mechanism confirmed, magnitude is a reasoned estimate that depends on live object counts). Where an automated estimate could not be hand-confirmed it is marked as such.

---

## TL;DR — Why a "simple" game runs slower than GTA 5

GTA 5 renders on the **GPU**. DodgeGame renders **100% on the CPU**, one pixel at a time, in software:

- The whole game is drawn with **Java2D / AWT** (`Canvas` + `BufferStrategy` + `Graphics2D`). There is **no OpenGL/Direct3D scene rendering** — LWJGL in this project is only used for OpenAL audio.
- Every frame the renderer calls `g.scale(scale, scale)` (`Game.java:806`) so the logical **1280×720** scene is drawn at your monitor's **native resolution** (e.g. 1080p/1440p). Every shape is therefore rasterized at full native pixel count by the CPU.
- On **boss levels** the scene fills with **dozens of translucent bullets + hundreds of translucent trail particles**, each of which is an **alpha-blended fill** (read-modify-write per pixel) rasterized at native resolution — and the loop renders this up to **~240 times per second** even though the simulation only updates 60 times per second.

None of this is a GPU problem — it's a **CPU pixel-fill + allocation problem**, and it's fixable without changing how the game looks or plays.

The boss-level drop specifically comes from **three compounding things**:
1. **Trail particles explode in count** during bullet-hell (each boss bullet emits a trail every 3 ticks → ~11 live trails per bullet → up to the 512 pool cap), and each live trail is an alpha-blended fill **every rendered frame**. → *Finding P1-A*
2. **No hardware acceleration is configured**, and the scene is upscaled per-primitive instead of once. → *Findings P0-A, P0-B*
3. **The loop renders ~4× more frames than the simulation needs**, multiplying all of the above and pinning a CPU core. → *Finding P1-D*

---

## Priority summary

| # | Finding | File:Line | Impact | Fix effort | Risk |
|---|---------|-----------|--------|------------|------|
| **P0-A** | No Java2D acceleration pipeline enabled (run config has zero accel flags) | `.idea/workspace.xml:52` | High | Trivial | Low |
| **P0-B** | Whole scene scaled per-primitive instead of rendered once and blitted (root cause) | `Game.java:806` | **Highest** | Medium | Low–Med |
| **P1-A** | Boss bullets saturate the 512-trail pool; every trail is an alpha-blended fill + `new Color` every frame | `EnemyBossBullet.java:52`, `TrailPool.java:86,92` | **High** | Small | Low |
| **P1-B** | Cached backgrounds are `TYPE_INT_ARGB` and bilinear-upscaled every frame (not display-compatible) | `PageRenderer.java:74`, `Game.java:1306` | Med–High | Small | Low |
| **P1-C** | Heavy full-screen overdraw every frame (clear + bg + tint + geo + walls + beat) | `Game.java:803,818,1213,1306` | Medium | Small | Low |
| **P1-D** | Loop renders uncapped toward 240 FPS, busy-waits with `Thread.yield()`, pins a core, 4× redundant work | `Game.java:430-435` | Med–High | Trivial | Low |
| **P2-A** | `GamePalette.accent()/accent(int)` allocate a `Color` (+ HSB math in colorblind mode) on every call, many/frame | `GamePalette.java:97,102` | Low–Med | Small | None |
| **P2-B** | Uncached grid-dot nested loop draws 600–1000+ fills every frame | `Game.java:1232` | Low–Med | Small | Low |
| **P2-C** | HUD rebuilds a `GradientPaint`+`Color`s and fills a full-width gradient band every frame | `HUD.java:152` | Low–Med | Trivial | None |
| **P2-D** | HUD draws a 600 pt anti-aliased glyph watermark every frame | `HUD.java:300` | Low–Med | Small | Low |
| **P2-E** | `AudioPlayer.getBeatPulse()` does 2 JNI queries + `Math.pow` per call, called 2×/frame | `AudioPlayer.java:202`, `Game.java:1206,1332` | Low | Trivial | None |
| **P2-F** | Boss render does full O(n) list scans 2×/frame to count minions | `BossSwarm.java:241,282` (pattern also in `BossSplitter`) | Low | Trivial | None |
| **P3-A** | `BossLaser` leaks immortal zero-velocity bullets while player is in a beam | `BossLaser.java:249` | Low–Med | Small | Low–Med |
| **P3-B** | Per-frame constant `Color`/`Font` re-allocation across projectiles, enemies, HUD toast | several | Low | Trivial | None |

---

## Recommended order of work (safest first)

**Phase 1 — zero/▲low-risk quick wins (do these first, ~1–2 hrs, no behavior change):**
1. P0-A — add Java2D acceleration VM flags and **measure**.
2. P1-D — cap the render loop to ~60 FPS and remove the busy-wait.
3. P1-A (the safe half) — cache trail colors in `TrailPool.render`.
4. P2-C, P2-E, P2-F, P2-A, P3-B — eliminate per-frame allocations / redundant scans.

**Phase 2 — moderate, still safe:**
5. P1-B — make the cached backgrounds display-compatible and pre-scaled.
6. P1-A (the bigger half) — load-aware trail throttle.
7. P2-B, P2-D — cache grid dots / watermark.

**Phase 3 — the architectural fix that addresses the root cause:**
8. P0-B — render the scene once at logical resolution into an accelerated back-buffer, then do a single hardware-scaled blit.

Each phase is independently shippable and testable with the in-game FPS counter (`Settings → Show FPS`, drawn at `Game.java:1084`).

---

## Detailed findings

### P0-A — No Java2D acceleration pipeline is enabled  **[verified]**

The IntelliJ run config sets only:
```
VM_PARAMETERS = -Djava.library.path=$PROJECT_DIR$/libs/lwjgl/native/windows --enable-native-access=ALL-UNNAMED
```
(`.idea/workspace.xml:52`; the `README.md` launch command has no flags either). There is **no** `sun.java2d.*` configuration anywhere in the project. On Windows the default pipeline is Direct3D, but this workload (per-primitive `AffineTransform` scaling + `AlphaComposite` translucency + anti-aliasing + `TYPE_INT_ARGB` images) is exactly the kind that silently falls back to software.

**Fix (try, then measure — this is data, not dogma):**
```
-Dsun.java2d.opengl=true
-Dsun.java2d.translaccel=true
-Dsun.java2d.ddscale=true
```
Set them in the run config `VM_PARAMETERS` (keep the existing two flags) and in the `README` launch line. The OpenGL pipeline accelerates translucent fills and scaled image blits — the two things this game does most. **Test on the target machine**: some GPU drivers render the OGL pipeline poorly, so treat it as A/B measurement, not a guaranteed win. (`-Dsun.java2d.d3d=true` is already the Windows default; only force it if OGL regresses.)

**Why low-risk:** flags only; pure rollback by removing them. No code change.

---

### P0-B — The whole scene is scaled per-primitive instead of rendered once  **[verified — root cause]**

```java
// Game.java:806-807
g.scale(scale, scale);
g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
```
Everything afterwards is drawn in logical 1280×720 coordinates but **rasterized at native device resolution** because of the global scale. At 1440p (`scale ≈ 2.0`) a 16 px bullet is rasterized as a ~32 px oval — **~4× the pixels** — and that multiplier hits *every* bullet, trail, grid dot, wall, and HUD element, on the CPU, every frame.

> Technical clarification (important for credibility): `KEY_INTERPOLATION_BILINEAR` only affects **`drawImage`** (the two cached backgrounds + `geoCache`). It does **not** resample `fillOval`/`fillRect`/`fillPolygon` — those rasterize directly at the scaled size. So the per-shape cost is "more device pixels to fill," and the bilinear cost is specifically on the full-screen image blits (see P1-B). Earlier "bilinear on every primitive" framing overstated the mechanism; the *fill-area* explosion is the real per-shape cost.

**Fix (the high-leverage one):** render the scene to an off-screen buffer at **logical** resolution, then scale that single image to the screen once:

```java
// Create once (recreate on resolution change). VolatileImage lives in VRAM and
// its scaled blit is hardware-accelerated.
private VolatileImage backBuffer; // 1280x720, from getGraphicsConfiguration().createCompatibleVolatileImage(...)

private void render() {
    BufferStrategy bs = getBufferStrategy();
    if (bs == null) { createBufferStrategy(3); return; }

    // 1) Draw the ENTIRE game into backBuffer at 1280x720 — NO g.scale here.
    Graphics2D gb = backBuffer.createGraphics();
    //   ... all existing world/HUD drawing, in logical coords, unscaled ...
    gb.dispose();

    // 2) One scaled blit to the screen (bilinear), hardware-accelerated.
    Graphics2D g = (Graphics2D) bs.getDrawGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(backBuffer, 0, 0, getWidth(), getHeight(), null);
    g.dispose();
    bs.show();
}
```

This converts *"N shapes each rasterized at native resolution"* into *"N shapes at 1280×720 (≈`scale²` fewer pixels) + one accelerated image upscale."* On a 1440p display that is roughly a **4× reduction in pixel-fill work** for the entire scene, and the upscale runs on the GPU. It is the single change most likely to lift the boss-level floor back above 60.

**Why low–medium risk:** the drawing code is unchanged — only *where* it draws (into `backBuffer`) and the final blit change. The screen-space effects in `render()` that currently sit *outside* the camera transform (damage vignette, slow-mo wash, transition fade, FPS text) must be drawn into the back-buffer too, in logical coordinates — they already use `WIDTH/HEIGHT`, so they port directly. Validate the FPS counter and HUD still land correctly after the move.

---

### P1-A — Boss bullets saturate the trail pool; each trail is an alloc + alpha fill every frame  **[verified mechanism / estimated magnitude]**

Each boss bullet emits a trail **every 3rd tick**:
```java
// EnemyBossBullet.java:52-53
if (++trailTick % 3 == 0)
    TrailPool.add(x, y, tc, SIZE, SIZE, 0.03f, TrailPool.SHAPE_CIRCLE);
```
A trail with `life = 0.03` survives ~33 ticks (`TrailPool.tick` subtracts ~0.0299/tick, `TrailPool.java:72-83`), so **each bullet keeps ~11 trails alive at once** (33 ÷ 3). At *K* simultaneous boss bullets that is **~11·K live trails**; the pool caps at `MAX_TRAILS = 512` (`TrailPool.java:11`), reached around **K ≈ 46 bullets**. A phase-3 boss realistically puts **~20–40 bullets** on screen *(estimated from the spread/ring/wave patterns)*, i.e. **~220–440 live trails** — and in combat mode `PlayerProjectile` (`PlayerProjectile.java:39`), dash trails, and `SwarmMinion` feed the *same* 512 pool, so saturation is reachable.

Then, **every rendered frame**, `TrailPool.render` walks the pool and for each live slot:
```java
// TrailPool.java:91-92
g.setComposite(ALPHA_CACHE[alphaIdx]);
g.setColor(new Color((int) t[R], (int) t[G], (int) t[B]));  // allocation per live trail per frame
```
followed by an alpha-blended `fillOval`/`fillRoundRect`/`fillPolygon`. So at ~300 live trails and the uncapped ~240 FPS render rate that is **~300 alpha-blended native-resolution fills and ~300 `Color` allocations per frame** — the dominant boss-level render cost, several times larger than the bullets' own fills.

**Fix — split into a free win + a tunable:**

*Free, pixel-identical (do unconditionally):* stop allocating a `Color` per trail per frame. Memoize by packed RGB:
```java
// TrailPool.java — fields
private static final java.util.HashMap<Integer, Color> COLOR_CACHE = new java.util.HashMap<>();
private static Color opaque(int r, int g, int b) {
    int key = (r << 16) | (g << 8) | b;
    Color c = COLOR_CACHE.get(key);
    if (c == null) { c = new Color(r, g, b); COLOR_CACHE.put(key, c); }
    return c;
}
// in render(), replace the `new Color(...)` with:
g.setColor(opaque((int) t[R], (int) t[G], (int) t[B]));
```
Trails use a tiny set of distinct RGBs, so this removes essentially all per-frame trail allocations with byte-identical output. (Cache *opaque* RGB only — the render path deliberately supplies alpha via `setComposite`, so caching a source color that carries its own alpha would double-blend.)

*The bigger win — reduce live-trail count (load-aware, preserves normal-load visuals):* add a live-trail counter to `TrailPool` and gate emission only when already saturated:
```java
// EnemyBossBullet.java:52 — only throttle under load, so normal fights look identical
if (++trailTick % 3 == 0 && TrailPool.liveCount() < 360)
    TrailPool.add(...);
```
This caps the worst case without touching the common case.

> ⚠️ Do **not** blindly change the modulus from `3` to `6` to halve trails. For fast bullets (`v ≈ 5 px/tick`) the 16 px trail dots currently overlap into a continuous streak at 3-tick spacing; at 6-tick spacing they leave ~14 px gaps and the trail visibly **dashes**. If you want a uniform reduction, use modulus `4` *and* raise `life` ~`0.03 → 0.045` so fast streaks stay continuous, and eyeball it on the ring/wave patterns.

**Why low-risk:** trails are purely cosmetic — `TrailPool` never affects collision or game state, so none of this changes gameplay. The color-cache half is visually identical; the throttle only degrades visuals *under load that is already dropping frames*.

---

### P1-B — Cached backgrounds are `TYPE_INT_ARGB` and bilinear-upscaled every frame  **[verified]**

`PageRenderer` correctly caches the game background as a static image (good — it is **not** regenerated per frame):
```java
// PageRenderer.java:74
BufferedImage img = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_ARGB);
```
But it is created with `new BufferedImage(TYPE_INT_ARGB)` rather than `GraphicsConfiguration.createCompatibleImage(...)`, so it is not in the screen-native format and is harder for the JVM to keep VRAM-managed. It is then blitted **through the global `g.scale`** every frame (`Game.java:818`), i.e. a full-screen **bilinear upscale to native resolution every frame**. The `geoCache` image (`Game.java:1306`) is the same, and is additionally *re-rendered into* every 4 frames (`Game.java:1253`), which un-manages it and forces a VRAM re-upload.

**Fix:**
- Build the background caches with `getGraphicsConfiguration().createCompatibleImage(WIDTH, HEIGHT, Transparency.TRANSLUCENT)` (or `createCompatibleVolatileImage`).
- Better, once P0-B lands: pre-render the background at **native** resolution and blit it **1:1** (no per-frame scale at all).
- For `geoCache`, consider rendering it at half resolution (it is slow-moving) or refreshing it less often than every 4 frames.

**Why low-risk:** same pixels, just a friendlier image format / blit path.

---

### P1-C — Heavy full-screen overdraw every frame  **[verified]**

In a single Game frame the renderer paints the full screen many times over:
1. `g.fillRect(0,0,getWidth(),getHeight())` opaque black clear at native res — `Game.java:803`
2. `PageRenderer.drawGameBackground` full-screen image blit — `Game.java:818`
3. `renderBeatVisuals` full-screen alpha tint `fillRect(0,0,WIDTH,HEIGHT)` — `Game.java:1213`
4. `renderGeoLayers` full-screen `geoCache` blit — `Game.java:1306`
5. neon walls + beat vignette + grid dots on top

That is **4–5× full-screen coverage per frame at native resolution** before a single bullet is drawn.

**Fixes:**
- The opaque black clear (step 1) is redundant when an **opaque** background blit (step 2) covers the whole screen the very next call — drop it, or make the background opaque and skip the clear.
- Fold the static background + the slow `geoCache` into a single composited layer so it is one blit, not two.
- After P0-B these full-screen fills happen at logical 1280×720 instead of native res — another ~`scale²` saving for free.

**Why low-risk:** these are background layers; removing a redundant clear under an opaque layer is visually identical.

---

### P1-D — The loop renders ~4× more often than it simulates, and busy-waits  **[verified]**

```java
// Game.java:419-435
while (delta >= 1) { tick(); delta--; }      // simulation: 60 Hz
if (running) render();                        // render: every loop iteration
...
long renderNs = 1000000000L / 240;            // target ~240 FPS
... Thread.sleep(...) ...
while (System.nanoTime() - now < renderNs) { Thread.yield(); }  // busy-wait spin
```
The simulation advances at 60 Hz, but the loop **renders up to ~240 FPS** — so on a healthy machine it draws **~4 object-identical frames per simulation tick**, doing 4× the pixel-fill and 4× the per-frame `Color` allocation for no visual benefit, and the trailing `Thread.yield()` spin **pins a CPU core at 100%** (which can also starve the audio thread and add scheduling jitter). When a boss frame is already slow, this wasted headroom is exactly what you don't have.

**Fix:** cap the render rate at the display refresh (≈60) and sleep instead of spin:
```java
long renderNs = 1000000000L / 60;             // was /240
long sleepNanos = renderNs - (System.nanoTime() - now);
if (sleepNanos > 0) {
    try { Thread.sleep(sleepNanos / 1000000, (int)(sleepNanos % 1000000)); }
    catch (InterruptedException ignored) {}
}
```
(Optionally render only when `delta` actually advanced.) This cuts common-case render work ~4× and frees the pinned core.

**Why low-risk:** the simulation is already decoupled at a fixed 60 Hz, so capping the *render* rate to 60 does not change game speed or feel — it just stops drawing redundant duplicate frames. The visible FPS number will read ~60 instead of ~240, which is the correct target for a 60 Hz simulation.

---

### P2-A — `GamePalette.accent()` allocates on every call  **[verified]**

```java
// GamePalette.java:97-107
public static Color accent() { return new Color(clamp(currentR), ...); }            // alloc every call
public static Color accent(int alpha) { return new Color(..., alpha); }              // alloc every call
// + in colorblind mode: Color.RGBtoHSB(...) + Color.getHSBColor(...) per call
```
`currentR/G/B` only change once per tick (`GamePalette.update`, called once per 60 Hz tick at `Game.java:594`), yet `accent()` is rebuilt at every call site — per projectile, per HUD element, neon walls, level-up borders, watermark — many times per frame, and in colorblind mode each call also runs full HSB conversion math.

**Fix:** compute the accent once per frame and cache it; have `accent()` return the cached instance and `accent(int)` derive from it. Refresh the cache in `update()` (which already runs every tick) so the animation/colorblind remap still updates. Pixel-identical, removes thousands of allocations/sec.

**Why no risk:** `Color` is immutable; same RGBA out, just not re-allocated.

---

### P2-B — Uncached grid-dot nested loop every frame  **[verified]**

```java
// Game.java:1232-1236  (inside renderBeatVisuals, runs every frame)
for (int x = 20; x < WIDTH; x += gridSpacing)
    for (int y = 20; y < HEIGHT; y += gridSpacing)
        g.fillRect(x - beatDotSize/2, y - beatDotSize/2, beatDotSize, beatDotSize);
```
At the densest difficulty (`gridSpacing = 30`) that is roughly `(1280/30)·(720/30) ≈ 1000` `fillRect` calls every frame — densest exactly on the hardest difficulty, where the scene is already heaviest.

**Fix:** the dots are static between beats; render them once into a cached image (like `geoCache`) and modulate only the alpha/size on the beat, or pre-render a 1-cell tile and `TexturePaint` it. After P1-D this also runs 4× less often.

**Why low-risk:** background decoration; cached version is visually equivalent.

---

### P2-C — HUD rebuilds a gradient + colors and fills a full-width band every frame  **[verified]**

```java
// HUD.java:152-153  (every frame)
g2.setPaint(new GradientPaint(0, 0, new Color(10,12,18,160), 0, 90, new Color(10,12,18,0)));
g2.fillRect(0, 0, Game.WIDTH, 90);
```
A new `GradientPaint` + 2 `Color` objects allocated and a full-width **gradient** fill (more expensive than a solid fill) every frame, plus the whole HUD runs with general + text anti-aliasing on (`HUD.java:148-149`).

**Fix:** hoist the two colors and the `GradientPaint` to `static final` (the geometry is fixed). Pixel-identical.

---

### P2-D — 600 pt anti-aliased glyph watermark every frame  **[verified]**

```java
// HUD.java:300-306  (every frame)
g2.setFont(FONT_LEVEL_BG);                       // Arial BOLD 600
g2.setColor(GamePalette.accent(Math.min(watermarkAlpha, 255)));
g2.drawString(levelStr, ..., Game.HEIGHT - 80);  // big AA glyph fill, native-res
```
Rasterizing a 600 pt anti-aliased glyph (then scaled to native resolution by the global transform) is a sizable fill every frame, and the alpha only changes on level-up.

**Fix:** render the watermark glyph once per level into a small cached image and blit it with the current alpha, or only redraw when `levelStr`/alpha changes. Combine with the P2-A accent cache.

**Why low-risk:** identical appearance; just cached.

---

### P2-E — `getBeatPulse()` recomputed twice per frame  **[verified]**

```java
// AudioPlayer.java:202-209
public static float getBeatPulse() {
    if (... || !isPlaying()) return 0;        // JNI/OpenAL query
    float pos = getPosition();                 // JNI/OpenAL query
    ...
    return (float) Math.pow(1.0 - beatPhase, 3.5);
}
```
Called in **both** `renderBeatVisuals` (`Game.java:1206`) and `renderNeonWalls` (`Game.java:1332`) — so **2× per frame** = 2 `isPlaying()` + 2 `getPosition()` native calls + 2 `Math.pow` for one value that is identical within a frame.

**Fix:** compute `beat` once at the top of `render()` and pass it down. Trivial, removes redundant JNI round-trips.

---

### P2-F — Boss render scans the whole object list twice per frame to count minions  **[verified]**

```java
// BossSwarm.java:182-188  — full O(n) list scan
private int countMinions() { for (... handler.getObjects() ...) if (obj instanceof SwarmMinion) count++; }
// called at BossSwarm.java:241 (render) AND :282 (renderBossBar) — 2× per render frame, plus once in tick()
```
At the uncapped ~240 FPS render rate that is ~480 full-list scans/sec just to count minions, over a list that also holds the minions and their bullets. (The `BossSplitter` boss-bar reportedly shares this 2-scans-per-render pattern at `BossSplitter.java:277` — *not hand-verified, but the same fix applies if confirmed*.)

**Fix:** count once per frame (or maintain a running counter as minions are added/removed) and reuse the value in `render`, `renderBossBar`, and `tick`.

**Why no risk:** same number, computed once.

---

### P3-A — `BossLaser` leaks immortal bullets while the player is in a beam  **[verified]**

```java
// BossLaser.java:249 — runs every tick the player overlaps a beam, no rate limit
handler.addObject(new EnemyBossBullet((int)px-4, (int)py-4, ID.BasicEnemy, handler, 0, 0));
```
These have velocity `(0,0)`, so `EnemyBossBullet.tick`'s off-screen cleanup (`line 46`) never fires and `Player.collision` does not remove them either — they **accumulate for the whole laser fight** (only cleared between levels). Because the spawn ignores the player's 45-tick invulnerability window, a single sustained beam hit can leak dozens. Each leaked bullet then renders 2 ovals/frame and feeds a trail every 3 ticks (compounding P1-A); a *parried* leaked bullet additionally runs a full O(n) boss scan every tick forever. (This is a slow leak contingent on the player taking beam hits, hence P3 — but it directly worsens the worst-case laser-boss frame.)

**Fix — give the beam-hit marker a lifespan, without changing the real spray bullets:**
```java
// EnemyBossBullet.java
private int lifespan = -1;                       // -1 = infinite (default, used by sprays)
public void setLifespan(int t) { lifespan = t; }
// at the top of tick(), after the move + off-screen check:
if (lifespan > 0 && --lifespan == 0) { handler.removeObject(this); return; }

// BossLaser.java:249
EnemyBossBullet hit = new EnemyBossBullet((int)px-4, (int)py-4, ID.BasicEnemy, handler, 0, 0);
hit.setLifespan(3);                              // survives >=1 player collision pass, then cleaned up
handler.addObject(hit);
```
`lifespan = 3` (not 1) is required because the player ticks **before** `BossLaser` (`Game.java:598` vs `:612`), so the marker must live into the next frame to register the hit.

**Why low–medium risk:** the hit cadence is governed by the player's 45-tick i-frames, not by spawn rate, so removing the surplus markers changes **no** damage outcome. Keep the `lifespan` default at `-1` so the legitimate self-cleaning spray bullets are untouched — only the line-249 spawn sets a short lifespan. (Avoid the "call a direct damage path instead" refactor: `Player.collision` encodes shield consumption, i-frames, screen shake, perk multipliers, damage tracking, and streak reset — reimplementing that elsewhere is the риск, not the leak.)

---

### P3-B — Scattered per-frame constant `Color`/`Font` re-allocation  **[verified, minor]**

Constants re-allocated every frame instead of hoisted:
- `PlayerProjectile.render` allocates `new Color(230,234,240)` (a constant) per projectile per frame — `PlayerProjectile.java:93`.
- `Player.render` allocates a `Color` inside the aura loop from loop-invariant inputs — `Player.java:583-584`.
- `HUD.renderToast` does `new Font("Arial", BOLD, 18)` every frame while a toast is visible — `HUD.java:412`.

**Fix:** hoist each to a `static final` constant (or above the loop). Individually negligible, collectively free GC relief; pixel-identical.

> These are micro-optimizations. They are listed for completeness but will **not** by themselves move boss FPS — prioritize P0/P1.

---

## What was investigated and found NOT worth changing

To keep the signal clean, these were checked and **rejected** as non-issues for boss FPS:

- **`BossLaser.renderBeams` stroke/color allocations** (`BossLaser.java:328+`): ~20 tiny objects/frame for a *single* boss object — negligible; the real cost there is rasterizing thick AA lines, which hoisting strokes would not change.
- **`FastEnemy`/`HardEnemy` vertex `int[]` allocations & trig per frame**: these enemy types **do not exist during boss fights** — `triggerBossSpawn` calls `handler.clearEnemys()` and `Spawn.tick()` early-returns while a boss is active, so they contribute **zero** cost on the levels that actually drop frames.
- **`FastEnemy.tick` sqrt / `HardEnemy.tick` atan2 redundancy**: real redundancy but on the 60 Hz tick path with single-digit instance counts — immeasurable.
- **`Player.render` lerp/aura allocations**: a single object, O(1) per frame — noise next to the bullet/trail swarm.
- **`PlayerProjectile.checkCollision` O(n) per projectile per tick**: bounded by `maxAmmo = 5`; the scan is cheap (early `continue` on non-enemies) and on the 60 Hz path.

---

## How to measure your progress

1. Turn on the built-in FPS counter: **Settings → Show FPS** (rendered at `Game.java:1084`).
2. Reproduce the worst case: reach a **boss level** (ideally in **combat mode**, which adds player-projectile trails to the same pool) and watch the number.
3. Apply **one phase at a time** and re-test the same boss, so you can attribute the gain.
4. Expected ordering of impact: **P0-B** (render-once-then-scale) and **P1-A** (trail load) should produce the largest boss-level gains; **P0-A** and **P1-D** are near-free multipliers; the P2 items remove steady per-frame overhead.

---

## Verification & caveats

- Findings **P1-A**, **P3-A**, and the `PlayerProjectile` allocation were run through an independent adversarial verification pass (which is why their severities here are calibrated down from the finders' first pass — e.g. P1-A "critical→high" once the 240 FPS vs 60 Hz distinction is accounted for).
- The remaining findings were **hand-verified against the cited `file:line`** while writing this report. Two items are explicitly marked where magnitude is estimated (boss bullet count *K*) or where a sibling file was not opened (`BossSplitter` double-scan).
- A broader automated verification sweep was cut short by **transient server-side rate limiting** (not a code or usage issue); the affected findings were therefore re-verified manually rather than left unconfirmed.
- The headline technical correction worth remembering: the boss-level cost is **CPU pixel-fill of many translucent shapes at native resolution, multiplied by a 4× render-over-tick ratio, with no GPU acceleration** — not "bilinear resampling of every primitive." The fixes above target that real mechanism.
