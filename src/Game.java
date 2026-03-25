import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import java.util.Random;

public class Game extends Canvas implements Runnable {

    private static final long serialVersionUID = -4241816582633136533L;

    // Logical resolution — HEIGHT is fixed, WIDTH adapts to screen aspect ratio
    public static int WIDTH = 1280;
    public static final int HEIGHT = 720;

    // Scaling
    private static double scale = 1.0;
    private static int windowWidth = 1280;
    private static int windowHeight = 720;

    private Thread thread;
    private boolean running = false;

    // Stores which state we came from when pausing (Game or Shop)
    public static STATE pausedFrom = STATE.Game;

    public int diff = 1;

    private int fps = 0;

    // Attempt display
    public static int currentAttempt = 0;
    public static float attemptFade = 0;
    private static final Font ATTEMPT_FONT = new Font("Arial", Font.BOLD, 20);
    private static final Font ATTEMPT_NUM_FONT = new Font("Arial", Font.BOLD, 28);

    // End-of-run stats
    public int lastScore = 0;
    public boolean lastIsHighScore = false;
    public int lastLevel = 0;
    public String lastTime = "";
    public int lastEnemies = 0;
    public int lastUpgrades = 0;
    public int lastHealthUps = 0;
    public int lastSpeedUps = 0;
    public int lastRefills = 0;
    public String lastDifficulty = "";

    private Random r;
    private static Handler handlerRef;
    private static Spawn spawnerRef;
    private Handler handler;
    private HUD hud;
    private Spawn spawner;
    private Menu menu;
    public Shop shop;
    private MusicPlayer musicPlayer;

    private static final Font PAUSED_FONT = new Font("Arial", Font.BOLD, 125);

    // Screen shake + damage flash
    private static float shakeIntensity = 0;
    private static float flashAlpha = 0;
    private static Random shakeRng = new Random();

    // Level-up pulse
    private static float levelUpFlash = 0;

    // Boss intro cinematic
    private static int bossIntroTimer = 0;
    private static boolean bossIntroActive = false;
    private static float[][] enemyExplosions; // [x, y, vx, vy, size, r, g, b, life]

    public static void triggerBossIntro() {
        bossIntroActive = true;
        bossIntroTimer = 0;
        shakeIntensity = 4f;
    }

    public static boolean isBossIntroActive() {
        return bossIntroActive;
    }

    public static void setEnemyExplosions(float[][] particles) {
        enemyExplosions = particles;
    }

    // Speed lines + camera intensity
    private float playerSpeed = 0;
    private float playerVelX = 0, playerVelY = 0;
    private float cameraZoom = 1f;
    private float cameraZoomTarget = 1f;
    private float[][] speedLines = new float[20][5]; // [x, y, angle, length, life]
    private int speedLineTimer = 0;

    // Geometric background layers
    private float geoPhase = 0;
    private java.awt.image.BufferedImage geoCache;
    private int geoCacheFrame = 0;

    // Screen transitions
    private STATE lastState = STATE.Menu;
    private float transitionAlpha = 0;
    private float transitionZoom = 0; // extra zoom for menu→game
    private static final float TRANS_SPEED = 0.12f;

    // Neon walls — impact flares [x, y, intensity, side(0=top,1=bottom,2=left,3=right)]
    private static float[][] wallFlares = new float[32][4];
    private static int wallFlareCount = 0;
    private static float wallPulsePhase = 0;

    public static void wallHit(float hitX, float hitY, int side) {
        if (wallFlareCount < wallFlares.length) {
            wallFlares[wallFlareCount] = new float[]{hitX, hitY, 1f, side};
            wallFlareCount++;
        }
    }

    // Death animation
    private int deathTimer = 0;
    private float deathFlash = 0;
    private float[][] deathParticles; // [i][0]=x, [1]=y, [2]=vx, [3]=vy, [4]=size, [5]=r, [6]=g, [7]=b
    private float deathPlayerX, deathPlayerY;

    public static void triggerHit() {
        shakeIntensity = 8f;
        flashAlpha = 0.6f;
    }

    public static float getLevelProgress() {
        return spawnerRef != null ? spawnerRef.getLevelProgress() : 0;
    }

    public static boolean isBossActive() {
        return spawnerRef != null && spawnerRef.isBossActive();
    }

    public static int getPlayerMultiplier() {
        if (handlerRef == null) return 1;
        for (int i = 0; i < handlerRef.getObjects().size(); i++) {
            GameObject obj = handlerRef.getObjects().get(i);
            if (obj instanceof Player) return ((Player) obj).getMultiplier();
        }
        return 1;
    }

    public static void triggerLevelUp() {
        levelUpFlash = 1f;
    }

    public enum STATE {
        Menu,
        MusicPlayer,
        Info,
        About,
        Update_Notes,
        Select,
        Help,
        HelpNLD,
        HelpENG,
        HelpFRA,
        HelpDEU,
        HelpRUS,
        HelpSPA,
        Shop,
        Game,
        Credits,
        End,
        Paused,
        Dying
    }

    public static STATE gameState = STATE.Menu;

    public Game() {
        System.out.println("Loading game...");

        // Calculate display size — largest 16:9 that fits the screen
        calculateWindowSize();

        handler = new Handler();
        handlerRef = handler;
        hud = new HUD();
        shop = new Shop(handler, hud);
        menu = new Menu(this, handler, hud);
        musicPlayer = new MusicPlayer();
        this.addKeyListener(new KeyInput(handler, this));
        this.addMouseListener(menu);
        this.addMouseListener(shop);
        this.addMouseListener(musicPlayer);
        this.addMouseMotionListener(menu);
        this.addMouseMotionListener(shop);
        this.addMouseWheelListener(menu);
        this.addMouseMotionListener(musicPlayer);

        AudioPlayer.load();
        AudioPlayer.play();

        new Window(windowWidth, windowHeight, "Dotch. - v3.0", this);

        spawner = new Spawn(handler, hud, this);
        spawnerRef = spawner;
        r = new Random();

        System.out.println("Game loaded!");

        if (gameState == STATE.Game) {
            hud.setLevel(1);
            hud.setScore(0);
            handler.addObject(new Player(WIDTH / 2 - 32, HEIGHT / 2 - 32, ID.Player, handler));
            handler.addObject(new BasicEnemy(r.nextInt(WIDTH - 50), r.nextInt(HEIGHT - 50), ID.BasicEnemy, handler));
        }
    }

    private void calculateWindowSize() {
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        windowWidth = screen.width;
        windowHeight = screen.height;

        // Scale uniformly by height — WIDTH adapts to screen aspect ratio
        scale = windowHeight / (double) HEIGHT;
        WIDTH = (int) (windowWidth / scale);

        System.out.println("Fullscreen: " + windowWidth + "x" + windowHeight
                + " (logical: " + WIDTH + "x" + HEIGHT + ", scale: " + String.format("%.2f", scale) + ")");
    }

    public static int toGameX(int screenX) {
        return (int) (screenX / scale);
    }

    public static int toGameY(int screenY) {
        return (int) (screenY / scale);
    }

    public static double getScale() {
        return scale;
    }

    public synchronized void start() {
        thread = new Thread(this);
        thread.start();
        running = true;
    }

    public synchronized void stop() {
        try {
            thread.join();
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        this.requestFocus();
        long lastTime = System.nanoTime();
        double ns = 1000000000.0 / 60.0;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                delta--;
            }

            if (running) {
                render();
            }
            frames++;
            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                fps = frames;
                frames = 0;
            }
        }

        stop();
    }

    private void tick() {
        // Auto-advance music — runs in every state
        if (!AudioPlayer.isStopped() && !AudioPlayer.isPaused() && !AudioPlayer.isPlaying()) {
            AudioPlayer.nextTrack();
            AudioPlayer.play();
        }

        if (gameState == STATE.Paused) {
            menu.tick(); // for hover animations
            return;
        }
        // Decay screen effects
        if (shakeIntensity > 0.1f) shakeIntensity *= 0.85f;
        else shakeIntensity = 0;
        if (flashAlpha > 0.01f) flashAlpha *= 0.88f;
        else flashAlpha = 0;
        if (levelUpFlash > 0.01f) levelUpFlash *= 0.93f;
        else levelUpFlash = 0;
        if (attemptFade > 0.005f) attemptFade *= 0.985f;
        else attemptFade = 0;

        // Boss intro cinematic
        if (bossIntroActive) {
            bossIntroTimer++;
            // Shake escalates during warning
            if (bossIntroTimer < 100) {
                shakeIntensity = Math.max(shakeIntensity, 2f + bossIntroTimer * 0.06f);
            }
            // Big shake on boss crash (frame 100)
            if (bossIntroTimer == 100) {
                shakeIntensity = 16f;
            }
            // Animate enemy explosion particles
            if (enemyExplosions != null) {
                for (float[] p : enemyExplosions) {
                    if (p[8] > 0) {
                        p[0] += p[2];
                        p[1] += p[3];
                        p[3] += 0.12f; // gravity
                        p[2] *= 0.97f;
                        p[8] -= 0.015f;
                    }
                }
            }
            // End intro
            if (bossIntroTimer >= 130) {
                bossIntroActive = false;
                enemyExplosions = null;
            }
        }

        // Screen transitions — detect state changes
        if (gameState != lastState) {
            // Skip transitions for states that have their own animations
            if (lastState != STATE.Dying && gameState != STATE.Dying
                    && gameState != STATE.Paused && lastState != STATE.Paused) {
                transitionAlpha = 1f;
                // Zoom-in effect when entering gameplay
                if (gameState == STATE.Game && lastState != STATE.Shop) {
                    transitionZoom = 1f;
                }
            }
            lastState = gameState;
        }
        if (transitionAlpha > 0.01f) transitionAlpha *= (1f - TRANS_SPEED);
        else transitionAlpha = 0;
        if (transitionZoom > 0.01f) transitionZoom *= 0.90f;
        else transitionZoom = 0;

        // Geometric layer animation
        geoPhase += 0.008f;

        // Wall pulse + flare decay
        wallPulsePhase += 0.04f;
        int alive = 0;
        for (int i = 0; i < wallFlareCount; i++) {
            wallFlares[i][2] *= 0.88f;
            if (wallFlares[i][2] > 0.02f) {
                wallFlares[alive] = wallFlares[i];
                alive++;
            }
        }
        wallFlareCount = alive;

        if (gameState == STATE.Game) {
            GamePalette.update(hud.getLevel());
            hud.tick();
            spawner.tick();
            handler.tick();

            // Track player velocity for speed lines
            int enemyCount = 0;
            for (int i = 0; i < handler.getObjects().size(); i++) {
                GameObject obj = handler.getObjects().get(i);
                if (obj instanceof Player) {
                    playerVelX = obj.getVelX();
                    playerVelY = obj.getVelY();
                    playerSpeed = (float) Math.sqrt(playerVelX * playerVelX + playerVelY * playerVelY);
                }
                ID id = obj.getId();
                if (id == ID.BasicEnemy || id == ID.FastEnemy || id == ID.SmartEnemy
                        || id == ID.HardEnemy || id == ID.EnemyBoss) {
                    enemyCount++;
                }
            }

            // Camera zoom — subtle zoom-out as enemies accumulate
            cameraZoomTarget = 1f - Math.min(enemyCount, 8) * 0.008f; // max ~6.4% zoom out
            cameraZoom += (cameraZoomTarget - cameraZoom) * 0.03f;

            // Spawn speed lines based on player movement
            updateSpeedLines();
            if (HUD.HEALTH <= 0) {
                // Capture all run stats
                lastScore = hud.getScore();
                lastLevel = hud.getLevel();
                lastTime = hud.getTimeSurvived();
                lastUpgrades = hud.getTotalUpgrades();
                lastHealthUps = hud.getHealthUpgrades();
                lastSpeedUps = hud.getSpeedUpgrades();
                lastRefills = hud.getRefills();
                lastDifficulty = diff == 0 ? "Normal" : diff == 1 ? "Hard" : "Insane";
                lastIsHighScore = Stats.submitScore(diff, lastScore);

                lastEnemies = 0;
                for (int i = 0; i < handler.getObjects().size(); i++) {
                    ID id = handler.getObjects().get(i).getId();
                    if (id == ID.BasicEnemy || id == ID.FastEnemy || id == ID.SmartEnemy
                            || id == ID.HardEnemy || id == ID.EnemyBoss) {
                        lastEnemies++;
                    }
                }

                // Find player position for death particles
                for (int i = 0; i < handler.getObjects().size(); i++) {
                    if (handler.getObjects().get(i).getId() == ID.Player) {
                        deathPlayerX = handler.getObjects().get(i).getX() + 24;
                        deathPlayerY = handler.getObjects().get(i).getY() + 24;
                        break;
                    }
                }

                // Spawn death particles
                int count = 25 + r.nextInt(10);
                deathParticles = new float[count][8];
                for (int i = 0; i < count; i++) {
                    float angle = (float) (r.nextFloat() * Math.PI * 2);
                    float speed = 2f + r.nextFloat() * 6f;
                    float size = 3 + r.nextFloat() * 8;
                    // White/teal fragments
                    float tint = r.nextFloat();
                    deathParticles[i] = new float[]{
                            deathPlayerX, deathPlayerY,
                            (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                            size,
                            200 + (int) (55 * tint), 220 + (int) (35 * tint), 230 + (int) (25 * tint)
                    };
                }

                // Remove player from world
                handler.getObjects().removeIf(obj -> obj.getId() == ID.Player);

                deathTimer = 0;
                deathFlash = 1f;
                shakeIntensity = 14f;
                gameState = STATE.Dying;
            }
        } else if (gameState == STATE.Dying) {
            deathTimer++;

            // Freeze for first 12 frames (200ms), then animate particles
            if (deathTimer > 12 && deathParticles != null) {
                for (float[] p : deathParticles) {
                    p[0] += p[2]; // x += vx
                    p[1] += p[3]; // y += vy
                    p[3] += 0.15f; // gravity
                    p[2] *= 0.98f; // drag
                    p[4] *= 0.985f; // shrink
                }
            }

            // White flash decay
            if (deathFlash > 0.01f) deathFlash *= 0.92f;
            else deathFlash = 0;

            // Transition to End screen after ~120 frames (2 seconds)
            if (deathTimer >= 120) {
                HUD.HEALTH = 100;
                hud.bounds = 0;
                gameState = STATE.End;
                handler.getObjects().clear();
                for (int i = 0; i < 15; i++) {
                    handler.addObject(new MenuParticle(r.nextInt(WIDTH), r.nextInt(HEIGHT), ID.MenuParticle, handler));
                }
                deathParticles = null;
            }
        } else if (gameState == STATE.Shop) {
            // shop handles its own input via mouse listener
        } else if (gameState == STATE.MusicPlayer) {
            musicPlayer.tick();
        } else {
            menu.tick();
            handler.tick();
        }
    }

    private void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics2D g = (Graphics2D) bs.getDrawGraphics();

        // Clear at native resolution before scaling
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Scale — everything below is in logical coordinates (WIDTHxHEIGHT)
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Transition zoom — slight zoom-in when entering game
        boolean hasTransZoom = transitionZoom > 0.02f;
        if (hasTransZoom) {
            float z = 1f + transitionZoom * 0.06f;
            g.translate(WIDTH * (1 - z) / 2, HEIGHT * (1 - z) / 2);
            g.scale(z, z);
        }

        if (gameState == STATE.Game || gameState == STATE.Shop || gameState == STATE.Paused || gameState == STATE.Dying) {
            PageRenderer.drawGameBackground(g);
            renderBeatVisuals(g);
            renderGeoLayers(g);
            renderNeonWalls(g);
        }

        if (gameState == STATE.Paused) {
            // Draw frozen game underneath
            handler.render(g);
            hud.render(g);
            // Dim overlay
            g.setColor(new Color(8, 10, 16, 180));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            // Pause menu rendered by Menu
            menu.render(g);
        } else if (gameState == STATE.Dying) {
            // Screen shake applies to frozen world
            float dsx = 0, dsy = 0;
            if (shakeIntensity > 0) {
                dsx = (shakeRng.nextFloat() - 0.5f) * 2 * shakeIntensity;
                dsy = (shakeRng.nextFloat() - 0.5f) * 2 * shakeIntensity;
                g.translate(dsx, dsy);
            }

            // Render frozen game world (enemies still visible, player removed)
            handler.render(g);

            if (shakeIntensity > 0) g.translate(-dsx, -dsy);

            // Death particles
            if (deathParticles != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float fade = Math.max(0, 1f - deathTimer / 100f);
                for (float[] p : deathParticles) {
                    if (p[4] < 0.5f) continue;
                    int alpha = (int) (fade * 255);
                    g.setColor(new Color((int) p[5], (int) p[6], (int) p[7], Math.max(0, Math.min(255, alpha))));
                    int s = (int) p[4];
                    g.fillRoundRect((int) p[0] - s / 2, (int) p[1] - s / 2, s, s, 3, 3);
                }
            }

            // White flash overlay
            if (deathFlash > 0) {
                g.setColor(new Color(255, 255, 255, (int) (deathFlash * 200)));
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }

            // Fade to black as we approach transition
            if (deathTimer > 80) {
                float blackFade = (deathTimer - 80) / 40f;
                g.setColor(new Color(10, 12, 18, (int) (Math.min(1, blackFade) * 255)));
                g.fillRect(0, 0, WIDTH, HEIGHT);
            }

            hud.render(g);
        } else if (gameState == STATE.Game) {
            // Camera zoom — subtle zoom-out with enemy count
            if (cameraZoom != 1f) {
                float zoomOffX = WIDTH * (1f - cameraZoom) / 2f;
                float zoomOffY = HEIGHT * (1f - cameraZoom) / 2f;
                g.translate(zoomOffX, zoomOffY);
                g.scale(cameraZoom, cameraZoom);
            }

            // Screen shake — offset the game world
            float sx = 0, sy = 0;
            if (shakeIntensity > 0) {
                sx = (shakeRng.nextFloat() - 0.5f) * 2 * shakeIntensity;
                sy = (shakeRng.nextFloat() - 0.5f) * 2 * shakeIntensity;
                g.translate(sx, sy);
            }
            handler.render(g);
            if (shakeIntensity > 0) {
                g.translate(-sx, -sy);
            }

            // Speed lines
            renderSpeedLines(g);

            // Reset camera zoom transform
            if (cameraZoom != 1f) {
                g.scale(1.0 / cameraZoom, 1.0 / cameraZoom);
                float zoomOffX = WIDTH * (1f - cameraZoom) / 2f;
                float zoomOffY = HEIGHT * (1f - cameraZoom) / 2f;
                g.translate(-zoomOffX, -zoomOffY);
            }

            // Damage flash — red border vignette
            if (flashAlpha > 0) {
                int border = 60;
                int alpha = (int) (flashAlpha * 255);
                // Top
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(235, 50, 50, alpha), 0, border, new Color(235, 50, 50, 0)));
                g.fillRect(0, 0, WIDTH, border);
                // Bottom
                g.setPaint(new java.awt.GradientPaint(0, HEIGHT - border, new Color(235, 50, 50, 0), 0, HEIGHT, new Color(235, 50, 50, alpha)));
                g.fillRect(0, HEIGHT - border, WIDTH, border);
                // Left
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(235, 50, 50, alpha), border, 0, new Color(235, 50, 50, 0)));
                g.fillRect(0, 0, border, HEIGHT);
                // Right
                g.setPaint(new java.awt.GradientPaint(WIDTH - border, 0, new Color(235, 50, 50, 0), WIDTH, 0, new Color(235, 50, 50, alpha)));
                g.fillRect(WIDTH - border, 0, border, HEIGHT);
            }

            // Boss intro cinematic overlay
            if (bossIntroActive) {
                renderBossIntro(g);
            }

            // Level-up flash — teal border pulse
            if (levelUpFlash > 0) {
                int border = 80;
                int alpha = (int) (levelUpFlash * 150);
                g.setPaint(new java.awt.GradientPaint(0, 0, GamePalette.accent(alpha), 0, border, GamePalette.accent(0)));
                g.fillRect(0, 0, WIDTH, border);
                g.setPaint(new java.awt.GradientPaint(0, HEIGHT - border, GamePalette.accent(0), 0, HEIGHT, GamePalette.accent(alpha)));
                g.fillRect(0, HEIGHT - border, WIDTH, border);
                g.setPaint(new java.awt.GradientPaint(0, 0, GamePalette.accent(alpha), border, 0, GamePalette.accent(0)));
                g.fillRect(0, 0, border, HEIGHT);
                g.setPaint(new java.awt.GradientPaint(WIDTH - border, 0, GamePalette.accent(0), WIDTH, 0, GamePalette.accent(alpha)));
                g.fillRect(WIDTH - border, 0, border, HEIGHT);
            }

            hud.render(g);

            // Attempt counter fade-in at start of run
            if (attemptFade > 0.05f) {
                int aAlpha = (int) (attemptFade * 200);
                float drift = (1f - attemptFade) * 15;
                g.setFont(ATTEMPT_FONT);
                g.setColor(new Color(140, 150, 165, Math.min(aAlpha, 255)));
                String aLabel = "ATTEMPT";
                java.awt.FontMetrics afm = g.getFontMetrics();
                int ax = (WIDTH - afm.stringWidth(aLabel)) / 2;
                g.drawString(aLabel, ax, HEIGHT / 2 + 60 + (int) drift);

                g.setFont(ATTEMPT_NUM_FONT);
                g.setColor(new Color(230, 234, 240, Math.min(aAlpha, 255)));
                String aNum = "#" + currentAttempt;
                afm = g.getFontMetrics();
                g.drawString(aNum, (WIDTH - afm.stringWidth(aNum)) / 2, HEIGHT / 2 + 90 + (int) drift);
            }
        } else if (gameState == STATE.Shop) {
            shop.render(g);
        } else if (gameState == STATE.MusicPlayer) {
            musicPlayer.render(g);
        } else if (gameState == STATE.Menu || gameState == STATE.End
                || gameState == STATE.Select || gameState == STATE.Credits) {
            menu.render(g);
        } else {
            menu.render(g);
            handler.render(g);
        }

        // Undo transition zoom
        if (hasTransZoom) {
            float z = 1f + transitionZoom * 0.06f;
            g.scale(1.0 / z, 1.0 / z);
            g.translate(WIDTH * (z - 1) / 2, HEIGHT * (z - 1) / 2);
        }

        // Screen transition overlay — fast fade from black
        if (transitionAlpha > 0) {
            g.setColor(new Color(10, 12, 18, (int) (transitionAlpha * 255)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // FPS counter — in-game only, bottom-left
        if (gameState == STATE.Game || gameState == STATE.Paused) {
            g.setFont(PageRenderer.SMALL_FONT);
            g.setColor(PageRenderer.TEXT_MUTED);
            g.drawString("FPS: " + fps, 24, HEIGHT - 12);
        }

        g.dispose();
        bs.show();
    }

    private void renderBossIntro(Graphics2D g) {
        // Dim overlay — builds up during warning
        float dimProgress = Math.min(bossIntroTimer / 80f, 1f);
        if (bossIntroTimer < 100) {
            g.setColor(new Color(10, 5, 5, (int) (dimProgress * 120)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        } else {
            // Flash on crash then fade
            float crashFade = Math.max(0, 1f - (bossIntroTimer - 100) / 30f);
            g.setColor(new Color(10, 5, 5, (int) (crashFade * 80)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // Enemy explosion particles
        if (enemyExplosions != null) {
            for (float[] p : enemyExplosions) {
                if (p[8] <= 0) continue;
                int alpha = (int) (p[8] * 255);
                g.setColor(new Color((int) p[5], (int) p[6], (int) p[7], Math.min(alpha, 255)));
                int s = (int) p[4];
                g.fillRoundRect((int) p[0] - s / 2, (int) p[1] - s / 2, s, s, 3, 3);
            }
        }

        // WARNING text — pulsing red
        if (bossIntroTimer < 100) {
            float pulse = (float) (Math.sin(bossIntroTimer * 0.2) * 0.4 + 0.6);
            int textAlpha = (int) (pulse * 255 * dimProgress);

            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 72));
            g.setColor(new Color(235, 60, 60, Math.min(textAlpha, 255)));
            java.awt.FontMetrics fm = g.getFontMetrics();
            String warn = "WARNING";
            g.drawString(warn, (WIDTH - fm.stringWidth(warn)) / 2, HEIGHT / 2 - 10);

            // Subtitle
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 22));
            g.setColor(new Color(235, 60, 60, Math.min((int) (textAlpha * 0.6f), 255)));
            fm = g.getFontMetrics();
            String sub = "BOSS INCOMING";
            g.drawString(sub, (WIDTH - fm.stringWidth(sub)) / 2, HEIGHT / 2 + 30);

            // Red border lines pulse
            int lineAlpha = (int) (pulse * 100 * dimProgress);
            g.setColor(new Color(235, 60, 60, Math.min(lineAlpha, 255)));
            g.fillRect(0, 0, WIDTH, 3);
            g.fillRect(0, HEIGHT - 3, WIDTH, 3);
            g.fillRect(0, 0, 3, HEIGHT);
            g.fillRect(WIDTH - 3, 0, 3, HEIGHT);
        }

        // White flash on boss crash
        if (bossIntroTimer >= 98 && bossIntroTimer <= 110) {
            float flashT = 1f - (bossIntroTimer - 98) / 12f;
            g.setColor(new Color(255, 255, 255, (int) (flashT * 180)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    private void updateSpeedLines() {
        speedLineTimer++;

        // Decay existing lines
        for (float[] line : speedLines) {
            if (line[4] > 0) line[4] -= 0.04f;
        }

        // Spawn new lines proportional to speed
        float threshold = 2f;
        if (playerSpeed > threshold && speedLineTimer % 2 == 0) {
            float intensity = Math.min((playerSpeed - threshold) / 10f, 1f);
            // Only spawn with probability based on intensity
            if (r.nextFloat() < intensity) {
                // Find a dead slot
                for (float[] line : speedLines) {
                    if (line[4] <= 0) {
                        // Spawn from the edge opposite to movement direction
                        float moveAngle = (float) Math.atan2(playerVelY, playerVelX);
                        // Lines come from the edges ahead of movement
                        float spread = (float) (r.nextFloat() * Math.PI - Math.PI / 2);
                        float lineAngle = moveAngle + (float) Math.PI + spread;

                        // Start position — at screen edge in the movement direction
                        float edgeX = WIDTH / 2f + (float) Math.cos(moveAngle) * (WIDTH / 2f + 20);
                        float edgeY = HEIGHT / 2f + (float) Math.sin(moveAngle) * (HEIGHT / 2f + 20);
                        // Randomize along the perpendicular
                        float perpX = (float) -Math.sin(moveAngle) * (r.nextFloat() - 0.5f) * WIDTH * 0.6f;
                        float perpY = (float) Math.cos(moveAngle) * (r.nextFloat() - 0.5f) * HEIGHT * 0.6f;

                        line[0] = edgeX + perpX;
                        line[1] = edgeY + perpY;
                        line[2] = lineAngle;
                        line[3] = 40 + r.nextFloat() * 60 * intensity; // length
                        line[4] = 0.6f + r.nextFloat() * 0.4f; // life
                        break;
                    }
                }
            }
        }
    }

    private void renderSpeedLines(Graphics2D g) {
        for (float[] line : speedLines) {
            if (line[4] <= 0) continue;
            int alpha = (int) (line[4] * 40);
            g.setColor(new Color(230, 234, 240, Math.min(alpha, 255)));
            float x1 = line[0];
            float y1 = line[1];
            float x2 = x1 + (float) Math.cos(line[2]) * line[3];
            float y2 = y1 + (float) Math.sin(line[2]) * line[3];
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        }
    }

    private void renderBeatVisuals(Graphics2D g) {
        float beat = AudioPlayer.getBeatPulse();

        // Background color tint — subtle wash from palette
        g.setColor(GamePalette.bgTint(12));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Grid dots — expand on beat
        int baseDotSize = 2;
        int beatDotSize = baseDotSize + (int) (beat * 3);
        int baseAlpha = 22 + (int) (beat * 35);
        g.setColor(GamePalette.accent(Math.min(baseAlpha, 255)));
        for (int x = 20; x < WIDTH; x += 40) {
            for (int y = 20; y < HEIGHT; y += 40) {
                g.fillOval(x - beatDotSize / 2, y - beatDotSize / 2, beatDotSize, beatDotSize);
            }
        }

        // Edge pulse — vignette throbs on beat
        if (beat > 0.1f) {
            int vigAlpha = (int) (beat * 25);
            g.setColor(GamePalette.accent(vigAlpha));
            g.fillRect(0, 0, WIDTH, 4);
            g.fillRect(0, HEIGHT - 4, WIDTH, 4);
            g.fillRect(0, 0, 4, HEIGHT);
            g.fillRect(WIDTH - 4, 0, 4, HEIGHT);
        }
    }

    private void renderGeoLayers(Graphics2D g) {
        // Redraw cache every 4 frames (15fps is plenty for slow-moving bg elements)
        if (geoCache == null || geoCache.getWidth() != WIDTH || ++geoCacheFrame >= 4) {
            geoCacheFrame = 0;
            if (geoCache == null || geoCache.getWidth() != WIDTH) {
                geoCache = new java.awt.image.BufferedImage(WIDTH, HEIGHT, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            }
            Graphics2D g2 = geoCache.createGraphics();
            g2.setComposite(java.awt.AlphaComposite.Clear);
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setComposite(java.awt.AlphaComposite.SrcOver);

            int cx = WIDTH / 2;
            int cy = HEIGHT / 2;
            float t = geoPhase;

            // Layer 1 — Large slow-rotating triangles
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.setColor(GamePalette.accent(10));
            for (int i = 0; i < 3; i++) {
                float angle = t * 0.4f + i * (float) (Math.PI * 2 / 3);
                float ox = (float) Math.cos(t * 0.15f + i) * 80;
                float oy = (float) Math.sin(t * 0.12f + i * 1.5f) * 60;
                drawRotatedTriangle(g2, cx + (int) ox, cy + (int) oy, 200 + i * 40, angle);
            }

            // Layer 2 — Hexagon ring
            g2.setColor(GamePalette.accent(8));
            g2.setStroke(new java.awt.BasicStroke(1f));
            float hexAngle = t * -0.25f;
            for (int i = 0; i < 6; i++) {
                float a = hexAngle + i * (float) (Math.PI / 3);
                drawHexagon(g2, cx + (int) (Math.cos(a) * 280), cy + (int) (Math.sin(a) * 280),
                        50 + (int) (Math.sin(t + i) * 10), a * 0.5f);
            }
            g2.setColor(GamePalette.accent(12));
            drawHexagon(g2, cx, cy, 160, t * 0.15f);

            // Layer 3 — Diagonal grid lines
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.setColor(new Color(40, 55, 75, 12));
            float go1 = (t * 30) % 80;
            for (float i = -HEIGHT; i < WIDTH + HEIGHT; i += 80) {
                g2.drawLine((int) (i + go1), 0, (int) (i + go1 - HEIGHT * 0.6f), HEIGHT);
            }
            g2.setColor(new Color(40, 55, 75, 8));
            float go2 = (t * 18) % 80;
            for (float i = -HEIGHT; i < WIDTH + HEIGHT; i += 80) {
                g2.drawLine((int) (i + go2), 0, (int) (i + go2 + HEIGHT * 0.6f), HEIGHT);
            }

            g2.dispose();
        }
        g.drawImage(geoCache, 0, 0, null);
    }

    private void drawRotatedTriangle(Graphics2D g, int cx, int cy, int size, float angle) {
        int[] xp = new int[3];
        int[] yp = new int[3];
        for (int i = 0; i < 3; i++) {
            float a = angle + i * (float) (Math.PI * 2 / 3);
            xp[i] = cx + (int) (Math.cos(a) * size / 2);
            yp[i] = cy + (int) (Math.sin(a) * size / 2);
        }
        g.drawPolygon(xp, yp, 3);
    }

    private void drawHexagon(Graphics2D g, int cx, int cy, int size, float angle) {
        int[] xp = new int[6];
        int[] yp = new int[6];
        for (int i = 0; i < 6; i++) {
            float a = angle + i * (float) (Math.PI / 3);
            xp[i] = cx + (int) (Math.cos(a) * size);
            yp[i] = cy + (int) (Math.sin(a) * size);
        }
        g.drawPolygon(xp, yp, 6);
    }

    private void renderNeonWalls(Graphics2D g) {
        float beat = AudioPlayer.getBeatPulse();
        float basePulse = (float) (Math.sin(wallPulsePhase) * 0.3 + 0.7);
        int baseAlpha = (int) ((basePulse + beat * 0.6f) * 40);
        baseAlpha = Math.min(baseAlpha, 80);

        // Thin border lines
        g.setColor(GamePalette.accent(baseAlpha));
        g.fillRect(0, 0, WIDTH, 2);
        g.fillRect(0, HEIGHT - 2, WIDTH, 2);
        g.fillRect(0, 0, 2, HEIGHT);
        g.fillRect(WIDTH - 2, 0, 2, HEIGHT);

        // Subtle edge glow (single layer, no GradientPaint)
        g.setColor(GamePalette.accent(baseAlpha / 4));
        g.fillRect(0, 0, WIDTH, 8);
        g.fillRect(0, HEIGHT - 8, WIDTH, 8);
        g.fillRect(0, 0, 8, HEIGHT);
        g.fillRect(WIDTH - 8, 0, 8, HEIGHT);

        // Impact flares — simple bright spots
        for (int i = 0; i < wallFlareCount; i++) {
            float fx = wallFlares[i][0];
            float fy = wallFlares[i][1];
            float intensity = wallFlares[i][2];
            int side = (int) wallFlares[i][3];
            int a = Math.min(255, (int) (intensity * 160));
            int aw = Math.min(255, (int) (intensity * 50));

            g.setColor(GamePalette.accent(a));
            if (side == 0)      g.fillRect((int) fx - 40, 0, 80, 4);
            else if (side == 1) g.fillRect((int) fx - 40, HEIGHT - 4, 80, 4);
            else if (side == 2) g.fillRect(0, (int) fy - 40, 4, 80);
            else                g.fillRect(WIDTH - 4, (int) fy - 40, 4, 80);

            // Wider faint glow
            g.setColor(GamePalette.accent(aw));
            if (side == 0)      g.fillRect((int) fx - 60, 0, 120, 10);
            else if (side == 1) g.fillRect((int) fx - 60, HEIGHT - 10, 120, 10);
            else if (side == 2) g.fillRect(0, (int) fy - 60, 10, 120);
            else                g.fillRect(WIDTH - 10, (int) fy - 60, 10, 120);
        }
    }

    public static float clamp(float var, float min, float max) {
        if (var >= max) return max;
        if (var <= min) return min;
        return var;
    }

    public static void main(String[] args) {
        new Game();
        System.out.println("Game running...");
    }
}
