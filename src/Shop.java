import java.awt.Color;
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

    // Hover state
    private float[] cardHover = new float[3];
    private int mouseX, mouseY;

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
                healthUpgradeCost, hud.getPoints() >= healthUpgradeCost, PageRenderer.SUCCESS, cardHover[0]);
        drawCard(g2, card2X(), CARD_Y, "Upgrade Speed", "Increases your movement speed.",
                speedUpgradeCost, hud.getPoints() >= speedUpgradeCost, PageRenderer.ACCENT, cardHover[1]);
        drawCard(g2, card3X(), CARD_Y, "Refill Health", "Fully restores your health bar.",
                refillHealthCost, hud.getPoints() >= refillHealthCost, PageRenderer.WARNING, cardHover[2]);

        // Footer
        g2.setFont(PageRenderer.BODY_FONT);
        g2.setColor(PageRenderer.TEXT_MUTED);
        String footer = "Press Space to return to game";
        fm = g2.getFontMetrics();
        g2.drawString(footer, (Game.WIDTH - fm.stringWidth(footer)) / 2, Game.HEIGHT - 50);
    }

    private void updateHover() {
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
                          int cost, boolean canAfford, Color accent, float hover) {
        // Card background with hover
        Color bg = PageRenderer.lerp(PageRenderer.SURFACE, new Color(30, 42, 58), hover);
        Color border = PageRenderer.lerp(PageRenderer.BORDER, accent, hover * 0.4f);
        g.setColor(bg);
        g.fillRoundRect(x, y, CARD_W, CARD_H, PageRenderer.R, PageRenderer.R);
        g.setColor(border);
        g.drawRoundRect(x, y, CARD_W, CARD_H, PageRenderer.R, PageRenderer.R);

        // Accent top strip
        Color stripColor = canAfford ? accent : PageRenderer.TEXT_MUTED;
        g.setColor(PageRenderer.lerp(stripColor, accent, hover));
        g.fillRoundRect(x, y, CARD_W, 4, 4, 4);

        // Title
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT, accent, hover * 0.3f));
        g.drawString(title, x + 20, y + 40);

        // Description
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString(desc, x + 20, y + 70);

        // Cost
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("COST", x + 20, y + 120);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(canAfford ? accent : PageRenderer.TEXT_MUTED);
        g.drawString(String.valueOf(cost), x + 20, y + 148);

        // Buy indicator
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

        if (hitCard(mx, my, card1X()) && hud.getPoints() >= healthUpgradeCost) {
            hud.setPoints(hud.getPoints() - healthUpgradeCost);
            healthUpgradeCost += 250;
            hud.bounds += 20;
            HUD.HEALTH = 100 + (hud.bounds / 2);
            hud.addHealthUpgrade();
        }

        if (hitCard(mx, my, card2X()) && hud.getPoints() >= speedUpgradeCost) {
            hud.setPoints(hud.getPoints() - speedUpgradeCost);
            speedUpgradeCost += 250;
            handler.spd++;
            hud.addSpeedUpgrade();
        }

        if (hitCard(mx, my, card3X()) && hud.getPoints() >= refillHealthCost) {
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
