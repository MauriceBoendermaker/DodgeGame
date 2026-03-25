import java.util.Random;

public class Spawn {

    private Handler handler;
    private HUD hud;
    private Game game;
    private Random r = new Random();

    private int scoreKeep = 0;
    private boolean bossActive = false;

    public Spawn(Handler handler, HUD hud, Game game) {
        this.handler = handler;
        this.hud = hud;
        this.game = game;
    }

    public void tick() {
        // Check if boss has been defeated
        if (bossActive) {
            boolean bossStillAlive = false;
            for (int i = 0; i < handler.getObjects().size(); i++) {
                GameObject obj = handler.getObjects().get(i);
                if (obj instanceof EnemyBoss) {
                    EnemyBoss boss = (EnemyBoss) obj;
                    if (boss.isDefeated()) {
                        handler.removeObject(boss);
                        bossActive = false;
                        // Resume with fresh enemies after boss
                        handler.addObject(new BasicEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.BasicEnemy, handler));
                        handler.addObject(new FastEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.FastEnemy, handler));
                    } else {
                        bossStillAlive = true;
                    }
                }
            }
            if (bossStillAlive) return; // Pause level progression during boss fight
        }

        scoreKeep++;

        if (scoreKeep >= 250) {
            scoreKeep = 0;
            hud.setLevel(hud.getLevel() + 1);
            Game.triggerLevelUp();
            hud.triggerLevelUpBanner();

            if (game.diff == 0) {
                spawnNormalDifficulty();
            } else if (game.diff == 1) {
                spawnHardDifficulty();
            } else if (game.diff == 2) {
                spawnInsaneDifficulty();
            } else if (game.diff == 4) {
                spawnNoPlayerDifficulty();
            }
        }
    }

    private void spawnNormalDifficulty() {
        int level = hud.getLevel();
        switch (level) {
            case 2:
            case 3:
                handler.addObject(new BasicEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.BasicEnemy, handler));
                break;
            case 4:
            case 6:
            case 8:
                handler.addObject(new FastEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.FastEnemy, handler));
                break;
            case 5:
            case 25:
                handler.addObject(new SmartEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.SmartEnemy, handler));
                break;
            case 10:
                handler.clearEnemys();
                handler.addObject(new EnemyBoss((Game.WIDTH / 2) - 48, -120, ID.EnemyBoss, handler));
                bossActive = true;
                break;
            case 15:
                handler.clearEnemys();
                handler.addObject(new FastEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.FastEnemy, handler));
                break;
            case 17:
                handler.addObject(new FastEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.FastEnemy, handler));
                break;
            case 18:
            case 20:
            case 22:
                handler.addObject(new BasicEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.BasicEnemy, handler));
                break;
        }
    }

    private void spawnHardDifficulty() {
        int level = hud.getLevel();
        switch (level) {
            case 2:
            case 6:
                handler.addObject(new HardEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.HardEnemy, handler));
                break;
            case 3:
                handler.addObject(new SmartEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.SmartEnemy, handler));
                break;
            case 4:
            case 5:
                handler.addObject(new BasicEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.BasicEnemy, handler));
                break;
            case 15:
                handler.clearEnemys();
                handler.addObject(new EnemyBoss((Game.WIDTH / 2) - 48, -120, ID.EnemyBoss, handler));
                bossActive = true;
                break;
        }
    }

    private void spawnInsaneDifficulty() {
        int level = hud.getLevel();
        switch (level) {
            case 2:
            case 3:
            case 10:
                handler.addObject(new FastEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.FastEnemy, handler));
                break;
            case 4:
                handler.addObject(new SmartEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.SmartEnemy, handler));
                break;
            case 5:
            case 7:
            case 20:
                handler.addObject(new HardEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.HardEnemy, handler));
                break;
            case 8:
            case 12:
                handler.addObject(new BasicEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.BasicEnemy, handler));
                break;
            case 15:
                handler.clearEnemys();
                handler.addObject(new EnemyBoss((Game.WIDTH / 2) - 48, -120, ID.EnemyBoss, handler));
                bossActive = true;
                break;
        }
    }

    private void spawnNoPlayerDifficulty() {
        int level = hud.getLevel();
        switch (level) {
            case 2:
                handler.addObject(new FastEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.FastEnemy, handler));
                break;
            case 3:
                handler.addObject(new HardEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.HardEnemy, handler));
                break;
            case 4:
                handler.addObject(new SmartEnemy(r.nextInt(Game.WIDTH - 50), r.nextInt(Game.HEIGHT - 50), ID.SmartEnemy, handler));
                break;
        }
    }
}
