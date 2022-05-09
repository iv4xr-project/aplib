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
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;


public class DungeonApp extends JPanel implements KeyListener {
	
	public MiniDungeon dungeon  ;
	String msgFromTheGame = "" ;
	
	int panelWidth = 700 ;
	int panelHeight = 600 ;
	int txtCharSize = 16 ;
	int topMargin = 50 ;
	int leftMargin = 50 ;
	
	Font consoleFont = new Font("Courier New", Font.BOLD, txtCharSize);
	
	/**
	 * If true will disable interaction with physcal keys; useful when we want an
	 * algorithms instead of humans to play the game.
	 */
	public boolean disableKey = false ;
	

	public DungeonApp(MiniDungeonConfig config) {
		super() ;
		restart(config) ;
		// dungeon.showConsoleIO = false ;
		addKeyListener(this);
	}
	
	void restart(MiniDungeonConfig config) {	
		dungeon = new MiniDungeon(config) ;
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
	
	char[] validCharCommand = { 'w', 'a', 's', 'd', 'e', 'r', 
			'i', 'j', 'k', 'l', 'o', 'p',
			'q', 'z' } ;
	
	boolean validCharCommand(char c) {
		for(char d : validCharCommand) {
			if (c==d) return true ;
		}
		return false ;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (disableKey) return ;
		char key = e.getKeyChar() ;
		keyPressedWorker(key) ;
	}
	
	public void keyPressedWorker(char key) {
		if(! validCharCommand(key)) return ;
		var config = dungeon.config ;
		if (key == 'z') {
			restart(config) ;
		}
		if (key == 'q') {
			System.exit(0);
		}
		else {
			if (dungeon.status == GameStatus.INPROGRESS)
				this.msgFromTheGame = dungeon.doCommand(key) ;
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
		lines.add("Commands:") ;
		lines.add("Frodo:   wasd to move | e:use-healpot | e:use-ragepot") ;
		lines.add("Smaegol: ijkl to move | o:use-healpot | p:use-ragepot") ;
		lines.add("q:quit | z:restart") ;
		lines.add("") ;
		lines.addAll(msgFromTheGame.lines().collect(Collectors.toList())) ;
		int k = 0 ;
		for(String r : lines) {
			gr.drawString(r,leftMargin, topMargin + k*txtCharSize) ;
			k++ ;
		}
		
	}
	
	/**
	 * To launch a DungeonApp in a window.
	 */
	public static JFrame deploy(DungeonApp app) {
		JFrame frame = new JFrame("Mini Dungeon");
		frame.add(app);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		return frame ;
	}
	
	public static void main(String[] args) {		
		MiniDungeonConfig config = new MiniDungeonConfig() ;
		//config.viewDistance = 4 ;
		System.out.println(">>> Configuration:\n" + config) ;
		deploy(new DungeonApp(config));
	}

}
