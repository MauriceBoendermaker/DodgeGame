import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Achievement system — 50 achievements across 4 categories.
 * Checked after each run ends. Persisted to dotch_achievements.dat.
 * Newly unlocked achievements are queued for in-game notification.
 */
public class Achievements {

    private static final String FILE = "dotch_achievements.dat";
    private static final int VERSION = 1;

    // Categories
    public static final int CAT_SURVIVAL = 0;
    public static final int CAT_SKILL = 1;
    public static final int CAT_PERSISTENCE = 2;
    public static final int CAT_CHALLENGE = 3;
    public static final String[] CAT_NAMES = {"Survival", "Skill", "Persistence", "Challenge"};

    // Achievement definition
    public static class Achievement {
        public final int id;
        public final String name;
        public final String description;
        public final int category;
        public boolean unlocked;

        Achievement(int id, String name, String desc, int category) {
            this.id = id;
            this.name = name;
            this.description = desc;
            this.category = category;
            this.unlocked = false;
        }
    }

    private static final List<Achievement> ALL = new ArrayList<>();
    private static final List<Achievement> newlyUnlocked = new ArrayList<>();

    // ==================== Achievement Definitions ====================

    static {
        // --- SURVIVAL (0-14) ---
        def(0, "First Steps", "Reach level 5", CAT_SURVIVAL);
        def(1, "Getting Warmed Up", "Reach level 10", CAT_SURVIVAL);
        def(2, "Veteran", "Reach level 15", CAT_SURVIVAL);
        def(3, "Endurance", "Reach level 20", CAT_SURVIVAL);
        def(4, "Unstoppable", "Reach level 30", CAT_SURVIVAL);
        def(5, "Minute Man", "Survive for 1 minute", CAT_SURVIVAL);
        def(6, "Five Alive", "Survive for 5 minutes", CAT_SURVIVAL);
        def(7, "Marathon Runner", "Survive for 10 minutes", CAT_SURVIVAL);
        def(8, "Score Chaser", "Score 5,000 in a single run", CAT_SURVIVAL);
        def(9, "High Roller", "Score 25,000 in a single run", CAT_SURVIVAL);
        def(10, "Score Legend", "Score 100,000 in a single run", CAT_SURVIVAL);
        def(11, "Boss Slayer", "Defeat your first boss", CAT_SURVIVAL);
        def(12, "Boss Hunter", "Defeat 5 bosses in a single run", CAT_SURVIVAL);
        def(13, "Wave Rider", "Reach wave 3", CAT_SURVIVAL);
        def(14, "Wave Master", "Reach wave 5", CAT_SURVIVAL);

        // --- SKILL (15-29) ---
        def(15, "Untouchable", "Survive 30 seconds without damage", CAT_SKILL);
        def(16, "Iron Will", "Survive 60 seconds without damage", CAT_SKILL);
        def(17, "Ghost", "Survive 2 minutes without damage", CAT_SKILL);
        def(18, "Flawless Boss", "Defeat a boss without taking any damage", CAT_SKILL);
        def(19, "Minimalist", "Reach level 5 without buying upgrades", CAT_SKILL);
        def(20, "Speedster", "Use the dash ability 50 times in one run", CAT_SKILL);
        def(21, "Bullet Time", "Use slow-motion 3 times in one run", CAT_SKILL);
        def(22, "Shield Breaker", "Have your shield absorb 5 hits in one run", CAT_SKILL);
        def(23, "Triple Multiplier", "Reach x3 score multiplier", CAT_SKILL);
        def(24, "Close Call", "Survive a run with less than 5 health remaining", CAT_SKILL);
        def(25, "Pacifist Start", "Reach level 3 without taking damage", CAT_SKILL);
        def(26, "No Shield Needed", "Reach level 10 without shield activating", CAT_SKILL);
        def(27, "Speed Demon", "Buy max speed upgrades in a single run", CAT_SKILL);
        def(28, "Tank Build", "Buy max health upgrades in a single run", CAT_SKILL);
        def(29, "Efficient", "Score 10,000 with 0 upgrades purchased", CAT_SKILL);

        // --- PERSISTENCE (30-42) ---
        def(30, "Newcomer", "Play 10 games", CAT_PERSISTENCE);
        def(31, "Regular", "Play 50 games", CAT_PERSISTENCE);
        def(32, "Dedicated", "Play 100 games", CAT_PERSISTENCE);
        def(33, "Veteran Player", "Play 500 games", CAT_PERSISTENCE);
        def(34, "Accumulator", "Reach 50,000 total lifetime score", CAT_PERSISTENCE);
        def(35, "Score Hoarder", "Reach 500,000 total lifetime score", CAT_PERSISTENCE);
        def(36, "Millionaire", "Reach 1,000,000 total lifetime score", CAT_PERSISTENCE);
        def(37, "Die Hard", "Die 50 times", CAT_PERSISTENCE);
        def(38, "Persistent", "Die 200 times", CAT_PERSISTENCE);
        def(39, "Level 5", "Reach profile level 5", CAT_PERSISTENCE);
        def(40, "Level 10", "Reach profile level 10", CAT_PERSISTENCE);
        def(41, "Level 25", "Reach profile level 25", CAT_PERSISTENCE);
        def(42, "Boss Veteran", "Defeat 25 bosses total", CAT_PERSISTENCE);

        // --- CHALLENGE (43-49) ---
        def(43, "Hard Mode", "Complete level 10 on Hard difficulty", CAT_CHALLENGE);
        def(44, "Insane Mode", "Complete level 10 on Insane difficulty", CAT_CHALLENGE);
        def(45, "Hard Veteran", "Reach level 20 on Hard difficulty", CAT_CHALLENGE);
        def(46, "Insane Veteran", "Reach level 15 on Insane difficulty", CAT_CHALLENGE);
        def(47, "All Rounder", "Play at least 10 games on each difficulty", CAT_CHALLENGE);
        def(48, "Hard High Score", "Score 50,000 on Hard difficulty", CAT_CHALLENGE);
        def(49, "Insane Survivor", "Survive 5 minutes on Insane difficulty", CAT_CHALLENGE);

        load();
    }

    private static void def(int id, String name, String desc, int cat) {
        ALL.add(new Achievement(id, name, desc, cat));
    }

    // ==================== Public API ====================

    public static List<Achievement> getAll() { return ALL; }
    public static int getCount() { return ALL.size(); }

    public static int getUnlockedCount() {
        int c = 0;
        for (Achievement a : ALL) if (a.unlocked) c++;
        return c;
    }

    public static int getUnlockedInCategory(int cat) {
        int c = 0;
        for (Achievement a : ALL) if (a.category == cat && a.unlocked) c++;
        return c;
    }

    public static int getTotalInCategory(int cat) {
        int c = 0;
        for (Achievement a : ALL) if (a.category == cat) c++;
        return c;
    }

    /** Get newly unlocked achievements (since last call). Clears the queue. */
    public static List<Achievement> popNewlyUnlocked() {
        List<Achievement> result = new ArrayList<>(newlyUnlocked);
        newlyUnlocked.clear();
        return result;
    }

    public static boolean hasNew() { return !newlyUnlocked.isEmpty(); }

    // ==================== Check Conditions ====================

    /**
     * Called after every run ends (from Game.java death sequence).
     * Checks all achievement conditions against current run + profile data.
     */
    public static void checkAfterRun(int runScore, int runLevel, int runTimeTicks,
                                      int runUpgrades, int runHealthUps, int runSpeedUps,
                                      int runBossesDefeated, float runDamageTaken,
                                      int runLongestStreakTicks, int difficulty,
                                      int runWave) {
        int runTimeSec = runTimeTicks / 60;
        int streakSec = runLongestStreakTicks / 60;

        // --- SURVIVAL ---
        checkUnlock(0, runLevel >= 5);
        checkUnlock(1, runLevel >= 10);
        checkUnlock(2, runLevel >= 15);
        checkUnlock(3, runLevel >= 20);
        checkUnlock(4, runLevel >= 30);
        checkUnlock(5, runTimeSec >= 60);
        checkUnlock(6, runTimeSec >= 300);
        checkUnlock(7, runTimeSec >= 600);
        checkUnlock(8, runScore >= 5000);
        checkUnlock(9, runScore >= 25000);
        checkUnlock(10, runScore >= 100000);
        checkUnlock(11, runBossesDefeated >= 1);
        checkUnlock(12, runBossesDefeated >= 5);
        checkUnlock(13, runWave >= 3);
        checkUnlock(14, runWave >= 5);

        // --- SKILL ---
        checkUnlock(15, streakSec >= 30);
        checkUnlock(16, streakSec >= 60);
        checkUnlock(17, streakSec >= 120);
        checkUnlock(18, runBossesDefeated >= 1 && runDamageTaken == 0);
        checkUnlock(19, runLevel >= 5 && runUpgrades == 0);
        // 20, 21, 22 checked during gameplay (need counters — defer to in-run checks)
        checkUnlock(23, Profile.getLongestStreak() >= 15); // 15s = STREAK_T3 / 60
        checkUnlock(24, runLevel >= 3 && HUD.HEALTH > 0 && HUD.HEALTH < 5);
        checkUnlock(25, runLevel >= 3 && runDamageTaken == 0);
        // 26 — no shield break for 10 levels — simplified: no damage for 10 levels is already ghost-tier
        checkUnlock(26, runLevel >= 10 && runDamageTaken == 0);
        checkUnlock(27, runSpeedUps >= HUD.MAX_TIER);
        checkUnlock(28, runHealthUps >= HUD.MAX_TIER);
        checkUnlock(29, runScore >= 10000 && runUpgrades == 0);

        // --- PERSISTENCE (profile-based) ---
        checkUnlock(30, Profile.getTotalGames() >= 10);
        checkUnlock(31, Profile.getTotalGames() >= 50);
        checkUnlock(32, Profile.getTotalGames() >= 100);
        checkUnlock(33, Profile.getTotalGames() >= 500);
        checkUnlock(34, Profile.getTotalScore() >= 50000);
        checkUnlock(35, Profile.getTotalScore() >= 500000);
        checkUnlock(36, Profile.getTotalScore() >= 1000000);
        checkUnlock(37, Profile.getTotalDeaths() >= 50);
        checkUnlock(38, Profile.getTotalDeaths() >= 200);
        checkUnlock(39, Profile.getLevel() >= 5);
        checkUnlock(40, Profile.getLevel() >= 10);
        checkUnlock(41, Profile.getLevel() >= 25);
        checkUnlock(42, Profile.getTotalBossesDefeated() >= 25);

        // --- CHALLENGE ---
        checkUnlock(43, difficulty == 1 && runLevel >= 10);
        checkUnlock(44, difficulty == 2 && runLevel >= 10);
        checkUnlock(45, difficulty == 1 && runLevel >= 20);
        checkUnlock(46, difficulty == 2 && runLevel >= 15);
        checkUnlock(47, Profile.getAttempts(0) >= 10
                && Profile.getAttempts(1) >= 10
                && Profile.getAttempts(2) >= 10);
        checkUnlock(48, difficulty == 1 && runScore >= 50000);
        checkUnlock(49, difficulty == 2 && runTimeSec >= 300);

        if (!newlyUnlocked.isEmpty()) {
            save();
        }
    }

    private static void checkUnlock(int id, boolean condition) {
        if (id >= 0 && id < ALL.size()) {
            Achievement a = ALL.get(id);
            if (!a.unlocked && condition) {
                a.unlocked = true;
                newlyUnlocked.add(a);
            }
        }
    }

    // ==================== Progress Helpers ====================

    /** Returns progress toward an achievement (0.0-1.0), or -1 if not applicable */
    public static float getProgress(int id) {
        Achievement a = ALL.get(id);
        if (a.unlocked) return 1f;

        switch (id) {
            // Survival: level-based
            case 0: return Profile.getHighLevel(bestDiff()) / 5f;
            case 1: return Profile.getHighLevel(bestDiff()) / 10f;
            case 2: return Profile.getHighLevel(bestDiff()) / 15f;
            case 3: return Profile.getHighLevel(bestDiff()) / 20f;
            case 4: return Profile.getHighLevel(bestDiff()) / 30f;

            // Score-based
            case 8: return Profile.getHighScore(bestDiff()) / 5000f;
            case 9: return Profile.getHighScore(bestDiff()) / 25000f;
            case 10: return Profile.getHighScore(bestDiff()) / 100000f;

            // Persistence: games
            case 30: return Profile.getTotalGames() / 10f;
            case 31: return Profile.getTotalGames() / 50f;
            case 32: return Profile.getTotalGames() / 100f;
            case 33: return Profile.getTotalGames() / 500f;

            // Persistence: score
            case 34: return Profile.getTotalScore() / 50000f;
            case 35: return Profile.getTotalScore() / 500000f;
            case 36: return Profile.getTotalScore() / 1000000f;

            // Persistence: deaths
            case 37: return Profile.getTotalDeaths() / 50f;
            case 38: return Profile.getTotalDeaths() / 200f;

            // Profile level
            case 39: return Profile.getLevel() / 5f;
            case 40: return Profile.getLevel() / 10f;
            case 41: return Profile.getLevel() / 25f;

            // Bosses
            case 42: return Profile.getTotalBossesDefeated() / 25f;
            case 11: return Math.min(Profile.getTotalBossesDefeated(), 1);

            default: return -1;
        }
    }

    private static int bestDiff() {
        int best = 0;
        for (int i = 1; i < 3; i++) {
            if (Profile.getHighLevel(i) > Profile.getHighLevel(best)) best = i;
        }
        return best;
    }

    // ==================== Persistence ====================

    private static void save() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(FILE))) {
            out.writeInt(VERSION);
            out.writeInt(ALL.size());
            for (Achievement a : ALL) {
                out.writeBoolean(a.unlocked);
            }
        } catch (IOException e) {
            // Silent
        }
    }

    private static void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            int version = in.readInt();
            int count = in.readInt();
            int toRead = Math.min(count, ALL.size());
            for (int i = 0; i < toRead; i++) {
                ALL.get(i).unlocked = in.readBoolean();
            }
            // Skip any extra if file has more than current definitions
            for (int i = toRead; i < count; i++) {
                in.readBoolean();
            }
        } catch (IOException e) {
            // Corrupted — keep defaults
        }
    }

    public static void resetAll() {
        for (Achievement a : ALL) a.unlocked = false;
        newlyUnlocked.clear();
        save();
    }
}
