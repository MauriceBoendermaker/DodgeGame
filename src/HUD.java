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

    // Health bar crack/shatter
    private int frameTick = 0;
    private static final java.util.Random crackRng = new java.util.Random(42); // fixed seed for consistent cracks

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
    private int waveCount = 1;
    private int lastBossLevel = 0; // tracks last level a boss was beaten
    private static final Font FONT_ANNOUNCE = new Font("Arial", Font.BOLD, 64);
    private static final Font FONT_ANNOUNCE_SUB = new Font("Arial", Font.BOLD, 20);

    // Score milestone pulse
    private static final int MILESTONE_INTERVAL = 2500;
    private float scorePulse = 0;
    private int lastMilestone = 0;

    // Achievement toast queue
    private final java.util.LinkedList<String> toastQueue = new java.util.LinkedList<>();
    private String currentToast = null;
    private int toastTimer = 0;
    private float toastSlide = 0; // 0=hidden, 1=visible
    private static final int TOAST_DURATION = 180; // 3 seconds
    private static final Font FONT_SCORE_BIG = new Font("Arial", Font.BOLD, 42);
    private static final Color GOLD = new Color(255, 210, 80);

    public void tick() {
        float maxHealth = 100 + (bounds / 2);
        HEALTH = Game.clamp(HEALTH, 0, maxHealth);

        // Score multiplied by streak + perk bonus
        int mult = (int) (getPlayerMultiplier() * Perks.getScoreMultiplier());
        score += mult;
        points += mult;
        ticksSurvived++;

        frameTick++;
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

        // Achievement toast tick
        if (currentToast != null) {
            toastTimer--;
            float target = toastTimer > 30 ? 1f : 0f;
            toastSlide += (target - toastSlide) * 0.12f;
            if (toastTimer <= 0) {
                currentToast = null;
                toastSlide = 0;
            }
        } else if (!toastQueue.isEmpty()) {
            currentToast = toastQueue.poll();
            toastTimer = TOAST_DURATION;
            toastSlide = 0;
        }
        // Check for new achievements
        if (Achievements.hasNew()) {
            for (Achievements.Achievement a : Achievements.popNewlyUnlocked()) {
                toastQueue.add(a.name);
            }
        }
    }

    public void triggerLevelUpBanner() {
        levelUpBanner = 1f;
        // Level announcements are now just the watermark flash — no center text
        isWaveAnnounce = false;
        announceTimer = 0;
    }

    public void triggerWaveAnnounce() {
        waveCount++;
        lastBossLevel = level;
        isWaveAnnounce = true;
        announceText = "WAVE " + waveCount;
        announceSubtext = waveCount == 1 ? "Good luck" : "Boss defeated - next wave";
        announceTimer = 110;
        levelUpBanner = 1f;
    }

    public int getLevelInWave() {
        return level - lastBossLevel;
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

        // Border tremble below 25%
        int bx = barX, by = barY;
        if (healthPct < 0.25f && healthPct > 0) {
            float trembleIntensity = (0.25f - healthPct) / 0.25f;
            bx += (int) ((Math.sin(frameTick * 1.2) * 1.5) * trembleIntensity);
            by += (int) ((Math.cos(frameTick * 1.7) * 0.8) * trembleIntensity);
        }

        // Background
        g2.setColor(BAR_BG);
        g2.fillRoundRect(bx, by, barW, barH, 7, 7);

        // Fill
        Color healthColor = getHealthColor(healthPct);
        int fillW = (int) (barW * healthPct);

        if (fillW > 0 && healthPct >= 0.25f) {
            // Normal or cracked fill
            g2.setColor(healthColor);
            g2.fillRoundRect(bx, by, fillW, barH, 7, 7);
        } else if (fillW > 0) {
            // Fragmented fill below 25% — draw in segments with gaps
            float flickerIntensity = (0.25f - healthPct) / 0.25f;
            int segCount = 5 + (int) (flickerIntensity * 4);
            int segW = Math.max(fillW / segCount, 2);
            for (int i = 0; i < segCount; i++) {
                int sx = bx + i * (fillW / segCount);
                int sw = segW - 1; // 1px gap between segments
                if (sx + sw > bx + fillW) sw = bx + fillW - sx;
                if (sw <= 0) continue;

                // Flicker — some segments randomly dim
                boolean flicker = flickerIntensity > 0.5f && ((frameTick + i * 7) % 5 == 0);
                Color segColor = flicker
                        ? new Color(healthColor.getRed() / 2, healthColor.getGreen() / 2, healthColor.getBlue() / 2)
                        : healthColor;

                // Vertical offset jitter on fragments
                int jitter = (int) (Math.sin(frameTick * 0.8 + i * 2.3) * 1.5 * flickerIntensity);
                g2.setColor(segColor);
                g2.fillRect(sx, by + jitter, sw, barH);
            }
        }

        // Crack lines below 50%
        if (healthPct < 0.5f && healthPct > 0 && fillW > 4) {
            float crackIntensity = (0.5f - healthPct) / 0.5f; // 0 at 50%, 1 at 0%
            int crackCount = 2 + (int) (crackIntensity * 5);
            crackRng.setSeed(42); // reset seed for consistent positions each frame
            g2.setColor(new Color(0, 0, 0, 60 + (int) (crackIntensity * 120)));
            for (int i = 0; i < crackCount; i++) {
                int cx = bx + 4 + crackRng.nextInt(Math.max(fillW - 8, 1));
                int cy1 = by + crackRng.nextInt(4);
                int cy2 = by + barH - crackRng.nextInt(4);
                int midX = cx + crackRng.nextInt(5) - 2;
                int midY = by + barH / 2 + crackRng.nextInt(4) - 2;
                g2.drawLine(cx, cy1, midX, midY);
                g2.drawLine(midX, midY, cx + crackRng.nextInt(3) - 1, cy2);
            }
        }

        // Border
        g2.setColor(healthPct < 0.25f
                ? lerpColor(BAR_BORDER, HEALTH_LOW, (0.25f - healthPct) / 0.25f * 0.5f)
                : BAR_BORDER);
        g2.drawRoundRect(bx, by, barW, barH, 7, 7);

        // HP text
        g2.setFont(FONT_LABEL);
        g2.setColor(new Color(255, 255, 255, 180));
        String hpText = (int) HEALTH + " / " + (int) maxHealth;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hpText, bx + (barW - fm.stringWidth(hpText)) / 2, by + 11);

        // Stats below health bar
        g2.setFont(FONT_STAT);
        g2.setColor(TEXT_DIM);
        g2.drawString("W" + waveCount + " LVL " + getLevelInWave(), barX, barY + 34);
        g2.drawString("PTS " + points, barX + 80, barY + 34);

        // Ability indicators below stats
        renderAbilityBar(g2, barX, barY + 46);

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
        int displayLevel = getLevelInWave();
        String levelStr = displayLevel <= 9 ? "0" + displayLevel : "" + displayLevel;
        fm = g2.getFontMetrics();
        g2.drawString(levelStr, (Game.WIDTH - fm.stringWidth(levelStr)) / 2, Game.HEIGHT - 80);

        // Wave announcement — slam in, hold, fade out (only for wave changes)
        if (isWaveAnnounce && announceTimer > 0 && announceText.length() > 0) {
            float t = 1f - (announceTimer / 110f);

            float alpha, scaleF, offsetY;
            if (t < 0.12f) {
                float p = t / 0.12f;
                scaleF = 1.8f - p * 0.8f;
                alpha = p;
                offsetY = 0;
            } else if (t < 0.65f) {
                scaleF = 1f;
                alpha = 1f;
                offsetY = 0;
            } else {
                float p = (t - 0.65f) / 0.35f;
                scaleF = 1f;
                alpha = 1f - p;
                offsetY = -p * 25;
            }

            int cx = Game.WIDTH / 2;
            int cy = Game.HEIGHT / 2 - 20 + (int) offsetY;
            int a = (int) (alpha * 255);

            int fontSize = (int) (64 * scaleF);
            if (fontSize > 8) {
                g2.setFont(FONT_ANNOUNCE.deriveFont((float) fontSize));
                g2.setColor(new Color(255, 210, 80, Math.min(a, 255)));
                fm = g2.getFontMetrics();
                g2.drawString(announceText, cx - fm.stringWidth(announceText) / 2, cy);
            }

            if (announceSubtext.length() > 0 && t > 0.08f) {
                float subAlpha = Math.min((t - 0.08f) / 0.12f, 1f) * alpha;
                g2.setFont(FONT_ANNOUNCE_SUB);
                g2.setColor(new Color(200, 200, 200, (int) (subAlpha * 180)));
                fm = g2.getFontMetrics();
                g2.drawString(announceSubtext, cx - fm.stringWidth(announceSubtext) / 2, cy + 35 + (int) offsetY);
            }

            if (t > 0.08f && t < 0.8f) {
                float lineP = Math.min((t - 0.08f) / 0.15f, 1f);
                if (t > 0.65f) lineP *= alpha;
                int lineW = (int) (200 * lineP);
                g2.setColor(new Color(255, 210, 80, (int) (lineP * 100)));
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

        // Achievement toast
        if (currentToast != null && toastSlide > 0.01f) {
            renderToast(g2);
        }
    }

    private void renderToast(Graphics2D g) {
        int toastW = 280;
        int toastH = 48;
        int toastX = Game.WIDTH - toastW - 20;
        int baseY = 80;
        int toastY = baseY - (int) ((1f - toastSlide) * 60); // slides down from above
        int alpha = (int) (toastSlide * 255);

        // Background
        g.setColor(new Color(22, 30, 44, (int) (alpha * 0.9f)));
        g.fillRoundRect(toastX, toastY, toastW, toastH, 8, 8);
        // Accent left strip
        g.setColor(new Color(255, 210, 80, alpha));
        g.fillRoundRect(toastX, toastY, 4, toastH, 4, 4);
        // Border
        g.setColor(new Color(255, 210, 80, alpha / 3));
        g.drawRoundRect(toastX, toastY, toastW, toastH, 8, 8);

        // Trophy icon
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(new Color(255, 210, 80, alpha));
        g.drawString("\u2605", toastX + 14, toastY + 31);

        // Label
        g.setFont(FONT_TIER);
        g.setColor(new Color(255, 210, 80, alpha));
        g.drawString("ACHIEVEMENT UNLOCKED", toastX + 38, toastY + 18);

        // Name
        g.setFont(FONT_STAT);
        g.setColor(new Color(230, 234, 240, alpha));
        g.drawString(currentToast, toastX + 38, toastY + 37);
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

    public int getWaveCount() { return waveCount; }

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
        lastBossLevel = 0;
        isWaveAnnounce = false;
    }

    private static final Color ABILITY_READY = new Color(78, 205, 196);
    private static final Color ABILITY_COOLDOWN = new Color(50, 60, 75);
    private static final Color SHIELD_COLOR = new Color(140, 220, 255);
    private static final Color SLOWMO_COLOR = new Color(180, 140, 255);
    private static final Font FONT_ABILITY = new Font("Arial", Font.BOLD, 10);
    private static final Font FONT_CHARGE = new Font("Arial", Font.BOLD, 12);

    private void renderAbilityBar(Graphics2D g, int x, int y) {
        // Find player
        Player player = null;
        for (int i = 0; i < Game.getHandler().getObjects().size(); i++) {
            GameObject obj = Game.getHandler().getObjects().get(i);
            if (obj instanceof Player) { player = (Player) obj; break; }
        }
        if (player == null) return;

        int iconSize = 22;
        int gap = 8;
        int ix = x;

        // === DASH ===
        float dashPct = player.getDashCooldownPct();
        boolean dashReady = dashPct <= 0;
        drawAbilityIcon(g, ix, y, iconSize, "SHIFT", dashReady, dashPct, ABILITY_READY);
        ix += iconSize + gap;

        // === SHIELD ===
        boolean shieldUp = player.isShieldActive();
        float shieldPct = player.getShieldCooldownPct();
        drawAbilityIcon(g, ix, y, iconSize, "SHD", shieldUp, shieldUp ? 0 : shieldPct, SHIELD_COLOR);
        ix += iconSize + gap;

        // === SLOW-MO ===
        int charges = player.getSlowmoCharges();
        boolean slowActive = player.isSlowmoActive();
        float slowPct = player.getSlowmoTimerPct();
        Color slowCol = slowActive ? new Color(220, 180, 255) : SLOWMO_COLOR;
        g.setColor(ABILITY_COOLDOWN);
        g.fillRoundRect(ix, y, iconSize, iconSize, 4, 4);
        if (slowActive) {
            // Active timer fill
            int fillH = (int) (iconSize * slowPct);
            g.setColor(new Color(slowCol.getRed(), slowCol.getGreen(), slowCol.getBlue(), 100));
            g.fillRoundRect(ix, y + iconSize - fillH, iconSize, fillH, 4, 4);
        }
        g.setColor(charges > 0 || slowActive ? slowCol : new Color(60, 60, 80));
        g.drawRoundRect(ix, y, iconSize, iconSize, 4, 4);
        // Charge count
        g.setFont(FONT_CHARGE);
        g.setColor(charges > 0 || slowActive ? slowCol : new Color(80, 80, 100));
        String chargeStr = slowActive ? "E" : String.valueOf(charges);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(chargeStr, ix + (iconSize - fm.stringWidth(chargeStr)) / 2, y + iconSize / 2 + 5);
    }

    private void drawAbilityIcon(Graphics2D g, int x, int y, int size, String label,
                                  boolean ready, float cooldownPct, Color readyColor) {
        // Background
        g.setColor(ABILITY_COOLDOWN);
        g.fillRoundRect(x, y, size, size, 4, 4);

        // Cooldown sweep (fill from bottom up)
        if (!ready && cooldownPct > 0) {
            int fillH = (int) (size * (1f - cooldownPct));
            g.setColor(new Color(readyColor.getRed(), readyColor.getGreen(), readyColor.getBlue(), 40));
            g.fillRoundRect(x, y + size - fillH, size, fillH, 4, 4);
        }

        // Border
        g.setColor(ready ? readyColor : new Color(60, 60, 80));
        g.drawRoundRect(x, y, size, size, 4, 4);

        // Label
        g.setFont(FONT_ABILITY);
        g.setColor(ready ? readyColor : new Color(80, 80, 100));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x + (size - fm.stringWidth(label)) / 2, y + size / 2 + 4);
    }
}
