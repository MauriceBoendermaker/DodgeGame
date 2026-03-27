import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class PlayerProjectile extends GameObject {

    private static final int SIZE = 10;

    private Handler handler;
    private float damage;
    private int trailTick = 0;
    private final Rectangle boundsRect = new Rectangle();

    public PlayerProjectile(float x, float y, ID id, Handler handler, float vx, float vy, float damage) {
        super(x, y, id);
        this.handler = handler;
        this.damage = damage;
        velX = vx;
        velY = vy;
    }

    public Rectangle getBounds() {
        boundsRect.setBounds((int) x, (int) y, SIZE, SIZE);
        return boundsRect;
    }

    public void tick() {
        x += velX;
        y += velY;

        // Remove if off-screen
        if (x < -50 || x > Game.WIDTH + 50 || y < -50 || y > Game.HEIGHT + 50) {
            handler.removeObject(this);
            return;
        }

        // Trail
        Color accent = GamePalette.accent();
        Color trailCol = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180);
        if (++trailTick % 2 == 0) {
            handler.addObject(new Trail(x, y, ID.Trail, trailCol, SIZE, SIZE, 0.04f, handler, Trail.SHAPE_CIRCLE));
        }

        // Collision with enemies
        checkCollision();
    }

    private void checkCollision() {
        java.util.List<GameObject> objects = handler.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            GameObject obj = objects.get(i);
            ID oid = obj.getId();
            // Only hit enemies
            if (oid != ID.BasicEnemy && oid != ID.FastEnemy
                    && oid != ID.SmartEnemy && oid != ID.HardEnemy
                    && oid != ID.EnemyBoss) continue;
            // Skip other projectiles (EnemyBossBullet uses ID.BasicEnemy but isn't a real enemy)
            if (obj instanceof EnemyBossBullet) continue;
            if (obj instanceof PlayerProjectile) continue;

            if (getBounds().intersects(obj.getBounds())) {
                if (obj.takeDamage(damage)) {
                    // Spawn hit particles
                    spawnHitParticles(obj);
                    handler.removeObject(this);
                    return;
                }
            }
        }
    }

    private void spawnHitParticles(GameObject target) {
        Color accent = GamePalette.accent();
        float cx = target.getX() + 16;
        float cy = target.getY() + 16;
        for (int i = 0; i < 4; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float speed = 2f + (float) Math.random() * 3f;
            handler.addObject(new Trail(
                    cx + (float) Math.cos(angle) * 8,
                    cy + (float) Math.sin(angle) * 8,
                    ID.Trail, accent, 6, 6, 0.06f, handler, Trail.SHAPE_CIRCLE));
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;
        Color accent = GamePalette.accent();

        // Outer glow
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
        g2.fillOval(ix - 3, iy - 3, SIZE + 6, SIZE + 6);
        // Core
        g2.setColor(new Color(230, 234, 240));
        g2.fillOval(ix, iy, SIZE, SIZE);
        // Inner accent
        g2.setColor(accent);
        g2.fillOval(ix + 2, iy + 2, SIZE - 4, SIZE - 4);
    }
}
