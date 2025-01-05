import java.awt.image.BufferedImage;

public class SpriteSheet8 {
	
	private BufferedImage sprite8;
	
	public SpriteSheet8 (BufferedImage ss8){
		this.sprite8 = ss8;
	}

	public BufferedImage grabImage(int col, int row, int heigth, int width){
		BufferedImage img8 = sprite8.getSubimage((row * 720) - 720, (col * 1280 - 1280), width, heigth);
		return img8;
	}	
}
//Nederlands