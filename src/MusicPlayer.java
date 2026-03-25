import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MusicPlayer extends MouseAdapter {

    // Spotify-inspired colors
    private static final Color BG_COLOR = new Color(25, 20, 20);
    private static final Color ACCENT_GREEN = new Color(29, 185, 84);
    private static final Color ACCENT_GREEN_HOVER = new Color(30, 215, 96);
    private static final Color BAR_BG = new Color(60, 60, 60);
    private static final Color TEXT_PRIMARY = Color.white;
    private static final Color TEXT_SECONDARY = new Color(179, 179, 179);
    private static final Color CONTROL_COLOR = new Color(200, 200, 200);
    private static final Color PANEL_COLOR = new Color(40, 40, 40);

    // Fonts
    private static final Font FONT_TITLE = new Font("Arial", Font.BOLD, 36);
    private static final Font FONT_TRACK = new Font("Arial", Font.BOLD, 28);
    private static final Font FONT_SUBTITLE = new Font("Arial", Font.PLAIN, 18);
    private static final Font FONT_TIME = new Font("Arial", Font.PLAIN, 14);
    private static final Font FONT_CONTROLS = new Font("Arial", Font.BOLD, 24);
    private static final Font FONT_PLAY = new Font("Arial", Font.BOLD, 32);
    private static final Font FONT_VOLUME_LABEL = new Font("Arial", Font.PLAIN, 14);
    private static final Font FONT_BACK = new Font("Arial", Font.BOLD, 18);
    private static final Font FONT_TRACK_NUM = new Font("Arial", Font.PLAIN, 16);

    // Layout constants
    private static final int CENTER_X = Game.WIDTH / 2;

    // Progress bar
    private static final int PROG_X = 240;
    private static final int PROG_Y = 430;
    private static final int PROG_W = 800;
    private static final int PROG_H = 6;
    private static final int PROG_KNOB = 14;

    // Control buttons (y position)
    private static final int CTRL_Y = 490;
    private static final int CTRL_BTN_SIZE = 48;
    private static final int PLAY_BTN_SIZE = 60;

    // Volume bar
    private static final int VOL_X = 490;
    private static final int VOL_Y = 580;
    private static final int VOL_W = 300;
    private static final int VOL_H = 6;
    private static final int VOL_KNOB = 12;

    // Back button
    private static final int BACK_X = 1145;
    private static final int BACK_Y = 52;
    private static final int BACK_W = 100;
    private static final int BACK_H = 33;

    public void tick() {
        // Auto-advance to next track when current ends
        if (!AudioPlayer.isStopped() && !AudioPlayer.isPaused() && !AudioPlayer.isPlaying()) {
            AudioPlayer.nextTrack();
            AudioPlayer.play();
        }
    }

    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        // Title
        g.setFont(FONT_TITLE);
        g.setColor(TEXT_PRIMARY);
        String title = "Music Player";
        int titleW = g.getFontMetrics().stringWidth(title);
        g.drawString(title, CENTER_X - titleW / 2, 80);

        // Decorative line under title
        g.setColor(ACCENT_GREEN);
        g.fillRect(CENTER_X - 60, 95, 120, 3);

        // Album art placeholder (rounded dark panel)
        int artSize = 200;
        int artX = CENTER_X - artSize / 2;
        int artY = 130;
        g.setColor(PANEL_COLOR);
        g2d.fillRoundRect(artX, artY, artSize, artSize, 16, 16);

        // Music note icon in art area
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(ACCENT_GREEN);
        String note = "\u266B";
        int noteW = g.getFontMetrics().stringWidth(note);
        g.drawString(note, CENTER_X - noteW / 2, artY + artSize / 2 + 25);

        // Track info
        if (AudioPlayer.getTrackCount() > 0) {
            AudioPlayer.Track track = AudioPlayer.getCurrentTrack();

            // Track name
            g.setFont(FONT_TRACK);
            g.setColor(TEXT_PRIMARY);
            String name = track.displayName;
            int nameW = g.getFontMetrics().stringWidth(name);
            g.drawString(name, CENTER_X - nameW / 2, 375);

            // Track number
            g.setFont(FONT_TRACK_NUM);
            g.setColor(TEXT_SECONDARY);
            String trackNum = "Track " + (AudioPlayer.getCurrentTrackIndex() + 1) + " of " + AudioPlayer.getTrackCount();
            int numW = g.getFontMetrics().stringWidth(trackNum);
            g.drawString(trackNum, CENTER_X - numW / 2, 400);

            // Progress bar
            renderProgressBar(g, g2d, track);

            // Time labels
            float pos = AudioPlayer.getPosition();
            float dur = track.duration;
            g.setFont(FONT_TIME);
            g.setColor(TEXT_SECONDARY);
            g.drawString(formatTime(pos), PROG_X, PROG_Y + 25);
            String durStr = formatTime(dur);
            int durW = g.getFontMetrics().stringWidth(durStr);
            g.drawString(durStr, PROG_X + PROG_W - durW, PROG_Y + 25);
        } else {
            g.setFont(FONT_TRACK);
            g.setColor(TEXT_SECONDARY);
            String noTracks = "No tracks loaded";
            int ntW = g.getFontMetrics().stringWidth(noTracks);
            g.drawString(noTracks, CENTER_X - ntW / 2, 375);
        }

        // Playback controls
        renderControls(g, g2d);

        // Volume control
        renderVolumeBar(g, g2d);

        // Back button
        g.setColor(PANEL_COLOR);
        g2d.fillRoundRect(BACK_X, BACK_Y, BACK_W, BACK_H, 8, 8);
        g.setColor(TEXT_PRIMARY);
        g.setFont(FONT_BACK);
        String backStr = "BACK";
        int backW = g.getFontMetrics().stringWidth(backStr);
        g.drawString(backStr, BACK_X + (BACK_W - backW) / 2, BACK_Y + 23);

        // Reset antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    private void renderProgressBar(Graphics g, Graphics2D g2d, AudioPlayer.Track track) {
        float pos = AudioPlayer.getPosition();
        float dur = track.duration;
        float progress = (dur > 0) ? Math.min(pos / dur, 1f) : 0;

        // Bar background
        g.setColor(BAR_BG);
        g2d.fillRoundRect(PROG_X, PROG_Y, PROG_W, PROG_H, 3, 3);

        // Filled portion
        int filledW = (int) (PROG_W * progress);
        if (filledW > 0) {
            g.setColor(ACCENT_GREEN);
            g2d.fillRoundRect(PROG_X, PROG_Y, filledW, PROG_H, 3, 3);
        }

        // Knob
        int knobX = PROG_X + filledW - PROG_KNOB / 2;
        g.setColor(TEXT_PRIMARY);
        g2d.fillOval(knobX, PROG_Y - (PROG_KNOB - PROG_H) / 2, PROG_KNOB, PROG_KNOB);
    }

    private void renderControls(Graphics g, Graphics2D g2d) {
        int spacing = 80;
        int prevX = CENTER_X - spacing * 2;
        int stopX = CENTER_X + spacing * 2;
        int prevTrackX = CENTER_X - spacing;
        int nextTrackX = CENTER_X + spacing;

        // Previous
        drawControlButton(g, g2d, prevX, CTRL_Y, CTRL_BTN_SIZE, "<<", FONT_CONTROLS);

        // Play/Pause (larger, green when playing)
        boolean playing = AudioPlayer.isPlaying();
        Color playColor = playing ? ACCENT_GREEN : PANEL_COLOR;
        g.setColor(playColor);
        g2d.fillOval(CENTER_X - PLAY_BTN_SIZE / 2, CTRL_Y - (PLAY_BTN_SIZE - CTRL_BTN_SIZE) / 2,
                PLAY_BTN_SIZE, PLAY_BTN_SIZE);
        g.setColor(playing ? Color.BLACK : TEXT_PRIMARY);
        g.setFont(FONT_PLAY);
        String playStr = playing ? "||" : ">";
        int playW = g.getFontMetrics().stringWidth(playStr);
        int playTextY = CTRL_Y - (PLAY_BTN_SIZE - CTRL_BTN_SIZE) / 2 + PLAY_BTN_SIZE / 2 + 10;
        g.drawString(playStr, CENTER_X - playW / 2, playTextY);

        // Next
        drawControlButton(g, g2d, nextTrackX, CTRL_Y, CTRL_BTN_SIZE, ">>", FONT_CONTROLS);

        // Stop
        drawControlButton(g, g2d, stopX, CTRL_Y, CTRL_BTN_SIZE, "\u25A0", FONT_CONTROLS);
    }

    private void drawControlButton(Graphics g, Graphics2D g2d, int cx, int cy, int size,
                                   String label, Font font) {
        g.setColor(PANEL_COLOR);
        g2d.fillOval(cx - size / 2, cy, size, size);
        g.setColor(CONTROL_COLOR);
        g.setFont(font);
        int labelW = g.getFontMetrics().stringWidth(label);
        g.drawString(label, cx - labelW / 2, cy + size / 2 + 8);
    }

    private void renderVolumeBar(Graphics g, Graphics2D g2d) {
        float vol = AudioPlayer.getVolume();

        // Label
        g.setFont(FONT_VOLUME_LABEL);
        g.setColor(TEXT_SECONDARY);
        g.drawString("\u266A", VOL_X - 25, VOL_Y + 5);
        g.drawString("\u266B", VOL_X + VOL_W + 12, VOL_Y + 5);

        // Volume percentage
        String volPct = Math.round(vol * 100) + "%";
        int pctW = g.getFontMetrics().stringWidth(volPct);
        g.drawString(volPct, CENTER_X - pctW / 2, VOL_Y + 30);

        // Bar background
        g.setColor(BAR_BG);
        g2d.fillRoundRect(VOL_X, VOL_Y, VOL_W, VOL_H, 3, 3);

        // Filled portion
        int filledW = (int) (VOL_W * vol);
        if (filledW > 0) {
            g.setColor(ACCENT_GREEN);
            g2d.fillRoundRect(VOL_X, VOL_Y, filledW, VOL_H, 3, 3);
        }

        // Knob
        int knobX = VOL_X + filledW - VOL_KNOB / 2;
        g.setColor(TEXT_PRIMARY);
        g2d.fillOval(knobX, VOL_Y - (VOL_KNOB - VOL_H) / 2, VOL_KNOB, VOL_KNOB);
    }

    public void mousePressed(MouseEvent e) {
        if (Game.gameState != Game.STATE.MusicPlayer) return;

        int mx = Game.toGameX(e.getX());
        int my = Game.toGameY(e.getY());

        // Back button
        if (mx >= BACK_X && mx <= BACK_X + BACK_W && my >= BACK_Y && my <= BACK_Y + BACK_H) {
            Game.gameState = Game.STATE.Menu;
            return;
        }

        // Progress bar click (seek)
        if (mx >= PROG_X && mx <= PROG_X + PROG_W && my >= PROG_Y - 10 && my <= PROG_Y + PROG_H + 10) {
            if (AudioPlayer.getTrackCount() > 0 && !AudioPlayer.isStopped()) {
                float ratio = (float) (mx - PROG_X) / PROG_W;
                float seekTime = ratio * AudioPlayer.getCurrentTrack().duration;
                AudioPlayer.seekTo(seekTime);
            }
            return;
        }

        // Volume bar click
        if (mx >= VOL_X && mx <= VOL_X + VOL_W && my >= VOL_Y - 10 && my <= VOL_Y + VOL_H + 10) {
            float ratio = (float) (mx - VOL_X) / VOL_W;
            AudioPlayer.setVolume(ratio);
            return;
        }

        // Control buttons
        int spacing = 80;
        int prevX = CENTER_X - spacing * 2;
        int nextX = CENTER_X + spacing;
        int stopX = CENTER_X + spacing * 2;

        // Play/Pause
        if (isInCircle(mx, my, CENTER_X, CTRL_Y + PLAY_BTN_SIZE / 2 - (PLAY_BTN_SIZE - CTRL_BTN_SIZE) / 2, PLAY_BTN_SIZE / 2)) {
            AudioPlayer.togglePlayPause();
            return;
        }

        // Previous
        if (isInCircle(mx, my, prevX, CTRL_Y + CTRL_BTN_SIZE / 2, CTRL_BTN_SIZE / 2)) {
            AudioPlayer.previousTrack();
            return;
        }

        // Next
        if (isInCircle(mx, my, nextX, CTRL_Y + CTRL_BTN_SIZE / 2, CTRL_BTN_SIZE / 2)) {
            AudioPlayer.nextTrack();
            return;
        }

        // Stop
        if (isInCircle(mx, my, stopX, CTRL_Y + CTRL_BTN_SIZE / 2, CTRL_BTN_SIZE / 2)) {
            AudioPlayer.stop();
            return;
        }
    }

    private boolean isInCircle(int mx, int my, int cx, int cy, int radius) {
        int dx = mx - cx;
        int dy = my - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    private String formatTime(float seconds) {
        int totalSec = (int) seconds;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }
}
