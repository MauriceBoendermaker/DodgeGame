import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class FastEnemy extends GameObject {

    private static final int SIZE = 32;
    private static final Color FILL = new Color(78, 205, 196);
    private static final Color GLOW = new Color(78, 205, 196, 35);
    private static final Color TRAIL_COLOR = new Color(78, 205, 196);

    private Handler handler;
    private int trailTick = 0;
    private float rotation = 0;
    private final Rectangle boundsRect = new Rectangle();
    private float hp = -1;

    public FastEnemy(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        velX = 2;
        velY = 9;
    }

    public void setCombatHp(float hp) { this.hp = hp; }

    @Override
    public boolean takeDamage(float dmg) {
        if (hp < 0) return false;
        hp -= dmg;
        if (hp <= 0) { die(); return true; }
        return true;
    }

    private void die() {
        for (int i = 0; i < 6; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            handler.addObject(new Trail(
                    x + SIZE / 2 + (float) Math.cos(angle) * 8,
                    y + SIZE / 2 + (float) Math.sin(angle) * 8,
                    ID.Trail, FILL, SIZE / 2, SIZE / 2, 0.05f, handler, Trail.SHAPE_DIAMOND));
        }
        Game.addEnemyKilled();
        Game.addKillBonus(75);
        handler.removeObject(this);
    }

    public Rectangle getBounds() {
        boundsRect.setBounds((int) x, (int) y, SIZE, SIZE);
        return boundsRect;
    }

    public void tick() {
        x += velX;
        y += velY;

        // Spin along velocity — faster movement = faster spin
        float speed = (float) Math.sqrt(velX * velX + velY * velY);
        rotation += speed * 0.04f;

        if (y <= 0 || y >= Game.HEIGHT - SIZE) {
            Game.wallHit(x + SIZE / 2, y <= 0 ? 0 : Game.HEIGHT, y <= 0 ? 0 : 1, 78, 205, 196);
            velY *= -1;
        }
        if (x <= 0 || x >= Game.WIDTH - SIZE) {
            Game.wallHit(x <= 0 ? 0 : Game.WIDTH, y + SIZE / 2, x <= 0 ? 2 : 3, 78, 205, 196);
            velX *= -1;
        }
        if (++trailTick % 3 == 0)
            handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.02f, handler, Trail.SHAPE_DIAMOND));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + SIZE / 2;
        int cy = (int) y + SIZE / 2;
        int half = SIZE / 2;

        // Rotated diamond
        g2.setColor(GLOW);
        drawRotatedDiamond(g2, cx, cy, half + 4, rotation);
        g2.setColor(FILL);
        drawRotatedDiamond(g2, cx, cy, half, rotation);
    }

    private void drawRotatedDiamond(Graphics2D g, int cx, int cy, int r, float angle) {
        int[] xp = new int[4];
        int[] yp = new int[4];
        for (int i = 0; i < 4; i++) {
            float a = angle + i * (float) (Math.PI / 2);
            xp[i] = cx + (int) (Math.cos(a) * r);
            yp[i] = cy + (int) (Math.sin(a) * r);
        }
        g.fillPolygon(xp, yp, 4);
    }
}
