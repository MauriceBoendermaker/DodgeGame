import java.awt.Dimension;

import javax.swing.JFrame;

public class Window {

    public Window(int width, int height, String title, Game game) {
        JFrame frame = new JFrame(title);

        // Set canvas size — frame will size around it
        Dimension size = new Dimension(width, height);
        game.setPreferredSize(size);
        game.setMinimumSize(size);
        game.setMaximumSize(size);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        game.start();
    }
}
