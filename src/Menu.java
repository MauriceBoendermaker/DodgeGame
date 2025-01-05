import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public class Menu extends MouseAdapter {

    boolean debug = false;

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    private Game game;
    private Handler handler;
    private HUD hud;
    private Random r = new Random();

    private BufferedImage main_image; //1

    private BufferedImage play_image; //2

    private BufferedImage info_image; //3

    private BufferedImage help_image; //4

    private BufferedImage updates_image; //5

    private BufferedImage about_image; //6

    private BufferedImage english_image; //7

    private BufferedImage nederlands_image; //8

    private BufferedImage deutsch_image; //9

    public int diff = 1;


    public Menu(Game game, Handler handler, HUD hud) {
        this.game = game;
        this.hud = hud;
        this.handler = handler;

        //AudioPlayer.load();

        //SpriteSheet Main Image
        SpriteSheet ss = new SpriteSheet(Game.sprite_sheet);
        main_image = ss.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        //SpriteSheet Play Image
        SpriteSheet2 ss2 = new SpriteSheet2(Game.sprite_sheet2);
        play_image = ss2.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        //SpriteSheet Info Image
        SpriteSheet3 ss3 = new SpriteSheet3(Game.sprite_sheet3);
        info_image = ss3.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        //SpriteSheet Help Image
        SpriteSheet4 ss4 = new SpriteSheet4(Game.sprite_sheet4);
        help_image = ss4.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        //SpriteSheet Updates Image
        SpriteSheet5 ss5 = new SpriteSheet5(Game.sprite_sheet5);
        updates_image = ss5.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        //SpriteSheet About Image
        SpriteSheet6 ss6 = new SpriteSheet6(Game.sprite_sheet6);
        about_image = ss6.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        //SpriteSheet English Image
        SpriteSheet7 ss7 = new SpriteSheet7(Game.sprite_sheet7);
        english_image = ss7.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        //SpriteSheet Nederlands Image
        SpriteSheet8 ss8 = new SpriteSheet8(Game.sprite_sheet8);
        nederlands_image = ss8.grabImage(1, 1, 720, 1280); //row = left to right || col = down

        //SpriteSheet Deutsch Image
        SpriteSheet9 ss9 = new SpriteSheet9(Game.sprite_sheet9);
        deutsch_image = ss9.grabImage(1, 1, 720, 1280); //row = left to right || col = down
    }

    public void mousePressed(MouseEvent e) {

        int mx = e.getX();
        int my = e.getY();

        if (Game.gameState == Game.STATE.Menu) {

            //play button
            if (mouseOver(mx, my, 47, 344, 232, 66)) {
                Game.gameState = Game.STATE.Select;
                System.out.println("PLAYbttn");
                //AudioPlayer.getSound("click_sound").play();
                return;
            }

            //Info button
            if (Game.gameState == Game.STATE.Menu) {
                if (mouseOver(mx, my, 46, 444, 232, 66)) {
                    Game.gameState = Game.STATE.Info;
                    System.out.println("INFObttn");
                    //AudioPlayer.getSound("click_sound").play();
                    return; //remove if program contain bugs
                }
            }

            //help button
            if (Game.gameState == Game.STATE.Menu) {
                if (mouseOver(mx, my, 46, 542, 232, 66)) {
                    Game.gameState = Game.STATE.Help;
                    System.out.println("HELPbttn");
                    //AudioPlayer.getSound("click_sound").play();
                    return; //remove if program contain bugs
                }
            }


            //quit button
            if (Game.gameState == Game.STATE.Menu) {
                if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                    System.out.println("QUITbttn");
                    //AudioPlayer.getSound("click_sound").play();
                    System.exit(1);
                }
            }
        }//end of Menu

        //normal button
        if (Game.gameState == Game.STATE.Select) {
            if (mouseOver(mx, my, 47, 335, 232, 66)) {
                Game.gameState = Game.STATE.Game;
                handler.addObject(new Player(1280 / 2 - 32, 720 / 2 - 32, ID.Player, handler));
                handler.clearEnemys();
                handler.addObject(new BasicEnemy(1280 - 50, 720 - 50, ID.BasicEnemy, handler));

                game.diff = 0;

                //AudioPlayer.getSound("click_sound").play();
                return; //remove if program contain bugs
            }
        }

        //hard button
        if (Game.gameState == Game.STATE.Select) {
            if (mouseOver(mx, my, 46, 435, 232, 66)) {
                Game.gameState = Game.STATE.Game;
                handler.addObject(new Player(1280 / 2 - 32, 720 / 2 - 32, ID.Player, handler));
                handler.clearEnemys();
                handler.addObject(new HardEnemy(1280 - 100, 720 - 100, ID.BasicEnemy, handler));

                game.diff = 1;

                //AudioPlayer.getSound("click_sound").play();
                return; //remove if program contain bugs
            }
        }

        //insane button
        if (Game.gameState == Game.STATE.Select) {
            if (mouseOver(mx, my, 46, 533, 232, 66)) {
                Game.gameState = Game.STATE.Game;
                handler.addObject(new Player(1280 / 2 - 32, 720 / 2 - 32, ID.Player, handler));
                handler.clearEnemys();
                handler.addObject(new HardEnemy(1280 - 50, 720 - 50, ID.BasicEnemy, handler));

                game.diff = 2;

                //AudioPlayer.getSound("click_sound").play();
                return; //remove if program contain bugs
            }
        }
			
			/*
			//no-enemy's button
			if(Game.gameState == Game.STATE.Select){
			if(mouseOver(mx, my, 1280 / 2 - 500, 120, 300, 100)){		
				Game.gameState = STATE.Game;
				handler.addObject(new Player(1280/2-32, 720/2-32, ID.Player, handler));
				handler.clearEnemys();
				
				game.diff = 3;
				
				//AudioPlayer.getSound("click_sound").play();
				
				return; //remove if program contain bugs
			}
		}
			
			//no-player button
			if(Game.gameState == Game.STATE.Select){
			if(mouseOver(mx, my, 1280 / 2 - 500, 240, 300, 100)){
				handler.clearEnemys();
				handler.addObject(new BasicEnemy(1280 - 50, 720 - 50, ID.BasicEnemy, handler));
				Game.gameState = STATE.Game;
				
				game.diff = 4;
				
				//AudioPlayer.getSound("click_sound").play();
				
				return; //remove if program contain bugs
			}
		}
		*/

        //back button for Select
        if (Game.gameState == Game.STATE.Select) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Menu;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for Out Of Order
			/*if(Game.gameState == Game.STATE.Out_Of_Order){
			if(mouseOver(mx, my, 210, 350, 200, 64)){
				Game.gameState = STATE.Select;
				AudioPlayer.getSound("click_sound").play();
				return;
			}
			}*/

        if (Game.gameState == Game.STATE.Menu) {
            //About button
            if (mouseOver(mx, my, 10, 590, 280, 64)) {
                Game.gameState = Game.STATE.About;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for About
        if (Game.gameState == Game.STATE.About) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Menu;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }
			
		/*if(Game.gameState == Game.STATE.Menu){
			//Info button
			if(mouseOver(mx, my, 46, 444, 232, 66)){
				Game.gameState = STATE.Info;
				//AudioPlayer.getSound("click_sound").play();
				return;
			}
		}*/

        //back button for Info
        if (Game.gameState == Game.STATE.Info) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Menu;
                System.out.println("INFOback");
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //Update Notes button
        if (Game.gameState == Game.STATE.Menu) {
            if (mouseOver(mx, my, 10, 665, 280, 64)) {
                Game.gameState = Game.STATE.Update_Notes;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for Update Notes
        if (Game.gameState == Game.STATE.Update_Notes) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Menu;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }


        //all the other back buttons
        //back button for help
        if (Game.gameState == Game.STATE.Help) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Menu;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //Nederlands button for help
        if (Game.gameState == Game.STATE.Help) {
            if (mouseOver(mx, my, 46, 435, 330, 66)) {
                Game.gameState = Game.STATE.HelpNLD;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for NL
        if (Game.gameState == Game.STATE.HelpNLD) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Help;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //English button for help
        if (Game.gameState == Game.STATE.Help) {
            if (mouseOver(mx, my, 47, 335, 330, 66)) {
                Game.gameState = Game.STATE.HelpENG;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for EN
        if (Game.gameState == Game.STATE.HelpENG) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Help;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //French button for help
        if (Game.gameState == Game.STATE.Help) {
            if (mouseOver(mx, my, 10, 250, 330, 66)) {
                Game.gameState = Game.STATE.HelpFRA;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for FR
        if (Game.gameState == Game.STATE.HelpFRA) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Help;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //Deutsch button for help
        if (Game.gameState == Game.STATE.Help) {
            if (mouseOver(mx, my, 46, 533, 330, 66)) {
                Game.gameState = Game.STATE.HelpDEU;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for DE
        if (Game.gameState == Game.STATE.HelpDEU) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Help;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //Russian button for help
        if (Game.gameState == Game.STATE.Help) {
            if (mouseOver(mx, my, 424, 175, 330, 66)) {
                Game.gameState = Game.STATE.HelpRUS;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for RU
        if (Game.gameState == Game.STATE.HelpRUS) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Help;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //Spanish button for help
        if (Game.gameState == Game.STATE.Help) {
            if (mouseOver(mx, my, 424, 250, 330, 66)) {
                Game.gameState = Game.STATE.HelpSPA;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }

        //back button for SPA
        if (Game.gameState == Game.STATE.HelpSPA) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Help;
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }


        //back button for end
        if (Game.gameState == Game.STATE.End) {
            if (mouseOver(mx, my, 1145, 52, 100, 33)) {
                Game.gameState = Game.STATE.Select;
                hud.setLevel(1);
                hud.setScore(0);
                hud.bounds = 0;

                //handler.addObject(new Player(1366/2-32, 768/2-32, ID.Player, handler));
                //handler.clearEnemys();
                //handler.addObject(new BasicEnemy(1366 - 50, 768 - 50, ID.BasicEnemy, handler));
                //AudioPlayer.getSound("click_sound").play();
                return;
            }
        }
    }


    public void mouseReleased(MouseEvent e) {

    }

    private boolean mouseOver(int mx, int my, int x, int y, int width, int height) {
        if (mx > x && mx < x + width) {
            if (my > y && my < y + height) {
                return true;
            } else return false;
        } else return false;
    }

    public void tick() {

    }

    public void render(Graphics g) {

        //font loading
		/*try{
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/game_over.ttf")));
		}catch(Exception e){
			e.printStackTrace();
		}
		
		//font loading
		try{
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/cubic.ttf")));
		}catch(Exception e){
			e.printStackTrace();
		}*/

        if (Game.gameState == Game.STATE.Menu) {

            Font fnt = new Font("arial", 1, 80);
            Font fnt2 = new Font("arial", 1, 50);
            Font fnt3 = new Font("arial", 1, 40);
            Font fnt4 = new Font("arial", 1, 125);
            Font fnt5 = new Font("arial", 1, 275);
            Font fnt6 = new Font("arial", 1, 350);
            Font fnt7 = new Font("arial", 1, 25);
            Font fnt8 = new Font("arial", 1, 20);
            Font fnt9 = new Font("arial", 1, 17);
			
			/*
			g.setFont(fnt8);
			g.setColor(Color.white);
			g.drawString("Current Build:", 5, 20);
			g.setFont(fnt7);
			g.setColor(Color.white);
			g.drawString("BETA Version", 5, 45);
			g.setFont(fnt9);
			g.setColor(new Color(255, 255, 255, 120));
			g.drawString("*Can Contain Bugs!", 5, 65);
			*/


            g.setColor(Color.white);
            g.setFont(fnt8);
            g.drawString("Current state of build: 25 Levels", 960, 680);


            int x = 1280;    //width
            int y = 720;    //height
            g.drawImage(main_image, (int) x, (int) y, null);
			
			/*g.setFont(fnt);
			g.setColor(Color.white);
			g.drawString(" - Menu - ", 1280 / 3 + 50, 200); //FAQ / Help / Quit / Play / About / Update Notes*/

        } else if (Game.gameState == Game.STATE.Help) {

            int x4 = 0;    //width
            int y4 = 0;    //height
            g.drawImage(help_image, (int) x4, (int) y4, null);

        } else if (Game.gameState == Game.STATE.HelpNLD) {

            int x8 = 0;    //width
            int y8 = 0;    //height
            g.drawImage(nederlands_image, (int) x8, (int) y8, null);

        } else if (Game.gameState == Game.STATE.HelpDEU) {

            int x9 = 0;    //width
            int y9 = 0;    //height
            g.drawImage(deutsch_image, (int) x9, (int) y9, null);

        } else if (Game.gameState == Game.STATE.HelpENG) {

            int x7 = 0;    //width
            int y7 = 0;    //height
            g.drawImage(english_image, (int) x7, (int) y7, null);

        } else if (Game.gameState == Game.STATE.End) {
            Font fnt = new Font("arial", 1, 50);
            Font fnt2 = new Font("arial", 1, 30);
            Font fnt3 = new Font("arial", 1, 13);

            g.setFont(fnt);
            g.setColor(Color.white);
            g.drawString("Game Over!", 175, 70);

            g.setFont(fnt2);
            g.setColor(Color.white);
            g.drawString("You lost with a score of: " + hud.getScore(), 175, 180);

            g.setFont(fnt2);
            g.drawRect(210, 350, 200, 64);
            g.drawString("Try Again >>", 222, 385);
        } else if (Game.gameState == Game.STATE.Select) {

            int x2 = 0;    //width
            int y2 = 0;    //height
            g.drawImage(play_image, (int) x2, (int) y2, null);
			
			/*
			 * old version
			Font fnt = new Font("arial", 1, 50);
			Font fnt2 = new Font("arial", 1, 30);
			Font fnt3 = new Font("arial", 1, 13);
			Font fnt4 = new Font("arial", 1, 20);
			Font fnt5 = new Font("arial", 1, 40);
		
			g.setFont(fnt);
			g.setColor(Color.white);
			g.drawString(" - Select Difficulty - ", 1280 / 2 - 235, 55);
			
			
			g.setFont(fnt);
			g.setColor(Color.green);
			g.drawRect(1280 / 2 - (300 / 2), 120, 300, 100);
			g.setColor(Color.darkGray);
			g.fillRect(1280 / 2 - (299 / 2), 121, 299, 99);
			g.setColor(Color.white);
			g.drawString("Normal", 552, 175);
			g.setFont(fnt4);
			g.drawString("(Are You a Pussy?!)", 545, 205);

			g.setFont(fnt);
			g.setColor(Color.orange);
			g.drawRect(1280 / 2 - (300 / 2), 240, 300, 100);
			g.setColor(Color.darkGray);
			g.fillRect(1280 / 2 - (299 / 2), 241, 299, 99);
			g.setColor(Color.white);
			g.drawString("Hard", 580, 295);
			g.setFont(fnt4);
			g.drawString("(Like a Real Pro!)", 558, 325);
			
			g.setFont(fnt);
			g.setColor(Color.red) ;
			g.drawRect(1280 / 2 - (300 / 2), 360, 300, 100);
			g.setColor(Color.darkGray);
			g.fillRect(1280 / 2 - (299 / 2), 361, 299, 99);
			g.setColor(Color.white);
			g.drawString("Insane", 560, 415);
			g.setFont(fnt4);
			g.drawString("(Only for the Best!)", 548, 445);
			
			g.setFont(fnt5);
			g.drawString("> Experimental:", 140, 110);
			
			g.setFont(fnt5);
			g.drawString("> Original:", 490, 110);		
			
			g.setFont(fnt);
			g.setColor(Color.white) ;
			g.drawRect(1280 / 2 - 500, 120, 300, 100);
			g.setColor(Color.darkGray);
			g.fillRect(1280 / 2 - 499, 121, 299, 99);
			g.setColor(Color.white);
			g.drawString("N.E.M.", 215, 175);
			g.setFont(fnt4);
			g.drawString("(No Enemy Mode!)", 200, 205);
			
			g.setFont(fnt);
			g.setColor(Color.white) ;
			g.drawRect(1280 / 2 - 500, 240, 300, 100);
			g.setColor(Color.darkGray);
			g.fillRect(1280 / 2 - 499, 241, 299, 99);
			g.setColor(Color.white);
			g.drawString("N.P.M.", 215, 295);
			g.setFont(fnt4);
			g.drawString("(No Player Mode!)", 205, 325);
			*/

        } else if (Game.gameState == Game.STATE.About) {
            //
        } else if (Game.gameState == Game.STATE.Info) {

            int x3 = 0;    //width
            int y3 = 0;    //height
            g.drawImage(info_image, (int) x3, (int) y3, null);

        } else if (Game.gameState == Game.STATE.Update_Notes) {
            //
		/*}else if(Game.gameState == Game.STATE.Out_Of_Order) {
			
			Font fnt = new Font("arial", 1, 50);
			Font fnt2 = new Font("arial", 1, 30);
			
			g.setFont(fnt);
			g.setColor(Color.red);
			g.drawString(" - Error [404] Not Found - ", 1366 / 3 - 10, 70);
			
			g.setFont(fnt2);
			g.setColor(Color.red);
			g.drawString("Out Of Order!", 10, 150);
			g.setColor(Color.white);
			g.drawString("On this moment the page you where looking for is unavailable.", 10, 180);
			
			g.setColor(Color.white);
			g.setFont(fnt2);
			g.drawRect(210, 350, 200, 64);
			g.drawString("<< Back", 250, 395);*/
        }
        if (Game.gameState == Game.STATE.Credits) {
            //
        }
    }
}
