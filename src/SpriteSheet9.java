import java.awt.image.BufferedImage;

public class SpriteSheet9 {
	
	private BufferedImage sprite9;
	
	public SpriteSheet9 (BufferedImage ss9){
		this.sprite9 = ss9;
	}

	public BufferedImage grabImage(int col, int row, int heigth, int width){
		BufferedImage img9 = sprite9.getSubimage((row * 720) - 720, (col * 1280 - 1280), width, heigth);
		return img9;
	}	
}
//Deutsch