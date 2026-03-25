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

    // Streak system
    private int streakTicks = 0;        // ticks without damage
    private float streakLevel = 0;      // 0-1 smoothed intensity
    private float hitPop = 0;           // flash on streak break
    private float auraPhase = 0;

    // Streak thresholds (in ticks, 60 = 1 second)
    private static final int STREAK_T1 = 180;   // 3s — glow starts
    private static final int STREAK_T2 = 480;   // 8s — trail intensifies
    private static final int STREAK_T3 = 900;   // 15s — full aura + multiplier
    private static final int STREAK_MAX = 1200;  // 20s — max intensity

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

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
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
        streakLevel += (targetStreak - streakLevel) * 0.04f;
        auraPhase += 0.06f + streakLevel * 0.04f;

        // Hit pop decay
        if (hitPop > 0.01f) hitPop *= 0.9f;
        else hitPop = 0;

        // Trail — more frequent and brighter at high streak
        int trailRate = streakLevel > 0.5f ? 2 : 3;
        float trailLife = 0.045f - streakLevel * 0.02f; // slower fade at high streak
        Color trailCol = lerpColor(new Color(200, 210, 220),
                GamePalette.accent(), streakLevel * 0.6f);

        if (++trailTick % trailRate == 0) {
            handler.addObject(new Trail(x, y, ID.Trail, trailCol, SIZE, SIZE, Math.max(trailLife, 0.015f), handler));
        }

        collision();
    }

    private boolean wasColliding = false;

    private void collision() {
        boolean colliding = false;
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject tempObject = handler.getObjects().get(i);
            if (tempObject.getId() == ID.BasicEnemy || tempObject.getId() == ID.FastEnemy
                    || tempObject.getId() == ID.SmartEnemy || tempObject.getId() == ID.HardEnemy
                    || tempObject.getId() == ID.EnemyBoss) {
                if (getBounds().intersects(tempObject.getBounds())) {
                    HUD.HEALTH -= 2;
                    colliding = true;
                }
            }
        }
        if (colliding && !wasColliding) {
            Game.triggerHit();
            // Break streak with visual pop
            if (streakTicks > STREAK_T1) {
                hitPop = 1f;
            }
            streakTicks = 0;
            streakLevel = 0;
        }
        wasColliding = colliding;
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;
        int cx = ix + SIZE / 2, cy = iy + SIZE / 2;

        Color accent = GamePalette.accent();

        // Hit pop — expanding white ring on streak break
        if (hitPop > 0) {
            int popRadius = (int) ((1f - hitPop) * 80);
            int popAlpha = (int) (hitPop * 200);
            g2.setColor(new Color(255, 255, 255, Math.min(popAlpha, 255)));
            g2.drawOval(cx - popRadius, cy - popRadius, popRadius * 2, popRadius * 2);
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

        // Outer glow — intensifies with streak
        int glowAlpha = 30 + (int) (streakLevel * 60);
        int glowSize = 5 + (int) (streakLevel * 6);
        Color glowCol = lerpColor(new Color(230, 234, 240), accent, streakLevel * 0.5f);
        g2.setColor(new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(),
                Math.min(glowAlpha, 255)));
        g2.fillRoundRect(ix - glowSize, iy - glowSize, SIZE + glowSize * 2, SIZE + glowSize * 2,
                R + glowSize, R + glowSize);

        // Inner glow
        int innerAlpha = 60 + (int) (streakLevel * 50);
        g2.setColor(new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(),
                Math.min(innerAlpha, 255)));
        g2.fillRoundRect(ix - 2, iy - 2, SIZE + 4, SIZE + 4, R + 2, R + 2);

        // Main shape — tints toward accent at high streak
        Color fill = lerpColor(FILL, accent, streakLevel * 0.25f);
        g2.setColor(fill);
        g2.fillRoundRect(ix, iy, SIZE, SIZE, R, R);

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
