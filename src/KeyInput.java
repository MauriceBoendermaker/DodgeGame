import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyInput extends KeyAdapter {

    private Handler handler;

    public KeyInput(Handler handler, Game game) {
        this.handler = handler;
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (Game.gameState == Game.STATE.Game) {
            Player player = findPlayer();
            if (player != null) {
                if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) player.moveUp = true;
                if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) player.moveDown = true;
                if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) player.moveRight = true;
                if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) player.moveLeft = true;
                if (key == KeyEvent.VK_SHIFT) player.dashInput = true;
                if (key == KeyEvent.VK_E) player.slowmoInput = true;
            }
        }

        // ESC — context-sensitive
        if (key == KeyEvent.VK_ESCAPE) {
            switch (Game.gameState) {
                case Game:
                case Shop:
                    Game.pausedFrom = Game.gameState;
                    Game.gameState = Game.STATE.Paused;
                    break;
                case Paused:
                    Game.gameState = Game.pausedFrom;
                    break;
                case Select:
                case About:
                case Update_Notes:
                case Credits:
                case MusicPlayer:
                case Statistics:
                case AchievementsPage:
                case Customize:
                case CoinShopPage:
                case DailyPage:
                    Game.gameState = Game.STATE.Menu;
                    break;
                case Loadout:
                    Game.gameState = Game.STATE.Select;
                    break;
                case Settings:
                    Game.gameState = (Menu.settingsReturnTo != null) ? Menu.settingsReturnTo : Game.STATE.Menu;
                    Menu.settingsReturnTo = null;
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

        if (key == KeyEvent.VK_P) {
            if (Game.gameState == Game.STATE.Game) {
                Game.pausedFrom = Game.STATE.Game;
                Game.gameState = Game.STATE.Paused;
            } else if (Game.gameState == Game.STATE.Paused) {
                Game.gameState = Game.pausedFrom;
            }
        }

        if (key == KeyEvent.VK_SPACE) {
            if (Game.gameState == Game.STATE.Game) Game.gameState = Game.STATE.Shop;
            else if (Game.gameState == Game.STATE.Shop) Game.gameState = Game.STATE.Game;
        }
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        Player player = findPlayer();
        if (player != null) {
            if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) player.moveUp = false;
            if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) player.moveDown = false;
            if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) player.moveRight = false;
            if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) player.moveLeft = false;
        }
    }

    private Player findPlayer() {
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject obj = handler.getObjects().get(i);
            if (obj instanceof Player) return (Player) obj;
        }
        return null;
    }
}
