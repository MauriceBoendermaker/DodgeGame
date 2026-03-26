import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Player extends GameObject {

    private static final int SIZE = 48;
    private static final int R = 12;
    private static final Color FILL = new Color(230, 234, 240);

    private static final float ACCEL = 0.45f;
    private static final float DECEL = 0.30f;

    private static final Font MULT_FONT = new Font("Arial", Font.BOLD, 16);

    private Handler handler;
    private int trailTick = 0;

    // Input state
    public boolean moveUp, moveDown, moveLeft, moveRight;

    // Invincibility frames
    private int iFrames = 0;
    private static final int I_FRAME_DURATION = 45; // ~0.75 seconds

    // Streak system
    private int streakTicks = 0;
    private float streakLevel = 0;
    private float hitPop = 0;
    private float auraPhase = 0;

    private static final int STREAK_T1 = 180;
    private static final int STREAK_T2 = 480;
    private static final int STREAK_T3 = 900;
    private static final int STREAK_MAX = 1200;

    // ===== DASH =====
    public boolean dashInput = false;
    private boolean dashing = false;
    private int dashTimer = 0;
    private float dashVelX, dashVelY;
    private int dashCooldown = 0;
    private static final int DASH_DURATION = 8;    // ticks (~0.13s)
    private static final int DASH_COOLDOWN = 90;   // ticks (~1.5s)
    private static final float DASH_DISTANCE = 120f;
    private static final float DASH_SPEED = DASH_DISTANCE / DASH_DURATION;

    // ===== SHIELD =====
    private int shieldCharges = 1 + CoinShop.getExtraShieldCharges();
    private boolean shieldActive = true;
    private int shieldCooldown = 0;
    private float shieldBreakEffect = 0; // 1.0 on break, decays
    private float shieldPulse = 0;
    private static final int SHIELD_COOLDOWN = 1800; // 30 seconds
    // Shatter particles [x, y, vx, vy, life]
    private float[][] shieldParticles;

    // ===== SLOW-MOTION =====
    public boolean slowmoInput = false;
    private int slowmoMaxCharges = 1 + Perks.getExtraSlowmoCharges() + CoinShop.getExtraSlowmoCharges();
    private int slowmoCharges = slowmoMaxCharges;
    private int slowmoTimer = 0;
    private int slowmoRegenTimer = 0;
    private static final int SLOWMO_DURATION = 150; // ticks at full speed (~2.5s)
    private static final int SLOWMO_REGEN_TICKS = 600; // ticks to regen one charge (~10s)

    public Player(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
    }

    public float getStreakLevel() { return streakLevel; }
    public int getStreakTicks() { return streakTicks; }

    public int getMultiplier() {
        if (streakTicks >= STREAK_T3) return 3;
        if (streakTicks >= STREAK_T2) return 2;
        return 1;
    }

    // Ability getters for HUD
    public float getDashCooldownPct() { return dashCooldown / (DASH_COOLDOWN * Perks.getDashCooldownMultiplier()); }
    public boolean isDashing() { return dashing; }
    public boolean isShieldActive() { return shieldActive; }
    public float getShieldCooldownPct() { return shieldCooldown / (SHIELD_COOLDOWN * Perks.getShieldCooldownMultiplier()); }
    public float getShieldBreakEffect() { return shieldBreakEffect; }
    public int getSlowmoCharges() { return slowmoCharges; }
    public int getSlowmoMaxCharges() { return slowmoMaxCharges; }
    public boolean isSlowmoActive() { return slowmoTimer > 0; }
    public float getSlowmoTimerPct() { return slowmoTimer / (float) SLOWMO_DURATION; }
    public float getSlowmoRegenPct() { return slowmoRegenTimer / (float) SLOWMO_REGEN_TICKS; }

    public void addShieldCharge() {
        shieldActive = true;
        shieldCooldown = 0;
        shieldCharges = 1 + CoinShop.getExtraShieldCharges();
    }

    public void addSlowmoCharge() { slowmoCharges++; }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        // Dash cooldown always ticks (not affected by slow-mo from player perspective)
        if (dashCooldown > 0) dashCooldown--;
        if (shieldCooldown > 0) {
            shieldCooldown--;
            if (shieldCooldown <= 0) {
                shieldActive = true;
                shieldCharges = 1 + CoinShop.getExtraShieldCharges();
            }
        }
        if (shieldBreakEffect > 0.01f) shieldBreakEffect *= 0.92f;
        else shieldBreakEffect = 0;
        shieldPulse += 0.05f;

        // Slow-motion timer
        if (slowmoTimer > 0) {
            slowmoTimer--;
            if (slowmoTimer <= 0) {
                Game.setTimeScale(1f);
            }
        }

        // Slow-motion charge regen
        if (slowmoTimer <= 0 && slowmoCharges < slowmoMaxCharges) {
            slowmoRegenTimer++;
            if (slowmoRegenTimer >= SLOWMO_REGEN_TICKS) {
                slowmoCharges++;
                slowmoRegenTimer = 0;
            }
        }

        // Update shield particles
        if (shieldParticles != null) {
            boolean anyAlive = false;
            for (float[] p : shieldParticles) {
                if (p[4] > 0) {
                    p[0] += p[2];
                    p[1] += p[3];
                    p[2] *= 0.96f;
                    p[3] *= 0.96f;
                    p[4] -= 0.02f;
                    anyAlive = true;
                }
            }
            if (!anyAlive) shieldParticles = null;
        }

        // === DASH ===
        if (dashInput && !dashing && dashCooldown <= 0) {
            dashInput = false;
            startDash();
        } else {
            dashInput = false;
        }

        if (dashing) {
            x += dashVelX;
            y += dashVelY;
            x = Game.clamp(x, 0, Game.WIDTH - 37);
            y = Game.clamp(y, 0, Game.HEIGHT - 60);
            dashTimer--;

            // Afterimage trail — intense during dash
            Color dashCol = GamePalette.accent();
            handler.addObject(new Trail(x, y, ID.Trail, dashCol, SIZE, SIZE, 0.06f, handler, PlayerSkins.getSelectedTrailShape()));

            if (dashTimer <= 0) {
                dashing = false;
                dashCooldown = (int) (DASH_COOLDOWN * Perks.getDashCooldownMultiplier());
                velX = dashVelX * 0.3f; // carry some momentum
                velY = dashVelY * 0.3f;
            }
            // Skip normal movement during dash, but still check streak and collision
            streakTicks++;
            float targetStreak = Math.min(streakTicks / (float) STREAK_MAX, 1f);
            streakLevel += (targetStreak - streakLevel) * 0.04f;
            auraPhase += 0.06f + streakLevel * 0.04f;
            if (hitPop > 0.01f) hitPop *= 0.9f; else hitPop = 0;
            if (iFrames > 0) iFrames--;
            // No collision during dash — i-frames
            return;
        }

        // === SLOW-MO ===
        if (slowmoInput && slowmoCharges > 0 && slowmoTimer <= 0) {
            slowmoInput = false;
            slowmoCharges--;
            slowmoRegenTimer = 0;
            slowmoTimer = SLOWMO_DURATION;
            Game.setTimeScale(0.3f);
        } else {
            slowmoInput = false;
        }

        // Normal movement
        float speed = handler.spd;

        float targetVX = 0, targetVY = 0;
        if (moveUp) targetVY -= speed;
        if (moveDown) targetVY += speed;
        if (moveLeft) targetVX -= speed;
        if (moveRight) targetVX += speed;

        if (targetVX != 0 && targetVY != 0) {
            targetVX *= 0.707f;
            targetVY *= 0.707f;
        }

        float lerpX = targetVX != 0 ? ACCEL : DECEL;
        float lerpY = targetVY != 0 ? ACCEL : DECEL;
        velX += (targetVX - velX) * lerpX;
        velY += (targetVY - velY) * lerpY;

        if (Math.abs(velX) < 0.1f && targetVX == 0) velX = 0;
        if (Math.abs(velY) < 0.1f && targetVY == 0) velY = 0;

        x += velX;
        y += velY;
        x = Game.clamp(x, 0, Game.WIDTH - 37);
        y = Game.clamp(y, 0, Game.HEIGHT - 60);

        // Streak grows
        streakTicks++;
        float targetStreak = Math.min(streakTicks / (float) STREAK_MAX, 1f);
        streakLevel += (targetStreak - streakLevel) * 0.04f * Perks.getStreakSpeedMultiplier();
        auraPhase += 0.06f + streakLevel * 0.04f;

        if (hitPop > 0.01f) hitPop *= 0.9f;
        else hitPop = 0;

        // Trail
        if (Settings.getPlayerTrail()) {
            int trailRate = streakLevel > 0.5f ? 2 : 3;
            float trailLife = 0.045f - streakLevel * 0.02f;
            Color trailCol = lerpColor(new Color(200, 210, 220),
                    GamePalette.accent(), streakLevel * 0.6f);

            if (++trailTick % trailRate == 0) {
                handler.addObject(new Trail(x, y, ID.Trail, trailCol, SIZE, SIZE, Math.max(trailLife, 0.015f), handler, PlayerSkins.getSelectedTrailShape()));
            }
        }

        if (iFrames > 0) iFrames--;
        collision();
    }

    private void startDash() {
        dashing = true;
        dashTimer = DASH_DURATION;
        iFrames = DASH_DURATION + 5; // i-frames during dash + tiny buffer

        // Dash in movement direction, or facing direction if stationary
        float dx = 0, dy = 0;
        if (moveUp) dy -= 1;
        if (moveDown) dy += 1;
        if (moveLeft) dx -= 1;
        if (moveRight) dx += 1;

        // If no direction held, use current velocity
        if (dx == 0 && dy == 0) {
            dx = velX;
            dy = velY;
        }

        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            dashVelX = (dx / len) * DASH_SPEED;
            dashVelY = (dy / len) * DASH_SPEED;
        } else {
            // Default: dash right
            dashVelX = DASH_SPEED;
            dashVelY = 0;
        }

        Game.triggerScreenShake(3f);
    }

    private float getDamage(ID id) {
        switch (id) {
            case FastEnemy:    return 8;
            case BasicEnemy:   return 12;
            case HardEnemy:    return 15;
            case SmartEnemy:   return 20;
            case EnemyBoss:    return 25;
            default:           return 12;
        }
    }

    private void collision() {
        if (iFrames > 0) return;

        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject tempObject = handler.getObjects().get(i);
            ID id = tempObject.getId();
            if (id == ID.BasicEnemy || id == ID.FastEnemy
                    || id == ID.SmartEnemy || id == ID.HardEnemy
                    || id == ID.EnemyBoss) {
                if (getBounds().intersects(tempObject.getBounds())) {
                    // Shield absorbs the hit
                    if (shieldActive) {
                        shieldCharges--;
                        if (shieldCharges <= 0) {
                            shieldActive = false;
                            shieldCooldown = (int) (SHIELD_COOLDOWN * Perks.getShieldCooldownMultiplier());
                        }
                        shieldBreakEffect = 1f;
                        spawnShieldParticles();
                        iFrames = I_FRAME_DURATION;
                        Game.triggerScreenShake(5f);
                        return;
                    }

                    float dmg = getDamage(id) * Perks.getDamageMultiplier();
                    HUD.HEALTH -= dmg;
                    Game.addDamage(dmg);
                    iFrames = I_FRAME_DURATION;
                    Game.triggerHit();

                    if (streakTicks > STREAK_T1) {
                        hitPop = 1f;
                    }
                    streakTicks = 0;
                    streakLevel = 0;
                    return;
                }
            }
        }
    }

    private void spawnShieldParticles() {
        shieldParticles = new float[24][5];
        java.util.Random rng = new java.util.Random();
        int cx = (int) x + SIZE / 2;
        int cy = (int) y + SIZE / 2;
        for (int i = 0; i < 24; i++) {
            float angle = rng.nextFloat() * (float) (Math.PI * 2);
            float speed = 2f + rng.nextFloat() * 5f;
            shieldParticles[i] = new float[]{
                    cx + (float) Math.cos(angle) * (SIZE / 2 + 8),
                    cy + (float) Math.sin(angle) * (SIZE / 2 + 8),
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    0.8f + rng.nextFloat() * 0.2f
            };
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;
        int cx = ix + SIZE / 2, cy = iy + SIZE / 2;

        Color accent = GamePalette.accent();

        // Shield shatter particles
        if (shieldParticles != null) {
            for (float[] p : shieldParticles) {
                if (p[4] > 0) {
                    int alpha = (int) (p[4] * 200);
                    g2.setColor(new Color(140, 220, 255, Math.min(alpha, 255)));
                    int sz = 3 + (int) (p[4] * 4);
                    g2.fillRect((int) p[0] - sz / 2, (int) p[1] - sz / 2, sz, sz);
                }
            }
        }

        // Hit pop — expanding white ring on streak break
        if (hitPop > 0) {
            int popRadius = (int) ((1f - hitPop) * 80);
            int popAlpha = (int) (hitPop * 200);
            g2.setColor(new Color(255, 255, 255, Math.min(popAlpha, 255)));
            g2.drawOval(cx - popRadius, cy - popRadius, popRadius * 2, popRadius * 2);
        }

        // Shield break flash
        if (shieldBreakEffect > 0.05f) {
            int flashR = (int) ((1f - shieldBreakEffect) * 60) + SIZE / 2;
            int flashAlpha = (int) (shieldBreakEffect * 180);
            g2.setColor(new Color(140, 220, 255, Math.min(flashAlpha, 255)));
            g2.drawOval(cx - flashR, cy - flashR, flashR * 2, flashR * 2);
        }

        // Particle aura — orbiting dots at high streak
        if (streakLevel > 0.3f) {
            float auraIntensity = (streakLevel - 0.3f) / 0.7f;
            int count = 3 + (int) (auraIntensity * 5);
            int auraAlpha = (int) (auraIntensity * 80);
            for (int i = 0; i < count; i++) {
                float a = auraPhase + i * (float) (Math.PI * 2 / count);
                float dist = 30 + (float) Math.sin(auraPhase * 1.5f + i) * 8;
                int px = cx + (int) (Math.cos(a) * dist);
                int py = cy + (int) (Math.sin(a) * dist);
                int dotSize = 2 + (int) (auraIntensity * 3);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                        Math.min(auraAlpha, 255)));
                g2.fillOval(px - dotSize / 2, py - dotSize / 2, dotSize, dotSize);
            }
        }

        // I-frame blink
        boolean visible = true;
        float iFrameAlpha = 1f;
        if (iFrames > 0 && !dashing) {
            visible = (iFrames / 3) % 2 == 0;
            iFrameAlpha = 0.5f + (float) Math.sin(iFrames * 0.5) * 0.3f;
        }

        // Shield bubble (drawn before player)
        if (shieldActive && visible) {
            float sPulse = (float) (Math.sin(shieldPulse) * 0.5 + 0.5);
            int shieldR = SIZE / 2 + 10 + (int) (sPulse * 3);
            int sAlpha = 25 + (int) (sPulse * 20);
            g2.setColor(new Color(140, 220, 255, sAlpha));
            g2.fillOval(cx - shieldR, cy - shieldR, shieldR * 2, shieldR * 2);
            g2.setColor(new Color(140, 220, 255, 50 + (int) (sPulse * 30)));
            g2.drawOval(cx - shieldR, cy - shieldR, shieldR * 2, shieldR * 2);
            // Inner ring
            int innerR = shieldR - 4;
            g2.setColor(new Color(200, 240, 255, 20 + (int) (sPulse * 15)));
            g2.drawOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
        }

        if (visible) {
            int skinShape = PlayerSkins.getSelectedShape();
            Color skinFill = PlayerSkins.getSelectedFill();
            Color skinGlow = PlayerSkins.getSelectedGlow();

            // Dash — bright afterglow during dash
            if (dashing) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                PlayerSkins.drawShape(g2, skinShape, ix - 8, iy - 8, SIZE + 16, R + 8,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
            }

            // Outer glow
            int glowAlpha = (int) ((30 + (int) (streakLevel * 60)) * iFrameAlpha);
            int glowSize = 5 + (int) (streakLevel * 6);
            Color glowCol = lerpColor(skinGlow, accent, streakLevel * 0.5f);
            PlayerSkins.drawShape(g2, skinShape, ix - glowSize, iy - glowSize,
                    SIZE + glowSize * 2, R + glowSize,
                    new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(),
                            Math.min(glowAlpha, 255)));

            // Inner glow
            int innerAlpha = (int) ((60 + (int) (streakLevel * 50)) * iFrameAlpha);
            PlayerSkins.drawShape(g2, skinShape, ix - 2, iy - 2, SIZE + 4, R + 2,
                    new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(),
                            Math.min(innerAlpha, 255)));

            // Main shape
            Color fill;
            if (dashing) {
                fill = lerpColor(skinFill, accent, 0.6f);
            } else if (iFrames > 0) {
                fill = lerpColor(skinFill, new Color(255, 255, 255), (float) Math.abs(Math.sin(iFrames * 0.4)) * 0.5f);
            } else {
                fill = lerpColor(skinFill, accent, streakLevel * 0.25f);
            }
            PlayerSkins.drawShape(g2, skinShape, ix, iy, SIZE, R, fill);
        }

        // Multiplier text above player
        if (getMultiplier() > 1) {
            g2.setFont(MULT_FONT);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    160 + (int) (streakLevel * 95)));
            String mult = "x" + getMultiplier();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(mult, cx - fm.stringWidth(mult) / 2, iy - 12);
        }
    }

    private Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
