package eu.iv4xr.framework.extensions.pathfinding;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.*;
import nl.uu.cs.aplib.utils.Pair;
import static org.junit.jupiter.api.Assertions.*;

public class Test_LayeredAreasNavigation {
	
	
	public Sparse2DTiledSurface_NavGraph mkArea() {
		
		var nav = new Sparse2DTiledSurface_NavGraph() ;
		nav.sizeX = 5 ;
		nav.sizeY = 5 ;
		
		for(int x=0; x<nav.sizeX; x++) {
			for (int y=0; y<nav.sizeY; y++) {
				nav.markAsSeen(new Tile(x,y));
			}
		}
		
		for(int x=0; x<nav.sizeX; x++) {
			nav.addObstacle(new Wall(x,0));
			nav.addObstacle(new Wall(x,nav.sizeY-1));
			nav.addObstacle(new Wall(0,x));
			nav.addObstacle(new Wall(nav.sizeX-1,x));
		}
		
		return nav ;
	}
	
	Pair<Integer, Tile> loc3(int area, int x, int y) {
		return new Pair(area, new Tile(x,y)) ;
	}
	
	@Test
	public void test_pathfinding() {
		
		var layers = new LayeredAreasNavigation<Tile,Sparse2DTiledSurface_NavGraph>() ;
		var area0 = mkArea() ;
		var area1 = mkArea() ;
		var area2 = mkArea() ;
		layers.addNextArea(area0, null, null, false);
		layers.addNextArea(area1, new Tile(3,1), new Tile(1,1), false);
		layers.addNextArea(area2, new Tile(3,1), new Tile(1,1), false);
		
		System.out.println("" + layers) ;
		
		var path = layers.findPath(loc3(0,1,1), loc3(0,3,3)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null && path.size()>0) ;

		path = layers.findPath(loc3(0,1,1), loc3(0,3,1)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null && path.size()>0) ;
		
		path = layers.findPath(loc3(0,1,1), loc3(1,1,1)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;

		path = layers.findPath(loc3(0,1,1), loc3(1,3,3)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;
		
		path = layers.findPath(loc3(0,1,1), loc3(2,3,3)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;
		
		// enable portal 0->1
		layers.setPortal(0, 1, true);
		
		path = layers.findPath(loc3(0,1,1), loc3(1,1,1)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;

		path = layers.findPath(loc3(0,1,1), loc3(1,3,3)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;
		
		path = layers.findPath(loc3(0,1,1), loc3(2,3,3)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path == null) ;

		// enable portal 1->2 too:
		layers.setPortal(1,2, true);
		
		path = layers.findPath(loc3(0,1,1), loc3(1,1,1)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;

		path = layers.findPath(loc3(0,1,1), loc3(1,3,3)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;
		
		path = layers.findPath(loc3(0,1,1), loc3(2,3,3)) ;
		System.out.println(">>> path: " + path) ;
		assertTrue(path != null) ;
		
	}
	
	@Test
	public void test_markvisible() {
		var layers = new LayeredAreasNavigation<Tile,Sparse2DTiledSurface_NavGraph>() ;
		var area0 = mkArea() ;
		var area1 = mkArea() ;
		var area2 = mkArea() ;
		layers.addNextArea(area0, null, null, false);
		layers.addNextArea(area1, new Tile(3,1), new Tile(1,1), false);
		layers.addNextArea(area2, new Tile(3,1), new Tile(1,1), false);
		
		layers.wipeOutMemory();
		System.out.println("" + layers) ;
		assertFalse(layers.hasbeenSeen(loc3(0,3,1))) ;
		assertFalse(layers.hasbeenSeen(loc3(1,1,1))) ;
		
	
		layers.markAsSeen(loc3(0,3,1));
		System.out.println("" + layers) ;
		assertTrue(layers.hasbeenSeen(loc3(0,3,1))) ;
		assertTrue(layers.hasbeenSeen(loc3(1,1,1))) ;
	
	}

	
	@Test
	public void test_explore() {
		var layers = new LayeredAreasNavigation<Tile,Sparse2DTiledSurface_NavGraph>() ;
		var area0 = mkArea() ;
		var area1 = mkArea() ;
		var area2 = mkArea() ;
		layers.addNextArea(area0, null, null, false);
		layers.addNextArea(area1, new Tile(3,1), new Tile(1,1), false);
		layers.addNextArea(area2, new Tile(3,1), new Tile(1,1), false);
		
		layers.wipeOutMemory();
		for(int x=0; x<=2; x++) {
			for (int y=0; y<=2; y++)  {
			   layers.markAsSeen(loc3(0,x,y));
			}
		}

		System.out.println("" + layers) ;
		var frontiers = layers.getFrontier() ;
		var path = layers.explore(loc3(0,1,1)) ;
		System.out.println("Frontiers: " + frontiers) ;
		System.out.println("explr-path: " + path) ;
		assertTrue(frontiers.size()>0) ;
		assertTrue(path.size()>0) ;
		assertTrue(frontiers.stream().allMatch(c -> c.fst == 0)) ;
		assertTrue(path.get(path.size()-1).equals(loc3(0,2,1))) ;
		
		// marking portal in layer-0 as seen; this will mark the high-portal
		// in layer-1 to be marked as seen as well:
		layers.markAsSeen(loc3(0,3,1));
		System.out.println("" + layers) ;
		frontiers = layers.getFrontier() ;
		path = layers.explore(loc3(0,1,1)) ;
		System.out.println("Frontiers: " + frontiers) ;
		System.out.println("explr-path: " + path) ;
		assertTrue(frontiers.size()>0) ;
		assertTrue(path.size()>0) ;
		assertTrue(frontiers.stream().anyMatch(c -> c.fst == 1)) ;
		assertTrue(path.get(path.size()-1).equals(loc3(0,1,2))) ;
		
		// explore with heuristic-node, but the portal is still closed:
		path = layers.explore(loc3(0,1,1), loc3(1,3,3)) ;
		System.out.println("explr-path: " + path) ;
		assertTrue(path.stream().allMatch(c -> c.fst == 0)) ;
		
		// ok now open the portal too:
		layers.setPortal(0, 1, true);
		path = layers.explore(loc3(0,1,1), loc3(1,3,3)) ;
		System.out.println("explr-path: " + path) ;
		assertTrue(path.get(path.size()-1).equals(loc3(1,1,1))) ;

	}

}
