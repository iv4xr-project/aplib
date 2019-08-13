package nl.uu.cs.aplib.ExampleUsages.FiveGame;

import java.util.*;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Term;
import nl.uu.cs.aplib.Agents.StateWithProlog;
import nl.uu.cs.aplib.ExampleUsages.FiveGame.FiveGame.GAMESTATUS;
import nl.uu.cs.aplib.ExampleUsages.FiveGame.FiveGame.SQUARE;
import nl.uu.cs.aplib.ExampleUsages.FiveGame.FiveGame.Square_;
import nl.uu.cs.aplib.MainConcepts.*;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import static nl.uu.cs.aplib.Agents.StateWithProlog.* ;

public class FiveGame_withAgent {
	
	static class MyState extends StateWithProlog {
		
		int boardsize ;
		SQUARE[][] board ;
		
		MyState() { super() ; }


        // defining bunch of predicates we will need
		
		String occupied(String type, String x, String y) { return mkPredString("occupied",type,x,y) ; }
		String occupied(String type, int x, int y) { return occupied(type, "" + x, "" + y) ; }
		
		String eastNeighbor(String type, String x, String xe, String y) { 
			return mkPredString("eastNeighbor",type,x,xe,y) ; 
		}
		String eastNeighbor(String type, int x, int xe, int y) { 
			return eastNeighbor(type, "" + x, "" + xe, "" + y) ; 
		}
		
		String northNeighbor(String type, String x, String y, String yn) { 
			return mkPredString("northNeighbor",type,x,y,yn) ; 
		}
		String northNeighbor(String type, int x, int y, int yn) { 
			return northNeighbor(type, "" + x, "" + y, "" + yn) ; 
		}
		
		String winningMove(String x, String y) { return mkPredString("winningMove",x,y) ; }
		String set4Move(String x, String y) { return mkPredString("set4Move",x,y) ; }
		String blockMove(String x, String y) { return mkPredString("blockMove",x,y) ; }
		

		void markBlockedSquares() throws InvalidTheoryException {
			for (int x=0; x<boardsize; x++) {
				for (int y=0; y<boardsize; y++) {
					//System.out.println(">>>") ;
					if (board[x][y] == SQUARE.BLOCKED) {
						//System.out.println(">>>==") ;
						addFacts(occupied(blocked(),x,y)) ;
					}
				}
			}
		}
				
		// be careful when using "not" and "is" because they are sensitive to the order
		// of evaluation/unification; e.g. don't do them when the binding are not resolved yet
		String ruleWinWest() {
           return 
        	 clause(winningMove("X","Y"))
               . IMPby(eastNeighbor(cross(),"A","B","Y")) 
               . and(eastNeighbor(cross(),"B","C","Y")) 
               . and(eastNeighbor(cross(),"C","D","Y")) 
               . and(eastNeighbor(cross(),"D","E","Y")) 
               . and(not(occupied("O","A","Y"))) 
        	   . and("X is A")
        	   . toString()
               ;
		}
		
		String ruleWinEast() {
	           return 
	        	 clause(winningMove("X","Y"))
	               . IMPby(occupied(cross(),"A","Y"))
	               . and(eastNeighbor(cross(),"A","B","Y")) 
	               . and(eastNeighbor(cross(),"B","C","Y")) 
	               . and(eastNeighbor(cross(),"C","D","Y")) 
	               . and("E is (D+1)") 
	               . and("E < " + boardsize)
	               . and(not(occupied("O","E","Y"))) 
	               . and("X is E") 
	               . toString()
	               ;
	    }
		
		String ruleWinSouth() {
	           return 
	        	 clause(winningMove("X","Y"))
	               . IMPby(northNeighbor(cross(),"X","A","B")) 
	               . and(northNeighbor(cross(),"X","B","C")) 
	               . and(northNeighbor(cross(),"X","C","D")) 
	               . and(northNeighbor(cross(),"X","D","E")) 
	               . and(not(occupied("O","X","A"))) 
	               . and("Y is A") 
	               . toString()
	               ;
		}	
		
		String ruleWinNorth() {
	           return 
	        	 clause(winningMove("X","Y"))
	               . IMPby(occupied(cross(),"X","A"))
	               . and(northNeighbor(cross(),"X","A","B")) 
	               . and(northNeighbor(cross(),"X","B","C")) 
	               . and(northNeighbor(cross(),"X","C","D")) 
	               . and("E is (D+1)")
	               . and("E < " + boardsize)
	               . and(not(occupied(cross(),"X","E"))) 
	               . and("Y is E") 
	               . toString()
                   ;
		}
		
		String ruleBlock4WestEast() {
			return
			  clause(blockMove("X","Y")) 
			  . IMPby(eastNeighbor(circle(),"A","B","Y"))
			  . and(eastNeighbor(circle(),"B","C","Y"))
			  . and(eastNeighbor(circle(),"C","D","Y"))
			  . and(eastNeighbor(circle(),"D","E","Y"))
			  . and(or(and(not(occupied("O","A","Y")), "X is A"),
					   and("F is (E+1)", "F < " + boardsize, not(occupied("O","F","Y")), "X is F")
					  ))
			  .toString() 
			  ;
		}
		
		String ruleBlock4SouthNorth() {
			return
			  clause(blockMove("X","Y")) 
			  . IMPby(northNeighbor(circle(),"X","A","B"))
			  . and(northNeighbor(circle(),"X","B","C"))
			  . and(northNeighbor(circle(),"X","C","D"))
			  . and(northNeighbor(circle(),"X","D","E"))
			  . and(or(and(not(occupied("O","X","A")), "Y is A"),
					   and("F is (E+1)", "F < " + boardsize, not(occupied("O","X","F")), "Y is F")
					  ))
			  .toString() 
			  ;
		}
		
		String rule4WestEast() {
	           return 
	        	 clause(set4Move("X","Y"))
	               . IMPby(eastNeighbor(cross(),"A","B","Y")) 
	               . and(eastNeighbor(cross(),"B","C","Y")) 
	               . and(eastNeighbor(cross(),"C","D","Y")) 
	               . and("E is (D+1)")
	               . and("E < " + boardsize)
	               . and(not(occupied("O","A","Y")))
	               . and(not(occupied("P","E","Y"))) 
	               . and(or(and("0 < A", "X is A"), 
	            		    and("(E+1) < " + boardsize, "X is E"))) 
	               . toString()
	               ;
	    }
		
		String rule4SouthNorth() {
	           return 
	        	 clause(set4Move("X","Y"))
	               . IMPby(northNeighbor(cross(),"X","A","B")) 
	               . and(northNeighbor(cross(),"X","B","C")) 
	               . and(northNeighbor(cross(),"X","C","D")) 
	               . and("E is (D+1)")
	               . and("E < " + boardsize)
	               . and(not(occupied("O","X","A")))
	               . and(not(occupied("P","X","E"))) 
	               . and(or(and("0 < A", "Y is A"), 
	            		    and("(E+1) < " + boardsize , "Y is E"))) 
	               . toString()
	               ;
	    }
		
		void createInitialTheory() throws InvalidTheoryException {
			markBlockedSquares() ;
			addRules(ruleWinWest().toString(),
					 ruleWinEast().toString(),
					 ruleBlock4WestEast(),
					 ruleBlock4SouthNorth(),
					 ruleWinSouth().toString(),
					 ruleWinNorth().toString(),
					 rule4WestEast().toString(),
					 rule4SouthNorth().toString()
					) ;
		}
		
		@Override
		public FiveGameEnv env() {
			return (FiveGameEnv) super.env() ;
		}
		
		@Override
		public MyState setEnvironment(Environment env)  {
			super.setEnvironment(env) ;
			board = env().thegame.getState() ;
			boardsize = env().thegame.boardsize ;
			try {
				createInitialTheory() ;
			}
			catch(Exception e) { return null ; }
			return this ;
		}
		
		@Override
		public void updateState() {
			super.updateState();
			var lastmove = env().thegame.lastmove ;
			if (lastmove != null) {
				try {
					markMove_(lastmove.sq,lastmove.x,lastmove.y) ;
				}
				catch(Exception e) { }
			}
		}
			
		void markMove(SQUARE sq, int x, int y) {
			try {
				markMove_(sq,x,y) ;
			}
			catch(Exception e) { } // swallow...
		}
		
		
		String cross()   { return "cross" ; }
		String circle()  { return "circle" ; }
		String blocked() { return "blocked" ; }
				
		
		String sqtype(SQUARE sq) {
			switch(sq) {
			  case CROSS : return cross() ;
			  case CIRCLE : return circle() ;
			  case BLOCKED : return blocked() ;
			}
			return null ;
		}
		
		void markMove_(SQUARE sq, int x, int y) throws InvalidTheoryException {
			board[x][y] = sq ;
			addFacts(occupied(sqtype(sq),x,y)) ;

			if (x>0) {
				addFacts(eastNeighbor(sqtype(sq),x-1,x,y)) ; 
			}
			if (x<boardsize-1 && board[x+1][y] != SQUARE.EMPTY) {
				addFacts(eastNeighbor(sqtype(board[x+1][y]), x, x+1, y)) ; 
			}
			if (y>0) {
				addFacts(northNeighbor(sqtype(sq),x,y-1,y)) ;
			}
			if (y<boardsize-1 && board[x][y+1] != SQUARE.EMPTY) {
				addFacts(northNeighbor(sqtype(board[x][y+1]), x, y, y+1)) ;
			}
		}
		
		List<Square_> getEmptySquares() {
			var r = new LinkedList<Square_>() ;
			for (int x=0; x<boardsize; x++)
				for (int y=0; y<boardsize; y++) {
					if (board[x][y] == SQUARE.EMPTY) {
						r.add(new Square_(SQUARE.EMPTY,x,y)) ;
					}
				}
			return r ;
		}
		
		boolean hasHVCrossNeighbor(int x, int y) {
			if (x>0 && board[x-1][y] == SQUARE.CROSS) return true ;
			if (x+1<boardsize && board[x+1][y] == SQUARE.CROSS) return true ;
			if (y>0 && board[x][y-1] == SQUARE.CROSS) return true ;
			if (y+1<boardsize && board[x][y+1] == SQUARE.CROSS) return true ;
			return false ;
		}
		
		List<Square_> getEmptySquaresWithHVNeighboringCross() {
	        return getEmptySquares().stream().filter(sq -> hasHVCrossNeighbor(sq.x,sq.y)).collect(Collectors.toList()) ;
		}
		
	}
	
	
	// just for testing
	static private void test() throws InvalidTheoryException {
		var state = new MyState() ;
		int N = 8 ;
		state.boardsize = N ;
		state.board = new SQUARE[N][N] ;
		for (int x=0; x<N; x++)
			for (int y=0; y<N; y++)
				state.board[x][y] = SQUARE.EMPTY ;
		
        //		state.board[0][0] = SQUARE.BLOCKED ;
		state.createInitialTheory()  ;
		
		state.markMove(SQUARE.CROSS,2,2);
		state.markMove(SQUARE.CROSS,3,2);
		state.markMove(SQUARE.CROSS,4,2);
		
		System.out.println(state.showTheory()) ;
		
		var solutions = state.query("set4Move(X,Y)", "X", "Y") ;
	
		if (solutions != null) {
			for (Term x : solutions)
				System.out.println(">> " + x) ;
		}
		else {
			System.out.println(">> no solution") ; 
		}
	}
	
	
	static public void main(String[] args) throws InvalidTheoryException, NoSolutionException, MalformedGoalException {
		
		// test() ;
		
		// creating an instance of the FiveGame
		var thegame = new FiveGame(7,0) ;
		// create an agent state and an environment, attached to the game:
		var state = new MyState().setEnvironment(new FiveGameEnv().attachGame(thegame));
		// creatint the agent:
		var agent = new BasicAgent() . attachState(state) ;
		
		var rnd = new Random() ;
		
		// defining various actions
		// this one will randomly choose an empty square, that has a horizontal or vertical cross-neighbor:
		var besideHV = action("besideHV") 
			. do_((MyState st) -> actionstate -> {
			     var empties = st.getEmptySquaresWithHVNeighboringCross() ;
			     if (empties.isEmpty()) empties = st.getEmptySquares() ;
			     if (empties.isEmpty()) return null ;
			     Square_ sq = empties.get(rnd.nextInt(empties.size())) ;
			     var status = st.env().move(SQUARE.CROSS, sq.x, sq.y) ;
			     st.markMove(SQUARE.CROSS, sq.x, sq.y) ;
			     return status ;
		       }) 
			. lift() ;
		
		// put a cross if there is a winning horizontal or vertical conf:
		var winningmove = action("winningmove")
			. do_((MyState st) -> actionstate -> {		
			     var solution = st.query(st.winningMove("X","Y"),"X","Y") ;
			     if (solution == null) return null ;
			     int x = intval(solution[0]) ;
			     int y = intval(solution[1]) ;
			     var status = st.env().move(SQUARE.CROSS, x, y) ;
			     st.markMove(SQUARE.CROSS,x,y) ;
			     return status ;
		      })
			. on_((MyState st) -> st.test(st.winningMove("X","Y")) ) 
			. lift() ;
		
		// block the opponent if it has a 4 consecutive hor. or vert. row:
		var block = action("block")
				. do_((MyState st) -> actionstate -> {		
				     var solution = st.query(st.blockMove("X","Y"),"X","Y") ;
				     if (solution == null) return null ;
				     int x = intval(solution[0]) ;
				     int y = intval(solution[1]) ;
				     var status = st.env().move(SQUARE.CROSS, x, y) ;
				     st.markMove(SQUARE.CROSS,x,y) ;
				     return status ;
			      })
				. on_((MyState st) -> st.test(st.blockMove("X","Y")) ) 
				. lift() ;
		
		// place a cross next to 3 consecutive hor or vert crosses:
		var smartmove = action("smartmove")
				. do_((MyState st) -> actionstate -> {		
				     var solution = st.query(st.set4Move("X","Y"),"X","Y") ;
				     if (solution == null) return null ;
				     int x = intval(solution[0]) ;
				     int y = intval(solution[1]) ;
				     var status = st.env().move(SQUARE.CROSS, x, y) ;
				     st.markMove(SQUARE.CROSS,x,y) ;
				     return status ;
			      })
				. on_((MyState st) -> st.test(st.set4Move("X","Y")) ) 
				. lift() ;
		
		// define a goal and specify a strategy:
		var g = goal("goal")
				. toSolve((GAMESTATUS st) -> st == GAMESTATUS.CROSSWON)
				. withStrategy(FIRSTof(winningmove,block,smartmove,besideHV))
				. lift()
				;
		
		agent.setGoal(g) ;
		var opponent = new FiveGame.RandomPlayer(SQUARE.CIRCLE,thegame) ;
		
		Scanner consoleInput = new Scanner(System.in);

		// now we let the agent play against an automated random player:
		while(thegame.getGameStatus() == GAMESTATUS.UNFINISHED) {
			opponent.move() ;
			if (thegame.getGameStatus() != GAMESTATUS.UNFINISHED) {
				thegame.print();
				thegame.printStatus();
				break ;
			}
			agent.update();
			thegame.print();
			thegame.printStatus();
			System.out.println("(press a ENTER to continue)") ;
			consoleInput.nextLine() ;
		}
		
	}
}
