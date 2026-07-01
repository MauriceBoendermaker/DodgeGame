# Dotch.

A fast-paced 2D dodge game built in Java using AWT/Swing. Navigate through waves of enemies, defeat bosses, unlock skins, and chase high scores across an endless progression system.

## Gameplay

Move your player around the arena to dodge enemies. Each collision drains health. Survive as long as possible, earn points for in-run upgrades, and accumulate XP and coins across runs to unlock permanent progression.

### Difficulty Modes

- **Normal** -- Predictable bouncing enemies. Recommended for beginners.
- **Hard** -- Enemies randomize direction on each bounce. Unpredictable.
- **Insane** -- Fast, smart, and random enemies combined. For experienced players.

Boss fights occur every 10 levels on all difficulties.

### Enemy Types

| Enemy | Shape | Behavior |
|-------|-------|----------|
| Basic | Red square | Bounces in straight lines |
| Fast | Teal diamond | High-speed diagonal movement |
| Smart | Purple circle | Tracks and follows the player |
| Hard | Yellow triangle | Randomizes direction on bounce |
| Boss | Large square | Moves horizontally, fires bullet patterns |

### Boss Types

Four boss variants rotate based on level and difficulty:

- **Classic** -- Fires spread shots, aimed bursts, wave streams, and ring bursts across 3 phases.
- **Splitter** -- Splits into smaller copies on defeat, each splitting again.
- **Laser** -- Stationary in center, rotates sweeping laser beams the player must dodge.
- **Swarm** -- Fragile but summons waves of mini-enemies that shoot projectiles.

### Player Abilities

| Key | Ability | Description |
|-----|---------|-------------|
| Shift | Dash | 120px directional burst with invincibility frames |
| E | Slow Motion | Everything slows to 30% speed for 2.5 seconds (limited charges) |
| Auto | Shield | Absorbs one hit, shatters with particle effect, recharges over 30s |

### In-Run Shop

Press Space to open the shop between waves:

- **Upgrade Health** -- Extends and refills the health bar.
- **Upgrade Speed** -- Increases movement speed.
- **Refill Health** -- Restores health to full.

### Endless Scaling

After the last scripted level per difficulty, a formula-based spawner takes over. Enemy count, speed, and type distribution scale infinitely. Periodic purge waves clear the field and spawn harder replacements.

## Meta-Progression

### XP & Profile Level

Every run earns XP based on score, level reached, difficulty, and bosses defeated. XP accumulates into a persistent profile level displayed on the main menu and game over screen.

### Coins & Coin Shop

Runs also earn coins -- a separate currency spent in the Coin Shop on permanent unlocks:

- **Permanent Upgrades** -- Starting health, speed, extra shield/slow-mo charges, coin earning bonus.
- **Skin Shapes** -- Circle, Triangle, Diamond, Star, Hexagon.
- **Color Palettes** -- Neon, Ember, Royal, Ghost, Golden, Crimson, Lime.
- **Perk Unlocks** -- Early access to perks that also unlock via profile level.

### Achievements

50 achievements across 4 categories (Survival, Skill, Persistence, Challenge). In-game toast notifications on unlock. Dedicated achievements page with progress bars.

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

### Player Customization

6 player shapes and 8 color palettes, each with a matching trail style. Unlocked via profile level, achievements, lifetime milestones, or purchased with coins.

### Daily Challenge

One seeded run per day with deterministic enemy spawns. 28-day cycle (4 weeks) with rotating difficulty. No perks or shop bonuses -- pure skill. Score and streak tracked on a dedicated calendar page.

## Controls

| Key | Action |
|-----|--------|
| W / Up Arrow | Move up |
| S / Down Arrow | Move down |
| A / Left Arrow | Move left |
| D / Right Arrow | Move right |
| Shift | Dash |
| E | Slow Motion |
| Space | Open/close shop |
| P / Escape | Pause |
| Escape | Back (menus) |

## Settings

Configurable from the main menu or pause menu:

- Music and SFX volume sliders
- Screen shake intensity (Off / Low / High)
- Particle density (Low / Medium / High)
- Show FPS toggle
- Player trail toggle
- Grid dots toggle
- Colorblind mode
- Language selection (English, Dutch, German)

## Statistics

Dedicated dashboard tracking lifetime stats:

- Total games, deaths, time played, cumulative score
- Per-difficulty best scores, levels, and survival times
- Enemy encounters by type, damage taken, longest streak
- Upgrades purchased, bosses defeated, favorite difficulty
- Recent run history with score chart (last 20 runs)

## Multi-language Help

In-game help available in English, Dutch, and German.

## Music

Built-in music player with 6 tracks:

- Virtual Riot - Energy Drink
- MDK - Press Start
- Desmeon - Hellcat
- MDK - Fingerbang
- Pegboard Nerds - Disconnected
- Avicii - Levels (Skrillex Remix)

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

- **v4.0** (March 2026) -- Player abilities, 4 boss types, endless scaling, XP/profile system, 50 achievements, skin customization, loadout/perks, coin shop, daily challenges, settings page, new music.
- **v3.0** (March 2026) -- Visual overhaul with dark theme, fullscreen, music player.
- **v2.0** (March 2026) -- Complete codebase rewrite.
- **v1.0** (November 2019) -- Renamed to Dotch, layout update, language support.
- **v0.1** (August 2016) -- Initial release.
