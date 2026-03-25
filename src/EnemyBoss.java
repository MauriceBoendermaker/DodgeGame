import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Random;

public class EnemyBoss extends GameObject {

    private static final int SIZE = 96;
    private static final int R = 16;
    private static final Color FILL = new Color(235, 87, 87);
    private static final Color GLOW = new Color(235, 87, 87, 25);
    private static final Color BORDER_COLOR = new Color(255, 120, 120);

    private Handler handler;
    private Random r = new Random();
    private int timer = 80;
    private int timer2 = 50;
    private float pulsePhase = 0;

    public EnemyBoss(int x, int y, ID id, Handler handler) {
        super(x, y, id);
        this.handler = handler;
        velX = 0;
        velY = 2;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, SIZE, SIZE);
    }

    public void tick() {
        x += velX;
        y += velY;
        pulsePhase += 0.08f;

        if (timer <= 0) velY = 0;
        else timer--;

        if (timer <= 0) timer2--;
        if (timer2 <= 0) {
            if (velX == 0) velX = 2;
            if (velX > 0) velX += 0.005f;
            else if (velX < 0) velX -= 0.005f;
            velX = Game.clamp(velX, -10, 10);

            int spawn = r.nextInt(20);
            if (spawn == 0) {
                handler.addObject(new EnemyBossBullet((int) x + 48, (int) y + 48, ID.BasicEnemy, handler));
            }
        }

        if (x <= 0 || x >= Game.WIDTH - SIZE) velX *= -1;
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int ix = (int) x, iy = (int) y;

        // Pulsing outer glow
        float pulse = (float) (Math.sin(pulsePhase) * 0.5 + 0.5);
        int glowSize = 8 + (int) (4 * pulse);
        g2.setColor(new Color(235, 87, 87, 15 + (int) (15 * pulse)));
        g2.fillRoundRect(ix - glowSize, iy - glowSize, SIZE + glowSize * 2, SIZE + glowSize * 2, R + glowSize, R + glowSize);

        // Main shape
        g2.setColor(FILL);
        g2.fillRoundRect(ix, iy, SIZE, SIZE, R, R);

        // Pulsing border
        g2.setColor(new Color(255, 120, 120, 100 + (int) (80 * pulse)));
        g2.drawRoundRect(ix, iy, SIZE, SIZE, R, R);
    }
}
