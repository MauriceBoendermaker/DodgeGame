import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.newdawn.slick.Music;
import org.newdawn.slick.Sound;

public class AudioPlayer {

    private static Map<String, Sound> sounds = new HashMap<>();
    private static List<Track> tracks = new ArrayList<>();
    private static int currentTrackIndex = 0;
    private static float volume = 0.10f;
    private static boolean paused = false;
    private static boolean stopped = true;

    public static void load() {
        try {
            // Startup tracks — one of these plays first, chosen randomly
            addTrack("energy_drink", "Virtual Riot - Energy Drink", "Virtual Riot - Energy Drink.ogg", 303f, 150f);
            addTrack("press_start", "MDK - Press Start", "MDK - Press Start.ogg", 263f, 160f);
            addTrack("hellcat", "Desmeon - Hellcat", "Desmeon - Hellcat.ogg", 226f, 150f);

            // Remaining tracks — shuffled after the first song
            addTrack("fingerbang", "MDK - Fingerbang", "MDK - Fingerbang.ogg", 228f, 150f);
            addTrack("disconnected", "Pegboard Nerds - Disconnected", "Pegboard Nerds - Disconnected.ogg", 242f, 128f);

            buildPlaylist();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void buildPlaylist() {
        Random r = new Random();

        // Pick one of the first two as the opener
        Track opener = tracks.get(r.nextInt(3));

        // Shuffle all tracks, then move the opener to front
        Collections.shuffle(tracks, r);
        tracks.remove(opener);
        tracks.add(0, opener);
        currentTrackIndex = 0;
    }

    private static void addTrack(String key, String displayName, String path, float duration, float bpm) throws Exception {
        tracks.add(new Track(key, displayName, new Music(path), duration, bpm));
    }

    public static void play() {
        if (tracks.isEmpty()) return;
        Track track = getCurrentTrack();
        if (paused) {
            track.music.resume();
            paused = false;
        } else {
            track.music.play();
            track.music.setVolume(volume);
        }
        stopped = false;
    }

    public static void pause() {
        if (tracks.isEmpty() || stopped) return;
        Track track = getCurrentTrack();
        if (track.music.playing()) {
            track.music.pause();
            paused = true;
        }
    }

    public static void stop() {
        if (tracks.isEmpty()) return;
        getCurrentTrack().music.stop();
        paused = false;
        stopped = true;
    }

    public static void togglePlayPause() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public static void nextTrack() {
        if (tracks.isEmpty()) return;
        boolean wasPlaying = isPlaying();
        stop();
        currentTrackIndex = (currentTrackIndex + 1) % tracks.size();
        if (wasPlaying) play();
    }

    public static void previousTrack() {
        if (tracks.isEmpty()) return;
        boolean wasPlaying = isPlaying();
        stop();
        currentTrackIndex = (currentTrackIndex - 1 + tracks.size()) % tracks.size();
        if (wasPlaying) play();
    }

    public static void setVolume(float vol) {
        volume = Math.max(0f, Math.min(1f, vol));
        if (!tracks.isEmpty() && !stopped) {
            getCurrentTrack().music.setVolume(volume);
        }
    }

    public static float getVolume() {
        return volume;
    }

    public static void seekTo(float seconds) {
        if (tracks.isEmpty() || stopped) return;
        Track track = getCurrentTrack();
        track.music.setPosition(seconds);
    }

    public static float getPosition() {
        if (tracks.isEmpty() || stopped) return 0;
        return getCurrentTrack().music.getPosition();
    }

    public static boolean isPlaying() {
        if (tracks.isEmpty()) return false;
        return getCurrentTrack().music.playing();
    }

    public static boolean isPaused() {
        return paused;
    }

    public static boolean isStopped() {
        return stopped;
    }

    public static Track getCurrentTrack() {
        return tracks.get(currentTrackIndex);
    }

    public static int getCurrentTrackIndex() {
        return currentTrackIndex;
    }

    public static int getTrackCount() {
        return tracks.size();
    }

    public static Sound getSound(String key) {
        return sounds.get(key);
    }

    /** Returns a 0-1 pulse that peaks sharply on each beat */
    public static float getBeatPulse() {
        if (tracks.isEmpty() || stopped || !isPlaying()) return 0;
        Track track = getCurrentTrack();
        float pos = getPosition();
        float beatsPerSec = track.bpm / 60f;
        float beatPhase = (pos * beatsPerSec) % 1f;
        // Sharp attack, smooth exponential decay
        return (float) Math.pow(1.0 - beatPhase, 3.5);
    }

    public static class Track {
        public final String key;
        public final String displayName;
        public final Music music;
        public final float duration;
        public final float bpm;

        public Track(String key, String displayName, Music music, float duration, float bpm) {
            this.key = key;
            this.displayName = displayName;
            this.music = music;
            this.duration = duration;
            this.bpm = bpm;
        }
    }
}
