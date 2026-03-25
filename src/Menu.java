import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Menu extends MouseAdapter {

    private Game game;
    private Handler handler;
    private HUD hud;

    // Fixed dimensions
    private static final int BW = PageRenderer.BTN_W;
    private static final int BH = PageRenderer.BTN_H;
    private static final int SP = PageRenderer.BTN_SPACING;
    private static final int LANG_W = 330;
    private static final int MUSIC_W = 170;
    private static final int MUSIC_H = 34;
    private static final int MUSIC_Y = 668;
    private static final int RETRY_W = 300;
    private static final int RETRY_H = 50;
    private static final int RETRY_Y = 440;

    // Y positions (fixed)
    private static final int PLAY_Y = 300;
    private static final int INFO_Y = PLAY_Y + SP;
    private static final int HELP_Y = INFO_Y + SP;

    // Dynamic X positions — depend on Game.WIDTH
    private static int btnX() { return PageRenderer.btnX(); }
    private static int langX() { return (Game.WIDTH - LANG_W) / 2; }
    private static int musicX() { return Game.WIDTH - 220; }
    private static int quitX() { return PageRenderer.backX(); }
    private static int retryX() { return (Game.WIDTH - RETRY_W) / 2; }

    public Menu(Game game, Handler handler, HUD hud) {
        this.game = game;
        this.hud = hud;
        this.handler = handler;
    }

    // ==================== Input ====================

    public void mousePressed(MouseEvent e) {
        int mx = Game.toGameX(e.getX());
        int my = Game.toGameY(e.getY());

        if (Game.gameState == Game.STATE.Menu) {
            int bx = btnX();
            if (hit(mx, my, bx, PLAY_Y, BW, BH)) { Game.gameState = Game.STATE.Select; return; }
            if (hit(mx, my, bx, INFO_Y, BW, BH)) { Game.gameState = Game.STATE.Info; return; }
            if (hit(mx, my, bx, HELP_Y, BW, BH)) { Game.gameState = Game.STATE.Help; return; }
            if (hit(mx, my, musicX(), MUSIC_Y, MUSIC_W, MUSIC_H)) { Game.gameState = Game.STATE.MusicPlayer; return; }
            // About | Changelog centered links — wide hit area across both
            int linkCenterX = Game.WIDTH / 2;
            if (hit(mx, my, linkCenterX - 85, 662, 170, 24)) {
                if (mx < linkCenterX) Game.gameState = Game.STATE.About;
                else Game.gameState = Game.STATE.Update_Notes;
                return;
            }
            if (hit(mx, my, quitX(), PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H)) { System.exit(0); }
            return;
        }

        if (Game.gameState == Game.STATE.Select) {
            int bx = btnX();
            if (hit(mx, my, bx, PLAY_Y, BW, BH)) { startGame(0, new BasicEnemy(Game.WIDTH - 50, Game.HEIGHT - 50, ID.BasicEnemy, handler)); return; }
            if (hit(mx, my, bx, INFO_Y, BW, BH)) { startGame(1, new HardEnemy(Game.WIDTH - 100, Game.HEIGHT - 100, ID.BasicEnemy, handler)); return; }
            if (hit(mx, my, bx, HELP_Y, BW, BH)) { startGame(2, new HardEnemy(Game.WIDTH - 50, Game.HEIGHT - 50, ID.BasicEnemy, handler)); return; }
            if (hitBack(mx, my)) { Game.gameState = Game.STATE.Menu; return; }
            return;
        }

        if (Game.gameState == Game.STATE.Help) {
            int lx = langX();
            if (hit(mx, my, lx, PLAY_Y, LANG_W, BH)) { Game.gameState = Game.STATE.HelpENG; return; }
            if (hit(mx, my, lx, INFO_Y, LANG_W, BH)) { Game.gameState = Game.STATE.HelpNLD; return; }
            if (hit(mx, my, lx, HELP_Y, LANG_W, BH)) { Game.gameState = Game.STATE.HelpDEU; return; }
            if (hitBack(mx, my)) { Game.gameState = Game.STATE.Menu; return; }
            return;
        }

        // Help language pages -> back to Help
        if (Game.gameState == Game.STATE.HelpENG || Game.gameState == Game.STATE.HelpNLD
                || Game.gameState == Game.STATE.HelpDEU || Game.gameState == Game.STATE.HelpFRA
                || Game.gameState == Game.STATE.HelpRUS || Game.gameState == Game.STATE.HelpSPA) {
            if (hitBack(mx, my)) { Game.gameState = Game.STATE.Help; return; }
            return;
        }

        // Info, About, Updates -> back to Menu
        if (Game.gameState == Game.STATE.Info || Game.gameState == Game.STATE.About
                || Game.gameState == Game.STATE.Update_Notes) {
            if (hitBack(mx, my)) { Game.gameState = Game.STATE.Menu; return; }
            return;
        }

        // End screen
        if (Game.gameState == Game.STATE.End) {
            if (hit(mx, my, retryX(), RETRY_Y, RETRY_W, RETRY_H)) {
                Game.gameState = Game.STATE.Select;
                hud.setLevel(1);
                hud.setScore(0);
                hud.bounds = 0;
                return;
            }
            if (hitBack(mx, my)) {
                Game.gameState = Game.STATE.Menu;
                hud.setLevel(1);
                hud.setScore(0);
                hud.bounds = 0;
                return;
            }
        }
    }

    private boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean hitBack(int mx, int my) {
        return hit(mx, my, PageRenderer.backX(), PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H);
    }

    private void startGame(int difficulty, GameObject firstEnemy) {
        Game.gameState = Game.STATE.Game;
        handler.addObject(new Player(Game.WIDTH / 2 - 32, Game.HEIGHT / 2 - 32, ID.Player, handler));
        handler.clearEnemys();
        handler.addObject(firstEnemy);
        game.diff = difficulty;
    }

    public void tick() {
    }

    // ==================== Rendering ====================

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (Game.gameState) {
            case Menu:          renderMainMenu(g2); break;
            case Select:        renderSelect(g2); break;
            case Help:          renderHelp(g2); break;
            case HelpENG:       renderHelpLang(g2, "English", "General", "Controls",
                                    new String[]{"Use W, A, S, D to move the player,", "dodge enemies, and score points.", "", "Arrow keys also work."},
                                    new String[]{"P  -  Pause", "Esc  -  Quit", "Space  -  Shop", "", "W / Up  -  Move up", "A / Left  -  Move left", "S / Down  -  Move down", "D / Right  -  Move right"}); break;
            case HelpNLD:       renderHelpLang(g2, "Nederlands", "Algemeen", "Besturing",
                                    new String[]{"Gebruik W, A, S, D om te bewegen,", "ontwijk vijanden en scoor punten.", "", "Pijltjestoetsen werken ook."},
                                    new String[]{"P  -  Pauze", "Esc  -  Afsluiten", "Spatie  -  Winkel", "", "W / Op  -  Omhoog", "A / Links  -  Links", "S / Neer  -  Omlaag", "D / Rechts  -  Rechts"}); break;
            case HelpDEU:       renderHelpLang(g2, "Deutsch", "Allgemeines", "Steuerung",
                                    new String[]{"Verwende W, A, S, D um zu bewegen,", "weiche Feinden aus und sammle Punkte.", "", "Pfeiltasten funktionieren auch."},
                                    new String[]{"P  -  Pause", "Esc  -  Verlassen", "Leertaste  -  Shop", "", "W / Hoch  -  Nach oben", "A / Links  -  Links", "S / Runter  -  Nach unten", "D / Rechts  -  Rechts"}); break;
            case Info:          renderInfo(g2); break;
            case About:         renderAbout(g2); break;
            case Update_Notes:  renderUpdates(g2); break;
            case End:           renderEnd(g2); break;
            default: break;
        }
    }

    // ---------- Main Menu ----------

    private void renderMainMenu(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawLogo(g, 200);

        int bx = btnX();
        PageRenderer.drawPrimaryButton(g, bx, PLAY_Y, BW, BH, "Play");
        PageRenderer.drawSecondaryButton(g, bx, INFO_Y, BW, BH, "Info");
        PageRenderer.drawSecondaryButton(g, bx, HELP_Y, BW, BH, "Help");

        // Quit (top-right)
        int qx = quitX();
        g.setColor(PageRenderer.SURFACE);
        g.fillRoundRect(qx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H, 8, 8);
        g.setColor(PageRenderer.BORDER);
        g.drawRoundRect(qx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H, 8, 8);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        PageRenderer.drawCenteredString(g, "Quit", qx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H);

        // Bottom bar
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("v3.0", 30, 700);

        // About | Updates links — centered
        g.setFont(PageRenderer.SMALL_FONT);
        java.awt.FontMetrics lfm = g.getFontMetrics();
        String aboutTxt = "About";
        String divider = "  |  ";
        String updatesTxt = "Changelog";
        int totalLinkW = lfm.stringWidth(aboutTxt + divider + updatesTxt);
        int linkStartX = (Game.WIDTH - totalLinkW) / 2;
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString(aboutTxt, linkStartX, 680);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString(divider, linkStartX + lfm.stringWidth(aboutTxt), 680);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString(updatesTxt, linkStartX + lfm.stringWidth(aboutTxt + divider), 680);

        // Music Player button
        int mx = musicX();
        g.setColor(PageRenderer.ACCENT);
        g.fillRoundRect(mx, MUSIC_Y, MUSIC_W, MUSIC_H, 8, 8);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.BG_DARK);
        PageRenderer.drawCenteredString(g, "Music Player", mx, MUSIC_Y, MUSIC_W, MUSIC_H);
    }

    // ---------- Select Difficulty ----------

    private void renderSelect(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Select Difficulty");
        PageRenderer.drawBackButton(g);

        // Subtitle
        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        String sub = "Choose your challenge";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(sub, (Game.WIDTH - fm.stringWidth(sub)) / 2, 130);

        int bx = btnX();
        PageRenderer.drawSecondaryButton(g, bx, PLAY_Y, BW, BH, "Normal");
        PageRenderer.drawWarningButton(g, bx, INFO_Y, BW, BH, "Hard");
        PageRenderer.drawDangerButton(g, bx, HELP_Y, BW, BH, "Insane");

        // Difficulty labels
        g.setFont(PageRenderer.SMALL_FONT);
        int labelX = bx + BW + 20;
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("Recommended for beginners", labelX, PLAY_Y + 30);
        g.setColor(new Color(245, 195, 68, 120));
        g.drawString("Unpredictable enemies", labelX, INFO_Y + 30);
        g.setColor(new Color(235, 87, 87, 120));
        g.drawString("Only for the brave", labelX, HELP_Y + 30);
    }

    // ---------- Help ----------

    private void renderHelp(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Help");
        PageRenderer.drawBackButton(g);

        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        String sub = "Select your language";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(sub, (Game.WIDTH - fm.stringWidth(sub)) / 2, 130);

        int lx = langX();
        PageRenderer.drawSecondaryButton(g, lx, PLAY_Y, LANG_W, BH, "English");
        PageRenderer.drawSecondaryButton(g, lx, INFO_Y, LANG_W, BH, "Nederlands");
        PageRenderer.drawSecondaryButton(g, lx, HELP_Y, LANG_W, BH, "Deutsch");
    }

    // ---------- Help Language ----------

    private void renderHelpLang(Graphics2D g, String language,
                                String leftTitle, String rightTitle,
                                String[] leftLines, String[] rightLines) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Help");
        PageRenderer.drawBackButton(g);

        // Language badge
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.ACCENT);
        FontMetrics fm = g.getFontMetrics();
        String badge = language.toUpperCase();
        int bw = fm.stringWidth(badge) + 20;
        int bx = (Game.WIDTH - bw) / 2;
        g.drawRoundRect(bx, 105, bw, 24, 6, 6);
        g.drawString(badge, bx + 10, 122);

        // Left panel
        int panelY = 160;
        int panelH = 310;
        PageRenderer.drawPanel(g, 60, panelY, 540, panelH);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString(leftTitle, 85, panelY + 38);

        // Separator line
        g.setColor(PageRenderer.BORDER);
        g.fillRect(85, panelY + 50, 490, 1);

        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        int y = panelY + 78;
        for (String line : leftLines) {
            if (!line.isEmpty()) g.drawString(line, 85, y);
            y += 24;
        }

        // Right panel
        PageRenderer.drawPanel(g, 630, panelY, 590, panelH);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString(rightTitle, 655, panelY + 38);

        g.setColor(PageRenderer.BORDER);
        g.fillRect(655, panelY + 50, 540, 1);

        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        y = panelY + 78;
        for (String line : rightLines) {
            if (!line.isEmpty()) g.drawString(line, 655, y);
            y += 24;
        }
    }

    // ---------- Info ----------

    private void renderInfo(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Info");
        PageRenderer.drawBackButton(g);

        // Contact panel
        PageRenderer.drawPanel(g, 60, 140, 600, 260);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Contact", 85, 178);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(85, 190, 550, 1);

        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString("Instagram:  @MauriceBoendermaker", 85, 220);
        g.drawString("Email:  mauriceboendermaker@gmail.com", 85, 248);

        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("Feedback:", 85, 290);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString("dodgegamefeedback@gmail.com", 85, 316);

        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("Original design by Jeffrey", 85, 380);

        // Quick links panel
        PageRenderer.drawPanel(g, 690, 140, 530, 120);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Quick Links", 715, 178);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(715, 190, 480, 1);

        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString("This game was originally created in 2016.", 715, 220);
        g.drawString("Built with Java AWT/Swing.", 715, 244);
    }

    // ---------- About ----------

    private void renderAbout(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "About");
        PageRenderer.drawBackButton(g);

        PageRenderer.drawPanel(g, 60, 140, Game.WIDTH - 120, 300);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Game Modes", 85, 178);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(85, 190, Game.WIDTH - 170, 1);

        g.setFont(PageRenderer.BODY_FONT);
        int y = 222;
        String[][] modes = {
                {"Normal", "Standard enemies with predictable movement patterns."},
                {"Hard", "Enemies change direction randomly on each bounce."},
                {"Insane", "Fast enemies, smart trackers, and random movement combined."}
        };
        for (String[] mode : modes) {
            g.setColor(PageRenderer.TEXT);
            g.drawString(mode[0], 85, y);
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString(mode[1], 200, y);
            y += 32;
        }

        // Mechanics
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Mechanics", 85, y + 20);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(85, y + 32, Game.WIDTH - 170, 1);

        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        y += 58;
        g.drawString("Dodge enemies to survive. Earn points over time to spend in the shop.", 85, y);
        g.drawString("Upgrade your health bar, movement speed, or refill health.", 85, y + 26);
        g.drawString("Every few levels, new enemies spawn. Boss fights occur at levels 10 and 15.", 85, y + 52);
    }

    // ---------- Changelog ----------

    private void renderUpdates(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Changelog");
        PageRenderer.drawBackButton(g);

        int y = 130;

        // v3.0
        PageRenderer.drawPanel(g, 60, y, Game.WIDTH - 120, 130);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("v3.0", 85, y + 32);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("25.03.2026", 140, y + 32);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(85, y + 44, Game.WIDTH - 170, 1);
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString("- Complete visual overhaul with modern dark theme.", 85, y + 68);
        g.drawString("- Fullscreen mode with automatic resolution scaling.", 85, y + 92);
        g.drawString("- Built-in music player with playback controls.", 85, y + 116);

        y += 148;

        // v2.0
        PageRenderer.drawPanel(g, 60, y, Game.WIDTH - 120, 130);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.TEXT);
        g.drawString("v2.0", 85, y + 32);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("25.03.2026", 140, y + 32);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(85, y + 44, Game.WIDTH - 170, 1);
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString("- Complete rewrite and overhaul of legacy codebase.", 85, y + 68);
        g.drawString("- Fixed resolution bugs, removed duplicate classes, cleaned up all dead code.", 85, y + 92);
        g.drawString("- No new features introduced — focused on code quality and stability.", 85, y + 116);

        y += 148;

        // v1.0
        PageRenderer.drawPanel(g, 60, y, Game.WIDTH - 120, 108);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("v1.0", 85, y + 32);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("10.11.2019", 140, y + 32);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(85, y + 44, Game.WIDTH - 170, 1);
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("- Name changed from \"Dodge Game!\" to \"Dotch\".", 85, y + 68);
        g.drawString("- Game layout updated. Added contact page and language support.", 85, y + 92);

        y += 126;

        // v0.1
        PageRenderer.drawPanel(g, 60, y, Game.WIDTH - 120, 130);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("v0.1", 85, y + 32);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("09.08.2016", 138, y + 32);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(85, y + 44, Game.WIDTH - 170, 1);
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("- Initial release with core game mechanics.", 85, y + 68);
        g.drawString("- Player movement, enemy spawning, collision detection, and scoring system.", 85, y + 92);
        g.drawString("- Three difficulty modes, boss fights, HUD, and in-game shop.", 85, y + 116);
    }

    // ---------- End / Game Over ----------

    private void renderEnd(Graphics2D g) {
        PageRenderer.drawBackground(g);

        // Game Over title
        g.setFont(PageRenderer.TITLE_FONT);
        g.setColor(PageRenderer.DANGER);
        String go = "Game Over";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(go, (Game.WIDTH - fm.stringWidth(go)) / 2, 180);

        // Score display
        PageRenderer.drawPanel(g, (Game.WIDTH - 400) / 2, 230, 400, 140);
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String label = "FINAL SCORE";
        fm = g.getFontMetrics();
        g.drawString(label, (Game.WIDTH - fm.stringWidth(label)) / 2, 268);

        g.setFont(new Font("Arial", Font.BOLD, 64));
        g.setColor(PageRenderer.TEXT);
        String score = String.valueOf(hud.getScore());
        fm = g.getFontMetrics();
        g.drawString(score, (Game.WIDTH - fm.stringWidth(score)) / 2, 345);

        // Retry button
        PageRenderer.drawPrimaryButton(g, retryX(), RETRY_Y, RETRY_W, RETRY_H, "Try Again");

        // Back to menu
        PageRenderer.drawBackButton(g);
    }
}
