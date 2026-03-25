import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

public class Player extends GameObject {

    private static final int SIZE = 48;
    private static final int R = 12;
    private static final Color GLOW = new Color(230, 234, 240, 30);
    private static final Color GLOW_INNER = new Color(230, 234, 240, 60);
    private static final Color FILL = new Color(230, 234, 240);
    private static final Color TRAIL_COLOR = new Color(200, 210, 220);

    private Handler handler;

    public Player(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        x += velX;
        y += velY;
        x = Game.clamp(x, 0, Game.WIDTH - 37);
        y = Game.clamp(y, 0, Game.HEIGHT - 60);

        handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.045f, handler));
        collision();
    }

    private void collision() {
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject tempObject = handler.getObjects().get(i);
            if (tempObject.getId() == ID.BasicEnemy || tempObject.getId() == ID.FastEnemy
                    || tempObject.getId() == ID.SmartEnemy || tempObject.getId() == ID.EnemyBoss) {
                if (getBounds().intersects(tempObject.getBounds())) {
                    HUD.HEALTH -= 2;
                }
            }
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;

        // Outer glow
        g2.setColor(GLOW);
        g2.fillRoundRect(ix - 5, iy - 5, SIZE + 10, SIZE + 10, R + 5, R + 5);
        // Inner glow
        g2.setColor(GLOW_INNER);
        g2.fillRoundRect(ix - 2, iy - 2, SIZE + 4, SIZE + 4, R + 2, R + 2);
        // Main shape
        g2.setColor(FILL);
        g2.fillRoundRect(ix, iy, SIZE, SIZE, R, R);
    }
}
