package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Wall;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Maze;
import nl.uu.cs.aplib.utils.Pair;

class UnitTestMaze {

	@Test
	void testMaze1() {
		
		var maze = new Maze(2,4) ;
		assertTrue(maze.id == 2 && maze.world.length == 4) ;
		for(int x=0; x<4; x++) {
			var row = maze.world[x] ;
			assertTrue(row.length == 4) ;
			for(int y=0; y<4; y++) {
				Entity e = maze.world[x][y] ;
				if(x==0 || x==3 || y==0 || y==3) {
					assertTrue(e instanceof Wall) ;
					assertTrue(e.x == x && e.y == y) ;
					assertTrue(e.mazeId == 2) ;
					assertTrue(e.id.equals("W_2_" + x + "_" + y)) ;
				}
				else {
					assertTrue(e == null) ;
				}
			}
		}
		
		//System.out.println("" + maze.getFreeTiles().size()) ;
		assertTrue(maze.getFreeTiles().size() == 4) ;
		System.out.println("" + maze.getClosestFreeSquare(3,3)) ;
		assertTrue(maze.getClosestFreeSquare(3,3).equals(new Pair<>(2,2))) ;
		
	}
	
	@Test
	void testBuildSimpleMaze() {
		
		var maze = Maze.buildSimpleMaze(1, new Random(171),4,1) ;
		assertTrue(maze.id == 1 && maze.world.length == 4) ;
		assertTrue(maze.getFreeTiles().size() == 4) ;

		maze = Maze.buildSimpleMaze(1, new Random(171),7,2) ;
		assertTrue(maze.id == 1 && maze.world.length == 7) ;
		assertTrue(maze.getFreeTiles().size() == 25 - 4) ;

		maze = Maze.buildSimpleMaze(1, new Random(171),7,3) ;
		assertTrue(maze.world.length == 7) ;
		assertTrue(maze.getFreeTiles().size() == 25 - 2*4) ;

	}
	
	@Test
	void testBrokenWalls() {
		var org = Maze.probabilityBuggyWall ;
		Maze.probabilityBuggyWall = 0f ;
		var maze = Maze.buildSimpleMaze(1, new Random(171),20,3) ;
		boolean hasBroken1 = maze.hasBrokenWall() ;
		Maze.probabilityBuggyWall = 1.0f ;
		maze = Maze.buildSimpleMaze(1, new Random(171),20,3) ;
		boolean hasBroken2 = maze.hasBrokenWall() ;
		Maze.probabilityBuggyWall = org ;
		assertFalse(hasBroken1) ;
		assertTrue(hasBroken2) ;
	}
	
}
