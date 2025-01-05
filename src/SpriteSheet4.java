import java.awt.image.BufferedImage;

public class SpriteSheet4 {
	
	private BufferedImage sprite4;
	
	public SpriteSheet4 (BufferedImage ss4){
		this.sprite4 = ss4;
	}

	public BufferedImage grabImage(int col, int row, int heigth, int width){
		BufferedImage img4 = sprite4.getSubimage((row * 720) - 720, (col * 1280 - 1280), width, heigth);
		return img4;
	}	
}
//HelpPage