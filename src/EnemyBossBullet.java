import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Random;

public class EnemyBossBullet extends GameObject {

    private static final int SIZE = 20;
    private static final Color FILL = new Color(235, 87, 87);
    private static final Color GLOW = new Color(235, 87, 87, 40);
    private static final Color TRAIL_COLOR = new Color(235, 87, 87);

    private Handler handler;

    public EnemyBossBullet(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        Random r = new Random();
        velX = r.nextInt(11) - 5;
        velY = 5;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        x += velX;
        y += velY;
        if (y >= Game.HEIGHT) handler.removeObject(this);
        handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.025f, handler));
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
