package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JPanel;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;


public class DungeonApp extends JPanel implements KeyListener {
	
	public MiniDungeon dungeon  ;
	String msgFromTheGame = "" ;
	
	int panelWidth = 600 ;
	int panelHeight = 600 ;
	int txtCharSize = 16 ;
	int topMargin = 50 ;
	int leftMargin = 50 ;
	
	Font consoleFont = new Font("Courier New", Font.BOLD, txtCharSize);
	
	public DungeonApp() {
		super() ;
		restart() ;
		// dungeon.showConsoleIO = false ;
		addKeyListener(this);
	}
	
	void restart() {
		dungeon = new MiniDungeon(20,4,5,4,3) ;
		msgFromTheGame = "" ;
	}
	
	@Override
	public void addNotify(){
		super.addNotify();
		requestFocus();
	}
	
	@Override
	public void keyTyped(KeyEvent e) { }


	@Override
	public void keyReleased(KeyEvent e) { }
	
	boolean validCharCommand(char c) {
		return c == 'w' || c == 'a' || c == 's' || c == 'd' || c == 'u' || c == 'r' ;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		char key = e.getKeyChar() ;
		if(! validCharCommand(key)) return ;
		if (key == 'r') {
			restart() ;
		}
		else {
			if (dungeon.status == GameStatus.INPROGRESS)
				this.msgFromTheGame = dungeon.doCommand("" + key) ;
		}
		repaint() ;
	}
	
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
		var lines = dungeon.toString().lines().collect(Collectors.toList()) ;
		lines.add("Commands: wasd: to move | u: use potion") ;
		lines.add("") ;
		lines.addAll(msgFromTheGame.lines().collect(Collectors.toList())) ;
		int k = 0 ;
		for(String r : lines) {
			gr.drawString(r,leftMargin, topMargin + k*txtCharSize) ;
			k++ ;
		}
		
	}
	
	public static void main(String[] args) {
		
		DungeonApp sc = new DungeonApp();
		
		JFrame frame = new JFrame("Mini Dungeon");
		
		frame.add(sc);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
