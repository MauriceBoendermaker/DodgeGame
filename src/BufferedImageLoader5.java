import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageLoader5
{
	
	private BufferedImage image5;
	
	public BufferedImage loadImage(String path)
	{
		try {
			image5 = ImageIO.read(getClass().getResource(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image5;
	}
	
}
//UpdatesPage