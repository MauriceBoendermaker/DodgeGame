# Dotch.

A 2D dodge game built in Java using AWT/Swing. Control a player block and avoid increasingly difficult waves of enemies across 25 levels.

## Gameplay

Move your block around the screen to dodge enemies. Each collision drains your health bar. Survive as long as possible, earn points, and spend them in the shop to upgrade your abilities.

### Difficulty Modes

- **Normal** -- Standard enemy spawns with a boss at level 10.
- **Hard** -- Tougher enemies introduced earlier, boss at level 15.
- **Insane** -- Fast and smart enemies from the start, boss at level 15.

### Enemy Types

| Enemy | Behavior |
|-------|----------|
| Basic | Bounces around the screen |
| Fast | Moves at higher speed |
| Smart | Tracks and follows the player |
| Hard | Aggressive bouncing pattern |
| Boss | Large enemy that moves horizontally and fires projectiles |

### Shop

Earn points during gameplay and spend them between waves by pressing Space:

- **Upgrade Health** -- Extends the health bar (cost starts at 1000, increases by 250).
- **Upgrade Speed** -- Increases player movement speed (cost starts at 750, increases by 250).
- **Refill Health** -- Restores health to full (cost starts at 750, increases by 250).

## Controls

| Key | Action |
|-----|--------|
| W / Up Arrow | Move up |
| S / Down Arrow | Move down |
| A / Left Arrow | Move left |
| D / Right Arrow | Move right |
| Space | Open/close shop |
| P | Pause |
| Escape | Quit |

## Multi-language Help

In-game help is available in English, Dutch, German, French, Russian, and Spanish.

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
java -cp "out;libs/jars/*" Game
```

## Status

Beta -- the game is functional but marked as a test version.
