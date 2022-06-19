package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;

public class Utils {

	public static Tile toTile(Vec3 p) {
		return new Tile((int)p.x, (int) p.z ) ;
	}

	static Pair<Integer,Tile> loc3(int mazeId, int x, int y) {
		return new Pair<>(mazeId, new Tile(x,y)) ;
	}

	/**
	 * Check  if two tiles are adjacent.
	 */
	public static boolean adjacent(Tile tile1, Tile tile2) {
		return (tile1.x == tile2.x && Math.abs(tile1.y - tile2.y) == 1)
				||
			   (tile1.y == tile2.y && Math.abs(tile1.x - tile2.x) == 1) ;
	}

	public static int manhattanDist(Tile t1, Tile t2) {
		return Math.abs(t1.x - t2.x) + Math.abs(t1.y - t2.y) ;
	}

	public static int mazeId(WorldEntity e) {
		return (int) e.properties.get("maze") ;
	}

}
