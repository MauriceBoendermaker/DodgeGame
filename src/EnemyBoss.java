import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Random;

public class EnemyBoss extends GameObject {

    private static final int SIZE = 96;
    private static final int R = 16;

    private Handler handler;
    private Random r = new Random();
    private float pulsePhase = 0;

    // Entry animation — syncs with boss intro cinematic
    private int entryTimer = 100; // matches Game.bossIntroTimer crash point
    private int warmupTimer = 30;

    // Health — drains over time (survival-based), boss dies when empty
    private float maxHp;
    private float hp;
    private boolean defeated = false;
    private int deathTimer = 0;

    // Attack system
    private int attackCooldown = 0;
    private int attackPattern = 0;
    private int patternStep = 0;
    private int burstCount = 0;

    // Phase thresholds
    private static final float PHASE2_THRESHOLD = 0.6f;
    private static final float PHASE3_THRESHOLD = 0.25f;

    // Colors per phase
    private static final Color FILL_P1 = new Color(235, 87, 87);
    private static final Color FILL_P2 = new Color(220, 60, 120);
    private static final Color FILL_P3 = new Color(180, 40, 180);
    private static final Color BAR_BG = new Color(22, 30, 44);
    private static final Color BAR_BORDER = new Color(40, 52, 70);

    private static final Font FONT_BOSS = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_PHASE = new Font("Arial", Font.BOLD, 11);

    public EnemyBoss(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        velX = 0;
        velY = 2;

        // HP scales with game difficulty
        maxHp = 1200;
        hp = maxHp;
    }

    public void setMaxHp(float hp) {
        this.maxHp = hp;
        this.hp = hp;
    }

    public boolean isDefeated() {
        return defeated && deathTimer > 60;
    }

    public float getHpPercent() {
        return hp / maxHp;
    }

    public int getPhase() {
        float pct = hp / maxHp;
        if (pct <= PHASE3_THRESHOLD) return 3;
        if (pct <= PHASE2_THRESHOLD) return 2;
        return 1;
    }

    public Rectangle getBounds() {
        if (defeated || entryTimer > 0) return new Rectangle(0, 0, 0, 0);
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        if (defeated) {
            deathTimer++;
            return;
        }

        x += velX;
        y += velY;
        pulsePhase += 0.08f;

        // Entry animation
        if (entryTimer > 0) {
            entryTimer--;
            if (entryTimer <= 0) velY = 0;
            return;
        }
        if (warmupTimer > 0) {
            warmupTimer--;
            if (warmupTimer <= 0 && velX == 0) velX = 2;
            return;
        }

        // HP drains over time — survive to defeat the boss
        float drainRate = 0.8f + (1f - hp / maxHp) * 0.4f; // drains faster as HP lowers
        hp -= drainRate;

        if (hp <= 0) {
            hp = 0;
            defeated = true;
            // Queue cascading bullet explosions instead of instant clear
            Game.triggerBulletCascade(x + SIZE / 2, y + SIZE / 2, handler);
            return;
        }

        // Movement — speeds up in later phases
        float speedMult = getPhase() == 3 ? 1.5f : getPhase() == 2 ? 1.2f : 1f;
        if (velX > 0) velX += 0.005f * speedMult;
        else if (velX < 0) velX -= 0.005f * speedMult;
        velX = Game.clamp(velX, -10 * speedMult, 10 * speedMult);

        if (x <= 0 || x >= Game.WIDTH - SIZE) {
            Game.wallHit(x <= 0 ? 0 : Game.WIDTH, y + SIZE / 2, x <= 0 ? 2 : 3);
            velX *= -1;
        }

        // Attack patterns
        attackCooldown--;
        if (attackCooldown <= 0) {
            executeAttack();
        }
    }

    private void executeAttack() {
        int phase = getPhase();
        int cx = (int) x + SIZE / 2;
        int cy = (int) y + SIZE;

        switch (attackPattern) {
            case 0: // Spread shot
                int count = phase == 3 ? 9 : phase == 2 ? 7 : 5;
                float startAngle = -(count - 1) * 8f;
                for (int i = 0; i < count; i++) {
                    float angle = startAngle + i * 16f;
                    float vx = (float) Math.sin(Math.toRadians(angle)) * 4;
                    float vy = 4 + Math.abs(vx) * 0.3f;
                    handler.addObject(new EnemyBossBullet(cx, cy, ID.BasicEnemy, handler, vx, vy));
                }
                attackCooldown = phase == 3 ? 40 : phase == 2 ? 55 : 70;
                break;

            case 1: // Aimed burst at player
                GameObject player = findPlayer();
                if (player != null) {
                    burstCount++;
                    float dx = player.getX() + 24 - cx;
                    float dy = player.getY() + 24 - cy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist > 0) {
                        float speed = phase >= 2 ? 6 : 5;
                        // Slight spread on each burst shot
                        float spread = (burstCount - 2) * 6f;
                        float baseAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
                        float a = (float) Math.toRadians(baseAngle + spread);
                        handler.addObject(new EnemyBossBullet(cx, cy, ID.BasicEnemy, handler,
                                (float) Math.cos(a) * speed, (float) Math.sin(a) * speed));
                    }
                    if (burstCount >= (phase >= 2 ? 5 : 3)) {
                        burstCount = 0;
                        attackCooldown = phase == 3 ? 50 : 65;
                    } else {
                        attackCooldown = 8; // fast between burst shots
                    }
                } else {
                    attackCooldown = 30;
                }
                break;

            case 2: // Wave — alternating left-right streams
                patternStep++;
                boolean leftSide = patternStep % 2 == 0;
                float wx = leftSide ? (int) x + 10 : (int) x + SIZE - 10;
                handler.addObject(new EnemyBossBullet((int) wx, cy, ID.BasicEnemy, handler,
                        leftSide ? -2f : 2f, 5f));
                if (patternStep >= (phase == 3 ? 16 : phase == 2 ? 12 : 8)) {
                    patternStep = 0;
                    attackCooldown = phase == 3 ? 35 : 50;
                } else {
                    attackCooldown = 5;
                }
                break;

            case 3: // Ring burst (phase 2+)
                if (phase >= 2) {
                    int ringCount = phase == 3 ? 16 : 12;
                    for (int i = 0; i < ringCount; i++) {
                        float a = (float) (2 * Math.PI * i / ringCount) + pulsePhase;
                        float speed = 3.5f;
                        handler.addObject(new EnemyBossBullet(cx, cy, ID.BasicEnemy, handler,
                                (float) Math.cos(a) * speed, (float) Math.sin(a) * speed));
                    }
                    attackCooldown = phase == 3 ? 60 : 80;
                } else {
                    attackCooldown = 10; // skip to next pattern in phase 1
                }
                break;
        }

        // Cycle to next pattern when cooldown is set (not mid-burst)
        if (attackCooldown > 15) {
            attackPattern = (attackPattern + 1) % 4;
        }
    }

    private GameObject findPlayer() {
        for (int i = 0; i < handler.getObjects().size(); i++) {
            if (handler.getObjects().get(i).getId() == ID.Player)
                return handler.getObjects().get(i);
        }
        return null;
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ix = (int) x, iy = (int) y;

        if (defeated) {
            // Death flash — shrinking, fading
            float t = Math.min(deathTimer / 60f, 1f);
            int shrink = (int) (t * SIZE / 2);
            int alpha = (int) ((1f - t) * 255);
            if (alpha > 0) {
                g2.setColor(new Color(255, 255, 255, alpha));
                g2.fillRoundRect(ix + shrink, iy + shrink, SIZE - shrink * 2, SIZE - shrink * 2, R, R);
            }
            return;
        }

        // Phase-based color
        Color fill = getPhase() == 3 ? FILL_P3 : getPhase() == 2 ? FILL_P2 : FILL_P1;

        float pulse = (float) (Math.sin(pulsePhase) * 0.5 + 0.5);
        int glowSize = 8 + (int) (4 * pulse);
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 15 + (int) (15 * pulse)));
        g2.fillRoundRect(ix - glowSize, iy - glowSize, SIZE + glowSize * 2, SIZE + glowSize * 2, R + glowSize, R + glowSize);

        g2.setColor(fill);
        g2.fillRoundRect(ix, iy, SIZE, SIZE, R, R);

        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 100 + (int) (80 * pulse)));
        g2.drawRoundRect(ix, iy, SIZE, SIZE, R, R);

        // Rotating inner geometry — two overlapping squares
        int bcx = ix + SIZE / 2, bcy = iy + SIZE / 2;
        int innerR = SIZE / 3;
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 50 + (int) (30 * pulse)));
        drawRotatedSquare(g2, bcx, bcy, innerR, pulsePhase * 0.6f);
        drawRotatedSquare(g2, bcx, bcy, innerR - 6, -pulsePhase * 0.4f);

        // Orbiting dots
        int dotCount = 4 + getPhase();
        int orbitR = SIZE / 2 - 8;
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 80));
        for (int i = 0; i < dotCount; i++) {
            float a = pulsePhase * 0.5f + i * (float) (Math.PI * 2 / dotCount);
            int dx = bcx + (int) (Math.cos(a) * orbitR);
            int dy = bcy + (int) (Math.sin(a) * orbitR);
            g2.fillOval(dx - 2, dy - 2, 4, 4);
        }

        // Boss health bar at top of screen
        renderBossBar(g2);
    }

    private void drawRotatedSquare(Graphics2D g, int cx, int cy, int r, float angle) {
        int[] xp = new int[4];
        int[] yp = new int[4];
        for (int i = 0; i < 4; i++) {
            float a = angle + i * (float) (Math.PI / 2);
            xp[i] = cx + (int) (Math.cos(a) * r);
            yp[i] = cy + (int) (Math.sin(a) * r);
        }
        g.drawPolygon(xp, yp, 4);
    }

    private void renderBossBar(Graphics2D g) {
        int barW = 400;
        int barH = 10;
        int barX = (Game.WIDTH - barW) / 2;
        int barY = Game.HEIGHT - 40;
        float pct = hp / maxHp;

        // Background
        g.setColor(BAR_BG);
        g.fillRoundRect(barX, barY, barW, barH, 5, 5);

        // Fill — color changes with phase
        Color fill = getPhase() == 3 ? FILL_P3 : getPhase() == 2 ? FILL_P2 : FILL_P1;
        int fillW = (int) (barW * pct);
        if (fillW > 0) {
            g.setColor(fill);
            g.fillRoundRect(barX, barY, fillW, barH, 5, 5);
        }

        // Border
        g.setColor(BAR_BORDER);
        g.drawRoundRect(barX, barY, barW, barH, 5, 5);

        // Label
        g.setFont(FONT_BOSS);
        g.setColor(PageRenderer.TEXT_SEC);
        FontMetrics fm = g.getFontMetrics();
        String label = "BOSS";
        g.drawString(label, barX - fm.stringWidth(label) - 10, barY + 9);

        // Phase indicator
        g.setFont(FONT_PHASE);
        g.setColor(fill);
        String phase = "Phase " + getPhase();
        fm = g.getFontMetrics();
        g.drawString(phase, barX + barW + 10, barY + 9);
    }
}
