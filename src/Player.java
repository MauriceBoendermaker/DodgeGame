import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
//import java.awt.image.BufferedImage;
import java.util.Random;

public class Player extends GameObject{
	
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
        Random r = new Random();
        Handler handler;
        
        //private BufferedImage player_image;
        
        public Player(int x,int y, ID id, Handler handler) {
                super(x, y, id);
                this.handler = handler;
                
                //SpriteSheet ss = new SpriteSheet(Game.sprite_sheet);
                
                //player_image = ss.grabImage(1, 1, 32, 32);
        }
        
        public Rectangle getBounds(){
                return new Rectangle((int) x,(int) y, 48, 48);
        }
        
        public void tick(){
                x += velX;
                y += velY;     
                x = Game.clamp(x, 0, 1280 -37);                          
                y = Game.clamp(y, 0, 720 - 60);
                
                handler.addObject(new Trail( x, y, ID.Trail, Color.white, 48, 48, 0.045f,handler));
                
                collision();
        }
        
        private void collision(){
        	
                for(int i = 0; i < handler.object.size(); i++){
                GameObject tempObject = handler.object.get(i);
                if(tempObject.getId() == ID.BasicEnemy || tempObject.getId() == ID.FastEnemy||tempObject.getId() == ID.SmartEnemy || tempObject.getId() == ID.EnemyBoss){
                                if(getBounds().intersects(tempObject.getBounds())){
                                        HUD.HEALTH -= 2;
                                }
                        }
                }
        }
        public void render(Graphics g){
                g.setColor(Color.white);
                g.fillRect((int)x, (int)y, 48, 48);
        		//g.drawImage(player_image, (int)x, (int)y, null);
        	}
        
}
