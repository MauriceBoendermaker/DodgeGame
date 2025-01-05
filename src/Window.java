import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

public class Window extends Canvas{

	private static final long serialVersionUID = -4241816582633136533L;
	
	public Window(int width2, int height2, /*int width, int height,*/ String title, Game game){
		
		JFrame frame = new JFrame(title);
	
		Dimension d = new Dimension(400, 40); //400, 40
		
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
		frame.setSize(720, 1280);
        frame.setSize(d);
        //Display the window.
        frame.setVisible(true);
		
		frame.setPreferredSize(new Dimension(1280, 720));
		frame.setMaximumSize(new Dimension(1280, 720));
		frame.setMinimumSize(new Dimension(1280, 720));
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false); //Resizable? - false = no, true = yes
		frame.setLocationRelativeTo(null);
		frame.add(game);
		
	    frame.setVisible(true);
	    //frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
	    
		game.start();
	}
	
}
