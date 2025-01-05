//import java.util.HashMap;
//import java.util.Map;
//
//import org.newdawn.slick.Music;
//import org.newdawn.slick.SlickException;
//import org.newdawn.slick.Sound;
//
//public class AudioPlayer {
//	
//	public static Map<String, Sound> soundMap = new HashMap<String, Sound>();
//	public static Map<String, Music> musicMap = new HashMap<String, Music>();
//	public static Map<String, Music> musicMap1 = new HashMap<String, Music>();
//	
//	//Music_Player
//	public static Map<String, Music> musicMap2 = new HashMap<String, Music>();
//	public static Map<String, Music> musicMap3 = new HashMap<String, Music>();
//	
//	public static void load(){
//		
//		try {
//			soundMap.put("click_sound", new Sound("res/click_sound.ogg"));
//			
//			musicMap1.put("game_music", new Music("res/game_music.ogg"));
//			
//			musicMap.put("menu_music", new Music("res/menu_music.ogg"));
//			
//			//Music_Player audio:
//			musicMap2.put("music1_music", new Music("res/Arc-North-Meant-To-Be-_feat.-Krista-Marina_-_RetroVision-Remix_.ogg"));
//			
//			musicMap3.put("music2_music", new Music("res/Arc-North-Meant-To-Be-_feat.-Krista-Marina_-_Lyric-Video_.ogg"));
//			
//		} catch (SlickException e) {
//			
//			e.printStackTrace();
//		}
//		
//	}
//
//	public static Music getMusic(String key){
//		return musicMap.get(key);
//	}
//	
//	public static Sound getSound(String key){
//		return soundMap.get(key);
//	}
//	
//	public static Music getMusic1(String key){
//		return musicMap1.get(key);
//	}
//	
//	//Music_Player:
//	public static Music getMusic2(String key){
//		return musicMap2.get(key);
//	}
//	
//	public static Music getMusic3(String key){
//		return musicMap3.get(key);
//	}
//}
