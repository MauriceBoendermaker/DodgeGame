import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Game extends Canvas implements Runnable {

    private static final long serialVersionUID = -4241816582633136533L;

    // Base (logical) resolution — all game logic uses these
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    // Scaling
    private static double scale = 1.0;
    private static int windowWidth = WIDTH;
    private static int windowHeight = HEIGHT;

    private Thread thread;
    private boolean running = false;

    public static boolean paused = false;

    public int diff = 1;

    private int fps = 0;

    private Random r;
    private Handler handler;
    private HUD hud;
    private Spawn spawner;
    private Menu menu;
    private Shop shop;
    private MusicPlayer musicPlayer;

    private BufferedImage main_image;

    private static final Font FPS_FONT = new Font("Arial", Font.PLAIN, 20);
    private static final Font PAUSED_FONT = new Font("Arial", Font.BOLD, 125);

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
        End
    }

    public static STATE gameState = STATE.Menu;

    public static BufferedImage sprite_sheet;
    public static BufferedImage sprite_sheet2;
    public static BufferedImage sprite_sheet3;
    public static BufferedImage sprite_sheet4;
    public static BufferedImage sprite_sheet5;
    public static BufferedImage sprite_sheet6;
    public static BufferedImage sprite_sheet7;
    public static BufferedImage sprite_sheet8;
    public static BufferedImage sprite_sheet9;

    public Game() {
        BufferedImageLoader loader = new BufferedImageLoader();
        sprite_sheet = loader.loadImage("MainPage.png");
        sprite_sheet2 = loader.loadImage("PlayPage.png");
        sprite_sheet3 = loader.loadImage("InfoPage.png");
        sprite_sheet4 = loader.loadImage("HelpPage.png");
        sprite_sheet5 = loader.loadImage("UpdatesPage.png");
        sprite_sheet6 = loader.loadImage("AboutPage.png");
        sprite_sheet7 = loader.loadImage("HelpEnglish.png");
        sprite_sheet8 = loader.loadImage("HelpNederlands.png");
        sprite_sheet9 = loader.loadImage("HelpDeutsch.png");

        main_image = new SpriteSheet(sprite_sheet).grabImage(1, 1, 720, 1280);

        System.out.println("Loading game...");
        System.out.println("Game loaded!");

        // Calculate display size — largest 16:9 that fits the screen
        calculateWindowSize();

        handler = new Handler();
        hud = new HUD();
        shop = new Shop(handler, hud);
        menu = new Menu(this, handler, hud);
        musicPlayer = new MusicPlayer();
        this.addKeyListener(new KeyInput(handler, this));
        this.addMouseListener(menu);
        this.addMouseListener(shop);
        this.addMouseListener(musicPlayer);

        AudioPlayer.load();
        AudioPlayer.play();

        new Window(windowWidth, windowHeight, "Dotch. - v2.0", this);

        spawner = new Spawn(handler, hud, this);
        r = new Random();

        if (gameState == STATE.Game) {
            hud.setLevel(1);
            hud.setScore(0);
            handler.addObject(new Player(WIDTH / 2 - 32, HEIGHT / 2 - 32, ID.Player, handler));
            handler.addObject(new BasicEnemy(r.nextInt(WIDTH - 50), r.nextInt(HEIGHT - 50), ID.BasicEnemy, handler));
        }
    }

    private void calculateWindowSize() {
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int availW = screenBounds.width;
        int availH = screenBounds.height;

        // Largest 16:9 that fits
        if (availW * 9 <= availH * 16) {
            windowWidth = availW;
            windowHeight = availW * 9 / 16;
        } else {
            windowHeight = availH;
            windowWidth = availH * 16 / 9;
        }

        scale = windowWidth / (double) WIDTH;
        System.out.println("Display: " + windowWidth + "x" + windowHeight + " (scale: " + String.format("%.2f", scale) + ")");
    }

    /** Convert screen pixel X to game coordinate */
    public static int toGameX(int screenX) {
        return (int) (screenX / scale);
    }

    /** Convert screen pixel Y to game coordinate */
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
        if (gameState == STATE.Game) {
            if (!paused) {
                hud.tick();
                spawner.tick();
                handler.tick();
            }
            if (HUD.HEALTH <= 0) {
                HUD.HEALTH = 100;
                hud.bounds = 0;
                hud.setLevel(1);
                hud.setPoints(0);
                gameState = STATE.End;
                handler.clearEnemys();
                for (int i = 0; i < 15; i++) {
                    handler.addObject(new MenuParticle(r.nextInt(WIDTH), r.nextInt(HEIGHT), ID.MenuParticle, handler));
                }
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

        // Apply scaling — everything below is in base 1280x720 coordinates
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (gameState == STATE.Menu) {
            g.drawImage(main_image, 0, 0, null);
        }

        // FPS counter
        g.setColor(fps >= 240 ? Color.green : Color.gray);
        g.setFont(FPS_FONT);
        g.drawString("FPS: " + fps, 1160, 19);

        if (paused) {
            g.setFont(PAUSED_FONT);
            g.setColor(Color.orange);
            g.drawString("PAUSED", WIDTH / 2 - 225, HEIGHT / 2);
        }

        if (gameState == STATE.Game) {
            hud.render(g);
            handler.render(g);
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

        g.dispose();
        bs.show();
    }

    public static float clamp(float var, float min, float max) {
        if (var >= max) return max;
        if (var <= min) return min;
        return var;
    }

    public static void main(String[] args) {
        new Game();
        System.out.println("Game Running...");
    }
}
