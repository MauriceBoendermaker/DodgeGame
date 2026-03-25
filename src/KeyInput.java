import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyInput extends KeyAdapter {

    private Handler handler;
    private boolean[] keyDown = new boolean[4];

    public KeyInput(Handler handler, Game game) {
        this.handler = handler;
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // Movement — only when actively playing
        if (Game.gameState == Game.STATE.Game) {
            for (int i = 0; i < handler.getObjects().size(); i++) {
                GameObject tempObject = handler.getObjects().get(i);
                if (tempObject.getId() == ID.Player) {
                    if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) {
                        tempObject.setVelY(-handler.spd);
                        keyDown[0] = true;
                    }
                    if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) {
                        tempObject.setVelY(handler.spd);
                        keyDown[1] = true;
                    }
                    if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) {
                        tempObject.setVelX(handler.spd);
                        keyDown[2] = true;
                    }
                    if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) {
                        tempObject.setVelX(-handler.spd);
                        keyDown[3] = true;
                    }
                }
            }
        }

        // ESC — context-sensitive
        if (key == KeyEvent.VK_ESCAPE) {
            switch (Game.gameState) {
                case Game:
                case Shop:
                    // Pause the game
                    Game.pausedFrom = Game.gameState;
                    Game.gameState = Game.STATE.Paused;
                    break;
                case Paused:
                    // Resume
                    Game.gameState = Game.pausedFrom;
                    break;
                case Select:
                case Info:
                case About:
                case Update_Notes:
                case Credits:
                case MusicPlayer:
                    Game.gameState = Game.STATE.Menu;
                    break;
                case Help:
                    Game.gameState = Game.STATE.Menu;
                    break;
                case HelpENG: case HelpNLD: case HelpDEU:
                case HelpFRA: case HelpRUS: case HelpSPA:
                    Game.gameState = Game.STATE.Help;
                    break;
                case End:
                    Game.gameState = Game.STATE.Menu;
                    break;
                default:
                    break;
            }
        }

        // P also toggles pause (legacy)
        if (key == KeyEvent.VK_P) {
            if (Game.gameState == Game.STATE.Game) {
                Game.pausedFrom = Game.STATE.Game;
                Game.gameState = Game.STATE.Paused;
            } else if (Game.gameState == Game.STATE.Paused) {
                Game.gameState = Game.pausedFrom;
            }
        }

        // Space — shop toggle (only when playing, not paused)
        if (key == KeyEvent.VK_SPACE) {
            if (Game.gameState == Game.STATE.Game) Game.gameState = Game.STATE.Shop;
            else if (Game.gameState == Game.STATE.Shop) Game.gameState = Game.STATE.Game;
        }
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject tempObject = handler.getObjects().get(i);
            if (tempObject.getId() == ID.Player) {
                if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) keyDown[0] = false;
                if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) keyDown[1] = false;
                if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) keyDown[2] = false;
                if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) keyDown[3] = false;

                if (!keyDown[0] && !keyDown[1]) tempObject.setVelY(0);
                if (!keyDown[2] && !keyDown[3]) tempObject.setVelX(0);
            }
        }
    }
}
