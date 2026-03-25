import java.awt.Color;

public class GamePalette {

    // Color scheme per level range
    private static final Color[] ACCENTS = {
            new Color(78, 205, 196),   // 1-4:  teal (default)
            new Color(120, 140, 255),  // 5-7:  blue
            new Color(180, 100, 255),  // 8-10: purple
            new Color(255, 100, 150),  // 11-14: pink
            new Color(235, 87, 87),    // 15-19: crimson
            new Color(255, 170, 60),   // 20+:  orange/gold
    };

    private static final Color[] BG_TINTS = {
            new Color(15, 30, 30),     // teal-dark
            new Color(15, 18, 38),     // blue-dark
            new Color(22, 14, 35),     // purple-dark
            new Color(30, 14, 22),     // pink-dark
            new Color(30, 12, 12),     // crimson-dark
            new Color(30, 22, 10),     // orange-dark
    };

    private static float currentR, currentG, currentB;
    private static float targetR, targetG, targetB;
    private static float bgR, bgG, bgB;
    private static float bgTargetR, bgTargetG, bgTargetB;
    private static boolean initialized = false;

    private static final float LERP_SPEED = 0.02f;

    public static void update(int level) {
        int idx = getIndex(level);
        Color target = ACCENTS[idx];
        Color bgTarget = BG_TINTS[idx];

        targetR = target.getRed();
        targetG = target.getGreen();
        targetB = target.getBlue();
        bgTargetR = bgTarget.getRed();
        bgTargetG = bgTarget.getGreen();
        bgTargetB = bgTarget.getBlue();

        if (!initialized) {
            currentR = targetR; currentG = targetG; currentB = targetB;
            bgR = bgTargetR; bgG = bgTargetG; bgB = bgTargetB;
            initialized = true;
        }

        currentR += (targetR - currentR) * LERP_SPEED;
        currentG += (targetG - currentG) * LERP_SPEED;
        currentB += (targetB - currentB) * LERP_SPEED;
        bgR += (bgTargetR - bgR) * LERP_SPEED;
        bgG += (bgTargetG - bgG) * LERP_SPEED;
        bgB += (bgTargetB - bgB) * LERP_SPEED;
    }

    public static Color accent() {
        return new Color(clamp(currentR), clamp(currentG), clamp(currentB));
    }

    public static Color accent(int alpha) {
        return new Color(clamp(currentR), clamp(currentG), clamp(currentB), Math.max(0, Math.min(255, alpha)));
    }

    public static Color bgTint() {
        return new Color(clamp(bgR), clamp(bgG), clamp(bgB));
    }

    public static Color bgTint(int alpha) {
        return new Color(clamp(bgR), clamp(bgG), clamp(bgB), Math.max(0, Math.min(255, alpha)));
    }

    public static void reset() {
        initialized = false;
    }

    private static int getIndex(int level) {
        if (level <= 4) return 0;
        if (level <= 7) return 1;
        if (level <= 10) return 2;
        if (level <= 14) return 3;
        if (level <= 19) return 4;
        return 5;
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }
}
