import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class PageRenderer {

    // -- Dark theme palette --
    public static final Color BG_DARK = new Color(10, 12, 18);
    public static final Color BG_MID = new Color(16, 22, 34);
    public static final Color SURFACE = new Color(22, 30, 44);
    public static final Color SURFACE_LIGHT = new Color(30, 40, 56);
    public static final Color BORDER = new Color(40, 52, 70);
    public static final Color ACCENT = new Color(78, 205, 196);
    public static final Color ACCENT_DIM = new Color(78, 205, 196, 40);
    public static final Color DANGER = new Color(235, 87, 87);
    public static final Color WARNING = new Color(245, 195, 68);
    public static final Color SUCCESS = new Color(72, 199, 142);
    public static final Color TEXT = new Color(230, 234, 240);
    public static final Color TEXT_SEC = new Color(140, 150, 165);
    public static final Color TEXT_MUTED = new Color(80, 92, 108);

    public static final int R = 12; // standard corner radius

    // -- Fonts --
    public static final Font LOGO_FONT = new Font("Arial", Font.BOLD, 92);
    public static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 52);
    public static final Font SUBTITLE_FONT = new Font("Arial", Font.PLAIN, 24);
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 20);
    public static final Font HEADING_FONT = new Font("Arial", Font.BOLD, 24);
    public static final Font BODY_FONT = new Font("Arial", Font.PLAIN, 17);
    public static final Font SMALL_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 13);

    // -- Button dimensions --
    public static final int BTN_W = 280;
    public static final int BTN_H = 50;
    public static final int BTN_SPACING = 66;

    // Back button (fixed size, dynamic x)
    public static final int BACK_Y = 24;
    public static final int BACK_W = 90;
    public static final int BACK_H = 34;

    // Dynamic positions — depend on Game.WIDTH which adapts to screen
    public static int btnX() { return (Game.WIDTH - BTN_W) / 2; }
    public static int backX() { return Game.WIDTH - 125; }

    private static BufferedImage bgCache;

    // ===== Background =====

    public static void drawBackground(Graphics2D g) {
        if (bgCache == null) {
            bgCache = createBackground();
        }
        g.drawImage(bgCache, 0, 0, null);
    }

    private static BufferedImage createBackground() {
        BufferedImage img = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Base gradient — deep dark diagonal
        g.setPaint(new GradientPaint(0, 0, BG_DARK, Game.WIDTH, Game.HEIGHT, BG_MID));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        int cx = Game.WIDTH / 2;

        // Large subtle teal glow — center
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, 340), 600,
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(30, 80, 78, 35), new Color(20, 50, 55, 15), new Color(0, 0, 0, 0)}
        ));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        // Small accent highlight — upper area
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, 120), 350,
                new float[]{0f, 1f},
                new Color[]{new Color(78, 205, 196, 12), new Color(78, 205, 196, 0)}
        ));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        g.dispose();
        return img;
    }

    // ===== Logo =====

    public static void drawLogo(Graphics2D g, int centerY) {
        g.setFont(LOGO_FONT);
        FontMetrics fm = g.getFontMetrics();
        String base = "Dotch";
        String dot = ".";
        int baseW = fm.stringWidth(base);
        int dotW = fm.stringWidth(dot);
        int totalW = baseW + dotW;
        int x = (Game.WIDTH - totalW) / 2;

        g.setColor(TEXT);
        g.drawString(base, x, centerY);
        g.setColor(ACCENT);
        g.drawString(dot, x + baseW, centerY);

        // Accent underline
        int lineW = 100;
        int lineX = (Game.WIDTH - lineW) / 2;
        g.fillRoundRect(lineX, centerY + 12, lineW, 3, 3, 3);
    }

    // ===== Title (sub-pages) =====

    public static void drawTitle(Graphics2D g, String title) {
        g.setFont(TITLE_FONT);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(title);
        g.setColor(TEXT);
        g.drawString(title, (Game.WIDTH - w) / 2, 80);

        // Subtle accent line
        int lineW = 60;
        g.setColor(ACCENT);
        g.fillRoundRect((Game.WIDTH - lineW) / 2, 92, lineW, 2, 2, 2);
    }

    // Hover-enhanced colors
    private static final Color ACCENT_BRIGHT = new Color(110, 225, 218);
    private static final Color SURFACE_HOVER = new Color(38, 50, 68);
    private static final Color DANGER_BRIGHT = new Color(255, 120, 120);
    private static final Color WARNING_BRIGHT = new Color(255, 220, 100);

    // ===== Buttons (all accept hover 0.0-1.0) =====

    public static void drawPrimaryButton(Graphics2D g, int x, int y, int w, int h, String text, float hover) {
        g.setColor(lerp(ACCENT, ACCENT_BRIGHT, hover));
        g.fillRoundRect(x, y, w, h, R, R);
        g.setFont(BUTTON_FONT);
        g.setColor(BG_DARK);
        drawCenteredString(g, text, x, y, w, h);
    }

    public static void drawSecondaryButton(Graphics2D g, int x, int y, int w, int h, String text, float hover) {
        g.setColor(lerp(SURFACE, SURFACE_HOVER, hover));
        g.fillRoundRect(x, y, w, h, R, R);
        g.setColor(lerp(BORDER, ACCENT, hover * 0.5f));
        g.drawRoundRect(x, y, w, h, R, R);
        g.setFont(BUTTON_FONT);
        g.setColor(lerp(TEXT, ACCENT_BRIGHT, hover * 0.3f));
        drawCenteredString(g, text, x, y, w, h);
    }

    public static void drawDangerButton(Graphics2D g, int x, int y, int w, int h, String text, float hover) {
        g.setColor(lerp(new Color(235, 87, 87, 15), new Color(235, 87, 87, 40), hover));
        g.fillRoundRect(x, y, w, h, R, R);
        g.setColor(lerp(DANGER, DANGER_BRIGHT, hover));
        g.drawRoundRect(x, y, w, h, R, R);
        g.setFont(BUTTON_FONT);
        g.setColor(lerp(DANGER, DANGER_BRIGHT, hover));
        drawCenteredString(g, text, x, y, w, h);
    }

    public static void drawWarningButton(Graphics2D g, int x, int y, int w, int h, String text, float hover) {
        g.setColor(lerp(new Color(245, 195, 68, 15), new Color(245, 195, 68, 40), hover));
        g.fillRoundRect(x, y, w, h, R, R);
        g.setColor(lerp(WARNING, WARNING_BRIGHT, hover));
        g.drawRoundRect(x, y, w, h, R, R);
        g.setFont(BUTTON_FONT);
        g.setColor(lerp(WARNING, WARNING_BRIGHT, hover));
        drawCenteredString(g, text, x, y, w, h);
    }

    public static void drawBackButton(Graphics2D g, float hover) {
        int bx = backX();
        g.setColor(lerp(SURFACE, SURFACE_HOVER, hover));
        g.fillRoundRect(bx, BACK_Y, BACK_W, BACK_H, 8, 8);
        g.setColor(lerp(BORDER, ACCENT, hover * 0.4f));
        g.drawRoundRect(bx, BACK_Y, BACK_W, BACK_H, 8, 8);
        g.setFont(SMALL_FONT);
        g.setColor(lerp(TEXT_SEC, TEXT, hover));
        drawCenteredString(g, "Back", bx, BACK_Y, BACK_W, BACK_H);
    }

    // ===== Utility =====

    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    // ===== Panel =====

    public static void drawPanel(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(SURFACE);
        g.fillRoundRect(x, y, w, h, R, R);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w, h, R, R);
    }

    // ===== Utility =====

    public static void drawCenteredString(Graphics2D g, String text, int rx, int ry, int rw, int rh) {
        FontMetrics fm = g.getFontMetrics();
        int x = rx + (rw - fm.stringWidth(text)) / 2;
        int y = ry + (rh - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);
    }
}
