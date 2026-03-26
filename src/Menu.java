import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
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
    private static final int RETRY_Y = 530;

    // Y positions — 5 main menu buttons
    private static final int MENU_SP = 58;
    private static final int PLAY_Y = 260;
    private static final int PROFILE_MENU_Y = PLAY_Y + MENU_SP;
    private static final int SHOP_MENU_Y = PROFILE_MENU_Y + MENU_SP;
    private static final int SETTINGS_MENU_Y = SHOP_MENU_Y + MENU_SP;
    private static final int HELP_MENU_Y = SETTINGS_MENU_Y + MENU_SP;

    // Quit icon (top-right)
    private static final int QUIT_SIZE = 34;
    private static final int QUIT_Y = 24;
    private static int quitIconX() { return Game.WIDTH - QUIT_SIZE - 24; }

    // Music icon (bottom-left)
    private static final int MUSIC_ICON_SIZE = 26;
    private static final int MUSIC_ICON_Y = 632;

    // Legacy aliases — used by Select/Help sub-screens (3-button layouts)
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
    private float[] btn = new float[6];
    private float backH, musicH, shopH, dailyH, aboutH, changelogH, creditsLinkH, quitH, retryH;
    private static final int SHOP_BTN_W = 170;
    private static final int SHOP_BTN_H = 34;
    private static final int SHOP_BTN_Y = 668;
    private static final int DAILY_BTN_W = 140;
    private static final int DAILY_BTN_H = 34;
    private static final int DAILY_BTN_Y = 668;
    private static int dailyBtnX() { return 30 + SHOP_BTN_W + 12; }
    private float[] pauseBtn = new float[4];
    private int mouseX, mouseY;
    private static final float LERP = 0.14f;

    // Help page scroll
    private float helpScroll = 0;
    private float helpScrollTarget = 0;

    // Settings page state

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
        boolean[] btnTargets = new boolean[6];
        boolean backTarget = false, musicTarget = false, shopTarget = false, dailyTarget = false, aboutTarget = false;
        boolean changelogTarget = false, creditsLinkTarget = false, quitTarget = false, retryTarget = false;

        switch (Game.gameState) {
            case Menu:
                int bx = btnX();
                btnTargets[0] = hit(mouseX, mouseY, bx, PLAY_Y, BW, BH);
                btnTargets[1] = hit(mouseX, mouseY, bx, PROFILE_MENU_Y, BW, BH);
                btnTargets[2] = hit(mouseX, mouseY, bx, SHOP_MENU_Y, BW, BH);
                btnTargets[3] = hit(mouseX, mouseY, bx, SETTINGS_MENU_Y, BW, BH);
                btnTargets[4] = hit(mouseX, mouseY, bx, HELP_MENU_Y, BW, BH);
                quitTarget = hit(mouseX, mouseY, quitIconX(), QUIT_Y, QUIT_SIZE, QUIT_SIZE);
                musicTarget = hit(mouseX, mouseY, 24, 20, MUSIC_ICON_SIZE, MUSIC_ICON_SIZE);
                dailyTarget = hit(mouseX, mouseY, Game.WIDTH - 204, 688, 180, 18);
                int[] linkXs = getBottomLinkXs();
                aboutTarget = hit(mouseX, mouseY, linkXs[0], 666, linkXs[1] - linkXs[0], 20);
                changelogTarget = hit(mouseX, mouseY, linkXs[1], 666, linkXs[2] - linkXs[1], 20);
                creditsLinkTarget = hit(mouseX, mouseY, linkXs[2], 666, linkXs[3] - linkXs[2], 20);
                break;
            case Profile:
                bx = btnX();
                btnTargets[0] = hit(mouseX, mouseY, bx, 200, BW, BH);
                btnTargets[1] = hit(mouseX, mouseY, bx, 258, BW, BH);
                btnTargets[2] = hit(mouseX, mouseY, bx, 316, BW, BH);
                backTarget = hitBack();
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
            case Statistics:
            case AchievementsPage:
            case Customize:
            case Loadout:
            case CoinShopPage:
            case DailyPage:
                backTarget = hitBack();
                break;
            default:
                backTarget = hitBack();
                break;
        }

        for (int i = 0; i < 6; i++) btn[i] = approach(btn[i], btnTargets[i]);
        backH = approach(backH, backTarget);
        musicH = approach(musicH, musicTarget);
        shopH = approach(shopH, shopTarget);
        dailyH = approach(dailyH, dailyTarget);
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
        if (Game.gameState == Game.STATE.Settings || Game.gameState == Game.STATE.Statistics
                || Game.gameState == Game.STATE.AchievementsPage
                || Game.gameState == Game.STATE.CoinShopPage
                || Game.gameState == Game.STATE.Update_Notes) {
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
            if (hit(mx, my, bx, PROFILE_MENU_Y, BW, BH)) { Game.gameState = Game.STATE.Profile; resetHover(); return; }
            if (hit(mx, my, bx, SHOP_MENU_Y, BW, BH)) { Game.gameState = Game.STATE.CoinShopPage; resetHover(); return; }
            if (hit(mx, my, bx, SETTINGS_MENU_Y, BW, BH)) { Game.gameState = Game.STATE.Settings; resetHover(); return; }
            if (hit(mx, my, bx, HELP_MENU_Y, BW, BH)) { Game.gameState = Game.STATE.Help; resetHover(); return; }
            if (hit(mx, my, 24, 20, MUSIC_ICON_SIZE, MUSIC_ICON_SIZE)) { Game.gameState = Game.STATE.MusicPlayer; resetHover(); return; }
            if (hit(mx, my, Game.WIDTH - 204, 688, 180, 18)) { Game.gameState = Game.STATE.DailyPage; resetHover(); return; }
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
            if (hit(mx, my, quitIconX(), QUIT_Y, QUIT_SIZE, QUIT_SIZE)) { System.exit(0); }
            return;
        }

        if (Game.gameState == Game.STATE.Profile) {
            int bx = btnX();
            if (hit(mx, my, bx, 200, BW, BH)) { Game.gameState = Game.STATE.Customize; resetHover(); return; }
            if (hit(mx, my, bx, 258, BW, BH)) { Game.gameState = Game.STATE.Statistics; resetHover(); return; }
            if (hit(mx, my, bx, 316, BW, BH)) { Game.gameState = Game.STATE.AchievementsPage; resetHover(); return; }
            if (hitBack()) { Game.gameState = Game.STATE.Menu; resetHover(); return; }
            return;
        }

        if (Game.gameState == Game.STATE.Select) {
            int bx = btnX();
            if (hit(mx, my, bx, PLAY_Y, BW, BH)) { pendingDifficulty = 0; Perks.clearLoadout(); Game.gameState = Game.STATE.Loadout; resetHover(); return; }
            if (hit(mx, my, bx, INFO_Y, BW, BH)) { pendingDifficulty = 1; Perks.clearLoadout(); Game.gameState = Game.STATE.Loadout; resetHover(); return; }
            if (hit(mx, my, bx, HELP_Y, BW, BH)) { pendingDifficulty = 2; Perks.clearLoadout(); Game.gameState = Game.STATE.Loadout; resetHover(); return; }
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

        if (Game.gameState == Game.STATE.Statistics || Game.gameState == Game.STATE.AchievementsPage) {
            if (hitBack()) { Game.gameState = Game.STATE.Profile; resetHover(); }
            return;
        }

        if (Game.gameState == Game.STATE.Customize) {
            handleCustomizeClick(mx, my);
            return;
        }

        if (Game.gameState == Game.STATE.Loadout) {
            handleLoadoutClick(mx, my);
            return;
        }

        if (Game.gameState == Game.STATE.CoinShopPage) {
            handleCoinShopClick(mx, my);
            return;
        }

        if (Game.gameState == Game.STATE.DailyPage) {
            if (hitBack()) { Game.gameState = Game.STATE.Menu; resetHover(); return; }
            // Play button
            int playX = (Game.WIDTH - 260) / 2;
            if (DailyChallenge.canPlay() && hit(mx, my, playX, Game.HEIGHT - 90, 260, 50)) {
                startDailyGame();
                return;
            }
            return;
        }

        if (Game.gameState == Game.STATE.About
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
        for (int i = 0; i < btn.length; i++) btn[i] = 0;
        for (int i = 0; i < 4; i++) pauseBtn[i] = 0;
        backH = musicH = shopH = dailyH = aboutH = changelogH = creditsLinkH = quitH = retryH = 0;
        helpScroll = helpScrollTarget = 0;
        settingsScroll = settingsScrollTarget = 0;
        for (int i = 0; i < SET_HOVER_COUNT; i++) setH[i] = 0;
        setMusicBarH = setSfxBarH = 0;
        settingsDrag = SettingsDrag.NONE;
        confirmAction = null;
        confirmYesH = confirmNoH = 0;
    }

    // Pending game start — set by Select, consumed by Loadout
    private int pendingDifficulty = -1;

    private void startGame(int difficulty, GameObject firstEnemy) {
        handler.getObjects().clear();
        hud.setLevel(1);
        hud.setScore(0);
        hud.setPoints(0);
        hud.bounds = 0;
        hud.resetStats();

        // Apply perk + coin shop bonuses to starting stats
        int healthBonus = Perks.getStartingHealthBonus() + CoinShop.getPermHealthBonus();
        float healthMult = Perks.getMaxHealthMultiplier();
        float baseHealth = (100 + healthBonus) * healthMult;
        HUD.HEALTH = baseHealth;
        hud.bounds = (int) ((baseHealth - 100) * 2);

        handler.spd = 6 + Perks.getStartingSpeedBonus() + CoinShop.getPermSpeedBonus();
        game.shop.reset();
        Game.setTimeScale(1f);
        GamePalette.setDifficulty(difficulty);
        GamePalette.reset();
        Profile.startRun(difficulty);
        Game.currentAttempt = Profile.getCurrentAttempt();
        game.resetRunTracking();
        Game.attemptFade = 1f;
        Game.gameState = Game.STATE.Game;
        handler.addObject(new Player(Game.WIDTH / 2 - 32, Game.HEIGHT / 2 - 32, ID.Player, handler));
        handler.addObject(firstEnemy);
        game.diff = difficulty;
        hud.triggerWaveAnnounce();
    }

    private void startDailyGame() {
        if (!DailyChallenge.canPlay()) return;

        int difficulty = DailyChallenge.todayDifficulty();
        handler.getObjects().clear();
        hud.setLevel(1);
        hud.setScore(0);
        hud.setPoints(0);
        hud.bounds = 0;
        hud.resetStats();

        // Daily runs: no perk/shop bonuses — pure skill, same conditions for everyone
        Perks.clearLoadout();
        HUD.HEALTH = 100;
        hud.bounds = 0;
        handler.spd = 6;
        game.shop.reset();
        Game.setTimeScale(1f);
        GamePalette.setDifficulty(difficulty);
        GamePalette.reset();
        Profile.startRun(difficulty);
        Game.currentAttempt = Profile.getCurrentAttempt();
        game.resetRunTracking();

        // Seed the spawner for deterministic enemy patterns
        Game.dailyMode = true;
        Game.seedSpawner(DailyChallenge.todaySeed());

        Game.attemptFade = 1f;
        Game.gameState = Game.STATE.Game;
        handler.addObject(new Player(Game.WIDTH / 2 - 32, Game.HEIGHT / 2 - 32, ID.Player, handler));

        // First enemy is also seeded (same for everyone)
        java.util.Random seedRng = new java.util.Random(DailyChallenge.todaySeed());
        GameObject firstEnemy;
        if (difficulty == 0) {
            firstEnemy = new BasicEnemy(seedRng.nextInt(Game.WIDTH - 80) + 20,
                    seedRng.nextInt(Game.HEIGHT - 80) + 20, ID.BasicEnemy, handler);
        } else {
            firstEnemy = new HardEnemy(seedRng.nextInt(Game.WIDTH - 80) + 20,
                    seedRng.nextInt(Game.HEIGHT - 80) + 20, ID.BasicEnemy, handler);
        }
        handler.addObject(firstEnemy);
        game.diff = difficulty;
        hud.triggerWaveAnnounce();
    }

    private void resetForMenu() {
        handler.getObjects().clear();
        hud.setLevel(1);
        hud.setScore(0);
        hud.setPoints(0);
        hud.bounds = 0;
        HUD.HEALTH = 100;
        Game.setTimeScale(1f);
        Game.dailyMode = false;
        resetHover();
    }

    // ==================== Rendering ====================

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (Game.gameState) {
            case Menu:          renderMainMenu(g2); break;
            case Profile:       renderProfile(g2); break;
            case Select:        renderSelect(g2); break;
            case Help:          renderHelp(g2); break;
            case HelpENG:       renderHelpPage(g2, "English", new String[][]{
                    {"How to Play", "Navigate your character through the arena while avoiding all enemies. Survive as long as possible to increase your score. Each level introduces new and tougher enemies. Boss battles occur at milestone levels. Earn points over time to spend on upgrades in the shop."},
                    {"Controls", "W / \u2191  -  Move up\nA / \u2190  -  Move left\nS / \u2193  -  Move down\nD / \u2192  -  Move right\n\nShift  -  Dash (short burst with invincibility)\nE  -  Slow Motion (slows everything for 2.5s)\nSpace  -  Open shop\nP / Esc  -  Pause menu\n\nShield activates automatically and absorbs one hit."},
                    {"Enemy Types", "Red squares bounce in straight lines. Teal diamonds move at high speed. Purple circles track and follow the player. Yellow triangles change direction randomly. Red bosses are large and spawn bullets."},
                    {"Tips", "Keep moving \u2014 standing still is the fastest way to lose health. Upgrade speed early for better survivability. Save points for health upgrades at higher levels. Each enemy type has a unique shape and behavior. Learn their patterns to dodge effectively.\n\nBefore each run you can choose up to 2 perks from the Loadout screen. Perks unlock as you level up."}
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
            case Statistics:    renderStatistics(g2); break;
            case AchievementsPage: renderAchievements(g2); break;
            case Customize:     renderCustomize(g2); break;
            case Loadout:       renderLoadout(g2); break;
            case CoinShopPage:  renderCoinShop(g2); break;
            case DailyPage:     renderDailyPage(g2); break;
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
        PageRenderer.drawLogo(g, 180);

        // Level + XP — top center
        int lvlCx = Game.WIDTH / 2;
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.ACCENT);
        String lvlLabel = "LEVEL " + Profile.getLevel();
        FontMetrics lfm = g.getFontMetrics();
        g.drawString(lvlLabel, lvlCx - lfm.stringWidth(lvlLabel) / 2, 34);
        int xpBarW = 120;
        int xpBarH = 3;
        int xpBarX = lvlCx - xpBarW / 2;
        g.setColor(PageRenderer.SURFACE);
        g.fillRoundRect(xpBarX, 40, xpBarW, xpBarH, 2, 2);
        int xpFillW = (int) (xpBarW * Profile.levelProgress());
        if (xpFillW > 0) {
            g.setColor(PageRenderer.ACCENT);
            g.fillRoundRect(xpBarX, 40, xpFillW, xpBarH, 2, 2);
        }

        // 5 centered buttons
        int bx = btnX();
        PageRenderer.drawPrimaryButton(g, bx, PLAY_Y, BW, BH, "Play", btn[0]);
        PageRenderer.drawSecondaryButton(g, bx, PROFILE_MENU_Y, BW, BH, "Profile", btn[1]);
        PageRenderer.drawSecondaryButton(g, bx, SHOP_MENU_Y, BW, BH, "Shop", btn[2]);
        PageRenderer.drawSecondaryButton(g, bx, SETTINGS_MENU_Y, BW, BH, "Settings", btn[3]);
        PageRenderer.drawSecondaryButton(g, bx, HELP_MENU_Y, BW, BH, "Help", btn[4]);

        // Quit icon (top-right) — X button
        int qx = quitIconX();
        g.setColor(PageRenderer.lerp(PageRenderer.SURFACE, new Color(60, 30, 30), quitH));
        g.fillRoundRect(qx, QUIT_Y, QUIT_SIZE, QUIT_SIZE, 8, 8);
        g.setColor(PageRenderer.lerp(PageRenderer.BORDER, PageRenderer.DANGER, quitH * 0.6f));
        g.drawRoundRect(qx, QUIT_Y, QUIT_SIZE, QUIT_SIZE, 8, 8);
        g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.DANGER, quitH));
        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int xc = qx + QUIT_SIZE / 2, yc = QUIT_Y + QUIT_SIZE / 2, xr = 6;
        g.drawLine(xc - xr, yc - xr, xc + xr, yc + xr);
        g.drawLine(xc + xr, yc - xr, xc - xr, yc + xr);
        g.setStroke(oldStroke);

        // Top-left: Now playing + music icon
        if (AudioPlayer.getTrackCount() > 0) {
            // Music icon
            g.setColor(PageRenderer.lerp(PageRenderer.SURFACE, new Color(25, 45, 55), musicH));
            g.fillRoundRect(24, 20, MUSIC_ICON_SIZE, MUSIC_ICON_SIZE, 6, 6);
            g.setColor(PageRenderer.lerp(PageRenderer.BORDER, PageRenderer.ACCENT, musicH * 0.5f));
            g.drawRoundRect(24, 20, MUSIC_ICON_SIZE, MUSIC_ICON_SIZE, 6, 6);
            g.setFont(PageRenderer.SMALL_FONT);
            g.setColor(PageRenderer.lerp(PageRenderer.TEXT_SEC, PageRenderer.ACCENT, musicH));
            PageRenderer.drawCenteredString(g, "\u266B", 24, 20, MUSIC_ICON_SIZE, MUSIC_ICON_SIZE);
            // Now playing text
            g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("Now playing", 24 + MUSIC_ICON_SIZE + 8, 28);
            g.setColor(PageRenderer.TEXT_SEC);
            g.drawString(AudioPlayer.getCurrentTrack().displayName, 24 + MUSIC_ICON_SIZE + 8, 44);
        }

        // Bottom-right: Daily streak
        boolean canPlayDaily = DailyChallenge.canPlay();
        Color dailyCol = canPlayDaily ? new Color(120, 200, 255) : PageRenderer.TEXT_MUTED;
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.lerp(canPlayDaily ? new Color(140, 190, 230) : PageRenderer.TEXT_MUTED,
                dailyCol, dailyH));
        String dailyLabel = canPlayDaily ? "Daily  \u00B7  " + DailyChallenge.getCurrentStreak() + " streak"
                : "Daily  \u00B7  Done";
        FontMetrics dfm = g.getFontMetrics();
        g.drawString(dailyLabel, Game.WIDTH - dfm.stringWidth(dailyLabel) - 24, 700);

        // Version — bottom-left
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("v4.0", 24, 700);

        // About | Changelog | Credits — centered bottom
        drawBottomLinks(g);
    }

    // ---------- Profile ----------

    private void renderProfile(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Profile");
        PageRenderer.drawBackButton(g, backH);

        // Level + XP
        int cx = Game.WIDTH / 2;
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.ACCENT);
        String lvlLabel = "LEVEL " + Profile.getLevel();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(lvlLabel, cx - fm.stringWidth(lvlLabel) / 2, 140);
        int xpBarW = 160;
        int xpBarH = 4;
        int xpBarX = cx - xpBarW / 2;
        g.setColor(PageRenderer.SURFACE);
        g.fillRoundRect(xpBarX, 150, xpBarW, xpBarH, 2, 2);
        int xpFillW = (int) (xpBarW * Profile.levelProgress());
        if (xpFillW > 0) {
            g.setColor(PageRenderer.ACCENT);
            g.fillRoundRect(xpBarX, 150, xpFillW, xpBarH, 2, 2);
        }

        // 3 buttons
        int bx = btnX();
        PageRenderer.drawSecondaryButton(g, bx, 200, BW, BH, "Customize", btn[0]);
        PageRenderer.drawSecondaryButton(g, bx, 258, BW, BH, "Statistics", btn[1]);
        PageRenderer.drawSecondaryButton(g, bx, 316, BW, BH,
                "Achievements  " + Achievements.getUnlockedCount() + "/" + Achievements.getCount(), btn[2]);
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
        setMusicBarH += ((onMusic ? 1f : 0f) - setMusicBarH) * LERP;

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

        // Audio panel: row 0 (music only)
        if (row <= 0) return y + 52 + row * SET_ROW_H + SET_ROW_H / 2;

        // Visual panel: starts after audio
        int audioH = 52 + 1 * SET_ROW_H + SET_PAD;
        y += audioH + gap;
        if (row <= 7) return y + 52 + (row - 2) * SET_ROW_H + SET_ROW_H / 2;

        // Controls panel (general panel removed)
        int visualH = 52 + 6 * SET_ROW_H + SET_PAD;
        y += visualH + gap;
        if (row <= 10) return y + 52 + (row - 10) * SET_ROW_H + SET_ROW_H / 2;

        // Danger panel
        int controlsH = 52 + 7 * 28 + SET_PAD;
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

        // --- Audio panel (music only) ---
        int audioH = 52 + 1 * SET_ROW_H + SET_PAD;
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

        // --- Controls panel (read-only) ---
        int controlsH = 52 + 7 * 28 + SET_PAD;
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
                if ("scores".equals(confirmAction)) Profile.resetHighScores();
                else if ("progress".equals(confirmAction)) { Profile.resetAll(); Settings.resetToDefaults(); Achievements.resetAll(); CoinShop.resetAll(); DailyChallenge.resetAll(); }
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

        // --- Audio: music slider only ---
        int audioH = 52 + 1 * SET_ROW_H + SET_PAD;
        int sliderY0 = y + 52 + SET_ROW_H / 2 - SET_SLIDER_H / 2;

        if (hitSlider(mx, my, sliderY0)) {
            settingsDrag = SettingsDrag.MUSIC_VOL;
            Settings.setMusicVolume(settingsSliderRatio(mx));
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

        // --- Controls (read-only) ---
        int controlsH = 52 + 7 * 28 + SET_PAD;
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
        int audioH = 52 + 1 * SET_ROW_H + SET_PAD;
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

        // ===== CONTROLS PANEL =====
        int controlLineH = 28;
        int controlsH = 52 + 7 * controlLineH + SET_PAD;
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
                {"Shift", "Dash"},
                {"E", "Slow Motion"},
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

    // ---------- Statistics ----------

    private void renderStatistics(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Statistics");
        PageRenderer.drawBackButton(g, backH);

        int contentTop = 130;
        int contentBottom = Game.HEIGHT - 30;
        int margin = 50;
        int pw = Game.WIDTH - margin * 2;
        int gap = 12;
        int pad = 20;
        int rowH = 24;

        java.awt.Shape oldClip = g.getClip();
        g.clipRect(0, contentTop, Game.WIDTH, contentBottom - contentTop);

        int y = contentTop - (int) settingsScroll;
        int lx = margin + pad;
        int vx = margin + pw - pad;

        // ===== OVERVIEW PANEL =====
        int overviewRows = 6;
        int overviewH = 52 + overviewRows * rowH + pad;
        if (y + overviewH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, overviewH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("Overview", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - pad * 2, 1);

            int ry = y + 64;
            statRow(g, lx, vx, ry, "Player Level", String.valueOf(Profile.getLevel())); ry += rowH;
            statRow(g, lx, vx, ry, "Total XP", String.valueOf(Profile.getTotalXp())); ry += rowH;
            statRow(g, lx, vx, ry, "Total Games", String.valueOf(Profile.getTotalGames())); ry += rowH;
            statRow(g, lx, vx, ry, "Total Deaths", String.valueOf(Profile.getTotalDeaths())); ry += rowH;
            statRow(g, lx, vx, ry, "Total Time Played", Profile.getTotalTimePlayed()); ry += rowH;
            statRow(g, lx, vx, ry, "Lifetime Score", String.valueOf(Profile.getTotalScore()));
        }
        y += overviewH + gap;

        // ===== PER-DIFFICULTY PANEL =====
        String[] diffNames = {"Normal", "Hard", "Insane"};
        int diffRows = 3 * 4 + 3; // 3 diffs * 4 rows + 3 headers
        int diffH = 52 + diffRows * rowH + pad;
        if (y + diffH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, diffH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("Per Difficulty", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - pad * 2, 1);

            int ry = y + 64;
            for (int d = 0; d < 3; d++) {
                g.setFont(PageRenderer.LABEL_FONT);
                g.setColor(d == 0 ? PageRenderer.ACCENT : d == 1 ? PageRenderer.WARNING : PageRenderer.DANGER);
                g.drawString(diffNames[d].toUpperCase(), lx, ry);
                ry += rowH;
                statRow(g, lx + 12, vx, ry, "Attempts", String.valueOf(Profile.getAttempts(d))); ry += rowH;
                statRow(g, lx + 12, vx, ry, "Best Score", String.valueOf(Profile.getHighScore(d))); ry += rowH;
                statRow(g, lx + 12, vx, ry, "Best Level", String.valueOf(Profile.getHighLevel(d))); ry += rowH;
                statRow(g, lx + 12, vx, ry, "Best Time", Profile.getBestTime(d)); ry += rowH;
            }
        }
        y += diffH + gap;

        // ===== COMBAT PANEL =====
        int combatRows = 8;
        int combatH = 52 + combatRows * rowH + pad;
        if (y + combatH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, combatH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("Combat", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - pad * 2, 1);

            int ry = y + 64;
            statRow(g, lx, vx, ry, "Total Damage Taken", String.valueOf(Profile.getTotalDamageTaken())); ry += rowH;
            statRow(g, lx, vx, ry, "Longest No-Damage Streak", Profile.getLongestStreak() + "s"); ry += rowH;
            statRow(g, lx, vx, ry, "Avg Survival Time", Profile.getAvgSurvivalSeconds() + "s"); ry += rowH;
            statRow(g, lx, vx, ry, "Bosses Defeated", String.valueOf(Profile.getTotalBossesDefeated())); ry += rowH;
            statRow(g, lx, vx, ry, "Favorite Difficulty", Profile.getDifficultyName(Profile.getFavoriteDifficulty())); ry += rowH;

            g.setFont(PageRenderer.LABEL_FONT);
            g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("ENEMIES ENCOUNTERED", lx, ry);
            ry += rowH;
            statRow(g, lx + 12, vx, ry, "Basic / Fast / Smart / Hard",
                    Profile.getEnemyBasic() + " / " + Profile.getEnemyFast()
                    + " / " + Profile.getEnemySmart() + " / " + Profile.getEnemyHard()); ry += rowH;
            statRow(g, lx + 12, vx, ry, "Bosses", String.valueOf(Profile.getEnemyBoss()));
        }
        y += combatH + gap;

        // ===== UPGRADES PANEL =====
        int upgradeRows = 4;
        int upgradeH = 52 + upgradeRows * rowH + pad;
        if (y + upgradeH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, upgradeH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("Upgrades", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - pad * 2, 1);

            int ry = y + 64;
            statRow(g, lx, vx, ry, "Total Purchased", String.valueOf(Profile.getTotalUpgrades())); ry += rowH;
            statRow(g, lx, vx, ry, "Health Upgrades", String.valueOf(Profile.getTotalHealthUps())); ry += rowH;
            statRow(g, lx, vx, ry, "Speed Upgrades", String.valueOf(Profile.getTotalSpeedUps())); ry += rowH;
            statRow(g, lx, vx, ry, "Health Refills", String.valueOf(Profile.getTotalRefills()));
        }
        y += upgradeH + gap;

        // ===== RECENT RUNS PANEL =====
        int histCount = Profile.getHistoryCount();
        int recentRows = Math.max(histCount, 1) + 1; // +1 for header row
        int recentH = 52 + recentRows * rowH + pad;
        if (y + recentH > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, recentH);
            g.setFont(PageRenderer.HEADING_FONT);
            g.setColor(PageRenderer.ACCENT);
            g.drawString("Recent Runs", lx, y + 34);
            g.setColor(PageRenderer.BORDER);
            g.fillRect(lx, y + 46, pw - pad * 2, 1);

            int ry = y + 64;

            if (histCount == 0) {
                g.setFont(PageRenderer.BODY_FONT);
                g.setColor(PageRenderer.TEXT_MUTED);
                g.drawString("No runs yet", lx, ry);
            } else {
                // Header
                g.setFont(PageRenderer.LABEL_FONT);
                g.setColor(PageRenderer.TEXT_MUTED);
                g.drawString("#", lx, ry);
                g.drawString("SCORE", lx + 40, ry);
                g.drawString("LEVEL", lx + 160, ry);
                g.drawString("DIFFICULTY", lx + 240, ry);
                ry += rowH;

                // Score chart — bar graph
                int maxScore = 1;
                for (int i = 0; i < histCount; i++) {
                    int s = Profile.getRecentScore(i);
                    if (s > maxScore) maxScore = s;
                }

                int chartX = lx + 340;
                int chartW = pw - pad * 2 - 340;

                for (int i = 0; i < histCount; i++) {
                    int score = Profile.getRecentScore(i);
                    int lvl = Profile.getRecentLevel(i);
                    int diff = Profile.getRecentDifficulty(i);

                    g.setFont(PageRenderer.SMALL_FONT);
                    g.setColor(PageRenderer.TEXT_MUTED);
                    g.drawString(String.valueOf(i + 1), lx, ry);

                    g.setColor(PageRenderer.TEXT);
                    g.drawString(String.valueOf(score), lx + 40, ry);
                    g.drawString(String.valueOf(lvl), lx + 160, ry);

                    Color diffCol = diff == 0 ? PageRenderer.ACCENT : diff == 1 ? PageRenderer.WARNING : PageRenderer.DANGER;
                    g.setColor(diffCol);
                    g.drawString(Profile.getDifficultyName(diff), lx + 240, ry);

                    // Mini bar
                    if (chartW > 20) {
                        int barW = (int) ((float) score / maxScore * chartW);
                        g.setColor(new Color(diffCol.getRed(), diffCol.getGreen(), diffCol.getBlue(), 60));
                        g.fillRoundRect(chartX, ry - 10, barW, 12, 3, 3);
                    }

                    ry += rowH;
                }
            }
        }
        y += recentH + gap;

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
    }

    private void statRow(Graphics2D g, int lx, int vx, int y, String label, String value) {
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        g.drawString(label, lx, y);
        g.setColor(PageRenderer.TEXT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(value, vx - fm.stringWidth(value), y);
    }

    // ---------- Daily Challenge ----------

    private float dailyPlayH = 0;

    private void renderDailyPage(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Daily Challenge");
        PageRenderer.drawBackButton(g, backH);

        int cx = Game.WIDTH / 2;

        // Today's info
        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        String dayInfo = DailyChallenge.todayLabel() + "  \u2014  " + DailyChallenge.todayDifficultyName();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(dayInfo, cx - fm.stringWidth(dayInfo) / 2, 120);

        // Streak + stats row
        int statsY = 145;
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String statsLine = "Streak: " + DailyChallenge.getCurrentStreak()
                + "   Best streak: " + DailyChallenge.getBestStreak()
                + "   Completed: " + DailyChallenge.getTotalCompleted()
                + "   Best score: " + DailyChallenge.getBestDailyScore();
        fm = g.getFontMetrics();
        g.drawString(statsLine, cx - fm.stringWidth(statsLine) / 2, statsY);

        // 28-day calendar grid (4 rows of 7)
        int cellW = 110;
        int cellH = 72;
        int cellGap = 8;
        int gridW = 7 * cellW + 6 * cellGap;
        int gridX = (Game.WIDTH - gridW) / 2;
        int gridY = 170;

        // Day-of-week headers
        String[] headers = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        for (int d = 0; d < 7; d++) {
            int hx = gridX + d * (cellW + cellGap) + cellW / 2;
            fm = g.getFontMetrics();
            g.drawString(headers[d], hx - fm.stringWidth(headers[d]) / 2, gridY);
        }
        gridY += 16;

        int today = DailyChallenge.dayInCycle();

        for (int i = 0; i < 28; i++) {
            int col = i % 7;
            int row = i / 7;
            int cellX = gridX + col * (cellW + cellGap);
            int cellY = gridY + row * (cellH + cellGap);

            boolean isToday = (i == today);
            boolean completed = DailyChallenge.isCompleted(i);
            int score = DailyChallenge.getScore(i);
            int diff = DailyChallenge.getDifficultyForDay(i);
            Color diffCol = diff == 0 ? PageRenderer.ACCENT : diff == 1 ? PageRenderer.WARNING : PageRenderer.DANGER;

            // Cell background
            Color bg = isToday ? new Color(25, 40, 55) : PageRenderer.SURFACE;
            g.setColor(bg);
            g.fillRoundRect(cellX, cellY, cellW, cellH, 6, 6);

            // Border — highlight today
            if (isToday) {
                g.setColor(new Color(120, 200, 255));
                g.drawRoundRect(cellX, cellY, cellW, cellH, 6, 6);
            } else {
                g.setColor(completed ? new Color(72, 199, 142, 60) : PageRenderer.BORDER);
                g.drawRoundRect(cellX, cellY, cellW, cellH, 6, 6);
            }

            // Week label on first column
            if (col == 0) {
                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(PageRenderer.TEXT_MUTED);
                g.drawString("W" + (row + 1), cellX + 4, cellY + 14);
            }

            // Difficulty dot
            g.setColor(diffCol);
            g.fillOval(cellX + cellW - 14, cellY + 4, 8, 8);

            // Score or status
            if (completed) {
                g.setFont(PageRenderer.BUTTON_FONT);
                g.setColor(PageRenderer.TEXT);
                String scoreStr = String.valueOf(score);
                fm = g.getFontMetrics();
                g.drawString(scoreStr, cellX + (cellW - fm.stringWidth(scoreStr)) / 2, cellY + 38);

                // Checkmark
                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(new Color(72, 199, 142));
                g.drawString("\u2713", cellX + cellW / 2 - 4, cellY + 56);
            } else if (isToday) {
                g.setFont(PageRenderer.SMALL_FONT);
                boolean canPlay = DailyChallenge.canPlay();
                g.setColor(canPlay ? new Color(120, 200, 255) : PageRenderer.TEXT_MUTED);
                String status = canPlay ? "PLAY" : "DONE";
                fm = g.getFontMetrics();
                g.drawString(status, cellX + (cellW - fm.stringWidth(status)) / 2, cellY + 42);
            } else {
                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(new Color(40, 50, 65));
                String dash = "\u2014";
                fm = g.getFontMetrics();
                g.drawString(dash, cellX + (cellW - fm.stringWidth(dash)) / 2, cellY + 42);
            }
        }

        // Play button
        int playX = (Game.WIDTH - 260) / 2;
        int playY = Game.HEIGHT - 90;
        boolean canPlay = DailyChallenge.canPlay();
        boolean playHovered = hit(mouseX, mouseY, playX, playY, 260, 50);
        dailyPlayH += ((playHovered ? 1f : 0f) - dailyPlayH) * 0.14f;

        if (canPlay) {
            PageRenderer.drawPrimaryButton(g, playX, playY, 260, 50,
                    "Play Today's Challenge", dailyPlayH);
        } else {
            g.setColor(PageRenderer.SURFACE);
            g.fillRoundRect(playX, playY, 260, 50, PageRenderer.R, PageRenderer.R);
            g.setColor(PageRenderer.BORDER);
            g.drawRoundRect(playX, playY, 260, 50, PageRenderer.R, PageRenderer.R);
            g.setFont(PageRenderer.BUTTON_FONT);
            g.setColor(PageRenderer.TEXT_MUTED);
            PageRenderer.drawCenteredString(g, "Completed Today", playX, playY, 260, 50);
        }
    }

    // ---------- Coin Shop ----------

    private static final int SHOP_CARD_W = 280;
    private static final int SHOP_CARD_H = 80;
    private static final int SHOP_COLS = 3;
    private static final int SHOP_GAP = 14;
    private static final Color COIN_GOLD = new Color(255, 210, 80);

    private void handleCoinShopClick(int mx, int my) {
        if (hitBack()) { Game.gameState = Game.STATE.Menu; resetHover(); return; }

        int contentTop = 150;
        int contentBottom = Game.HEIGHT - 30;
        if (my < contentTop || my > contentBottom) return;

        int totalW = SHOP_CARD_W * SHOP_COLS + SHOP_GAP * (SHOP_COLS - 1);
        int gridX = (Game.WIDTH - totalW) / 2;
        int gridY = contentTop - (int) settingsScroll;

        int lastCat = -1;
        int row = 0, col = 0;
        for (int i = 0; i < CoinShop.ITEM_COUNT; i++) {
            CoinShop.Item item = CoinShop.getItem(i);
            if (item.category != lastCat) {
                if (col > 0) { row++; col = 0; }
                gridY += (lastCat >= 0 ? 10 : 0) + 36;
                lastCat = item.category;
            }
            int cx = gridX + col * (SHOP_CARD_W + SHOP_GAP);
            int cy = gridY + row * (SHOP_CARD_H + SHOP_GAP);

            if (hit(mx, my, cx, cy, SHOP_CARD_W, SHOP_CARD_H) && !item.purchased && CoinShop.canAfford(i)) {
                CoinShop.purchase(i);
                return;
            }

            col++;
            if (col >= SHOP_COLS) { col = 0; row++; }
        }
    }

    private void renderCoinShop(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Shop");
        PageRenderer.drawBackButton(g, backH);

        // Coin balance
        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(COIN_GOLD);
        String bal = Profile.getCoins() + " coins";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(bal, (Game.WIDTH - fm.stringWidth(bal)) / 2, 125);

        int contentTop = 150;
        int contentBottom = Game.HEIGHT - 30;
        int totalW = SHOP_CARD_W * SHOP_COLS + SHOP_GAP * (SHOP_COLS - 1);
        int gridX = (Game.WIDTH - totalW) / 2;

        java.awt.Shape oldClip = g.getClip();
        g.clipRect(0, contentTop, Game.WIDTH, contentBottom - contentTop);

        int gridY = contentTop - (int) settingsScroll;
        int lastCat = -1;
        int row = 0, col = 0;

        for (int i = 0; i < CoinShop.ITEM_COUNT; i++) {
            CoinShop.Item item = CoinShop.getItem(i);

            // Category header
            if (item.category != lastCat) {
                if (col > 0) { row++; col = 0; }
                int headerY = gridY + row * (SHOP_CARD_H + SHOP_GAP) + (lastCat >= 0 ? 10 : 0);
                gridY += (lastCat >= 0 ? 10 : 0) + 36;
                lastCat = item.category;

                if (headerY > contentTop - 40 && headerY < contentBottom) {
                    g.setFont(PageRenderer.HEADING_FONT);
                    g.setColor(PageRenderer.ACCENT);
                    g.drawString(CoinShop.CAT_NAMES[item.category], gridX, headerY + 24);
                    g.setColor(PageRenderer.BORDER);
                    g.fillRect(gridX, headerY + 30, totalW, 1);
                }
            }

            int cx = gridX + col * (SHOP_CARD_W + SHOP_GAP);
            int cy = gridY + row * (SHOP_CARD_H + SHOP_GAP);

            if (cy + SHOP_CARD_H > contentTop - 10 && cy < contentBottom + 10) {
                renderShopCard(g, cx, cy, item);
            }

            col++;
            if (col >= SHOP_COLS) { col = 0; row++; }
        }

        int totalHeight = gridY + (row + 1) * (SHOP_CARD_H + SHOP_GAP) + (int) settingsScroll - contentTop;
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
    }

    private void renderShopCard(Graphics2D g, int x, int y, CoinShop.Item item) {
        boolean purchased = item.purchased;
        boolean canAfford = !purchased && Profile.getCoins() >= item.cost;
        boolean hovered = hit(mouseX, mouseY, x, y, SHOP_CARD_W, SHOP_CARD_H);

        // Background
        Color bg = purchased ? new Color(22, 32, 28) :
                (hovered && canAfford) ? new Color(30, 40, 56) : PageRenderer.SURFACE;
        g.setColor(bg);
        g.fillRoundRect(x, y, SHOP_CARD_W, SHOP_CARD_H, 8, 8);

        // Border
        Color border = purchased ? new Color(72, 199, 142, 80) :
                (hovered && canAfford) ? COIN_GOLD : PageRenderer.BORDER;
        g.setColor(border);
        g.drawRoundRect(x, y, SHOP_CARD_W, SHOP_CARD_H, 8, 8);

        // Name
        g.setFont(PageRenderer.BUTTON_FONT);
        g.setColor(purchased ? PageRenderer.TEXT_MUTED : PageRenderer.TEXT);
        g.drawString(item.name, x + 12, y + 24);

        // Description
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(purchased ? new Color(60, 70, 80) : PageRenderer.TEXT_SEC);
        g.drawString(item.description, x + 12, y + 44);

        if (purchased) {
            g.setFont(PageRenderer.LABEL_FONT);
            g.setColor(new Color(72, 199, 142));
            g.drawString("OWNED", x + SHOP_CARD_W - 60, y + 24);
        } else {
            // Price
            g.setFont(PageRenderer.LABEL_FONT);
            g.setColor(canAfford ? COIN_GOLD : PageRenderer.TEXT_MUTED);
            String price = item.cost + " coins";
            g.drawString(price, x + 12, y + SHOP_CARD_H - 12);

            // Buy button
            if (canAfford && hovered) {
                g.setColor(COIN_GOLD);
                g.fillRoundRect(x + SHOP_CARD_W - 55, y + SHOP_CARD_H - 28, 44, 20, 4, 4);
                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(PageRenderer.BG_DARK);
                g.drawString("Buy", x + SHOP_CARD_W - 45, y + SHOP_CARD_H - 13);
            }
        }
    }

    // ---------- Loadout ----------

    private static final int PERK_CARD_W = 240;
    private static final int PERK_CARD_H = 110;
    private static final int PERK_GAP = 16;
    private static final int PERK_COLS = 4;
    private static final int START_BTN_W = 280;
    private static final int START_BTN_H = 50;
    private float loadoutStartH = 0;

    private static int perkGridX() { return (Game.WIDTH - (PERK_CARD_W * PERK_COLS + PERK_GAP * (PERK_COLS - 1))) / 2; }

    private void handleLoadoutClick(int mx, int my) {
        // Back to select
        if (hitBack()) { Game.gameState = Game.STATE.Select; resetHover(); return; }

        // Start button
        int startX = (Game.WIDTH - START_BTN_W) / 2;
        int startY = Game.HEIGHT - 90;
        if (hit(mx, my, startX, startY, START_BTN_W, START_BTN_H)) {
            launchPendingGame();
            return;
        }

        // Perk cards
        int gridX = perkGridX();
        int gridY = 200;
        for (int i = 0; i < Perks.PERK_COUNT; i++) {
            int col = i % PERK_COLS;
            int row = i / PERK_COLS;
            int cx = gridX + col * (PERK_CARD_W + PERK_GAP);
            int cy = gridY + row * (PERK_CARD_H + PERK_GAP);
            if (hit(mx, my, cx, cy, PERK_CARD_W, PERK_CARD_H)) {
                Perks.toggleEquip(i);
                return;
            }
        }
    }

    private void launchPendingGame() {
        if (pendingDifficulty < 0) return;
        GameObject firstEnemy;
        if (pendingDifficulty == 0) {
            firstEnemy = new BasicEnemy(Game.WIDTH - 50, Game.HEIGHT - 50, ID.BasicEnemy, handler);
        } else {
            firstEnemy = new HardEnemy(Game.WIDTH - 100, Game.HEIGHT - 100, ID.BasicEnemy, handler);
        }
        startGame(pendingDifficulty, firstEnemy);
    }

    private void renderLoadout(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Loadout");
        PageRenderer.drawBackButton(g, backH);

        // Difficulty label
        String diffName = pendingDifficulty == 0 ? "Normal" : pendingDifficulty == 1 ? "Hard" : "Insane";
        Color diffCol = pendingDifficulty == 0 ? PageRenderer.ACCENT : pendingDifficulty == 1 ? PageRenderer.WARNING : PageRenderer.DANGER;
        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(diffCol);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(diffName, (Game.WIDTH - fm.stringWidth(diffName)) / 2, 122);

        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String hint = "Select up to 2 perks  (" + Perks.getEquippedCount() + "/2)";
        fm = g.getFontMetrics();
        g.drawString(hint, (Game.WIDTH - fm.stringWidth(hint)) / 2, 155);

        // Perk grid
        int gridX = perkGridX();
        int gridY = 180;

        for (int i = 0; i < Perks.PERK_COUNT; i++) {
            int col = i % PERK_COLS;
            int row = i / PERK_COLS;
            int cx = gridX + col * (PERK_CARD_W + PERK_GAP);
            int cy = gridY + row * (PERK_CARD_H + PERK_GAP);

            boolean unlocked = Perks.isUnlocked(i);
            boolean equipped = Perks.isEquipped(i);
            boolean hovered = hit(mouseX, mouseY, cx, cy, PERK_CARD_W, PERK_CARD_H);

            renderPerkCard(g, cx, cy, i, unlocked, equipped, hovered);
        }

        // Equipped summary
        int sumY = gridY + ((Perks.PERK_COUNT + PERK_COLS - 1) / PERK_COLS) * (PERK_CARD_H + PERK_GAP) + 10;
        renderEquippedSummary(g, sumY);

        // Start button
        int startX = (Game.WIDTH - START_BTN_W) / 2;
        int startY = Game.HEIGHT - 90;
        boolean startHovered = hit(mouseX, mouseY, startX, startY, START_BTN_W, START_BTN_H);
        loadoutStartH += ((startHovered ? 1f : 0f) - loadoutStartH) * 0.14f;
        PageRenderer.drawPrimaryButton(g, startX, startY, START_BTN_W, START_BTN_H, "Start Run", loadoutStartH);
    }

    private void renderPerkCard(Graphics2D g, int x, int y, int perkId,
                                 boolean unlocked, boolean equipped, boolean hovered) {
        // Card background
        Color bg;
        if (equipped) bg = new Color(25, 45, 55);
        else if (hovered && unlocked) bg = new Color(30, 40, 56);
        else bg = PageRenderer.SURFACE;
        g.setColor(bg);
        g.fillRoundRect(x, y, PERK_CARD_W, PERK_CARD_H, 10, 10);

        // Border
        Color border;
        if (equipped) border = PageRenderer.ACCENT;
        else if (hovered && unlocked) border = new Color(60, 80, 100);
        else border = PageRenderer.BORDER;
        g.setColor(border);
        g.drawRoundRect(x, y, PERK_CARD_W, PERK_CARD_H, 10, 10);

        // Accent top strip
        if (equipped) {
            g.setColor(PageRenderer.ACCENT);
            g.fillRoundRect(x, y, PERK_CARD_W, 3, 3, 3);
        }

        if (unlocked) {
            // Icon
            g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 22));
            g.setColor(equipped ? PageRenderer.ACCENT : PageRenderer.TEXT_SEC);
            g.drawString(Perks.ICONS[perkId], x + 14, y + 30);

            // Name
            g.setFont(PageRenderer.BUTTON_FONT);
            g.setColor(equipped ? PageRenderer.TEXT : PageRenderer.TEXT_SEC);
            g.drawString(Perks.NAMES[perkId], x + 44, y + 28);

            // Description
            g.setFont(PageRenderer.SMALL_FONT);
            g.setColor(equipped ? PageRenderer.TEXT_SEC : PageRenderer.TEXT_MUTED);
            // Word-wrap description into card width
            drawCardDesc(g, Perks.DESCRIPTIONS[perkId], x + 14, y + 48, PERK_CARD_W - 28);

            // Equipped badge
            if (equipped) {
                g.setFont(PageRenderer.LABEL_FONT);
                g.setColor(PageRenderer.ACCENT);
                g.drawString("EQUIPPED", x + PERK_CARD_W - 72, y + PERK_CARD_H - 10);
            }
        } else {
            // Locked state
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
            g.setColor(new Color(50, 58, 72));
            g.drawString("\u25CB", x + 14, y + 30);

            g.setFont(PageRenderer.BUTTON_FONT);
            g.setColor(new Color(60, 70, 85));
            g.drawString(Perks.NAMES[perkId], x + 44, y + 28);

            g.setFont(PageRenderer.SMALL_FONT);
            g.setColor(new Color(50, 60, 75));
            g.drawString(Perks.getUnlockDesc(perkId), x + 14, y + 50);
        }
    }

    private void drawCardDesc(Graphics2D g, String text, int x, int y, int maxW) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int cy = y;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(test) > maxW && line.length() > 0) {
                g.drawString(line.toString(), x, cy);
                cy += 16;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) g.drawString(line.toString(), x, cy);
    }

    private void renderEquippedSummary(Graphics2D g, int y) {
        int cx = Game.WIDTH / 2;
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);

        int slot1 = Perks.getEquipped(0);
        int slot2 = Perks.getEquipped(1);

        if (slot1 < 0 && slot2 < 0) {
            String msg = "No perks selected \u2014 click a perk to equip it";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, cx - fm.stringWidth(msg) / 2, y);
        } else {
            StringBuilder sb = new StringBuilder("Active: ");
            if (slot1 >= 0) sb.append(Perks.NAMES[slot1]);
            if (slot1 >= 0 && slot2 >= 0) sb.append("  +  ");
            if (slot2 >= 0) sb.append(Perks.NAMES[slot2]);
            g.setColor(PageRenderer.ACCENT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(sb.toString(), cx - fm.stringWidth(sb.toString()) / 2, y);
        }
    }

    // ---------- Customize ----------

    // Layout
    private static final int SKIN_CELL = 70;
    private static final int SKIN_GAP = 12;
    private static final int SKIN_COLS = 6;

    private void handleCustomizeClick(int mx, int my) {
        if (hitBack()) { Game.gameState = Game.STATE.Profile; resetHover(); return; }

        int margin = 60;
        int pad = 20;

        // Shape grid — y starts at 190
        int gridY = 190;
        int gridX = margin + pad;
        for (int i = 0; i < PlayerSkins.SHAPE_COUNT; i++) {
            int col = i % SKIN_COLS;
            int row = i / SKIN_COLS;
            int cx = gridX + col * (SKIN_CELL + SKIN_GAP);
            int cy = gridY + row * (SKIN_CELL + SKIN_GAP);
            if (hit(mx, my, cx, cy, SKIN_CELL, SKIN_CELL)) {
                if (PlayerSkins.isShapeUnlocked(i)) {
                    PlayerSkins.setSelectedShape(i);
                }
                return;
            }
        }

        // Color grid — y starts at 390
        int colorY = 390;
        for (int i = 0; i < PlayerSkins.COLOR_COUNT; i++) {
            int col = i % SKIN_COLS;
            int row = i / SKIN_COLS;
            int cx = gridX + col * (SKIN_CELL + SKIN_GAP);
            int cy = colorY + row * (SKIN_CELL + SKIN_GAP);
            if (hit(mx, my, cx, cy, SKIN_CELL, SKIN_CELL)) {
                if (PlayerSkins.isColorUnlocked(i)) {
                    PlayerSkins.setSelectedColor(i);
                }
                return;
            }
        }
    }

    private void renderCustomize(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Customize");
        PageRenderer.drawBackButton(g, backH);

        int margin = 60;
        int pad = 20;
        int pw = Game.WIDTH - margin * 2;

        // ===== SHAPE SELECTOR =====
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Shape", margin + pad, 170);

        int gridX = margin + pad;
        int gridY = 190;

        for (int i = 0; i < PlayerSkins.SHAPE_COUNT; i++) {
            int col = i % SKIN_COLS;
            int row = i / SKIN_COLS;
            int cx = gridX + col * (SKIN_CELL + SKIN_GAP);
            int cy = gridY + row * (SKIN_CELL + SKIN_GAP);

            boolean unlocked = PlayerSkins.isShapeUnlocked(i);
            boolean selected = (i == PlayerSkins.getSelectedShape());
            boolean hovered = hit(mouseX, mouseY, cx, cy, SKIN_CELL, SKIN_CELL);

            // Cell background
            Color bg = selected ? new Color(30, 50, 60) : new Color(22, 30, 44);
            if (hovered && unlocked) bg = new Color(35, 48, 64);
            g.setColor(bg);
            g.fillRoundRect(cx, cy, SKIN_CELL, SKIN_CELL, 8, 8);

            // Border
            Color border = selected ? PageRenderer.ACCENT : hovered ? new Color(60, 75, 95) : PageRenderer.BORDER;
            g.setColor(border);
            g.drawRoundRect(cx, cy, SKIN_CELL, SKIN_CELL, 8, 8);

            if (unlocked) {
                // Draw the shape preview
                int previewSize = 30;
                int px = cx + (SKIN_CELL - previewSize) / 2;
                int py = cy + 6;
                Color previewCol = selected ? PageRenderer.ACCENT : PageRenderer.TEXT_SEC;
                PlayerSkins.drawShape(g, i, px, py, previewSize, 6, previewCol);

                // Name
                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(selected ? PageRenderer.ACCENT : PageRenderer.TEXT_MUTED);
                FontMetrics fm = g.getFontMetrics();
                String name = PlayerSkins.SHAPE_NAMES[i];
                g.drawString(name, cx + (SKIN_CELL - fm.stringWidth(name)) / 2, cy + SKIN_CELL - 8);
            } else {
                // Locked
                g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
                g.setColor(new Color(50, 58, 72));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("\u25CB", cx + (SKIN_CELL - fm.stringWidth("\u25CB")) / 2, cy + 34);

                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(new Color(55, 65, 80));
                fm = g.getFontMetrics();
                String lock = "Locked";
                g.drawString(lock, cx + (SKIN_CELL - fm.stringWidth(lock)) / 2, cy + SKIN_CELL - 8);

                // Tooltip on hover
                if (hovered) {
                    String desc = PlayerSkins.getShapeUnlockDesc(i);
                    g.setFont(PageRenderer.SMALL_FONT);
                    g.setColor(PageRenderer.TEXT_MUTED);
                    fm = g.getFontMetrics();
                    g.drawString(desc, cx, cy + SKIN_CELL + 14);
                }
            }
        }

        // ===== COLOR PALETTE SELECTOR =====
        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("Color", margin + pad, 370);

        int colorY = 390;

        for (int i = 0; i < PlayerSkins.COLOR_COUNT; i++) {
            int col = i % SKIN_COLS;
            int row = i / SKIN_COLS;
            int cx = gridX + col * (SKIN_CELL + SKIN_GAP);
            int cy = colorY + row * (SKIN_CELL + SKIN_GAP);

            boolean unlocked = PlayerSkins.isColorUnlocked(i);
            boolean selected = (i == PlayerSkins.getSelectedColor());
            boolean hovered = hit(mouseX, mouseY, cx, cy, SKIN_CELL, SKIN_CELL);

            // Cell background
            Color bg = selected ? new Color(30, 50, 60) : new Color(22, 30, 44);
            if (hovered && unlocked) bg = new Color(35, 48, 64);
            g.setColor(bg);
            g.fillRoundRect(cx, cy, SKIN_CELL, SKIN_CELL, 8, 8);

            // Border
            Color border = selected ? PageRenderer.ACCENT : hovered ? new Color(60, 75, 95) : PageRenderer.BORDER;
            g.setColor(border);
            g.drawRoundRect(cx, cy, SKIN_CELL, SKIN_CELL, 8, 8);

            if (unlocked) {
                // Color swatch
                Color swatch = PlayerSkins.COLOR_FILLS[i];
                int swatchSize = 24;
                int sx = cx + (SKIN_CELL - swatchSize) / 2;
                int sy = cy + 10;
                g.setColor(swatch);
                g.fillRoundRect(sx, sy, swatchSize, swatchSize, 6, 6);
                if (selected) {
                    g.setColor(new Color(255, 255, 255, 60));
                    g.drawRoundRect(sx, sy, swatchSize, swatchSize, 6, 6);
                }

                // Name
                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(selected ? swatch : PageRenderer.TEXT_MUTED);
                FontMetrics fm = g.getFontMetrics();
                String name = PlayerSkins.COLOR_NAMES[i];
                g.drawString(name, cx + (SKIN_CELL - fm.stringWidth(name)) / 2, cy + SKIN_CELL - 8);
            } else {
                // Locked
                g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
                g.setColor(new Color(50, 58, 72));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("\u25CB", cx + (SKIN_CELL - fm.stringWidth("\u25CB")) / 2, cy + 34);

                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(new Color(55, 65, 80));
                fm = g.getFontMetrics();
                String lock = "Locked";
                g.drawString(lock, cx + (SKIN_CELL - fm.stringWidth(lock)) / 2, cy + SKIN_CELL - 8);

                if (hovered) {
                    String desc = PlayerSkins.getColorUnlockDesc(i);
                    g.setFont(PageRenderer.SMALL_FONT);
                    g.setColor(PageRenderer.TEXT_MUTED);
                    fm = g.getFontMetrics();
                    g.drawString(desc, cx, cy + SKIN_CELL + 14);
                }
            }
        }

        // ===== LIVE PREVIEW =====
        int previewX = Game.WIDTH - margin - 200;
        int previewY = 170;
        PageRenderer.drawPanel(g, previewX, previewY, 180, 220);

        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        g.drawString("PREVIEW", previewX + 60, previewY + 22);

        // Draw player preview with selected skin
        int pSize = 64;
        int ppx = previewX + (180 - pSize) / 2;
        int ppy = previewY + 50;

        // Glow
        Color skinFill = PlayerSkins.getSelectedFill();
        g.setColor(new Color(skinFill.getRed(), skinFill.getGreen(), skinFill.getBlue(), 30));
        PlayerSkins.drawShape(g, PlayerSkins.getSelectedShape(),
                ppx - 6, ppy - 6, pSize + 12, 14, g.getColor());

        // Main shape
        PlayerSkins.drawShape(g, PlayerSkins.getSelectedShape(), ppx, ppy, pSize, 10, skinFill);

        // Labels
        g.setFont(PageRenderer.BODY_FONT);
        g.setColor(PageRenderer.TEXT);
        FontMetrics fm = g.getFontMetrics();
        String shapeName = PlayerSkins.SHAPE_NAMES[PlayerSkins.getSelectedShape()];
        g.drawString(shapeName, previewX + (180 - fm.stringWidth(shapeName)) / 2, ppy + pSize + 28);

        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(skinFill);
        fm = g.getFontMetrics();
        String colorName = PlayerSkins.COLOR_NAMES[PlayerSkins.getSelectedColor()];
        g.drawString(colorName, previewX + (180 - fm.stringWidth(colorName)) / 2, ppy + pSize + 48);

        // Trail shape label
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String trailLabel = "Trail: " + getTrailName(PlayerSkins.getSelectedTrailShape());
        fm = g.getFontMetrics();
        g.drawString(trailLabel, previewX + (180 - fm.stringWidth(trailLabel)) / 2, ppy + pSize + 68);
    }

    private String getTrailName(int trailShape) {
        switch (trailShape) {
            case Trail.SHAPE_CIRCLE: return "Circle";
            case Trail.SHAPE_DIAMOND: return "Diamond";
            case Trail.SHAPE_TRIANGLE: return "Triangle";
            default: return "Square";
        }
    }

    // ---------- Achievements ----------

    private static final Color ACH_GOLD = new Color(255, 210, 80);
    private static final Color ACH_LOCKED = new Color(40, 48, 62);

    private void renderAchievements(Graphics2D g) {
        PageRenderer.drawBackground(g);
        PageRenderer.drawTitle(g, "Achievements");
        PageRenderer.drawBackButton(g, backH);

        // Summary line
        g.setFont(PageRenderer.SUBTITLE_FONT);
        g.setColor(PageRenderer.TEXT_SEC);
        String summary = Achievements.getUnlockedCount() + " / " + Achievements.getCount() + " unlocked";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(summary, (Game.WIDTH - fm.stringWidth(summary)) / 2, 120);

        int contentTop = 140;
        int contentBottom = Game.HEIGHT - 30;
        int margin = 40;
        int pw = Game.WIDTH - margin * 2;
        int pad = 16;
        int gap = 10;
        int cardH = 54;
        int cardGap = 6;

        java.awt.Shape oldClip = g.getClip();
        g.clipRect(0, contentTop, Game.WIDTH, contentBottom - contentTop);

        int y = contentTop - (int) settingsScroll;

        // Render by category
        for (int cat = 0; cat < 4; cat++) {
            java.util.List<Achievements.Achievement> inCat = new java.util.ArrayList<>();
            for (Achievements.Achievement a : Achievements.getAll()) {
                if (a.category == cat) inCat.add(a);
            }
            if (inCat.isEmpty()) continue;

            int catUnlocked = Achievements.getUnlockedInCategory(cat);
            int catTotal = Achievements.getTotalInCategory(cat);

            // Category header
            int headerH = 40;
            if (y + headerH > contentTop - 20 && y < contentBottom + 20) {
                g.setFont(PageRenderer.HEADING_FONT);
                g.setColor(PageRenderer.ACCENT);
                g.drawString(Achievements.CAT_NAMES[cat], margin + pad, y + 28);

                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(PageRenderer.TEXT_MUTED);
                String catCount = catUnlocked + "/" + catTotal;
                fm = g.getFontMetrics();
                g.drawString(catCount, margin + pw - pad - fm.stringWidth(catCount), y + 28);

                g.setColor(PageRenderer.BORDER);
                g.fillRect(margin + pad, y + 34, pw - pad * 2, 1);
            }
            y += headerH;

            // Achievement cards
            for (Achievements.Achievement a : inCat) {
                if (y + cardH > contentTop - 10 && y < contentBottom + 10) {
                    renderAchievementCard(g, margin, y, pw, cardH, a);
                }
                y += cardH + cardGap;
            }

            y += gap;
        }

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
    }

    private void renderAchievementCard(Graphics2D g, int x, int y, int w, int h,
                                        Achievements.Achievement a) {
        boolean unlocked = a.unlocked;

        // Card background
        Color bg = unlocked ? new Color(28, 36, 50) : new Color(18, 22, 32);
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, 8, 8);

        // Left accent strip
        Color accent = unlocked ? ACH_GOLD : ACH_LOCKED;
        g.setColor(accent);
        g.fillRoundRect(x, y, 4, h, 4, 4);

        // Trophy / lock icon
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        g.setColor(unlocked ? ACH_GOLD : new Color(50, 58, 72));
        g.drawString(unlocked ? "\u2605" : "\u25CB", x + 16, y + h / 2 + 7);

        // Name
        g.setFont(PageRenderer.BUTTON_FONT);
        g.setColor(unlocked ? PageRenderer.TEXT : new Color(80, 90, 105));
        g.drawString(a.name, x + 44, y + 22);

        // Description
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(unlocked ? PageRenderer.TEXT_SEC : new Color(55, 65, 80));
        g.drawString(a.description, x + 44, y + 40);

        // Progress bar (if applicable and not unlocked)
        if (!unlocked) {
            float progress = Achievements.getProgress(a.id);
            if (progress >= 0 && progress < 1f) {
                int barW = 100;
                int barH2 = 4;
                int barX = x + w - barW - 16;
                int barY = y + h / 2 - barH2 / 2;
                g.setColor(new Color(30, 38, 52));
                g.fillRoundRect(barX, barY, barW, barH2, 2, 2);
                int fillW = (int) (barW * Math.min(progress, 1f));
                if (fillW > 0) {
                    g.setColor(new Color(78, 205, 196, 120));
                    g.fillRoundRect(barX, barY, fillW, barH2, 2, 2);
                }
                g.setFont(PageRenderer.SMALL_FONT);
                g.setColor(PageRenderer.TEXT_MUTED);
                String pctStr = (int) (progress * 100) + "%";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(pctStr, barX + barW + 6, barY + 4);
            }
        } else {
            // Unlocked checkmark
            g.setFont(PageRenderer.LABEL_FONT);
            g.setColor(ACH_GOLD);
            FontMetrics fm = g.getFontMetrics();
            String check = "UNLOCKED";
            g.drawString(check, x + w - fm.stringWidth(check) - 16, y + h / 2 + 5);
        }

        // Border
        g.setColor(unlocked ? new Color(255, 210, 80, 30) : new Color(40, 48, 62));
        g.drawRoundRect(x, y, w, h, 8, 8);
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
        drawCreditLine(g, x, y, "Audio", "Virtual Riot  /  MDK  /  Desmeon  /  Pegboard Nerds  /  Avicii  /  Skrillex"); y += spacing;
        drawCreditLine(g, x, y, "Version", "v4.0  \u2014  March 2026"); y += spacing;

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

        int contentTop = 130;
        int contentBottom = Game.HEIGHT - 30;
        int margin = 60;
        int pw = Game.WIDTH - margin * 2;
        int lineH = 22;
        int gap = 14;

        java.awt.Shape oldClip = g.getClip();
        g.clipRect(0, contentTop, Game.WIDTH, contentBottom - contentTop);

        int y = contentTop - (int) settingsScroll;

        // ===== v4.0 =====
        String[] v4lines = {
                "- Player abilities: Dash (Shift), Shield (auto), Slow-Motion (E).",
                "- 4 boss types: Classic, Splitter, Laser, Swarm \u2014 rotating per level.",
                "- Endless scaling: formula-based spawning after scripted levels.",
                "- Boss every 10th level across all difficulties.",
                "- Player XP and profile level system with persistent progression.",
                "- 50 achievements across Survival, Skill, Persistence, and Challenge.",
                "- Player skin customization: 6 shapes, 8 color palettes.",
                "- Loadout system: 8 perks, choose up to 2 before each run.",
                "- Coin Shop: 25 permanent unlocks purchased with earned coins.",
                "- Statistics dashboard with lifetime stats and recent run history.",
                "- Daily Challenge: seeded run per day, 28-day cycle, streak tracking.",
                "- Settings page: volume, screen shake, particles, FPS, colorblind mode.",
                "- Settings accessible from pause menu during gameplay.",
                "- Redesigned main menu with unified button layout."
        };
        int v4h = 52 + v4lines.length * lineH + 16;
        if (y + v4h > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, v4h);
            g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.ACCENT);
            g.drawString("v4.0", 85, y + 32);
            g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("25.03.2026", 140, y + 32);
            g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, pw - 50, 1);
            g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_SEC);
            int ly = y + 66;
            for (String line : v4lines) { g.drawString(line, 85, ly); ly += lineH; }
        }
        y += v4h + gap;

        // ===== v3.0 =====
        String[] v3lines = {
                "- Complete visual overhaul with modern dark theme.",
                "- Fullscreen mode with automatic resolution scaling.",
                "- Built-in music player with playback controls."
        };
        int v3h = 52 + v3lines.length * lineH + 16;
        if (y + v3h > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, v3h);
            g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.TEXT);
            g.drawString("v3.0", 85, y + 32);
            g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("25.03.2026", 140, y + 32);
            g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, pw - 50, 1);
            g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_SEC);
            int ly = y + 66;
            for (String line : v3lines) { g.drawString(line, 85, ly); ly += lineH; }
        }
        y += v3h + gap;

        // ===== v2.0 =====
        String[] v2lines = {
                "- Complete rewrite and overhaul of legacy codebase.",
                "- Fixed resolution bugs, removed duplicate classes, cleaned up all dead code.",
                "- No new features \u2014 focused on code quality and stability."
        };
        int v2h = 52 + v2lines.length * lineH + 16;
        if (y + v2h > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, v2h);
            g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("v2.0", 85, y + 32);
            g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("25.03.2026", 140, y + 32);
            g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, pw - 50, 1);
            g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            int ly = y + 66;
            for (String line : v2lines) { g.drawString(line, 85, ly); ly += lineH; }
        }
        y += v2h + gap;

        // ===== v1.0 =====
        String[] v1lines = {
                "- Name changed from \"Dodge Game!\" to \"Dotch\".",
                "- Game layout updated. Added contact page and language support."
        };
        int v1h = 52 + v1lines.length * lineH + 16;
        if (y + v1h > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, v1h);
            g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("v1.0", 85, y + 32);
            g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("10.11.2019", 140, y + 32);
            g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, pw - 50, 1);
            g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            int ly = y + 66;
            for (String line : v1lines) { g.drawString(line, 85, ly); ly += lineH; }
        }
        y += v1h + gap;

        // ===== v0.1 =====
        String[] v01lines = {
                "- Initial release with core game mechanics.",
                "- Player movement, enemy spawning, collision detection, and scoring.",
                "- Three difficulty modes, boss fights, HUD, and in-game shop."
        };
        int v01h = 52 + v01lines.length * lineH + 16;
        if (y + v01h > contentTop - 20 && y < contentBottom + 20) {
            PageRenderer.drawPanel(g, margin, y, pw, v01h);
            g.setFont(PageRenderer.HEADING_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("v0.1", 85, y + 32);
            g.setFont(PageRenderer.SMALL_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("09.08.2016", 138, y + 32);
            g.setColor(PageRenderer.BORDER); g.fillRect(85, y + 44, pw - 50, 1);
            g.setFont(PageRenderer.BODY_FONT); g.setColor(PageRenderer.TEXT_MUTED);
            int ly = y + 66;
            for (String line : v01lines) { g.drawString(line, 85, ly); ly += lineH; }
        }
        y += v01h + gap;

        int totalHeight = y + (int) settingsScroll - contentTop;
        int maxScroll = Math.max(0, totalHeight - (contentBottom - contentTop) + 20);
        settingsScrollTarget = Math.min(settingsScrollTarget, maxScroll);

        g.setClip(oldClip);

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
        int bestScore = Profile.getHighScore(game.diff);
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
        drawStatRow(g, rightX + 20, sy, panelW - 40, "Total purchases", String.valueOf(game.lastUpgrades)); sy += 26;

        // Perks used
        int p1 = Perks.getEquipped(0), p2 = Perks.getEquipped(1);
        String perksUsed = (p1 < 0 && p2 < 0) ? "None" :
                (p1 >= 0 && p2 >= 0) ? Perks.NAMES[p1] + ", " + Perks.NAMES[p2] :
                (p1 >= 0) ? Perks.NAMES[p1] : Perks.NAMES[p2];
        drawStatRow(g, rightX + 20, sy, panelW - 40, "Perks", perksUsed);

        // XP panel — below stats
        int xpY = statsY + 210;
        int xpW = Game.WIDTH - 120;
        PageRenderer.drawPanel(g, 60, xpY, xpW, 52);

        g.setFont(PageRenderer.HEADING_FONT);
        g.setColor(PageRenderer.ACCENT);
        g.drawString("+" + game.lastXpEarned + " XP", 80, xpY + 24);
        // Coins earned
        g.setColor(new Color(255, 210, 80));
        String coinStr = "+" + game.lastCoinsEarned + " coins";
        g.drawString(coinStr, 260, xpY + 24);

        // Level + XP bar
        g.setFont(PageRenderer.LABEL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String lvlStr = "LVL " + Profile.getLevel();
        g.drawString(lvlStr, 80, xpY + 44);

        int xpBarX = 140;
        int xpBarW = xpW - 100;
        int xpBarH = 8;
        int xpBarY = xpY + 36;
        g.setColor(new Color(22, 30, 44));
        g.fillRoundRect(xpBarX, xpBarY, xpBarW, xpBarH, 4, 4);
        float xpPct = Profile.levelProgress();
        int xpFillW = (int) (xpBarW * xpPct);
        if (xpFillW > 0) {
            g.setColor(PageRenderer.ACCENT);
            g.fillRoundRect(xpBarX, xpBarY, xpFillW, xpBarH, 4, 4);
        }
        g.setColor(PageRenderer.BORDER);
        g.drawRoundRect(xpBarX, xpBarY, xpBarW, xpBarH, 4, 4);

        // Level-up notification
        if (game.lastLeveledUp) {
            g.setFont(PageRenderer.BUTTON_FONT);
            g.setColor(new Color(255, 210, 80));
            String lupStr = "LEVEL UP!  LVL " + Profile.getLevel();
            fm = g.getFontMetrics();
            g.drawString(lupStr, 60 + xpW - fm.stringWidth(lupStr) - 20, xpY + 28);
        }

        // Achievement count
        g.setFont(PageRenderer.SMALL_FONT);
        g.setColor(PageRenderer.TEXT_MUTED);
        String achStr = Achievements.getUnlockedCount() + "/" + Achievements.getCount() + " achievements";
        g.drawString(achStr, 80, xpY + 44 + 14);

        // Buttons
        PageRenderer.drawPrimaryButton(g, retryX(), RETRY_Y, RETRY_W, RETRY_H, "Try Again", retryH);
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
