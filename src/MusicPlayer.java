import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MusicPlayer extends MouseAdapter {

    private static final int CX = Game.WIDTH / 2;

    // Progress bar
    private static final int PROG_X = 240;
    private static final int PROG_Y = 430;
    private static final int PROG_W = 800;
    private static final int PROG_H = 5;
    private static final int PROG_KNOB = 14;

    // Controls
    private static final int CTRL_Y = 490;
    private static final int CTRL_SIZE = 44;
    private static final int PLAY_SIZE = 56;

    // Volume
    private static final int VOL_X = 490;
    private static final int VOL_Y = 580;
    private static final int VOL_W = 300;
    private static final int VOL_H = 5;
    private static final int VOL_KNOB = 12;

    public void tick() {
        if (!AudioPlayer.isStopped() && !AudioPlayer.isPaused() && !AudioPlayer.isPlaying()) {
            AudioPlayer.nextTrack();
            AudioPlayer.play();
        }
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        PageRenderer.drawBackground(g2);
        PageRenderer.drawTitle(g2, "Music Player");
        PageRenderer.drawBackButton(g2);

        // Album art placeholder
        int artSize = 180;
        int artX = CX - artSize / 2;
        int artY = 130;
        PageRenderer.drawPanel(g2, artX, artY, artSize, artSize);

        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 60));
        g2.setColor(PageRenderer.ACCENT);
        FontMetrics fm = g2.getFontMetrics();
        String note = "\u266B";
        g2.drawString(note, CX - fm.stringWidth(note) / 2, artY + artSize / 2 + 20);

        if (AudioPlayer.getTrackCount() > 0) {
            AudioPlayer.Track track = AudioPlayer.getCurrentTrack();

            // Track name
            g2.setFont(PageRenderer.HEADING_FONT);
            g2.setColor(PageRenderer.TEXT);
            fm = g2.getFontMetrics();
            g2.drawString(track.displayName, CX - fm.stringWidth(track.displayName) / 2, 365);

            // Track number
            g2.setFont(PageRenderer.SMALL_FONT);
            g2.setColor(PageRenderer.TEXT_MUTED);
            String info = "Track " + (AudioPlayer.getCurrentTrackIndex() + 1) + " of " + AudioPlayer.getTrackCount();
            fm = g2.getFontMetrics();
            g2.drawString(info, CX - fm.stringWidth(info) / 2, 390);

            // Progress bar
            float pos = AudioPlayer.getPosition();
            float dur = track.duration;
            float progress = (dur > 0) ? Math.min(pos / dur, 1f) : 0;

            g2.setColor(PageRenderer.BORDER);
            g2.fillRoundRect(PROG_X, PROG_Y, PROG_W, PROG_H, 3, 3);
            int filledW = (int) (PROG_W * progress);
            if (filledW > 0) {
                g2.setColor(PageRenderer.ACCENT);
                g2.fillRoundRect(PROG_X, PROG_Y, filledW, PROG_H, 3, 3);
            }
            g2.setColor(PageRenderer.TEXT);
            g2.fillOval(PROG_X + filledW - PROG_KNOB / 2, PROG_Y - (PROG_KNOB - PROG_H) / 2, PROG_KNOB, PROG_KNOB);

            // Time labels
            g2.setFont(PageRenderer.SMALL_FONT);
            g2.setColor(PageRenderer.TEXT_MUTED);
            g2.drawString(formatTime(pos), PROG_X, PROG_Y + 25);
            String durStr = formatTime(dur);
            fm = g2.getFontMetrics();
            g2.drawString(durStr, PROG_X + PROG_W - fm.stringWidth(durStr), PROG_Y + 25);
        } else {
            g2.setFont(PageRenderer.HEADING_FONT);
            g2.setColor(PageRenderer.TEXT_MUTED);
            String msg = "No tracks loaded";
            fm = g2.getFontMetrics();
            g2.drawString(msg, CX - fm.stringWidth(msg) / 2, 375);
        }

        // Controls
        renderControls(g2);

        // Volume
        renderVolume(g2);
    }

    private void renderControls(Graphics2D g) {
        int spacing = 80;
        int prevX = CX - spacing * 2;
        int nextX = CX + spacing;
        int stopX = CX + spacing * 2;

        // Prev
        drawCtrlBtn(g, prevX, CTRL_Y, CTRL_SIZE, "<<");

        // Play/Pause (larger)
        boolean playing = AudioPlayer.isPlaying();
        g.setColor(playing ? PageRenderer.ACCENT : PageRenderer.SURFACE_LIGHT);
        int py = CTRL_Y - (PLAY_SIZE - CTRL_SIZE) / 2;
        g.fillOval(CX - PLAY_SIZE / 2, py, PLAY_SIZE, PLAY_SIZE);
        if (!playing) {
            g.setColor(PageRenderer.BORDER);
            g.drawOval(CX - PLAY_SIZE / 2, py, PLAY_SIZE, PLAY_SIZE);
        }
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));
        g.setColor(playing ? PageRenderer.BG_DARK : PageRenderer.TEXT);
        String sym = playing ? "||" : ">";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(sym, CX - fm.stringWidth(sym) / 2, py + PLAY_SIZE / 2 + fm.getAscent() / 3);

        // Next
        drawCtrlBtn(g, nextX, CTRL_Y, CTRL_SIZE, ">>");

        // Stop
        drawCtrlBtn(g, stopX, CTRL_Y, CTRL_SIZE, "\u25A0");
    }

    private void drawCtrlBtn(Graphics2D g, int cx, int cy, int size, String label) {
        g.setColor(PageRenderer.SURFACE_LIGHT);
        g.fillOval(cx - size / 2, cy, size, size);
        g.setColor(PageRenderer.BORDER);
        g.drawOval(cx - size / 2, cy, size, size);
        g.setColor(PageRenderer.TEXT_SEC);
        g.setFont(PageRenderer.BUTTON_FONT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, cx - fm.stringWidth(label) / 2, cy + size / 2 + fm.getAscent() / 3);
    }

    private void renderVolume(Graphics2D g) {
        float vol = AudioPlayer.getVolume();

        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("\u266A", VOL_X - 22, VOL_Y + 5);
        g.drawString("\u266B", VOL_X + VOL_W + 10, VOL_Y + 5);

        String pct = Math.round(vol * 100) + "%";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(pct, CX - fm.stringWidth(pct) / 2, VOL_Y + 28);

        g.setColor(PageRenderer.BORDER);
        g.fillRoundRect(VOL_X, VOL_Y, VOL_W, VOL_H, 3, 3);
        int filledW = (int) (VOL_W * vol);
        if (filledW > 0) {
            g.setColor(PageRenderer.ACCENT);
            g.fillRoundRect(VOL_X, VOL_Y, filledW, VOL_H, 3, 3);
        }
        g.setColor(PageRenderer.TEXT);
        g.fillOval(VOL_X + filledW - VOL_KNOB / 2, VOL_Y - (VOL_KNOB - VOL_H) / 2, VOL_KNOB, VOL_KNOB);
    }

    public void mousePressed(MouseEvent e) {
        if (Game.gameState != Game.STATE.MusicPlayer) return;

        int mx = Game.toGameX(e.getX());
        int my = Game.toGameY(e.getY());

        // Back
        if (mx >= PageRenderer.BACK_X && mx <= PageRenderer.BACK_X + PageRenderer.BACK_W
                && my >= PageRenderer.BACK_Y && my <= PageRenderer.BACK_Y + PageRenderer.BACK_H) {
            Game.gameState = Game.STATE.Menu;
            return;
        }

        // Progress bar
        if (mx >= PROG_X && mx <= PROG_X + PROG_W && my >= PROG_Y - 12 && my <= PROG_Y + PROG_H + 12) {
            if (AudioPlayer.getTrackCount() > 0 && !AudioPlayer.isStopped()) {
                float ratio = (float) (mx - PROG_X) / PROG_W;
                AudioPlayer.seekTo(ratio * AudioPlayer.getCurrentTrack().duration);
            }
            return;
        }

        // Volume bar
        if (mx >= VOL_X && mx <= VOL_X + VOL_W && my >= VOL_Y - 12 && my <= VOL_Y + VOL_H + 12) {
            AudioPlayer.setVolume((float) (mx - VOL_X) / VOL_W);
            return;
        }

        // Controls
        int spacing = 80;
        if (inCircle(mx, my, CX, CTRL_Y + PLAY_SIZE / 2 - (PLAY_SIZE - CTRL_SIZE) / 2, PLAY_SIZE / 2)) {
            AudioPlayer.togglePlayPause(); return;
        }
        if (inCircle(mx, my, CX - spacing * 2, CTRL_Y + CTRL_SIZE / 2, CTRL_SIZE / 2)) {
            AudioPlayer.previousTrack(); return;
        }
        if (inCircle(mx, my, CX + spacing, CTRL_Y + CTRL_SIZE / 2, CTRL_SIZE / 2)) {
            AudioPlayer.nextTrack(); return;
        }
        if (inCircle(mx, my, CX + spacing * 2, CTRL_Y + CTRL_SIZE / 2, CTRL_SIZE / 2)) {
            AudioPlayer.stop(); return;
        }
    }

    private boolean inCircle(int mx, int my, int cx, int cy, int r) {
        return (mx - cx) * (mx - cx) + (my - cy) * (my - cy) <= r * r;
    }

    private String formatTime(float seconds) {
        int total = (int) seconds;
        return String.format("%d:%02d", total / 60, total % 60);
    }
}
