package nl.uu.cs.aplib.exampleUsages.fiveGame;

import static nl.uu.cs.aplib.agents.PrologReasoner.and;
import static nl.uu.cs.aplib.agents.PrologReasoner.not;
import static nl.uu.cs.aplib.agents.PrologReasoner.or;
import static nl.uu.cs.aplib.agents.PrologReasoner.predicate;
import static nl.uu.cs.aplib.agents.PrologReasoner.rule;
import static nl.uu.cs.aplib.exampleUsages.fiveGame.Strategies.addStrategyRules;
import static nl.uu.cs.aplib.exampleUsages.fiveGame.Strategies.blockMove;
import static nl.uu.cs.aplib.exampleUsages.fiveGame.Strategies.blocked;
import static nl.uu.cs.aplib.exampleUsages.fiveGame.Strategies.occupied;

import alice.tuprolog.InvalidTheoryException;
import nl.uu.cs.aplib.agents.PrologReasoner;
import nl.uu.cs.aplib.agents.PrologReasoner.PredicateName;
import nl.uu.cs.aplib.agents.PrologReasoner.Rule;
import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.SQUARE;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame_withAgent.MyState;
import nl.uu.cs.aplib.mainConcepts.Environment;

/**
 * Strategies for playing FiveGame. They are programmed in Prolog and will be
 * used by our aplib-agent later in {@link FiveGame_withAgent}.
 * 
 * @author wish
 */
public class Strategies {

	// defining some predicate and atom names for Prolog:
    static String cross = "cross";
    static String circle = "circle";
    static String blocked = "blocked";
    
    /**
     * winningMove(X,Y) : (X,Y) would be a winning move for cross. Currently the strategies
     * below do not include diagonal winning move.
     */
    static PredicateName winningMove = predicate("winningMove");

    /**
     * occupied(Ty,X,Y): square (X,Y) is occupied by a piece of type Ty (which can be a blocker).
     */
    static PredicateName occupied = predicate("occupied");

    static PredicateName free = predicate("free");

    
    /**
     * eastNeighbor(Ty,X1,X2,Y) : (X2,Y) is occupied by a piece of type Ty, and X1=X2-1.
     */
    static PredicateName eastNeighbor = predicate("eastNeighbor");

    /**
     * northNeighbor(Ty,X,Y1,Y2) : (X,Y2) is occupied by a piece of type Ty, and Y1=Y2-1.
     */
    static PredicateName northNeighbor = predicate("northNeighbor");
    
    /**
     * northEastNeighbor(Ty,X1,Y1,X2,Y2) : (X2,Y2) is occupied by a piece of type Ty, 
     * and X1=X2-1 and Y1=Y2-1.
     */
    static PredicateName northEastNeighbor = predicate("northEastNeighbor");
    
    /**
     * southEastNeighbor(Ty,X1,Y1,X2,Y2) : (X2,Y2) is occupied by a piece of type Ty, 
     * and X1=X2-1 and Y1=Y2+1.
     */
    static PredicateName southEastNeighbor = predicate("southEastNeighbor");
    
    /**
     * blockMove(X,Y) : the opponent has a free triple-circles, and
     * that placing a cross in (X,Y) would block one side of this triple.
     */
    static PredicateName blockMove = predicate("blockMove");
    
    /**
     * set4Move(X,Y) : cross-player has a triple, and placing a cross at (X,Y)
     * would extend this triple to a quadruple (one away from winning).
     * Currently the strategies below do not include diagonal quadruples.
     */
    static PredicateName set4Move = predicate("set4Move");
    
    static PredicateName tripleWestEast = predicate("tripleWestEast") ;
    static PredicateName tripleSouthNorth = predicate("tripleSouthNorth") ;
    static PredicateName tripleSWNE = predicate("tripleSWNE") ;
    static PredicateName tripleNWSE = predicate("tripleNWSE") ;
    
    static PredicateName quatroWestEast = predicate("quatroWestEast") ;
    static PredicateName quatroSouthNorth = predicate("quatroSouthNorth") ;
    static PredicateName quatroSWNE = predicate("quatroSWNE") ;
    static PredicateName quatroNWSE = predicate("quatroNWSE") ;
    
    // The strategy to play the next move is controlled by the following set of
    // rules:

    // be careful when using "not" and "is" because they are sensitive to the order
    // of evaluation/unification; e.g. don't do them when the binding are not
    // resolved yet
    
    static Rule ruleFree() {
    	return 
    	rule(free.on("X","Y")).impBy(
    		and(
     		  not(occupied.on(cross,"X","Y")),
    		  not(occupied.on(circle,"X","Y")),
  		      not(occupied.on(blocked,"X","Y")))		
    	) ;
    }
  
    static Rule ruleTripleWestEast() { 
    	return
    	rule(tripleWestEast.on("TY","A","D","Y")).impBy(
    			 eastNeighbor.on("TY", "A", "B", "Y"))
                .and(eastNeighbor.on("TY", "B", "C", "Y"))
                .and(eastNeighbor.on("TY", "C", "D", "Y"))
    	;
    }
    
    static Rule ruleQuatroWestEast() { 
    	return
    	rule(quatroWestEast.on("TY","A","C","Y")).impBy(
    			tripleWestEast.on("TY", "A", "B", "Y"))
                .and(eastNeighbor.on("TY", "B", "C", "Y"))
    	;
    }
    
    static Rule ruleTripleSouthNorth() { 
    	return
    	rule(tripleSouthNorth.on("TY","X","A","D")).impBy(
    			northNeighbor.on("TY", "X", "A", "B"))
                .and(northNeighbor.on("TY", "X", "B", "C"))
                .and(northNeighbor.on("TY", "X", "C", "D"))
    	;
    }
    
    static Rule ruleQuatroSouthNorth() { 
    	return
    	rule(quatroSouthNorth.on("TY","X","A","C")).impBy(
    			tripleSouthNorth.on("TY", "X", "A", "B"))
                .and(northNeighbor.on("TY", "X", "B", "C"))
    	;
    }
    
    static Rule ruleTripleSWNE() { 
    	return
    	rule(tripleSWNE.on("TY","A","B","G", "H")).impBy(
    			northEastNeighbor.on("TY", "A", "B", "C", "D"))
                .and(northEastNeighbor.on("TY", "C", "D", "E", "F"))
                .and(northEastNeighbor.on("TY", "E", "F", "G", "H"))
    	;
    }
    
    static Rule ruleQuatroSWNE() { 
    	return
    	rule(quatroSWNE.on("TY","A","B","E", "F")).impBy(
    			tripleSWNE.on("TY", "A", "B", "C", "D"))
                .and(northEastNeighbor.on("TY", "C", "D", "E", "F"))
    	;
    }
    
    static Rule ruleTripleNWSE() { 
    	return
    	rule(tripleNWSE.on("TY","A","B","G", "H")).impBy(
    			southEastNeighbor.on("TY", "A", "B", "C", "D"))
                .and(southEastNeighbor.on("TY", "C", "D", "E", "F"))
                .and(southEastNeighbor.on("TY", "E", "F", "G", "H"))
    	;
    }
    
    static Rule ruleQuatroNWSE() { 
    	return
    	rule(quatroNWSE.on("TY","A","B","E", "F")).impBy(
    			tripleNWSE.on("TY", "A", "B", "C", "D"))
                .and(southEastNeighbor.on("TY", "C", "D", "E", "F"))
    	;
    }
    
    static Rule ruleWinWest() { 
    	return 
    	rule(winningMove.on("A", "Y")).impBy(quatroWestEast.on(cross, "A", "E", "Y"))
            .and(free.on("A", "Y")) ;
    }

    
    static Rule ruleWinEast(int boardsize) { 
    	return
    	rule(winningMove.on("E", "Y")).impBy(occupied.on(cross, "A", "Y"))
            .and(tripleWestEast.on(cross, "A", "D", "Y"))
            .and("E is (D+1)").and("E < " + boardsize)
            .and(free.on("E", "Y")) ;
    }
    
    static Rule ruleWinSouth() { 
    	return 
    	rule(winningMove.on("X", "A")).impBy(quatroSouthNorth.on(cross, "X", "A", "B"))
            .and(free.on("X", "A")) ;
    }
    
    static Rule ruleWinNorth(int boardsize) { 
    	return
    	rule(winningMove.on("X", "E")).impBy(occupied.on(cross, "X", "A"))
            .and(tripleSouthNorth.on(cross, "X", "A", "D")) 
            .and("E is (D+1)").and("E < " + boardsize)
            .and(free.on("E", "Y")) ;

    } 
    
    static Rule ruleBlock3WestEast(int boardsize) { 
    	return
    	rule(blockMove.on("A", "Y")).impBy(tripleWestEast.on(circle, "A", "D", "Y"))
            .and(free.on("A", "Y"))
            .and("E is (D+1)").and("E < " + boardsize).and(free.on("E", "Y")) ;
    }

    static Rule ruleBlock3SouthNorth(int boardsize) { 
    	return
    	rule(blockMove.on("X", "A")).impBy(tripleSouthNorth.on(circle, "X", "A", "D"))
            .and(free.on("X", "A"))
            .and("E is (D+1)").and("E < " + boardsize).and(free.on("X", "E")) ;
    }
    
    static Rule ruleBlock3SWNE(int boardsize) { 
    	return
    	rule(blockMove.on("A", "H")).impBy(
    			 tripleSWNE.on(circle, "A", "H", "D", "K"))
            .and(free.on("A", "H"))
            .and("E is (D+1)").and("L is (K+1)")
            .and("E < " + boardsize).and("L < " + boardsize)
            .and(free.on("E", "L")) ;
    }

    static Rule ruleBlock3NWSE(int boardsize) { 
    	return
    	rule(blockMove.on("A", "H")).impBy(
    			 tripleNWSE.on(circle, "A", "H", "D", "K"))
            .and(free.on("A", "H"))
            .and("E is (D+1)").and("L is (K-1)")
            .and("E < " + boardsize).and("L >= 0")
            .and(free.on("E", "L")) ;
    }
    
    static Rule ruleBlock4WestEast(int boardsize) { 
    	return
    	rule(blockMove.on("X", "Y")).impBy(quatroWestEast.on(circle, "A", "E", "Y"))
            .and(or(and(free.on("A", "Y"), "X is A"),
                    and("F is (E+1)", "F < " + boardsize, free.on("F", "Y"), "X is F")));
    }
    
    static Rule ruleBlock4WestEast_b(int boardsize) { 
    	return
    	rule(blockMove.on("X", "Y")).impBy(tripleSouthNorth.on(circle, "A", "E", "Y"))
            .and(occupied.on(circle,"A", "Y"))
            .and("X is (E+1)").and("X < " + boardsize) 
            .and(free.on("X", "Y")) 
            ;
    }
    
    static Rule ruleBlock4SouthNorth(int boardsize) { 
    	return
    	rule(blockMove.on("X", "Y")).impBy(quatroSouthNorth.on(circle, "X", "A", "E"))
            .and(or(and(free.on("X", "A"), "Y is A"),
                    and("F is (E+1)", "F < " + boardsize, free.on("X", "F"), "Y is F")));
    }
    
    static Rule ruleBlock4SouthNorth_b(int boardsize) { 
    	return
    	rule(blockMove.on("X", "Y")).impBy(tripleSouthNorth.on(circle, "X", "A", "E"))
            .and(occupied.on(circle,"X", "A"))
            .and("Y is (E+1)").and("Y < " + boardsize) 
            .and(free.on("X", "Y")) 
            ;
    }
    
    static Rule ruleBlock4SWNE(int boardsize) { 
    	return
    	rule(blockMove.on("X", "Y")).impBy(quatroSWNE.on(circle, "A", "H", "E", "L"))
            .and(or(and(free.on("A", "H"), "X is A", "Y is H"),
                    and("F is (E+1)", "F < " + boardsize, 
                    	"M is (L+1)", "M < " + boardsize, 
                    	free.on("F", "M"), "X is F", "Y is M")));
    }
    
    static Rule ruleBlock4SWNE_b(int boardsize) { 
    	return
    	rule(blockMove.on("X", "Y")).impBy(tripleSWNE.on(circle, "A", "H", "E", "L"))
            .and(occupied.on(circle,"A", "H"))
            .and("X is (E+1)").and("X < " + boardsize)
            .and("Y is (L+1)").and("Y < " + boardsize) 
            .and(free.on("X", "Y")) 
            ;
    }
    
    static Rule ruleBlock4NWSE(int boardsize) { 
    	return
    	rule(blockMove.on("X", "Y")).impBy(quatroNWSE.on(circle, "A", "H", "E", "L"))
            .and(or(and(free.on("A", "H"), "X is A", "Y is H"),
                    and("F is (E+1)", "F < " + boardsize, 
                    	"M is (L-1)", "M >= 0", 
                    	free.on("F", "M"), "X is F", "Y is M")));
    }
    
    static Rule ruleBlock4NWSE_b(int boardsize) { 
    	return
    	rule(blockMove.on("X", "Y")).impBy(tripleNWSE.on(circle, "A", "H", "E", "L"))
            .and(occupied.on(circle,"A", "H"))
            .and("X is (E+1)").and("X < " + boardsize)
            .and("Y is (L-1)").and("L >= 0") 
            .and(free.on("X", "Y")) 
            ;
    }
    
    static Rule rule4WestEast(int boardsize) { 
    	return
    	rule(set4Move.on("X", "Y")).impBy(tripleWestEast.on(cross, "A", "D", "Y"))
            .and("E is (D+1)").and("E < " + boardsize)
            .and(free.on("A", "Y"))
            .and(free.on("E", "Y"))
            .and(or(and("0 < A", "X is A"), and("(E+1) < " + boardsize, "X is E")));
    }
       
    static Rule rule4SouthNorth(int boardsize) { 
    	return
    	rule(set4Move.on("X", "Y")).impBy(tripleSouthNorth.on(cross, "X", "A", "D"))
            .and("E is (D+1)").and("E < " + boardsize)
            .and(free.on("X", "A"))
            .and(free.on("X", "E"))
            .and(or(and("0 < A", "Y is A"), and("(E+1) < " + boardsize, "Y is E")));
    }
    
    /**
     * To add a the above strategy rules to Prolog-base.
     */
    static void addStrategyRules(PrologReasoner prolog, int boardsize) throws InvalidTheoryException {
    	prolog.add(
    		ruleFree(),
    		ruleTripleWestEast(),
    		ruleTripleSouthNorth(),
    		ruleTripleSWNE(),
    		ruleTripleNWSE(),
    		ruleQuatroWestEast(),
    		ruleQuatroSouthNorth(),
    		ruleQuatroSWNE(),
    		ruleQuatroNWSE(),
    		ruleWinWest(), 
        	ruleWinEast(boardsize), 
        	ruleWinSouth(),
            ruleWinNorth(boardsize), 
        	ruleBlock3WestEast(boardsize), 
        	ruleBlock3SouthNorth(boardsize), 
        	ruleBlock3SWNE(boardsize),
        	ruleBlock3NWSE(boardsize),
        	ruleBlock4WestEast(boardsize), 
        	ruleBlock4WestEast_b(boardsize), 
        	ruleBlock4SouthNorth(boardsize), 
        	ruleBlock4SouthNorth_b(boardsize), 
        	ruleBlock4SWNE(boardsize),
        	ruleBlock4SWNE_b(boardsize),
        	ruleBlock4NWSE(boardsize),
        	ruleBlock4NWSE_b(boardsize),
        	rule4SouthNorth(boardsize),
            rule4WestEast(boardsize), 
            rule4SouthNorth(boardsize));
    }
    
    /**
	 * To initialize the prolog-base with strategy-rules and the initial locations
	 * of blocked-squares.
	 * 
	 * @throws InvalidTheoryException
	 */
    static void initializeProlog(PrologReasoner prolog, FiveGame thegame) throws InvalidTheoryException {
        addStrategyRules(prolog,thegame.boardsize) ;
        // and add blocked-squares to prolog:
        for (int x = 0; x < thegame.boardsize; x++) {
            for (int y = 0; y < thegame.boardsize; y++) {
            // System.out.println(">>>") ;
            if (thegame.board[x][y] == SQUARE.BLOCKED) {
                // System.out.println(">>>==") ;
                prolog.facts(occupied.on(blocked, x, y));
            }
            }
        } 
    }
    
    /**
     * To translate a move on the game to relevant facts to be put in the Prolog-base.
     */
    static void markMove(PrologReasoner prolog, FiveGame thegame, SQUARE sq, int x, int y) {
    	try {
            markMoveWorker(prolog, thegame, sq, x, y);
            //System.out.println(">>> marking move " + sq + " " + x + "," + y) ;
        } catch (Exception e) {
        } // swallow...
    }
    
    static String sqtype(SQUARE sq) {
        switch (sq) {
        case CROSS:
            return cross;
        case CIRCLE:
            return circle;
        case BLOCKED:
            return blocked;
        }
        return null;
    }
    
    
    static void markMoveWorker(PrologReasoner prolog, FiveGame thegame, SQUARE sq, int x, int y) throws InvalidTheoryException {
    	var board = thegame.board ;
    	var boardsize = thegame.boardsize ;
        prolog.facts(occupied.on(sqtype(sq), x, y));
        if (x > 0) {
            prolog.facts(eastNeighbor.on(sqtype(sq), x - 1, x, y));
        }
        if (x < boardsize - 1 && board[x + 1][y] != SQUARE.EMPTY) {
            prolog.facts(eastNeighbor.on(sqtype(board[x + 1][y]), x, x + 1, y));
        }
        if (y > 0) {
            prolog.facts(northNeighbor.on(sqtype(sq), x, y - 1, y));
        }
        if (y < boardsize - 1 && board[x][y + 1] != SQUARE.EMPTY) {
            prolog.facts(northNeighbor.on(sqtype(board[x][y + 1]), x, y, y + 1));
        }
        if(x>0 && y>0) {
        	prolog.facts(northEastNeighbor.on(sqtype(sq), x-1, y-1, x, y));
        }
        if (x < boardsize - 1 && y < boardsize - 1 && board[x+1][y+1] != SQUARE.EMPTY) {
        	prolog.facts(northEastNeighbor.on(sqtype(board[x+1][y+1]), x,y, x+1, y+1));
        }
        if(x>0 && y < boardsize - 1) {
        	prolog.facts(southEastNeighbor.on(sqtype(sq), x-1, y+1, x, y));
        }
        if (x < boardsize - 1 && y>0 && board[x+1][y-1] != SQUARE.EMPTY) {
        	prolog.facts(southEastNeighbor.on(sqtype(board[x+1][y-1]), x,y, x+1, y-1));
        }
    }
    
    private static void testMove(PrologReasoner prolog, FiveGame thegame, SQUARE sq, int x, int y) {
    	thegame.move(sq, x, y) ;
    	markMove(prolog,thegame,sq,x,y) ;
    }
    
    public static void main(String[] args) throws InvalidTheoryException {
    	FiveGame thegame = new FiveGame(10,0) ;
    	var state = new State() ;
    	state.attachProlog() ;
    	var prolog = state.prolog() ;
    	addStrategyRules(prolog,thegame.boardsize) ;
    	testMove(prolog,thegame,SQUARE.CIRCLE,0,5) ;
    	testMove(prolog,thegame,SQUARE.CIRCLE,1,4) ;
    	testMove(prolog,thegame,SQUARE.CIRCLE,2,3) ;
    	testMove(prolog,thegame,SQUARE.CIRCLE,3,2) ;
    	
    	var qr = prolog.query(blockMove.on("A", "Y")) ;
    	int a = qr.int_("A");
        int d = qr.int_("Y");
        System.out.println(">>> " + a + "," + d) ;
        
    }
    

}
