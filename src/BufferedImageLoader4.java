import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageLoader4
{
	
	private BufferedImage image4;
	
	public BufferedImage loadImage(String path)
	{
		try {
			image4 = ImageIO.read(getClass().getResource(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image4;
	}
	
}
//HelpPage