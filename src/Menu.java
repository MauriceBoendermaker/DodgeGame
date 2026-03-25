import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class Menu extends MouseAdapter implements MouseWheelListener {

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
    private static final int RETRY_Y = 472;

    // Y positions
    private static final int PLAY_Y = 300;
    private static final int INFO_Y = PLAY_Y + SP;
    private static final int HELP_Y = INFO_Y + SP;

    // Pause menu layout
    private static final int PAUSE_W = 260;
    private static final int PAUSE_H = 46;
    private static final int PAUSE_SP = 60;
    private static final int PAUSE_Y0 = 310;
    private static int pauseX() { return (Game.WIDTH - PAUSE_W) / 2; }

    // Dynamic X positions
    private static int btnX() { return PageRenderer.btnX(); }
    private static int langX() { return (Game.WIDTH - LANG_W) / 2; }
    private static int musicX() { return Game.WIDTH - 220; }
    private static int quitX() { return PageRenderer.backX(); }
    private static int retryX() { return (Game.WIDTH - RETRY_W) / 2; }

    // Hover state
    private float[] btn = new float[3];
    private float backH, musicH, settingsH, aboutH, changelogH, creditsLinkH, quitH, retryH;
    private float[] pauseBtn = new float[4];
    private int mouseX, mouseY;
    private static final float LERP = 0.14f;

    // Help page scroll
    private float helpScroll = 0;
    private float helpScrollTarget = 0;

    // Settings page state
    private static final int SETTINGS_W = 170;
    private static final int SETTINGS_H = 34;
    private static final int SETTINGS_Y = 668;
    private static int settingsX() { return 30; }

    private float settingsScroll = 0;
    private float settingsScrollTarget = 0;

    // Settings hover states: 0=musicVol, 1=sfxVol, 2=shakeOff, 3=shakeLow, 4=shakeHigh,
    // 5=partLow, 6=partMed, 7=partHigh, 8=fpsToggle, 9=trailToggle, 10=gridToggle,
    // 11=colorblindToggle, 12-17=lang buttons, 18=resetScores, 19=resetAll
    private static final int SET_HOVER_COUNT = 20;
    private float[] setH = new float[SET_HOVER_COUNT];
    private float setMusicBarH, setSfxBarH;

    // Settings drag
    private enum SettingsDrag { NONE, MUSIC_VOL, SFX_VOL }
    private SettingsDrag settingsDrag = SettingsDrag.NONE;

    // Confirmation dialog
    private String confirmAction = null; // "scores" or "progress" or null
    private float confirmYesH, confirmNoH;

    // Track where Settings was opened from (null = main menu)
    public static Game.STATE settingsReturnTo = null;

    public Menu(Game game, Handler handler, HUD hud) {
        this.game = game;
        this.hud = hud;
        this.handler = handler;
    }

    // ==================== Hover ====================

    public void tick() {
        updateHover();
        // Smooth scroll
        helpScroll += (helpScrollTarget - helpScroll) * 0.18f;
        if (Math.abs(helpScroll - helpScrollTarget) < 0.5f) helpScroll = helpScrollTarget;
        settingsScroll += (settingsScrollTarget - settingsScroll) * 0.18f;
        if (Math.abs(settingsScroll - settingsScrollTarget) < 0.5f) settingsScroll = settingsScrollTarget;
    }

    private void updateHover() {
        // Determine targets based on current state
        boolean[] btnTargets = new boolean[3];
        boolean backTarget = false, musicTarget = false, settingsTarget = false, aboutTarget = false;
        boolean changelogTarget = false, creditsLinkTarget = false, quitTarget = false, retryTarget = false;

        switch (Game.gameState) {
            case Menu:
                int bx = btnX();
                btnTargets[0] = hit(mouseX, mouseY, bx, PLAY_Y, BW, BH);
                btnTargets[1] = hit(mouseX, mouseY, bx, INFO_Y, BW, BH);
                btnTargets[2] = hit(mouseX, mouseY, bx, HELP_Y, BW, BH);
                quitTarget = hit(mouseX, mouseY, quitX(), PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H);
                musicTarget = hit(mouseX, mouseY, musicX(), MUSIC_Y, MUSIC_W, MUSIC_H);
                settingsTarget = hit(mouseX, mouseY, settingsX(), SETTINGS_Y, SETTINGS_W, SETTINGS_H);
                int[] linkXs = getBottomLinkXs();
                aboutTarget = hit(mouseX, mouseY, linkXs[0], 666, linkXs[1] - linkXs[0], 20);
                changelogTarget = hit(mouseX, mouseY, linkXs[1], 666, linkXs[2] - linkXs[1], 20);
                creditsLinkTarget = hit(mouseX, mouseY, linkXs[2], 666, linkXs[3] - linkXs[2], 20);
                break;
            case Select:
                bx = btnX();
                btnTargets[0] = hit(mouseX, mouseY, bx, PLAY_Y, BW, BH);
                btnTargets[1] = hit(mouseX, mouseY, bx, INFO_Y, BW, BH);
                btnTargets[2] = hit(mouseX, mouseY, bx, HELP_Y, BW, BH);
                backTarget = hitBack();
                break;
            case Help:
                int lx = langX();
                btnTargets[0] = hit(mouseX, mouseY, lx, PLAY_Y, LANG_W, BH);
                btnTargets[1] = hit(mouseX, mouseY, lx, INFO_Y, LANG_W, BH);
                btnTargets[2] = hit(mouseX, mouseY, lx, HELP_Y, LANG_W, BH);
                backTarget = hitBack();
                break;
            case End:
                retryTarget = hit(mouseX, mouseY, retryX(), RETRY_Y, RETRY_W, RETRY_H);
                backTarget = hitBack();
                break;
            case Paused:
                int px = pauseX();
                btnTargets[0] = hit(mouseX, mouseY, px, PAUSE_Y0, PAUSE_W, PAUSE_H);
                btnTargets[1] = hit(mouseX, mouseY, px, PAUSE_Y0 + PAUSE_SP, PAUSE_W, PAUSE_H);
                btnTargets[2] = hit(mouseX, mouseY, px, PAUSE_Y0 + PAUSE_SP * 2, PAUSE_W, PAUSE_H);
                pauseBtn[3] = approach(pauseBtn[3], hit(mouseX, mouseY, px, PAUSE_Y0 + PAUSE_SP * 3, PAUSE_W, PAUSE_H));
                break;
            case Settings:
                backTarget = hitBack();
                updateSettingsHover();
                break;
            default:
                backTarget = hitBack();
                break;
        }

        for (int i = 0; i < 3; i++) btn[i] = approach(btn[i], btnTargets[i]);
        backH = approach(backH, backTarget);
        musicH = approach(musicH, musicTarget);
        settingsH = approach(settingsH, settingsTarget);
        aboutH = approach(aboutH, aboutTarget);
        changelogH = approach(changelogH, changelogTarget);
        creditsLinkH = approach(creditsLinkH, creditsLinkTarget);
        quitH = approach(quitH, quitTarget);
        retryH = approach(retryH, retryTarget);
    }

    private float approach(float current, boolean target) {
        float t = target ? 1f : 0f;
        current += (t - current) * LERP;
        if (Math.abs(current - t) < 0.01f) current = t;
        return current;
    }

    // ==================== Input ====================

    public void mouseMoved(MouseEvent e) {
        mouseX = Game.toGameX(e.getX());
        mouseY = Game.toGameY(e.getY());
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (Game.gameState == Game.STATE.HelpENG || Game.gameState == Game.STATE.HelpNLD
                || Game.gameState == Game.STATE.HelpDEU) {
            helpScrollTarget += e.getWheelRotation() * 40;
            helpScrollTarget = Math.max(0, helpScrollTarget);
        }
        if (Game.gameState == Game.STATE.Settings) {
            settingsScrollTarget += e.getWheelRotation() * 40;
            settingsScrollTarget = Math.max(0, settingsScrollTarget);
        }
    }

    public void mouseDragged(MouseEvent e) {
        mouseX = Game.toGameX(e.getX());
        mouseY = Game.toGameY(e.getY());
        if (Game.gameState == Game.STATE.Settings) {
            if (settingsDrag == SettingsDrag.MUSIC_VOL) {
                Settings.setMusicVolume(settingsSliderRatio(mouseX));
            } else if (settingsDrag == SettingsDrag.SFX_VOL) {
                Settings.setSfxVolume(settingsSliderRatio(mouseX));
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        settingsDrag = SettingsDrag.NONE;
    }

    public void mousePressed(MouseEvent e) {
        int mx = Game.toGameX(e.getX());
        int my = Game.toGameY(e.getY());

        if (Game.gameState == Game.STATE.Paused) {
            int px = pauseX();
            // Resume
            if (hit(mx, my, px, PAUSE_Y0, PAUSE_W, PAUSE_H)) {
                Game.gameState = Game.pausedFrom;
                resetHover(); return;
            }
            // Main Menu
            if (hit(mx, my, px, PAUSE_Y0 + PAUSE_SP, PAUSE_W, PAUSE_H)) {
                resetForMenu();
                Game.gameState = Game.STATE.Menu;
                return;
            }
            // Settings
            if (hit(mx, my, px, PAUSE_Y0 + PAUSE_SP * 2, PAUSE_W, PAUSE_H)) {
                settingsReturnTo = Game.STATE.Paused;
                Game.gameState = Game.STATE.Settings;
                resetHover(); return;
            }
            // Quit
            if (hit(mx, my, px, PAUSE_Y0 + PAUSE_SP * 3, PAUSE_W, PAUSE_H)) {
                System.exit(0);
            }
            return;
        }

        if (Game.gameState == Game.STATE.Menu) {
            int bx = btnX();
            if (hit(mx, my, bx, PLAY_Y, BW, BH)) { Game.gameState = Game.STATE.Select; resetHover(); return; }
            if (hit(mx, my, bx, INFO_Y, BW, BH)) { Game.gameState = Game.STATE.Info; resetHover(); return; }
            if (hit(mx, my, bx, HELP_Y, BW, BH)) { Game.gameState = Game.STATE.Help; resetHover(); return; }
            if (hit(mx, my, musicX(), MUSIC_Y, MUSIC_W, MUSIC_H)) { Game.gameState = Game.STATE.MusicPlayer; resetHover(); return; }
            if (hit(mx, my, settingsX(), SETTINGS_Y, SETTINGS_W, SETTINGS_H)) { Game.gameState = Game.STATE.Settings; resetHover(); return; }
            int[] linkXs = getBottomLinkXs();
            if (hit(mx, my, linkXs[0], 666, linkXs[1] - linkXs[0], 20)) {
                Game.gameState = Game.STATE.About; resetHover(); return;
            }
            if (hit(mx, my, linkXs[1], 666, linkXs[2] - linkXs[1], 20)) {
                Game.gameState = Game.STATE.Update_Notes; resetHover(); return;
            }
            if (hit(mx, my, linkXs[2], 666, linkXs[3] - linkXs[2], 20)) {
                Game.gameState = Game.STATE.Credits; resetHover(); return;
            }
            if (hit(mx, my, quitX(), PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H)) { System.exit(0); }
            return;
        }

        if (Game.gameState == Game.STATE.Select) {
            int bx = btnX();
            if (hit(mx, my, bx, PLAY_Y, BW, BH)) { startGame(0, new BasicEnemy(Game.WIDTH - 50, Game.HEIGHT - 50, ID.BasicEnemy, handler)); return; }
            if (hit(mx, my, bx, INFO_Y, BW, BH)) { startGame(1, new HardEnemy(Game.WIDTH - 100, Game.HEIGHT - 100, ID.BasicEnemy, handler)); return; }
            if (hit(mx, my, bx, HELP_Y, BW, BH)) { startGame(2, new HardEnemy(Game.WIDTH - 50, Game.HEIGHT - 50, ID.BasicEnemy, handler)); return; }
            if (hitBack()) { Game.gameState = Game.STATE.Menu; resetHover(); return; }
            return;
        }

        if (Game.gameState == Game.STATE.Help) {
            int lx = langX();
            if (hit(mx, my, lx, PLAY_Y, LANG_W, BH)) { Game.gameState = Game.STATE.HelpENG; resetHover(); return; }
            if (hit(mx, my, lx, INFO_Y, LANG_W, BH)) { Game.gameState = Game.STATE.HelpNLD; resetHover(); return; }
            if (hit(mx, my, lx, HELP_Y, LANG_W, BH)) { Game.gameState = Game.STATE.HelpDEU; resetHover(); return; }
            if (hitBack()) { Game.gameState = Game.STATE.Menu; resetHover(); return; }
            return;
        }

        if (Game.gameState == Game.STATE.HelpENG || Game.gameState == Game.STATE.HelpNLD
                || Game.gameState == Game.STATE.HelpDEU || Game.gameState == Game.STATE.HelpFRA
                || Game.gameState == Game.STATE.HelpRUS || Game.gameState == Game.STATE.HelpSPA) {
            if (hitBack()) { Game.gameState = Game.STATE.Help; resetHover(); return; }
            return;
        }

        if (Game.gameState == Game.STATE.Settings) {
            handleSettingsClick(mx, my);
            return;
        }

        if (Game.gameState == Game.STATE.Info || Game.gameState == Game.STATE.About
                || Game.gameState == Game.STATE.Update_Notes || Game.gameState == Game.STATE.Credits) {
            if (hitBack()) { Game.gameState = Game.STATE.Menu; resetHover(); return; }
            return;
        }

        if (Game.gameState == Game.STATE.End) {
            if (hit(mx, my, retryX(), RETRY_Y, RETRY_W, RETRY_H)) {
                resetForMenu();
                Game.gameState = Game.STATE.Select;
                return;
            }
            if (hitBack()) {
                resetForMenu();
                Game.gameState = Game.STATE.Menu;
                return;
            }
        }
    }

    private boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean hitBack() {
        return hit(mouseX, mouseY, PageRenderer.backX(), PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H);
    }

    private void resetHover() {
        for (int i = 0; i < 3; i++) btn[i] = 0;
        for (int i = 0; i < 4; i++) pauseBtn[i] = 0;
        backH = musicH = settingsH = aboutH = changelogH = creditsLinkH = quitH = retryH = 0;
        helpScroll = helpScrollTarget = 0;
        settingsScroll = settingsScrollTarget = 0;
        for (int i = 0; i < SET_HOVER_COUNT; i++) setH[i] = 0;
        setMusicBarH = setSfxBarH = 0;
        settingsDrag = SettingsDrag.NONE;
        confirmAction = null;
        confirmYesH = confirmNoH = 0;
    }

    private void startGame(int difficulty, GameObject firstEnemy) {
        handler.getObjects().clear();
        hud.setLevel(1);
        hud.setScore(0);
        hud.setPoints(0);
        hud.bounds = 0;
        hud.resetStats();
        HUD.HEALTH = 100;
        handler.spd = 6;
        game.shop.reset();
        GamePalette.setDifficulty(difficulty);
        GamePalette.reset();
        Stats.newAttempt(difficulty);
        Game.currentAttempt = Stats.getCurrentAttempt();
        Game.attemptFade = 1f;
        Game.gameState = Game.STATE.Game;
        handler.addObject(new Player(Game.WIDTH / 2 - 32, Game.HEIGHT / 2 - 32, ID.Player, handler));
        handler.addObject(firstEnemy);
        game.diff = difficulty;
        hud.triggerWaveAnnounce(); // Wave 1 announcement
    }

    private void resetForMenu() {
        handler.getObjects().clear();
        hud.setLevel(1);
        hud.setScore(0);
        hud.setPoints(0);
        hud.bounds = 0;
        HUD.HEALTH = 100;
        resetHover();
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
            case HelpENG:       renderHelpPage(g2, "English", new String[][]{
                    {"How to Play", "Navigate your character through the arena while avoiding all enemies. Survive as long as possible to increase your score. Each level introduces new and tougher enemies. Boss battles occur at milestone levels. Earn points over time to spend on upgrades in the shop."},
                    {"Controls", "W / \u2191  -  Move up\nA / \u2190  -  Move left\nS / \u2193  -  Move down\nD / \u2192  -  Move right\n\nSpace  -  Open shop\nP / Esc  -  Pause menu"},
                    {"Enemy Types", "Red squares bounce in straight lines. Teal diamonds move at high speed. Purple circles track and follow the player. Yellow triangles change direction randomly. Red bosses are large and spawn bullets."},
                    {"Tips", "Keep moving \u2014 standing still is the fastest way to lose health. Upgrade speed early for better survivability. Save points for health upgrades at higher levels. Each enemy type has a unique shape and behavior. Learn their patterns to dodge effectively."}
                }); break;
            case HelpNLD:       renderHelpPage(g2, "Nederlands", new String[][]{
                    {"Hoe te spelen", "Navigeer je karakter door de arena en ontwijk alle vijanden. Overleef zo lang mogelijk om je score te verhogen. Elk level introduceert nieuwe en sterkere vijanden. Baasgevechten vinden plaats bij mijlpaalniveaus. Verdien punten om upgrades te kopen in de winkel."},
                    {"Besturing", "W / \u2191  -  Omhoog\nA / \u2190  -  Links\nS / \u2193  -  Omlaag\nD / \u2192  -  Rechts\n\nSpatie  -  Winkel openen\nP / Esc  -  Pauzemenu"},
                    {"Vijandtypes", "Rode vierkanten stuiteren in rechte lijnen. Turquoise ruiten bewegen met hoge snelheid. Paarse cirkels volgen de speler. Gele driehoeken veranderen willekeurig van richting. Rode bazen zijn groot en schieten kogels."},
                    {"Tips", "Blijf bewegen \u2014 stilstaan is de snelste manier om gezondheid te verliezen. Upgrade snelheid vroeg voor betere overleving. Spaar punten voor gezondheid bij hogere levels. Elk vijandtype heeft een unieke vorm en gedrag. Leer hun patronen om effectief te ontwijken."}
                }); break;
            case HelpDEU:       renderHelpPage(g2, "Deutsch", new String[][]{
                    {"Spielanleitung", "Navigiere deinen Charakter durch die Arena und weiche allen Feinden aus. \u00DCberlebe so lange wie m\u00F6glich, um deinen Score zu erh\u00F6hen. Jedes Level bringt neue, st\u00E4rkere Feinde. Bossk\u00E4mpfe finden bei Meilensteinleveln statt. Verdiene Punkte f\u00FCr Upgrades im Shop."},
                    {"Steuerung", "W / \u2191  -  Nach oben\nA / \u2190  -  Links\nS / \u2193  -  Nach unten\nD / \u2192  -  Rechts\n\nLeertaste  -  Shop \u00F6ffnen\nP / Esc  -  Pausenmen\u00FC"},
                    {"Gegnertypen", "Rote Quadrate prallen in geraden Linien ab. T\u00FCrkise Rauten bewegen sich mit hoher Geschwindigkeit. Lila Kreise verfolgen den Spieler. Gelbe Dreiecke \u00E4ndern zuf\u00E4llig die Richtung. Rote Bosse sind gro\u00DF und schie\u00DFen Kugeln."},
                    {"Tipps", "Bleib in Bewegung \u2014 Stillstehen kostet am schnellsten Gesundheit. Upgrade Geschwindigkeit fr\u00FCh f\u00FCr besseres \u00DCberleben. Spare Punkte f\u00FCr h\u00F6here Level. Jeder Feindtyp hat eine eigene Form und Verhaltensweise. Lerne ihre Muster."}
                }); break;
            case Settings:      renderSettings(g2); break;
            case Info:          renderInfo(g2); break;
            case About:         renderAbout(g2); break;
            case Update_Notes:  renderUpdates(g2); break;
            case Credits:       renderCredits(g2); break;
            case End:           renderEnd(g2); break;
            case Paused:        renderPauseMenu(g2); break;
            default: break;
        }
    }

    // ---------- Main Menu ----------

    private void renderMainMenu(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawLogo(g, 200);

        int bx = btnX();
        PageRenderer.drawPrimaryButton(g, bx, PLAY_Y, BW, BH, "Play", btn[0]);
        PageRenderer.drawSecondaryButton(g, bx, INFO_Y, BW, BH, "Info", btn[1]);
        PageRenderer.drawSecondaryButton(g, bx, HELP_Y, BW, BH, "Help", btn[2]);

        // Quit (top-right)
        int qx = quitX();
        g.setColor(PageRenderer.lerp(PageRenderer.SURFACE, new Color(38, 50, 68), quitH));
        g.fillRoundRect(qx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H, 8, 8);
        g.setColor(PageRenderer.lerp(PageRenderer.BORDER, PageRenderer.ACCENT, quitH * 0.4f));
        g.drawRoundRect(qx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H, 8, 8);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.TEXT, quitH));
        PageRenderer.drawCenteredString(g, "Quit", qx, PageRenderer.BACK_Y, PageRenderer.BACK_W, PageRenderer.BACK_H);

        // Bottom bar
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("v3.0", 30, 700);

        // Now playing — top left
        if (AudioPlayer.getTrackCount() > 0) {
            g.setFont(PageRenderer.SMALL_FONT);
            g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("Now playing", 24, 28);
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString(AudioPlayer.getCurrentTrack().displayName, 24, 46);
        }

        // About | Changelog | Credits links — centered
        drawBottomLinks(g);

        // Music Player button (bottom right)
        int mx = musicX();
        g.setColor(PageRenderer.lerp(PageRenderer.ACCENT, new Color(110, 225, 218), musicH));
        g.fillRoundRect(mx, MUSIC_Y, MUSIC_W, MUSIC_H, 8, 8);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.BG_DARK);
        PageRenderer.drawCenteredString(g, "Music Player", mx, MUSIC_Y, MUSIC_W, MUSIC_H);

        // Settings button (bottom left)
        int sx = settingsX();
        g.setColor(PageRenderer.lerp(PageRenderer.SURFACE, new Color(38, 50, 68), settingsH));
        g.fillRoundRect(sx, SETTINGS_Y, SETTINGS_W, SETTINGS_H, 8, 8);
        g.setColor(PageRenderer.lerp(PageRenderer.BORDER, PageRenderer.ACCENT, settingsH * 0.4f));
        g.drawRoundRect(sx, SETTINGS_Y, SETTINGS_W, SETTINGS_H, 8, 8);
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.TEXT, settingsH));
        PageRenderer.drawCenteredString(g, "Settings", sx, SETTINGS_Y, SETTINGS_W, SETTINGS_H);
    }

    // ---------- Select Difficulty ----------

    private void renderSelect(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Select Difficulty");
        PageRenderer.drawBackButton(g, backH);

        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        String sub = "Choose your challenge";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(sub, (Game.WIDTH - fm.stringWidth(sub)) / 2, 130);

        int bx = btnX();
        PageRenderer.drawSecondaryButton(g, bx, PLAY_Y, BW, BH, "Normal", btn[0]);
        PageRenderer.drawWarningButton(g, bx, INFO_Y, BW, BH, "Hard", btn[1]);
        PageRenderer.drawDangerButton(g, bx, HELP_Y, BW, BH, "Insane", btn[2]);

        g.setFont(PageRenderer.SMALL_FONT);
        int labelX = bx + BW + 20;
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT_MUTED, PageRenderer.TEXT_SEC, btn[0]));
        g.drawString("Recommended for beginners", labelX, PLAY_Y + 30);
        g.setColor(PageRenderer.lerp(new Color(245, 195, 68, 120), new Color(255, 220, 100), btn[1]));
        g.drawString("Unpredictable enemies", labelX, INFO_Y + 30);
        g.setColor(PageRenderer.lerp(new Color(235, 87, 87, 120), new Color(255, 120, 120), btn[2]));
        g.drawString("Only for the brave", labelX, HELP_Y + 30);
    }

    // ---------- Help ----------

    private void renderHelp(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Help");
        PageRenderer.drawBackButton(g, backH);

        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        String sub = "Select your language";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(sub, (Game.WIDTH - fm.stringWidth(sub)) / 2, 130);

        int lx = langX();
        PageRenderer.drawSecondaryButton(g, lx, PLAY_Y, LANG_W, BH, "English", btn[0]);
        PageRenderer.drawSecondaryButton(g, lx, INFO_Y, LANG_W, BH, "Nederlands", btn[1]);
        PageRenderer.drawSecondaryButton(g, lx, HELP_Y, LANG_W, BH, "Deutsch", btn[2]);
    }

    // ---------- Help Language ----------

    private void renderHelpPage(Graphics2D g, String language, String[][] sections) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Help");
        PageRenderer.drawBackButton(g, backH);

        // Language badge
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.ACCENT);
        FontMetrics badgeFm = g.getFontMetrics();
        String badge = language.toUpperCase();
        int bw = badgeFm.stringWidth(badge) + 20;
        int bxp = (Game.WIDTH - bw) / 2;
        g.drawRoundRect(bxp, 105, bw, 24, 6, 6);
        g.drawString(badge, bxp + 10, 122);

        // Content area
        int margin = 60;
        int panelW = Game.WIDTH - margin * 2;
        int contentTop = 145;
        int contentBottom = Game.HEIGHT - 30;
        int gap = 12;
        int padding = 20;
        int lineH = 22;

        // Clip to content area
        java.awt.Shape oldClip = g.getClip();
        g.clipRect(0, contentTop, Game.WIDTH, contentBottom - contentTop);

        // Calculate total content height for scroll clamping
        g.setFont(PageRenderer.BODY_FONT);
        FontMetrics bodyFm = g.getFontMetrics();
        int totalHeight = 0;
        for (String[] section : sections) {
            int sectionContentH = measureSectionHeight(section[1], panelW - padding * 2, bodyFm, lineH);
            totalHeight += 52 + sectionContentH + padding + gap;
        }
        int maxScroll = Math.max(0, totalHeight - (contentBottom - contentTop) + 20);
        helpScrollTarget = Math.min(helpScrollTarget, maxScroll);

        int y = contentTop - (int) helpScroll;

        for (String[] section : sections) {
            String title = section[0];
            String text = section[1];

            // Measure this section
            int sectionContentH = measureSectionHeight(text, panelW - padding * 2, bodyFm, lineH);
            int panelH = 52 + sectionContentH + padding;

            // Only draw if visible
            if (y + panelH > contentTop - 20 && y < contentBottom + 20) {
                PageRenderer.drawPanel(g, margin, y, panelW, panelH);

                g.setFont(PageRenderer.HEADING_FONT);
                g.setColor(PageRenderer.ACCENT);
                g.drawString(title, margin + padding, y + 34);

                g.setColor(PageRenderer.BORDER);
                g.fillRect(margin + padding, y + 46, panelW - padding * 2, 1);

                g.setFont(PageRenderer.BODY_FONT);
                g.setColor(PageRenderer.TEXT_SEC);
                drawWrappedText(g, text, margin + padding, y + 68, panelW - padding * 2, lineH);
            }

            y += panelH + gap;
        }

        g.setClip(oldClip);

        // Scroll indicator if content overflows
        if (maxScroll > 0) {
            float scrollPct = (helpScrollTarget > 0) ? helpScroll / maxScroll : 0;
            int trackH = contentBottom - contentTop - 20;
            int thumbH = Math.max(30, (int) ((float) (contentBottom - contentTop) / totalHeight * trackH));
            int thumbY = contentTop + 10 + (int) ((trackH - thumbH) * scrollPct);
            g.setColor(new Color(40, 52, 70, 80));
            g.fillRoundRect(Game.WIDTH - 22, contentTop + 10, 6, trackH, 3, 3);
            g.setColor(new Color(78, 205, 196, 120));
            g.fillRoundRect(Game.WIDTH - 22, thumbY, 6, thumbH, 3, 3);
        }
    }

    private int measureSectionHeight(String text, int maxW, FontMetrics fm, int lineH) {
        String[] parts = text.split("\n", -1);
        int lines = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                lines++;
            } else {
                lines += wrapLineCount(part, maxW, fm);
            }
        }
        return lines * lineH;
    }

    private int wrapLineCount(String text, int maxW, FontMetrics fm) {
        String[] words = text.split(" ");
        int count = 1;
        int lineW = 0;
        for (String word : words) {
            int wordW = fm.stringWidth(word + " ");
            if (lineW + wordW > maxW && lineW > 0) {
                count++;
                lineW = 0;
            }
            lineW += wordW;
        }
        return count;
    }

    private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxW, int lineH) {
        FontMetrics fm = g.getFontMetrics();
        String[] parts = text.split("\n", -1);
        int cy = y;
        for (String part : parts) {
            if (part.isEmpty()) {
                cy += lineH;
                continue;
            }
            String[] words = part.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(test) > maxW && line.length() > 0) {
                    g.drawString(line.toString(), x, cy);
                    cy += lineH;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (line.length() > 0) {
                g.drawString(line.toString(), x, cy);
                cy += lineH;
            }
        }
    }

    // ---------- Settings ----------

    // Layout constants for settings page
    private static final int SET_MARGIN = 60;
    private static final int SET_PAD = 20;
    private static final int SET_ROW_H = 42;
    private static final int SET_SLIDER_W = 200;
    private static final int SET_SLIDER_H = 5;
    private static final int SET_SLIDER_KNOB = 12;
    private static final int SET_DRAG_MARGIN = 16;
    private static final int SET_OPT_W = 80;
    private static final int SET_OPT_H = 32;
    private static final int SET_OPT_GAP = 8;
    private static final int SET_TOGGLE_W = 50;
    private static final int SET_TOGGLE_H = 26;
    private static final int SET_LANG_W = 120;
    private static final int SET_LANG_H = 34;
    private static final int SET_LANG_GAP = 10;
    private static final int SET_DANGER_W = 260;
    private static final int SET_DANGER_H = 40;

    private static int setPanelW() { return Game.WIDTH - SET_MARGIN * 2; }
    private static int setLabelX() { return SET_MARGIN + SET_PAD; }
    private static int setControlX() { return SET_MARGIN + setPanelW() / 2 + 20; }

    private float settingsSliderRatio(int mx) {
        return Math.max(0f, Math.min(1f, (float)(mx - setControlX()) / SET_SLIDER_W));
    }

    private boolean hitSlider(int mx, int my, int sliderY) {
        return mx >= setControlX() - SET_DRAG_MARGIN && mx <= setControlX() + SET_SLIDER_W + SET_DRAG_MARGIN
                && my >= sliderY - SET_DRAG_MARGIN && my <= sliderY + SET_SLIDER_H + SET_DRAG_MARGIN;
    }

    private void updateSettingsHover() {
        // Slider bar hovers
        boolean onMusic = settingsDrag == SettingsDrag.MUSIC_VOL
                || hitSlider(mouseX, mouseY, getSettingsRowY(0));
        boolean onSfx = settingsDrag == SettingsDrag.SFX_VOL
                || hitSlider(mouseX, mouseY, getSettingsRowY(1));
        setMusicBarH += ((onMusic ? 1f : 0f) - setMusicBarH) * LERP;
        setSfxBarH += ((onSfx ? 1f : 0f) - setSfxBarH) * LERP;

        // Option button hovers
        boolean[] targets = getSettingsHoverTargets();
        for (int i = 0; i < SET_HOVER_COUNT; i++) {
            setH[i] = approach(setH[i], targets[i]);
        }

        // Confirm dialog hovers
        if (confirmAction != null) {
            int cx = Game.WIDTH / 2;
            int dy = Game.HEIGHT / 2;
            confirmYesH = approach(confirmYesH, hit(mouseX, mouseY, cx - 130, dy + 30, 120, 38));
            confirmNoH = approach(confirmNoH, hit(mouseX, mouseY, cx + 10, dy + 30, 120, 38));
        }
    }

    private int getSettingsRowY(int row) {
        // Returns approximate Y for a settings row, adjusted by scroll
        // Row 0-1: Audio (panel starts at y=130)
        // Row 2: Screen shake (panel starts ~260)
        // etc. — computed dynamically during render, so for hover we compute it here too
        int contentTop = 130;
        int y = contentTop - (int) settingsScroll;
        int gap = 12;

        // Audio panel: rows 0,1
        if (row <= 1) return y + 52 + row * SET_ROW_H + SET_ROW_H / 2;

        // Visual panel: starts after audio
        int audioH = 52 + 2 * SET_ROW_H + SET_PAD;
        y += audioH + gap;
        // rows 2-7 are visual (shake=0-2, particle=3-5 within visual, but mapped as global indices 2-7)
        if (row <= 7) return y + 52 + (row - 2) * SET_ROW_H + SET_ROW_H / 2;

        // General panel
        int visualH = 52 + 6 * SET_ROW_H + SET_PAD;
        y += visualH + gap;
        if (row <= 9) return y + 52 + (row - 8) * SET_ROW_H + SET_ROW_H / 2;

        // Controls panel
        int generalH = 52 + 2 * SET_ROW_H + SET_PAD;
        y += generalH + gap;
        if (row <= 10) return y + 52 + (row - 10) * SET_ROW_H + SET_ROW_H / 2;

        // Danger panel
        int controlsH = 52 + 5 * 28 + SET_PAD;
        y += controlsH + gap;
        return y + 52 + (row - 11) * SET_ROW_H + SET_ROW_H / 2;
    }

    private boolean[] getSettingsHoverTargets() {
        boolean[] t = new boolean[SET_HOVER_COUNT];
        if (confirmAction != null) return t; // Don't hover underneath dialog

        int contentTop = 130;
        int contentBottom = Game.HEIGHT - 30;
        int gap = 12;
        int cx = setControlX();
        int y = contentTop - (int) settingsScroll;

        // --- Audio panel ---
        int audioH = 52 + 2 * SET_ROW_H + SET_PAD;
        // Sliders handled separately via bar hover
        y += audioH + gap;

        // --- Visual panel ---
        int rowY;
        // Screen shake (row 0 in visual)
        rowY = y + 52 + SET_ROW_H / 2 - SET_OPT_H / 2;
        for (int i = 0; i < 3; i++) {
            int ox = cx + i * (SET_OPT_W + SET_OPT_GAP);
            t[2 + i] = hit(mouseX, mouseY, ox, rowY, SET_OPT_W, SET_OPT_H)
                    && mouseY > contentTop && mouseY < contentBottom;
        }

        // Particle density (row 1 in visual)
        rowY = y + 52 + SET_ROW_H + SET_ROW_H / 2 - SET_OPT_H / 2;
        for (int i = 0; i < 3; i++) {
            int ox = cx + i * (SET_OPT_W + SET_OPT_GAP);
            t[5 + i] = hit(mouseX, mouseY, ox, rowY, SET_OPT_W, SET_OPT_H)
                    && mouseY > contentTop && mouseY < contentBottom;
        }

        // FPS toggle (row 2)
        rowY = y + 52 + 2 * SET_ROW_H + SET_ROW_H / 2 - SET_TOGGLE_H / 2;
        t[8] = hit(mouseX, mouseY, cx, rowY, SET_TOGGLE_W, SET_TOGGLE_H)
                && mouseY > contentTop && mouseY < contentBottom;

        // Trail toggle (row 3)
        rowY = y + 52 + 3 * SET_ROW_H + SET_ROW_H / 2 - SET_TOGGLE_H / 2;
        t[9] = hit(mouseX, mouseY, cx, rowY, SET_TOGGLE_W, SET_TOGGLE_H)
                && mouseY > contentTop && mouseY < contentBottom;

        // Grid toggle (row 4)
        rowY = y + 52 + 4 * SET_ROW_H + SET_ROW_H / 2 - SET_TOGGLE_H / 2;
        t[10] = hit(mouseX, mouseY, cx, rowY, SET_TOGGLE_W, SET_TOGGLE_H)
                && mouseY > contentTop && mouseY < contentBottom;

        // Colorblind toggle (row 5)
        rowY = y + 52 + 5 * SET_ROW_H + SET_ROW_H / 2 - SET_TOGGLE_H / 2;
        t[11] = hit(mouseX, mouseY, cx, rowY, SET_TOGGLE_W, SET_TOGGLE_H)
                && mouseY > contentTop && mouseY < contentBottom;

        int visualH = 52 + 6 * SET_ROW_H + SET_PAD;
        y += visualH + gap;

        // --- General panel ---
        // Language buttons (row 0)
        rowY = y + 52 + SET_ROW_H / 2 - SET_LANG_H / 2;
        for (int i = 0; i < 3; i++) {
            int lx = cx + i * (SET_LANG_W / 2 + SET_LANG_GAP);
            int lw = SET_LANG_W / 2;
            t[12 + i] = hit(mouseX, mouseY, lx, rowY, lw, SET_LANG_H)
                    && mouseY > contentTop && mouseY < contentBottom;
        }

        int generalH = 52 + 2 * SET_ROW_H + SET_PAD;
        y += generalH + gap;

        // --- Controls panel (read-only) ---
        int controlsH = 52 + 5 * 28 + SET_PAD;
        y += controlsH + gap;

        // --- Danger zone ---
        rowY = y + 52 + SET_ROW_H / 2 - SET_DANGER_H / 2;
        int dangerCx = (Game.WIDTH - (SET_DANGER_W * 2 + 20)) / 2;
        t[18] = hit(mouseX, mouseY, dangerCx, rowY, SET_DANGER_W, SET_DANGER_H)
                && mouseY > contentTop && mouseY < contentBottom;
        t[19] = hit(mouseX, mouseY, dangerCx + SET_DANGER_W + 20, rowY, SET_DANGER_W, SET_DANGER_H)
                && mouseY > contentTop && mouseY < contentBottom;

        return t;
    }

    private void handleSettingsClick(int mx, int my) {
        // Confirmation dialog takes priority
        if (confirmAction != null) {
            int cx = Game.WIDTH / 2;
            int dy = Game.HEIGHT / 2;
            if (hit(mx, my, cx - 130, dy + 30, 120, 38)) {
                // Confirm yes
                if ("scores".equals(confirmAction)) Stats.resetHighScores();
                else if ("progress".equals(confirmAction)) { Stats.resetAll(); Settings.resetToDefaults(); }
                confirmAction = null;
            } else if (hit(mx, my, cx + 10, dy + 30, 120, 38)) {
                confirmAction = null; // Cancel
            }
            return;
        }

        // Back
        if (hitBack()) {
            Game.gameState = (settingsReturnTo != null) ? settingsReturnTo : Game.STATE.Menu;
            settingsReturnTo = null;
            resetHover(); return;
        }

        int contentTop = 130;
        int contentBottom = Game.HEIGHT - 30;
        if (my < contentTop || my > contentBottom) return;

        int gap = 12;
        int cx2 = setControlX();
        int y = contentTop - (int) settingsScroll;

        // --- Audio: sliders ---
        int audioH = 52 + 2 * SET_ROW_H + SET_PAD;
        int sliderY0 = y + 52 + SET_ROW_H / 2 - SET_SLIDER_H / 2;
        int sliderY1 = y + 52 + SET_ROW_H + SET_ROW_H / 2 - SET_SLIDER_H / 2;

        if (hitSlider(mx, my, sliderY0)) {
            settingsDrag = SettingsDrag.MUSIC_VOL;
            Settings.setMusicVolume(settingsSliderRatio(mx));
            return;
        }
        if (hitSlider(mx, my, sliderY1)) {
            settingsDrag = SettingsDrag.SFX_VOL;
            Settings.setSfxVolume(settingsSliderRatio(mx));
            return;
        }

        y += audioH + gap;

        // --- Visual: options & toggles ---
        int rowY;

        // Screen shake
        rowY = y + 52 + SET_ROW_H / 2 - SET_OPT_H / 2;
        for (int i = 0; i < 3; i++) {
            int ox = cx2 + i * (SET_OPT_W + SET_OPT_GAP);
            if (hit(mx, my, ox, rowY, SET_OPT_W, SET_OPT_H)) {
                Settings.setScreenShake(i); return;
            }
        }

        // Particle density
        rowY = y + 52 + SET_ROW_H + SET_ROW_H / 2 - SET_OPT_H / 2;
        for (int i = 0; i < 3; i++) {
            int ox = cx2 + i * (SET_OPT_W + SET_OPT_GAP);
            if (hit(mx, my, ox, rowY, SET_OPT_W, SET_OPT_H)) {
                Settings.setParticleDensity(i); return;
            }
        }

        // FPS toggle
        rowY = y + 52 + 2 * SET_ROW_H + SET_ROW_H / 2 - SET_TOGGLE_H / 2;
        if (hit(mx, my, cx2, rowY, SET_TOGGLE_W, SET_TOGGLE_H)) {
            Settings.setShowFps(!Settings.getShowFps()); return;
        }

        // Trail toggle
        rowY = y + 52 + 3 * SET_ROW_H + SET_ROW_H / 2 - SET_TOGGLE_H / 2;
        if (hit(mx, my, cx2, rowY, SET_TOGGLE_W, SET_TOGGLE_H)) {
            Settings.setPlayerTrail(!Settings.getPlayerTrail()); return;
        }

        // Grid toggle
        rowY = y + 52 + 4 * SET_ROW_H + SET_ROW_H / 2 - SET_TOGGLE_H / 2;
        if (hit(mx, my, cx2, rowY, SET_TOGGLE_W, SET_TOGGLE_H)) {
            Settings.setGridDots(!Settings.getGridDots()); return;
        }

        // Colorblind toggle
        rowY = y + 52 + 5 * SET_ROW_H + SET_ROW_H / 2 - SET_TOGGLE_H / 2;
        if (hit(mx, my, cx2, rowY, SET_TOGGLE_W, SET_TOGGLE_H)) {
            Settings.setColorblindMode(!Settings.getColorblindMode()); return;
        }

        int visualH = 52 + 6 * SET_ROW_H + SET_PAD;
        y += visualH + gap;

        // --- General: language ---
        rowY = y + 52 + SET_ROW_H / 2 - SET_LANG_H / 2;
        for (int i = 0; i < 3; i++) {
            int lx = cx2 + i * (SET_LANG_W / 2 + SET_LANG_GAP);
            int lw = SET_LANG_W / 2;
            if (hit(mx, my, lx, rowY, lw, SET_LANG_H)) {
                Settings.setLanguage(i); return;
            }
        }

        int generalH = 52 + 2 * SET_ROW_H + SET_PAD;
        y += generalH + gap;

        // --- Controls (read-only) ---
        int controlsH = 52 + 5 * 28 + SET_PAD;
        y += controlsH + gap;

        // --- Danger zone ---
        rowY = y + 52 + SET_ROW_H / 2 - SET_DANGER_H / 2;
        int dangerCx = (Game.WIDTH - (SET_DANGER_W * 2 + 20)) / 2;
        if (hit(mx, my, dangerCx, rowY, SET_DANGER_W, SET_DANGER_H)) {
            confirmAction = "scores"; return;
        }
        if (hit(mx, my, dangerCx + SET_DANGER_W + 20, rowY, SET_DANGER_W, SET_DANGER_H)) {
            confirmAction = "progress"; return;
        }
    }

    private void renderSettings(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Settings");
        PageRenderer.drawBackButton(g, backH);

        int contentTop = 130;
        int contentBottom = Game.HEIGHT - 30;
        int pw = setPanelW();
        int gap = 12;
        int cx = setControlX();
        int lx = setLabelX();

        // Clip to content area
        java.awt.Shape oldClip = g.getClip();
        g.clipRect(0, contentTop, Game.WIDTH, contentBottom - contentTop);

        int y = contentTop - (int) settingsScroll;

        // ===== AUDIO PANEL =====
        int audioH = 52 + 2 * SET_ROW_H + SET_PAD;
        if (y + audioH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, SET_MARGIN, y, pw, audioH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("Audio", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - SET_PAD * 2, 1);

            // Music volume
            int rowY = y + 52 + SET_ROW_H / 2;
            g.setFont(PageRenderer.BODY_FONT);
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("Music Volume", lx, rowY + 5);
            renderSlider(g, cx, rowY - SET_SLIDER_H / 2, Settings.getMusicVolume(), setMusicBarH);

            // SFX volume
            rowY = y + 52 + SET_ROW_H + SET_ROW_H / 2;
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("SFX Volume", lx, rowY + 5);
            renderSlider(g, cx, rowY - SET_SLIDER_H / 2, Settings.getSfxVolume(), setSfxBarH);
        }
        y += audioH + gap;

        // ===== VISUAL PANEL =====
        int visualH = 52 + 6 * SET_ROW_H + SET_PAD;
        if (y + visualH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, SET_MARGIN, y, pw, visualH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("Visual", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - SET_PAD * 2, 1);

            // Screen shake
            int rowY = y + 52 + SET_ROW_H / 2;
            g.setFont(PageRenderer.BODY_FONT);
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("Screen Shake", lx, rowY + 5);
            String[] shakeLabels = {"Off", "Low", "High"};
            renderOptionGroup(g, cx, rowY - SET_OPT_H / 2, shakeLabels, Settings.getScreenShake(), 2);

            // Particle density
            rowY = y + 52 + SET_ROW_H + SET_ROW_H / 2;
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("Particle Density", lx, rowY + 5);
            String[] partLabels = {"Low", "Medium", "High"};
            renderOptionGroup(g, cx, rowY - SET_OPT_H / 2, partLabels, Settings.getParticleDensity(), 5);

            // Show FPS
            rowY = y + 52 + 2 * SET_ROW_H + SET_ROW_H / 2;
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("Show FPS", lx, rowY + 5);
            renderToggle(g, cx, rowY - SET_TOGGLE_H / 2, Settings.getShowFps(), setH[8]);

            // Player trail
            rowY = y + 52 + 3 * SET_ROW_H + SET_ROW_H / 2;
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("Player Trail", lx, rowY + 5);
            renderToggle(g, cx, rowY - SET_TOGGLE_H / 2, Settings.getPlayerTrail(), setH[9]);

            // Grid dots
            rowY = y + 52 + 4 * SET_ROW_H + SET_ROW_H / 2;
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("Grid Dots", lx, rowY + 5);
            renderToggle(g, cx, rowY - SET_TOGGLE_H / 2, Settings.getGridDots(), setH[10]);

            // Colorblind mode
            rowY = y + 52 + 5 * SET_ROW_H + SET_ROW_H / 2;
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("Colorblind Mode", lx, rowY + 5);
            renderToggle(g, cx, rowY - SET_TOGGLE_H / 2, Settings.getColorblindMode(), setH[11]);
        }
        y += visualH + gap;

        // ===== GENERAL PANEL =====
        int generalH = 52 + 2 * SET_ROW_H + SET_PAD;
        if (y + generalH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, SET_MARGIN, y, pw, generalH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("General", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - SET_PAD * 2, 1);

            // Language
            int rowY = y + 52 + SET_ROW_H / 2;
            g.setFont(PageRenderer.BODY_FONT);
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString("Language", lx, rowY + 5);
            String[] langLabels = {"ENG", "NLD", "DEU"};
            int curLang = Settings.getLanguage();
            for (int i = 0; i < 3; i++) {
                int lbx = cx + i * (SET_LANG_W / 2 + SET_LANG_GAP);
                int lbw = SET_LANG_W / 2;
                int lby = rowY - SET_LANG_H / 2;
                boolean active = (i == curLang);
                if (active) {
                    g.setColor(PageRenderer.ACCENT);
                    g.fillRoundRect(lbx, lby, lbw, SET_LANG_H, 6, 6);
                    g.setFont(PageRenderer.LABEL_FONT);
                    g.setColor(PageRenderer.BG_DARK);
                } else {
                    g.setColor(PageRenderer.lerp(PageRenderer.SURFACE, new Color(38, 50, 68), setH[12 + i]));
                    g.fillRoundRect(lbx, lby, lbw, SET_LANG_H, 6, 6);
                    g.setColor(PageRenderer.lerp(PageRenderer.BORDER, PageRenderer.ACCENT, setH[12 + i] * 0.4f));
                    g.drawRoundRect(lbx, lby, lbw, SET_LANG_H, 6, 6);
                    g.setFont(PageRenderer.LABEL_FONT);
                    g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.TEXT, setH[12 + i]));
                }
                PageRenderer.drawCenteredString(g, langLabels[i], lbx, lby, lbw, SET_LANG_H);
            }

            // Help redirect note
            rowY = y + 52 + SET_ROW_H + SET_ROW_H / 2;
            g.setFont(PageRenderer.SMALL_FONT);
            g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("Help pages will open in the selected language", lx, rowY + 5);
        }
        y += generalH + gap;

        // ===== CONTROLS PANEL =====
        int controlLineH = 28;
        int controlsH = 52 + 5 * controlLineH + SET_PAD;
        if (y + controlsH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, SET_MARGIN, y, pw, controlsH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("Controls", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - SET_PAD * 2, 1);

            int ky = y + 68;
            g.setFont(PageRenderer.BODY_FONT);
            String[][] controls = {
                {"W / \u2191  A / \u2190  S / \u2193  D / \u2192", "Move"},
                {"Space", "Open Shop"},
                {"P / Esc", "Pause"},
                {"Esc", "Back (menus)"},
                {"Mouse Wheel", "Scroll"}
            };
            for (String[] c : controls) {
                g.setColor(PageRenderer.TEXT);
                g.drawString(c[0], lx, ky);
                g.setColor(PageRenderer.TEXT_MUTED);
                g.drawString(c[1], cx, ky);
                ky += controlLineH;
            }
        }
        y += controlsH + gap;

        // ===== DANGER ZONE PANEL =====
        int dangerH = 52 + SET_ROW_H + SET_PAD;
        if (y + dangerH > contentTop - 20 && y < contentBottom + 20) {
            // Danger panel with red border
            g.setColor(PageRenderer.SURFACE);
            g.fillRoundRect(SET_MARGIN, y, pw, dangerH, PageRenderer.R, PageRenderer.R);
            g.setColor(new Color(235, 87, 87, 60));
            g.drawRoundRect(SET_MARGIN, y, pw, dangerH, PageRenderer.R, PageRenderer.R);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.DANGER);
            g.drawString("Danger Zone", lx, y + 34);
            g.setColor(new Color(235, 87, 87, 60));
            g.fillRect(lx, y + 46, pw - SET_PAD * 2, 1);

            int rowY = y + 52 + SET_ROW_H / 2 - SET_DANGER_H / 2;
            int dangerCx = (Game.WIDTH - (SET_DANGER_W * 2 + 20)) / 2;
            PageRenderer.drawDangerButton(g, dangerCx, rowY, SET_DANGER_W, SET_DANGER_H, "Reset High Scores", setH[18]);
            PageRenderer.drawDangerButton(g, dangerCx + SET_DANGER_W + 20, rowY, SET_DANGER_W, SET_DANGER_H, "Reset All Progress", setH[19]);
        }
        y += dangerH + gap;

        int totalHeight = y + (int) settingsScroll - contentTop;
        int maxScroll = Math.max(0, totalHeight - (contentBottom - contentTop) + 20);
        settingsScrollTarget = Math.min(settingsScrollTarget, maxScroll);

        g.setClip(oldClip);

        // Scroll indicator
        if (maxScroll > 0) {
            float scrollPct = (settingsScrollTarget > 0) ? settingsScroll / maxScroll : 0;
            int trackH = contentBottom - contentTop - 20;
            int thumbH = Math.max(30, (int) ((float) (contentBottom - contentTop) / totalHeight * trackH));
            int thumbY = contentTop + 10 + (int) ((trackH - thumbH) * scrollPct);
            g.setColor(new Color(40, 52, 70, 80));
            g.fillRoundRect(Game.WIDTH - 22, contentTop + 10, 6, trackH, 3, 3);
            g.setColor(new Color(78, 205, 196, 120));
            g.fillRoundRect(Game.WIDTH - 22, thumbY, 6, thumbH, 3, 3);
        }

        // Confirmation dialog overlay
        if (confirmAction != null) {
            renderConfirmDialog(g);
        }
    }

    private void renderSlider(Graphics2D g, int x, int y, float value, float barH) {
        // Background bar
        g.setColor(PageRenderer.lerp(PageRenderer.BORDER, new Color(55, 70, 90), barH));
        g.fillRoundRect(x, y, SET_SLIDER_W, SET_SLIDER_H, 3, 3);

        // Filled portion
        int filledW = (int) (SET_SLIDER_W * value);
        if (filledW > 0) {
            g.setColor(PageRenderer.ACCENT);
            g.fillRoundRect(x, y, filledW, SET_SLIDER_H, 3, 3);
        }

        // Knob
        int knobSize = SET_SLIDER_KNOB + (int) (3 * barH);
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT, PageRenderer.ACCENT, barH));
        g.fillOval(x + filledW - knobSize / 2, y + SET_SLIDER_H / 2 - knobSize / 2, knobSize, knobSize);

        // Percentage label
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String pct = Math.round(value * 100) + "%";
        g.drawString(pct, x + SET_SLIDER_W + 14, y + SET_SLIDER_H / 2 + 5);
    }

    private void renderOptionGroup(Graphics2D g, int x, int y, String[] labels, int selected, int hoverOffset) {
        for (int i = 0; i < labels.length; i++) {
            int ox = x + i * (SET_OPT_W + SET_OPT_GAP);
            boolean active = (i == selected);
            float h = setH[hoverOffset + i];
            if (active) {
                g.setColor(PageRenderer.ACCENT);
                g.fillRoundRect(ox, y, SET_OPT_W, SET_OPT_H, 6, 6);
                g.setFont(PageRenderer.LABEL_FONT);
                g.setColor(PageRenderer.BG_DARK);
            } else {
                g.setColor(PageRenderer.lerp(PageRenderer.SURFACE, new Color(38, 50, 68), h));
                g.fillRoundRect(ox, y, SET_OPT_W, SET_OPT_H, 6, 6);
                g.setColor(PageRenderer.lerp(PageRenderer.BORDER, PageRenderer.ACCENT, h * 0.4f));
                g.drawRoundRect(ox, y, SET_OPT_W, SET_OPT_H, 6, 6);
                g.setFont(PageRenderer.LABEL_FONT);
                g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.TEXT, h));
            }
            PageRenderer.drawCenteredString(g, labels[i], ox, y, SET_OPT_W, SET_OPT_H);
        }
    }

    private void renderToggle(Graphics2D g, int x, int y, boolean on, float hover) {
        Color bg = on
                ? PageRenderer.lerp(PageRenderer.ACCENT, new Color(110, 225, 218), hover)
                : PageRenderer.lerp(PageRenderer.SURFACE_LIGHT, new Color(48, 62, 82), hover);
        Color border = on
                ? PageRenderer.lerp(PageRenderer.ACCENT, new Color(110, 225, 218), hover)
                : PageRenderer.lerp(PageRenderer.BORDER, PageRenderer.ACCENT, hover * 0.3f);

        g.setColor(bg);
        g.fillRoundRect(x, y, SET_TOGGLE_W, SET_TOGGLE_H, SET_TOGGLE_H, SET_TOGGLE_H);
        if (!on) {
            g.setColor(border);
            g.drawRoundRect(x, y, SET_TOGGLE_W, SET_TOGGLE_H, SET_TOGGLE_H, SET_TOGGLE_H);
        }

        // Knob
        int knobSize = SET_TOGGLE_H - 6;
        int knobX = on ? x + SET_TOGGLE_W - knobSize - 3 : x + 3;
        g.setColor(on ? PageRenderer.BG_DARK : PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.TEXT, hover));
        g.fillOval(knobX, y + 3, knobSize, knobSize);
    }

    private void renderConfirmDialog(Graphics2D g) {
        // Dim overlay
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        int dw = 400;
        int dh = 160;
        int dx = (Game.WIDTH - dw) / 2;
        int dy = (Game.HEIGHT - dh) / 2;

        // Dialog panel
        g.setColor(PageRenderer.SURFACE);
        g.fillRoundRect(dx, dy, dw, dh, PageRenderer.R, PageRenderer.R);
        g.setColor(PageRenderer.DANGER);
        g.drawRoundRect(dx, dy, dw, dh, PageRenderer.R, PageRenderer.R);

        // Title
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.DANGER);
        String title = "scores".equals(confirmAction) ? "Reset High Scores?" : "Reset All Progress?";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (Game.WIDTH - fm.stringWidth(title)) / 2, dy + 40);

        // Description
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        String desc = "scores".equals(confirmAction)
                ? "All high scores will be permanently erased."
                : "All scores, attempts, and settings will be reset.";
        fm = g.getFontMetrics();
        g.drawString(desc, (Game.WIDTH - fm.stringWidth(desc)) / 2, dy + 72);

        // Buttons
        int btnY = dy + dh / 2 + 30;
        int cxd = Game.WIDTH / 2;
        PageRenderer.drawDangerButton(g, cxd - 130, btnY, 120, 38, "Confirm", confirmYesH);
        PageRenderer.drawSecondaryButton(g, cxd + 10, btnY, 120, 38, "Cancel", confirmNoH);
    }

    // ---------- Info ----------

    private void renderInfo(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Info");
        PageRenderer.drawBackButton(g, backH);

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
        PageRenderer.drawBackButton(g, backH);

        int margin = 60;
        int panelW = Game.WIDTH - margin * 2;
        int pad = 20;
        int lineH = 22;
        int gap = 12;
        int y = 130;

        // The Game
        g.setFont(PageRenderer.BODY_FONT);
        FontMetrics fm = g.getFontMetrics();
        String gameDesc = "Dotch. is a fast-paced dodge game where your only goal is survival. Navigate through waves of enemies that grow stronger with each level. Earn points to upgrade your speed, health, and survivability. Originally created in 2016 and rebuilt from the ground up in 2026.";
        int gameH = 52 + measureSectionHeight(gameDesc, panelW - pad * 2, fm, lineH) + pad;
        PageRenderer.drawPanel(g, margin, y, panelW, gameH);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("The Game", margin + pad, y + 34);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(margin + pad, y + 46, panelW - pad * 2, 1);
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        drawWrappedText(g, gameDesc, margin + pad, y + 68, panelW - pad * 2, lineH);
        y += gameH + gap;

        // Game Modes
        String modesDesc = "Normal  \u2014  Predictable bouncing enemies. Recommended for new players.\n\nHard  \u2014  Enemies randomize direction on each bounce. Unpredictable and challenging.\n\nInsane  \u2014  Fast, smart, and random enemies combined. For experienced players only.";
        int modesH = 52 + measureSectionHeight(modesDesc, panelW - pad * 2, fm, lineH) + pad;
        PageRenderer.drawPanel(g, margin, y, panelW, modesH);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Game Modes", margin + pad, y + 34);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(margin + pad, y + 46, panelW - pad * 2, 1);
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        drawWrappedText(g, modesDesc, margin + pad, y + 68, panelW - pad * 2, lineH);
        y += modesH + gap;

        // Enemy Types
        int enemyPanelY = y;
        int iconCol = margin + pad;
        int textCol = iconCol + 26;
        int enemyLineH = 28;
        String[][] enemies = {
                {"square", "Basic", "Bounces in straight lines at constant speed.", "235,87,87"},
                {"diamond", "Fast", "High speed diagonal movement across the arena.", "78,205,196"},
                {"circle", "Smart", "Tracks and follows the player continuously.", "199,125,255"},
                {"triangle", "Hard", "Changes direction randomly on every bounce.", "245,195,68"},
                {"boss", "Boss", "Large enemy that spawns bullets. Appears at milestone levels.", "235,87,87"}
        };
        int enemyH = 52 + enemies.length * enemyLineH + pad;
        PageRenderer.drawPanel(g, margin, y, panelW, enemyH);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Enemy Types", margin + pad, y + 34);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(margin + pad, y + 46, panelW - pad * 2, 1);

        int ey = y + 68;
        for (String[] e : enemies) {
            String[] rgb = e[3].split(",");
            Color c = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
            drawEnemyIcon(g, iconCol, ey - 12, 14, e[0], c);
            g.setFont(PageRenderer.BODY_FONT);
            g.setColor(PageRenderer.TEXT);
            g.drawString(e[1], textCol, ey);
            g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString(e[2], textCol + 80, ey);
            ey += enemyLineH;
        }
    }

    private void drawEnemyIcon(Graphics2D g, int x, int y, int s, String shape, Color c) {
        g.setColor(c);
        switch (shape) {
            case "square": g.fillRoundRect(x, y, s, s, 4, 4); break;
            case "diamond":
                g.fillPolygon(new int[]{x+s/2, x+s, x+s/2, x}, new int[]{y, y+s/2, y+s, y+s/2}, 4); break;
            case "circle": g.fillOval(x, y, s, s); break;
            case "triangle":
                g.fillPolygon(new int[]{x+s/2, x+s, x}, new int[]{y, y+s, y+s}, 3); break;
            case "boss":
                g.fillRoundRect(x, y, s, s, 4, 4);
                g.setColor(new Color(255, 120, 120));
                g.drawRoundRect(x, y, s, s, 4, 4); break;
        }
    }

    // ---------- Credits ----------

    private void renderCredits(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Credits");
        PageRenderer.drawBackButton(g, backH);

        int margin = 60;
        int panelW = Game.WIDTH - margin * 2;
        int pad = 20;

        PageRenderer.drawPanel(g, margin, 130, panelW, 320);

        int x = margin + pad;
        int y = 170;
        int spacing = 52;

        drawCreditLine(g, x, y, "Development", "Maurice Boendermaker"); y += spacing;
        drawCreditLine(g, x, y, "Original Visual Graphic Design", "Jeffrey Persoon"); y += spacing;
        drawCreditLine(g, x, y, "Built With", "Java AWT / Swing"); y += spacing;
        drawCreditLine(g, x, y, "Audio", "Virtual Riot  /  MDK  /  Desmeon  /  Pegboard Nerds"); y += spacing;
        drawCreditLine(g, x, y, "Version", "v3.0  \u2014  March 2026"); y += spacing;

        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String footer = "First release: August 2016";
        g.drawString(footer, x, y + 10);
    }

    private void drawCreditLine(Graphics2D g, int x, int y, String role, String name) {
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString(role.toUpperCase(), x, y);
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT);
        g.drawString(name, x, y + 20);
    }

    // ---------- Bottom Links Helper ----------

    private int[] getBottomLinkXs() {
        java.awt.Canvas c = new java.awt.Canvas();
        FontMetrics fm = c.getFontMetrics(PageRenderer.SMALL_FONT);
        String a = "About", d = "  |  ", cl = "Changelog", d2 = "  |  ", cr = "Credits";
        int total = fm.stringWidth(a + d + cl + d2 + cr);
        int sx = (Game.WIDTH - total) / 2;
        int x1 = sx;
        int x2 = x1 + fm.stringWidth(a + d);
        int x3 = x2 + fm.stringWidth(cl + d2);
        int x4 = x3 + fm.stringWidth(cr);
        return new int[]{x1, x2, x3, x4};
    }

    private void drawBottomLinks(Graphics2D g) {
        g.setFont(PageRenderer.SMALL_FONT);
        FontMetrics fm = g.getFontMetrics();
        String a = "About", d = "  |  ", cl = "Changelog", d2 = "  |  ", cr = "Credits";
        int total = fm.stringWidth(a + d + cl + d2 + cr);
        int x = (Game.WIDTH - total) / 2;

        g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.ACCENT, aboutH));
        g.drawString(a, x, 680);
        x += fm.stringWidth(a);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString(d, x, 680);
        x += fm.stringWidth(d);
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.ACCENT, changelogH));
        g.drawString(cl, x, 680);
        x += fm.stringWidth(cl);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString(d2, x, 680);
        x += fm.stringWidth(d2);
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.ACCENT, creditsLinkH));
        g.drawString(cr, x, 680);
    }

    // ---------- Changelog ----------

    private void renderUpdates(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Changelog");
        PageRenderer.drawBackButton(g, backH);

        int y = 130;

        PageRenderer.drawPanel(g, 60, y, Game.WIDTH - 120, 130);
        g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.ACCENT);
        g.drawString("v3.0", 85, y + 32);
        g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("25.03.2026", 140, y + 32);
        g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, Game.WIDTH - 170, 1);
        g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_SEC);
        g.drawString("- Complete visual overhaul with modern dark theme.", 85, y + 68);
        g.drawString("- Fullscreen mode with automatic resolution scaling.", 85, y + 92);
        g.drawString("- Built-in music player with playback controls.", 85, y + 116);

        y += 148;
        PageRenderer.drawPanel(g, 60, y, Game.WIDTH - 120, 130);
        g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.TEXT);
        g.drawString("v2.0", 85, y + 32);
        g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("25.03.2026", 140, y + 32);
        g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, Game.WIDTH - 170, 1);
        g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_SEC);
        g.drawString("- Complete rewrite and overhaul of legacy codebase.", 85, y + 68);
        g.drawString("- Fixed resolution bugs, removed duplicate classes, cleaned up all dead code.", 85, y + 92);
        g.drawString("- No new features introduced \u2014 focused on code quality and stability.", 85, y + 116);

        y += 148;
        PageRenderer.drawPanel(g, 60, y, Game.WIDTH - 120, 108);
        g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("v1.0", 85, y + 32);
        g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("10.11.2019", 140, y + 32);
        g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, Game.WIDTH - 170, 1);
        g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("- Name changed from \"Dodge Game!\" to \"Dotch\".", 85, y + 68);
        g.drawString("- Game layout updated. Added contact page and language support.", 85, y + 92);

        y += 126;
        PageRenderer.drawPanel(g, 60, y, Game.WIDTH - 120, 130);
        g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("v0.1", 85, y + 32);
        g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("09.08.2016", 138, y + 32);
        g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, Game.WIDTH - 170, 1);
        g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("- Initial release with core game mechanics.", 85, y + 68);
        g.drawString("- Player movement, enemy spawning, collision detection, and scoring system.", 85, y + 92);
        g.drawString("- Three difficulty modes, boss fights, HUD, and in-game shop.", 85, y + 116);
    }

    // ---------- End / Game Over ----------

    private void renderEnd(Graphics2D g) {
        PageRenderer.drawBackground(g);

        int cx = Game.WIDTH / 2;

        // Title
        g.setFont(PageRenderer.TITLE_FONT);
        g.setColor(PageRenderer.DANGER);
        String go = "Game Over";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(go, cx - fm.stringWidth(go) / 2, 72);

        // Performance rating + attempt
        String rating = getRating(game.lastScore, game.lastLevel);
        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(PageRenderer.ACCENT);
        fm = g.getFontMetrics();
        String ratingLine = rating + "  -  Attempt #" + Game.currentAttempt;
        g.drawString(ratingLine, cx - fm.stringWidth(ratingLine) / 2, 105);

        // Score panel — centered
        int scoreW = 320;
        int scoreX = cx - scoreW / 2;
        PageRenderer.drawPanel(g, scoreX, 125, scoreW, 110);
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String sl = "FINAL SCORE";
        fm = g.getFontMetrics();
        g.drawString(sl, cx - fm.stringWidth(sl) / 2, 155);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 52));
        g.setColor(game.lastIsHighScore ? new Color(255, 210, 80) : PageRenderer.TEXT);
        String scoreStr = String.valueOf(game.lastScore);
        fm = g.getFontMetrics();
        g.drawString(scoreStr, cx - fm.stringWidth(scoreStr) / 2, 218);

        if (game.lastIsHighScore) {
            g.setFont(PageRenderer.LABEL_FONT);
            g.setColor(new Color(255, 210, 80));
            String hs = "NEW HIGH SCORE";
            fm = g.getFontMetrics();
            g.drawString(hs, cx - fm.stringWidth(hs) / 2, 232);
        }

        // Stats panels — two columns
        int gap = 14;
        int panelW = (Game.WIDTH - 120 - gap) / 2;
        int leftX = 60;
        int rightX = leftX + panelW + gap;
        int statsY = 252;

        // Left — Run Stats
        PageRenderer.drawPanel(g, leftX, statsY, panelW, 196);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Run Stats", leftX + 20, statsY + 32);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(leftX + 20, statsY + 44, panelW - 40, 1);

        int sy = statsY + 70;
        drawStatRow(g, leftX + 20, sy, panelW - 40, "Difficulty", game.lastDifficulty); sy += 26;
        drawStatRow(g, leftX + 20, sy, panelW - 40, "Level reached", String.valueOf(game.lastLevel)); sy += 26;
        drawStatRow(g, leftX + 20, sy, panelW - 40, "Time survived", game.lastTime); sy += 26;
        drawStatRow(g, leftX + 20, sy, panelW - 40, "Enemies on screen", String.valueOf(game.lastEnemies)); sy += 26;
        int bestScore = Stats.getHighScore(game.diff);
        drawStatRow(g, leftX + 20, sy, panelW - 40, "Best score", String.valueOf(bestScore));

        // Right — Upgrades
        PageRenderer.drawPanel(g, rightX, statsY, panelW, 196);
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Upgrades", rightX + 20, statsY + 32);
        g.setColor(PageRenderer.BORDER);
        g.fillRect(rightX + 20, statsY + 44, panelW - 40, 1);

        sy = statsY + 70;
        drawStatRow(g, rightX + 20, sy, panelW - 40, "Health upgrades", String.valueOf(game.lastHealthUps)); sy += 26;
        drawStatRow(g, rightX + 20, sy, panelW - 40, "Speed upgrades", String.valueOf(game.lastSpeedUps)); sy += 26;
        drawStatRow(g, rightX + 20, sy, panelW - 40, "Health refills", String.valueOf(game.lastRefills)); sy += 26;
        drawStatRow(g, rightX + 20, sy, panelW - 40, "Total purchases", String.valueOf(game.lastUpgrades));

        // Buttons
        int btnY = 472;
        PageRenderer.drawPrimaryButton(g, retryX(), btnY, RETRY_W, RETRY_H, "Try Again", retryH);
        PageRenderer.drawBackButton(g, backH);
    }

    private void drawStatRow(Graphics2D g, int x, int y, int w, String label, String value) {
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString(label, x, y);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(PageRenderer.TEXT);
        g.drawString(value, x + w - fm.stringWidth(value), y);
    }

    private String getRating(int score, int level) {
        if (level >= 20) return "\"Legendary\"";
        if (level >= 15) return "\"Untouchable\"";
        if (level >= 10) return "\"Veteran\"";
        if (level >= 7) return "\"Survivor\"";
        if (level >= 5) return "\"Fighter\"";
        if (level >= 3) return "\"Rookie\"";
        return "\"Beginner\"";
    }

    // ---------- Pause Menu ----------

    private void renderPauseMenu(Graphics2D g) {
        // Title
        g.setFont(PageRenderer.TITLE_FONT);
        g.setColor(PageRenderer.ACCENT);
        float pulse = (float) (Math.sin(System.nanoTime() / 400_000_000.0) * 0.3 + 0.7);
        g.setColor(new Color(78, 205, 196, (int) (255 * pulse)));
        String title = "Paused";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (Game.WIDTH - fm.stringWidth(title)) / 2, 250);

        // Buttons
        int px = pauseX();
        PageRenderer.drawPrimaryButton(g, px, PAUSE_Y0, PAUSE_W, PAUSE_H, "Resume", btn[0]);
        PageRenderer.drawSecondaryButton(g, px, PAUSE_Y0 + PAUSE_SP, PAUSE_W, PAUSE_H, "Main Menu", btn[1]);
        PageRenderer.drawSecondaryButton(g, px, PAUSE_Y0 + PAUSE_SP * 2, PAUSE_W, PAUSE_H, "Settings", pauseBtn[3]);
        PageRenderer.drawDangerButton(g, px, PAUSE_Y0 + PAUSE_SP * 3, PAUSE_W, PAUSE_H, "Quit Game", btn[2]);

        // Hint
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String hint = "Press ESC to resume";
        fm = g.getFontMetrics();
        g.drawString(hint, (Game.WIDTH - fm.stringWidth(hint)) / 2, PAUSE_Y0 + PAUSE_SP * 3 + PAUSE_H + 30);
    }
}
