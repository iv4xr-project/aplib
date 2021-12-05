package nl.uu.cs.aplib.exampleUsages.fiveGame;

import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.GAMESTATUS;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.SQUARE;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.utils.Pair;

public class FiveGameEnv extends Environment {

    FiveGame thegame;

    // variables for keeping track relevant part of FiveGame's state:
    //int boardsize;
    //SQUARE[][] board;
    //Square_ lastmove;

    public FiveGameEnv() {
        super();
    }

    public FiveGameEnv attachGame(FiveGame g) {
        thegame = g;
        //board = g.getState();
        //boardsize = g.boardsize;
        return this;
    }

    @Override
    protected Object sendCommand_(EnvOperation cmd) {
    	
    	switch (cmd.command) {
    	   case "move" : 
    		   Object[] arg_ = (Object[]) cmd.arg;
               thegame.move((SQUARE) arg_[0], (int) arg_[1], (int) arg_[2]);
               return thegame.getGameStatus();
    	   case "observe" : 
    		   return new Pair<GAMESTATUS, SQUARE[][]>(thegame.getGameStatus(), thegame.board) ;
    	}
    	throw new IllegalArgumentException();
    }
    
    @Override
    public Pair<GAMESTATUS, SQUARE[][]> observe(String agentId) {
        return (Pair<GAMESTATUS, SQUARE[][]>) this.sendCommand("ANONYMOUS", null, "observe", null) ;
    }


    public GAMESTATUS move(SQUARE ty, int x, int y) {
        Object[] arg = { ty, (Integer) x, (Integer) y };
        var o = sendCommand("ANONYMOUS", null, "move", arg);
        return (GAMESTATUS) o;
    }

}
