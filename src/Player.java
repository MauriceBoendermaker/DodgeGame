import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Player extends GameObject {

    private Handler handler;

    public Player(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 48, 48);
    }

    public void tick() {
        x += velX;
        y += velY;
        x = Game.clamp(x, 0, Game.WIDTH - 37);
        y = Game.clamp(y, 0, Game.HEIGHT - 60);

        handler.addObject(new Trail(x, y, ID.Trail, Color.white, 48, 48, 0.045f, handler));

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
        g.setColor(Color.white);
        g.fillRect((int) x, (int) y, 48, 48);
    }
}
