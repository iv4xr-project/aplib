package nl.uu.cs.aplib.exampleUsages.fiveGame;

import java.util.*;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Term;
import nl.uu.cs.aplib.agents.PrologReasoner;
import nl.uu.cs.aplib.agents.State;

import static nl.uu.cs.aplib.agents.PrologReasoner.*;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.GAMESTATUS;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.SQUARE;
import nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame.Square_;
import nl.uu.cs.aplib.mainConcepts.*;

import static nl.uu.cs.aplib.AplibEDSL.*;

public class FiveGame_withAgent {

    // defining some predicate and atom names for Prolog:
    static String cross = "cross";
    static String circle = "circle";
    static String blocked = "blocked";
    static PredicateName winningMove = predicate("winningMove");
    static PredicateName eastNeighbor = predicate("eastNeighbor");
    static PredicateName northNeighbor = predicate("northNeighbor");
    static PredicateName occupied = predicate("occupied");
    static PredicateName blockMove = predicate("blockMove");
    static PredicateName set4Move = predicate("set4Move");

    static class MyState extends State {
    	
    	FiveGame.SQUARE[][] board ;
    	int boardsize ;

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
            board = env().thegame.board ;
            boardsize = board.length;
            var prolog = prolog();

            // The strategy to play the next move is controlled by the following set of
            // rules:

            // be careful when using "not" and "is" because they are sensitive to the order
            // of evaluation/unification; e.g. don't do them when the binding are not
            // resolved yet
            var ruleWinWest = rule(winningMove.on("X", "Y")).impBy(eastNeighbor.on(cross, "A", "B", "Y"))
                    .and(eastNeighbor.on(cross, "B", "C", "Y")).and(eastNeighbor.on(cross, "C", "D", "Y"))
                    .and(eastNeighbor.on(cross, "D", "E", "Y")).and(not(occupied.on("O", "A", "Y"))).and("X is A");

            var ruleWinEast = rule(winningMove.on("X", "Y")).impBy(occupied.on(cross, "A", "Y"))
                    .and(eastNeighbor.on(cross, "A", "B", "Y")).and(eastNeighbor.on(cross, "B", "C", "Y"))
                    .and(eastNeighbor.on(cross, "C", "D", "Y")).and("E is (D+1)").and("E < " + boardsize)
                    .and(not(occupied.on("O", "E", "Y"))).and("X is E");

            var ruleWinSouth = rule(winningMove.on("X", "Y")).impBy(northNeighbor.on(cross, "X", "A", "B"))
                    .and(northNeighbor.on(cross, "X", "B", "C")).and(northNeighbor.on(cross, "X", "C", "D"))
                    .and(northNeighbor.on(cross, "X", "D", "E")).and(not(occupied.on("O", "X", "A"))).and("Y is A");

            var ruleWinNorth = rule(winningMove.on("X", "Y")).impBy(occupied.on(cross, "X", "A"))
                    .and(northNeighbor.on(cross, "X", "A", "B")).and(northNeighbor.on(cross, "X", "B", "C"))
                    .and(northNeighbor.on(cross, "X", "C", "D")).and("E is (D+1)").and("E < " + boardsize)
                    .and(not(occupied.on(cross, "X", "E"))).and("Y is E");

            var ruleBlock4WestEast = rule(blockMove.on("X", "Y")).impBy(eastNeighbor.on(circle, "A", "B", "Y"))
                    .and(eastNeighbor.on(circle, "B", "C", "Y")).and(eastNeighbor.on(circle, "C", "D", "Y"))
                    .and(eastNeighbor.on(circle, "D", "E", "Y")).and(or(and(not(occupied.on("O", "A", "Y")), "X is A"),
                            and("F is (E+1)", "F < " + boardsize, not(occupied.on("O", "F", "Y")), "X is F")));

            var ruleBlock4SouthNorth = rule(blockMove.on("X", "Y")).impBy(northNeighbor.on(circle, "X", "A", "B"))
                    .and(northNeighbor.on(circle, "X", "B", "C")).and(northNeighbor.on(circle, "X", "C", "D"))
                    .and(northNeighbor.on(circle, "X", "D", "E")).and(or(and(not(occupied.on("O", "X", "A")), "Y is A"),
                            and("F is (E+1)", "F < " + boardsize, not(occupied.on("O", "X", "F")), "Y is F")));

            var rule4WestEast = rule(set4Move.on("X", "Y")).impBy(eastNeighbor.on(cross, "A", "B", "Y"))
                    .and(eastNeighbor.on(cross, "B", "C", "Y")).and(eastNeighbor.on(cross, "C", "D", "Y"))
                    .and("E is (D+1)").and("E < " + boardsize).and(not(occupied.on("O", "A", "Y")))
                    .and(not(occupied.on("P", "E", "Y")))
                    .and(or(and("0 < A", "X is A"), and("(E+1) < " + boardsize, "X is E")));

            var rule4SouthNorth = rule(set4Move.on("X", "Y")).impBy(northNeighbor.on(cross, "X", "A", "B"))
                    .and(northNeighbor.on(cross, "X", "B", "C")).and(northNeighbor.on(cross, "X", "C", "D"))
                    .and("E is (D+1)").and("E < " + boardsize).and(not(occupied.on("O", "X", "A")))
                    .and(not(occupied.on("P", "X", "E")))
                    .and(or(and("0 < A", "Y is A"), and("(E+1) < " + boardsize, "Y is E")));

            try {
                // now add the strategy-rules to the prolog engine:
                prolog.add(ruleWinWest, ruleWinEast, ruleBlock4WestEast, ruleBlock4SouthNorth, ruleWinSouth,
                        ruleWinNorth, rule4WestEast, rule4SouthNorth);
                // and add blocked-squares to prolog:
                for (int x = 0; x < boardsize; x++) {
                    for (int y = 0; y < boardsize; y++) {
                        // System.out.println(">>>") ;
                        if (board[x][y] == SQUARE.BLOCKED) {
                            // System.out.println(">>>==") ;
                            prolog.facts(occupied.on(blocked, x, y));
                        }
                    }
                }
            } catch (Exception e) {
                throw new Error("Fail to add clauses to Prolog");
            }

            return this;
        }

        @Override
        public void updateState(String agentID) {
        	var obs = env().observe(agentID) ;
        	board = obs.snd ;
        }

        void markMove(SQUARE sq, int x, int y) {
            try {
                markMove_(sq, x, y);
            } catch (Exception e) {
            } // swallow...
        }

        String sqtype(SQUARE sq) {
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

        void markMove_(SQUARE sq, int x, int y) throws InvalidTheoryException {
            //env().board[x][y] = sq;
            prolog().facts(occupied.on(sqtype(sq), x, y));
            if (x > 0) {
                prolog().facts(eastNeighbor.on(sqtype(sq), x - 1, x, y));
            }
            if (x < boardsize - 1 && board[x + 1][y] != SQUARE.EMPTY) {
                prolog().facts(eastNeighbor.on(sqtype(board[x + 1][y]), x, x + 1, y));
            }
            if (y > 0) {
                prolog().facts(northNeighbor.on(sqtype(sq), x, y - 1, y));
            }
            if (y < boardsize - 1 && board[x][y + 1] != SQUARE.EMPTY) {
                prolog().facts(northNeighbor.on(sqtype(board[x][y + 1]), x, y, y + 1));
            }
        }

        List<Square_> getEmptySquares() {
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

    // just for testing
    static private void test() throws InvalidTheoryException {
        int N = 8;
        var thegame = new FiveGame(N, 0);
        // create an agent state and an environment, attached to the game:
        var state = new MyState().setEnvironment(new FiveGameEnv().attachGame(thegame));

        state.markMove(SQUARE.CROSS, 2, 2);
        state.markMove(SQUARE.CROSS, 3, 2);
        state.markMove(SQUARE.CROSS, 4, 2);

        System.out.println(state.prolog().showTheory());

        var solution = state.prolog().query("set4Move(X,Y)");

        if (solution != null) {
            System.out.println(">> x = " + solution.int_("X"));
            System.out.println(">> y = " + solution.int_("Y"));
        } else {
            System.out.println(">> no solution");
        }

    }

    static public void main(String[] args) throws InvalidTheoryException, NoSolutionException, MalformedGoalException {

        // test() ;

        // creating an instance of the FiveGame
        var thegame = new FiveGame(7, 0);
        // create an agent state and an environment, attached to the game:
        var state = new MyState().setEnvironment(new FiveGameEnv().attachGame(thegame));
        // creatint the agent:
        var agent = new BasicAgent().attachState(state);

        var rnd = new Random();

        // defining various actions
        // this one will randomly choose an empty square, that has a horizontal or
        // vertical cross-neighbor:
        var besideHV = action("besideHV").do1((MyState st) -> {
            var empties = st.getEmptySquaresWithHVNeighboringCross();
            if (empties.isEmpty())
                empties = st.getEmptySquares();
            if (empties.isEmpty())
                return null;
            Square_ sq = empties.get(rnd.nextInt(empties.size()));
            var status = st.env().move(SQUARE.CROSS, sq.x, sq.y);
            st.markMove(SQUARE.CROSS, sq.x, sq.y);
            return status;
        }).lift();

        // put a cross if there is a winning horizontal or vertical conf:
        var winningmove = action("winningmove").do2((MyState st) -> (QueryResult qsolution) -> {
            if (qsolution == null)
                return null;
            int x = qsolution.int_("X");
            int y = qsolution.int_("Y");
            var status = st.env().move(SQUARE.CROSS, x, y);
            st.markMove(SQUARE.CROSS, x, y);
            return status;
        }).on((MyState st) -> st.prolog().query(winningMove.on("X", "Y"))).lift();

        // block the opponent if it has a 4 consecutive hor. or vert. row:
        var block = action("block").do2((MyState st) -> (QueryResult qsolution) -> {
            if (qsolution == null)
                return null;
            int x = qsolution.int_("X");
            int y = qsolution.int_("Y");
            var status = st.env().move(SQUARE.CROSS, x, y);
            st.markMove(SQUARE.CROSS, x, y);
            return status;
        }).on((MyState st) -> st.prolog().query(blockMove.on("X", "Y"))).lift();

        // place a cross next to 3 consecutive hor or vert crosses:
        var smartmove = action("smartmove").do2((MyState st) -> (QueryResult qsolution) -> {
            if (qsolution == null)
                return null;
            int x = qsolution.int_("X");
            int y = qsolution.int_("Y");
            var status = st.env().move(SQUARE.CROSS, x, y);
            st.markMove(SQUARE.CROSS, x, y);
            return status;
        }).on((MyState st) -> st.prolog().query(set4Move.on("X", "Y"))).lift();

        // define a goal and specify a tactic:
        var g = goal("goal").toSolve((GAMESTATUS st) -> st == GAMESTATUS.CROSSWON)
                .withTactic(FIRSTof(winningmove, block, smartmove, besideHV)).lift();

        agent.setGoal(g);
        var opponent = new FiveGame.RandomPlayer(SQUARE.CIRCLE, thegame);

        Scanner consoleInput = new Scanner(System.in);

        // now we let the agent play against an automated random player:
        while (thegame.getGameStatus() == GAMESTATUS.UNFINISHED) {
            opponent.move();
            if (thegame.getGameStatus() != GAMESTATUS.UNFINISHED) {
                thegame.print();
                thegame.printStatus();
                break;
            }
            agent.update();
            thegame.print();
            thegame.printStatus();
            System.out.println("(press a ENTER to continue)");
            consoleInput.nextLine();
        }
    }
}
