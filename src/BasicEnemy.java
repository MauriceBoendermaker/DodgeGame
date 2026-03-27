import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class BasicEnemy extends GameObject {

    private static final int SIZE = 32;
    private static final int R = 8;
    private static final Color FILL = new Color(235, 87, 87);
    private static final Color GLOW = new Color(235, 87, 87, 35);
    private static final Color TRAIL_COLOR = new Color(235, 87, 87);

    private Handler handler;
    private int trailTick = 0;
    private final Rectangle boundsRect = new Rectangle();
    private float hp = -1; // -1 = no HP (dodge mode)

    public BasicEnemy(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        velX = 5;
        velY = 5;
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
            TrailPool.add(
                    x + SIZE / 2 + (float) Math.cos(angle) * 8,
                    y + SIZE / 2 + (float) Math.sin(angle) * 8,
                    FILL, SIZE / 2, SIZE / 2, 0.05f);
        }
        Game.addEnemyKilled();
        Game.addKillBonus(50);
        handler.removeObject(this);
    }

    public Rectangle getBounds() {
        boundsRect.setBounds((int) x, (int) y, SIZE, SIZE);
        return boundsRect;
    }

    public void tick() {
        x += velX;
        y += velY;
        if (y <= 0 || y >= Game.HEIGHT - SIZE) {
            Game.wallHit(x + SIZE / 2, y <= 0 ? 0 : Game.HEIGHT, y <= 0 ? 0 : 1, 235, 87, 87);
            velY *= -1;
        }
        if (x <= 0 || x >= Game.WIDTH - SIZE) {
            Game.wallHit(x <= 0 ? 0 : Game.WIDTH, y + SIZE / 2, x <= 0 ? 2 : 3, 235, 87, 87);
            velX *= -1;
        }
        if (++trailTick % 3 == 0)
            TrailPool.add(x, y, TRAIL_COLOR, SIZE, SIZE, 0.02f);
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;
        g2.setColor(GLOW);
        g2.fillRoundRect(ix - 4, iy - 4, SIZE + 8, SIZE + 8, R + 4, R + 4);
        g2.setColor(FILL);
        g2.fillRoundRect(ix, iy, SIZE, SIZE, R, R);
    }
}
