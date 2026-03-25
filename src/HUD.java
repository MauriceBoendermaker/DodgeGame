import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class HUD {

    public int bounds = 0;
    public static float HEALTH = 100;

    private int score = 0;
    private int level = 1;
    private int points = 0;

    // Run stats
    private int ticksSurvived = 0;
    private int healthUpgrades = 0;
    private int speedUpgrades = 0;
    private int refills = 0;

    public static final int MAX_TIER = 10;

    private static final Font FONT_SCORE = new Font("Arial", Font.BOLD, 32);
    private static final Font FONT_LABEL = new Font("Arial", Font.BOLD, 13);
    private static final Font FONT_STAT = new Font("Arial", Font.PLAIN, 16);
    private static final Font FONT_LEVEL_BG = new Font("Arial", Font.BOLD, 600);
    private static final Font FONT_TIER = new Font("Arial", Font.BOLD, 10);
    private static final Font FONT_LEVEL_UP = new Font("Arial", Font.BOLD, 28);

    // Colors
    private static final Color BAR_BG = new Color(22, 30, 44);
    private static final Color BAR_BORDER = new Color(40, 52, 70);
    private static final Color TEXT = new Color(230, 234, 240);
    private static final Color TEXT_DIM = new Color(100, 112, 128);
    // ACCENT is now dynamic — use GamePalette.accent() for in-game elements
    private static final Color HEALTH_HIGH = new Color(72, 199, 142);
    private static final Color HEALTH_MID = new Color(245, 195, 68);
    private static final Color HEALTH_LOW = new Color(235, 87, 87);
    private static final Color TIER_HEALTH = new Color(72, 199, 142);
    private static final Color TIER_SPEED = new Color(100, 180, 220);
    private static final Color TIER_REFILL = new Color(245, 195, 68);
    private static final Color TIER_BG = new Color(30, 38, 52);

    // Level-up announcement
    private float levelUpBanner = 0;
    private int announceTimer = 0;
    private String announceText = "";
    private String announceSubtext = "";
    private boolean isWaveAnnounce = false;
    private int waveCount = 0;
    private static final Font FONT_ANNOUNCE = new Font("Arial", Font.BOLD, 64);
    private static final Font FONT_ANNOUNCE_SUB = new Font("Arial", Font.BOLD, 20);

    // Score milestone pulse
    private static final int MILESTONE_INTERVAL = 2500;
    private float scorePulse = 0;
    private int lastMilestone = 0;
    private static final Font FONT_SCORE_BIG = new Font("Arial", Font.BOLD, 42);
    private static final Color GOLD = new Color(255, 210, 80);

    public void tick() {
        float maxHealth = 100 + (bounds / 2);
        HEALTH = Game.clamp(HEALTH, 0, maxHealth);

        // Score multiplied by streak
        int mult = getPlayerMultiplier();
        score += mult;
        points += mult;
        ticksSurvived++;

        if (levelUpBanner > 0.01f) levelUpBanner *= 0.96f;
        else levelUpBanner = 0;
        if (announceTimer > 0) announceTimer--;

        // Score milestone check
        int currentMilestone = score / MILESTONE_INTERVAL;
        if (currentMilestone > lastMilestone && lastMilestone >= 0) {
            scorePulse = 1f;
        }
        lastMilestone = currentMilestone;
        if (scorePulse > 0.01f) scorePulse *= 0.92f;
        else scorePulse = 0;
    }

    public void triggerLevelUpBanner() {
        levelUpBanner = 1f;

        // Wave = group of 15 levels. Wave 1 = levels 1-15, Wave 2 = 16-30, etc.
        int newWave = ((level - 1) / 15) + 1;
        boolean isNewWave = newWave > waveCount;
        waveCount = newWave;

        int levelInWave = ((level - 1) % 15) + 1;

        if (isNewWave) {
            isWaveAnnounce = true;
            announceText = "WAVE " + waveCount;
            announceSubtext = waveCount == 1 ? "Good luck" : "Boss defeated - next wave";
            announceTimer = 110;
        } else {
            isWaveAnnounce = false;
            announceText = "LEVEL " + levelInWave;
            announceSubtext = "";
            announceTimer = 90;
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Subtle top gradient overlay for readability
        g2.setPaint(new GradientPaint(0, 0, new Color(10, 12, 18, 160), 0, 90, new Color(10, 12, 18, 0)));
        g2.fillRect(0, 0, Game.WIDTH, 90);

        // Health bar
        float maxHealth = 100 + (bounds / 2);
        float healthPct = HEALTH / maxHealth;
        int barW = 200 + bounds;
        int barH = 14;
        int barX = 24;
        int barY = 20;

        g2.setColor(BAR_BG);
        g2.fillRoundRect(barX, barY, barW, barH, 7, 7);

        Color healthColor = getHealthColor(healthPct);
        int fillW = (int) (barW * healthPct);
        if (fillW > 0) {
            g2.setColor(healthColor);
            g2.fillRoundRect(barX, barY, fillW, barH, 7, 7);
        }

        g2.setColor(BAR_BORDER);
        g2.drawRoundRect(barX, barY, barW, barH, 7, 7);

        g2.setFont(FONT_LABEL);
        g2.setColor(new Color(255, 255, 255, 180));
        String hpText = (int) HEALTH + " / " + (int) maxHealth;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hpText, barX + (barW - fm.stringWidth(hpText)) / 2, barY + 11);

        // Stats below health bar
        g2.setFont(FONT_STAT);
        g2.setColor(TEXT_DIM);
        g2.drawString("LVL " + level, barX, barY + 34);
        g2.drawString("PTS " + points, barX + 80, barY + 34);

        // Upgrade tiers — right side of top bar
        int tierX = Game.WIDTH - 220;
        int tierY = 16;
        drawTierBar(g2, tierX, tierY, "HP", healthUpgrades, TIER_HEALTH);
        drawTierBar(g2, tierX, tierY + 18, "SPD", speedUpgrades, TIER_SPEED);
        drawTierBar(g2, tierX, tierY + 36, "REF", refills, TIER_REFILL);

        // Score — top center with milestone pulse
        int scoreCX = Game.WIDTH / 2;
        int scoreY = 42;

        // Radial pulse ring on milestone
        if (scorePulse > 0.05f) {
            int ringRadius = (int) ((1f - scorePulse) * 120);
            int ringAlpha = (int) (scorePulse * 120);
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), Math.min(ringAlpha, 255)));
            g2.drawOval(scoreCX - ringRadius, scoreY - 15 - ringRadius, ringRadius * 2, ringRadius * 2);
            // Second ring, slightly delayed
            int ring2 = (int) ((1f - scorePulse * 0.8f) * 80);
            int ring2Alpha = (int) (scorePulse * 60);
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), Math.min(ring2Alpha, 255)));
            g2.drawOval(scoreCX - ring2, scoreY - 15 - ring2, ring2 * 2, ring2 * 2);
        }

        // Score number — bumps size and flashes gold on milestone
        Font scoreFont = scorePulse > 0.1f ? FONT_SCORE_BIG : FONT_SCORE;
        Color scoreColor = scorePulse > 0.1f
                ? lerpColor(TEXT, GOLD, scorePulse)
                : TEXT;
        g2.setFont(scoreFont);
        g2.setColor(scoreColor);
        String scoreStr = String.valueOf(score);
        fm = g2.getFontMetrics();
        g2.drawString(scoreStr, (Game.WIDTH - fm.stringWidth(scoreStr)) / 2, scoreY);

        // Score label
        g2.setFont(FONT_LABEL);
        Color labelColor = scorePulse > 0.1f
                ? lerpColor(TEXT_DIM, GOLD, scorePulse * 0.5f)
                : TEXT_DIM;
        g2.setColor(labelColor);
        String scoreLabel = "SCORE";
        fm = g2.getFontMetrics();
        g2.drawString(scoreLabel, (Game.WIDTH - fm.stringWidth(scoreLabel)) / 2, 18);

        // Large level watermark in background — flashes on level-up
        g2.setFont(FONT_LEVEL_BG);
        int watermarkAlpha = 20 + (int) (levelUpBanner * 60);
        g2.setColor(GamePalette.accent(Math.min(watermarkAlpha, 255)));
        String levelStr = level <= 9 ? "0" + level : "" + level;
        fm = g2.getFontMetrics();
        g2.drawString(levelStr, (Game.WIDTH - fm.stringWidth(levelStr)) / 2, Game.HEIGHT - 80);

        // Level/Wave announcement — slam in, hold, fade out
        if (announceTimer > 0 && announceText.length() > 0) {
            int total = isWaveAnnounce ? 110 : 90;
            float t = 1f - (announceTimer / (float) total); // 0 at start, 1 at end

            // Phase: 0-0.15 slam in, 0.15-0.7 hold, 0.7-1.0 fade out
            float alpha;
            float scaleF;
            float offsetY;

            if (t < 0.15f) {
                // Slam in — scale from 2x to 1x, fade in fast
                float p = t / 0.15f;
                scaleF = 2f - p; // 2.0 -> 1.0
                alpha = p;
                offsetY = 0;
            } else if (t < 0.7f) {
                // Hold
                scaleF = 1f;
                alpha = 1f;
                offsetY = 0;
            } else {
                // Fade out + drift up
                float p = (t - 0.7f) / 0.3f;
                scaleF = 1f;
                alpha = 1f - p;
                offsetY = -p * 30;
            }

            int cx = Game.WIDTH / 2;
            int cy = Game.HEIGHT / 2 - 20 + (int) offsetY;
            int a = (int) (alpha * 255);

            // Draw scaled text
            Color textColor = isWaveAnnounce
                    ? new Color(255, 210, 80, Math.min(a, 255))
                    : GamePalette.accent(Math.min(a, 255));

            // Main text
            int fontSize = (int) (64 * scaleF);
            if (fontSize > 8) {
                Font scaledFont = FONT_ANNOUNCE.deriveFont((float) fontSize);
                g2.setFont(scaledFont);
                g2.setColor(textColor);
                fm = g2.getFontMetrics();
                g2.drawString(announceText, cx - fm.stringWidth(announceText) / 2, cy);
            }

            // Subtext (wave only)
            if (isWaveAnnounce && announceSubtext.length() > 0 && t > 0.1f) {
                float subAlpha = Math.min((t - 0.1f) / 0.15f, 1f) * alpha;
                g2.setFont(FONT_ANNOUNCE_SUB);
                g2.setColor(new Color(200, 200, 200, (int) (subAlpha * 200)));
                fm = g2.getFontMetrics();
                g2.drawString(announceSubtext, cx - fm.stringWidth(announceSubtext) / 2, cy + 35 + (int) offsetY);
            }

            // Accent line under text — scales in then out
            if (t > 0.1f && t < 0.85f) {
                float lineP = Math.min((t - 0.1f) / 0.2f, 1f);
                if (t > 0.7f) lineP *= alpha;
                int lineW = (int) (200 * lineP);
                g2.setColor(isWaveAnnounce
                        ? new Color(255, 210, 80, (int) (lineP * 120))
                        : GamePalette.accent((int) (lineP * 120)));
                g2.fillRect(cx - lineW / 2, cy + 8 + (int) offsetY, lineW, 2);
            }
        }

        // Level progress bar — bottom of screen
        if (!Game.isBossActive()) {
            float progress = Game.getLevelProgress();
            int progH = 4;
            int progY = Game.HEIGHT - progH;

            // Background
            g2.setColor(BAR_BG);
            g2.fillRect(0, progY, Game.WIDTH, progH);

            // Fill
            int progFillW = (int) (Game.WIDTH * progress);
            if (progFillW > 0) {
                // Flash brighter near completion
                Color barColor = progress > 0.85f
                        ? lerpColor(GamePalette.accent(), GOLD, (progress - 0.85f) / 0.15f)
                        : GamePalette.accent();
                g2.setColor(barColor);
                g2.fillRect(0, progY, progFillW, progH);
            }

            // Completion flash — the level-up banner flash handles the burst
            if (levelUpBanner > 0.5f) {
                g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(),
                        (int) (levelUpBanner * 200)));
                g2.fillRect(0, progY, Game.WIDTH, progH);
            }
        }
    }

    private void drawTierBar(Graphics2D g, int x, int y, String label, int tier, Color color) {
        g.setFont(FONT_TIER);
        g.setColor(TEXT_DIM);
        g.drawString(label, x, y + 9);

        int dotX = x + 30;
        int dotSize = 8;
        int dotGap = 4;
        for (int i = 0; i < MAX_TIER; i++) {
            if (i < tier) {
                g.setColor(color);
                g.fillRoundRect(dotX + i * (dotSize + dotGap), y + 1, dotSize, dotSize, 3, 3);
            } else {
                g.setColor(TIER_BG);
                g.fillRoundRect(dotX + i * (dotSize + dotGap), y + 1, dotSize, dotSize, 3, 3);
            }
        }

        if (tier >= MAX_TIER) {
            g.setFont(FONT_TIER);
            g.setColor(color);
            g.drawString("MAX", dotX + MAX_TIER * (dotSize + dotGap) + 4, y + 9);
        }
    }

    private Color getHealthColor(float pct) {
        if (pct > 0.5f) {
            float t = (pct - 0.5f) * 2f;
            return lerpColor(HEALTH_MID, HEALTH_HIGH, t);
        } else {
            float t = pct * 2f;
            return lerpColor(HEALTH_LOW, HEALTH_MID, t);
        }
    }

    private Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private int getPlayerMultiplier() {
        // Find player in handler to read multiplier
        // This is called from tick which is in the game loop, so handler is accessible via Game
        // For simplicity, we'll make this work through a static reference
        return Game.getPlayerMultiplier();
    }

    public void setScore(int score) { this.score = score; }
    public int getScore() { return score; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    // Stats tracking
    public int getTicksSurvived() { return ticksSurvived; }
    public int getHealthUpgrades() { return healthUpgrades; }
    public int getSpeedUpgrades() { return speedUpgrades; }
    public int getRefills() { return refills; }
    public void addHealthUpgrade() { healthUpgrades++; }
    public void addSpeedUpgrade() { speedUpgrades++; }
    public void addRefill() { refills++; }
    public int getTotalUpgrades() { return healthUpgrades + speedUpgrades + refills; }

    public String getTimeSurvived() {
        int totalSec = ticksSurvived / 60;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        if (min > 0) return min + "m " + sec + "s";
        return sec + "s";
    }

    public void resetStats() {
        ticksSurvived = 0;
        healthUpgrades = 0;
        speedUpgrades = 0;
        refills = 0;
        levelUpBanner = 0;
        scorePulse = 0;
        lastMilestone = 0;
        announceTimer = 0;
        waveCount = 0;
    }
}
