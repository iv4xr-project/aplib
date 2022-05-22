package eu.iv4xr.framework.extensions.pathfinding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.*;

public class Test_Sparse2DTiledSurface_NavGraph {
	
	/**
	 * Test path-finding with blockers.
	 */
	@Test
	public void test_pathfinding() {
		
		var nav = new Sparse2DTiledSurface_NavGraph() ;
		nav.maxX = 5 ;
		nav.maxY = 5 ;
		
		for(int x=0; x<nav.maxX; x++) {
			for (int y=0; y<nav.maxY; y++) {
				nav.markAsSeen(new Tile(x,y));
			}
		}
		
		for(int x=0; x<nav.maxX; x++) {
			nav.addObstacle(new Wall(x,0));
			nav.addObstacle(new Wall(x,nav.maxY-1));
			nav.addObstacle(new Wall(0,x));
			nav.addObstacle(new Wall(nav.maxX-1,x));
		}
		
		System.out.println(nav) ;
		
		var path = nav.findPath(1, 1, 3, 3) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;
		
		var d1 = new Door(1,2) ;
		var d2 = new Door(2,2) ;
		
		nav.addObstacle(d1);
		nav.addObstacle(d2);
		
		System.out.println(nav) ;
		
		path = nav.findPath(1, 1, 3, 3) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;

		nav.addObstacle(new Door(3,2));

		System.out.println(nav) ;
		
		// should give no path:
		path = nav.findPath(1, 1, 3, 3) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;
	
		// let's open one of the doors, should then give a path
		d1.isOpen = true ;
		System.out.println(nav) ;
		path = nav.findPath(1, 1, 3, 3) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;	
	}
	
	@Test
	public void test_visibility() {
		var nav = new Sparse2DTiledSurface_NavGraph() ;
		nav.maxX = 12 ;
		nav.maxY = 12 ;
		
		for(int x=0; x<nav.maxX; x++) {
			nav.addObstacle(new Wall(x,0));
			nav.addObstacle(new Wall(x,nav.maxY-1));
			nav.addObstacle(new Wall(0,x));
			nav.addObstacle(new Wall(nav.maxX-1,x));
		}
		
		assertTrue(!nav.hasbeenSeen(0,0)) ;
		assertTrue(!nav.hasbeenSeen(1,1)) ;
		
		System.out.println(nav) ;
		var path = nav.findPath(1, 1, 10, 10) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;
		
		nav.perfect_memory_pathfinding = true ;
		path = nav.findPath(1, 1, 10, 10) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;
		nav.perfect_memory_pathfinding = false ;
		path = nav.findPath(1, 1, 10, 10) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;
		
		
		for(int x=0; x<5; x++) {
			for (int y=0; y<5; y++) {
				nav.markAsSeen(new Tile(x,y));
			}
		}
		System.out.println(nav) ;
		path = nav.findPath(1, 1, 10, 10) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;
		path = nav.findPath(1, 1, 4, 4) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;
		
		nav.wipeOutMemory();
		System.out.println(nav) ;
		path = nav.findPath(1, 1, 4, 4) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;	
	}
	
	@Test
	public void test_explore() {
		var nav = new Sparse2DTiledSurface_NavGraph() ;
		nav.maxX = 12 ;
		nav.maxY = 12 ;
		
		for(int x=0; x<nav.maxX; x++) {
			nav.addObstacle(new Wall(x,0));
			nav.addObstacle(new Wall(x,nav.maxY-1));
			nav.addObstacle(new Wall(0,x));
			nav.addObstacle(new Wall(nav.maxX-1,x));
		}
		
		for(int x=0; x<5; x++) {
			for (int y=0; y<5; y++) {
				nav.markAsSeen(new Tile(x,y));
			}
		}
		System.out.println(nav) ;
		var path = nav.explore(0,0,0,0) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;	
		path = nav.explore(1,1,1,1) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;	
		
		var frontiers = nav.getFrontier() ;
		System.out.println(">>> frontiers: " + frontiers) ;
		assertTrue(frontiers.size() == 9) ;
		assertTrue(frontiers.contains(new Tile(4,4))) ;
		
		for(int x=0; x<6; x++) {
			for (int y=0; y<6; y++) {
				nav.markAsSeen(new Tile(x,y));
			}
		}
		System.out.println(nav) ;
		path = nav.explore(1,1,1,1) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;	
		frontiers = nav.getFrontier() ;
		System.out.println(">>> frontiers: " + frontiers) ;
		assertTrue(frontiers.size() == 11) ;
		assertFalse(frontiers.contains(new Tile(4,4))) ;
	}
	

}
