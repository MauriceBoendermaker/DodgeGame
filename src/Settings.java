import java.io.*;

public class Settings {

    private static final String FILE = "dotch_settings.dat";

    // Audio
    private static float musicVolume = 0.10f;
    private static float sfxVolume = 0.50f;

    // Visuals
    private static int screenShake = 2;      // 0=Off, 1=Low, 2=High
    private static int particleDensity = 2;  // 0=Low, 1=Medium, 2=High
    private static boolean showFps = true;
    private static boolean playerTrail = true;
    private static boolean gridDots = true;
    private static boolean colorblindMode = false;

    // General
    private static int language = 0; // 0=ENG, 1=NLD, 2=DEU

    static {
        load();
        // Sync music volume to AudioPlayer on startup
        AudioPlayer.setVolume(musicVolume);
    }

    // ===== Audio =====

    public static float getMusicVolume() { return musicVolume; }
    public static void setMusicVolume(float v) {
        musicVolume = clamp(v);
        AudioPlayer.setVolume(musicVolume);
        save();
    }

    public static float getSfxVolume() { return sfxVolume; }
    public static void setSfxVolume(float v) {
        sfxVolume = clamp(v);
        save();
    }

    // ===== Visuals =====

    public static int getScreenShake() { return screenShake; }
    public static void setScreenShake(int v) { screenShake = Math.max(0, Math.min(2, v)); save(); }

    public static int getParticleDensity() { return particleDensity; }
    public static void setParticleDensity(int v) { particleDensity = Math.max(0, Math.min(2, v)); save(); }

    public static boolean getShowFps() { return showFps; }
    public static void setShowFps(boolean v) { showFps = v; save(); }

    public static boolean getPlayerTrail() { return playerTrail; }
    public static void setPlayerTrail(boolean v) { playerTrail = v; save(); }

    public static boolean getGridDots() { return gridDots; }
    public static void setGridDots(boolean v) { gridDots = v; save(); }

    public static boolean getColorblindMode() { return colorblindMode; }
    public static void setColorblindMode(boolean v) { colorblindMode = v; save(); }

    // ===== General =====

    public static int getLanguage() { return language; }
    public static void setLanguage(int v) { language = Math.max(0, Math.min(2, v)); save(); }

    // ===== Screen shake multiplier for game code =====

    public static float getShakeMultiplier() {
        switch (screenShake) {
            case 0: return 0f;
            case 1: return 0.5f;
            case 2: return 1f;
            default: return 0.5f;
        }
    }

    // ===== Particle density multiplier for game code =====

    public static float getParticleDensityMultiplier() {
        switch (particleDensity) {
            case 0: return 0.4f;
            case 1: return 1f;
            case 2: return 1.5f;
            default: return 1f;
        }
    }

    // ===== Persistence =====

    private static void save() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(FILE))) {
            out.writeInt(1); // version
            out.writeFloat(musicVolume);
            out.writeFloat(sfxVolume);
            out.writeInt(screenShake);
            out.writeInt(particleDensity);
            out.writeBoolean(showFps);
            out.writeBoolean(playerTrail);
            out.writeBoolean(gridDots);
            out.writeBoolean(colorblindMode);
            out.writeInt(language);
        } catch (IOException e) {
            // Silent fail
        }
    }

    private static void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            int version = in.readInt();
            if (version >= 1) {
                musicVolume = clamp(in.readFloat());
                sfxVolume = clamp(in.readFloat());
                screenShake = Math.max(0, Math.min(2, in.readInt()));
                particleDensity = Math.max(0, Math.min(2, in.readInt()));
                showFps = in.readBoolean();
                playerTrail = in.readBoolean();
                gridDots = in.readBoolean();
                colorblindMode = in.readBoolean();
                language = Math.max(0, Math.min(5, in.readInt()));
            }
        } catch (IOException e) {
            // Corrupted — use defaults
            resetToDefaults();
        }
    }

    public static void resetToDefaults() {
        musicVolume = 0.10f;
        sfxVolume = 0.50f;
        screenShake = 2;
        particleDensity = 2;
        showFps = true;
        playerTrail = true;
        gridDots = true;
        colorblindMode = false;
        language = 0;
        AudioPlayer.setVolume(musicVolume);
        save();
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
