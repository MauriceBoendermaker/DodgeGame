import java.awt.Dimension;

import javax.swing.JFrame;

public class Window {

    public Window(int width, int height, String title, Game game) {
        JFrame frame = new JFrame(title);

        Dimension size = new Dimension(width, height);
        frame.setPreferredSize(size);
        frame.setMaximumSize(size);
        frame.setMinimumSize(size);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.add(game);
        frame.setVisible(true);

        game.start();
    }
}
