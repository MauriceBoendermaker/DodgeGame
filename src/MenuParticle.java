import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Random;

public class MenuParticle extends GameObject{
	
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
        private Handler handler;
        
        Random r = new Random();
        
        private Color col;
        
        public MenuParticle(int x, int y, ID id, Handler handler){
                super(x,y,id);
                this.handler = handler;
                
                velX = (r.nextInt(7 - -7) + -7);
                velY = (r.nextInt(7 - -7) + -7);
                if(velX == 0) velX = 1;
                if(velY == 0) velY = 1;
                
                col = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
        }
        
        public Rectangle getBounds(){
                return new Rectangle((int) x,(int) y, 16, 16);
        }
        
        public void tick(){
                x += velX;
                y += velY;     
                if(y <= 0 || y >= 768 - 32) velY *= -1;
                if(x <= 0 || x >= 1366 - 16) velX *= -1;
                handler.addObject(new Trail((int)x,(int) y, ID.Trail, col, 16, 16, 0.04f,handler));
        }
        
        public void render(Graphics g){
                g.setColor(col);
                g.fillRect((int)x,(int)y,16,16);
        }
}