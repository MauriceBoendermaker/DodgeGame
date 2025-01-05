import java.awt.image.BufferedImage;

public class SpriteSheet5 {
	
	private BufferedImage sprite5;
	
	public SpriteSheet5 (BufferedImage ss5){
		this.sprite5 = ss5;
	}

	public BufferedImage grabImage(int col, int row, int heigth, int width){
		BufferedImage img5 = sprite5.getSubimage((row * 720) - 720, (col * 1280 - 1280), width, heigth);
		return img5;
	}	
}
//UpdatesPage