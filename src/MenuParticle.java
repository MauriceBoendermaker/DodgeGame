import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Random;

public class MenuParticle extends GameObject {

    private static final int SIZE = 16;
    private Handler handler;
    private Color col;

    public MenuParticle(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;

        Random r = new Random();
        velX = r.nextInt(15) - 7;
        velY = r.nextInt(15) - 7;
        if (velX == 0) velX = 1;
        if (velY == 0) velY = 1;

        col = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        x += velX;
        y += velY;
        if (y <= 0 || y >= Game.HEIGHT - SIZE * 2) velY *= -1;
        if (x <= 0 || x >= Game.WIDTH - SIZE) velX *= -1;
        handler.addObject(new Trail(x, y, ID.Trail, col, SIZE, SIZE, 0.04f, handler));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(col);
        g2.fillRoundRect((int) x, (int) y, SIZE, SIZE, 6, 6);
    }
}
