import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.Random;

/**
 * Laser Boss — hovers in the center of the arena, rotating laser beam patterns.
 * Fires sweeping beams that the player must dodge by reading the telegraph and moving.
 * The beams are rendered as thick lines with glow. Multiple patterns rotate through phases.
 */
public class BossLaser extends GameObject {

    private static final int SIZE = 80;
    private static final int R = 40; // circular boss

    private Handler handler;
    private Random r = new Random();
    private float pulsePhase = 0;
    private final Rectangle boundsRect = new Rectangle();
    private static final Rectangle EMPTY_BOUNDS = new Rectangle(0, 0, 0, 0);

    // Entry
    private int entryTimer = 100;
    private int warmupTimer = 40;

    // Health
    private float maxHp = 1400;
    private float hp;
    private boolean defeated = false;
    private int deathTimer = 0;

    // Laser system
    private int beamCount = 2;       // active beams
    private float beamAngle = 0;     // current rotation angle
    private float beamSpeed = 0.012f; // radians per tick
    private int patternTimer = 0;
    private int pattern = 0;         // 0=sweep, 1=cross, 2=spiral
    private float telegraphAlpha = 0;// flashes before beam activates
    private boolean beamsActive = false;
    private int telegraphTimer = 0;
    private static final int TELEGRAPH_DURATION = 60;
    private static final int PATTERN_DURATION = 300;

    // Beam damage — dealt through collision rectangles
    private static final float BEAM_LENGTH = 900;
    private static final float BEAM_WIDTH = 12;

    // Phase thresholds
    private static final float PHASE2 = 0.55f;
    private static final float PHASE3 = 0.2f;

    // Colors — electric blue theme
    private static final Color FILL = new Color(80, 140, 255);
    private static final Color FILL_P2 = new Color(120, 100, 255);
    private static final Color FILL_P3 = new Color(180, 80, 255);
    private static final Color BEAM_COLOR = new Color(80, 160, 255);
    private static final Color BEAM_GLOW = new Color(80, 160, 255, 30);
    private static final Color TELEGRAPH_COLOR = new Color(80, 160, 255, 40);
    private static final Color BAR_BG = new Color(22, 30, 44);
    private static final Color BAR_BORDER = new Color(40, 52, 70);
    private static final Font FONT_BOSS = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_PHASE = new Font("Arial", Font.BOLD, 11);

    public BossLaser(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        this.hp = maxHp;
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
        if (pct <= PHASE3) return 3;
        if (pct <= PHASE2) return 2;
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
            return;
        }

        pulsePhase += 0.06f;

        // Entry — slide to center
        if (entryTimer > 0) {
            x += velX;
            y += velY;
            entryTimer--;
            if (entryTimer <= 0) {
                velX = 0;
                velY = 0;
                // Snap to center
                x = Game.WIDTH / 2 - SIZE / 2;
                y = Game.HEIGHT / 2 - SIZE / 2;
            }
            return;
        }
        if (warmupTimer > 0) {
            warmupTimer--;
            if (warmupTimer <= 0) {
                startNewPattern();
            }
            return;
        }

        // HP drains over time
        float drainRate = 0.7f + (1f - hp / maxHp) * 0.35f;
        if (Game.combatMode) drainRate *= 0.35f;
        hp -= drainRate;

        if (hp <= 0) {
            hp = 0;
            defeated = true;
            Game.triggerBulletCascade(x + SIZE / 2, y + SIZE / 2, handler);
            return;
        }

        // Slight hovering movement
        float hoverX = (float) Math.sin(pulsePhase * 0.4f) * 0.3f;
        float hoverY = (float) Math.cos(pulsePhase * 0.3f) * 0.2f;
        x += hoverX;
        y += hoverY;

        // Telegraph phase — warn player before beams activate
        if (telegraphTimer > 0) {
            telegraphTimer--;
            telegraphAlpha = (float) Math.sin(telegraphTimer * 0.15f) * 0.5f + 0.5f;
            if (telegraphTimer <= 0) {
                beamsActive = true;
            }
            return;
        }

        // Pattern execution
        if (beamsActive) {
            int phase = getPhase();
            float speedMult = phase == 3 ? 1.6f : phase == 2 ? 1.3f : 1f;

            switch (pattern) {
                case 0: // Steady sweep
                    beamAngle += beamSpeed * speedMult;
                    break;
                case 1: // Cross — 4 beams, slower rotation
                    beamAngle += beamSpeed * 0.7f * speedMult;
                    break;
                case 2: // Spiral — accelerating rotation
                    float accel = 1f + patternTimer * 0.002f;
                    beamAngle += beamSpeed * accel * speedMult;
                    break;
            }

            // Check beam collision with player
            checkBeamCollision();

            patternTimer++;
            if (patternTimer >= PATTERN_DURATION) {
                beamsActive = false;
                startNewPattern();
            }

            // Occasional bullet spray between beam sweeps (phase 2+)
            if (phase >= 2 && patternTimer % (phase == 3 ? 40 : 60) == 0) {
                int count = phase == 3 ? 6 : 4;
                for (int i = 0; i < count; i++) {
                    float a = (float) (Math.PI * 2 * i / count) + pulsePhase;
                    handler.addObject(new EnemyBossBullet(
                            (int) (x + SIZE / 2), (int) (y + SIZE / 2), ID.BasicEnemy, handler,
                            (float) Math.cos(a) * 3f, (float) Math.sin(a) * 3f));
                }
            }
        }
    }

    private void startNewPattern() {
        pattern = (pattern + 1) % 3;
        patternTimer = 0;

        int phase = getPhase();
        switch (pattern) {
            case 0: beamCount = phase >= 2 ? 3 : 2; break;
            case 1: beamCount = 4; break;
            case 2: beamCount = phase == 3 ? 3 : 2; break;
        }
        beamSpeed = 0.012f + phase * 0.003f;
        telegraphTimer = TELEGRAPH_DURATION;
        beamsActive = false;
    }

    private void checkBeamCollision() {
        GameObject player = findPlayer();
        if (player == null) return;

        float cx = x + SIZE / 2;
        float cy = y + SIZE / 2;
        float px = player.getX() + 24;
        float py = player.getY() + 24;

        for (int i = 0; i < beamCount; i++) {
            float angle = beamAngle + i * (float) (Math.PI * 2 / beamCount);

            // Point-to-line distance check
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);

            // Project player position onto beam line
            float relX = px - cx;
            float relY = py - cy;
            float dot = relX * dx + relY * dy;

            if (dot > 30 && dot < BEAM_LENGTH) { // beyond the boss body, within beam range
                float perpDist = Math.abs(relX * dy - relY * dx);
                if (perpDist < BEAM_WIDTH + 12) { // 12 = player half-size
                    // Beam hit — damage is handled through boss body collision in Player
                    // Create a temporary bullet at player position to trigger hit
                    handler.addObject(new EnemyBossBullet((int) px - 4, (int) py - 4,
                            ID.BasicEnemy, handler, 0, 0));
                }
            }
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
            float t = Math.min(deathTimer / 60f, 1f);
            int shrink = (int) (t * SIZE / 2);
            int alpha = (int) ((1f - t) * 255);
            if (alpha > 0) {
                g2.setColor(new Color(255, 255, 255, alpha));
                g2.fillOval(ix + shrink, iy + shrink, SIZE - shrink * 2, SIZE - shrink * 2);
            }
            return;
        }

        Color fill = getPhase() == 3 ? FILL_P3 : getPhase() == 2 ? FILL_P2 : FILL;
        float pulse = (float) (Math.sin(pulsePhase) * 0.5 + 0.5);
        int cx = ix + SIZE / 2, cy = iy + SIZE / 2;

        // Draw beams (behind boss)
        if (beamsActive || telegraphTimer > 0) {
            renderBeams(g2, cx, cy, fill);
        }

        // Glow
        int glowSize = 8 + (int) (4 * pulse);
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 12 + (int) (10 * pulse)));
        g2.fillOval(cx - SIZE / 2 - glowSize, cy - SIZE / 2 - glowSize,
                SIZE + glowSize * 2, SIZE + glowSize * 2);

        // Main body — circle with inner rings
        g2.setColor(fill);
        g2.fillOval(ix, iy, SIZE, SIZE);
        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 80 + (int) (60 * pulse)));
        g2.drawOval(ix, iy, SIZE, SIZE);

        // Inner concentric rings
        g2.setColor(new Color(255, 255, 255, 20 + (int) (15 * pulse)));
        g2.drawOval(cx - SIZE / 4, cy - SIZE / 4, SIZE / 2, SIZE / 2);
        g2.drawOval(cx - SIZE / 6, cy - SIZE / 6, SIZE / 3, SIZE / 3);

        // Rotating beam origin indicators
        for (int i = 0; i < beamCount; i++) {
            float angle = beamAngle + i * (float) (Math.PI * 2 / beamCount);
            int dotX = cx + (int) (Math.cos(angle) * (SIZE / 2 - 6));
            int dotY = cy + (int) (Math.sin(angle) * (SIZE / 2 - 6));
            g2.setColor(new Color(255, 255, 255, 120 + (int) (80 * pulse)));
            g2.fillOval(dotX - 3, dotY - 3, 6, 6);
        }

        renderBossBar(g2);
    }

    private void renderBeams(Graphics2D g, int cx, int cy, Color fill) {
        Stroke oldStroke = g.getStroke();

        for (int i = 0; i < beamCount; i++) {
            float angle = beamAngle + i * (float) (Math.PI * 2 / beamCount);
            int endX = cx + (int) (Math.cos(angle) * BEAM_LENGTH);
            int endY = cy + (int) (Math.sin(angle) * BEAM_LENGTH);

            if (beamsActive) {
                // Wide glow
                g.setStroke(new BasicStroke(BEAM_WIDTH * 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(BEAM_GLOW);
                g.drawLine(cx, cy, endX, endY);

                // Core beam
                g.setStroke(new BasicStroke(BEAM_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 180));
                g.drawLine(cx, cy, endX, endY);

                // Bright center
                g.setStroke(new BasicStroke(BEAM_WIDTH / 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 255, 255, 160));
                g.drawLine(cx, cy, endX, endY);
            } else {
                // Telegraph — dashed thin line
                int alpha = (int) (telegraphAlpha * 60);
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10, new float[]{10, 10}, pulsePhase * 20));
                g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), alpha));
                g.drawLine(cx, cy, endX, endY);
            }
        }

        g.setStroke(oldStroke);
    }

    private void renderBossBar(Graphics2D g) {
        int barW = 400;
        int barH = 10;
        int barX = (Game.WIDTH - barW) / 2;
        int barY = Game.HEIGHT - 40;
        float pct = hp / maxHp;

        g.setColor(BAR_BG);
        g.fillRoundRect(barX, barY, barW, barH, 5, 5);

        Color fill = getPhase() == 3 ? FILL_P3 : getPhase() == 2 ? FILL_P2 : FILL;
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
        g.drawString("LASER", barX - fm.stringWidth("LASER") - 10, barY + 9);

        g.setFont(FONT_PHASE);
        g.setColor(fill);
        g.drawString("Phase " + getPhase(), barX + barW + 10, barY + 9);
    }
}
