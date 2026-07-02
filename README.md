# Dotch.

A fast-paced 2D dodge game built in Java using AWT/Swing. Navigate through waves of enemies, defeat bosses, unlock skins, and chase high scores across an endless progression system.

Current build: **v4.1** (March 2026).

## Gameplay

Move your player around the arena to dodge enemies. Each collision drains health. Survive as long as possible, earn points for in-run upgrades, and accumulate XP and coins across runs to unlock permanent progression.

Two mechanics run underneath every mode:

- **Spawn telegraphs** -- No enemy ever appears without warning. Each spawn is preceded by a ~1-second telegraph (pulsing rings, crosshair, and a materializing shape) so you can read the arena before it fills.
- **Dodge streak & multiplier** -- Surviving near-misses builds a dodge streak that ramps your score multiplier (x1 -> x2 -> x3). Your player aura and trail react as the streak climbs, and the streak resets when you take a hit.

### Difficulty Modes

- **Normal** -- Predictable bouncing enemies. Recommended for beginners.
- **Hard** -- Adds the erratic Hard enemy type that re-rolls its direction on every wall bounce. Default mode.
- **Insane** -- Fast, smart, and unpredictable enemies combined, scaling faster. For experienced players.
- **Combat** -- Fight back: shoot, parry, and dash-strike enemies. Bosses no longer die on their own timer -- you have to actively damage them.

Boss fights occur every 10 levels on all modes.

### Enemy Types

| Enemy | Shape | Behavior |
|-------|-------|----------|
| Basic | Red rounded square (glow) | Bounces diagonally in straight lines |
| Fast | Teal spinning diamond | Fast, near-vertical, continuously rotating |
| Smart | Purple breathing circle | Homes in on the player with a rotating crosshair ring |
| Hard | Amber triangle | Re-rolls its direction on every wall bounce; points where it travels |
| Swarm Minion | Small orange diamond | Summoned by the Swarm boss; some fire homing bullets |

Every enemy has a glow halo, a shape-matched fading trail, and bursts into particles on death, awarding a kill bonus (Basic 50, Fast 75, Hard 100, Smart 125).

### Boss Types

A boss spawns every 10th level. The first boss is always Classic; later bosses rotate by level and difficulty. Base boss HP scales each encounter (1200 at level 10, +200 per boss), with per-type modifiers.

- **Classic** -- Pulsing square that moves horizontally and fires spread shots, aimed bursts, wave streams, and ring bursts across 3 phases.
- **Splitter** -- Bouncing hexagon that splits into smaller, faster copies on defeat -- each split again into a final generation.
- **Laser** -- Stationary circle in the center that fires telegraphed, rotating sweeping laser beams the player must dodge.
- **Swarm** -- Fragile drifting pentagon that doesn't attack directly; instead it summons waves of mini-enemies that shoot projectiles.

**Defeating a boss:** outside Combat mode, a boss's health drains automatically over time and accelerates as it drops -- surviving the fight is what beats it. You can speed a boss's demise by **parrying its bullets** back at it (a parried bullet deals heavy damage). In **Combat mode** the automatic drain is cut to ~35%, so bosses must be actively damaged with shooting, parries, and dash-strikes.

### Player Abilities

| Key | Ability | Description |
|-----|---------|-------------|
| Shift | Dash | 120px directional burst with invincibility frames and an afterimage trail (cooldown scales with perks) |
| E | Slow Motion | Everything slows to 30% speed for ~2.5s; charges now regenerate over time (~10s each) |
| Auto | Shield | Absorbs one hit per charge, shatters with particles, then recharges over ~30s (start with 1+ charges) |

**Combat mode adds:**

| Key | Ability | Description |
|-----|---------|-------------|
| F / Hold Mouse | Shoot | Fire a projectile aimed at the cursor (limited ammo that regenerates; faster with a higher streak) |
| Q | Parry | Brief parry window that deflects boss bullets, deals damage, and spikes your streak |
| Shift | Dash-Strike | Dashing through an enemy in Combat mode damages it |

### In-Run Shop

Press Space during a run to toggle the shop open and closed:

- **Upgrade Health** -- Extends and refills the health bar.
- **Upgrade Speed** -- Increases movement speed.
- **Refill Health** -- Restores health to full.

Each upgrade is tiered (capped at a maximum tier) with escalating point costs per purchase.

### Endless Scaling

After the last scripted level for a mode, a formula-based spawner takes over. Enemy count, speed, and type distribution scale infinitely, weighting harder enemies over time. Periodic purge waves clear the field and spawn a fresh, tougher pack (roughly every 15 waves on standard modes, every 12 in Combat).

## Meta-Progression

### XP & Profile Level

Every run earns XP based on score, level reached, difficulty, and bosses defeated. XP accumulates into a persistent profile level displayed on the main menu and game over screen.

### Coins & Coin Shop

Runs also earn coins -- a separate currency spent in the Coin Shop on permanent unlocks. The shop holds **25 items across 4 categories**:

- **Permanent Upgrades** (7) -- Starting health, speed, extra shield/slow-mo charges, coin earning bonus, and more.
- **Skin Shapes** (5) -- Circle, Triangle, Diamond, Star, Hexagon.
- **Color Palettes** (7) -- Neon, Ember, Royal, Ghost, Golden, Crimson, Lime.
- **Perk Unlocks** (6) -- Early access to perks that otherwise unlock via profile level or milestones.

### Achievements

50 achievements defined across 4 categories (Survival, Skill, Persistence, Challenge), with in-game toast notifications on unlock and a dedicated achievements page with progress bars for milestone goals. *(A few skill achievements -- Speedster, Bullet Time, Shield Breaker -- are defined but not yet wired up in the current build.)*

### Loadout / Perks

Before each run, choose up to 2 passive perks:

| Perk | Effect |
|------|--------|
| Swift | +1 starting speed |
| Thick Skin | 20% damage reduction |
| Fortified | +25 starting health |
| Glass Cannon | Half health, double score |
| Adrenaline | 40% shorter dash cooldown |
| Second Wind | 50% faster shield recharge |
| Slow Starter | +1 slow-motion charge |
| Streak Master | Dodge streak builds 30% faster |

Perks unlock via profile level, milestones (bosses defeated), achievements, or early purchase in the Coin Shop.

### Player Customization

6 player shapes (Square, Circle, Triangle, Diamond, Star, Hexagon) and 8 color palettes (Default, Neon, Ember, Royal, Ghost, Golden, Crimson, Lime), each shape with a matching trail style. Unlocked via profile level, lifetime milestones, achievements, or purchased with coins.

### Daily Challenge

One seeded run per day with deterministic enemy spawns and a rotating difficulty (28-day / 4-week cycle). No perks or shop bonuses -- pure skill. Score and streak tracked on a dedicated calendar page.

## Controls

| Key | Action |
|-----|--------|
| W / Up Arrow | Move up |
| S / Down Arrow | Move down |
| A / Left Arrow | Move left |
| D / Right Arrow | Move right |
| Shift | Dash (dash-strike in Combat) |
| E | Slow Motion |
| F / Hold Mouse | Shoot (Combat mode) |
| Q | Parry (Combat mode) |
| Space | Open/close shop |
| P / Escape | Pause |
| Escape | Back (menus) |
| Mouse Wheel | Scroll (menus) |

## Settings

Configurable from the Settings page (main menu or pause menu):

- Music volume slider
- Screen shake intensity (Off / Low / High)
- Particle density (Low / Medium / High)
- Show FPS toggle
- Player trail toggle
- Grid dots toggle
- Colorblind mode
- Controls reference
- Danger Zone: Reset High Scores, Reset All Progress

*The SFX volume slider and in-game language selection are temporarily disabled in v4.1.*

## Statistics

Dedicated dashboard tracking lifetime stats across scrollable panels:

- Profile level, total XP, total games, deaths, time played, cumulative score
- Per-difficulty best scores, best levels, attempts, and survival times
- Enemy encounters by type, damage taken, longest streak, bosses defeated
- Upgrade breakdown by type, favorite difficulty
- Recent run history with score chart (last 20 runs)

## Multi-language Help

In-game help available in English, Dutch, and German.

## Music

Built-in music player with 6 tracks, shuffle on load, and auto-advance:

- Virtual Riot - Energy Drink
- MDK - Press Start
- Desmeon - Hellcat
- MDK - Fingerbang
- Pegboard Nerds - Disconnected
- Avicii - Levels (Skrillex Remix)

Transport controls (previous / play-pause / next), a seekable progress bar, an in-player volume slider, and a BPM beat-pulse are all included. *(Sound effects are not implemented in the current build.)*

## Requirements

- Java 8 or later
- Libraries included in `libs/jars/`:
  - Slick2D
  - LWJGL
  - JOrbis / JOgg
  - JInput

## Running

Compile and run the `Game` class as the main entry point:

```
javac -cp "libs/jars/*" -d out src/*.java
java -Dsun.java2d.opengl=true -Dsun.java2d.translaccel=true -Dsun.java2d.ddscale=true -cp "out;libs/jars/*" Game
```

## Version History

- **v4.1** (March 2026) -- New main menu layout (Play, Profile, Shop, Settings, Help) and a Profile hub for Customize, Statistics, and Achievements. Ability icons, compact music-player icon. Slow-motion charges now regenerate over time, with a chrome time-warp visual. HUD spacing and alignment fixes. Performance: render loop capped at 60 FPS (was uncapped), deferred object removal, and cached hot-path objects. SFX volume slider and language selection temporarily disabled.
- **v4.0** (March 2026) -- Player abilities, 4 boss types, endless scaling, XP/profile system, 50 achievements, skin customization, loadout/perks, coin shop, daily challenges, settings page, new music.
- **v3.0** (March 2026) -- Visual overhaul with dark theme, fullscreen, music player.
- **v2.0** (March 2026) -- Complete codebase rewrite.
- **v1.0** (November 2019) -- Renamed to Dotch, layout update, language support.
- **v0.1** (August 2016) -- Initial release.
