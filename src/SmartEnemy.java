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
    private int trailTick = 0;
    private float pulsePhase = 0;
    private final Rectangle boundsRect = new Rectangle();
    private float hp = -1;

    public SmartEnemy(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        java.util.List<GameObject> objects = handler.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            if (objects.get(i).getId() == ID.Player) {
                player = objects.get(i);
            }
        }
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
                    ID.Trail, FILL, SIZE / 2, SIZE / 2, 0.05f, handler, Trail.SHAPE_CIRCLE));
        }
        Game.addEnemyKilled();
        Game.addKillBonus(125);
        handler.removeObject(this);
    }

    public Rectangle getBounds() {
        boundsRect.setBounds((int) x, (int) y, SIZE, SIZE);
        return boundsRect;
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

        // Pulse faster when closer to player
        float closeness = Math.max(0, 1f - distance / 300f);
        pulsePhase += 0.08f + closeness * 0.12f;

        if (++trailTick % 4 == 0)
            handler.addObject(new Trail(x, y, ID.Trail, TRAIL_COLOR, SIZE, SIZE, 0.06f, handler, Trail.SHAPE_CIRCLE));
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;
        int cx = ix + SIZE / 2, cy = iy + SIZE / 2;

        // Breathing pulse — size oscillates, faster when close to player
        float pulse = (float) (Math.sin(pulsePhase) * 0.5 + 0.5);
        int breathe = (int) (pulse * 4);

        // Outer glow — breathes
        int glowSize = SIZE + 8 + breathe * 2;
        g2.setColor(new Color(GLOW.getRed(), GLOW.getGreen(), GLOW.getBlue(),
                25 + (int) (pulse * 20)));
        g2.fillOval(cx - glowSize / 2, cy - glowSize / 2, glowSize, glowSize);

        // Main circle — breathes
        int mainSize = SIZE + breathe;
        g2.setColor(FILL);
        g2.fillOval(cx - mainSize / 2, cy - mainSize / 2, mainSize, mainSize);

        // Inner ring — rotating crosshair that tracks the player
        float trackAngle = (float) Math.atan2(player.getY() + 24 - cy, player.getX() + 24 - cx);
        int innerR = SIZE / 2 - 4;
        g2.setColor(new Color(255, 255, 255, 40 + (int) (pulse * 40)));
        for (int i = 0; i < 4; i++) {
            float a = trackAngle + i * (float) (Math.PI / 2);
            int dx = (int) (Math.cos(a) * innerR);
            int dy = (int) (Math.sin(a) * innerR);
            g2.drawLine(cx + dx / 2, cy + dy / 2, cx + dx, cy + dy);
        }
    }
}
