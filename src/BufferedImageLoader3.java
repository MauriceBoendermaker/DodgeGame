import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageLoader3
{
	
	private BufferedImage image3;
	
	public BufferedImage loadImage(String path)
	{
		try {
			image3 = ImageIO.read(getClass().getResource(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image3;
	}
	
}
//InfoPage