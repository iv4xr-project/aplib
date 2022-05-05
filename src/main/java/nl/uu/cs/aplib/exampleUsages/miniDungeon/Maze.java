package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.util.Random;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Wall;

public class Maze {
	
	public enum MazeShape { SIMPLE } 
	
	public String id ;
	public Entity[][] world ;
	
	/**
	 * Construct a maze of NxN, sorounded by walls.
	 */
	public Maze(String id, int size) {
		this.id = id;
		world = new Entity[size][size];
		// walls around the arena:
		for (int i = 0; i < size; i++) {
			world[0][i] = new Wall(0, i);
			world[size - 1][i] = new Wall(size - 1, i);
			world[i][0] = new Wall(i, 0);
			world[i][size - 1] = new Wall(i, size - 1);
		}
	}
	
	/**
	 * Construct an NxN maze with a single zig-zag corridor.
	 */
	public static Maze buildSimpleMaze(String id, Random rnd, int size, int numberOfCorridors) {
		Maze maze = new Maze(id,size) ;
		var world = maze.world ;
		// walls that build the maze (just a simple maze) :
		int corridorWidth = size / numberOfCorridors;
		int coridorX = size;
		boolean alt = true;
		for (int cr = 1; cr < numberOfCorridors; cr++) {
			coridorX = coridorX - corridorWidth;
			if (alt) {
				for (int y = 1; y < size - 2; y++) {
					Wall w = new Wall(coridorX, y) ;
					w.mazeId = id ;
					world[coridorX][y] = w ;
				}
			} else {
				for (int y = size - 2; 1 < y; y--) {
					Wall w = new Wall(coridorX, y) ;
					w.mazeId = id ;
					world[coridorX][y] = w ;
				}
			}
			alt = !alt;
		}
		return maze ;
	}

}
