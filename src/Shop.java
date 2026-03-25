import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Shop extends MouseAdapter {

    private Handler handler;
    private HUD hud;

    private int healthUpgradeCost = 1000;
    private int speedUpgradeCost = 750;
    private int refillHealthCost = 750;

    private static final Font FONT_TITLE = new Font("Arial", Font.PLAIN, 50);
    private static final Font FONT_SUBTITLE = new Font("Arial", Font.PLAIN, 25);
    private static final Font FONT_ITEM = new Font("Arial", Font.PLAIN, 20);

    public Shop(Handler handler, HUD hud) {
        this.handler = handler;
        this.hud = hud;
    }

    public void render(Graphics g) {
        g.setColor(Color.white);
        g.setFont(FONT_TITLE);
        g.drawString(" - Shop - ", Game.WIDTH / 2 - 100, 50);

        g.setFont(FONT_SUBTITLE);
        g.drawString("Points to spend: " + hud.getPoints(), Game.WIDTH / 2 - 105, 75);

        // Upgrade Health box
        drawShopItem(g, 1280 / 3 - 250, 720 / 4, 175, 100,
                "Upgrade Health", healthUpgradeCost, hud.getPoints() >= healthUpgradeCost);

        // Upgrade Speed box
        drawShopItem(g, 1280 / 3 + 145, 720 / 4, 175, 100,
                "Upgrade Speed", speedUpgradeCost, hud.getPoints() >= speedUpgradeCost);

        // Refill Health box
        drawShopItem(g, 1280 / 3 + 595, 720 / 4, 175, 100,
                "Refill Health", refillHealthCost, hud.getPoints() >= refillHealthCost);

        g.setColor(Color.white);
        g.setFont(FONT_SUBTITLE);
        g.drawString("*Press SpaceBar to go back*", Game.WIDTH / 2 - 185, Game.HEIGHT - 100);

        g.drawString("- Upgrade Health: Makes the HealthBar LONGER & REFILL Health!", 25, 350);
        g.drawString("- Upgrade Speed: Makes you move FASTER!", 25, 390);
        g.drawString("- Refill Health: Refill the COMPLETE HealthBar!", 25, 430);

        g.setColor(Color.yellow);
        g.drawString("(At Higher Levels, ONLY the Colors can be Bugged/Glitched!!)", Game.WIDTH / 2 - 320, Game.HEIGHT - 65);
    }

    private void drawShopItem(Graphics g, int x, int y, int w, int h,
                              String name, int cost, boolean canAfford) {
        g.setColor(canAfford ? Color.green : Color.red);
        g.drawRect(x, y, w, h);

        g.setFont(FONT_ITEM);
        g.drawString(name, x + 10, y + 45);
        g.drawString("Cost: " + cost, x + 10, y + 75);
    }

    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        // Upgrade Health
        if (mx >= 1280 / 3 - 250 && mx <= 1280 / 3 - 250 + 175) {
            if (my >= 720 / 4 && my <= 720 / 4 + 100) {
                if (hud.getPoints() >= healthUpgradeCost) {
                    hud.setPoints(hud.getPoints() - healthUpgradeCost);
                    healthUpgradeCost += 250;
                    hud.bounds += 20;
                    HUD.HEALTH = 100 + (hud.bounds / 2);
                }
            }
        }

        // Upgrade Speed
        if (mx >= 1280 / 3 + 145 && mx <= 1280 / 3 + 145 + 175) {
            if (my >= 720 / 4 && my <= 720 / 4 + 100) {
                if (hud.getPoints() >= speedUpgradeCost) {
                    hud.setPoints(hud.getPoints() - speedUpgradeCost);
                    speedUpgradeCost += 250;
                    handler.spd++;
                }
            }
        }

        // Refill Health
        if (mx >= 1280 / 3 + 595 && mx <= 1280 / 3 + 595 + 175) {
            if (my >= 720 / 4 && my <= 720 / 4 + 100) {
                if (hud.getPoints() >= refillHealthCost) {
                    hud.setPoints(hud.getPoints() - refillHealthCost);
                    refillHealthCost += 250;
                    HUD.HEALTH = 100 + (hud.bounds / 2);
                }
            }
        }
    }
}
