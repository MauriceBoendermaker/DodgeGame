import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
//import java.awt.image.BufferedImage;
import java.awt.Toolkit;

public class FastEnemy extends GameObject{
	
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		//private BufferedImage enemy_image;
		
	    private Handler handler;
        public FastEnemy(int x, int y, ID id, Handler handler){
                super(x,y,id);
                this.handler = handler;
                
                velX = 2;
                velY = 9;
                
                //SpriteSheet ss = new SpriteSheet(Game.sprite_sheet);
                
                //enemy_image = ss.grabImage(1, 3, 16, 16);
        }
        public Rectangle getBounds(){
                return new Rectangle((int) x,(int) y, 32, 32);
        }
        
        public void tick(){
                x += velX;
                y += velY;     
                if(y <= 0 || y >= 768 - 32) velY *= -1;
                if(x <= 0 || x >= 1366 - 16) velX *= -1;
                handler.addObject(new Trail((int)x,(int) y, ID.Trail, Color.CYAN, 32, 32, 0.02f,handler));
        }
        
        public void render(Graphics g){
        		g.setColor(Color.cyan);
        		g.fillRect((int)x, (int)y, 32, 32);
        		//g.drawImage(enemy_image, (int)x, (int)y, null);
        }
}