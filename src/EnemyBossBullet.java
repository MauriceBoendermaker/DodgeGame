import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class EnemyBossBullet extends GameObject {

    private static final int SIZE = 16;
    private static final Color FILL = new Color(235, 87, 87);
    private static final Color GLOW = new Color(235, 87, 87, 40);
    private static final Color TRAIL_COLOR = new Color(235, 87, 87);

    private Handler handler;

    public EnemyBossBullet(int x, int y, ID id, Handler handler, float vx, float vy) {
        super(x, y, id);
        this.handler = handler;
        velX = vx;
        velY = vy;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        x += velX;
        y += velY;
        // Remove if off-screen
        if (y >= Game.HEIGHT || y < -50 || x < -50 || x > Game.WIDTH + 50) {
            handler.removeObject(this);
            return;
        }
        handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.03f, handler));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;

        g2.setColor(GLOW);
        g2.fillOval(ix - 3, iy - 3, SIZE + 6, SIZE + 6);
        g2.setColor(FILL);
        g2.fillOval(ix, iy, SIZE, SIZE);
    }
}
