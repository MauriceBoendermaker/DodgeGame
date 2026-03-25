import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

public class FastEnemy extends GameObject {

    private static final int SIZE = 32;
    private static final Color FILL = new Color(78, 205, 196);
    private static final Color GLOW = new Color(78, 205, 196, 35);
    private static final Color TRAIL_COLOR = new Color(78, 205, 196);

    private Handler handler;

    public FastEnemy(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        velX = 2;
        velY = 9;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        x += velX;
        y += velY;
        if (y <= 0 || y >= Game.HEIGHT - SIZE) velY *= -1;
        if (x <= 0 || x >= Game.WIDTH - SIZE) velX *= -1;
        handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.02f, handler));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + SIZE / 2;
        int cy = (int) y + SIZE / 2;
        int half = SIZE / 2;

        // Diamond shape (rotated square)
        Polygon glow = new Polygon(
                new int[]{cx, cx + half + 4, cx, cx - half - 4},
                new int[]{cy - half - 4, cy, cy + half + 4, cy}, 4);
        g2.setColor(GLOW);
        g2.fillPolygon(glow);

        Polygon diamond = new Polygon(
                new int[]{cx, cx + half, cx, cx - half},
                new int[]{cy - half, cy, cy + half, cy}, 4);
        g2.setColor(FILL);
        g2.fillPolygon(diamond);
    }
}
