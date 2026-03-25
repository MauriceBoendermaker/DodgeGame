import java.awt.image.BufferedImage;

public class SpriteSheet {

    private BufferedImage sprite;

    public SpriteSheet(BufferedImage ss) {
        this.sprite = ss;
    }

    public BufferedImage grabImage(int col, int row, int height, int width) {
        return sprite.getSubimage((row * 720) - 720, (col * 1280) - 1280, width, height);
    }
}
