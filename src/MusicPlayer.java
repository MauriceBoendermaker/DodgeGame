import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MusicPlayer extends MouseAdapter {

    // Dynamic positions
    private static int cx() { return Game.WIDTH / 2; }
    private static int progX() { return (Game.WIDTH - PROG_W) / 2; }
    private static int volX() { return (Game.WIDTH - VOL_W) / 2; }

    // Fixed dimensions
    private static final int PROG_Y = 430;
    private static final int PROG_W = 800;
    private static final int PROG_H = 5;
    private static final int PROG_KNOB = 14;
    private static final int CTRL_Y = 490;
    private static final int CTRL_SIZE = 44;
    private static final int PLAY_SIZE = 56;
    private static final int VOL_Y = 580;
    private static final int VOL_W = 300;
    private static final int VOL_H = 5;
    private static final int VOL_KNOB = 12;
    private static final int DRAG_MARGIN = 16;
    private static final int CTRL_SPACING = 80;

    // Button indices
    private static final int BTN_PREV = 0;
    private static final int BTN_PLAY = 1;
    private static final int BTN_NEXT = 2;
    private static final int BTN_BACK = 3;
    private static final int BTN_COUNT = 4;

    // Hover animation state
    private float[] hover = new float[BTN_COUNT];
    private float progBarHover = 0;
    private float volBarHover = 0;

    // Mouse tracking
    private int mouseX, mouseY;

    // Drag state
    private enum DragTarget { NONE, PROGRESS, VOLUME }
    private DragTarget dragging = DragTarget.NONE;

    // Hover colors
    private static final Color CTRL_BG = PageRenderer.SURFACE_LIGHT;
    private static final Color CTRL_BG_HOVER = new Color(48, 62, 82);
    private static final Color CTRL_ICON = PageRenderer.TEXT_SEC;
    private static final Color CTRL_ICON_HOVER = PageRenderer.TEXT;
    private static final Color PLAY_BG_ACTIVE = PageRenderer.ACCENT;
    private static final Color PLAY_BG_ACTIVE_HOVER = new Color(110, 225, 218);

    public void tick() {
        // Music auto-advance is handled globally in Game.tick()

        if (Game.gameState != Game.STATE.MusicPlayer) return;

        // Update hover animations
        boolean[] targets = getHoverTargets();
        for (int i = 0; i < BTN_COUNT; i++) {
            float target = targets[i] ? 1f : 0f;
            hover[i] += (target - hover[i]) * 0.14f;
            if (Math.abs(hover[i] - target) < 0.01f) hover[i] = target;
        }

        // Bar hover
        boolean onProg = dragging == DragTarget.PROGRESS
                || hitBar(mouseX, mouseY, progX(), PROG_Y, PROG_W);
        boolean onVol = dragging == DragTarget.VOLUME
                || hitBar(mouseX, mouseY, volX(), VOL_Y, VOL_W);
        progBarHover += ((onProg ? 1f : 0f) - progBarHover) * 0.14f;
        volBarHover += ((onVol ? 1f : 0f) - volBarHover) * 0.14f;
    }

    private boolean[] getHoverTargets() {
        boolean[] t = new boolean[BTN_COUNT];
        int cxv = cx();
        int playCy = CTRL_Y + CTRL_SIZE / 2 - (PLAY_SIZE - CTRL_SIZE) / 2 + PLAY_SIZE / 2;
        t[BTN_PREV] = inCircle(mouseX, mouseY, cxv - CTRL_SPACING, CTRL_Y + CTRL_SIZE / 2, CTRL_SIZE / 2 + 4);
        t[BTN_PLAY] = inCircle(mouseX, mouseY, cxv, playCy, PLAY_SIZE / 2 + 4);
        t[BTN_NEXT] = inCircle(mouseX, mouseY, cxv + CTRL_SPACING, CTRL_Y + CTRL_SIZE / 2, CTRL_SIZE / 2 + 4);
        t[BTN_BACK] = mouseX >= PageRenderer.backX() && mouseX <= PageRenderer.backX() + PageRenderer.BACK_W
                && mouseY >= PageRenderer.BACK_Y && mouseY <= PageRenderer.BACK_Y + PageRenderer.BACK_H;
        return t;
    }

    // ==================== Rendering ====================

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        PageRenderer.drawBackground(g2);
        PageRenderer.drawTitle(g2, "Music Player");

        // Back button with hover
        int bx = PageRenderer.backX();
        g2.setColor(lerp(PageRenderer.SURFACE, CTRL_BG_HOVER, hover[BTN_BACK]));
        g2.fillRoundRect(bx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H, 8, 8);
        g2.setColor(lerp(PageRenderer.BORDER, PageRenderer.ACCENT, hover[BTN_BACK] * 0.4f));
        g2.drawRoundRect(bx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H, 8, 8);
        g2.setFont(PageRenderer.SMALL_FONT);
        g2.setColor(lerp(PageRenderer.TEXT_SEC, PageRenderer.TEXT, hover[BTN_BACK]));
        PageRenderer.drawCenteredString(g2, "Back", bx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H);

        // Album art
        int artSize = 180;
        int artX = cx() - artSize / 2;
        int artY = 130;
        PageRenderer.drawPanel(g2, artX, artY, artSize, artSize);
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 60));
        g2.setColor(PageRenderer.ACCENT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("\u266B", cx() - fm.stringWidth("\u266B") / 2, artY + artSize / 2 + 20);

        if (AudioPlayer.getTrackCount() > 0) {
            AudioPlayer.Track track = AudioPlayer.getCurrentTrack();

            // Track name
            g2.setFont(PageRenderer.HEADING_FONT);
            g2.setColor(PageRenderer.TEXT);
            fm = g2.getFontMetrics();
            g2.drawString(track.displayName, cx() - fm.stringWidth(track.displayName) / 2, 365);

            // Track number
            g2.setFont(PageRenderer.SMALL_FONT);
            g2.setColor(PageRenderer.TEXT_MUTED);
            String info = "Track " + (AudioPlayer.getCurrentTrackIndex() + 1) + " of " + AudioPlayer.getTrackCount();
            fm = g2.getFontMetrics();
            g2.drawString(info, cx() - fm.stringWidth(info) / 2, 390);

            // Progress bar
            renderProgressBar(g2, track);
        } else {
            g2.setFont(PageRenderer.HEADING_FONT);
            g2.setColor(PageRenderer.TEXT_MUTED);
            fm = g2.getFontMetrics();
            String msg = "No tracks loaded";
            g2.drawString(msg, cx() - fm.stringWidth(msg) / 2, 375);
        }

        renderControls(g2);
        renderVolumeBar(g2);
    }

    private void renderProgressBar(Graphics2D g, AudioPlayer.Track track) {
        float pos = AudioPlayer.getPosition();
        float dur = track.duration;
        float progress = (dur > 0) ? Math.min(pos / dur, 1f) : 0;
        int px = progX();

        // Bar background
        g.setColor(lerp(PageRenderer.BORDER, new Color(55, 70, 90), progBarHover));
        g.fillRoundRect(px, PROG_Y, PROG_W, PROG_H, 3, 3);

        // Filled
        int filledW = (int) (PROG_W * progress);
        if (filledW > 0) {
            g.setColor(PageRenderer.ACCENT);
            g.fillRoundRect(px, PROG_Y, filledW, PROG_H, 3, 3);
        }

        // Knob — grows slightly on hover
        int knobSize = PROG_KNOB + (int) (4 * progBarHover);
        g.setColor(lerp(PageRenderer.TEXT, PageRenderer.ACCENT, progBarHover));
        g.fillOval(px + filledW - knobSize / 2, PROG_Y + PROG_H / 2 - knobSize / 2, knobSize, knobSize);

        // Time labels
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString(formatTime(pos), px, PROG_Y + 25);
        String durStr = formatTime(dur);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(durStr, px + PROG_W - fm.stringWidth(durStr), PROG_Y + 25);
    }

    private void renderControls(Graphics2D g) {
        int cxv = cx();
        int prevX = cxv - CTRL_SPACING;
        int nextX = cxv + CTRL_SPACING;
        int cy = CTRL_Y + CTRL_SIZE / 2;

        // Previous
        drawCtrlCircle(g, prevX, CTRL_Y, CTRL_SIZE, hover[BTN_PREV], false);
        drawPrevIcon(g, prevX, cy, hover[BTN_PREV]);

        // Play/Pause
        boolean playing = AudioPlayer.isPlaying();
        int py = CTRL_Y - (PLAY_SIZE - CTRL_SIZE) / 2;
        int playCy = py + PLAY_SIZE / 2;
        if (playing) {
            g.setColor(lerp(PLAY_BG_ACTIVE, PLAY_BG_ACTIVE_HOVER, hover[BTN_PLAY]));
        } else {
            g.setColor(lerp(CTRL_BG, CTRL_BG_HOVER, hover[BTN_PLAY]));
        }
        g.fillOval(cxv - PLAY_SIZE / 2, py, PLAY_SIZE, PLAY_SIZE);
        if (!playing) {
            g.setColor(lerp(PageRenderer.BORDER, PageRenderer.ACCENT, hover[BTN_PLAY] * 0.5f));
            g.drawOval(cxv - PLAY_SIZE / 2, py, PLAY_SIZE, PLAY_SIZE);
        }
        Color iconCol = playing
                ? PageRenderer.BG_DARK
                : lerp(CTRL_ICON, PageRenderer.ACCENT, hover[BTN_PLAY]);
        if (playing) {
            drawPauseIcon(g, cxv, playCy, iconCol);
        } else {
            drawPlayIcon(g, cxv, playCy, iconCol);
        }

        // Next
        drawCtrlCircle(g, nextX, CTRL_Y, CTRL_SIZE, hover[BTN_NEXT], false);
        drawNextIcon(g, nextX, cy, hover[BTN_NEXT]);
    }

    private void drawCtrlCircle(Graphics2D g, int cx, int cy, int size, float h, boolean active) {
        g.setColor(lerp(CTRL_BG, CTRL_BG_HOVER, h));
        g.fillOval(cx - size / 2, cy, size, size);
        g.setColor(lerp(PageRenderer.BORDER, PageRenderer.ACCENT, h * 0.4f));
        g.drawOval(cx - size / 2, cy, size, size);
    }

    private void drawPlayIcon(Graphics2D g, int cx, int cy, Color c) {
        g.setColor(c);
        Polygon tri = new Polygon(
                new int[]{cx - 7, cx - 7, cx + 10},
                new int[]{cy - 12, cy + 12, cy}, 3);
        g.fillPolygon(tri);
    }

    private void drawPauseIcon(Graphics2D g, int cx, int cy, Color c) {
        g.setColor(c);
        g.fillRoundRect(cx - 9, cy - 10, 6, 20, 2, 2);
        g.fillRoundRect(cx + 3, cy - 10, 6, 20, 2, 2);
    }

    private void drawPrevIcon(Graphics2D g, int cx, int cy, float h) {
        g.setColor(lerp(CTRL_ICON, CTRL_ICON_HOVER, h));
        // Bar
        g.fillRect(cx - 9, cy - 7, 3, 14);
        // Triangle
        Polygon tri = new Polygon(
                new int[]{cx + 8, cx - 4, cx + 8},
                new int[]{cy - 7, cy, cy + 7}, 3);
        g.fillPolygon(tri);
    }

    private void drawNextIcon(Graphics2D g, int cx, int cy, float h) {
        g.setColor(lerp(CTRL_ICON, CTRL_ICON_HOVER, h));
        // Triangle
        Polygon tri = new Polygon(
                new int[]{cx - 8, cx + 4, cx - 8},
                new int[]{cy - 7, cy, cy + 7}, 3);
        g.fillPolygon(tri);
        // Bar
        g.fillRect(cx + 6, cy - 7, 3, 14);
    }

    private void renderVolumeBar(Graphics2D g) {
        float vol = AudioPlayer.getVolume();
        int vx = volX();

        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("\u266A", vx - 22, VOL_Y + 5);
        g.drawString("\u266B", vx + VOL_W + 10, VOL_Y + 5);

        String pct = Math.round(vol * 100) + "%";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(pct, cx() - fm.stringWidth(pct) / 2, VOL_Y + 28);

        // Bar background
        g.setColor(lerp(PageRenderer.BORDER, new Color(55, 70, 90), volBarHover));
        g.fillRoundRect(vx, VOL_Y, VOL_W, VOL_H, 3, 3);

        // Filled
        int filledW = (int) (VOL_W * vol);
        if (filledW > 0) {
            g.setColor(PageRenderer.ACCENT);
            g.fillRoundRect(vx, VOL_Y, filledW, VOL_H, 3, 3);
        }

        // Knob
        int knobSize = VOL_KNOB + (int) (3 * volBarHover);
        g.setColor(lerp(PageRenderer.TEXT, PageRenderer.ACCENT, volBarHover));
        g.fillOval(vx + filledW - knobSize / 2, VOL_Y + VOL_H / 2 - knobSize / 2, knobSize, knobSize);
    }

    // ==================== Input ====================

    public void mousePressed(MouseEvent e) {
        if (Game.gameState != Game.STATE.MusicPlayer) return;

        int mx = Game.toGameX(e.getX());
        int my = Game.toGameY(e.getY());

        // Back
        if (mx >= PageRenderer.backX() && mx <= PageRenderer.backX() + PageRenderer.BACK_W
                && my >= PageRenderer.BACK_Y && my <= PageRenderer.BACK_Y + PageRenderer.BACK_H) {
            Game.gameState = Game.STATE.Menu;
            return;
        }

        if (hitBar(mx, my, progX(), PROG_Y, PROG_W)) {
            dragging = DragTarget.PROGRESS;
            applyProgress(mx);
            return;
        }

        if (hitBar(mx, my, volX(), VOL_Y, VOL_W)) {
            dragging = DragTarget.VOLUME;
            applyVolume(mx);
            return;
        }

        int cxv = cx();
        int playCy = CTRL_Y + CTRL_SIZE / 2 - (PLAY_SIZE - CTRL_SIZE) / 2 + PLAY_SIZE / 2;
        if (inCircle(mx, my, cxv, playCy, PLAY_SIZE / 2)) {
            AudioPlayer.togglePlayPause(); return;
        }
        if (inCircle(mx, my, cxv - CTRL_SPACING, CTRL_Y + CTRL_SIZE / 2, CTRL_SIZE / 2)) {
            AudioPlayer.previousTrack(); return;
        }
        if (inCircle(mx, my, cxv + CTRL_SPACING, CTRL_Y + CTRL_SIZE / 2, CTRL_SIZE / 2)) {
            AudioPlayer.nextTrack(); return;
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (Game.gameState != Game.STATE.MusicPlayer) return;
        mouseX = Game.toGameX(e.getX());
        mouseY = Game.toGameY(e.getY());
        if (dragging == DragTarget.PROGRESS) applyProgress(mouseX);
        else if (dragging == DragTarget.VOLUME) applyVolume(mouseX);
    }

    public void mouseMoved(MouseEvent e) {
        mouseX = Game.toGameX(e.getX());
        mouseY = Game.toGameY(e.getY());
    }

    public void mouseReleased(MouseEvent e) {
        dragging = DragTarget.NONE;
    }

    // ==================== Helpers ====================

    private void applyProgress(int mx) {
        if (AudioPlayer.getTrackCount() > 0 && !AudioPlayer.isStopped()) {
            AudioPlayer.seekTo(clampRatio(mx, progX(), PROG_W) * AudioPlayer.getCurrentTrack().duration);
        }
    }

    private void applyVolume(int mx) {
        float vol = clampRatio(mx, volX(), VOL_W);
        Settings.setMusicVolume(vol);
    }

    private float clampRatio(int mx, int barX, int barW) {
        return Math.max(0f, Math.min(1f, (float) (mx - barX) / barW));
    }

    private boolean hitBar(int mx, int my, int barX, int barY, int barW) {
        return mx >= barX - DRAG_MARGIN && mx <= barX + barW + DRAG_MARGIN
                && my >= barY - DRAG_MARGIN && my <= barY + PROG_H + DRAG_MARGIN;
    }

    private boolean inCircle(int mx, int my, int cx, int cy, int r) {
        return (mx - cx) * (mx - cx) + (my - cy) * (my - cy) <= r * r;
    }

    private Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private String formatTime(float seconds) {
        int total = (int) seconds;
        return String.format("%d:%02d", total / 60, total % 60);
    }
}
