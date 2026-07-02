import java.awt.Color;

public class GamePalette {

    // Difficulty themes
    // Normal: clean teal progression
    private static final Color[][] DIFF_ACCENTS = {
        { // Normal — teal to blue to gold
            new Color(78, 205, 196), new Color(100, 170, 240), new Color(140, 130, 255),
            new Color(180, 140, 220), new Color(200, 160, 100), new Color(255, 200, 80),
        },
        { // Hard — warm amber/orange tones
            new Color(245, 180, 60), new Color(240, 150, 50), new Color(235, 120, 60),
            new Color(230, 100, 80), new Color(220, 80, 60), new Color(255, 130, 40),
        },
        { // Insane — deep red/purple aggressive
            new Color(220, 50, 80), new Color(200, 50, 140), new Color(160, 50, 200),
            new Color(140, 40, 220), new Color(180, 30, 100), new Color(255, 50, 50),
        }
    };

    private static final Color[][] DIFF_BG_TINTS = {
        { // Normal
            new Color(12, 25, 28), new Color(12, 18, 32), new Color(18, 14, 30),
            new Color(22, 14, 25), new Color(25, 18, 12), new Color(28, 22, 10),
        },
        { // Hard
            new Color(28, 20, 10), new Color(30, 18, 10), new Color(30, 15, 10),
            new Color(30, 12, 10), new Color(28, 10, 8), new Color(30, 16, 6),
        },
        { // Insane
            new Color(28, 8, 14), new Color(25, 8, 20), new Color(20, 8, 28),
            new Color(18, 6, 30), new Color(24, 6, 14), new Color(30, 8, 8),
        }
    };

    private static int difficulty = 0;
    private static float currentR, currentG, currentB;
    private static float targetR, targetG, targetB;
    private static float bgR, bgG, bgB;
    private static float bgTargetR, bgTargetG, bgTargetB;
    private static boolean initialized = false;

    private static final float LERP_SPEED = 0.02f;

    public static void setDifficulty(int diff) {
        difficulty = Math.max(0, Math.min(2, diff));
    }

    public static int getDifficulty() {
        return difficulty;
    }

    /** Background animation speed multiplier */
    public static float getGeoSpeed() {
        return difficulty == 0 ? 1f : difficulty == 1 ? 1.6f : 2.4f;
    }

    /** Particle/visual density multiplier */
    public static float getParticleDensity() {
        return difficulty == 0 ? 1f : difficulty == 1 ? 1.3f : 1.8f;
    }

    /** Screen distortion intensity (Insane only) */
    public static float getDistortion() {
        return difficulty == 2 ? 1f : 0f;
    }

    public static void update(int level) {
        int idx = getIndex(level);
        Color[] accents = DIFF_ACCENTS[difficulty];
        Color[] bgTints = DIFF_BG_TINTS[difficulty];
        Color target = accents[idx];
        Color bgTarget = bgTints[idx];

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

    // Cached accent — recomputed only when the underlying RGB or colorblind mode changes.
    // accent() is called many times per frame but currentR/G/B only lerp once per tick, so
    // this returns the same immutable Color instead of allocating (+ HSB math) on every call.
    private static Color cachedAccent;
    private static int cachedR = -1, cachedG = -1, cachedB = -1;
    private static boolean cachedColorblind;

    public static Color accent() {
        int r = clamp(currentR), g = clamp(currentG), b = clamp(currentB);
        boolean cb = Settings.getColorblindMode();
        if (cachedAccent == null || r != cachedR || g != cachedG || b != cachedB || cb != cachedColorblind) {
            Color base = new Color(r, g, b);
            cachedAccent = cb ? toColorblind(base) : base;
            cachedR = r; cachedG = g; cachedB = b; cachedColorblind = cb;
        }
        return cachedAccent;
    }

    public static Color accent(int alpha) {
        Color base = accent();
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    /** Remap a color toward a deuteranopia-friendly palette (blue/orange/yellow) */
    private static Color toColorblind(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float hue = hsb[0];
        // Shift greens/reds to blue/orange range
        // Red (0.0/1.0) → orange (0.08), green (0.33) → blue (0.58), teal (0.5) → blue (0.6)
        if (hue < 0.15f || hue > 0.9f) {
            hue = 0.08f; // reds → orange
        } else if (hue < 0.45f) {
            hue = 0.58f; // greens/yellows → blue
        } else if (hue < 0.7f) {
            hue = 0.62f; // teals/cyans → deeper blue
        } else {
            hue = 0.75f; // purples stay in blue-violet range
        }
        return Color.getHSBColor(hue, Math.min(hsb[1] * 1.1f, 1f), hsb[2]);
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
