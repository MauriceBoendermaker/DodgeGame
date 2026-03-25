import java.util.HashMap;
import java.util.Map;

import org.newdawn.slick.Music;
import org.newdawn.slick.Sound;

public class AudioPlayer {

    private static Map<String, Sound> sounds = new HashMap<>();
    private static Map<String, Music> music = new HashMap<>();

    public static void load() {
        try {
            music.put("game_music", new Music("game_music.ogg"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Sound getSound(String key) {
        return sounds.get(key);
    }

    public static Music getMusic(String key) {
        return music.get(key);
    }
}
