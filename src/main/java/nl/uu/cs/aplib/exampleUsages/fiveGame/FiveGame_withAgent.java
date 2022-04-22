package nl.uu.cs.aplib.exampleUsages.fiveGame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JPanel;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoSolutionException;
import nl.uu.cs.aplib.agents.PrologReasoner;
//import alice.tuprolog.SolveInfo;
//import alice.tuprolog.Term;
//import nl.uu.cs.aplib.agents.PrologReasoner;
import nl.uu.cs.aplib.agents.State;

import static nl.uu.cs.aplib.agents.PrologReasoner.*;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.GAMESTATUS;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.SQUARE;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.Square_;
import nl.uu.cs.aplib.mainConcepts.*;

import static nl.uu.cs.aplib.AplibEDSL.*;

import static nl.uu.cs.aplib.exampleUsages.fiveGame.Strategies.* ;

/**
 * In this demo we will show how to create an agent (an aplib-agent) to automatically
 * play a Fivegame. Its strategies to play the game are coded in Prolog, see {@link Strategies}.
 * The agent will use a state-structure that includes a Prolog-engine to execute the
 * strategies.
 * 
 * <p>Run the main-method to see how this aplib-agent plays against a random-agent (just
 * an agent that plays by randomly placing a piece).
 * 
 * @author wish
 *
 */
public class FiveGame_withAgent {

    /**
     * Defining the state-structure of our aplib-agent that will later play
     * FiveGame. This state will have no additional variables/field. However,
     * it has access to the instance of the played game through its env().
     * It also extends the class State, hence it can have a Prolog-base (which
     * we will use).
     */
    public static class MyState extends State {
    	
        /**
         * Constructor. It will also create a prolog-engine and attach it to this state.
         */
        MyState() {
            super();
            this.attachProlog();
        }

        @Override
        public FiveGameEnv env() {
            return (FiveGameEnv) super.env();
        }

        /**
         * Attach the FiveGame environment to this state, and configure the prolog
         * engine to contain the AI for playing the game.
         * 
         * @throws InvalidTheoryException
         */
        @Override
        public MyState setEnvironment(Environment env) {
            super.setEnvironment(env);
            var prolog = prolog();  
            try {
            	Strategies.initializeProlog(prolog(),env().thegame);
            }
            catch(Exception e) {
            	throw new Error("Failed to initialize Prolog") ;
            }
            return this;
        }

        @Override
        public void updateState(String agentID) {
        	// put the move of opponent in prolog base:
        	Strategies.markMove(prolog(), env().thegame, 
        			env().thegame.lastmove.sq, 
        			env().thegame.lastmove.x, 
        			env().thegame.lastmove.y) ;
        }

  
        List<Square_> getEmptySquares() {
        	var boardsize = env().thegame.boardsize ;
        	var board = env().thegame.board ;
            var r = new LinkedList<Square_>();
            for (int x = 0; x < boardsize; x++)
                for (int y = 0; y < boardsize; y++) {
                    if (board[x][y] == SQUARE.EMPTY) {
                        r.add(new Square_(SQUARE.EMPTY, x, y));
                    }
                }
            return r;
        }

        boolean hasHVCrossNeighbor(int x, int y) {
        	var boardsize = env().thegame.boardsize ;
        	var board = env().thegame.board ;
            if (x > 0 && board[x - 1][y] == SQUARE.CROSS)
                return true;
            if (x + 1 < boardsize && board[x + 1][y] == SQUARE.CROSS)
                return true;
            if (y > 0 && board[x][y - 1] == SQUARE.CROSS)
                return true;
            if (y + 1 < boardsize && board[x][y + 1] == SQUARE.CROSS)
                return true;
            return false;
        }

        List<Square_> getEmptySquaresWithHVNeighboringCross() {
            return getEmptySquares().stream().filter(sq -> hasHVCrossNeighbor(sq.x, sq.y)).collect(Collectors.toList());
        }

    }
    
        
    /**
     * Create and configure an aplib-agent to play a FiveGame.
     */
    public static BasicAgent configureAgent(FiveGame thegame) {
    	
    	// create an agent state and an environment, attached to the game:
        var state = new MyState().setEnvironment(new FiveGameEnv().attachGame(thegame));
        // creating the agent:
        var agent = new BasicAgent().attachState(state);

        var rnd = new Random();

        // Defining various actions that the agent can do:
        
        // this one will randomly choose an empty square, that has a horizontal or
        // vertical cross-neighbor:
        var besideHV = action("besideHV").do1((MyState st) -> {
        	System.out.println(">>> doing besideHV") ;
            var empties = st.getEmptySquaresWithHVNeighboringCross();
            if (empties.isEmpty())
                empties = st.getEmptySquares();
            if (empties.isEmpty()) {
            	//throw new Error() ;
            	return null;	
            }
                
            Square_ sq = empties.get(rnd.nextInt(empties.size()));
            var status = st.env().move(SQUARE.CROSS, sq.x, sq.y);
            Strategies.markMove(st.prolog(),thegame,SQUARE.CROSS, sq.x, sq.y);
            return status;
        }).lift();

        // put a cross if there is a winning horizontal or vertical conf:
        var winningmove = action("winningmove").do2((MyState st) -> (QueryResult qsolution) -> {
            if (qsolution == null)
                return null;
            int x = qsolution.int_("X");
            int y = qsolution.int_("Y");
            System.out.println(">>> doing winningmove (" + x + "," + y + ")") ;
            var status = st.env().move(SQUARE.CROSS, x, y);
            Strategies.markMove(st.prolog(),thegame,SQUARE.CROSS, x, y);
            return status;
        }).on((MyState st) -> st.prolog().query(winningMove.on("X", "Y"))).lift();

        // block the opponent if it has a 3 or 4 consecutive pieces:
        var block = action("block").do2((MyState st) -> (QueryResult qsolution) -> {
            if (qsolution == null)
                return null;
            int x = qsolution.int_("X");
            int y = qsolution.int_("Y");
            System.out.println(">>> doing block (" + x + "," + y + ")") ;
            var status = st.env().move(SQUARE.CROSS, x, y);
            Strategies.markMove(st.prolog(),thegame,SQUARE.CROSS, x, y);
            return status;
        }).on((MyState st) -> st.prolog().query(blockMove.on("X", "Y"))).lift();

         
        // place a cross next to 3 consecutive hor or vert crosses:
        var smartmove = action("smartmove").do2((MyState st) -> (QueryResult qsolution) -> {
            if (qsolution == null)
                return null;
            int x = qsolution.int_("X");
            int y = qsolution.int_("Y");
            System.out.println(">>> doing smartmove (" + x + "," + y + ")") ;
            var status = st.env().move(SQUARE.CROSS, x, y);
            Strategies.markMove(st.prolog(),thegame,SQUARE.CROSS, x, y);
            return status;
        }).on((MyState st) -> st.prolog().query(set4Move.on("X", "Y"))).lift();

        // define a goal for the agent (to win) and specify a tactic to achieve that:
        var g = goal("goal").toSolve((GAMESTATUS st) -> st == GAMESTATUS.CROSSWON)
                .withTactic(FIRSTof(
                		winningmove, 
                		block, 
                		smartmove, 
                		besideHV)).lift();

        agent.setGoal(g);
        
    	return agent ;
    }
    
    /**
     * Create an instance of FiveGame. Create two agents: one is aplib-agent and one is
     * a random-agent. They will play against each other on the game. The aplib-agent
     * will play cross, the random-agent plays circle.
     * 
     * Then, both agents will be run to play the game.
     */
    static public void main(String[] args) throws InvalidTheoryException, NoSolutionException, MalformedGoalException {

        // test() ;
    	var thegame = new FiveGame(8,3);  
    	var agent = configureAgent(thegame) ;
    	var opponent = new FiveGame.RandomPlayer(SQUARE.CIRCLE, thegame);
    	
    	// run the game:
    	GameDisplay nicedisplay = GameDisplay.makeDisplay(thegame) ;
    	nicedisplay.repaint() ;
    	boolean agentTurn = false ;
    	Scanner in = new Scanner(System.in);
    	while(thegame.getGameStatus() == GAMESTATUS.UNFINISHED) {
    		if(agentTurn)
    			agent.update();
    		else {
    			opponent.move() ;
    		}
    		agentTurn = !agentTurn ;
        	nicedisplay.repaint() ;
    		System.out.println(thegame.toString() + "\n" + thegame.toStringShort()) ;
    		System.out.println("x:aplib-agent | o:random-agent (press a ENTER to continue)") ;
    		in.nextLine();
    	}
    }
}
