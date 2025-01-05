import java.awt.image.BufferedImage;

public class SpriteSheet6 {
	
	private BufferedImage sprite6;
	
	public SpriteSheet6 (BufferedImage ss6){
		this.sprite6 = ss6;
	}

	public BufferedImage grabImage(int col, int row, int heigth, int width){
		BufferedImage img6 = sprite6.getSubimage((row * 720) - 720, (col * 1280 - 1280), width, heigth);
		return img6;
	}	
}
//UpdatesPage