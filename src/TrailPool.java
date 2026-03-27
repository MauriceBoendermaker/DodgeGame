import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Pre-allocated pool for trail particles. Replaces per-trail object allocation
 * with a fixed-size float array — zero GC pressure, O(1) add, single-pass render.
 */
public class TrailPool {

    private static final int MAX_TRAILS = 512;

    // Per-trail layout: [x, y, width, height, alpha, life, r, g, b, shape]
    private static final int X = 0, Y = 1, W = 2, H = 3, ALPHA = 4, LIFE = 5,
            R = 6, G = 7, B = 8, SHAPE = 9;
    private static final int FIELDS = 10;

    private static final float[][] pool = new float[MAX_TRAILS][FIELDS];

    // Pre-cached AlphaComposite instances (0..255)
    private static final AlphaComposite[] ALPHA_CACHE = new AlphaComposite[256];
    static {
        for (int i = 0; i < 256; i++) {
            ALPHA_CACHE[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / 255f);
        }
    }
    private static final AlphaComposite OPAQUE = ALPHA_CACHE[255];

    // Shape constants (match Trail.java)
    public static final int SHAPE_ROUND_RECT = 0;
    public static final int SHAPE_DIAMOND    = 1;
    public static final int SHAPE_CIRCLE     = 2;
    public static final int SHAPE_TRIANGLE   = 3;

    // Scan cursor — start searching for empty slots from here to avoid O(n) worst-case
    private static int cursor = 0;

    public static void add(float x, float y, Color color, int width, int height, float life, int shape) {
        for (int j = 0; j < MAX_TRAILS; j++) {
            int i = (cursor + j) % MAX_TRAILS;
            if (pool[i][ALPHA] <= 0) {
                float[] t = pool[i];
                t[X] = x; t[Y] = y;
                t[W] = width; t[H] = height;
                t[ALPHA] = 1f;
                t[LIFE] = life;
                t[R] = color.getRed();
                t[G] = color.getGreen();
                t[B] = color.getBlue();
                t[SHAPE] = shape;
                cursor = (i + 1) % MAX_TRAILS;
                return;
            }
        }
        // Pool full — oldest trail gets overwritten (graceful degradation)
        float[] t = pool[cursor];
        t[X] = x; t[Y] = y;
        t[W] = width; t[H] = height;
        t[ALPHA] = 1f;
        t[LIFE] = life;
        t[R] = color.getRed();
        t[G] = color.getGreen();
        t[B] = color.getBlue();
        t[SHAPE] = shape;
        cursor = (cursor + 1) % MAX_TRAILS;
    }

    public static void add(float x, float y, Color color, int width, int height, float life) {
        add(x, y, color, width, height, life, SHAPE_ROUND_RECT);
    }

    public static void tick() {
        for (int i = 0; i < MAX_TRAILS; i++) {
            float[] t = pool[i];
            if (t[ALPHA] > 0) {
                if (t[ALPHA] > t[LIFE]) {
                    t[ALPHA] -= (t[LIFE] - 0.0001f);
                } else {
                    t[ALPHA] = 0;
                }
            }
        }
    }

    public static void render(Graphics2D g) {
        for (int i = 0; i < MAX_TRAILS; i++) {
            float[] t = pool[i];
            if (t[ALPHA] <= 0) continue;

            int alphaIdx = Math.min(255, (int) (t[ALPHA] * 255));
            g.setComposite(ALPHA_CACHE[alphaIdx]);
            g.setColor(new Color((int) t[R], (int) t[G], (int) t[B]));

            int shrink = (int) ((1f - t[ALPHA]) * 4);
            int dx = (int) t[X] + shrink;
            int dy = (int) t[Y] + shrink;
            int dw = (int) t[W] - shrink * 2;
            int dh = (int) t[H] - shrink * 2;

            if (dw <= 0 || dh <= 0) continue;

            int cx = dx + dw / 2;
            int cy = dy + dh / 2;
            int half = dw / 2;

            switch ((int) t[SHAPE]) {
                case SHAPE_DIAMOND:
                    g.fillPolygon(
                            new int[]{cx, cx + half, cx, cx - half},
                            new int[]{cy - half, cy, cy + half, cy}, 4);
                    break;
                case SHAPE_CIRCLE:
                    g.fillOval(dx, dy, dw, dh);
                    break;
                case SHAPE_TRIANGLE:
                    g.fillPolygon(
                            new int[]{cx, cx + half, cx - half},
                            new int[]{cy - half, cy + half, cy + half}, 3);
                    break;
                default:
                    g.fillRoundRect(dx, dy, dw, dh, 8, 8);
                    break;
            }
        }
        g.setComposite(OPAQUE);
    }

    public static void clear() {
        for (int i = 0; i < MAX_TRAILS; i++) {
            pool[i][ALPHA] = 0;
        }
        cursor = 0;
    }
}
