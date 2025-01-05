import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageLoader2
{
	
	private BufferedImage image2;
	
	public BufferedImage loadImage(String path)
	{
		try {
			image2 = ImageIO.read(getClass().getResource(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image2;
	}
	
}
//PlayPage