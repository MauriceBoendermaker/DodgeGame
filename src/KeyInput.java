import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyInput extends KeyAdapter {

	private Handler handler;
	private boolean[] KeyDown = new boolean[4];

	Game game;

	public KeyInput(Handler handler, Game game){
		this.handler = handler;

		this.game = game;

		KeyDown[0]=false;
		KeyDown[1]=false;
		KeyDown[2]=false;
		KeyDown[3]=false;
	}

	public void keyPressed(KeyEvent e){
		int key = e.getKeyCode();

		for(int i = 0; i < handler.object.size(); i++){
			GameObject tempObject = handler.object.get(i);

			if(tempObject.getId() == ID.Player){
				//key events for player 1

				//key events for W,A,S,D
				if(key == KeyEvent.VK_W) { tempObject.setVelY(-handler.spd); KeyDown[0]=true; }
				if(key == KeyEvent.VK_S) { tempObject.setVelY(handler.spd); KeyDown[1]=true; }
				if(key == KeyEvent.VK_D) { tempObject.setVelX(handler.spd); KeyDown[2]=true; }
				if(key == KeyEvent.VK_A) { tempObject.setVelX(-handler.spd); KeyDown[3]=true; }

				//key events for UP,DOWN,LEFT,RIGHT
				if(key == KeyEvent.VK_UP) { tempObject.setVelY(-handler.spd); KeyDown[0]=true; }
				if(key == KeyEvent.VK_DOWN) { tempObject.setVelY(handler.spd); KeyDown[1]=true; }
				if(key == KeyEvent.VK_RIGHT) { tempObject.setVelX(handler.spd); KeyDown[2]=true; }
				if(key == KeyEvent.VK_LEFT) { tempObject.setVelX(-handler.spd); KeyDown[3]=true; }
			}
		}

		if(key == KeyEvent.VK_P)
		{

			if(Game.gameState == Game.STATE.Game)
			{
				if(Game.paused) Game.paused = false;
				else Game.paused = true;
			}

		}
		if(key == KeyEvent.VK_ESCAPE) System.exit(1);
		if(key == KeyEvent.VK_SPACE){
			if(Game.gameState == Game.STATE.Game) Game.gameState = Game.STATE.Shop;
			else if(Game.gameState == Game.STATE.Shop) Game.gameState = Game.STATE.Game;
		}

	}

	public void keyReleased(KeyEvent e){
		int key = e.getKeyCode();

		for(int i = 0; i < handler.object.size(); i++){
			GameObject tempObject = handler.object.get(i);

			if(tempObject.getId() == ID.Player){
				//key events for player 1

				//key events for W,A,S,D
				if(key == KeyEvent.VK_W) KeyDown[0]=false;
				if(key == KeyEvent.VK_S) KeyDown[1]=false;
				if(key == KeyEvent.VK_D) KeyDown[2]=false;
				if(key == KeyEvent.VK_A) KeyDown[3]=false;

				//key events for UP,DOWN,LEFT,RIGHT
				if(key == KeyEvent.VK_UP) KeyDown[0]=false;
				if(key == KeyEvent.VK_DOWN) KeyDown[1]=false;
				if(key == KeyEvent.VK_RIGHT) KeyDown[2]=false;
				if(key == KeyEvent.VK_LEFT) KeyDown[3]=false;

			//vertical movement
			if(!KeyDown[0] && !KeyDown[1]) tempObject.setVelY(0);
			//horizontal movement
			if(!KeyDown[2] && !KeyDown[3]) tempObject.setVelX(0);
			}
		}

	}

}
