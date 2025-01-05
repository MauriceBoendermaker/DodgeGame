import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.newdawn.slick.util.Log;

public class Game extends Canvas implements Runnable {

    private static final long serialVersionUID = -4241816582633136533L;

    public final int WIDTH = 1280, HEIGHT = 720;

    private Thread thread;
    private boolean running = false;

    public static boolean paused = false;

    public int diff = 1; //1 works

    // 0 = normal
    // 1 = hard
    // 2 = insane
    // 3 = NEM
    // 4 = NPM

    private int fps = 0;

    private Random r;
    private Handler handler;
    private HUD hud;
    private Spawn spawner;
    private Menu menu;
    private Shop shop;

    private BufferedImage main_image;

    private int B1 = 1000; //1000
    private int B2 = 750; //750
    private int B3 = 750; //750


    public enum STATE {
        Menu, //Menu = main
        MusicPlayer,
        Info,
        About,
        Update_Notes,
        Select, //Select = play
        Help,
        Help1,
        HelpNLD,
        HelpENG,
        HelpFRA,
        HelpDEU,
        HelpRUS,
        HelpSPA,
        Shop,
        Game,
        //Out_Of_Order,
        Credits,
        End,
        ;
    }

    ;

    public static STATE gameState = STATE.Menu;

    public static BufferedImage sprite_sheet; //Main

    public static BufferedImage sprite_sheet2; //Play (Select)

    public static BufferedImage sprite_sheet3; //Info

    public static BufferedImage sprite_sheet4; //Help

    public static BufferedImage sprite_sheet5; //Updates

    public static BufferedImage sprite_sheet6; //About

    public static BufferedImage sprite_sheet7; //English

    public static BufferedImage sprite_sheet8; //Nederlands

    public static BufferedImage sprite_sheet9; //Deutsch


    public Game() {

        BufferedImageLoader loader = new BufferedImageLoader();
        sprite_sheet = loader.loadImage("MainPage.png");

        BufferedImageLoader2 loader2 = new BufferedImageLoader2();
        sprite_sheet2 = loader2.loadImage("PlayPage.png"); //BG

        BufferedImageLoader3 loader3 = new BufferedImageLoader3();
        sprite_sheet3 = loader3.loadImage("InfoPage.png");

        BufferedImageLoader4 loader4 = new BufferedImageLoader4();
        sprite_sheet4 = loader4.loadImage("HelpPage.png");

        BufferedImageLoader5 loader5 = new BufferedImageLoader5();
        sprite_sheet5 = loader5.loadImage("UpdatesPage.png");

        BufferedImageLoader6 loader6 = new BufferedImageLoader6();
        sprite_sheet6 = loader6.loadImage("AboutPage.png");

        BufferedImageLoader7 loader7 = new BufferedImageLoader7();
        sprite_sheet7 = loader7.loadImage("HelpEnglish.png");

        BufferedImageLoader8 loader8 = new BufferedImageLoader8();
        sprite_sheet8 = loader8.loadImage("HelpNederlands.png");

        BufferedImageLoader9 loader9 = new BufferedImageLoader9();
        sprite_sheet9 = loader9.loadImage("HelpDeutsch.png");

        //SpriteSheet Main image (background/menu)
        SpriteSheet ss = new SpriteSheet(Game.sprite_sheet);

        main_image = ss.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        System.out.println("\n");
        System.out.println("Loading game...");//1
        System.out.println("Game loaded!");
        System.out.println(" ");

        handler = new Handler();
        hud = new HUD();
        shop = new Shop(handler, hud);
        menu = new Menu(this, handler, hud);
        this.addKeyListener(new KeyInput(handler, this));
        this.addMouseListener(menu);
        this.addMouseListener(shop);

        //AudioPlayer.load();
        //AudioPlayer.getMusic1("game_music").loop(1, 0.15f);

        //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        new Window(1280, 720, "Dotch. - [BETA] [TEST VERSION]", this);

        spawner = new Spawn(handler, hud, this);

        r = new Random();

        if (gameState == STATE.Game) {
            hud.setLevel(1);
            hud.setScore(0);
            handler.addObject(new Player(WIDTH / 2 - 32, HEIGHT / 2 - 32, ID.Player, handler));
            handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
        }
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

        //FPS Check
        long lastTimeChecked = System.nanoTime();
        int frames = 0;

        this.requestFocus();
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames1 = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                delta--;
            }

            if (running)
                render();
            frames1++;
            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                System.out.println("FPS: " + frames1);
                frames1 = 0;
            }

            frames++;
            if (System.nanoTime() - lastTimeChecked >= 1000000000) {
                fps = frames;
                frames = 0;
                lastTimeChecked = System.nanoTime();
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
                hud.bounds = 0;
                HUD.HEALTH = 100;
                B1 = 1000;
                B2 = 750;
                B3 = 750;
                gameState = STATE.End;
                {
                    handler.clearEnemys();
                    hud.setLevel(1);
                    hud.setPoints(0);
                    for (int i = 0; i < 15; i++) {
                        handler.addObject(new MenuParticle(r.nextInt(WIDTH), r.nextInt(HEIGHT), ID.MenuParticle, handler));
                    }
                    //gameState = STATE.Select;

                    //hud.bounds = 0;
                    //gameState = STATE.Select; //toggle on (with '//') for no retry, but select-menu
                    //with out '//', -> try again button
                }
            }

        } else if (gameState == STATE.End) {
            menu.tick();
            handler.tick();

            B1 = 1000;
            B2 = 750;
            B3 = 750;

        } else if (gameState == STATE.Menu || gameState == STATE.Select) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.Help) {
            menu.tick();
            handler.tick();
            gameState = STATE.Help;
        }
        if (gameState == STATE.HelpDEU) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.HelpENG) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.HelpFRA) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.HelpNLD) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.HelpRUS) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.HelpSPA) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.Info) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.About) {
            menu.tick();
            handler.tick();
        }
        if (gameState == STATE.Update_Notes) {
            menu.tick();
            handler.tick();
                	/*}if(gameState == STATE.Out_Of_Order) {
                		menu.tick();
                		handler.tick();*/
        }
    }

    private void render() {

        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();
        //if(gameState != STATE.MusicPlayer) {

        g.setColor(Color.black);
        g.fillRect(0, 0, WIDTH, HEIGHT);


        if (gameState == STATE.Menu) {

            int x0 = 0;    //width
            int y0 = 0;    //height
            g.drawImage(main_image, (int) x0, (int) y0, null);

        }

        //}else if(gameState == STATE.MusicPlayer) {
        //g.setColor(Color.black);
        //g.fillRect(0, 0, WIDTH, HEIGHT);
        //}

        //FPS Counter
        if (fps >= 120 && fps <= 240) {
            g.setColor(Color.gray);
            g.setFont(new Font("arial", Font.PLAIN, 20));
            g.drawString("FPS: " + fps, 1160, 19);
            //g.drawString("Good", 1140, 45);
        } else if (fps >= 60 && fps <= 120) {
            g.setColor(Color.gray);
            g.setFont(new Font("arial", Font.PLAIN, 20));
            g.drawString("FPS: " + fps, 1160, 19);
            //g.drawString("Middle", 1140, 45);
        } else if (fps <= 60) {
            g.setColor(Color.gray);
            g.setFont(new Font("arial", Font.PLAIN, 20));
            g.drawString("FPS: " + fps, 1160, 19);
            //g.drawString("Bad", 1140, 45);
        } else if (fps >= 240) {
            g.setColor(Color.green);
            g.setFont(new Font("arial", Font.PLAIN, 20));
            g.drawString("FPS: " + fps, 1160, 19);
            //g.drawString("Very Good", 1140, 45);
        } else if (fps >= 0 && fps <= 60) {
            g.setColor(Color.gray);
            g.setFont(new Font("arial", Font.PLAIN, 20));
            g.drawString("FPS: " + fps, 1160, 19);
            //g.drawString("Very Bad", 1140, 45);
        }

        //W-A-Sw-D Touchscreen buttons
        //if(gameState == STATE.Game) {
        //g.setColor(Color.red);
        //g.drawRect(500, 500, 50, 50);
        //g.fillRect(50q0, 500, 50, 50);

        //}

        if (paused) {
            Font fnt = new Font("arial", 1, 125);

            g.setFont(fnt);
            g.setColor(Color.orange);
            g.drawString("PAUSED", (int) (1280 / 2 - 225), 720 / 2);
            g.setColor(Color.orange);
            //g.drawString("\u26A0", (int) (1280 / 2 - 400), 720 / 2); //Uitroepteken Symbool (update de Unicode!)
            //g.drawString("\u26A0", (int) (1280 / 2 + 350), 720 / 2); //Uitroepteken Symbool (update de Unicode!)
        }

        if (gameState == STATE.Game) {
            hud.render(g);
            handler.render(g);
        } else if (gameState == STATE.Shop) {
            shop.render(g);
        } else if (gameState == STATE.Menu) {
            menu.render(g);
            //handler.render(g); //causes the crashing error in Thread 2 - Handler, LinkedList
        } else if (gameState == STATE.End || gameState == STATE.Select) {
            menu.render(g);
            //handler.render(g);
        } else if (gameState == STATE.HelpNLD || gameState == STATE.HelpENG || gameState == STATE.HelpFRA || gameState == STATE.HelpDEU || gameState == STATE.HelpSPA || gameState == STATE.HelpRUS) {
            menu.render(g);
            handler.render(g);
        } else if (gameState == STATE.Help) {
            menu.render(g);
            handler.render(g);
        } else if (gameState == STATE.Info) {
            menu.render(g);
            handler.render(g);
        } else if (gameState == STATE.About) {
            menu.render(g);
            handler.render(g);
        } else if (gameState == STATE.Update_Notes) {
            menu.render(g);
            handler.render(g);
            //}else if(gameState == STATE.Out_Of_Order) {
            //menu.render(g);
        } else if (gameState == STATE.Credits) {
            menu.render(g);
        }
        g.dispose();
        bs.show();
    }

    public void mouseReleased(MouseEvent e) {

    }

    public static float clamp(float var, float min, float max) {
        if (var >= max)
            return var = max;
        else if (var < min)
            return var = min;
        else
            return var;
    }

    public static void main(String args[]) {
        new Game();

        System.out.println("All Loaded;");
        System.out.println("Game Running...");
    }
}