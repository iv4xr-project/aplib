package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.util.Scanner;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

/**
 * A Command-line based app for MiniDungeon.
 * 
 * @author Wish
 *
 */
public class DungeonCommandLineApp {
	
	//static public boolean showConsoleIO = true ;
	
	static char toChar(Entity e) {
		if (e == null) return '.' ; 
		switch(e.type) {
		    case WALL    : return '#' ;
		    case FRODO   : return '@' ;
		    case SMEAGOL : return '&' ;
		    case MONSTER : return 'm' ;
		    case HEALPOT : return '%' ;
		    case RAGEPOT : return '!' ;
		    case SCROLL  : return '?' ;
		    case SHRINE  : return 'S' ;
		}
		/*
		if (e instanceof Door) {
			Door d = (Door) e ;
			if (d.isOpen) return '-' ;
			return 'X' ;
		}
		*/
		throw new IllegalArgumentException() ;		
	}
	
	static void consolePrint(String s) {
		//if (showConsoleIO) 
		System.out.println(s) ;
	}
	
	static public String showMap(MiniDungeon theGame) {
			float viewDistanceSq = theGame.config.viewDistance * theGame.config.viewDistance ;
			StringBuffer z = new StringBuffer() ;
			var world = theGame.currentMaze(theGame.frodo()).world ;
			Entity[][] world2 = null ;
			if (theGame.config.enableSmeagol) {
				world2 = theGame.currentMaze(theGame.smeagol()).world ;
			}
			for(int row = theGame.config.worldSize-1 ; 0<=row; row--) {
				for(int x = 0; x<theGame.config.worldSize; x++) {
					boolean isVisible = 
							theGame.isVisible(theGame.frodo(),theGame.frodo().mazeId,x,row) 
							|| (theGame.config.enableSmeagol 
									&& world2==world 
									&& theGame.isVisible(theGame.smeagol(),theGame.smeagol().mazeId,x,row)) ;
					
					if (isVisible) {
						z.append(toChar(world[x][row])) ;					
					}
					else 
						z.append(" ") ;
				}
				if (world2 != null && world2 != world) {
					z.append("    ") ;
					for(int x = 0; x<theGame.config.worldSize; x++) {
						if (theGame.isVisible(theGame.smeagol(),theGame.smeagol().mazeId,x,row)) {
							z.append(toChar(world2[x][row])) ;					
						}
						else 
							z.append(" ") ;
					}
				}
				z.append("\n") ;
			}
			z.append(theGame.showGameStatus()) ;
			
			return z.toString() ;
	}
	
	/**
	 * An instance of the game with just simple console.
	 */
	public static void main(String[] args) {
		
		MiniDungeon dg = new MiniDungeon(new MiniDungeonConfig()) ;
		dg.config.viewDistance = 4 ;
		var scanner = new Scanner(System.in) ;
		while(dg.status == GameStatus.INPROGRESS) {
			consolePrint(showMap(dg)) ;
			consolePrint("Commands Frodo: wasd | e:use-healpot | r:use-ragepot") ;
			consolePrint("       Smeagol: ijkl | o:use-healpot | p:use-ragepot") ;
			String cmd = scanner.nextLine() ;
			char[] commands = cmd.toCharArray() ;
			for (int c=0; c<commands.length; c++) {
				String msg = dg.doCommand(commands[c]) ;
				if (msg!=null && msg!="") consolePrint(msg) ;
				if(dg.status != GameStatus.INPROGRESS) return ;
			}
		}
	}

}
