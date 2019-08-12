package nl.uu.cs.aplib.ExampleUsages.FiveGame;

import java.util.*;

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
		
		String occupied(String x, String y) { return mkPredString("occupied",x,y) ; }
		String occupied(int x, int y) { return occupied("" + x, "" + y) ; }
		String cross(String x, String y) { return mkPredString("cross",x,y) ; }
		String cross(int x, int y) { return cross("" + x, "" + y) ; }
		
		String crossEast(String x, String xe, String y) { return mkPredString("crossEast",x,xe,y) ; }
		String crossEast(int x, int xe, int y) { return crossEast("" + x, "" + xe, "" + y) ; }
		
		String crossNorth(String x, String y, String yn) { return mkPredString("crossNorth",x,y,yn) ; }
		String crossNorth(int x, int y, int yn) { return crossNorth("" + x, "" + y, "" + yn) ; }
		
		String winningMove(String x, String y) { return mkPredString("winningMove",x,y) ; }
		String set4Move(String x, String y) { return mkPredString("set4Move",x,y) ; }
		

		void markBlockedSquares() throws InvalidTheoryException {
			for (int x=0; x<boardsize; x++) {
				for (int y=0; y<boardsize; y++) {
					//System.out.println(">>>") ;
					if (board[x][y] == SQUARE.BLOCKED) {
						//System.out.println(">>>==") ;
						addFacts(occupied(x,y)) ;
					}
				}
			}
		}
				
		// be careful when using "not" and "is" because they are sensitive to the order
		// of evaluation/unification; e.g. don't do them when the binding are not resolved yet
		String ruleWinWest() {
           return 
        	 clause(winningMove("X","Y"))
               . IMPby(crossEast("A","B","Y")) 
               . and(crossEast("B","C","Y")) 
               . and(crossEast("C","D","Y")) 
               . and(crossEast("D","E","Y")) 
               . and(not(occupied("A","Y"))) 
        	   . and("X is A")
        	   . toString()
               ;
		}
		
		String ruleWinEast() {
	           return 
	        	 clause(winningMove("X","Y"))
	               . IMPby(cross("A","Y"))
	               . and(crossEast("A","B","Y")) 
	               . and(crossEast("B","C","Y")) 
	               . and(crossEast("C","D","Y")) 
	               . and("E is (D+1)") 
	               . and("E < " + boardsize)
	               . and(not(occupied("E","Y"))) 
	               . and("X is E") 
	               . toString()
	               ;
	    }
		
		String ruleWinSouth() {
	           return 
	        	 clause(winningMove("X","Y"))
	               . IMPby(crossNorth("X","A","B")) 
	               . and(crossNorth("X","B","C")) 
	               . and(crossNorth("X","C","D")) 
	               . and(crossNorth("X","D","E")) 
	               . and(not(occupied("X","A"))) 
	               . and("Y is A") 
	               . toString()
	               ;
		}	
		
		String ruleWinNorth() {
	           return 
	        	 clause(winningMove("X","Y"))
	               . IMPby(cross("X","A"))
	               . and(crossNorth("X","A","B")) 
	               . and(crossNorth("X","B","C")) 
	               . and(crossNorth("X","C","D")) 
	               . and("E is (D+1)")
	               . and("E < " + boardsize)
	               . and(not(occupied("X","E"))) 
	               . and("Y is E") 
	               . toString()
                   ;
		}
		
		String rule4WestEast() {
	           return 
	        	 clause(set4Move("X","Y"))
	               . IMPby(crossEast("A","B","Y")) 
	               . and(crossEast("B","C","Y")) 
	               . and(crossEast("C","D","Y")) 
	               . and("E is (D+1)")
	               . and("E < " + boardsize)
	               . and(not(occupied("A","Y")))
	               . and(not(occupied("E","Y"))) 
	               . and(or(and("0 < A", "X is A"), 
	            		    and("(E+1) < " + boardsize, "X is E"))) 
	               . toString()
	               ;
	    }
		
		String rule4SouthNorth() {
	           return 
	        	 clause(set4Move("X","Y"))
	               . IMPby(crossNorth("X","A","B")) 
	               . and(crossNorth("X","B","C")) 
	               . and(crossNorth("X","C","D")) 
	               . and("E is (D+1)")
	               . and("E < " + boardsize)
	               . and(not(occupied("X","A")))
	               . and(not(occupied("X","E"))) 
	               . and(or(and("0 < A", "Y is A"), 
	            		    and("(E+1) < " + boardsize , "Y is E"))) 
	               . toString()
	               ;
	    }
		
		void createInitialTheory() throws InvalidTheoryException {
			markBlockedSquares() ;
			addRules(ruleWinWest().toString(),
					 ruleWinEast().toString(),
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
		
		void markMove_(SQUARE sq, int x, int y) throws InvalidTheoryException {
			board[x][y] = sq ;
			addFacts(occupied(x,y)) ;
			if (sq == SQUARE.CROSS) addFacts(cross(x,y)) ; 

			if (x>0 && sq == SQUARE.CROSS) {
				addFacts(crossEast(x-1,x,y)) ; 
			}
			if (x<boardsize-1 && board[x+1][y] == SQUARE.CROSS) {
				addFacts(crossEast(x,x+1,y)) ; 
			}
			if (y>0 && sq == SQUARE.CROSS) {
				addFacts(crossNorth(x,y-1,y)) ;
			}
			if (y<boardsize-1 && board[x][y+1] == SQUARE.CROSS) {
				addFacts(crossNorth(x,y,y+1)) ;
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
	}
	
	static public void main(String[] args) throws InvalidTheoryException, NoSolutionException, MalformedGoalException {
		
		/*
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
		*/
		
		
		var thegame = new FiveGame(10,0) ;
		var state = new MyState().setEnvironment(new FiveGameEnv().attachGame(thegame));
		var agent = new BasicAgent() . attachState(state) ;
		
		var rnd = new Random() ;
		
		var random = action("random")
			. do_((MyState st) -> actionstate -> {
			     var empties = st.getEmptySquares() ;
			     if (empties.isEmpty()) return null ;
			     Square_ sq = empties.get(rnd.nextInt(empties.size())) ;
			     st.env().thegame.move(SQUARE.CROSS, sq.x, sq.y) ;
			     st.markMove(SQUARE.CROSS, sq.x, sq.y) ;
			     return st.env().thegame.getGameStatus() ;
		       }) 
			. lift() ;
		
		var winningmove = action("winningmove")
			. do_((MyState st) -> actionstate -> {		
			     var solution = st.query(st.winningMove("X","Y"),"X","Y") ;
			     if (solution == null) return null ;
			     int x = intval(solution[0]) ;
			     int y = intval(solution[1]) ;
			     st.env().thegame.move(SQUARE.CROSS, x, y) ;
			     st.markMove(SQUARE.CROSS,x,y) ;
			     return st.env().thegame.getGameStatus() ;
		      })
			. on_((MyState st) -> st.test(st.winningMove("X","Y")) ) 
			. lift() ;
		
		var smartmove = action("smartmove")
				. do_((MyState st) -> actionstate -> {		
				     var solution = st.query(st.set4Move("X","Y"),"X","Y") ;
				     if (solution == null) return null ;
				     int x = intval(solution[0]) ;
				     int y = intval(solution[1]) ;
				     st.env().thegame.move(SQUARE.CROSS, x, y) ;
				     st.markMove(SQUARE.CROSS,x,y) ;
				     return st.env().thegame.getGameStatus() ;
			      })
				. on_((MyState st) -> st.test(st.set4Move("X","Y")) ) 
				. lift() ;
		
		var g = goal("goal")
				. toSolve((GAMESTATUS st) -> st == GAMESTATUS.CROSSWON)
				. withStrategy(FIRSTof(winningmove,smartmove,random))
				. lift()
				;
		
		agent.setGoal(g) ;
		var opponent = new FiveGame.RandomPlayer(SQUARE.CIRCLE,thegame) ;
		
		Scanner consoleInput = new Scanner(System.in);

		while(thegame.getGameStatus() == GAMESTATUS.UNFINISHED) {
			opponent.move() ;
			agent.update();
			thegame.print();
			thegame.printStatus();
			consoleInput.nextLine() ;
		}
		
		
	}

}
