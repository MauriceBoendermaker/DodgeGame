import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Random;

/**
 * Small bouncing enemy spawned by BossSwarm. Some shoot bullets.
 * Uses ID.BasicEnemy for collision (deals standard damage).
 */
public class SwarmMinion extends GameObject {

    private static final int SIZE = 20;
    private static final Color FILL = new Color(245, 170, 60);
    private static final Color GLOW = new Color(245, 170, 60, 30);
    private static final Color TRAIL_COLOR = new Color(245, 170, 60);

    private Handler handler;
    private Random r = new Random();
    private int trailTick = 0;
    private final Rectangle boundsRect = new Rectangle();
    private boolean canShoot;
    private int shootCooldown;
    private float rotation = 0;

    public SwarmMinion(int x, int y, Handler handler, boolean canShoot) {
        super(x, y, ID.BasicEnemy);
        this.handler = handler;
        this.canShoot = canShoot;
        this.shootCooldown = 120 + r.nextInt(80);

        // Random bounce direction
        float speed = 3f + r.nextFloat() * 3f;
        float angle = r.nextFloat() * (float) (Math.PI * 2);
        velX = (float) Math.cos(angle) * speed;
        velY = (float) Math.sin(angle) * speed;
    }

    public Rectangle getBounds() {
        boundsRect.setBounds((int) x, (int) y, SIZE, SIZE);
        return boundsRect;
    }

    public void tick() {
        x += velX;
        y += velY;
        rotation += 0.08f;

        // Bounce
        if (y <= 0 || y >= Game.HEIGHT - SIZE) {
            velY *= -1;
            if (y <= 0) y = 0;
            if (y >= Game.HEIGHT - SIZE) y = Game.HEIGHT - SIZE;
        }
        if (x <= 0 || x >= Game.WIDTH - SIZE) {
            velX *= -1;
            if (x <= 0) x = 0;
            if (x >= Game.WIDTH - SIZE) x = Game.WIDTH - SIZE;
        }

        if (++trailTick % 5 == 0) {
            handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.03f, handler, Trail.SHAPE_DIAMOND));
        }

        // Shoot
        if (canShoot) {
            shootCooldown--;
            if (shootCooldown <= 0) {
                shootCooldown = 140 + r.nextInt(60);
                // Find player and shoot toward them
                for (int i = 0; i < handler.getObjects().size(); i++) {
                    if (handler.getObjects().get(i).getId() == ID.Player) {
                        GameObject player = handler.getObjects().get(i);
                        float dx = player.getX() + 24 - (x + SIZE / 2);
                        float dy = player.getY() + 24 - (y + SIZE / 2);
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        if (dist > 0) {
                            float speed = 3.5f;
                            handler.addObject(new EnemyBossBullet(
                                    (int) (x + SIZE / 2), (int) (y + SIZE / 2), ID.BasicEnemy, handler,
                                    dx / dist * speed, dy / dist * speed));
                        }
                        break;
                    }
                }
            }
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + SIZE / 2;
        int cy = (int) y + SIZE / 2;
        int half = SIZE / 2;

        // Rotated diamond (like FastEnemy but smaller)
        g2.setColor(GLOW);
        drawDiamond(g2, cx, cy, half + 3);
        g2.setColor(FILL);
        drawDiamond(g2, cx, cy, half);

        // Shoot indicator
        if (canShoot) {
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillOval(cx - 2, cy - 2, 4, 4);
        }
    }

    private void drawDiamond(Graphics2D g, int cx, int cy, int r) {
        int[] xp = new int[4];
        int[] yp = new int[4];
        for (int i = 0; i < 4; i++) {
            float a = rotation + i * (float) (Math.PI / 2);
            xp[i] = cx + (int) (Math.cos(a) * r);
            yp[i] = cy + (int) (Math.sin(a) * r);
        }
        g.fillPolygon(xp, yp, 4);
    }
}
