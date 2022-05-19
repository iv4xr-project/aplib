package eu.iv4xr.framework.extensions.pathfinding;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Wall;

public class Test_Sparse2DTiledSurface_NavGraph {
	
	@Test
	public void test1() {
		
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
		
		var path = nav.findPath(1, 1, 3, 3) ;
		
		System.out.println(">>> " + path) ;
		
		
	}
	

}
