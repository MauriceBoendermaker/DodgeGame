import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class SmartEnemy extends GameObject {

    private static final int SIZE = 32;
    private static final Color FILL = new Color(199, 125, 255);
    private static final Color GLOW = new Color(199, 125, 255, 35);
    private static final Color TRAIL_COLOR = new Color(199, 125, 255);

    private Handler handler;
    private GameObject player;

    public SmartEnemy(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        for (int i = 0; i < handler.getObjects().size(); i++) {
            if (handler.getObjects().get(i).getId() == ID.Player) {
                player = handler.getObjects().get(i);
            }
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        x += velX;
        y += velY;
        float diffX = x - player.getX() - 16;
        float diffY = y - player.getY() - 16;
        float distance = (float) Math.sqrt((x - player.getX()) * (x - player.getX())
                + (y - player.getY()) * (y - player.getY()));
        velX = ((-2 / distance) * diffX);
        velY = ((-2 / distance) * diffY);
        handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.02f, handler));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;
        int cx = ix + SIZE / 2, cy = iy + SIZE / 2;

        // Circle with glow
        g2.setColor(GLOW);
        g2.fillOval(cx - SIZE / 2 - 4, cy - SIZE / 2 - 4, SIZE + 8, SIZE + 8);
        g2.setColor(FILL);
        g2.fillOval(ix, iy, SIZE, SIZE);
    }
}
