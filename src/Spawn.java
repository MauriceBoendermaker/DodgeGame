import java.awt.Color;
import java.util.Random;

public class Spawn {

    private Handler handler;
    private HUD hud;
    private Game game;
    private Random r = new Random();

    private int scoreKeep = 0;
    private boolean bossActive = false;

    public float getLevelProgress() { return scoreKeep / 250f; }
    public boolean isBossActive() { return bossActive; }

    // Enemy colors for telegraphs
    private static final Color C_BASIC = new Color(235, 87, 87);
    private static final Color C_FAST = new Color(78, 205, 196);
    private static final Color C_SMART = new Color(199, 125, 255);
    private static final Color C_HARD = new Color(245, 195, 68);

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
                        hud.triggerWaveAnnounce();
                        spawnTelegraph(new BasicEnemy(rx(), ry(), ID.BasicEnemy, handler), C_BASIC);
                        spawnTelegraph(new FastEnemy(rx(), ry(), ID.FastEnemy, handler), C_FAST);
                    } else {
                        bossStillAlive = true;
                    }
                }
            }
            if (bossStillAlive) return;
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

    private int rx() { return r.nextInt(Game.WIDTH - 80) + 20; }
    private int ry() { return r.nextInt(Game.HEIGHT - 80) + 20; }

    private void triggerBossSpawn() {
        // Collect enemy positions for explosion particles
        java.util.List<float[]> particles = new java.util.ArrayList<>();
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject obj = handler.getObjects().get(i);
            if (obj.getId() == ID.Player || obj.getId() == ID.Trail || obj.getId() == ID.SpawnTelegraph) continue;
            float ex = obj.getX() + 16;
            float ey = obj.getY() + 16;
            // Determine color from enemy type
            Color c = C_BASIC;
            if (obj.getId() == ID.FastEnemy) c = C_FAST;
            else if (obj.getId() == ID.SmartEnemy) c = C_SMART;
            else if (obj.getId() == ID.HardEnemy) c = C_HARD;
            // Spawn 6 fragments per enemy
            for (int j = 0; j < 6; j++) {
                float angle = (float) (r.nextFloat() * Math.PI * 2);
                float speed = 3f + r.nextFloat() * 5f;
                float size = 3 + r.nextFloat() * 6;
                particles.add(new float[]{
                        ex, ey,
                        (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                        size, c.getRed(), c.getGreen(), c.getBlue(), 1f
                });
            }
        }
        Game.setEnemyExplosions(particles.toArray(new float[0][]));

        handler.clearEnemys();
        Game.triggerBossIntro();

        // Boss enters off-screen — slides in during the intro
        handler.addObject(new EnemyBoss((Game.WIDTH / 2) - 48, -120, ID.EnemyBoss, handler));
        bossActive = true;
    }

    private void spawnTelegraph(GameObject enemy, Color color) {
        handler.addObject(new SpawnTelegraph(enemy.getX(), enemy.getY(), handler, enemy, color, 32));
    }

    private void spawnNormalDifficulty() {
        int level = hud.getLevel();
        switch (level) {
            case 2:
            case 3:
                spawnTelegraph(new BasicEnemy(rx(), ry(), ID.BasicEnemy, handler), C_BASIC); break;
            case 4:
            case 6:
            case 8:
                spawnTelegraph(new FastEnemy(rx(), ry(), ID.FastEnemy, handler), C_FAST); break;
            case 5:
            case 25:
                spawnTelegraph(new SmartEnemy(rx(), ry(), ID.SmartEnemy, handler), C_SMART); break;
            case 10:
                triggerBossSpawn();
                break;
            case 15:
                handler.clearEnemys();
                spawnTelegraph(new FastEnemy(rx(), ry(), ID.FastEnemy, handler), C_FAST); break;
            case 17:
                spawnTelegraph(new FastEnemy(rx(), ry(), ID.FastEnemy, handler), C_FAST); break;
            case 18:
            case 20:
            case 22:
                spawnTelegraph(new BasicEnemy(rx(), ry(), ID.BasicEnemy, handler), C_BASIC); break;
        }
    }

    private void spawnHardDifficulty() {
        int level = hud.getLevel();
        switch (level) {
            case 2:
            case 6:
                spawnTelegraph(new HardEnemy(rx(), ry(), ID.HardEnemy, handler), C_HARD); break;
            case 3:
                spawnTelegraph(new SmartEnemy(rx(), ry(), ID.SmartEnemy, handler), C_SMART); break;
            case 4:
            case 5:
                spawnTelegraph(new BasicEnemy(rx(), ry(), ID.BasicEnemy, handler), C_BASIC); break;
            case 15:
                triggerBossSpawn();
                break;
        }
    }

    private void spawnInsaneDifficulty() {
        int level = hud.getLevel();
        switch (level) {
            case 2:
            case 3:
            case 10:
                spawnTelegraph(new FastEnemy(rx(), ry(), ID.FastEnemy, handler), C_FAST); break;
            case 4:
                spawnTelegraph(new SmartEnemy(rx(), ry(), ID.SmartEnemy, handler), C_SMART); break;
            case 5:
            case 7:
            case 20:
                spawnTelegraph(new HardEnemy(rx(), ry(), ID.HardEnemy, handler), C_HARD); break;
            case 8:
            case 12:
                spawnTelegraph(new BasicEnemy(rx(), ry(), ID.BasicEnemy, handler), C_BASIC); break;
            case 15:
                triggerBossSpawn();
                break;
        }
    }

    private void spawnNoPlayerDifficulty() {
        int level = hud.getLevel();
        switch (level) {
            case 2:
                spawnTelegraph(new FastEnemy(rx(), ry(), ID.FastEnemy, handler), C_FAST); break;
            case 3:
                spawnTelegraph(new HardEnemy(rx(), ry(), ID.HardEnemy, handler), C_HARD); break;
            case 4:
                spawnTelegraph(new SmartEnemy(rx(), ry(), ID.SmartEnemy, handler), C_SMART); break;
        }
    }
}
