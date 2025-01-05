import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageLoader6
{
	
	private BufferedImage image6;
	
	public BufferedImage loadImage(String path)
	{
		try {
			image6 = ImageIO.read(getClass().getResource(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image6;
	}
	
}
//AboutPage