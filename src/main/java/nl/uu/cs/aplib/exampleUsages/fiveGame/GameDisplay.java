package nl.uu.cs.aplib.exampleUsages.fiveGame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Non-interactive window to displaye the state of a FiveGame.
 */
public class GameDisplay extends JPanel implements KeyListener {
	
	public FiveGame thegame ;
	
	int panelWidth = 600 ;
	int panelHeight = 300 ;
	int txtCharSize = 16 ;
	int topMargin = 50 ;
	int leftMargin = 50 ;
	Font consoleFont = new Font("Courier New", Font.BOLD, txtCharSize) ;
	
	GameDisplay() {
		super() ;
	}

	@Override
	public void keyTyped(KeyEvent e) {	}

	@Override
	public void keyPressed(KeyEvent e) { }

	@Override
	public void keyReleased(KeyEvent e) { }
	
	@Override
	public Dimension getPreferredSize(){
		return new Dimension(panelWidth, panelHeight);
	}
	
	@Override
	public void paintComponent(Graphics gr){
        super.paintComponent(gr);
		gr.setColor(Color.black);
		gr.fillRect(0, 0, panelWidth, panelHeight);
		gr.setColor(Color.white) ;
		gr.setFont(consoleFont);
		var str = thegame.toString() + "\n" + thegame.toStringShort() ;
		var lines = str.lines().collect(Collectors.toList()) ;
		int k = 0 ;
		for(String r : lines) {
			gr.drawString(r,leftMargin, topMargin + k*txtCharSize) ;
			k++ ;
		}			
	}
	
	public static GameDisplay makeDisplay(FiveGame thegame) {
		GameDisplay niceDisplay = new GameDisplay() ;
		niceDisplay.thegame = thegame ;
	    JFrame frame = new JFrame("FiveGame");
		frame.add(niceDisplay);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		return niceDisplay ;
	}

}
