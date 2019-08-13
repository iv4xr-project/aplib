package nl.uu.cs.aplib.ExampleUsages.FiveGame;

import nl.uu.cs.aplib.ExampleUsages.FiveGame.FiveGame.GAMESTATUS;
import nl.uu.cs.aplib.ExampleUsages.FiveGame.FiveGame.SQUARE;
import nl.uu.cs.aplib.MainConcepts.Environment;

public class FiveGameEnv extends Environment {
	
	FiveGame thegame ;
	
	public FiveGameEnv() { super() ; }
	public FiveGameEnv attachGame(FiveGame g) {
		thegame = g ; return this ;
	}
	
	public GAMESTATUS move(SQUARE ty, int x, int y) {
	    thegame.move(ty, x, y) ;
	    return thegame.getGameStatus() ;
	}
}
