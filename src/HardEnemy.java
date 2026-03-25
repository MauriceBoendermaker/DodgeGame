import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Random;

public class HardEnemy extends GameObject {

    private Handler handler;
    private Random r = new Random();

    public HardEnemy(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;

        velX = 5;
        velY = 5;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 32, 32);
    }

    public void tick() {
        x += velX;
        y += velY;

        if (y <= 0 || y >= Game.HEIGHT - 32) {
            velY = (y <= 0) ? (r.nextInt(7) + 1) : -(r.nextInt(7) + 1);
        }
        if (x <= 0 || x >= Game.WIDTH - 16) {
            velX = (x <= 0) ? (r.nextInt(7) + 1) : -(r.nextInt(7) + 1);
        }

        handler.addObject(new Trail(x, y, ID.Trail, Color.yellow, 32, 32, 0.02f, handler));
    }

    public void render(Graphics g) {
        g.setColor(Color.yellow);
        g.fillRect((int) x, (int) y, 32, 32);
    }
}
