import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageLoader9
{
	
	private BufferedImage image9;
	
	public BufferedImage loadImage(String path)
	{
		try {
			image9 = ImageIO.read(getClass().getResource(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image9;
	}
	
}
//DeutschPage