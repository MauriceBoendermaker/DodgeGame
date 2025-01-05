import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageLoader8
{
	
	private BufferedImage image8;
	
	public BufferedImage loadImage(String path)
	{
		try {
			image8 = ImageIO.read(getClass().getResource(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image8;
	}
	
}
//NederlandsPage