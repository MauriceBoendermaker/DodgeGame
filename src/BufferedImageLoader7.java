import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageLoader7
{
	
	private BufferedImage image7;
	
	public BufferedImage loadImage(String path)
	{
		try {
			image7 = ImageIO.read(getClass().getResource(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image7;
	}
	
}
//EnglishPage