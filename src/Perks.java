/**
 * Loadout / perk system — choose up to 2 passive perks before each run.
 * Perks unlock via profile level or achievements. Effects applied during gameplay.
 */
public class Perks {

    // Perk IDs
    public static final int THICK_SKIN = 0;    // 20% damage reduction
    public static final int SWIFT = 1;         // +1 starting speed
    public static final int FORTIFIED = 2;     // +25 starting health
    public static final int GLASS_CANNON = 3;  // half health, double score
    public static final int ADRENALINE = 4;    // dash cooldown 40% shorter
    public static final int SECOND_WIND = 5;   // shield recharges 50% faster
    public static final int SLOW_STARTER = 6;  // +1 slow-mo charge
    public static final int STREAK_MASTER = 7; // streak builds 30% faster
    public static final int PERK_COUNT = 8;

    public static final int MAX_EQUIPPED = 2;

    public static final String[] NAMES = {
            "Thick Skin", "Swift", "Fortified", "Glass Cannon",
            "Adrenaline", "Second Wind", "Slow Starter", "Streak Master"
    };

    public static final String[] DESCRIPTIONS = {
            "Take 20% less damage from all sources",
            "+1 starting movement speed",
            "+25 starting health capacity",
            "Half health, but earn double score",
            "Dash cooldown is 40% shorter",
            "Shield recharges 50% faster",
            "+1 slow-motion charge at start",
            "Dodge streak builds 30% faster"
    };

    // Icons — single character symbols for the perk cards
    public static final String[] ICONS = {
            "\u2764",  // heart
            "\u21E8",  // arrow right
            "\u2694",  // crossed swords (shield)
            "\u2620",  // skull
            "\u21AF",  // lightning
            "\u25D4",  // circle with segment
            "\u23F1",  // stopwatch
            "\u2605"   // star
    };

    // Currently equipped perks for this run (-1 = empty)
    private static int[] equipped = {-1, -1};

    // ===== Unlock conditions =====

    public static boolean isUnlocked(int perk) {
        if (CoinShop.isPerkUnlockedByShop(perk)) return true;
        switch (perk) {
            case THICK_SKIN:    return Profile.getLevel() >= 2;
            case SWIFT:         return true;
            case FORTIFIED:     return Profile.getLevel() >= 4;
            case GLASS_CANNON:  return Profile.getLevel() >= 8;
            case ADRENALINE:    return Profile.getLevel() >= 6;
            case SECOND_WIND:   return Profile.getLevel() >= 10;
            case SLOW_STARTER:  return Profile.getTotalBossesDefeated() >= 5;
            case STREAK_MASTER: return Achievements.getUnlockedCount() >= 10;
            default: return false;
        }
    }

    public static String getUnlockDesc(int perk) {
        switch (perk) {
            case THICK_SKIN:    return "Reach profile level 2";
            case SWIFT:         return ""; // always unlocked
            case FORTIFIED:     return "Reach profile level 4";
            case GLASS_CANNON:  return "Reach profile level 8";
            case ADRENALINE:    return "Reach profile level 6";
            case SECOND_WIND:   return "Reach profile level 10";
            case SLOW_STARTER:  return "Defeat 5 bosses";
            case STREAK_MASTER: return "Unlock 10 achievements";
            default: return "";
        }
    }

    // ===== Equip / Unequip =====

    public static int getEquipped(int slot) {
        return (slot >= 0 && slot < MAX_EQUIPPED) ? equipped[slot] : -1;
    }

    public static boolean isEquipped(int perk) {
        return equipped[0] == perk || equipped[1] == perk;
    }

    public static void toggleEquip(int perk) {
        if (!isUnlocked(perk)) return;

        // If already equipped, unequip
        for (int i = 0; i < MAX_EQUIPPED; i++) {
            if (equipped[i] == perk) {
                equipped[i] = -1;
                return;
            }
        }

        // Find empty slot
        for (int i = 0; i < MAX_EQUIPPED; i++) {
            if (equipped[i] == -1) {
                equipped[i] = perk;
                return;
            }
        }

        // Both slots full — replace the second one
        equipped[1] = perk;
    }

    public static void clearLoadout() {
        equipped[0] = -1;
        equipped[1] = -1;
    }

    public static int getEquippedCount() {
        int c = 0;
        if (equipped[0] >= 0) c++;
        if (equipped[1] >= 0) c++;
        return c;
    }

    // ===== Effect queries (used by gameplay code) =====

    public static boolean has(int perk) {
        return equipped[0] == perk || equipped[1] == perk;
    }

    /** Damage multiplier — applied to all incoming damage */
    public static float getDamageMultiplier() {
        float mult = 1f;
        if (has(THICK_SKIN)) mult *= 0.8f;
        if (has(GLASS_CANNON)) mult *= 1f; // glass cannon doesn't increase damage, just halves max HP
        return mult;
    }

    /** Starting speed bonus */
    public static int getStartingSpeedBonus() {
        return has(SWIFT) ? 1 : 0;
    }

    /** Starting health bonus */
    public static int getStartingHealthBonus() {
        return has(FORTIFIED) ? 25 : 0;
    }

    /** Max health multiplier (Glass Cannon halves it) */
    public static float getMaxHealthMultiplier() {
        return has(GLASS_CANNON) ? 0.5f : 1f;
    }

    /** Score multiplier from perks */
    public static float getScoreMultiplier() {
        return has(GLASS_CANNON) ? 2f : 1f;
    }

    /** Dash cooldown multiplier */
    public static float getDashCooldownMultiplier() {
        return has(ADRENALINE) ? 0.6f : 1f;
    }

    /** Shield cooldown multiplier */
    public static float getShieldCooldownMultiplier() {
        return has(SECOND_WIND) ? 0.5f : 1f;
    }

    /** Extra slow-mo charges */
    public static int getExtraSlowmoCharges() {
        return has(SLOW_STARTER) ? 1 : 0;
    }

    /** Streak build speed multiplier */
    public static float getStreakSpeedMultiplier() {
        return has(STREAK_MASTER) ? 1.3f : 1f;
    }
}
