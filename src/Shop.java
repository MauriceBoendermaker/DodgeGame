import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Shop extends MouseAdapter {

    private Handler handler;
    private HUD hud;

    private int healthUpgradeCost = 1000;
    private int speedUpgradeCost = 750;
    private int refillHealthCost = 750;

    public void reset() {
        healthUpgradeCost = 1000;
        speedUpgradeCost = 750;
        refillHealthCost = 750;
    }

    // Card layout
    private static final int CARD_W = 320;
    private static final int CARD_H = 200;
    private static final int CARD_Y = 180;
    private static final int CARD_GAP = 40;
    private static final int TOTAL_W = CARD_W * 3 + CARD_GAP * 2;

    private static int cardStartX() { return (Game.WIDTH - TOTAL_W) / 2; }
    private static int card1X() { return cardStartX(); }
    private static int card2X() { return cardStartX() + CARD_W + CARD_GAP; }
    private static int card3X() { return cardStartX() + (CARD_W + CARD_GAP) * 2; }

    // Preview types
    private static final int PREVIEW_HEALTH = 0;
    private static final int PREVIEW_SPEED = 1;
    private static final int PREVIEW_REFILL = 2;

    // Hover state
    private float[] cardHover = new float[3];
    private int mouseX, mouseY;
    private int frameTick = 0;

    public Shop(Handler handler, HUD hud) {
        this.handler = handler;
        this.hud = hud;
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        PageRenderer.drawBackground(g2);
        PageRenderer.drawTitle(g2, "Shop");

        // Update hover
        updateHover();

        // Points display
        g2.setFont(PageRenderer.SUBTITLE_FONT);
        g2.setColor(PageRenderer.ACCENT);
        String pts = hud.getPoints() + " points";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(pts, (Game.WIDTH - fm.stringWidth(pts)) / 2, 130);

        drawCard(g2, card1X(), CARD_Y, "Upgrade Health", "Extends and refills your health bar.",
                healthUpgradeCost, hud.getPoints() >= healthUpgradeCost, PageRenderer.SUCCESS, cardHover[0],
                hud.getHealthUpgrades(), PREVIEW_HEALTH);
        drawCard(g2, card2X(), CARD_Y, "Upgrade Speed", "Increases your movement speed.",
                speedUpgradeCost, hud.getPoints() >= speedUpgradeCost, PageRenderer.ACCENT, cardHover[1],
                hud.getSpeedUpgrades(), PREVIEW_SPEED);
        drawCard(g2, card3X(), CARD_Y, "Refill Health", "Fully restores your health bar.",
                refillHealthCost, hud.getPoints() >= refillHealthCost, PageRenderer.WARNING, cardHover[2],
                hud.getRefills(), PREVIEW_REFILL);

        // Footer
        g2.setFont(PageRenderer.BODY_FONT);
        g2.setColor(PageRenderer.TEXT_MUTED);
        String footer = "Press Space to return to game";
        fm = g2.getFontMetrics();
        g2.drawString(footer, (Game.WIDTH - fm.stringWidth(footer)) / 2, Game.HEIGHT - 50);
    }

    private void updateHover() {
        frameTick++;
        int[] cardXs = {card1X(), card2X(), card3X()};
        for (int i = 0; i < 3; i++) {
            boolean over = mouseX >= cardXs[i] && mouseX <= cardXs[i] + CARD_W
                    && mouseY >= CARD_Y && mouseY <= CARD_Y + CARD_H;
            float target = over ? 1f : 0f;
            cardHover[i] += (target - cardHover[i]) * 0.14f;
            if (Math.abs(cardHover[i] - target) < 0.01f) cardHover[i] = target;
        }
    }

    private void drawCard(Graphics2D g, int x, int y, String title, String desc,
                          int cost, boolean canAfford, Color accent, float hover,
                          int tier, int previewType) {
        boolean maxed = tier >= HUD.MAX_TIER;

        // Card background with hover
        Color bg = PageRenderer.lerp(PageRenderer.SURFACE, new Color(30, 42, 58), hover);
        Color border = PageRenderer.lerp(PageRenderer.BORDER, maxed ? accent : PageRenderer.BORDER, maxed ? 0.3f : hover * 0.4f);
        g.setColor(bg);
        g.fillRoundRect(x, y, CARD_W, CARD_H, PageRenderer.R, PageRenderer.R);
        g.setColor(border);
        g.drawRoundRect(x, y, CARD_W, CARD_H, PageRenderer.R, PageRenderer.R);

        // Accent top strip
        g.setColor(maxed ? accent : (canAfford ? accent : PageRenderer.TEXT_MUTED));
        g.fillRoundRect(x, y, CARD_W, 4, 4, 4);

        // Title
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT, accent, hover * 0.3f));
        g.drawString(title, x + 20, y + 40);

        // Tier dots
        int dotX = x + 20;
        int dotY = y + 55;
        int dotSize = 8;
        int dotGap = 3;
        for (int i = 0; i < HUD.MAX_TIER; i++) {
            g.setColor(i < tier ? accent : new Color(30, 38, 52));
            g.fillRoundRect(dotX + i * (dotSize + dotGap), dotY, dotSize, dotSize, 3, 3);
        }
        if (maxed) {
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.setColor(accent);
            g.drawString("MAX", dotX + HUD.MAX_TIER * (dotSize + dotGap) + 4, dotY + 8);
        }

        // Description
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString(desc, x + 20, y + 88);

        if (maxed) {
            // Maxed state
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(accent);
            g.drawString("Fully upgraded", x + 20, y + 148);
        } else {
            // Cost
            g.setFont(PageRenderer.LABEL_FONT);
            g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("COST", x + 20, y + 120);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(canAfford ? accent : PageRenderer.TEXT_MUTED);
            g.drawString(String.valueOf(cost), x + 20, y + 148);

            // Buy button
            if (canAfford) {
                Color buyBg = PageRenderer.lerp(accent, new Color(
                        Math.min(accent.getRed() + 30, 255),
                        Math.min(accent.getGreen() + 30, 255),
                        Math.min(accent.getBlue() + 30, 255)), hover);
                g.setColor(buyBg);
                g.fillRoundRect(x + CARD_W - 70, y + CARD_H - 40, 50, 24, 6, 6);
                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(PageRenderer.BG_DARK);
                g.drawString("Buy", x + CARD_W - 58, y + CARD_H - 23);
            }
        }

        // Preview animation — visible when hovering
        if (hover > 0.1f) {
            int px = x + CARD_W - 90;
            int py = y + 100;
            float h = hover;
            float t = frameTick * 0.05f;

            switch (previewType) {
                case PREVIEW_HEALTH:
                    drawHealthPreview(g, px, py, accent, h, t);
                    break;
                case PREVIEW_SPEED:
                    drawSpeedPreview(g, px, py, accent, h, t);
                    break;
                case PREVIEW_REFILL:
                    drawRefillPreview(g, px, py, accent, h, t);
                    break;
            }
        }
    }

    private void drawHealthPreview(Graphics2D g, int x, int y, Color accent, float hover, float t) {
        int alpha = (int) (hover * 200);

        // Mini health bar background
        int barW = 70, barH = 10;
        g.setColor(new Color(22, 30, 44, alpha));
        g.fillRoundRect(x, y, barW, barH, 5, 5);

        // Current fill
        float pct = HUD.HEALTH / (100f + hud.bounds / 2f);
        int fillW = (int) (barW * pct);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
        g.fillRoundRect(x, y, fillW, barH, 5, 5);

        // Pulsing extension showing the upgrade effect
        float pulse = (float) (Math.sin(t * 4) * 0.5 + 0.5);
        int extW = 8 + (int) (pulse * 6);
        int extAlpha = (int) (hover * pulse * 160);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), extAlpha));
        g.fillRoundRect(x + fillW, y, extW, barH, 5, 5);

        // "+" icon
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (hover * 180)));
        g.fillRect(x + barW + 10, y + 2, 8, 2);
        g.fillRect(x + barW + 13, y - 1, 2, 8);

        // Border
        g.setColor(new Color(40, 52, 70, alpha));
        g.drawRoundRect(x, y, barW + extW, barH, 5, 5);
    }

    private void drawSpeedPreview(Graphics2D g, int x, int y, Color accent, float hover, float t) {
        int alpha = (int) (hover * 180);
        int cx = x + 35, cy = y + 5;

        // Mini player square
        g.setColor(new Color(230, 234, 240, alpha));
        g.fillRoundRect(cx - 6, cy - 6, 12, 12, 4, 4);

        // Motion lines — stream backward from the player
        for (int i = 0; i < 4; i++) {
            float offset = ((t * 8 + i * 1.5f) % 6f);
            int lineX = cx - 12 - (int) (offset * 6);
            int lineAlpha = (int) (hover * (1f - offset / 6f) * 140);
            int lineW = 6 + (int) ((1f - offset / 6f) * 8);
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    Math.max(0, Math.min(255, lineAlpha))));
            g.fillRect(lineX, cy - 1 + (i - 2) * 4, lineW, 2);
        }

        // Speed arrow
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
        g.fillPolygon(
                new int[]{cx + 14, cx + 22, cx + 14},
                new int[]{cy - 5, cy, cy + 5}, 3);
    }

    private void drawRefillPreview(Graphics2D g, int x, int y, Color accent, float hover, float t) {
        int alpha = (int) (hover * 200);

        // Mini health bar background
        int barW = 70, barH = 10;
        g.setColor(new Color(22, 30, 44, alpha));
        g.fillRoundRect(x, y, barW, barH, 5, 5);

        // Animated fill — sweeps from left to full
        float fillPct = ((t * 2) % 3f);
        if (fillPct > 1f) fillPct = 1f; // hold at full briefly
        int fillW = (int) (barW * fillPct);
        Color fillColor = PageRenderer.lerp(
                new Color(235, 87, 87), accent, fillPct);
        g.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), alpha));
        g.fillRoundRect(x, y, fillW, barH, 5, 5);

        // Sparkle at the fill edge
        if (fillPct > 0.2f && fillPct < 1f) {
            float sparkle = (float) (Math.sin(t * 12) * 0.5 + 0.5);
            int sparkAlpha = (int) (sparkle * hover * 200);
            g.setColor(new Color(255, 255, 255, Math.min(sparkAlpha, 255)));
            g.fillOval(x + fillW - 3, y + 1, 6, 6);
        }

        // Full indicator flash
        if (fillPct >= 1f) {
            float flash = (float) (Math.sin(t * 6) * 0.3 + 0.7);
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    (int) (flash * hover * 80)));
            g.fillRoundRect(x, y, barW, barH, 5, 5);
        }

        // Border
        g.setColor(new Color(40, 52, 70, alpha));
        g.drawRoundRect(x, y, barW, barH, 5, 5);
    }

    public void mouseMoved(MouseEvent e) {
        mouseX = Game.toGameX(e.getX());
        mouseY = Game.toGameY(e.getY());
    }

    public void mouseDragged(MouseEvent e) {
        mouseX = Game.toGameX(e.getX());
        mouseY = Game.toGameY(e.getY());
    }

    public void mousePressed(MouseEvent e) {
        int mx = Game.toGameX(e.getX());
        int my = Game.toGameY(e.getY());

        if (hitCard(mx, my, card1X()) && hud.getPoints() >= healthUpgradeCost
                && hud.getHealthUpgrades() < HUD.MAX_TIER) {
            hud.setPoints(hud.getPoints() - healthUpgradeCost);
            healthUpgradeCost += 250;
            hud.bounds += 20;
            HUD.HEALTH = 100 + (hud.bounds / 2);
            hud.addHealthUpgrade();
        }

        if (hitCard(mx, my, card2X()) && hud.getPoints() >= speedUpgradeCost
                && hud.getSpeedUpgrades() < HUD.MAX_TIER) {
            hud.setPoints(hud.getPoints() - speedUpgradeCost);
            speedUpgradeCost += 250;
            handler.spd++;
            hud.addSpeedUpgrade();
        }

        if (hitCard(mx, my, card3X()) && hud.getPoints() >= refillHealthCost
                && hud.getRefills() < HUD.MAX_TIER) {
            hud.setPoints(hud.getPoints() - refillHealthCost);
            refillHealthCost += 250;
            HUD.HEALTH = 100 + (hud.bounds / 2);
            hud.addRefill();
        }
    }

    private boolean hitCard(int mx, int my, int cardX) {
        return mx >= cardX && mx <= cardX + CARD_W && my >= CARD_Y && my <= CARD_Y + CARD_H;
    }
}
