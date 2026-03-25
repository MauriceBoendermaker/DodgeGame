import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class BasicEnemy extends GameObject {

    private static final int SIZE = 32;
    private static final int R = 8;
    private static final Color FILL = new Color(235, 87, 87);
    private static final Color GLOW = new Color(235, 87, 87, 35);
    private static final Color TRAIL_COLOR = new Color(235, 87, 87);

    private Handler handler;
    private int trailTick = 0;

    public BasicEnemy(int x, int y, ID id, Handler handler) {
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
            Game.wallHit(x + SIZE / 2, y <= 0 ? 0 : Game.HEIGHT, y <= 0 ? 0 : 1);
            velY *= -1;
        }
        if (x <= 0 || x >= Game.WIDTH - SIZE) {
            Game.wallHit(x <= 0 ? 0 : Game.WIDTH, y + SIZE / 2, x <= 0 ? 2 : 3);
            velX *= -1;
        }
        if (++trailTick % 3 == 0)
            handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.02f, handler));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;
        g2.setColor(GLOW);
        g2.fillRoundRect(ix - 4, iy - 4, SIZE + 8, SIZE + 8, R + 4, R + 4);
        g2.setColor(FILL);
        g2.fillRoundRect(ix, iy, SIZE, SIZE, R, R);
    }
}
