import java.awt.image.BufferedImage;

public class SpriteSheet2 {
	
	private BufferedImage sprite2;
	
	public SpriteSheet2 (BufferedImage ss2){
		this.sprite2 = ss2;
	}

	public BufferedImage grabImage(int col, int row, int heigth, int width){
		BufferedImage img2 = sprite2.getSubimage((row * 720) - 720, (col * 1280 - 1280), width, heigth);
		return img2;
	}
	
}
//PlayPage