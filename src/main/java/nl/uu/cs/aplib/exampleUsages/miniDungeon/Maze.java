package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import eu.iv4xr.framework.spatial.IntVec2D;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Wall;
import nl.uu.cs.aplib.utils.Pair;

public class Maze {
	
	public enum MazeShape { SIMPLE } 
	
	public int id ;
	/**
	 * An NxN maze.
	 */
	public Entity[][] world ;
	public int size ;
	
	/**
	 * Probability of introducing a buggy wall. A buggy wall can be passed through.
	 * There can only be maximum one buggy wall per maze.
	 */
	public static float probabilityBuggyWall = 0.05f ;
	
	static private void assignIdToWall(Wall w, int mazeId) {
		w.mazeId = mazeId ;
		w.id = "W_" + mazeId + "_" + w.x + "_" + w.y ;
 	}
	
	/**
	 * Construct a maze of NxN, surrounded by walls.
	 */
	public Maze(int mazeId, int size) {
		this.id = mazeId;
		this.size = size ;
		world = new Entity[size][size];
		// walls around the arena:
		for (int i = 0; i < size; i++) {
			Wall w = new Wall(0, i,"");
			assignIdToWall(w,mazeId) ;
			world[0][i] = w ;
			// BUG found by unit test
			//w = new Wall(size - 1, 0, "");
			w = new Wall(size - 1, i, "");
			assignIdToWall(w,mazeId) ;
			world[size - 1][i] = w ;
			w =  new Wall(i, 0, "");
			assignIdToWall(w,mazeId) ;
			world[i][0] = w ;
			w = new Wall(i, size - 1, "");
			assignIdToWall(w,mazeId) ;
			world[i][size - 1] = w ;
		}
	}
	
	public List<Pair<Integer,Integer>> getFreeTiles() {
		List<Pair<Integer, Integer>> freeSquares = new LinkedList<>();
		int size = world.length ;
		for (int x = 1; x < size - 1; x++) {
			for (int y = 1; y < size - 1; y++) {
				if (world[x][y] == null) {
					freeSquares.add(new Pair<Integer, Integer>(x, y));
				}
			}
		}
		return freeSquares ;
	}
	
	public Pair<Integer,Integer> getClosestFreeSquare(int x, int y) {
		var freeTiles = getFreeTiles() ;
		if (freeTiles.isEmpty()) return null ;
		Pair<Integer,Integer> minp = null ;
		float mindistSq = Float.MAX_VALUE ;
		var p0 = new IntVec2D(x,y) ;
		for(var q : freeTiles) {
			var qv = new IntVec2D(q.fst,q.snd) ;
			var distSq = IntVec2D.distSq(p0,qv) ;
			if (distSq < mindistSq) {
				mindistSq = distSq ;
				minp = q ;
			}
		}
		return minp ;
	}
	
	public boolean hasBrokenWall() {
		for(int x=0; x<size; x++) {
			for (int y=0; y<size;y++) {
				var e = world[x][y] ;
				if (e!=null && e.type == EntityType.WALL) {
					var w = (Wall) e ;
					if (w.brokenwall) return true ;
				}
			}
		}
		return false ;
	}
		
	/**
	 * Construct an NxN maze with a single zig-zag corridor. There is some a probability
	 * it generates a buggy-wall in the maze.
	 */
	public static Maze buildSimpleMaze(int mazeId, Random rnd, int size, int numberOfCorridors) {
		Maze maze = new Maze(mazeId,size) ;
		var world = maze.world ;
		int numberOfBuggyWall = 0 ;
		int maxNumberOfBuggyWalls = 1 ;
		// walls that build the maze (just a simple maze) :
		int corridorWidth = size / numberOfCorridors;
		int coridorX = size;
		boolean alt = true;
		for (int cr = 1; cr < numberOfCorridors; cr++) {
			coridorX = coridorX - corridorWidth;
			if (alt) {
				for (int y = 1; y < size - 2; y++) {
					Wall w = new Wall(coridorX, y,"") ;
					// injecting one buggy wall:
					if (numberOfBuggyWall <maxNumberOfBuggyWalls && rnd.nextFloat() <= probabilityBuggyWall) {
						w.brokenwall = true ;
						System.out.println("### BW") ;
						numberOfBuggyWall++ ;
					}
					assignIdToWall(w,mazeId) ;
					world[coridorX][y] = w ;
				}
			} else {
				for (int y = size - 2; 1 < y; y--) {
					Wall w = new Wall(coridorX, y,"") ;
					if (numberOfBuggyWall <maxNumberOfBuggyWalls && rnd.nextFloat() <= probabilityBuggyWall) {
						w.brokenwall = true ;
						numberOfBuggyWall++ ;
					}
					assignIdToWall(w,mazeId) ;
					world[coridorX][y] = w ;
				}
			}
			alt = !alt;
		}
		// HACK
		// add a wall above shrines to prevent oscilating path-planning around
		// a shrine. 
		//Wall w = new Wall(size-2,2,"") ;
		//assignIdToWall(w,mazeId) ;
		//world[size-2][2] = w ;
		//w = new Wall(1,2,"") ;
		//assignIdToWall(w,mazeId) ;
		//world[1][2] = w ;
		return maze ;
	}
	
	public static boolean has_atleast_K_FreeNeighbours(int x, int y, 
			Maze maze, 
			int minNumberOfFreeNeighbours) {
		if (! (0<x && x < maze.size-1 && 0<y && y < maze.size-1)) 
			return false ;
		int free = 0  ;
		if (maze.world[x-1][y] == null) free++ ;
		if (maze.world[x+1][y] == null) free++ ;
		if (maze.world[x][y-1] == null) free++ ;
		if (maze.world[x][y+1] == null) free++ ;
		
		return free >= minNumberOfFreeNeighbours ;					
	}
	
	public static void removeThoseWithTooManyOccupiedNeighbours(
			List<Pair<Integer,Integer>> freeTiles,
			Maze maze,
			int minNumberOfFreeNeighbours) {
		freeTiles.removeIf(tile -> ! has_atleast_K_FreeNeighbours(tile.fst,tile.snd,maze,minNumberOfFreeNeighbours)) ;
	}

}
