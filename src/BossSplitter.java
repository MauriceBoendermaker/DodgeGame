import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Random;

/**
 * Splitter Boss — on defeat, splits into smaller copies that each split once more.
 * Generation 0 = main boss (large), gen 1 = halves, gen 2 = quarters (final).
 * The boss is defeated when all fragments are gone.
 * Bounces around the arena. Each generation is faster and smaller.
 */
public class BossSplitter extends GameObject {

    private static final int BASE_SIZE = 88;
    private static final int R = 14;

    private Handler handler;
    private Random r = new Random();
    private float pulsePhase;
    private final Rectangle boundsRect = new Rectangle();
    private static final Rectangle EMPTY_BOUNDS = new Rectangle(0, 0, 0, 0);

    private int generation; // 0 = main, 1 = half, 2 = quarter
    private int size;
    private float maxHp;
    private float hp;
    private boolean defeated = false;
    private int deathTimer = 0;
    private boolean hasSpawned = false; // prevent double-split

    // Entry animation (gen 0 only)
    private int entryTimer;
    private int warmupTimer = 30;

    // Colors — green/teal theme to distinguish from red EnemyBoss
    private static final Color FILL_G0 = new Color(72, 199, 142);
    private static final Color FILL_G1 = new Color(90, 210, 155);
    private static final Color FILL_G2 = new Color(110, 220, 170);
    private static final Color BAR_BG = new Color(22, 30, 44);
    private static final Color BAR_BORDER = new Color(40, 52, 70);
    private static final Font FONT_BOSS = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_PHASE = new Font("Arial", Font.BOLD, 11);

    public BossSplitter(int x, int y, ID id, Handler handler, int generation) {
        super(x, y, id);
        this.handler = handler;
        this.generation = generation;
        this.pulsePhase = r.nextFloat() * 6.28f;

        // Size shrinks per generation
        size = generation == 0 ? BASE_SIZE : generation == 1 ? BASE_SIZE / 2 : BASE_SIZE / 3;

        // HP per generation
        maxHp = generation == 0 ? 900 : generation == 1 ? 300 : 100;
        hp = maxHp;

        // Speed increases per generation
        float speed = generation == 0 ? 3f : generation == 1 ? 5f : 7f;
        velX = (r.nextBoolean() ? speed : -speed) * (0.8f + r.nextFloat() * 0.4f);
        velY = (r.nextBoolean() ? speed : -speed) * (0.8f + r.nextFloat() * 0.4f);

        entryTimer = generation == 0 ? 100 : 0;
        if (generation == 0) {
            velX = 0;
            velY = 2;
        }
    }

    public void setMaxHp(float hp) {
        // Scale all generations proportionally
        float ratio = hp / 900f;
        if (generation == 0) { this.maxHp = hp; }
        else if (generation == 1) { this.maxHp = 300 * ratio; }
        else { this.maxHp = 100 * ratio; }
        this.hp = this.maxHp;
    }

    public boolean isDefeated() {
        if (generation < 2) return false; // gen 0/1 split, never "fully" defeated
        return defeated && deathTimer > 30;
    }

    /** Check if this specific fragment is dead (used by Spawn to detect all fragments gone) */
    public boolean isFragmentDead() {
        return defeated && deathTimer > 30;
    }

    public int getGeneration() { return generation; }

    public float getHpPercent() { return hp / maxHp; }

    public Rectangle getBounds() {
        if (defeated || entryTimer > 0) return EMPTY_BOUNDS;
        boundsRect.setBounds((int) x, (int) y, size, size);
        return boundsRect;
    }

    public void tick() {
        if (defeated) {
            deathTimer++;
            if (!hasSpawned && generation < 2) {
                hasSpawned = true;
                split();
            }
            return;
        }

        pulsePhase += 0.06f;

        // Entry animation (gen 0 only)
        if (entryTimer > 0) {
            x += velX;
            y += velY;
            entryTimer--;
            if (entryTimer <= 0) { velY = 0; }
            return;
        }
        if (warmupTimer > 0) {
            warmupTimer--;
            if (warmupTimer <= 0 && generation == 0) {
                float speed = 3f;
                velX = (r.nextBoolean() ? speed : -speed);
                velY = (r.nextBoolean() ? speed : -speed) * (0.6f + r.nextFloat() * 0.4f);
            }
            return;
        }

        x += velX;
        y += velY;

        // HP drains over time
        float drainRate = 0.6f + (1f - hp / maxHp) * 0.3f;
        if (generation == 1) drainRate *= 1.3f;
        if (generation == 2) drainRate *= 1.8f;
        hp -= drainRate;

        if (hp <= 0) {
            hp = 0;
            defeated = true;
            if (generation == 2) {
                Game.triggerBulletCascade(x + size / 2, y + size / 2, handler);
            }
            return;
        }

        // Bounce off walls
        if (y <= 0 || y >= Game.HEIGHT - size) {
            Game.wallHit(x + size / 2, y <= 0 ? 0 : Game.HEIGHT, y <= 0 ? 0 : 1,
                    getFill().getRed(), getFill().getGreen(), getFill().getBlue());
            velY *= -1;
            if (y <= 0) y = 0;
            if (y >= Game.HEIGHT - size) y = Game.HEIGHT - size;
        }
        if (x <= 0 || x >= Game.WIDTH - size) {
            Game.wallHit(x <= 0 ? 0 : Game.WIDTH, y + size / 2, x <= 0 ? 2 : 3,
                    getFill().getRed(), getFill().getGreen(), getFill().getBlue());
            velX *= -1;
            if (x <= 0) x = 0;
            if (x >= Game.WIDTH - size) x = Game.WIDTH - size;
        }

        // Shoot occasional bullets (gen 0 and 1 only)
        if (generation <= 1 && r.nextInt(generation == 0 ? 80 : 120) == 0) {
            float angle = r.nextFloat() * (float) (Math.PI * 2);
            float bspeed = 3f + r.nextFloat() * 2f;
            handler.addObject(new EnemyBossBullet(
                    (int) (x + size / 2), (int) (y + size / 2), ID.BasicEnemy, handler,
                    (float) Math.cos(angle) * bspeed, (float) Math.sin(angle) * bspeed));
        }
    }

    private void split() {
        int nextGen = generation + 1;
        int nextSize = nextGen == 1 ? BASE_SIZE / 2 : BASE_SIZE / 3;
        int count = nextGen == 1 ? 2 : 3;

        float cx = x + size / 2;
        float cy = y + size / 2;

        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.PI * 2 * i / count) + r.nextFloat() * 0.5f;
            float offset = size * 0.4f;
            int sx = (int) (cx + Math.cos(angle) * offset - nextSize / 2);
            int sy = (int) (cy + Math.sin(angle) * offset - nextSize / 2);
            sx = Math.max(0, Math.min(Game.WIDTH - nextSize, sx));
            sy = Math.max(0, Math.min(Game.HEIGHT - nextSize, sy));

            BossSplitter fragment = new BossSplitter(sx, sy, ID.EnemyBoss, handler, nextGen);
            // Scale HP proportionally
            float ratio = maxHp / 900f;
            fragment.setMaxHp(nextGen == 1 ? 300 * ratio : 100 * ratio);
            handler.addObject(fragment);
        }
    }

    private Color getFill() {
        return generation == 0 ? FILL_G0 : generation == 1 ? FILL_G1 : FILL_G2;
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ix = (int) x, iy = (int) y;

        if (defeated) {
            float t = Math.min(deathTimer / 30f, 1f);
            int shrink = (int) (t * size / 2);
            int alpha = (int) ((1f - t) * 255);
            if (alpha > 0) {
                g2.setColor(new Color(255, 255, 255, alpha));
                g2.fillOval(ix + shrink, iy + shrink, size - shrink * 2, size - shrink * 2);
            }
            return;
        }

        Color fill = getFill();
        float pulse = (float) (Math.sin(pulsePhase) * 0.5 + 0.5);

        // Glow
        int glowSize = 6 + (int) (3 * pulse);
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 15 + (int) (12 * pulse)));
        g2.fillOval(ix - glowSize, iy - glowSize, size + glowSize * 2, size + glowSize * 2);

        // Main body — hexagonal shape
        int cx = ix + size / 2, cy = iy + size / 2;
        int hr = size / 2;
        int[] hx = new int[6], hy = new int[6];
        for (int i = 0; i < 6; i++) {
            float a = pulsePhase * 0.3f + i * (float) (Math.PI / 3);
            hx[i] = cx + (int) (Math.cos(a) * hr);
            hy[i] = cy + (int) (Math.sin(a) * hr);
        }
        g2.setColor(fill);
        g2.fillPolygon(hx, hy, 6);
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 100 + (int) (60 * pulse)));
        g2.drawPolygon(hx, hy, 6);

        // Inner split lines — hint at the splitting mechanic
        if (generation < 2) {
            g2.setColor(new Color(255, 255, 255, 30 + (int) (20 * pulse)));
            int lines = generation == 0 ? 2 : 3;
            for (int i = 0; i < lines; i++) {
                float a = pulsePhase * 0.2f + i * (float) (Math.PI / lines);
                int dx = (int) (Math.cos(a) * hr * 0.7f);
                int dy = (int) (Math.sin(a) * hr * 0.7f);
                g2.drawLine(cx - dx, cy - dy, cx + dx, cy + dy);
            }
        }

        // Gen 0 draws boss bar
        if (generation == 0) {
            renderBossBar(g2);
        }
    }

    private void renderBossBar(Graphics2D g) {
        int barW = 400;
        int barH = 10;
        int barX = (Game.WIDTH - barW) / 2;
        int barY = Game.HEIGHT - 40;

        // Aggregate HP across all living fragments
        float totalHp = 0, totalMax = 0;
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject obj = handler.getObjects().get(i);
            if (obj instanceof BossSplitter) {
                BossSplitter bs = (BossSplitter) obj;
                if (!bs.defeated) {
                    totalHp += bs.hp;
                    totalMax += bs.maxHp;
                }
            }
        }
        // Include self if still alive
        if (!defeated) {
            // Already counted in loop above
        }
        if (totalMax <= 0) return;
        float pct = totalHp / totalMax;

        g.setColor(BAR_BG);
        g.fillRoundRect(barX, barY, barW, barH, 5, 5);

        Color fill = getFill();
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
        g.drawString("SPLITTER", barX - fm.stringWidth("SPLITTER") - 10, barY + 9);

        g.setFont(FONT_PHASE);
        g.setColor(fill);
        // Count living fragments
        int fragments = 0;
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject obj = handler.getObjects().get(i);
            if (obj instanceof BossSplitter && !((BossSplitter) obj).defeated) fragments++;
        }
        g.drawString(fragments + " alive", barX + barW + 10, barY + 9);
    }
}
