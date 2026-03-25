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

    // Level-up banner
    private float levelUpBanner = 0;

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
    }

    public void triggerLevelUpBanner() {
        levelUpBanner = 1f;
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

        // Score — top center
        g2.setFont(FONT_SCORE);
        g2.setColor(TEXT);
        String scoreStr = String.valueOf(score);
        fm = g2.getFontMetrics();
        g2.drawString(scoreStr, (Game.WIDTH - fm.stringWidth(scoreStr)) / 2, 42);
        g2.setFont(FONT_LABEL);
        g2.setColor(TEXT_DIM);
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

        // Level-up banner
        if (levelUpBanner > 0.05f) {
            g2.setFont(FONT_LEVEL_UP);
            int alpha = (int) (levelUpBanner * 255);
            g2.setColor(GamePalette.accent(alpha));
            String lvlUp = "LEVEL " + level;
            fm = g2.getFontMetrics();
            int bannerY = Game.HEIGHT / 2 - 30 - (int) ((1f - levelUpBanner) * 20);
            g2.drawString(lvlUp, (Game.WIDTH - fm.stringWidth(lvlUp)) / 2, bannerY);
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
    }
}
