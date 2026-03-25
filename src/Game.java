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
    public int lastScore = 0;

    private Random r;
    private Handler handler;
    private HUD hud;
    private Spawn spawner;
    private Menu menu;
    private Shop shop;
    private MusicPlayer musicPlayer;

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
        End,
        Paused
    }

    public static STATE gameState = STATE.Menu;

    public Game() {
        System.out.println("Loading game...");

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
        this.addMouseMotionListener(menu);
        this.addMouseMotionListener(shop);
        this.addMouseMotionListener(musicPlayer);

        AudioPlayer.load();
        AudioPlayer.play();

        new Window(windowWidth, windowHeight, "Dotch. - v3.0", this);

        spawner = new Spawn(handler, hud, this);
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
        if (gameState == STATE.Paused) {
            menu.tick(); // for hover animations
            return;
        }
        if (gameState == STATE.Game) {
            hud.tick();
            spawner.tick();
            handler.tick();
            if (HUD.HEALTH <= 0) {
                // Capture score before resetting
                lastScore = hud.getScore();
                HUD.HEALTH = 100;
                hud.bounds = 0;
                gameState = STATE.End;
                handler.getObjects().clear();
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

        // Scale — everything below is in logical coordinates (WIDTHxHEIGHT)
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // FPS counter
        g.setColor(fps >= 240 ? Color.green : Color.gray);
        g.setFont(FPS_FONT);
        g.drawString("FPS: " + fps, WIDTH - 120, 19);

        if (gameState == STATE.Game || gameState == STATE.Shop || gameState == STATE.Paused) {
            PageRenderer.drawGameBackground(g);
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
        } else if (gameState == STATE.Game) {
            handler.render(g);
            hud.render(g);
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
