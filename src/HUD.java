import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;

public class HUD {
	
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
		public int bounds = 0;
        public static float HEALTH = 100;
        private float greenValue = 255;
        
        private int score = 0;
        private int level = 1;
        private int points = 0;
        
        public void tick(){
        	
                HEALTH = (int) Game.clamp(HEALTH, 0, 100+(bounds / 2));
                
                greenValue = HEALTH * 2; 
                greenValue = (int) Game.clamp(greenValue, 0, 255);   
                
                score++;
                
                points++;
        }
        
        public void render(Graphics g){
        		
        		Font fnt = new Font("arial", 1, 20);
        		Font fnt1 = new Font("arial", 1, 750);
        		Font fnt2 = new Font("arial", 1, 60);
        	
        		g.setFont(fnt);
                g.setColor(Color.gray);
                g.fillRect(15, 15, 200 + bounds, 32);
                
                g.setColor(new Color(75,(int) greenValue, 0));
                g.fillRect(15, 15,(int) HEALTH * 2, 32);
                
                g.setColor(Color.white);
                g.drawRect(15, 15, 200 + bounds, 32);
                
                g.drawString("Score: " + score, 15, 96);
                g.drawString("Points: " + points, 15, 80);
                g.drawString("Level: " + level, 15, 64);
                g.drawString("SpaceBar = Shop", 15, 120);
                g.drawString("'ESC' = Exit", 15, 136);
                g.drawString("'P' = Pause", 15, 162);
                
                if(level == 0 || level == 1 || level == 2 || level == 3 || level == 4 || level == 5 || level == 6 || level == 7 || level == 8 || level == 9) {
                g.setFont(fnt1);
                g.setColor(new Color(255, 255, 255, 75));
                g.drawString("0" + level, 1280 / 2 - 375, 768 - 150);
                }else if(level >= 10) {
            	g.setFont(fnt1);
                g.setColor(new Color(255, 255, 255, 75));
                g.drawString("" + level, 1280 / 2 - 375, 768 - 150);
                }
                
                g.setFont(fnt2);
                g.drawString("Score: " + score, 1280 / 2 - 100, 60);

        }
        
        public void setScore(int score){
                this.score = score;
        }
        
        public int getScore(){
                return score;
        }
        
        public int getLevel(){
                return level;
        }
        
        public void setLevel(int level){
                this.level = level;
        }
        
        public int getPoints(){
            return points;
        }
    
        public void setPoints(int points){
            this.points = points;
        }
}
