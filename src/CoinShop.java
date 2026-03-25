/**
 * Coin Shop — permanent unlocks purchased with coins earned across runs.
 * Items include: skin/color unlocks, perk unlocks, and permanent stat boosts.
 * Purchased state persisted via Profile (v3).
 */
public class CoinShop {

    // Item categories
    public static final int CAT_UPGRADE = 0;
    public static final int CAT_SKIN = 1;
    public static final int CAT_COLOR = 2;
    public static final int CAT_PERK = 3;
    public static final String[] CAT_NAMES = {"Permanent Upgrades", "Skin Shapes", "Color Palettes", "Perk Unlocks"};

    // Item definition
    public static class Item {
        public final int id;
        public final String name;
        public final String description;
        public final int cost;
        public final int category;
        public boolean purchased;

        Item(int id, String name, String desc, int cost, int category) {
            this.id = id;
            this.name = name;
            this.description = desc;
            this.cost = cost;
            this.category = category;
            this.purchased = false;
        }
    }

    // ===== Item definitions =====
    // Permanent upgrades (0-6)
    // Skin shapes (7-11) — unlock shapes that also have level requirements
    // Color palettes (12-18) — unlock colors
    // Perk unlocks (19-24) — unlock perks early

    private static final Item[] ITEMS = {
        // --- Permanent upgrades ---
        new Item(0,  "Health Boost I",     "+10 permanent starting health",            200,  CAT_UPGRADE),
        new Item(1,  "Health Boost II",    "+10 more starting health (total +20)",     500,  CAT_UPGRADE),
        new Item(2,  "Health Boost III",   "+10 more starting health (total +30)",     1000, CAT_UPGRADE),
        new Item(3,  "Speed Boost",        "+1 permanent starting speed",              600,  CAT_UPGRADE),
        new Item(4,  "Extra Shield",       "Start with 2 shield charges",              800,  CAT_UPGRADE),
        new Item(5,  "Slow-Mo Reserve",    "+1 starting slow-motion charge",           700,  CAT_UPGRADE),
        new Item(6,  "Coin Magnet",        "Earn 25% more coins per run",              1500, CAT_UPGRADE),

        // --- Skin shapes ---
        new Item(7,  "Circle Shape",       "Unlock the Circle player shape",           150,  CAT_SKIN),
        new Item(8,  "Triangle Shape",     "Unlock the Triangle player shape",         300,  CAT_SKIN),
        new Item(9,  "Diamond Shape",      "Unlock the Diamond player shape",          250,  CAT_SKIN),
        new Item(10, "Star Shape",         "Unlock the Star player shape",             600,  CAT_SKIN),
        new Item(11, "Hexagon Shape",      "Unlock the Hexagon player shape",          500,  CAT_SKIN),

        // --- Color palettes ---
        new Item(12, "Neon Palette",       "Unlock the Neon teal color",               100,  CAT_COLOR),
        new Item(13, "Ember Palette",      "Unlock the Ember orange color",            200,  CAT_COLOR),
        new Item(14, "Royal Palette",      "Unlock the Royal purple color",            350,  CAT_COLOR),
        new Item(15, "Ghost Palette",      "Unlock the Ghost silver color",            500,  CAT_COLOR),
        new Item(16, "Golden Palette",     "Unlock the Golden color",                  800,  CAT_COLOR),
        new Item(17, "Crimson Palette",    "Unlock the Crimson red color",             600,  CAT_COLOR),
        new Item(18, "Lime Palette",       "Unlock the Lime green color",              400,  CAT_COLOR),

        // --- Perk unlocks ---
        new Item(19, "Thick Skin Perk",    "Unlock the Thick Skin perk",               300,  CAT_PERK),
        new Item(20, "Fortified Perk",     "Unlock the Fortified perk",                400,  CAT_PERK),
        new Item(21, "Adrenaline Perk",    "Unlock the Adrenaline perk",               500,  CAT_PERK),
        new Item(22, "Glass Cannon Perk",  "Unlock the Glass Cannon perk",             600,  CAT_PERK),
        new Item(23, "Second Wind Perk",   "Unlock the Second Wind perk",              700,  CAT_PERK),
        new Item(24, "Slow Starter Perk",  "Unlock the Slow Starter perk",             500,  CAT_PERK),
    };

    public static final int ITEM_COUNT = ITEMS.length;

    // ===== Queries =====

    public static Item getItem(int id) { return (id >= 0 && id < ITEM_COUNT) ? ITEMS[id] : null; }
    public static int getItemCount() { return ITEM_COUNT; }
    public static boolean isPurchased(int id) { return (id >= 0 && id < ITEM_COUNT) && ITEMS[id].purchased; }

    public static boolean canAfford(int id) {
        return id >= 0 && id < ITEM_COUNT && Profile.getCoins() >= ITEMS[id].cost;
    }

    // ===== Purchase =====

    public static boolean purchase(int id) {
        if (id < 0 || id >= ITEM_COUNT) return false;
        Item item = ITEMS[id];
        if (item.purchased) return false;
        if (!Profile.spendCoins(item.cost)) return false;
        item.purchased = true;
        Profile.saveSkinSelection(); // triggers full save
        return true;
    }

    // ===== Effect queries for gameplay =====

    /** Permanent starting health bonus from shop */
    public static int getPermHealthBonus() {
        int bonus = 0;
        if (ITEMS[0].purchased) bonus += 10;
        if (ITEMS[1].purchased) bonus += 10;
        if (ITEMS[2].purchased) bonus += 10;
        return bonus;
    }

    /** Permanent starting speed bonus from shop */
    public static int getPermSpeedBonus() {
        return ITEMS[3].purchased ? 1 : 0;
    }

    /** Extra shield charges from shop */
    public static int getExtraShieldCharges() {
        return ITEMS[4].purchased ? 1 : 0;
    }

    /** Extra slow-mo charges from shop */
    public static int getExtraSlowmoCharges() {
        return ITEMS[5].purchased ? 1 : 0;
    }

    /** Coin earning multiplier */
    public static float getCoinMultiplier() {
        return ITEMS[6].purchased ? 1.25f : 1f;
    }

    /** Check if a skin shape is unlocked via shop (alternative to level unlock) */
    public static boolean isShapeUnlockedByShop(int shape) {
        switch (shape) {
            case PlayerSkins.SHAPE_CIRCLE:   return ITEMS[7].purchased;
            case PlayerSkins.SHAPE_TRIANGLE: return ITEMS[8].purchased;
            case PlayerSkins.SHAPE_DIAMOND:  return ITEMS[9].purchased;
            case PlayerSkins.SHAPE_STAR:     return ITEMS[10].purchased;
            case PlayerSkins.SHAPE_HEXAGON:  return ITEMS[11].purchased;
            default: return false;
        }
    }

    /** Check if a color is unlocked via shop */
    public static boolean isColorUnlockedByShop(int color) {
        switch (color) {
            case PlayerSkins.COLOR_NEON:     return ITEMS[12].purchased;
            case PlayerSkins.COLOR_EMBER:    return ITEMS[13].purchased;
            case PlayerSkins.COLOR_ROYAL:    return ITEMS[14].purchased;
            case PlayerSkins.COLOR_GHOST:    return ITEMS[15].purchased;
            case PlayerSkins.COLOR_GOLDEN:   return ITEMS[16].purchased;
            case PlayerSkins.COLOR_CRIMSON:  return ITEMS[17].purchased;
            case PlayerSkins.COLOR_LIME:     return ITEMS[18].purchased;
            default: return false;
        }
    }

    /** Check if a perk is unlocked via shop */
    public static boolean isPerkUnlockedByShop(int perk) {
        switch (perk) {
            case Perks.THICK_SKIN:   return ITEMS[19].purchased;
            case Perks.FORTIFIED:    return ITEMS[20].purchased;
            case Perks.ADRENALINE:   return ITEMS[21].purchased;
            case Perks.GLASS_CANNON: return ITEMS[22].purchased;
            case Perks.SECOND_WIND:  return ITEMS[23].purchased;
            case Perks.SLOW_STARTER: return ITEMS[24].purchased;
            default: return false;
        }
    }

    // ===== Persistence helpers (called by Profile) =====

    public static void loadPurchased(boolean[] data) {
        int count = Math.min(data.length, ITEM_COUNT);
        for (int i = 0; i < count; i++) {
            ITEMS[i].purchased = data[i];
        }
    }

    public static void resetAll() {
        for (Item item : ITEMS) item.purchased = false;
    }
}
