import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Random;

public class EnemyBossBullet extends GameObject{
	
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
        private Handler handler;
        Random r = new Random();
        
        public EnemyBossBullet(int x, int y, ID id, Handler handler){
                super(x,y,id);
                this.handler = handler;
                
                velX = (r.nextInt(5 - -5) + -5);
                velY = 5;
        }
        
        public Rectangle getBounds(){
                return new Rectangle((int) x,(int) y, 32, 32);
        }
        
        public void tick(){
        	
                x += velX;
                y += velY;     
                
                //if(y <= 0 || y >= Game.HEIGHT - 32) velY *= -1;
                //if(x <= 0 || x >= Game.WIDTH - 16) velX *= -1;
                
                if(y >= 768) handler.removeObject(this);
                
                handler.addObject(new Trail((int)x,(int) y, ID.Trail, Color.red, 32, 32, 0.02f,handler));
        }
        
        public void render(Graphics g){
                g.setColor(Color.red);
                g.fillRect((int)x,(int)y,32, 32);
        }
}