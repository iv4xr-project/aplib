package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;


public class DungeonApp extends JPanel implements KeyListener {
	
	public MiniDungeon dungeon  ;
	String msgFromTheGame = "" ;
	
	int yMargin = 20 ;
	int xMargin = 20 ;
	
	/**
	 * Image containing the sprites for drawing the game (pic of monsters, walls etc). Each
	 * sprite is a picture of some size NxN pixels, and arranged as tiles in the Image.
	 */
	Image sprites ;

	/**
	 * The size of each sprite in the sprite-image, e.g. 32x32 pixels.
	 */
	int origSpriteWidth = 32 ;
	
	/**
	 * The displayed size of each sprite. e.g. 16x16 pixels. This could be different than 
	 * the original size of the sprite, in which case scaling is applied.
	 */
	int scaledSpriteWidtgh = 24 ;
	
	int panelWidth ;
	int panelHeight ;

	int txtCharSize = 16 ;
	Font consoleFont = new Font("Courier New", Font.BOLD, txtCharSize);
	
	/**
	 * If true will disable interaction with physcal keys; useful when we want an
	 * algorithms instead of humans to play the game.
	 */
	public boolean disableKey = false ;
	
	// sounds:
	Clip soundWelcome ;
	Clip soundExit ;
	Clip soundAlmostDie ;
	Clip soundDie ;

	public DungeonApp(MiniDungeonConfig config) throws Exception {
		super() ;
		
		// loading the sprites:
		Toolkit t=Toolkit.getDefaultToolkit();  
		Path p = Path.of("assets","NethackImages.png") ;
        sprites = t.getImage(p.toString()); 
        
		restart(config) ;
		
        // calculating dimensions:
        panelWidth = scaledSpriteWidtgh*config.worldSize*2 + 4*xMargin ;
        panelHeight = scaledSpriteWidtgh*config.worldSize + 3*yMargin + txtCharSize*14 ; // 14 lines text...
		
        // loading sounds:
        p = Path.of("assets","welcome.wav") ;  
        File f = new File(p.toString()) ;
        AudioInputStream ais = AudioSystem.getAudioInputStream(f);
        soundWelcome = AudioSystem.getClip() ;
        soundWelcome.open(ais);
        
        p = Path.of("assets","exit.wav") ;  
        f = new File(p.toString()) ;
        ais = AudioSystem.getAudioInputStream(f);
        soundExit = AudioSystem.getClip() ;
        soundExit.open(ais);
        
        p = Path.of("assets","death.wav") ;  
        f = new File(p.toString()) ;
        ais = AudioSystem.getAudioInputStream(f);
        soundAlmostDie = AudioSystem.getClip() ;
        soundAlmostDie.open(ais);
        
        p = Path.of("assets","die.wav") ;  
        f = new File(p.toString()) ;
        ais = AudioSystem.getAudioInputStream(f);
        soundDie = AudioSystem.getClip() ;
        soundDie.open(ais);
        
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
			soundWelcome.setMicrosecondPosition(0);
			soundWelcome.start();
		}
		if (key == 'q') {
			closeAllSounds() ;
			System.exit(0);
		}
		else {
			if (dungeon.status == GameStatus.INPROGRESS) {
				int turnNrWas = dungeon.turnNr ;
				int frodoAreaWas = dungeon.frodo().mazeId ;
				int smeagolAreaWas = -1 ;
				int frodoHPWas = dungeon.frodo().hp ;
				int smeagolHPWas = -1 ;
				
				if (dungeon.config.enableSmeagol) {
					smeagolAreaWas = dungeon.smeagol().mazeId ;
					smeagolHPWas = dungeon.smeagol().hp ;
				}
				
				this.msgFromTheGame = dungeon.doCommand(key) ;
				
				// logic for playing sound:
				int frodoAreaNow = dungeon.frodo().mazeId ;
				int smeagolAreaNow = -1 ;
				if (dungeon.config.enableSmeagol) {
					smeagolAreaNow = dungeon.smeagol().mazeId ;
				}
				if ((frodoAreaWas != frodoAreaNow) || (smeagolAreaWas != smeagolAreaNow)) {
					soundExit.setMicrosecondPosition(0);
					soundExit.start();
				}
				else if (turnNrWas != dungeon.turnNr) {
					Player frodo = dungeon.frodo() ;
					Player smeagol = dungeon.smeagol() ;
					if((frodoHPWas > 0 && frodo.hp <= 0)
							|| (smeagol != null && smeagolHPWas>0 && smeagol.hp<=0)
							) {
						soundDie.stop();
						soundDie.setMicrosecondPosition(0);
						soundDie.start();
						
					}
					if ((frodo.hp>0 && frodo.hp<5) ||
							(smeagol != null && smeagol.hp>0 && smeagol.hp<5)) {
						// almost dead:
						soundAlmostDie.stop();
						soundAlmostDie.setMicrosecondPosition(0);
						soundAlmostDie.start();
						
					}
				}
				
			}
		}
		repaint() ;
	}
	
	void closeAllSounds() {
		soundWelcome.close();
		soundExit.close(); 
		soundAlmostDie.close();
	}
	
	@Override
	public Dimension getPreferredSize(){
		return new Dimension(panelWidth, panelHeight);
	}
	
	
	void paintSprite(Graphics gr, int leftCornerXOnCanvas, int leftCornerYOnCanvas, int spriteIndexX, int spriteIndexY) {
		int xS1 = spriteIndexX * origSpriteWidth ;
		int yS1 = spriteIndexY * origSpriteWidth ;
		int xS2 = xS1 + origSpriteWidth - 1 ;
		int yS2 = yS1 + origSpriteWidth - 1 ;
		int xx2 = leftCornerXOnCanvas + scaledSpriteWidtgh - 1 ;
		int yy2 = leftCornerYOnCanvas + scaledSpriteWidtgh - 1 ;
		gr.drawImage(sprites,leftCornerXOnCanvas,leftCornerYOnCanvas,xx2,yy2,xS1,yS1,xS2,yS2,this) ;
	}
	
	List<Entity> neighbors(Entity[][] world, int x, int y) {
		List<Entity> neighbors = new LinkedList<>() ;
		if (x>0) 
			neighbors.add(world[x-1][y]) ;
		if (x<world.length-1)
			neighbors.add(world[x+1][y]) ;
		if (y>0)
			neighbors.add(world[x][y-1]) ;
		if (y<world.length-1)
			neighbors.add(world[x][y+1]) ;
		return neighbors ;
	}
	
	/**
	 * Draw the entity in world[x][y] on the screen's canvas. 
	 * @param onSecondMap If true the entity is drawn on the second-map, else on the first-map.
	 */
	void drawTile(Graphics gr, Entity[][] world, int x, int y, boolean onSecondMap) {
		
		int mapOrigX = xMargin ;
		int mapOrigY = yMargin ;
		
		if(onSecondMap) 
			mapOrigX += dungeon.config.worldSize*scaledSpriteWidtgh + 2*xMargin ;
		
		int yy = dungeon.config.worldSize - 1 - y ;
		int yy1 = mapOrigY + yy*scaledSpriteWidtgh ;
		int xx1 = mapOrigX + x*scaledSpriteWidtgh ;
		
		Entity e = world[x][y] ;
		if (e == null) {
			int spriteIndexX = 29 ;
			int spriteIndexY = 21 ;
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
		else if (e instanceof Wall) {
			int N = dungeon.config.worldSize ;
			int spriteIndexX = 11 ;
			int spriteIndexY = 21 ;
			int wallNeighbours = (int) neighbors(world,x,y).stream().filter(f -> f instanceof Wall).count() ;
			if (wallNeighbours == 3) {
				// A T-split
				//spriteIndexX = 36 ;
				//spriteIndexY = 32 ;
				spriteIndexX = 22 ;
				spriteIndexY = 21 ;
			}
			if (wallNeighbours == 1) {
				// A wall jutting end
				spriteIndexX = 0 ;
				spriteIndexY = 26 ;
			}
			if(((x==0 && y==0) || (x==0 && y==N-1) || (x==N-1 && y==0) || (x==N-1 && y==N-1))
			    && world[x][y] instanceof Wall) {
				// corners
				spriteIndexX = 22 ;
				spriteIndexY = 21 ;
			}
			
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
		else if (e instanceof HealingPotion) {
			int spriteIndexX = 32 ;
			int spriteIndexY = 16 ;
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
		else if (e instanceof RagePotion) {
			int spriteIndexX = 30 ;
			int spriteIndexY = 16 ;
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
		else if (e instanceof Scroll) {
			int spriteIndexX = 0 ;
			int spriteIndexY = 21 ;
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
		else if (e instanceof Frodo) {
			int spriteIndexX = 38 ;
			int spriteIndexY = 12 ;
			var player = (Player) e ;
			if (player.hp <= 5) {
				spriteIndexX = 37 ;
				spriteIndexY = 12 ;
			}
			else if(player.rageTimer>0) {
				spriteIndexX = 36 ;
				spriteIndexY = 12 ;
			}
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
		else if (e instanceof Smeagol) {
			int spriteIndexX = 1 ;
			int spriteIndexY = 32 ;
			var player = (Player) e ;
			if (player.hp <= 5) {
				spriteIndexX = 3 ;
				spriteIndexY = 5 ;
			}
			else if(player.rageTimer>0) {
				spriteIndexX = 1 ;
				spriteIndexY = 5 ;
			}
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
		else if (e instanceof Monster) {
			int spriteIndexX = 2 ;//4 ; 
			int spriteIndexY = 0 ; //27 ;
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
		else if (e instanceof Shrine) {
			var sh = (Shrine) e ;
			int spriteIndexX = 38 ;
			int spriteIndexY = 21 ;
			if (sh.cleansed) {
				spriteIndexX = 20 ;
				spriteIndexY = 22 ;
			}
			paintSprite(gr,xx1,yy1,spriteIndexX,spriteIndexY) ;
		}
	}
	
	boolean loaded = false ;
	
	@Override
	public void paintComponent(Graphics gr){
        super.paintComponent(gr);
		gr.setColor(Color.black);
		gr.fillRect(0, 0, panelWidth, panelHeight);
		
		int mapOrigX = xMargin ;
		int mapOrigY = yMargin ;
		
		var world = dungeon.currentMaze(dungeon.frodo()).world ;
		Entity[][] world2 = null ;
		if (dungeon.config.enableSmeagol) {
			world2 = dungeon.currentMaze(dungeon.smeagol()).world ;
		}
		for(int row = dungeon.config.worldSize-1 ; 0<=row; row--) {
			for(int x = 0; x < dungeon.config.worldSize; x++) {
				boolean isVisible = 
						dungeon.isVisible(dungeon.frodo(),dungeon.frodo().mazeId,x,row) 
						|| (dungeon.config.enableSmeagol && world2==world 
						    && dungeon.isVisible(dungeon.smeagol(),dungeon.smeagol().mazeId,x,row)) ;
				
				if (isVisible) {
					drawTile(gr,world,x,row,false) ;
				}
			}
			if (world2 != null && world2 != world) {
				for(int x = 0; x < dungeon.config.worldSize; x++) {
					if (dungeon.isVisible(dungeon.smeagol(),dungeon.smeagol().mazeId,x,row)) {
						drawTile(gr,world2,x,row,true) ;
					}
				}
			}
		}
		
		gr.setColor(Color.white) ;
		gr.setFont(consoleFont);
		//var lines = dungeon.toString().lines().collect(Collectors.toList()) ;
		List<String> lines = new LinkedList<>() ;
		lines = dungeon.showGameStatus().lines().collect(Collectors.toList()) ;
		lines.add("Commands:") ;
		lines.add("Frodo:   wasd to move | e:use-healpot | e:use-ragepot") ;
		lines.add("Smaegol: ijkl to move | o:use-healpot | p:use-ragepot") ;
		lines.add("q:quit | z:restart") ;
		lines.add("") ;
		lines.addAll(msgFromTheGame.lines().collect(Collectors.toList())) ;
		int k = 0 ;
		int yStartText = dungeon.config.worldSize*scaledSpriteWidtgh + 2*yMargin ;
		for(String r : lines) {
			gr.drawString(r,xMargin, yStartText + k*txtCharSize) ;
			k++ ;
		}
		
		if(!loaded) {
			soundWelcome.start();
			loaded = true ;
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
	
	public static void main(String[] args) throws Exception {		
		MiniDungeonConfig config = new MiniDungeonConfig() ;
		config.numberOfMonsters = 30 ;
		config.numberOfHealPots = 10 ;
		config.numberOfRagePots = 10 ;
		config.viewDistance = 6 ;
		System.out.println(">>> Configuration:\n" + config) ;
		deploy(new DungeonApp(config));
	}

}
