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

    // Last scripted level per difficulty — endless kicks in after this
    private static final int SCRIPT_END_NORMAL = 25;
    private static final int SCRIPT_END_HARD = 15;
    private static final int SCRIPT_END_INSANE = 20;

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

            int level = hud.getLevel();

            // Boss every 10th level — universal across all difficulties
            if (level % 10 == 0 && level > 0) {
                // Scale boss HP: base 1200, +200 per boss encounter
                int bossNum = level / 10;
                float bossHp = 1200 + (bossNum - 1) * 200;
                triggerBossSpawn(bossHp);
            } else if (game.diff == 0) {
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
        triggerBossSpawn(1200);
    }

    private void triggerBossSpawn(float bossHp) {
        // Collect enemy positions for explosion particles
        java.util.List<float[]> particles = new java.util.ArrayList<>();
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject obj = handler.getObjects().get(i);
            if (obj.getId() == ID.Player || obj.getId() == ID.Trail || obj.getId() == ID.SpawnTelegraph) continue;
            float ex = obj.getX() + 16;
            float ey = obj.getY() + 16;
            Color c = C_BASIC;
            if (obj.getId() == ID.FastEnemy) c = C_FAST;
            else if (obj.getId() == ID.SmartEnemy) c = C_SMART;
            else if (obj.getId() == ID.HardEnemy) c = C_HARD;
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

        // Refill 20% health on boss level
        float maxHealth = 100 + (hud.bounds / 2);
        HUD.HEALTH = Math.min(HUD.HEALTH + maxHealth * 0.2f, maxHealth);

        // Boss enters off-screen — slides in during the intro
        EnemyBoss boss = new EnemyBoss((Game.WIDTH / 2) - 48, -120, ID.EnemyBoss, handler);
        boss.setMaxHp(bossHp);
        handler.addObject(boss);
        bossActive = true;
    }

    private void spawnTelegraph(GameObject enemy, Color color) {
        handler.addObject(new SpawnTelegraph(enemy.getX(), enemy.getY(), handler, enemy, color, 32));
    }

    /** Spawn an enemy with scaled velocity, then telegraph it. */
    private void spawnScaled(GameObject enemy, Color color, float speedMult) {
        enemy.setVelX(enemy.getVelX() * speedMult);
        enemy.setVelY(enemy.getVelY() * speedMult);
        spawnTelegraph(enemy, color);
    }

    private void explodeClearEnemys() {
        java.util.List<float[]> particles = new java.util.ArrayList<>();
        for (int i = 0; i < handler.getObjects().size(); i++) {
            GameObject obj = handler.getObjects().get(i);
            ID id = obj.getId();
            if (id == ID.Player || id == ID.Trail || id == ID.SpawnTelegraph) continue;
            float ex = obj.getX() + 16;
            float ey = obj.getY() + 16;
            Color c = C_BASIC;
            if (id == ID.FastEnemy) c = C_FAST;
            else if (id == ID.SmartEnemy) c = C_SMART;
            else if (id == ID.HardEnemy) c = C_HARD;
            else if (id == ID.EnemyBoss) c = C_BASIC;
            for (int j = 0; j < 5; j++) {
                float angle = (float) (r.nextFloat() * Math.PI * 2);
                float speed = 2f + r.nextFloat() * 4f;
                float size = 2 + r.nextFloat() * 5;
                particles.add(new float[]{
                        ex, ey,
                        (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                        size, c.getRed(), c.getGreen(), c.getBlue(), 0.8f
                });
            }
        }
        if (!particles.isEmpty()) {
            Game.setEnemyExplosions(particles.toArray(new float[0][]));
        }
        handler.clearEnemys();
    }

    // ==================== Scripted levels ====================

    private void spawnNormalDifficulty() {
        int level = hud.getLevel();
        if (level > SCRIPT_END_NORMAL) {
            spawnEndless(level, SCRIPT_END_NORMAL, 0);
            return;
        }
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
            case 15:
                explodeClearEnemys();
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
        if (level > SCRIPT_END_HARD) {
            spawnEndless(level, SCRIPT_END_HARD, 1);
            return;
        }
        switch (level) {
            case 2:
            case 6:
                spawnTelegraph(new HardEnemy(rx(), ry(), ID.HardEnemy, handler), C_HARD); break;
            case 3:
                spawnTelegraph(new SmartEnemy(rx(), ry(), ID.SmartEnemy, handler), C_SMART); break;
            case 4:
            case 5:
                spawnTelegraph(new BasicEnemy(rx(), ry(), ID.BasicEnemy, handler), C_BASIC); break;
        }
    }

    private void spawnInsaneDifficulty() {
        int level = hud.getLevel();
        if (level > SCRIPT_END_INSANE) {
            spawnEndless(level, SCRIPT_END_INSANE, 2);
            return;
        }
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

    // ==================== Endless scaling ====================

    /**
     * Formula-based endless spawner. Called for every level past the scripted content.
     * Boss levels (every 10th) are already handled in tick(), so this only handles non-boss levels.
     *
     * @param level      Current level number
     * @param scriptEnd  Last scripted level for this difficulty
     * @param difficulty 0=normal, 1=hard, 2=insane
     */
    private void spawnEndless(int level, int scriptEnd, int difficulty) {
        int wave = level - scriptEnd; // 1, 2, 3, ...

        // Difficulty escalation multiplier
        float diffMult = difficulty == 0 ? 1.0f : difficulty == 1 ? 1.25f : 1.5f;

        // Speed multiplier: gentle logarithmic growth, caps around ~1.8x
        float speedMult = 1.0f + (float) Math.log(1 + wave * diffMult * 0.08) * 0.7f;

        // Purge wave — every 15 waves, clears field and spawns a modest replacement set
        if (wave % 15 == 0) {
            explodeClearEnemys();
            int packSize = 2 + (int) (wave * diffMult * 0.06f);
            packSize = Math.min(packSize, 4);
            for (int i = 0; i < packSize; i++) {
                spawnWeightedEnemy(wave, diffMult, speedMult);
            }
            return;
        }

        // Regular wave — spawn 1 enemy, occasionally 2 at higher waves
        int count = 1;
        // Second enemy starts appearing around wave 6, becomes reliable around wave 15
        if (wave >= 6 && r.nextFloat() < Math.min(0.6f, wave * diffMult * 0.025f)) {
            count = 2;
        }

        for (int i = 0; i < count; i++) {
            spawnWeightedEnemy(wave, diffMult, speedMult);
        }
    }

    /**
     * Spawn a single enemy with type chosen by weighted random based on wave progression.
     * Type distribution shifts gradually — Basic stays common early, harder types creep in.
     */
    private void spawnWeightedEnemy(int wave, float diffMult, float speedMult) {
        // Progression factor: gentle ramp
        float t = wave * diffMult * 0.07f;

        // Basic:  dominant early (40), fades slowly to a floor (10)
        // Fast:   steady presence (25), slight rise then holds
        // Hard:   starts low (15), rises gradually (30 cap)
        // Smart:  starts low (10), rises slowly (25 cap)
        float wBasic = Math.max(10, 40 - t * 2f);
        float wFast  = 25 + Math.min(5, t * 0.8f);
        float wHard  = Math.min(30, 15 + t * 1.5f);
        float wSmart = Math.min(25, 10 + t * 1.2f);

        float total = wBasic + wFast + wHard + wSmart;
        float roll = r.nextFloat() * total;

        if (roll < wBasic) {
            spawnScaled(new BasicEnemy(rx(), ry(), ID.BasicEnemy, handler), C_BASIC, speedMult);
        } else if (roll < wBasic + wFast) {
            spawnScaled(new FastEnemy(rx(), ry(), ID.FastEnemy, handler), C_FAST, speedMult);
        } else if (roll < wBasic + wFast + wHard) {
            spawnScaled(new HardEnemy(rx(), ry(), ID.HardEnemy, handler), C_HARD, speedMult);
        } else {
            // SmartEnemy tracks via distance formula, speed scaling doesn't apply the same way
            // Instead, just spawn it — its tracking naturally gets harder with more enemies on screen
            spawnTelegraph(new SmartEnemy(rx(), ry(), ID.SmartEnemy, handler), C_SMART);
        }
    }
}
