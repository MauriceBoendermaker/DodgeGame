import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Player extends GameObject {

    private static final int SIZE = 48;
    private static final int R = 12;
    private static final Color GLOW = new Color(230, 234, 240, 30);
    private static final Color GLOW_INNER = new Color(230, 234, 240, 60);
    private static final Color FILL = new Color(230, 234, 240);
    private static final Color TRAIL_COLOR = new Color(200, 210, 220);

    // Acceleration / deceleration
    private static final float ACCEL = 0.45f;
    private static final float DECEL = 0.30f;

    private Handler handler;
    private int trailTick = 0;

    // Input state — set by KeyInput
    public boolean moveUp, moveDown, moveLeft, moveRight;

    public Player(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        float speed = handler.spd;

        // Target velocity based on input
        float targetVX = 0;
        float targetVY = 0;
        if (moveUp) targetVY -= speed;
        if (moveDown) targetVY += speed;
        if (moveLeft) targetVX -= speed;
        if (moveRight) targetVX += speed;

        // Diagonal normalization — prevent faster diagonal movement
        if (targetVX != 0 && targetVY != 0) {
            targetVX *= 0.707f;
            targetVY *= 0.707f;
        }

        // Lerp toward target (accelerate when input, decelerate when no input)
        float lerpX = targetVX != 0 ? ACCEL : DECEL;
        float lerpY = targetVY != 0 ? ACCEL : DECEL;
        velX += (targetVX - velX) * lerpX;
        velY += (targetVY - velY) * lerpY;

        // Snap to zero when very close (prevents micro-drift)
        if (Math.abs(velX) < 0.1f && targetVX == 0) velX = 0;
        if (Math.abs(velY) < 0.1f && targetVY == 0) velY = 0;

        x += velX;
        y += velY;
        x = Game.clamp(x, 0, Game.WIDTH - 37);
        y = Game.clamp(y, 0, Game.HEIGHT - 60);

        if (++trailTick % 3 == 0) {
            handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.045f, handler));
        }
        collision();
    }

    private boolean wasColliding = false;

    private void collision() {
        boolean colliding = false;
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject tempObject = handler.getObjects().get(i);
            if (tempObject.getId() == ID.BasicEnemy || tempObject.getId() == ID.FastEnemy
                    || tempObject.getId() == ID.SmartEnemy || tempObject.getId() == ID.HardEnemy
                    || tempObject.getId() == ID.EnemyBoss) {
                if (getBounds().intersects(tempObject.getBounds())) {
                    HUD.HEALTH -= 2;
                    colliding = true;
                }
            }
        }
        if (colliding && !wasColliding) {
            Game.triggerHit();
        }
        wasColliding = colliding;
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;

        g2.setColor(GLOW);
        g2.fillRoundRect(ix - 5, iy - 5, SIZE + 10, SIZE + 10, R + 5, R + 5);
        g2.setColor(GLOW_INNER);
        g2.fillRoundRect(ix - 2, iy - 2, SIZE + 4, SIZE + 4, R + 2, R + 2);
        g2.setColor(FILL);
        g2.fillRoundRect(ix, iy, SIZE, SIZE, R, R);
    }
}
