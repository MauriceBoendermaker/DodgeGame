import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Shop extends MouseAdapter {

    //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    Handler handler;
    HUD hud;

    private int B1 = 1000; //1000
    private int B2 = 750; //750
    private int B3 = 750; //750

    public Shop(Handler handler, HUD hud) {
        this.handler = handler;
        this.hud = hud;

    }

    public void render(Graphics g) {
        Font fnt = new Font("arial", 1, 25);

        g.setColor(Color.white);
        g.setFont(new Font("arial", 0, 50));
        g.drawString(" - Shop - ", 1280 / 2 - 100, 50);

        g.setFont(new Font("arial", 0, 25));
        g.drawString("Points to spend: " + hud.getPoints(), 1280 / 2 - 105, 75);

        //box 1 (B1)
        if (hud.getPoints() <= 1000) {
            g.setColor(Color.red);
            g.drawRect(1280 / 3 - 250, 720 / 4, 175, 100);

            g.setFont(new Font("arial", 0, 20));
            g.drawString("Upgrade Health", 1280 / 3 - 240, 225);
            g.drawString("Cost: " + B1, 1280 / 3 - 240, 255);
        } else if (hud.getPoints() >= 1000) {
            g.setColor(Color.green);
            g.drawRect(1280 / 3 - 250, 720 / 4, 175, 100);

            g.setFont(new Font("arial", 0, 20));
            g.drawString("Upgrade Health", 1280 / 3 - 240, 225);
            g.drawString("Cost: " + B1, 1280 / 3 - 240, 255);
        }


        //
		
		
		/*
		//box 1 (B1)
		//on 2nd try?
		if(hud.getPoints() <= 1000) {
			g.setColor(Color.red);
			g.drawRect(1280 / 3 - 250, 720 / 4, 175, 100);
			
			g.setFont(new Font("arial", 0, 20));
			g.drawString("Upgrade Health", 1280 / 3 - 240, 225);
			g.drawString("Cost: " + B1, 1280 / 3 - 240, 255);
		}else if(hud.getPoints() >= 1000) {
			g.setColor(Color.green);
			g.drawRect(1280 / 3 - 250, 720 / 4, 175, 100);
			
			g.setFont(new Font("arial", 0, 20));
			g.drawString("Upgrade Health", 1280 / 3 - 240, 225);
			g.drawString("Cost: " + B1, 1280 / 3 - 240, 255);
		}
		
		
		
		//
		*/


        //box 2 (B2)
        if (hud.getPoints() <= 750) {
            g.setColor(Color.red);
            g.drawRect(1280 / 3 + 145, 720 / 4, 175, 100);

            g.setFont(new Font("arial", 0, 20));
            g.drawString("Upgrade Speed", 1280 / 3 + 155, 225);
            g.drawString("Cost: " + B2, 1280 / 3 + 155, 255);
        } else if (hud.getPoints() >= 751) {
            g.setColor(Color.green);
            g.drawRect(1280 / 3 + 145, 720 / 4, 175, 100);

            g.setFont(new Font("arial", 0, 20));
            g.drawString("Upgrade Speed", 1280 / 3 + 155, 225);
            g.drawString("Cost: " + B2, 1280 / 3 + 155, 255);
        }

        //box 3 (B3)
        if (hud.getPoints() <= 750) {
            g.setColor(Color.red);
            g.drawRect(1280 / 3 + 595, 720 / 4, 175, 100);

            g.setFont(new Font("arial", 0, 20));
            g.drawString("Refill Health", 1280 / 3 + 605, 225);
            g.drawString("Cost: " + B3, 1280 / 3 + 605, 255);
        } else if (hud.getPoints() >= 750) {
            g.setColor(Color.green);
            g.drawRect(1280 / 3 + 595, 720 / 4, 175, 100);

            g.setFont(new Font("arial", 0, 20));
            g.drawString("Refill Health", 1280 / 3 + 605, 225);
            g.drawString("Cost: " + B3, 1280 / 3 + 605, 255);
        }
        g.setColor(Color.white);
        g.setFont(fnt);
        g.drawString("*Press SpaceBar to go back*", 1280 / 2 - 200 + 15, 720 - 100);

        g.setColor(Color.white);
        g.setFont(fnt);
        g.drawString("- Upgrade Health: Makes the HealthBar LONGER & REFILL Health!", 25, 350);
        g.drawString("- Upgrade Speed: Makes you move FASTER!", 25, 390);
        g.drawString("- Refill Health: Refill the COMPLETE HealthBar!", 25, 430);

        g.setColor(Color.yellow);
        g.setFont(fnt);
        g.drawString("(At Higher Levels, ONLY the Colors can be Bugged/Glitched!!)", 1280 / 2 - 325 + 5, 720 - 65);

    }

    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        //box 1
        if (mx >= 1280 / 3 - 250 && mx <= 1280 / 3 + 425) { //2nd "width" + 175
            if (my >= 720 / 4 && my <= 720 / 4 + 100) { //2nd "height" + 100
                //you've selected box 1
                System.out.println("Box 1");
                if (hud.getPoints() >= B1) {
                    hud.setPoints(hud.getPoints() - B1);
                    B1 += 250; //250
                    hud.bounds += 20;
                    hud.HEALTH = (100 + (hud.bounds / 2));
                }
            }
        }

        //box 2
        if (mx >= 1280 / 3 + 145 && mx <= 1280 / 3 + 320) { //2nd "width" + 175
            if (my >= 720 / 4 && my <= 720 / 4 + 100) { //2nd "height" + 100
                //you've selected box 2
                System.out.println("Box 2");
                if (hud.getPoints() >= B2) {
                    hud.setPoints(hud.getPoints() - B2);
                    B2 += 250; //250
                    handler.spd++;
                }
            }
        }

        //box 3
        if (mx >= 1280 / 3 + 595 && mx <= 1280 / 3 + 770) { //2nd "width" + 175
            if (my >= 720 / 4 && my <= 720 / 4 + 100) { //2nd "height" + 100
                //you've selected box 3
                System.out.println("Box 3");
                if (hud.getPoints() >= B3) {
                    hud.setPoints(hud.getPoints() - B3);
                    B3 += 250; //250
                    hud.HEALTH = (100 + (hud.bounds / 2));
                }
            }
        }

    }
}
