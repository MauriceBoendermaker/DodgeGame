import java.awt.image.BufferedImage;

public class SpriteSheet3 {
	
	private BufferedImage sprite3;
	
	public SpriteSheet3 (BufferedImage ss3){
		this.sprite3 = ss3;
	}

	public BufferedImage grabImage(int col, int row, int heigth, int width){
		BufferedImage img3 = sprite3.getSubimage((row * 720) - 720, (col * 1280 - 1280), width, heigth);
		return img3;
	}
}
//InfoPage