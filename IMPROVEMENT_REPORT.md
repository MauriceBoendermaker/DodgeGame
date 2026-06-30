# Dotch (DodgeGame) — Full Review: Bugs & Enhancements

**Scope:** Improve/enhance *existing* features and fix bugs across the whole app — gameplay mechanisms, engine, persistence, audio, and the front-end. No new features proposed.
**Date:** 2026-06-30 · **Build:** v4.1 · **Codebase:** ~12.3k LOC, 40 Java source files (`src/`), AWT/Swing + Slick2D/LWJGL.

**Method:** Every source file was read in full and audited across 14 dimensions (9 file-clusters + 5 cross-cutting: concurrency, performance/resources, persistence-robustness, front-end UX, balance). Each correctness claim was independently re-verified against the source. Findings below are de-duplicated and consolidated; line numbers are approximate anchors.

**Verdict in one line:** The game is feature-rich and the rendering/feel work is genuinely good, but there is **one latent crash/freeze race**, **several difficulty/economy logic bugs that make mode selection and the headline "Combat" mode behave incorrectly**, a cluster of **save-robustness gaps that can corrupt or even brick a profile**, and a layer of **per-frame allocation** that quietly defeats the engine's own "zero-GC" design. None are hard to fix.

---

## Severity legend

| Tag | Meaning |
|-----|---------|
| 🔴 **P1** | Crash, freeze, data loss, or a bug that breaks a core system's intent |
| 🟠 **P2** | Clear bug / balance fault that players will hit and notice |
| 🟡 **P3** | Polish, minor bug, perf, UX inconsistency |
| ⚪ **Cleanup** | Dead code / nitpick — no runtime effect, reduces risk of future mis-edits |

---

## 🔴 Priority 1 — Fix first

### 1. Game-object list is shared between the EDT and the game thread with no synchronization → intermittent crash/freeze
`Handler.java:8`, `KeyInput.java:101`, `Menu.java:434/459/513`, `Game.java:787`

`Handler.objects` is a plain `ArrayList`. The game thread iterates it by index in `Handler.tick()`/`render()` while Swing listeners on the **EDT** structurally mutate the same list: `Menu.startGame()/startDailyGame()/resetForMenu()` call `getObjects().clear()` + `addObject(...)`, and `KeyInput.findPlayer()` iterates it on every key event. If a `clear()` shrinks the list between the loop's `size()` check and `get(i)`, the game thread throws `IndexOutOfBoundsException`. **`run()` has no try/catch around `tick()` (`Game.java:420`), so the exception kills the game thread and freezes the whole game.** Most reachable on the End screen (15 background particles ticking while you click **Retry/Main Menu**) and at run start/restart.

**Fix:** Serialize all list access. Cleanest: route start/restart/reset and input through a command queue drained at the top of `tick()` on the game thread; or guard every add/remove/iterate with one lock. At minimum, cache the `Player` reference instead of scanning the list from the EDT, and wrap `tick()`/`render()` in `run()` so a stray exception can't silently kill the loop.

> Related concurrency issues (same root cause, lower impact) are consolidated in [§ Concurrency](#concurrency--threading) below — `gameState`, player input booleans, `HUD.HEALTH`, `mouseShootHeld`, and `AudioPlayer` state are all non-volatile cross-thread fields. The **death-frame TOCTOU** (#23) can actually corrupt the saved profile, so it's P2.

### 2. The classic boss stops attacking mid-fight (attack-pattern soft-lock)
`EnemyBoss.java:220`

Pattern 3 (ring burst) is phase-2-only; in phase 1 it sets `attackCooldown = 10` intending to "skip to the next pattern." But the pattern-advance gate is `if (attackCooldown > 15)`, and `10` fails it, so `attackPattern` is **never incremented**. The boss re-enters case 3 every 10 ticks, **firing nothing**, until HP finally drains into phase 2. That's a ~4 s dead silence in normal mode and **tens of seconds in Combat mode** (drain ×0.35). The very first boss every player meets visibly stops fighting.

**Fix:** In the phase-1 branch set `attackCooldown` to a value `> 15` (e.g. 18), or explicitly `attackPattern = (attackPattern + 1) % 4;`.

### 3. Difficulty selection is inverted/dead for ~30 levels
`Spawn.java:491` (endless), `Spawn.java:433` (Hard), `Spawn.java:445` (Insane)

Endless escalation uses `wave = level − scriptEnd`, and the per-difficulty `scriptEnd` values differ (`Normal 25, Insane 20, Hard 15`). Because Insane's `scriptEnd` is *higher* than Hard's, at the same level Insane's wave count is 5 lower; solving the intensity equations, **Hard's spawn speed and double-spawn chance exceed Insane's from level ~16 to ~45.** Worse:

- **Insane spawns *nothing* at levels 16–19** (`spawnInsaneDifficulty` has no cases there and endless hasn't started).
- **Hard has a dead zone at levels 7–15** (`spawnHardDifficulty` only covers 2–6; endless starts at 16; enemies never despawn), so "Hard" sits flat — at times *lighter than Normal*.
- **A quarter of endless spawns (SmartEnemy) never speed-scale** (`Spawn.java:551` uses `spawnTelegraph`, not `spawnScaled`, and `SmartEnemy.tick` hard-codes ~2 px/tick), so they fall further behind the difficulty ramp every wave.

Net effect: the headline difficulty choice doesn't mean what it says for a large mid-game stretch.

**Fix:** Drive endless intensity off the absolute `level` (or a per-difficulty offset folded into `diffMult`) so a higher difficulty is never mechanically lighter; fill the empty scripted Insane levels (16–19) and Hard levels (7–15), or lower their `SCRIPT_END_*` so endless takes over right after the intro; give `SmartEnemy` a `setTrackSpeed(2 * speedMult)` so it ramps with the others.

### 4. "Combat" mode bookkeeping is inconsistent — over-rewarded, mis-attributed, and never records a score
`Profile.java:74/121/166/295`, `Menu.java:454/1983/2941`

Combat runs use `game.diff == 3`, which has **no case** in `calculateRunXp`/`calculateRunCoins`, so it falls through to the **Insane tier** (2.0× XP, 1.6× coins). Combat also awards large per-kill bonuses (50–125) that inflate score, which feeds those multipliers — making Combat **the most efficient XP/coin farm in the game**. Simultaneously:

- `Profile.startRun(combatMode ? 1 : difficulty)` logs Combat as a **Hard** attempt.
- `submitScore`/`endRun` per-difficulty bests both guard `difficulty < 3`, so Combat **never records a high score, level, or best time** and `lastIsHighScore` is always false.
- The End screen reads `Profile.getHighScore(3)` → clamped to **"Best score: 0"** for every Combat run (`Menu.java:2941`).
- The Loadout screen labels a Combat run **"Insane" in red** (`Menu.java:1983`).

**Fix:** Give `diff == 3` its own explicit XP/coin multiplier, record Combat bests in a dedicated slot (or map Combat consistently to one difficulty everywhere — `startRun`, `endRun`, `submitScore`, and the End-screen lookup), and add the `"Combat"` label/colour to the Loadout screen.

### 5. A corrupted save file can crash the game at launch — or silently wipe progress
`Profile.java:330/395/437/442`

Three compounding persistence faults:

1. **Crash-on-launch:** the v3 load reads `itemCount` straight from disk and does `new boolean[itemCount]` with no bounds check (`Profile.java:437`). A garbage/negative value throws `NegativeArraySizeException` (or `OutOfMemoryError` for a huge value) — **not** an `IOException`, so it escapes the `catch (IOException)`, propagates out of the `static { load(); }` initializer as `ExceptionInInitializerError`, and the game can't start. The comment literally says "defaults are fine," but this path isn't survivable.
2. **Non-atomic writes:** every `save()` opens `new FileOutputStream(FILE)`, which **truncates the good file before** the first byte is written. `save()` fires constantly (start/end of run, every purchase, every skin change, twice in the death frame). A crash/power-loss mid-write leaves a truncated file. (Same pattern in `Settings`, `Achievements`, `DailyChallenge`.)
3. **No rollback on partial read:** `load()` writes each field directly into the statics; an `EOFException` partway leaves a *hybrid* state (some loaded, some default) that the **next `save()` then persists permanently** (`Profile.java:442`). `historyIndex` is also loaded unvalidated and used as a raw array index in `endRun` (`Profile.java:191/422`) → `ArrayIndexOutOfBounds` on a bad file.

**Fix:** Clamp `itemCount` (and `historyIndex`/`historyCount`) before use and broaden the catch to `Exception` so any malformed save falls back to defaults. Write to `FILE + ".tmp"`, `flush()` + `getFD().sync()`, then `Files.move(tmp, real, ATOMIC_MOVE, REPLACE_EXISTING)` (factor one shared helper for all persisters). On any load failure, call `resetAll()` rather than half-applying.

### 6. Per-frame allocation defeats the engine's own "zero-GC" design
`TrailPool.java:92`, `GamePalette.java:97`, `HUD.java:152/412`, `Game.java:1253`

The render path runs up to ~240×/s and allocates heavily:

- **`TrailPool.render()`** (class header literally says "zero GC pressure") does `new Color(...)` for **every** live trail every frame, plus two `new int[]{...}` per diamond/triangle trail — worst case ~123k `Color` + ~246k `int[]`/sec.
- **`GamePalette.accent()/accent(int)`** allocate a `Color` on every call (10–15+ per frame), and in **colorblind mode** also run a full `RGBtoHSB`/`getHSBColor` round-trip each time — even though the value only changes once per tick.
- **`HUD.render()`** rebuilds a constant `GradientPaint` + two `Color`s every frame; **`renderToast()`** news up a `Font` every frame; the **geo-background cache** rebuilds on a frame counter, so at 240fps it rebuilds ~60×/s instead of the documented ~15.

**Fix:** Cache the accent `Color` (and its colorblind variant) when `update()` runs; hoist the HUD gradient/font to `static final`; reuse a small RGB→`Color` cache and scratch `int[]` buffers in `TrailPool`; gate the geo-cache rebuild on wall-clock time. This is the single biggest "feel/perf" win and is low-risk.

---

## Concurrency & threading

The game loop runs on a dedicated thread; all input runs on the Swing EDT; `AudioPlayer.musicEnded` fires on Slick's audio thread. Almost no shared state is `volatile` or guarded. Most of these are benign on Windows/x86 (strong memory model) but are real and worth fixing as a group with the §1 command-queue refactor.

- 🟠 **#23 Death-frame TOCTOU corrupts the profile.** `gameState` is non-volatile (`Game.java:322`). The death handler does a lot of work (incl. `Profile.submitScore`/`endRun` + saves) while still `STATE.Game`, flipping to `Dying` only at the end (`Game.java:745`). A `SPACE`/`ESC` press that reads the stale `Game` state can strand the run in `Shop`/`Paused` with `HEALTH <= 0`; resuming re-enters the death block and **runs `endRun`/`submitScore` a second time** — double XP/coins and inflated lifetime stats persisted to disk. *(Fix: make `gameState` volatile and funnel all transitions through one synchronized setter / the game-thread queue; ignore `SPACE`/`ESC` unless state is exactly `Game`/`Shop`/`Paused`.)*
- 🟡 **AudioPlayer playback state race** (`AudioPlayer.java:16/98`). `currentTrackIndex/paused/stopped/trackEnded/trackStartTime` are non-volatile; `checkAutoAdvance()` (game thread) races `next/prev/togglePlayPause/seek` (EDT) at a track boundary → overlapping tracks / desynced index/UI. *(Fix: volatile flags + one lock around the stop→increment→play transition.)*
- 🟡 **Player input booleans / `mouseShootHeld` / `mouseGameX/Y` non-volatile** (`Player.java:26`, `Game.java:102`). Real data race; worst realistic case ~1 frame of input latency (not the "stuck movement" some tools claim). *(Fix: mark `volatile`.)*
- 🟡 **`HUD.HEALTH` non-volatile** (`HUD.java:14`), written on the EDT at run start and read on the game thread. *(Fix: volatile, or set start-of-run health on the game thread.)*
- ⚪ **`running` non-volatile + written after `thread.start()`, and `stop()` self-joins** (`Game.java:392`). The shutdown path is dead (only `EXIT_ON_CLOSE` ends the loop). *(Fix: set `running=true` before `start()`, make it `volatile`, remove the self-join.)*

---

## Game loop & performance

- 🟡 **Busy-wait frame cap pins a core** (`Game.java:435`). After a coarse sleep the loop spins `while (...) Thread.yield();` for ~1 ms every frame (~¼ core at 240fps) and renders **static screens (Menu/Settings/Paused) at up to 240fps**. *(Fix: `parkNanos` to within ~0.2 ms of the deadline; cap non-gameplay states to ~60fps. Consider exposing an FPS/vsync toggle in Settings, which already has the `getShowFps` plumbing.)*
- 🟡 **HUD/attempt counter render inside the camera transforms** (`Game.java:986`). The flash/vignette effects are correctly drawn in screen space, but the HUD is re-wrapped in the camera zoom + look-ahead, so the health bar/score/icons **scale up to ~6.4% and drift up to 15px** with movement, and snap position at death (the Dying state draws the HUD in screen space). *(Fix: render the HUD and attempt counter in screen space, matching the Dying state.)*
- 🟡 **No delta clamp** (`Game.java:419`). After a long stall (GC, window drag, OS suspend) `while (delta >= 1) tick()` fast-forwards dozens of ticks in one frame with nothing rendered. *(Fix: clamp `delta` to ~5 before the inner loop.)*
- 🟡 **`getBeatPulse()` computed twice per frame** (`Game.java:1206/1332`) — two Slick2D `getPosition()` calls + `Math.pow` for an identical value. *(Fix: compute once per render pass.)*

---

## Bosses

- 🟠 **Laser boss spawns immortal damage "mines"** (`BossLaser.java:249`). Each tick the player overlaps a beam, it spawns a brand-new `EnemyBossBullet` at the player with velocity **(0,0)**. Those only self-remove when off-screen, so they **accumulate for the whole fight**, pump the TrailPool, and act as invisible-among-the-bullets static damage zones; parrying one does nothing (`-1.5 × 0 = 0`). *(Fix: apply beam damage directly to the player respecting i-frames, or give the proxy bullet a 1–2 tick self-expiry.)*
- 🟠 **Splitter fragments get a fraction of intended HP** (`BossSplitter.java:204`). `split()` passes the **already-per-generation-scaled** value into `setMaxHp()`, which **scales again** → gen-1 ≈ ⅓ and gen-2 ≈ 1/27–1/81 of intended. Combined with per-tick HP drain, the gen-2 quarters die almost instantly, so the boss's signature "splits into smaller copies" barely happens and it's trivially easy. *(Fix: pass the gen-0 base `900f * ratio` into `setMaxHp`, or set `maxHp` directly without re-scaling.)*
- 🟠 **Splitter HP bar + fragment counter vanish after the main body dies** (`BossSplitter.java:264`). Only `generation == 0` renders the aggregate bar, but gen-0 is removed ~30 ticks after it splits while gen-1/2 fight on — so the bar disappears exactly when the player needs "how many left?" feedback. *(Fix: render the aggregate bar from the lowest-generation living fragment.)*
- 🟡 **Laser lethal hitbox is ~2× the visible core** (`BossLaser.java:246`). Collision uses `perpDist < BEAM_WIDTH + 12` treating the half-width as `BEAM_WIDTH` (12) instead of `BEAM_WIDTH/2` (6) → "I didn't even touch it" deaths. *(Fix: `perpDist < BEAM_WIDTH/2f + 12`, or widen the drawn core.)*
- 🟡 **Splitter fragments freeze for 30 ticks after a split** (`BossSplitter.java:130`) — `warmupTimer = 30` for all generations returns before movement, so fragments sit motionless then dart off (looks like a hitch). *(Fix: `warmupTimer = generation == 0 ? 30 : 0;`.)*
- 🟡 **Overlapping Splitter deaths cut each other's cascade short** (`BossSplitter.java:154`) — each gen-2 death resets the shared cascade, so the finale feels weaker than other bosses. *(Fix: trigger once for the final fragment, or make the cascade additive.)*
- 🟡 **Aimed-burst fan is lopsided in phase 2+** (`EnemyBoss.java:178`) — offsets come out `-6,0,+6,+12,+18` (centred on the 2nd shot, not the middle). *(Fix: centre the spread on the burst midpoint.)*

---

## Enemies

- 🟠 **HardEnemy *replaces* velocity on bounce instead of negating it** (`HardEnemy.java:73`). On any wall hit it sets the component to a fresh random 1–7, which (a) **permanently wipes the spawn speed-scaling** the first time it touches a wall (a wave-60 Hard enemy moves like wave-1), and (b) the small new magnitude can fail to clear the wall next tick, **re-triggering `wallHit()` for several frames** (particle/flare spam + wall-hug jitter). Unlike `SwarmMinion`, it never clamps position. *(Fix: keep the magnitude, flip only the sign toward inside — `velY = (y<=0?1:-1)*Math.abs(velY)` — and clamp position after the flip.)*
- 🟠 **Diamond/triangle enemies use a full 32×32 square hitbox** (`HardEnemy.java:54`, also `FastEnemy`, `SwarmMinion`). The shapes are inscribed in the box (radius 16) but `getBounds()` returns the whole square, and collision uses `Rectangle.intersects()` — so the player takes damage (and bullets register) in **empty corners**. The triangle is the worst offender. *(Fix: inset the bounds to match the shape, or use a radius test; even a centred ~22–24px box removes the phantom hits.)*
- 🟡 **`takeDamage` guard is `hp < 0` not `hp <= 0`** (`BasicEnemy.java:30`, also Fast/Smart/Hard). An exactly-lethal hit leaves `hp == 0`, which still passes the guard, so a second projectile resolving the same tick (before `flushRemoves`) calls `die()` again → **double kill + double score credit**. *(Fix: `if (hp <= 0) return false;`, or a `dead` flag.)*
- 🟡 **SmartEnemy divides by raw corner distance with no zero-guard** (`SmartEnemy.java:66`) — distance 0 yields ±Infinity velocity (enemy teleports away). Rare, but the homing target also uses an inconsistent `−16` vs raw-corner reference. *(Fix: `if (distance < 1f) distance = 1f;` and use the same centre point for both.)*
- 🟡 **SwarmMinion has no combat HP** (`SwarmMinion.java:26`) — it uses `ID.BasicEnemy` so player shots/dash-strike target it, but `takeDamage` returns false so bullets **pass through and aren't consumed**. Inconsistent and surprising in Combat-mode Swarm fights. *(Fix: give it combat HP, or exclude it from the projectile filter.)*
- 🟡 **Spawn telegraph always previews a rounded square** (`SpawnTelegraph.java:61`) even for diamond/triangle enemies, so the only type cue during the warning is colour. *(Fix: draw the actual silhouette keyed off `toSpawn.getId()`.)*

---

## Player & abilities

- 🟠 **Dash-strike applies full damage every overlapping tick** (`Player.java:439`). `checkDashStrike()` runs all 8 dash ticks with no per-target dedup, so a target overlapped the whole dash takes up to **8 × 40 = 320** damage — amount depends on frame overlap, and it can effectively one-shot bosses. *(Fix: track a `Set<GameObject>` of struck targets per dash, cleared in `startDash()`.)*
- 🟡 **Player wall clamps use magic `37`/`60` instead of `SIZE` (48)** (`Player.java:225/313`). The player's right side pokes **11px past the right wall** while the bottom stops **12px short** — asymmetric, and inconsistent with enemies (which clamp flush to `WIDTH/HEIGHT − SIZE`). *(Fix: use `Game.WIDTH − SIZE` / `Game.HEIGHT − SIZE` in all four clamps.)*
- 🟡 **Cooldown abilities drop input instead of buffering it** (`Player.java:215`). Pressing Shift/Q one tick before the cooldown ends silently discards the press (the `else` clears the flag), so mashing feels unresponsive. *(Fix: a small input buffer — remember intent ~4–6 ticks and fire when the ability frees up.)* *(Also flagged as a balance/feel item.)*
- 🟡 **Trail colour allocated every tick even when no trail is emitted** (`Player.java:329`) — `lerpColor(...)` (two `Color` allocs) runs every tick but is used only every 2nd/3rd. *(Fix: move inside the `% trailRate == 0` block.)*
- 🟡 **`spawnShieldParticles` news up a `java.util.Random` per shield break** (`Player.java:503`). *(Fix: reuse one static `Random`, like the rest of the codebase.)*
- ⚪ **Streak smoothing during dash ignores the Streak Master perk** (`Player.java:247` vs `319`) — aura grows slower while dashing with that perk. *(Fix: apply `Perks.getStreakSpeedMultiplier()` in the dash branch too.)*

---

## Spawning & difficulty balance

*(See P1 §3 for the inversion/dead-zone cluster — the items below are the rest.)*

- 🟡 **Glass Cannon is a near-strict economic upgrade** (`Perks.java:157`, `HUD.java:84`). Its 2× score multiplier also doubles in-run shop **points**, end-run **XP**, and end-run **coins** — doubling the whole economy — while its only downside (half max HP) is routinely absorbed by the free shield (which doesn't even break streak) + dash i-frames. It overshadows the other perks. *(Fix: cap the bonus to displayed/leaderboard score only, or add a real downside such as shield-disabled.)*
- 🟡 **Glass Cannon + Fortified shows "62 / 63" at full health** (`Menu.java:446`, `HUD.java:80`). `(100+25)*0.5 = 62.5` → `bounds = −75`, and `maxHealth = 100 + (−75/2)` truncates to 63 while `HEALTH = 62.5`, so the bar can never read full. *(Fix: round `baseHealth` to an even int and store an explicit `maxHealth`.)*
- 🟡 **Survival boss fights grow without bound** (`Spawn.java:59`) — boss HP scales linearly per encounter with no cap. *(Fix: soft-cap or curve the HP growth.)*
- ⚪ **"caps around ~1.8×" comment is wrong** (`Spawn.java:497`) — the `Math.log` speed multiplier is unbounded. *(Fix: correct the comment or actually clamp.)*
- ⚪ **No-player mode (diff 4) stops spawning after level 4** (`Spawn.java:468`) — no endless branch; arena goes empty except bosses. *(If it's a showcase mode, document it; otherwise add an endless fall-through.)*
- ⚪ **Unreachable scripted cases on multiples of 10** (`Spawn.java:421/454/460`) — `tick()` routes those levels to the boss branch first, so `case 20`/`case 10` never run. *(Fix: remove or relocate.)*

> **Design note (my own observation):** pressing **Space** opens the in-run Shop, and the Shop state ticks no enemies — so it doubles as an unlimited free pause mid-wave (separate from P/Esc pause). Harmless to score (which also freezes), but worth a deliberate decision: gate the shop to between-wave windows, or accept it as intended.

---

## Economy, perks & progression

- 🟠 **"Reset All Progress" doesn't actually reset shop purchases** (`Menu.java:1040`, `Profile.java:321`, `CoinShop.java:178`). The reset calls `Profile.resetAll()` (which `save()`s, writing the **still-true** `CoinShop` purchased flags) *before* `CoinShop.resetAll()` clears them — and `CoinShop.resetAll()` never saves. So on next launch every permanent upgrade/skin/perk is **restored for free**. *(Fix: clear `CoinShop` (and `PlayerSkins` selection) before `Profile.resetAll()`, or have `resetAll()` reset them so its single `save()` writes clean state.)*
- 🟠 **Three achievements are permanently unobtainable** (`Achievements.java:179`). #20 Speedster (dash 50×), #21 Bullet Time (slow-mo 3×), #22 Shield Breaker (absorb 5) are commented "checked during gameplay" but **no in-run counter exists** — max completion caps at 47/50. *(Fix: add per-run counters in `Player`/`Game` and check them in `checkAfterRun`.)*
- 🟡 **#24 "Close Call" can never unlock** (`Achievements.java:181`) — the check needs `HUD.HEALTH > 0 && < 5`, but it only runs at death when `HEALTH` is clamped to 0. *(Fix: track the run's minimum non-zero health and test that.)*
- 🟡 **Reset leaves a now-locked skin equipped** (`Menu.java:1040`, `PlayerSkins`) — selection isn't reset and isn't revalidated on render, so the player keeps wearing (and re-persists) a cosmetic they no longer own; the Customize grid shows it simultaneously "selected" and "Locked." *(Fix: reset selection to defaults and fall back when the current selection is locked.)*
- 🟡 **Equipped perks survive a progress reset and keep applying** (`Perks.java:129`) — effect queries never re-check `isUnlocked()`, and `equipped[]` isn't cleared by `resetAll()`. After a reset mid-run a now-locked perk (e.g. Glass Cannon) keeps working. *(Fix: clear loadout in `resetAll()` and/or guard effect queries with `isUnlocked`.)*
- 🟡 **In-run "Refill Health" is buyable at full HP** (`Shop.java:317`) — no health-full guard, so it deducts points, raises its own cost, and burns a tier for nothing, with no UI signal. *(Fix: gate on `HEALTH < max` and grey the card when full.)*
- 🟡 **#23 "Triple Multiplier" checks lifetime streak, not the run's** (`Achievements.java:180`) — uses `Profile.getLongestStreak()` so it can unlock on a run where x3 was never reached; also no progress bar. #26 "No Shield Needed" doesn't match its text either. *(Fix: pass the run's peak multiplier and check that.)*
- 🟡 **`toggleEquip` always overwrites slot 1 when both are full** (`Perks.java:112`) — slot 0 becomes "sticky." *(Fix: FIFO replacement.)*
- 🟡 **Chosen loadout isn't persisted** (`Perks.java:49`) — `equipped[]` resets every launch. *(Fix: persist it if perks are meant to stick between sessions.)*
- ⚪ **Double save + duplicate high-score write on death** (`Game.java:664/692`) — `submitScore` and `endRun` both update `highScores` and both `save()` in the same frame. *(Fix: have `endRun` return the is-record flag; drop the separate `submitScore`.)*
- ⚪ **CoinShop logic & persistence are positional** (`CoinShop.java:104`, `Profile` save/load) — effect getters and the saved boolean array reference raw indices, so inserting/removing an item silently corrupts saved unlocks. *(Fix: persist keyed by `Item.id`.)*
- ⚪ **CoinShop purchase writes the profile twice and debits before marking owned** (`CoinShop.java:93`). *(Fix: set purchased first, single `save()`.)*

---

## Daily challenge

- 🟡 **Weekday labels & difficulty schedule are offset 4 days** (`DailyChallenge.java:53`). `dayOfWeek()` treats `epochDay % 7 == 0` as Monday, but epoch day 0 (1970-01-01) is a **Thursday** — so the whole Mon–Sun calendar (and the advertised Mon=Normal… schedule) is shifted; e.g. a real Tuesday shows as "Sat" and gets Insane instead of Normal. The seed/determinism is unaffected. *(Fix: `LocalDate.now().getDayOfWeek().getValue() - 1`, or offset epoch math by 3.)*
- 🟡 **Board shows stale results after a 28-day cycle rollover** (`DailyChallenge.java:150`). `checkCycleReset()` only clears after **>28** idle days; an active player crossing a cycle boundary keeps last cycle's `completed[]`/`scores[]` (indexed by `epochDay % 28`) until each slot is replayed. *(Fix: store the cycle index `epochDay/28` and clear when it changes.)*

---

## Audio & music

- 🟠 **Play/Pause button hit area is 22px below where it's drawn** (`MusicPlayer.java:84/323`). The most-used control: the top ~22px of the visible button doesn't respond, and a strip below it does. Prev/Next are correct — only Play carries a spurious `+ CTRL_SIZE/2`. *(Fix: drop the extra term in both the hover and click computations; factor into one helper.)*
- 🟠 **Auto-advance counts paused time** (`AudioPlayer.java:104`). End-of-track is decided by wall-clock vs duration, but pause/resume never shifts `trackStartTime`, so pausing 60s then resuming **force-skips the song ~60s early**. `seekTo()` rebases correctly — pause/resume is the inconsistent path. *(Fix: drive end detection off `music.getPosition()` (already used elsewhere), or accumulate paused time.)*
- 🟠 **One missing track file aborts the whole playlist** (`AudioPlayer.java:24`). All six `addTrack()` calls, listener registration, and `buildPlaylist()` share one try/catch; a missing `.ogg` throws and skips the rest → only track 1 loads, **no end listener**, and auto-advance loops it forever. *(Fix: wrap each `addTrack` individually; register listeners + build playlist unconditionally afterward.)*
- 🟡 **Volume applied one statement after `play()`** (`AudioPlayer.java:73`) — Slick2D starts at gain 1.0, so each track/auto-advance begins with a brief full-volume onset transient before dropping to ~0.10. *(Fix: `music.play(1f, volume)`.)*
- 🟡 **Volume slider rewrites `dotch_settings.dat` on every drag event** (`MusicPlayer.java:362` → `Settings.setMusicVolume` → `save()`) — dozens of full file rewrites per drag; same for `seekTo`. *(Fix: update live during drag, persist once on `mouseReleased`.)*
- 🟡 **Beat pulse is an open-loop metronome** (`AudioPlayer.java:202`) — assumes beat 1 at position 0 with no intro offset, so the visual pulse drifts off-beat over a track. *(Fix: per-track `beatOffsetSeconds` + more precise BPM.)*
- ⚪ **`sounds` map / `getSound()` are dead** (`AudioPlayer.java:198`) and `sfxVolume` is stored but never applied — there are no SFX. *(Fix: wire up SFX, or remove the dead plumbing — see also the README SFX mismatch.)*

---

## Front-end / UI / UX

- 🟠 **Loadout perk cards: click hit-test is 20px below the rendered cards** (`Menu.java:1948` vs `render` `gridY=180`). The top 20px of each card does nothing and the gap below equips it — selecting perks feels broken. *(Fix: use the same `gridY=180` (extract a shared constant).)*
- 🟡 **Pause-menu hover glow is swapped** between **Settings** and **Quit** (`Menu.java:3054`) — hovering Settings lights the red Quit button. Clicks are correct (position-based). *(Fix: render each button with its own hover slot.)*
- 🟡 **Game-over particles are spawned and ticked but never rendered** (`Game.java:772`) — `handler.render()` isn't called for `STATE.End`, so the intended Game-Over background never appears (wasted work). *(Fix: render them on the End screen, or stop spawning them.)*
- 🟡 **Main-menu music icon is invisible with no tracks, but its hit area still works** (`Menu.java:302`) — clicking empty top-left navigates to the Music Player. *(Fix: guard hover/click with the same `getTrackCount() > 0` used for drawing.)*
- 🟡 **Main menu allocates an AWT `Canvas` every tick for font metrics** (`Menu.java:2662`). *(Fix: cache the `FontMetrics` / link positions; recompute only when `WIDTH` changes.)*
- 🟡 **Colorblind mode only remaps the dynamic accent** (`GamePalette.java:99`). The colours that actually need disambiguating — enemy types (red/teal/purple/yellow, hardcoded in `Spawn` + enemy renderers) and the health bar's red/yellow/green — are untouched, so the toggle barely helps the players it's for. *(Fix: route enemy-type and health colours through a colorblind-aware helper, and/or add shape/pattern cues.)*

---

## Localization & documentation

- 🟡 **Combat-mode controls (F = shoot, Q = parry, click-to-shoot) are undocumented** (`KeyInput.java:25`) — absent from the README Controls table, the in-game Help, and the Settings panel. A player entering Combat has no way to discover parry. *(Fix: document them everywhere; add a Combat row to the Help/Settings controls.)*
- 🟡 **Dutch & German help pages omit Dash (Shift), Slow-Motion (E), and the auto-Shield note** (`Menu.java:546`) — the English page lists them. Non-English players never learn two core abilities. *(Fix: add the missing lines to HelpNLD/HelpDEU.)*
- 🟡 **README advertises an SFX volume slider that's disabled** (`Settings.java:37`) — no slider is rendered, the `SFX_VOL` drag branch and `setSfxBarH`/`getSfxVolume` are dead, and the changelog even says "Temporarily disabled SFX volume slider." *(Fix: implement it, or drop the claim and the dead plumbing.)*
- 🟡 **README advertises a Language selector that has no UI** (`Settings.java:66`) — `Settings.getLanguage()` has no consumer; language is actually chosen ad-hoc on the Help screen, and `setH` reserves dead "lang button" hover slots 12–17. *(Fix: add a real language control to Settings + have Help honour it, or remove the claim and dead slots.)*
- 🟡 **`Settings.load()` clamps `language` to 0–5 but the setter clamps 0–2** (`Settings.java:123`) — there are only 3 languages; a stale/corrupt file can load an out-of-range value (latent `AIOOBE` the day localization indexes a 3-element table). *(Fix: clamp the load to 0–2.)*
- 🟡 **HUD shows "LVL 0" / "00" watermark for the first level of every wave** (`HUD.java:142`). `triggerWaveAnnounce()` sets `lastBossLevel = level`, so `getLevelInWave()` is 0 at run start and for ~4s after every boss. *(Fix: display `getLevelInWave()+1`, or init `lastBossLevel = level − 1`.)*
- ⚪ **Combat mode has zero documentation** in README/About/Help (`README.md:9`) despite being a full mode. *(Fix: add a Combat entry to Difficulty Modes / About.)*
- ⚪ **README version history is stale** — game is v4.1, README tops out at v4.0 (`README.md:170`). *(Fix: add the v4.1 entry; reconcile the SFX/language claims.)*

---

## Cleanup (dead code — no runtime effect, but removes traps)

- **`Stats.java`** is fully superseded by `Profile.java` and never called, yet still owns a parallel `dotch_stats.dat` read/write path — a format-drift hazard. *(Delete, or reduce to a one-shot legacy importer.)*
- **`Trail.java`** is never instantiated (everything uses `TrailPool`); it duplicates the decay/shape logic. *(Delete; keep shape constants if referenced.)*
- **`SpriteSheet.java`** and **`BufferedImageLoader.java`** are unused; both have latent bugs (`SpriteSheet.grabImage` has inverted axes/`RasterFormatException`; `BufferedImageLoader.loadImage` lets `IllegalArgumentException` from a null URL escape its `IOException`-only catch). *(Delete, or null-check + broaden the catch if kept.)*
- **`Spawn.triggerBossSpawn()`** (no-arg), **`Player.addShieldCharge()`/`addSlowmoCharge()`**, **`HUD.kills`/`addKill()`/`getKills()`** (display uses `Game.getRunKills()`), and the **`mult *= 1f` no-op** in `Perks.getDamageMultiplier()` are all unused/dead. *(Remove or wire up.)*
- **Menu dead members** (`Menu.java:24`): unused `MUSIC_*` constants + `musicX()`, `SHOP_BTN_*`/`DAILY_BTN_*`, the `shopH` hover float, `setSfxBarH`, the unreachable `SettingsDrag.SFX_VOL` drag branch, and `pauseBtn[0..2]`. *(Remove to clarify the real layout math.)*
- **`Game.java:149/496`** — cascade particles are allocated 8-wide but written 9-wide; the 9th (size) value is never read (render derives size from life). *(Either use it or drop it.)*
- **`HUD.java:96`** — `lastMilestone >= 0` is always true in the milestone check. *(Simplify.)*
- **`TrailPool.tick()`** (`:77`) — decay kills trails at `alpha ≈ life` (not ~0) so they pop while still partly visible; the `− 0.0001f` term is a meaningless nudge. *(Decay by a clear rate, fade to 0.)*

---

## What's solid (don't touch)

- The fixed-timestep loop's *simulation* model (player ticks at full rate; everything else at `timeScale`) is clean, and the slow-mo accumulator is correct.
- `TrailPool`'s pooling concept is right — it just needs the per-frame `Color`/`int[]` allocations removed to deliver on its promise.
- The save **format** is sensibly versioned with append-only growth and `min(count, size)` guards on load; it's the **atomicity and validation** that need hardening, not the layout.
- Enemy wall-bounce for Basic/Fast is correct (velocity always flips inward — no stuck-wall bug); only HardEnemy's *replace-magnitude* variant is wrong.
- The visual/“juice” layer (screen shake, camera look-ahead, transitions, boss intro, death sequence, beat-reactive background) is well-built and a real strength.

---

## Suggested order of attack

1. **§1 Handler race** (crash/freeze) — biggest stability win; the command-queue refactor also resolves most of the §Concurrency list and the §23 profile-corruption TOCTOU.
2. **§5 save robustness** (atomic write + validation + rollback) — prevents the launch crash and progress loss; small, self-contained.
3. **§2 boss soft-lock** and **§3 difficulty inversion/dead-zones** — these make the core game behave as designed.
4. **§4 Combat-mode bookkeeping** — make the mode's rewards/scores consistent.
5. **§6 per-frame allocation** — one focused pass over `TrailPool`/`GamePalette`/`HUD` for a smoother, cooler-running game.
6. The P2/P3 bug fixes (boss mines, splitter HP, dash-strike dedup, hitboxes, hit-test offsets, refill-at-full) — each is small and independently shippable.
7. Front-end/docs/localization polish and the dead-code cleanup as a final sweep.
