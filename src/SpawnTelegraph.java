import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class SpawnTelegraph extends GameObject {

    private static final int DURATION = 60; // ~1 second at 60fps

    private Handler handler;
    private GameObject toSpawn;
    private int timer = 0;
    private Color color;
    private int size;

    public SpawnTelegraph(float x, float y, Handler handler, GameObject toSpawn, Color color, int size) {
        super(x, y, ID.SpawnTelegraph);
        this.handler = handler;
        this.toSpawn = toSpawn;
        this.color = color;
        this.size = size;
    }

    public void tick() {
        timer++;
        if (timer >= DURATION) {
            handler.removeObject(this);
            handler.addObject(toSpawn);
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        float progress = timer / (float) DURATION; // 0 to 1
        int cx = (int) x + size / 2;
        int cy = (int) y + size / 2;

        // Pulsing warning ring — expands then contracts
        float pulse = (float) (Math.sin(timer * 0.3) * 0.5 + 0.5);
        int ringRadius = (int) (size * 0.8f + pulse * size * 0.4f);
        int ringAlpha = (int) ((0.3f + pulse * 0.4f) * 255 * (1f - progress * 0.3f));
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(ringAlpha, 255)));
        g2.drawOval(cx - ringRadius / 2, cy - ringRadius / 2, ringRadius, ringRadius);

        // Inner ring — tighter
        int innerRadius = (int) (size * 0.4f + pulse * size * 0.2f);
        int innerAlpha = (int) (pulse * 120 * (1f - progress * 0.3f));
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(innerAlpha, 255)));
        g2.drawOval(cx - innerRadius / 2, cy - innerRadius / 2, innerRadius, innerRadius);

        // Materializing shape — fades in and scales up in the last 40%
        if (progress > 0.6f) {
            float matProgress = (progress - 0.6f) / 0.4f; // 0 to 1
            float scale = 0.3f + matProgress * 0.7f;
            int matAlpha = (int) (matProgress * 180);
            int matSize = (int) (size * scale);
            int mx = cx - matSize / 2;
            int my = cy - matSize / 2;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(matAlpha, 255)));
            g2.fillRoundRect(mx, my, matSize, matSize, 8, 8);
        }

        // Cross-hair lines
        int crossAlpha = (int) ((1f - progress) * 60);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(crossAlpha, 255)));
        int crossLen = (int) (size * 1.2f);
        g2.drawLine(cx - crossLen / 2, cy, cx + crossLen / 2, cy);
        g2.drawLine(cx, cy - crossLen / 2, cx, cy + crossLen / 2);
    }

    public Rectangle getBounds() {
        return null; // doesn't collide
    }
}
