import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
    private float rotation = 0;
    private float targetRotation = 0;
    private final Rectangle boundsRect = new Rectangle();

    public HardEnemy(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        velX = 5;
        velY = 5;
        targetRotation = (float) Math.atan2(velY, velX);
        rotation = targetRotation;
    }

    public Rectangle getBounds() {
        boundsRect.setBounds((int) x, (int) y, SIZE, SIZE);
        return boundsRect;
    }

    public void tick() {
        x += velX;
        y += velY;

        // Rotate toward movement direction with snap on bounce
        targetRotation = (float) Math.atan2(velY, velX);
        float diff = targetRotation - rotation;
        // Normalize angle difference
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        rotation += diff * 0.15f;

        if (y <= 0 || y >= Game.HEIGHT - SIZE) {
            Game.wallHit(x + SIZE / 2, y <= 0 ? 0 : Game.HEIGHT, y <= 0 ? 0 : 1, 245, 195, 68);
            velY = (y <= 0) ? (r.nextInt(7) + 1) : -(r.nextInt(7) + 1);
            targetRotation = (float) Math.atan2(velY, velX);
        }
        if (x <= 0 || x >= Game.WIDTH - SIZE) {
            Game.wallHit(x <= 0 ? 0 : Game.WIDTH, y + SIZE / 2, x <= 0 ? 2 : 3, 245, 195, 68);
            velX = (x <= 0) ? (r.nextInt(7) + 1) : -(r.nextInt(7) + 1);
            targetRotation = (float) Math.atan2(velY, velX);
        }
        if (++trailTick % 3 == 0)
            handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.02f, handler, Trail.SHAPE_TRIANGLE));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + SIZE / 2;
        int cy = (int) y + SIZE / 2;
        int half = SIZE / 2;

        // Triangle pointing in movement direction
        g2.setColor(GLOW);
        drawRotatedTriangle(g2, cx, cy, half + 4, rotation);
        g2.setColor(FILL);
        drawRotatedTriangle(g2, cx, cy, half, rotation);
    }

    private void drawRotatedTriangle(Graphics2D g, int cx, int cy, int r, float angle) {
        int[] xp = new int[3];
        int[] yp = new int[3];
        for (int i = 0; i < 3; i++) {
            float a = angle + i * (float) (Math.PI * 2 / 3);
            xp[i] = cx + (int) (Math.cos(a) * r);
            yp[i] = cy + (int) (Math.sin(a) * r);
        }
        g.fillPolygon(xp, yp, 3);
    }
}
