import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

public class Trail extends GameObject {

    public static final int SHAPE_ROUND_RECT = 0;
    public static final int SHAPE_DIAMOND = 1;
    public static final int SHAPE_CIRCLE = 2;
    public static final int SHAPE_TRIANGLE = 3;
    private static final AlphaComposite OPAQUE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);

    private float alpha = 1;
    private float life;
    private Handler handler;
    private Color color;
    private int width, height;
    private int shape;

    public Trail(float x, float y, ID id, Color color, int width, int height, float life, Handler handler) {
        this(x, y, id, color, width, height, life, handler, SHAPE_ROUND_RECT);
    }

    public Trail(float x, float y, ID id, Color color, int width, int height, float life, Handler handler, int shape) {
        super(x, y, id);
        this.handler = handler;
        this.color = color;
        this.width = width;
        this.height = height;
        this.life = life;
        this.shape = shape;
    }

    public void tick() {
        if (alpha > life) {
            alpha -= (life - 0.0001f);
        } else {
            handler.removeObject(this);
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(color);

        int shrink = (int) ((1f - alpha) * 4);
        int dx = (int) x + shrink;
        int dy = (int) y + shrink;
        int dw = width - shrink * 2;
        int dh = height - shrink * 2;

        if (dw <= 0 || dh <= 0) {
            g2.setComposite(OPAQUE);
            return;
        }

        int cx = dx + dw / 2;
        int cy = dy + dh / 2;
        int half = dw / 2;

        switch (shape) {
            case SHAPE_DIAMOND:
                g2.fillPolygon(
                        new int[]{cx, cx + half, cx, cx - half},
                        new int[]{cy - half, cy, cy + half, cy}, 4);
                break;
            case SHAPE_CIRCLE:
                g2.fillOval(dx, dy, dw, dh);
                break;
            case SHAPE_TRIANGLE:
                g2.fillPolygon(
                        new int[]{cx, cx + half, cx - half},
                        new int[]{cy - half, cy + half, cy + half}, 3);
                break;
            default:
                g2.fillRoundRect(dx, dy, dw, dh, 8, 8);
                break;
        }

        g2.setComposite(OPAQUE);
    }

    public Rectangle getBounds() {
        return null;
    }
}
