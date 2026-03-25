import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Menu extends MouseAdapter {

    private Game game;
    private Handler handler;
    private HUD hud;

    private BufferedImage play_image;
    private BufferedImage info_image;
    private BufferedImage help_image;
    private BufferedImage english_image;
    private BufferedImage nederlands_image;
    private BufferedImage deutsch_image;

    private static final Font FONT_SMALL = new Font("Arial", Font.BOLD, 20);
    private static final Font FONT_MEDIUM = new Font("Arial", Font.BOLD, 30);
    private static final Font FONT_LARGE = new Font("Arial", Font.BOLD, 50);

    public Menu(Game game, Handler handler, HUD hud) {
        this.game = game;
        this.hud = hud;
        this.handler = handler;

        play_image = new SpriteSheet(Game.sprite_sheet2).grabImage(1, 1, 720, 1280);
        info_image = new SpriteSheet(Game.sprite_sheet3).grabImage(1, 1, 720, 1280);
        help_image = new SpriteSheet(Game.sprite_sheet4).grabImage(1, 1, 720, 1280);
        english_image = new SpriteSheet(Game.sprite_sheet7).grabImage(1, 1, 720, 1280);
        nederlands_image = new SpriteSheet(Game.sprite_sheet8).grabImage(1, 1, 720, 1280);
        deutsch_image = new SpriteSheet(Game.sprite_sheet9).grabImage(1, 1, 720, 1280);
    }

    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        if (Game.gameState == Game.STATE.Menu) {
            if (mouseOver(mx, my, 47, 344, 232, 66)) {
                Game.gameState = Game.STATE.Select;
                return;
            }
            if (mouseOver(mx, my, 46, 444, 232, 66)) {
                Game.gameState = Game.STATE.Info;
                return;
            }
            if (mouseOver(mx, my, 46, 542, 232, 66)) {
                Game.gameState = Game.STATE.Help;
                return;
            }
            if (mouseOver(mx, my, 10, 590, 280, 64)) {
                Game.gameState = Game.STATE.About;
                return;
            }
            if (mouseOver(mx, my, 10, 665, 280, 64)) {
                Game.gameState = Game.STATE.Update_Notes;
                return;
            }
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                System.exit(0);
            }
            return;
        }

        if (Game.gameState == Game.STATE.Select) {
            // Normal
            if (mouseOver(mx, my, 47, 335, 232, 66)) {
                startGame(0, new BasicEnemy(Game.WIDTH - 50, Game.HEIGHT - 50, ID.BasicEnemy, handler));
                return;
            }
            // Hard
            if (mouseOver(mx, my, 46, 435, 232, 66)) {
                startGame(1, new HardEnemy(Game.WIDTH - 100, Game.HEIGHT - 100, ID.BasicEnemy, handler));
                return;
            }
            // Insane
            if (mouseOver(mx, my, 46, 533, 232, 66)) {
                startGame(2, new HardEnemy(Game.WIDTH - 50, Game.HEIGHT - 50, ID.BasicEnemy, handler));
                return;
            }
            // Back
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Menu;
                return;
            }
            return;
        }

        if (Game.gameState == Game.STATE.Help) {
            if (mouseOver(mx, my, 47, 335, 330, 66)) {
                Game.gameState = Game.STATE.HelpENG;
                return;
            }
            if (mouseOver(mx, my, 46, 435, 330, 66)) {
                Game.gameState = Game.STATE.HelpNLD;
                return;
            }
            if (mouseOver(mx, my, 46, 533, 330, 66)) {
                Game.gameState = Game.STATE.HelpDEU;
                return;
            }
            if (mouseOver(mx, my, 10, 250, 330, 66)) {
                Game.gameState = Game.STATE.HelpFRA;
                return;
            }
            if (mouseOver(mx, my, 424, 175, 330, 66)) {
                Game.gameState = Game.STATE.HelpRUS;
                return;
            }
            if (mouseOver(mx, my, 424, 250, 330, 66)) {
                Game.gameState = Game.STATE.HelpSPA;
                return;
            }
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Menu;
                return;
            }
            return;
        }

        // Back buttons for help language pages → go back to Help
        if (Game.gameState == Game.STATE.HelpNLD || Game.gameState == Game.STATE.HelpENG
                || Game.gameState == Game.STATE.HelpFRA || Game.gameState == Game.STATE.HelpDEU
                || Game.gameState == Game.STATE.HelpRUS || Game.gameState == Game.STATE.HelpSPA) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Help;
                return;
            }
            return;
        }

        // Back buttons for Info, About, Update_Notes → go back to Menu
        if (Game.gameState == Game.STATE.Info || Game.gameState == Game.STATE.About
                || Game.gameState == Game.STATE.Update_Notes) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Menu;
                return;
            }
            return;
        }

        // Back button for End → go back to Select
        if (Game.gameState == Game.STATE.End) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Select;
                hud.setLevel(1);
                hud.setScore(0);
                hud.bounds = 0;
                return;
            }
        }
    }

    private void startGame(int difficulty, GameObject firstEnemy) {
        Game.gameState = Game.STATE.Game;
        handler.addObject(new Player(Game.WIDTH / 2 - 32, Game.HEIGHT / 2 - 32, ID.Player, handler));
        handler.clearEnemys();
        handler.addObject(firstEnemy);
        game.diff = difficulty;
    }

    private boolean mouseOver(int mx, int my, int x, int y, int width, int height) {
        return mx > x && mx < x + width && my > y && my < y + height;
    }

    public void tick() {
    }

    public void render(Graphics g) {
        if (Game.gameState == Game.STATE.Menu) {
            g.setColor(Color.white);
            g.setFont(FONT_SMALL);
            g.drawString("Current state of build: 25 Levels", 960, 680);

        } else if (Game.gameState == Game.STATE.Select) {
            g.drawImage(play_image, 0, 0, null);

        } else if (Game.gameState == Game.STATE.Help) {
            g.drawImage(help_image, 0, 0, null);

        } else if (Game.gameState == Game.STATE.HelpENG) {
            g.drawImage(english_image, 0, 0, null);

        } else if (Game.gameState == Game.STATE.HelpNLD) {
            g.drawImage(nederlands_image, 0, 0, null);

        } else if (Game.gameState == Game.STATE.HelpDEU) {
            g.drawImage(deutsch_image, 0, 0, null);

        } else if (Game.gameState == Game.STATE.Info) {
            g.drawImage(info_image, 0, 0, null);

        } else if (Game.gameState == Game.STATE.End) {
            g.setFont(FONT_LARGE);
            g.setColor(Color.white);
            g.drawString("Game Over!", 175, 70);

            g.setFont(FONT_MEDIUM);
            g.drawString("You lost with a score of: " + hud.getScore(), 175, 180);

            g.drawRect(210, 350, 200, 64);
            g.drawString("Try Again >>", 222, 385);
        }
    }
}
