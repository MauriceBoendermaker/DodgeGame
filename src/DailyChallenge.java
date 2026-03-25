import java.io.*;
import java.time.LocalDate;

/**
 * Daily Challenge — one seeded run per day, 28-day cycle (4 weeks).
 *
 * Seed: deterministic from epoch day. Same seed = same enemy spawns.
 * Difficulty rotates across the week: Mon/Tue=Normal, Wed/Thu=Hard, Fri/Sat=Insane, Sun=Hard.
 * One attempt per day. Score + streak tracked.
 */
public class DailyChallenge {

    private static final String FILE = "dotch_daily.dat";
    private static final int VERSION = 1;
    private static final int CYCLE_LENGTH = 28;
    private static final long BASE_SEED = 0xD07C_DA17L;

    // Difficulty per day-of-week (0=Mon ... 6=Sun)
    // Mon=Normal, Tue=Normal, Wed=Hard, Thu=Hard, Fri=Insane, Sat=Insane, Sun=Hard
    private static final int[] DAY_DIFFICULTY = {0, 0, 1, 1, 2, 2, 1};

    // Persistent state
    private static int[] scores = new int[CYCLE_LENGTH];      // score per day in cycle
    private static boolean[] completed = new boolean[CYCLE_LENGTH];
    private static long lastPlayedEpochDay = 0;
    private static int currentStreak = 0;
    private static int bestStreak = 0;
    private static int totalCompleted = 0;
    private static int bestDailyScore = 0;

    static {
        load();
    }

    // ===== Day / Cycle calculations =====

    /** Current epoch day (days since 1970-01-01) */
    public static long todayEpochDay() {
        return LocalDate.now().toEpochDay();
    }

    /** Position in the 28-day cycle (0-27) */
    public static int dayInCycle() {
        return (int) (todayEpochDay() % CYCLE_LENGTH);
    }

    /** Week within the cycle (0-3) */
    public static int weekInCycle() {
        return dayInCycle() / 7;
    }

    /** Day of week (0=Mon ... 6=Sun) */
    public static int dayOfWeek() {
        return dayInCycle() % 7;
    }

    /** Seed for today's challenge */
    public static long todaySeed() {
        long epochDay = todayEpochDay();
        int cycleDay = (int) (epochDay % CYCLE_LENGTH);
        int week = cycleDay / 7;
        int dayInWeek = cycleDay % 7;
        // Same day-of-week across weeks shares a base pattern, week adds variation
        return BASE_SEED ^ (dayInWeek * 7919L + week * 104729L + (epochDay / CYCLE_LENGTH) * 999983L);
    }

    /** Difficulty for today */
    public static int todayDifficulty() {
        return DAY_DIFFICULTY[dayOfWeek()];
    }

    public static String todayDifficultyName() {
        return todayDifficulty() == 0 ? "Normal" : todayDifficulty() == 1 ? "Hard" : "Insane";
    }

    /** Human-readable day label */
    public static String dayLabel(int dayInCycle) {
        String[] weekdays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int week = dayInCycle / 7;
        int dow = dayInCycle % 7;
        return "W" + (week + 1) + " " + weekdays[dow];
    }

    public static String todayLabel() {
        return dayLabel(dayInCycle());
    }

    // ===== Attempt tracking =====

    public static boolean hasPlayedToday() {
        return lastPlayedEpochDay == todayEpochDay();
    }

    public static boolean canPlay() {
        return !hasPlayedToday();
    }

    /** Submit today's score. Called once when the daily run ends. */
    public static void submitScore(int score) {
        long today = todayEpochDay();
        int day = dayInCycle();

        scores[day] = score;
        completed[day] = true;
        totalCompleted++;
        if (score > bestDailyScore) bestDailyScore = score;

        // Streak — check if yesterday was played
        if (lastPlayedEpochDay == today - 1) {
            currentStreak++;
        } else if (lastPlayedEpochDay != today) {
            currentStreak = 1;
        }
        if (currentStreak > bestStreak) bestStreak = currentStreak;

        lastPlayedEpochDay = today;
        save();
    }

    // ===== Getters =====

    public static int getScore(int dayInCycle) {
        return (dayInCycle >= 0 && dayInCycle < CYCLE_LENGTH) ? scores[dayInCycle] : 0;
    }

    public static boolean isCompleted(int dayInCycle) {
        return (dayInCycle >= 0 && dayInCycle < CYCLE_LENGTH) && completed[dayInCycle];
    }

    public static int getCurrentStreak() { return currentStreak; }
    public static int getBestStreak() { return bestStreak; }
    public static int getTotalCompleted() { return totalCompleted; }
    public static int getBestDailyScore() { return bestDailyScore; }

    public static int getDifficultyForDay(int dayInCycle) {
        return DAY_DIFFICULTY[dayInCycle % 7];
    }

    public static String getDifficultyNameForDay(int dayInCycle) {
        int d = getDifficultyForDay(dayInCycle);
        return d == 0 ? "Normal" : d == 1 ? "Hard" : "Insane";
    }

    // ===== Cycle reset check =====

    /** Called on load — if we've moved to a new cycle, clear old scores */
    private static void checkCycleReset() {
        // If more than CYCLE_LENGTH days since last play, reset the board
        long daysSince = todayEpochDay() - lastPlayedEpochDay;
        if (daysSince > CYCLE_LENGTH) {
            scores = new int[CYCLE_LENGTH];
            completed = new boolean[CYCLE_LENGTH];
            currentStreak = 0;
            save();
        }
    }

    // ===== Persistence =====

    private static void save() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(FILE))) {
            out.writeInt(VERSION);
            out.writeLong(lastPlayedEpochDay);
            out.writeInt(currentStreak);
            out.writeInt(bestStreak);
            out.writeInt(totalCompleted);
            out.writeInt(bestDailyScore);
            for (int i = 0; i < CYCLE_LENGTH; i++) {
                out.writeInt(scores[i]);
                out.writeBoolean(completed[i]);
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
            if (version >= 1) {
                lastPlayedEpochDay = in.readLong();
                currentStreak = in.readInt();
                bestStreak = in.readInt();
                totalCompleted = in.readInt();
                bestDailyScore = in.readInt();
                for (int i = 0; i < CYCLE_LENGTH; i++) {
                    scores[i] = in.readInt();
                    completed[i] = in.readBoolean();
                }
            }
        } catch (IOException e) {
            // Corrupted
        }
        checkCycleReset();
    }

    public static void resetAll() {
        scores = new int[CYCLE_LENGTH];
        completed = new boolean[CYCLE_LENGTH];
        lastPlayedEpochDay = 0;
        currentStreak = 0;
        bestStreak = 0;
        totalCompleted = 0;
        bestDailyScore = 0;
        save();
    }
}
