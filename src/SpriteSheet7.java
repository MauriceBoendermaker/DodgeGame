import java.awt.image.BufferedImage;

public class SpriteSheet7 {
	
	private BufferedImage sprite7;
	
	public SpriteSheet7 (BufferedImage ss7){
		this.sprite7 = ss7;
	}

	public BufferedImage grabImage(int col, int row, int heigth, int width){
		BufferedImage img7 = sprite7.getSubimage((row * 720) - 720, (col * 1280 - 1280), width, heigth);
		return img7;
	}	
}
//English