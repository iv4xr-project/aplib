package nl.uu.cs.aplib.exampleUsages.fiveGame;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * A simple game as an example for agents to play. The game is played by two
 * players on an NxN board. The players take turn to put their pieces. The
 * pieces of player-1 are called "circles", that of player-2 are "crosses". A
 * piece is placed on an empty square. Some squares may be blocked; these are
 * randomly determined when the board was created.
 * 
 * A player wins if she can make a horizontal, vertical, or diagonal connected
 * segment of length 5, consisting of only her pieces.
 * 
 * The game ends in tie if the board is full, and no player wins.
 * 
 * @author wish
 *
 */
public class FiveGame {

    static public enum SQUARE {
        EMPTY, CIRCLE, CROSS, BLOCKED
    }

    static public enum GAMESTATUS {
        UNFINISHED, TIE, CIRCLEWON, CROSSWON
    }

    static public class Square_ {
        public int x;
        public int y;
        public SQUARE sq;

        public Square_(SQUARE sq, int x, int y) {
            this.sq = sq;
            this.x = x;
            this.y = y;
        }
    }

    Random rnd = new Random();

    int boardsize;
    SQUARE[][] board;
    Square_ lastmove;

    /**
     * Construct an instance of FiveGame of the specified size x size.
     * 
     * @param size
     * @param numOfBlocked This many squares will be randomly selected and blocked.
     */
    public FiveGame(int size, int numOfBlocked) {
        boardsize = size;
        board = new SQUARE[size][size];
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++) {
                board[x][y] = SQUARE.EMPTY;
            }
        // randomly place blocks:
        while (numOfBlocked > 0) {
            int x = rnd.nextInt(size);
            int y = rnd.nextInt(size);
            if (board[x][y] == SQUARE.BLOCKED)
                continue;
            board[x][y] = SQUARE.BLOCKED;
            numOfBlocked--;
        }
    }

    private String line(int n) {
        var s = "";
        for (int k = 0; k < n; k++)
            s += "=";
        return s;
    }

    /**
     * Get a copy of the game board.
     */
    public SQUARE[][] getState() {
        SQUARE[][] copy = new SQUARE[boardsize][boardsize];
        for (int x = 0; x < boardsize; x++)
            for (int y = 0; y < boardsize; y++) {
                copy[x][y] = board[x][y];
            }
        return copy;
    }

    private boolean winningColumn(SQUARE ty, int x, int y) {
        if (y > boardsize - 5)
            return false;
        for (int k = 0; k < 5; k++) {
            if (board[x][y + k] != ty)
                return false;
        }
        return true;
    }

    private boolean winningRow(SQUARE ty, int x, int y) {
        if (x > boardsize - 5)
            return false;
        for (int k = 0; k < 5; k++) {
            if (board[x + k][y] != ty)
                return false;
        }
        return true;
    }

    private boolean winningDiagonal1(SQUARE ty, int x, int y) {
        if (x > boardsize - 5 || y > boardsize - 5)
            return false;
        for (int k = 0; k < 5; k++) {
            if (board[x + k][y + k] != ty)
                return false;
        }
        return true;
    }

    private boolean winningDiagonal2(SQUARE ty, int x, int y) {
        if (x > boardsize - 5 || y < 4)
            return false;
        for (int k = 0; k < 5; k++) {
            if (board[x + k][y - k] != ty)
                return false;
        }
        return true;
    }

    /**
     * Get the game status (unfinished/cross-won/circle-won/tie).
     */
    public GAMESTATUS getGameStatus() {
        boolean hasEmpty = false;
        for (int x = 0; x < boardsize; x++)
            for (int y = 0; y < boardsize; y++) {
                if (winningColumn(SQUARE.CROSS, x, y) || winningRow(SQUARE.CROSS, x, y)
                        || winningDiagonal1(SQUARE.CROSS, x, y) || winningDiagonal2(SQUARE.CROSS, x, y))
                    return GAMESTATUS.CROSSWON;
                if (winningColumn(SQUARE.CIRCLE, x, y) || winningRow(SQUARE.CIRCLE, x, y)
                        || winningDiagonal1(SQUARE.CIRCLE, x, y) || winningDiagonal2(SQUARE.CIRCLE, x, y))
                    return GAMESTATUS.CIRCLEWON;
                if (board[x][y] == SQUARE.EMPTY)
                    hasEmpty = true;
            }
        if (hasEmpty)
            return GAMESTATUS.UNFINISHED;
        else
            return GAMESTATUS.TIE;
    }

    /**
     * Place a piece of the specified type in the given coordinate. If the square is
     * unoccupied, this returns true, else false.
     * 
     * @param ty Should be either CROSS or CIRCLE.
     */
    public boolean move(SQUARE ty, int x, int y) {
        if (x < 0 || x >= boardsize || y < 0 || y >= boardsize)
            throw new IllegalArgumentException();
        if (ty != SQUARE.CROSS && ty != SQUARE.CIRCLE)
            throw new IllegalArgumentException();
        if (board[x][y] != SQUARE.EMPTY)
            return false;
        if (lastmove == null)
            lastmove = new Square_(ty, x, y);
        else {
            lastmove.sq = ty;
            lastmove.x = x;
            lastmove.y = y;
        }
        board[x][y] = ty;
        return true;
    }

    public Square_ getLastmove() {
        return lastmove;
    }

    @Override
    /**
     * Show the play board as a string.
     */
    public String toString() {
        var s = line(boardsize + 2);
        for (int x = 0; x < boardsize; x++) {
            s += "\n ";
            for (int y = 0; y < boardsize; y++)
                if (board[x][y] == SQUARE.EMPTY)
                    s += " ";
                else if (board[x][y] == SQUARE.CROSS)
                    s += "X";
                else if (board[x][y] == SQUARE.CIRCLE)
                    s += "O";
                else
                    s += "#";
        }
        s += "\n" + line(boardsize + 2);
        return s;
    }

    /**
     * Produce a string summarizing the status of the game.
     */
    public String toStringShort() {
        int crosses = 0;
        int circles = 0;
        int empty = 0;
        int block = 0;
        for (int x = 0; x < boardsize; x++)
            for (int y = 0; y < boardsize; y++) {
                if (board[x][y] == SQUARE.CROSS)
                    crosses++;
                else if (board[x][y] == SQUARE.CIRCLE)
                    circles++;
                else if (board[x][y] == SQUARE.EMPTY)
                    empty++;
                else
                    block++;
            }
        var s = "";
        s += "(" + boardsize + "x" + boardsize + ") " + crosses + " X, " + circles + " O, " + block + " #, " + empty
                + " empties. Status: " + getGameStatus();
        return s;
    }

    public void print() {
        System.out.println(toString());
    }

    public void printStatus() {
        System.out.println(toStringShort());
    }

    /**
     * A simple random auto-player.
     */
    static public class RandomPlayer {
        FiveGame game;
        SQUARE ty;
        Random rnd = new Random();

        /**
         * Construct an instance of RandomPlayer.
         * 
         * @param ty   Should be either CROSS or CIRCLE.
         * @param game The instance of the FiveGame this player is supposed to play on.
         */
        public RandomPlayer(SQUARE ty, FiveGame game) {
            this.ty = ty;
            this.game = game;
        }

        List<int[]> getNeigbors(int x, int y) {
            int N = game.boardsize;
            var neighbors = new LinkedList<int[]>();
            int xlow = Math.max(0, x - 1);
            int xhigh = Math.min(N - 1, x + 1);
            int ylow = Math.max(0, y - 1);
            int yhigh = Math.min(N - 1, y + 1);
            for (int x_ = xlow; x_ <= xhigh; x_++)
                for (int y_ = ylow; y_ <= yhigh; y_++) {
                    int[] position = { x_, y_ };
                    if (x_ != x && y_ != y)
                        neighbors.add(position);
                }
            return neighbors;
        }

        /**
         * Find a random empty place, which is adjacent to another of the same type, and
         * put a piece there. If there is no such square then choose a random empty
         * square. If there is no empty square left, the method returns false.
         */
        public boolean move() {
            var empties = new LinkedList<int[]>();
            var emptiesAdjacent = new LinkedList<int[]>();
            for (int x = 0; x < game.boardsize; x++)
                for (int y = 0; y < game.boardsize; y++) {
                    if (game.board[x][y] == SQUARE.EMPTY) {
                        int[] position = { x, y };
                        empties.add(position);
                        for (int[] neighbor : getNeigbors(x, y)) {
                            int x_ = neighbor[0];
                            int y_ = neighbor[1];
                            if (game.board[x_][y_] == ty) {
                                emptiesAdjacent.add(position);
                                break;
                            }
                        }
                    }
                }
            if (!emptiesAdjacent.isEmpty()) {
                var position = emptiesAdjacent.get(rnd.nextInt(emptiesAdjacent.size()));
                game.move(ty, position[0], position[1]);
                return true;
            }
            if (!empties.isEmpty()) {
                var position = empties.get(rnd.nextInt(empties.size()));
                game.move(ty, position[0], position[1]);
                return true;
            }
            return false;
        }

    }

    static public void main(String[] args) {

        // a small demo of two instances of RandomPlayer playing the FiveGame

        FiveGame game = new FiveGame(6, 3);
        RandomPlayer player1 = new RandomPlayer(SQUARE.CIRCLE, game);
        RandomPlayer player2 = new RandomPlayer(SQUARE.CROSS, game);
        game.print();
        game.printStatus();

        boolean circleTurn = true;
        while (game.getGameStatus() == GAMESTATUS.UNFINISHED) {
            if (circleTurn) {
                player1.move();
                circleTurn = false;
            } else {
                player2.move();
                circleTurn = true;
            }
        }

        game.print();
        game.printStatus();

    }
}
