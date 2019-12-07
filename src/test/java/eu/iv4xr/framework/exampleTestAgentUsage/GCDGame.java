package eu.iv4xr.framework.exampleTestAgentUsage;

import java.util.Random;

/**
 * A simple game as an example of "Program under Test". This class implements a
 * game called GCD-game. The game is played on an imaginary 2D grid space. The
 * game maintains three variebles/fields: (x,y,gcd). The fields x,y represent
 * the current position of the player in the 2D grid. Any non-negative x,y are
 * valid. The greatest common divisor of these x and y is stored in the field
 * gcd. The player can move up, down, left or right, one square at at time. The
 * player wins if he/she manage to find a square whose greatest common divisor
 * is 1 (in other words, if x and y are relative prime to each other).
 */
public class GCDGame {
	
	int x ;
	int y ;
	int gcd ;
		
	/**
	 * Create an instance of the game. The player is placed in a random square.
	 */
	public GCDGame() {
		Random rnd = new Random() ;
		x = rnd.nextInt(19) + 1 ;
		y = rnd.nextInt(19) + 1 ;
		gcd = Calculate_GCD(x,y) ;
	}
	
	/**
	 * Move the player one square up.
	 */
	public void up() { y++ ; gcd = Calculate_GCD(x,y) ; }
	
	/**
	 * Move the player one square down.
	 */
	public void down() { if (y>0) y-- ; gcd = Calculate_GCD(x,y) ; }
	
	public void right() { x++ ; gcd = Calculate_GCD(x,y) ; }
	public void left() { if (x>0) x-- ; gcd = Calculate_GCD(x,y) ; }
	
	private int Calculate_GCD(int x,int y) {
		if (x==y) return x ;
		if (x==1 || y==1) return 1 ;
		if (y != 0)
           return Calculate_GCD(y, x % y);
        else
    	   return x;
	}
	
	/**
	 * True if the greatest common divisor of x and y is 1.
	 */
	public boolean win() { return gcd == 1 ; }
}