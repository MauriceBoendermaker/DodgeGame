import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Random;

/**
 * Swarm Boss — relatively fragile but summons waves of mini-enemies.
 * Drifts slowly around the arena. Periodically spawns SwarmMinions that bounce
 * and shoot small bullets. The boss itself doesn't attack directly.
 * Defeat it fast before the minions overwhelm you.
 */
public class BossSwarm extends GameObject {

    private static final int SIZE = 72;

    private Handler handler;
    private Random r = new Random();
    private float pulsePhase = 0;
    private final Rectangle boundsRect = new Rectangle();
    private static final Rectangle EMPTY_BOUNDS = new Rectangle(0, 0, 0, 0);

    // Entry
    private int entryTimer = 100;
    private int warmupTimer = 30;

    // Health — drains faster than other bosses (fragile)
    private float maxHp = 800;
    private float hp;
    private boolean defeated = false;
    private int deathTimer = 0;

    // Summon system
    private int summonCooldown = 0;
    private int summonWave = 0;

    // Movement — slow drift
    private float driftAngle;

    // Colors — amber/orange theme
    private static final Color FILL_P1 = new Color(245, 180, 60);
    private static final Color FILL_P2 = new Color(240, 140, 50);
    private static final Color FILL_P3 = new Color(235, 100, 50);
    private static final Color BAR_BG = new Color(22, 30, 44);
    private static final Color BAR_BORDER = new Color(40, 52, 70);
    private static final Font FONT_BOSS = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_PHASE = new Font("Arial", Font.BOLD, 11);

    // Minion tracking
    private static final int MAX_MINIONS = 8;

    public BossSwarm(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        this.hp = maxHp;
        this.driftAngle = r.nextFloat() * (float) (Math.PI * 2);
        velX = 0;
        velY = 2;
    }

    public void setMaxHp(float hp) {
        this.maxHp = hp;
        this.hp = hp;
    }

    public boolean isDefeated() {
        return defeated && deathTimer > 60;
    }

    @Override
    public boolean takeDamage(float dmg) {
        if (defeated) return false;
        hp -= dmg;
        if (hp < 0) hp = 0;
        return true;
    }

    public float getHpPercent() { return hp / maxHp; }

    public int getPhase() {
        float pct = hp / maxHp;
        if (pct <= 0.25f) return 3;
        if (pct <= 0.55f) return 2;
        return 1;
    }

    public Rectangle getBounds() {
        if (defeated || entryTimer > 0) return EMPTY_BOUNDS;
        boundsRect.setBounds((int) x, (int) y, SIZE, SIZE);
        return boundsRect;
    }

    public void tick() {
        if (defeated) {
            deathTimer++;
            // Kill all minions when boss dies
            if (deathTimer == 1) {
                for (int i = handler.getObjects().size() - 1; i >= 0; i--) {
                    GameObject obj = handler.getObjects().get(i);
                    if (obj instanceof SwarmMinion) {
                        handler.removeObject(obj);
                    }
                }
                Game.triggerBulletCascade(x + SIZE / 2, y + SIZE / 2, handler);
            }
            return;
        }

        pulsePhase += 0.07f;

        // Entry animation
        if (entryTimer > 0) {
            x += velX;
            y += velY;
            entryTimer--;
            if (entryTimer <= 0) {
                velX = 0;
                velY = 0;
            }
            return;
        }
        if (warmupTimer > 0) {
            warmupTimer--;
            return;
        }

        // HP drains faster than other bosses
        float drainRate = 1.0f + (1f - hp / maxHp) * 0.5f;
        if (Game.combatMode) drainRate *= 0.35f;
        hp -= drainRate;

        if (hp <= 0) {
            hp = 0;
            defeated = true;
            return;
        }

        // Slow drift movement
        driftAngle += 0.01f + r.nextFloat() * 0.005f;
        float speed = 1.2f;
        x += Math.cos(driftAngle) * speed;
        y += Math.sin(driftAngle) * speed * 0.6f;

        // Keep in bounds with soft bounce
        float margin = 60;
        if (x < margin) { x = margin; driftAngle = (float) Math.PI - driftAngle; }
        if (x > Game.WIDTH - SIZE - margin) { x = Game.WIDTH - SIZE - margin; driftAngle = (float) Math.PI - driftAngle; }
        if (y < margin) { y = margin; driftAngle = -driftAngle; }
        if (y > Game.HEIGHT - SIZE - margin) { y = Game.HEIGHT - SIZE - margin; driftAngle = -driftAngle; }

        // Summon minions
        summonCooldown--;
        if (summonCooldown <= 0) {
            int phase = getPhase();
            int minionCount = countMinions();

            if (minionCount < MAX_MINIONS) {
                int toSpawn = phase == 3 ? 3 : phase == 2 ? 2 : 1;
                toSpawn = Math.min(toSpawn, MAX_MINIONS - minionCount);
                summonWave++;

                for (int i = 0; i < toSpawn; i++) {
                    float angle = (float) (Math.PI * 2 * i / toSpawn) + summonWave * 1.2f;
                    float dist = SIZE * 0.8f;
                    int sx = (int) (x + SIZE / 2 + Math.cos(angle) * dist);
                    int sy = (int) (y + SIZE / 2 + Math.sin(angle) * dist);
                    sx = Math.max(20, Math.min(Game.WIDTH - 40, sx));
                    sy = Math.max(20, Math.min(Game.HEIGHT - 40, sy));

                    boolean shoots = phase >= 2 || (summonWave % 3 == 0);
                    handler.addObject(new SwarmMinion(sx, sy, handler, shoots));
                }
            }

            summonCooldown = phase == 3 ? 100 : phase == 2 ? 140 : 180;
        }
    }

    private int countMinions() {
        int count = 0;
        for (int i = 0; i < handler.getObjects().size(); i++) {
            if (handler.getObjects().get(i) instanceof SwarmMinion) count++;
        }
        return count;
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ix = (int) x, iy = (int) y;

        if (defeated) {
            float t = Math.min(deathTimer / 60f, 1f);
            int shrink = (int) (t * SIZE / 2);
            int alpha = (int) ((1f - t) * 255);
            if (alpha > 0) {
                g2.setColor(new Color(255, 255, 255, alpha));
                g2.fillOval(ix + shrink, iy + shrink, SIZE - shrink * 2, SIZE - shrink * 2);
            }
            return;
        }

        Color fill = getPhase() == 3 ? FILL_P3 : getPhase() == 2 ? FILL_P2 : FILL_P1;
        float pulse = (float) (Math.sin(pulsePhase) * 0.5 + 0.5);
        int cx = ix + SIZE / 2, cy = iy + SIZE / 2;

        // Glow
        int glowSize = 10 + (int) (5 * pulse);
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 12 + (int) (10 * pulse)));
        g2.fillOval(cx - SIZE / 2 - glowSize, cy - SIZE / 2 - glowSize,
                SIZE + glowSize * 2, SIZE + glowSize * 2);

        // Main body — pentagon shape
        int hr = SIZE / 2;
        int[] hx = new int[5], hy = new int[5];
        for (int i = 0; i < 5; i++) {
            float a = pulsePhase * 0.2f + i * (float) (Math.PI * 2 / 5) - (float) (Math.PI / 2);
            hx[i] = cx + (int) (Math.cos(a) * hr);
            hy[i] = cy + (int) (Math.sin(a) * hr);
        }
        g2.setColor(fill);
        g2.fillPolygon(hx, hy, 5);
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 100 + (int) (60 * pulse)));
        g2.drawPolygon(hx, hy, 5);

        // Inner star pattern
        g2.setColor(new Color(255, 255, 255, 25 + (int) (15 * pulse)));
        int innerR = SIZE / 4;
        for (int i = 0; i < 5; i++) {
            float a1 = pulsePhase * 0.2f + i * (float) (Math.PI * 2 / 5) - (float) (Math.PI / 2);
            float a2 = pulsePhase * 0.2f + ((i + 2) % 5) * (float) (Math.PI * 2 / 5) - (float) (Math.PI / 2);
            g2.drawLine(
                    cx + (int) (Math.cos(a1) * innerR), cy + (int) (Math.sin(a1) * innerR),
                    cx + (int) (Math.cos(a2) * innerR), cy + (int) (Math.sin(a2) * innerR));
        }

        // Orbiting summon indicators
        int minionCount = countMinions();
        if (minionCount > 0) {
            g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 50));
            for (int i = 0; i < minionCount; i++) {
                float a = pulsePhase * 0.6f + i * (float) (Math.PI * 2 / minionCount);
                int dx = cx + (int) (Math.cos(a) * (SIZE / 2 + 12));
                int dy = cy + (int) (Math.sin(a) * (SIZE / 2 + 12));
                g2.fillOval(dx - 3, dy - 3, 6, 6);
            }
        }

        renderBossBar(g2, minionCount);
    }

    private void renderBossBar(Graphics2D g, int minionCount) {
        int barW = 400;
        int barH = 10;
        int barX = (Game.WIDTH - barW) / 2;
        int barY = Game.HEIGHT - 40;
        float pct = hp / maxHp;

        g.setColor(BAR_BG);
        g.fillRoundRect(barX, barY, barW, barH, 5, 5);

        Color fill = getPhase() == 3 ? FILL_P3 : getPhase() == 2 ? FILL_P2 : FILL_P1;
        int fillW = (int) (barW * pct);
        if (fillW > 0) {
            g.setColor(fill);
            g.fillRoundRect(barX, barY, fillW, barH, 5, 5);
        }

        g.setColor(BAR_BORDER);
        g.drawRoundRect(barX, barY, barW, barH, 5, 5);

        g.setFont(FONT_BOSS);
        g.setColor(PageRenderer.TEXT_SEC);
        FontMetrics fm = g.getFontMetrics();
        g.drawString("SWARM", barX - fm.stringWidth("SWARM") - 10, barY + 9);

        g.setFont(FONT_PHASE);
        g.setColor(fill);
        g.drawString(minionCount + " minions", barX + barW + 10, barY + 9);
    }
}
