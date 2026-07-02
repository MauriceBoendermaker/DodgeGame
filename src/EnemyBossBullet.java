import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class EnemyBossBullet extends GameObject {

    private static final int SIZE = 16;
    private static final Color FILL = new Color(235, 87, 87);
    private static final Color GLOW = new Color(235, 87, 87, 40);
    private static final Color TRAIL_COLOR = new Color(235, 87, 87);
    private static final Color PARRY_FILL = new Color(78, 205, 196);
    private static final Color PARRY_GLOW = new Color(78, 205, 196, 60);
    private static final float PARRY_DAMAGE = 90f;

    private Handler handler;
    private int trailTick = 0;
    private final Rectangle boundsRect = new Rectangle();
    private boolean parried = false;

    public EnemyBossBullet(int x, int y, ID id, Handler handler, float vx, float vy) {
        super(x, y, id);
        this.handler = handler;
        velX = vx;
        velY = vy;
    }

    public boolean isParried() { return parried; }

    public void parry() {
        parried = true;
        // Reverse and speed up
        velX *= -1.5f;
        velY *= -1.5f;
    }

    public Rectangle getBounds() {
        boundsRect.setBounds((int) x, (int) y, SIZE, SIZE);
        return boundsRect;
    }

    public void tick() {
        x += velX;
        y += velY;
        // Remove if off-screen
        if (y >= Game.HEIGHT + 50 || y < -50 || x < -50 || x > Game.WIDTH + 50) {
            handler.removeObject(this);
            return;
        }

        Color tc = parried ? PARRY_FILL : TRAIL_COLOR;
        // Load-aware throttle (P1-A): identical under normal load, but once the shared trail pool
        // nears saturation (bullet-hell boss frames) we skip emission to cap the worst case.
        // Trails are purely cosmetic, so this never affects gameplay. trailTick still advances
        // every tick, so the emission cadence is unchanged when trails resume.
        if (++trailTick % 3 == 0 && TrailPool.liveCount() < 360)
            TrailPool.add(x, y, tc, SIZE, SIZE, 0.03f, TrailPool.SHAPE_CIRCLE);

        // Parried bullets damage bosses on contact
        if (parried) {
            checkBossCollision();
        }
    }

    private void checkBossCollision() {
        java.util.List<GameObject> objects = handler.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            GameObject obj = objects.get(i);
            if (obj.getId() != ID.EnemyBoss) continue;
            if (obj instanceof EnemyBossBullet) continue;
            if (getBounds().intersects(obj.getBounds())) {
                obj.takeDamage(PARRY_DAMAGE);
                Game.triggerScreenShake(6f);
                // Spawn impact particles
                Color accent = GamePalette.accent();
                for (int j = 0; j < 6; j++) {
                    float angle = (float) (Math.random() * Math.PI * 2);
                    TrailPool.add(
                            x + (float) Math.cos(angle) * 12,
                            y + (float) Math.sin(angle) * 12,
                            accent, 8, 8, 0.06f, TrailPool.SHAPE_CIRCLE);
                }
                handler.removeObject(this);
                return;
            }
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;

        if (parried) {
            g2.setColor(PARRY_GLOW);
            g2.fillOval(ix - 4, iy - 4, SIZE + 8, SIZE + 8);
            g2.setColor(PARRY_FILL);
            g2.fillOval(ix, iy, SIZE, SIZE);
            // Bright core
            g2.setColor(new Color(230, 255, 250));
            g2.fillOval(ix + 3, iy + 3, SIZE - 6, SIZE - 6);
        } else {
            g2.setColor(GLOW);
            g2.fillOval(ix - 3, iy - 3, SIZE + 6, SIZE + 6);
            g2.setColor(FILL);
            g2.fillOval(ix, iy, SIZE, SIZE);
        }
    }
}
