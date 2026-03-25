import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Random;

public class HardEnemy extends GameObject {

    private static final int SIZE = 32;
    private static final Color FILL = new Color(245, 195, 68);
    private static final Color GLOW = new Color(245, 195, 68, 35);
    private static final Color TRAIL_COLOR = new Color(245, 195, 68);

    private Handler handler;
    private Random r = new Random();
    private int trailTick = 0;

    public HardEnemy(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        velX = 5;
        velY = 5;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        x += velX;
        y += velY;
        if (y <= 0 || y >= Game.HEIGHT - SIZE) {
            Game.wallHit(x + SIZE / 2, y <= 0 ? 0 : Game.HEIGHT, y <= 0 ? 0 : 1, 245, 195, 68);
            velY = (y <= 0) ? (r.nextInt(7) + 1) : -(r.nextInt(7) + 1);
        }
        if (x <= 0 || x >= Game.WIDTH - SIZE) {
            Game.wallHit(x <= 0 ? 0 : Game.WIDTH, y + SIZE / 2, x <= 0 ? 2 : 3, 245, 195, 68);
            velX = (x <= 0) ? (r.nextInt(7) + 1) : -(r.nextInt(7) + 1);
        }
        if (++trailTick % 3 == 0)
            handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.02f, handler, Trail.SHAPE_TRIANGLE));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + SIZE / 2;
        int cy = (int) y + SIZE / 2;
        int half = SIZE / 2;

        // Triangle shape — points upward
        Polygon glowTri = new Polygon(
                new int[]{cx, cx + half + 4, cx - half - 4},
                new int[]{cy - half - 4, cy + half + 2, cy + half + 2}, 3);
        g2.setColor(GLOW);
        g2.fillPolygon(glowTri);

        Polygon tri = new Polygon(
                new int[]{cx, cx + half, cx - half},
                new int[]{cy - half, cy + half, cy + half}, 3);
        g2.setColor(FILL);
        g2.fillPolygon(tri);
    }
}
