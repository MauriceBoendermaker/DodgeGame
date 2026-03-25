import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Player skin system — shapes + color palettes with unlock conditions.
 * Each shape has a matching trail style. Colors override the player's base fill.
 */
public class PlayerSkins {

    // ===== Shape definitions =====
    public static final int SHAPE_SQUARE = 0;    // default rounded rect
    public static final int SHAPE_CIRCLE = 1;
    public static final int SHAPE_TRIANGLE = 2;
    public static final int SHAPE_DIAMOND = 3;
    public static final int SHAPE_STAR = 4;
    public static final int SHAPE_HEXAGON = 5;
    public static final int SHAPE_COUNT = 6;

    public static final String[] SHAPE_NAMES = {
            "Square", "Circle", "Triangle", "Diamond", "Star", "Hexagon"
    };

    // Trail shape mapping — matches Trail.SHAPE_* constants
    public static final int[] SHAPE_TRAIL = {
            Trail.SHAPE_ROUND_RECT,  // square → round rect trail
            Trail.SHAPE_CIRCLE,      // circle → circle trail
            Trail.SHAPE_TRIANGLE,    // triangle → triangle trail
            Trail.SHAPE_DIAMOND,     // diamond → diamond trail
            Trail.SHAPE_CIRCLE,      // star → circle trail (stars too complex for trail)
            Trail.SHAPE_ROUND_RECT,  // hexagon → round rect trail
    };

    // ===== Color palette definitions =====
    public static final int COLOR_DEFAULT = 0;   // white/light gray
    public static final int COLOR_NEON = 1;      // teal/cyan
    public static final int COLOR_EMBER = 2;     // warm orange
    public static final int COLOR_ROYAL = 3;     // purple
    public static final int COLOR_GHOST = 4;     // silver/ice blue
    public static final int COLOR_GOLDEN = 5;    // gold
    public static final int COLOR_CRIMSON = 6;   // deep red
    public static final int COLOR_LIME = 7;      // green
    public static final int COLOR_COUNT = 8;

    public static final String[] COLOR_NAMES = {
            "Default", "Neon", "Ember", "Royal", "Ghost", "Golden", "Crimson", "Lime"
    };

    // Base fill colors per palette
    public static final Color[] COLOR_FILLS = {
            new Color(230, 234, 240),  // default
            new Color(78, 205, 196),   // neon teal
            new Color(245, 170, 60),   // ember
            new Color(180, 120, 255),  // royal purple
            new Color(180, 200, 220),  // ghost silver
            new Color(255, 210, 80),   // golden
            new Color(220, 70, 80),    // crimson
            new Color(120, 220, 80),   // lime
    };

    // Glow accent per palette (used for trail tint)
    public static final Color[] COLOR_GLOWS = {
            new Color(200, 210, 220),  // default
            new Color(100, 230, 220),  // neon
            new Color(255, 190, 80),   // ember
            new Color(200, 150, 255),  // royal
            new Color(160, 200, 240),  // ghost
            new Color(255, 225, 120),  // golden
            new Color(240, 100, 100),  // crimson
            new Color(140, 240, 100),  // lime
    };

    // ===== Unlock conditions =====

    /** Check if a shape is unlocked */
    public static boolean isShapeUnlocked(int shape) {
        if (shape == SHAPE_SQUARE) return true;
        if (CoinShop.isShapeUnlockedByShop(shape)) return true;
        switch (shape) {
            case SHAPE_CIRCLE:   return Profile.getLevel() >= 3;
            case SHAPE_TRIANGLE: return Profile.getLevel() >= 6;
            case SHAPE_DIAMOND:  return Profile.getTotalGames() >= 25;
            case SHAPE_STAR:     return Profile.getLevel() >= 12;
            case SHAPE_HEXAGON:  return Profile.getTotalBossesDefeated() >= 10;
            default: return false;
        }
    }

    /** Get unlock description for a locked shape */
    public static String getShapeUnlockDesc(int shape) {
        switch (shape) {
            case SHAPE_CIRCLE:   return "Reach profile level 3";
            case SHAPE_TRIANGLE: return "Reach profile level 6";
            case SHAPE_DIAMOND:  return "Play 25 games";
            case SHAPE_STAR:     return "Reach profile level 12";
            case SHAPE_HEXAGON:  return "Defeat 10 bosses";
            default: return "";
        }
    }

    /** Check if a color is unlocked */
    public static boolean isColorUnlocked(int color) {
        if (color == COLOR_DEFAULT) return true;
        if (CoinShop.isColorUnlockedByShop(color)) return true;
        switch (color) {
            case COLOR_NEON:     return Profile.getLevel() >= 2;
            case COLOR_EMBER:    return Profile.getLevel() >= 5;
            case COLOR_ROYAL:    return Profile.getLevel() >= 8;
            case COLOR_GHOST:    return Profile.getTotalScore() >= 100000;
            case COLOR_GOLDEN:   return Profile.getLevel() >= 15;
            case COLOR_CRIMSON:  return Achievements.getUnlockedCount() >= 20;
            case COLOR_LIME:     return Profile.getHighLevel(2) >= 10;
            default: return false;
        }
    }

    /** Get unlock description for a locked color */
    public static String getColorUnlockDesc(int color) {
        switch (color) {
            case COLOR_NEON:     return "Reach profile level 2";
            case COLOR_EMBER:    return "Reach profile level 5";
            case COLOR_ROYAL:    return "Reach profile level 8";
            case COLOR_GHOST:    return "Earn 100,000 total score";
            case COLOR_GOLDEN:   return "Reach profile level 15";
            case COLOR_CRIMSON:  return "Unlock 20 achievements";
            case COLOR_LIME:     return "Reach level 10 on Insane";
            default: return "";
        }
    }

    // ===== Current selection (persisted in Profile) =====

    private static int selectedShape = SHAPE_SQUARE;
    private static int selectedColor = COLOR_DEFAULT;

    public static int getSelectedShape() { return selectedShape; }
    public static int getSelectedColor() { return selectedColor; }
    public static Color getSelectedFill() { return COLOR_FILLS[selectedColor]; }
    public static Color getSelectedGlow() { return COLOR_GLOWS[selectedColor]; }
    public static int getSelectedTrailShape() { return SHAPE_TRAIL[selectedShape]; }

    public static void setSelectedShape(int shape) {
        if (shape >= 0 && shape < SHAPE_COUNT && isShapeUnlocked(shape)) {
            selectedShape = shape;
            Profile.saveSkinSelection();
        }
    }

    public static void setSelectedColor(int color) {
        if (color >= 0 && color < COLOR_COUNT && isColorUnlocked(color)) {
            selectedColor = color;
            Profile.saveSkinSelection();
        }
    }

    // Called by Profile on load/save
    public static int getShapeId() { return selectedShape; }
    public static int getColorId() { return selectedColor; }
    public static void loadSelection(int shape, int color) {
        selectedShape = (shape >= 0 && shape < SHAPE_COUNT) ? shape : SHAPE_SQUARE;
        selectedColor = (color >= 0 && color < COLOR_COUNT) ? color : COLOR_DEFAULT;
    }

    // ===== Drawing helpers =====

    /** Draw the player shape at (ix, iy) with given size, corner radius, and color */
    public static void drawShape(Graphics2D g, int shape, int ix, int iy, int size, int r, Color fill) {
        int cx = ix + size / 2, cy = iy + size / 2;
        int half = size / 2;
        g.setColor(fill);

        switch (shape) {
            case SHAPE_CIRCLE:
                g.fillOval(ix, iy, size, size);
                break;
            case SHAPE_TRIANGLE: {
                int[] xp = new int[3], yp = new int[3];
                for (int i = 0; i < 3; i++) {
                    float a = (float) (-Math.PI / 2 + i * Math.PI * 2 / 3);
                    xp[i] = cx + (int) (Math.cos(a) * half);
                    yp[i] = cy + (int) (Math.sin(a) * half);
                }
                g.fillPolygon(xp, yp, 3);
                break;
            }
            case SHAPE_DIAMOND:
                g.fillPolygon(
                        new int[]{cx, cx + half, cx, cx - half},
                        new int[]{cy - half, cy, cy + half, cy}, 4);
                break;
            case SHAPE_STAR: {
                int points = 5;
                int[] xp = new int[points * 2], yp = new int[points * 2];
                for (int i = 0; i < points * 2; i++) {
                    float a = (float) (-Math.PI / 2 + i * Math.PI / points);
                    float dist = (i % 2 == 0) ? half : half * 0.45f;
                    xp[i] = cx + (int) (Math.cos(a) * dist);
                    yp[i] = cy + (int) (Math.sin(a) * dist);
                }
                g.fillPolygon(xp, yp, points * 2);
                break;
            }
            case SHAPE_HEXAGON: {
                int[] xp = new int[6], yp = new int[6];
                for (int i = 0; i < 6; i++) {
                    float a = (float) (i * Math.PI / 3);
                    xp[i] = cx + (int) (Math.cos(a) * half);
                    yp[i] = cy + (int) (Math.sin(a) * half);
                }
                g.fillPolygon(xp, yp, 6);
                break;
            }
            default: // SHAPE_SQUARE
                g.fillRoundRect(ix, iy, size, size, r, r);
                break;
        }
    }

    /** Draw outline of the shape */
    public static void drawShapeOutline(Graphics2D g, int shape, int ix, int iy, int size, int r, Color color) {
        int cx = ix + size / 2, cy = iy + size / 2;
        int half = size / 2;
        g.setColor(color);

        switch (shape) {
            case SHAPE_CIRCLE:
                g.drawOval(ix, iy, size, size);
                break;
            case SHAPE_TRIANGLE: {
                int[] xp = new int[3], yp = new int[3];
                for (int i = 0; i < 3; i++) {
                    float a = (float) (-Math.PI / 2 + i * Math.PI * 2 / 3);
                    xp[i] = cx + (int) (Math.cos(a) * half);
                    yp[i] = cy + (int) (Math.sin(a) * half);
                }
                g.drawPolygon(xp, yp, 3);
                break;
            }
            case SHAPE_DIAMOND:
                g.drawPolygon(
                        new int[]{cx, cx + half, cx, cx - half},
                        new int[]{cy - half, cy, cy + half, cy}, 4);
                break;
            case SHAPE_STAR: {
                int points = 5;
                int[] xp = new int[points * 2], yp = new int[points * 2];
                for (int i = 0; i < points * 2; i++) {
                    float a = (float) (-Math.PI / 2 + i * Math.PI / points);
                    float dist = (i % 2 == 0) ? half : half * 0.45f;
                    xp[i] = cx + (int) (Math.cos(a) * dist);
                    yp[i] = cy + (int) (Math.sin(a) * dist);
                }
                g.drawPolygon(xp, yp, points * 2);
                break;
            }
            case SHAPE_HEXAGON: {
                int[] xp = new int[6], yp = new int[6];
                for (int i = 0; i < 6; i++) {
                    float a = (float) (i * Math.PI / 3);
                    xp[i] = cx + (int) (Math.cos(a) * half);
                    yp[i] = cy + (int) (Math.sin(a) * half);
                }
                g.drawPolygon(xp, yp, 6);
                break;
            }
            default:
                g.drawRoundRect(ix, iy, size, size, r, r);
                break;
        }
    }
}
