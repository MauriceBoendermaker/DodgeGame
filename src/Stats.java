import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Stats {

    private static final String FILE = "dotch_stats.dat";

    // attempts[difficulty] and highScores[difficulty]
    private static int[] attempts = new int[3];     // 0=normal, 1=hard, 2=insane
    private static int[] highScores = new int[3];
    private static int currentAttempt = 0;

    static {
        load();
    }

    public static void newAttempt(int difficulty) {
        if (difficulty >= 0 && difficulty < 3) {
            attempts[difficulty]++;
            currentAttempt = attempts[difficulty];
            save();
        }
    }

    public static int getCurrentAttempt() {
        return currentAttempt;
    }

    public static int getAttempts(int difficulty) {
        if (difficulty >= 0 && difficulty < 3) return attempts[difficulty];
        return 0;
    }

    public static int getHighScore(int difficulty) {
        if (difficulty >= 0 && difficulty < 3) return highScores[difficulty];
        return 0;
    }

    public static boolean submitScore(int difficulty, int score) {
        if (difficulty >= 0 && difficulty < 3 && score > highScores[difficulty]) {
            highScores[difficulty] = score;
            save();
            return true;
        }
        return false;
    }

    private static void save() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(FILE))) {
            for (int i = 0; i < 3; i++) out.writeInt(attempts[i]);
            for (int i = 0; i < 3; i++) out.writeInt(highScores[i]);
        } catch (IOException e) {
            // Silent fail — stats are nice-to-have
        }
    }

    private static void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            for (int i = 0; i < 3; i++) attempts[i] = in.readInt();
            for (int i = 0; i < 3; i++) highScores[i] = in.readInt();
        } catch (IOException e) {
            // Corrupted or old format — reset
            attempts = new int[3];
            highScores = new int[3];
        }
    }
}
