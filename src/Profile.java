import java.io.*;

/**
 * Persistent player profile — XP, level, lifetime statistics, run history.
 * Single file: dotch_profile.dat (versioned binary).
 * Absorbs and extends what Stats.java tracked.
 */
public class Profile {

    private static final String FILE = "dotch_profile.dat";
    private static final int VERSION = 3;

    // ===== XP, Level & Coins =====
    private static long totalXp = 0;
    private static int level = 1;
    private static long coins = 0;
    private static int runCoinsEarned = 0;

    // ===== Per-difficulty stats (0=Normal, 1=Hard, 2=Insane) =====
    private static int[] attempts = new int[3];
    private static int[] highScores = new int[3];
    private static int[] highLevels = new int[3];
    private static int[] bestTimeTicks = new int[3]; // ticks survived (best)

    // ===== Lifetime aggregates =====
    private static int totalGames = 0;
    private static int totalDeaths = 0;
    private static long totalTimeTicks = 0;   // total ticks across all runs
    private static long totalScore = 0;       // cumulative score
    private static long totalDamageTaken = 0;
    private static int totalUpgradesBought = 0;
    private static int totalHealthUps = 0;
    private static int totalSpeedUps = 0;
    private static int totalRefills = 0;
    private static int longestStreakTicks = 0; // longest no-damage streak ever
    private static int totalBossesDefeated = 0;

    // Enemy encounter counts (lifetime)
    private static int enemyBasic = 0;
    private static int enemyFast = 0;
    private static int enemySmart = 0;
    private static int enemyHard = 0;
    private static int enemyBoss = 0;

    // Recent run history (last 20 scores) — circular buffer
    private static final int HISTORY_SIZE = 20;
    private static int[] recentScores = new int[HISTORY_SIZE];
    private static int[] recentLevels = new int[HISTORY_SIZE];
    private static int[] recentDifficulty = new int[HISTORY_SIZE]; // 0/1/2
    private static int historyIndex = 0; // next write position
    private static int historyCount = 0; // total entries (up to HISTORY_SIZE)

    // Current run tracking (not persisted, reset each run)
    private static int currentAttempt = 0;
    private static int runXpEarned = 0;
    private static boolean runLeveledUp = false;
    private static int runLevelBefore = 1;

    static {
        load();
    }

    // ==================== XP System ====================

    /**
     * XP formula per run:
     * base = score / 10
     * levelBonus = levelsReached * 50
     * diffMultiplier = Normal 1.0, Hard 1.5, Insane 2.0
     * bossBonus = bossesDefeated * 100
     * total = (base + levelBonus + bossBonus) * diffMultiplier
     */
    public static int calculateRunXp(int score, int levelsReached, int difficulty, int bossesDefeated) {
        float diffMult = difficulty == 0 ? 1.0f : difficulty == 1 ? 1.5f : 2.0f;
        int base = score / 10;
        int levelBonus = levelsReached * 50;
        int bossBonus = bossesDefeated * 100;
        return (int) ((base + levelBonus + bossBonus) * diffMult);
    }

    /**
     * XP required to reach the next level.
     * Follows a soft exponential: 500 + level * 200 + level^1.5 * 50
     * Level 1→2: 780, Level 5→6: 2060, Level 10→11: 3680, Level 20→21: 6970
     */
    public static long xpForNextLevel(int lvl) {
        return (long) (500 + lvl * 200 + Math.pow(lvl, 1.5) * 50);
    }

    /** XP accumulated within current level (for progress bar) */
    public static long xpInCurrentLevel() {
        long xp = totalXp;
        for (int i = 1; i < level; i++) {
            xp -= xpForNextLevel(i);
        }
        return Math.max(0, xp);
    }

    public static float levelProgress() {
        long needed = xpForNextLevel(level);
        if (needed <= 0) return 0;
        return Math.min(1f, xpInCurrentLevel() / (float) needed);
    }

    private static void addXp(int xp) {
        totalXp += xp;
        while (xpInCurrentLevel() >= xpForNextLevel(level)) {
            level++;
            runLeveledUp = true;
        }
    }

    /**
     * Coins earned per run:
     * base = score / 50
     * levelBonus = levelsReached * 10
     * bossBonus = bossesDefeated * 25
     * diffMultiplier = Normal 1.0, Hard 1.3, Insane 1.6
     */
    public static int calculateRunCoins(int score, int levelsReached, int difficulty, int bossesDefeated) {
        float diffMult = difficulty == 0 ? 1.0f : difficulty == 1 ? 1.3f : 1.6f;
        int base = score / 50;
        int levelBonus = levelsReached * 10;
        int bossBonus = bossesDefeated * 25;
        return (int) ((base + levelBonus + bossBonus) * diffMult);
    }

    // Coin accessors
    public static long getCoins() { return coins; }
    public static int getRunCoinsEarned() { return runCoinsEarned; }

    public static boolean spendCoins(long amount) {
        if (amount > coins) return false;
        coins -= amount;
        save();
        return true;
    }

    // ==================== Run lifecycle ====================

    /** Called when a new run starts */
    public static void startRun(int difficulty) {
        if (difficulty >= 0 && difficulty < 3) {
            attempts[difficulty]++;
            currentAttempt = attempts[difficulty];
        }
        totalGames++;
        runXpEarned = 0;
        runLeveledUp = false;
        runLevelBefore = level;
        save();
    }

    /**
     * Called when a run ends (player dies).
     * @return XP earned this run
     */
    public static int endRun(int difficulty, int score, int levelReached, int ticksSurvived,
                              int healthUps, int speedUps, int refills,
                              int longestStreak, int bossesDefeated,
                              float damageTaken,
                              int encBasic, int encFast, int encSmart, int encHard, int encBoss) {
        totalDeaths++;

        // Per-difficulty bests
        if (difficulty >= 0 && difficulty < 3) {
            if (score > highScores[difficulty]) highScores[difficulty] = score;
            if (levelReached > highLevels[difficulty]) highLevels[difficulty] = levelReached;
            if (ticksSurvived > bestTimeTicks[difficulty]) bestTimeTicks[difficulty] = ticksSurvived;
        }

        // Lifetime aggregates
        totalScore += score;
        totalTimeTicks += ticksSurvived;
        totalDamageTaken += (long) damageTaken;
        totalUpgradesBought += healthUps + speedUps + refills;
        totalHealthUps += healthUps;
        totalSpeedUps += speedUps;
        totalRefills += refills;
        totalBossesDefeated += bossesDefeated;
        if (longestStreak > longestStreakTicks) longestStreakTicks = longestStreak;

        // Enemy encounters
        enemyBasic += encBasic;
        enemyFast += encFast;
        enemySmart += encSmart;
        enemyHard += encHard;
        enemyBoss += encBoss;

        // Recent history
        recentScores[historyIndex] = score;
        recentLevels[historyIndex] = levelReached;
        recentDifficulty[historyIndex] = difficulty;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
        if (historyCount < HISTORY_SIZE) historyCount++;

        // XP
        int xp = calculateRunXp(score, levelReached, difficulty, bossesDefeated);
        runXpEarned = xp;
        addXp(xp);

        // Coins (with Coin Magnet multiplier from shop)
        int earnedCoins = (int) (calculateRunCoins(score, levelReached, difficulty, bossesDefeated)
                * CoinShop.getCoinMultiplier());
        runCoinsEarned = earnedCoins;
        coins += earnedCoins;

        save();
        return xp;
    }

    // ==================== Getters ====================

    // XP & Level
    public static long getTotalXp() { return totalXp; }
    public static int getLevel() { return level; }
    public static int getRunXpEarned() { return runXpEarned; }
    public static boolean didLevelUp() { return runLeveledUp; }
    public static int getLevelBefore() { return runLevelBefore; }

    // Per-difficulty
    public static int getAttempts(int diff) { return (diff >= 0 && diff < 3) ? attempts[diff] : 0; }
    public static int getHighScore(int diff) { return (diff >= 0 && diff < 3) ? highScores[diff] : 0; }
    public static int getHighLevel(int diff) { return (diff >= 0 && diff < 3) ? highLevels[diff] : 0; }
    public static String getBestTime(int diff) {
        if (diff < 0 || diff >= 3) return "0s";
        return formatTicks(bestTimeTicks[diff]);
    }
    public static int getCurrentAttempt() { return currentAttempt; }

    // Lifetime
    public static int getTotalGames() { return totalGames; }
    public static int getTotalDeaths() { return totalDeaths; }
    public static String getTotalTimePlayed() { return formatTicks((int) Math.min(totalTimeTicks, Integer.MAX_VALUE)); }
    public static long getTotalScore() { return totalScore; }
    public static long getTotalDamageTaken() { return totalDamageTaken; }
    public static int getTotalUpgrades() { return totalUpgradesBought; }
    public static int getTotalHealthUps() { return totalHealthUps; }
    public static int getTotalSpeedUps() { return totalSpeedUps; }
    public static int getTotalRefills() { return totalRefills; }
    public static int getLongestStreak() { return longestStreakTicks / 60; } // in seconds
    public static int getTotalBossesDefeated() { return totalBossesDefeated; }

    // Enemy encounters
    public static int getEnemyBasic() { return enemyBasic; }
    public static int getEnemyFast() { return enemyFast; }
    public static int getEnemySmart() { return enemySmart; }
    public static int getEnemyHard() { return enemyHard; }
    public static int getEnemyBoss() { return enemyBoss; }

    // Derived
    public static int getAvgSurvivalSeconds() {
        if (totalGames == 0) return 0;
        return (int) (totalTimeTicks / totalGames / 60);
    }

    public static int getFavoriteDifficulty() {
        int max = 0, fav = 0;
        for (int i = 0; i < 3; i++) {
            if (attempts[i] > max) { max = attempts[i]; fav = i; }
        }
        return fav;
    }

    public static String getDifficultyName(int diff) {
        return diff == 0 ? "Normal" : diff == 1 ? "Hard" : "Insane";
    }

    // History
    public static int getHistoryCount() { return historyCount; }

    /** Get recent run at index (0 = most recent) */
    public static int getRecentScore(int ago) {
        if (ago >= historyCount) return 0;
        int idx = (historyIndex - 1 - ago + HISTORY_SIZE * 2) % HISTORY_SIZE;
        return recentScores[idx];
    }

    public static int getRecentLevel(int ago) {
        if (ago >= historyCount) return 0;
        int idx = (historyIndex - 1 - ago + HISTORY_SIZE * 2) % HISTORY_SIZE;
        return recentLevels[idx];
    }

    public static int getRecentDifficulty(int ago) {
        if (ago >= historyCount) return 0;
        int idx = (historyIndex - 1 - ago + HISTORY_SIZE * 2) % HISTORY_SIZE;
        return recentDifficulty[idx];
    }

    // ==================== Compat with Stats.java interface ====================

    /** Called by existing code that uses Stats.submitScore */
    public static boolean submitScore(int difficulty, int score) {
        if (difficulty >= 0 && difficulty < 3 && score > highScores[difficulty]) {
            highScores[difficulty] = score;
            save();
            return true;
        }
        return false;
    }

    public static void resetHighScores() {
        highScores = new int[3];
        highLevels = new int[3];
        bestTimeTicks = new int[3];
        save();
    }

    public static void resetAll() {
        totalXp = 0; level = 1;
        attempts = new int[3]; highScores = new int[3]; highLevels = new int[3]; bestTimeTicks = new int[3];
        totalGames = 0; totalDeaths = 0; totalTimeTicks = 0; totalScore = 0; totalDamageTaken = 0;
        totalUpgradesBought = 0; totalHealthUps = 0; totalSpeedUps = 0; totalRefills = 0;
        longestStreakTicks = 0; totalBossesDefeated = 0;
        enemyBasic = 0; enemyFast = 0; enemySmart = 0; enemyHard = 0; enemyBoss = 0;
        recentScores = new int[HISTORY_SIZE]; recentLevels = new int[HISTORY_SIZE];
        recentDifficulty = new int[HISTORY_SIZE]; historyIndex = 0; historyCount = 0;
        currentAttempt = 0;
        coins = 0;
        save();
    }

    /** Called when skin selection changes */
    public static void saveSkinSelection() { save(); }

    // ==================== Persistence ====================

    private static void save() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(FILE))) {
            out.writeInt(VERSION);

            // XP & Level
            out.writeLong(totalXp);
            out.writeInt(level);

            // Per-difficulty
            for (int i = 0; i < 3; i++) out.writeInt(attempts[i]);
            for (int i = 0; i < 3; i++) out.writeInt(highScores[i]);
            for (int i = 0; i < 3; i++) out.writeInt(highLevels[i]);
            for (int i = 0; i < 3; i++) out.writeInt(bestTimeTicks[i]);

            // Lifetime
            out.writeInt(totalGames);
            out.writeInt(totalDeaths);
            out.writeLong(totalTimeTicks);
            out.writeLong(totalScore);
            out.writeLong(totalDamageTaken);
            out.writeInt(totalUpgradesBought);
            out.writeInt(totalHealthUps);
            out.writeInt(totalSpeedUps);
            out.writeInt(totalRefills);
            out.writeInt(longestStreakTicks);
            out.writeInt(totalBossesDefeated);

            // Enemies
            out.writeInt(enemyBasic);
            out.writeInt(enemyFast);
            out.writeInt(enemySmart);
            out.writeInt(enemyHard);
            out.writeInt(enemyBoss);

            // History
            out.writeInt(historyIndex);
            out.writeInt(historyCount);
            for (int i = 0; i < HISTORY_SIZE; i++) out.writeInt(recentScores[i]);
            for (int i = 0; i < HISTORY_SIZE; i++) out.writeInt(recentLevels[i]);
            for (int i = 0; i < HISTORY_SIZE; i++) out.writeInt(recentDifficulty[i]);

            // v2: Skin selection
            out.writeInt(PlayerSkins.getShapeId());
            out.writeInt(PlayerSkins.getColorId());

            // v3: Coins + CoinShop purchases
            out.writeLong(coins);
            out.writeInt(CoinShop.ITEM_COUNT);
            for (int i = 0; i < CoinShop.ITEM_COUNT; i++) {
                out.writeBoolean(CoinShop.isPurchased(i));
            }

        } catch (IOException e) {
            // Silent fail
        }
    }

    private static void load() {
        // Try to import legacy Stats.dat if Profile doesn't exist yet
        File pf = new File(FILE);
        if (!pf.exists()) {
            importLegacyStats();
            return;
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(pf))) {
            int version = in.readInt();
            if (version >= 1) {
                totalXp = in.readLong();
                level = in.readInt();

                for (int i = 0; i < 3; i++) attempts[i] = in.readInt();
                for (int i = 0; i < 3; i++) highScores[i] = in.readInt();
                for (int i = 0; i < 3; i++) highLevels[i] = in.readInt();
                for (int i = 0; i < 3; i++) bestTimeTicks[i] = in.readInt();

                totalGames = in.readInt();
                totalDeaths = in.readInt();
                totalTimeTicks = in.readLong();
                totalScore = in.readLong();
                totalDamageTaken = in.readLong();
                totalUpgradesBought = in.readInt();
                totalHealthUps = in.readInt();
                totalSpeedUps = in.readInt();
                totalRefills = in.readInt();
                longestStreakTicks = in.readInt();
                totalBossesDefeated = in.readInt();

                enemyBasic = in.readInt();
                enemyFast = in.readInt();
                enemySmart = in.readInt();
                enemyHard = in.readInt();
                enemyBoss = in.readInt();

                historyIndex = in.readInt();
                historyCount = in.readInt();
                for (int i = 0; i < HISTORY_SIZE; i++) recentScores[i] = in.readInt();
                for (int i = 0; i < HISTORY_SIZE; i++) recentLevels[i] = in.readInt();
                for (int i = 0; i < HISTORY_SIZE; i++) recentDifficulty[i] = in.readInt();

                // v2: Skin selection
                if (version >= 2) {
                    PlayerSkins.loadSelection(in.readInt(), in.readInt());
                }

                // v3: Coins + CoinShop purchases
                if (version >= 3) {
                    coins = in.readLong();
                    int itemCount = in.readInt();
                    boolean[] purchased = new boolean[itemCount];
                    for (int i = 0; i < itemCount; i++) purchased[i] = in.readBoolean();
                    CoinShop.loadPurchased(purchased);
                }
            }
        } catch (IOException e) {
            // Corrupted or old format — defaults are fine
        }
    }

    /** Import data from legacy dotch_stats.dat if it exists */
    private static void importLegacyStats() {
        File legacy = new File("dotch_stats.dat");
        if (!legacy.exists()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(legacy))) {
            for (int i = 0; i < 3; i++) attempts[i] = in.readInt();
            for (int i = 0; i < 3; i++) highScores[i] = in.readInt();
            totalGames = attempts[0] + attempts[1] + attempts[2];
            totalDeaths = totalGames; // reasonable estimate
            save();
        } catch (IOException e) {
            // Can't read legacy — start fresh
        }
    }

    private static String formatTicks(int ticks) {
        int totalSec = ticks / 60;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        if (min > 0) return min + "m " + sec + "s";
        return sec + "s";
    }
}
