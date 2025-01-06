package eu.iv4xr.framework.extensions.mbt;

import eu.iv4xr.framework.spatial.IntVec2D;

public class SimpleGame {
	
	static int NORTH = 0 ;
	static int EAST = 1 ;
	static int SOUTH = 2 ;
	static int WEST = 3;
	
	public IntVec2D goal = new IntVec2D(4,4) ;
	public IntVec2D pos = new IntVec2D(0,0) ;
	public int orientation = NORTH ; // NORTH
	public boolean win = false ;
	public int score = 0 ;
	
	public void turnR() {
		System.out.println("#  turn right.") ;
		orientation = (orientation + 1) % 4 ;
	}
	
	public void turnL() {
		System.out.println("#  turn left.") ;
		orientation = orientation==0 ? 3 : orientation-1 ;
	}
	
	public void moveForward() throws Exception {
		if (win)
			return ;
		IntVec2D pos2 = new IntVec2D(pos.x,pos.y) ;
		switch(orientation) {
			case 0 : pos2.y++ ; break ;
			case 1 : pos2.x++ ; break ;
			case 2 : pos2.y-- ; break ;
			default: pos2.x-- ; 
		}
		System.out.println("#  move forward to " + pos2) ;
		if (pos2.x < 0 || pos2.x > goal.x || pos2.y<0 || pos2.y > goal.y) {
			System.out.println("## INVALID MOVE!") ;
			throw new Exception("INVALID MOVE!") ;
		}
		pos = pos2 ;
		if (pos.equals(goal)) {
			System.out.println("## YOU WIN!") ;
			win = true ;
		}
	}
	
	public static void main(String[] args) throws Exception {
		var G = new SimpleGame() ;
		G.moveForward(); G.moveForward(); G.moveForward(); G.moveForward();
		G.turnR();
		G.moveForward(); G.moveForward(); G.moveForward(); G.moveForward();
		
		
	}

}
