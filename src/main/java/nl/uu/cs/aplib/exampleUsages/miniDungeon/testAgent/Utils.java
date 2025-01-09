package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib.adjustedFindPath;

import java.util.LinkedList;
import java.util.List;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;

public class Utils {

	public static Tile toTile(Vec3 p) {
		return new Tile((int)p.x, (int) p.z ) ;
	}

	public static Pair<Integer,Tile> loc3(int mazeId, int x, int y) {
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
	
	public static String otherPlayer(MyAgentState S) {
		if (S.worldmodel.agentId.equals("Frodo")) return "Smeagol" ;
		return "Frodo" ;
	}

	public static int mazeId(WorldEntity e) {
		return (int) e.properties.get("maze") ;
	}
	
	public static boolean isScroll(WorldEntity e) {
		return e.type.startsWith("SC") ;
	}
	
	public static boolean isHealPot(WorldEntity e) {
		return e.type.startsWith("H") ;
	}
	
	public static boolean isRagePot(WorldEntity e) {
		return e.type.startsWith("R") ;
	}
	
	public static boolean isShrine(WorldEntity e) {
		return e.type.startsWith("SH") ;
	}
	
	public static boolean isMoonShrine(WorldEntity e) {
		return e.id.startsWith("SM") ;
	}
	
	public static boolean isSunShrine(WorldEntity e) {
		return e.id.startsWith("SS") ;
	}
	
	public static boolean isImmortalShrine(WorldEntity e) {
		return e.id.startsWith("SI") ;
	}
	
	public static boolean isMonster(WorldEntity e) {
		return e.type.startsWith("M") ;
	}
	
	public static boolean isWall(WorldEntity e) {
		return e.type.startsWith("W") ;
	}
	

	/**
	 * Give the straight-line distance-square between two entities, if they
	 * are in the same maze; else the distance is the difference between
	 * mazeIds times some large multiplier (1000000).
	 */
	public static float distanceBetweenEntities(MyAgentState S, WorldEntity e1, WorldEntity e2) {
		int e1_maze = (int) e1.properties.get("maze") ;
	    int e2_maze = (int) e2.properties.get("maze") ;
	    
	    if (e1_maze == e2_maze) {
	    	var p1 = e1.position.copy() ;
	        var p2 = e2.position.copy() ;
	        p1.y = 0 ;
	        p2.y = 0 ;
	        return  Vec3.distSq(p1, p2) ;
	    }
	    return Math.abs(e1_maze - e2_maze)*1000000 ;
	}

	/**
	 * Give the straight-line distance-square between the agent that owns
	 * the given state and the given entity e, if they
	 * are in the same maze; else the distance is the difference between
	 * their mazeIds times some large multiplier (1000000).
	 */
	public static float distanceToAgent(MyAgentState S, WorldEntity e) {
		var aname = S.worldmodel.agentId ;
	    var player = S.worldmodel.elements.get(aname) ;
	    return distanceBetweenEntities(S, player, e) ;
	}

	/**
	 * check if the location of the entity e is reachable from the 
	 * agent current position.
	 */
	public static boolean isReachable(MyAgentState S, WorldEntity e) {
		var aname = S.worldmodel.agentId ;
	    var player = S.worldmodel.elements.get(aname) ;
	    int player_maze = (int) player.properties.get("maze") ;
	    int e_maze = (int) e.properties.get("maze") ;
	    
		var t1 = toTile(player.position) ;
		var t2 = toTile(e.position) ;
		var path = adjustedFindPath(S, player_maze,t1.x,t1.y,e_maze,t2.x,t2.y) ;
		return path!=null && path.size()>0 ;
	}

	public static List<Tile> adjacentTiles(MyAgentState S, Tile current) {
		List<Tile> adjacents = new LinkedList<>() ;
		Tile q = new Tile(current.x, current.y+1) ;
		if (Utils.inMap(S,q)) adjacents.add(q) ;
		q = new Tile(current.x, current.y-1) ;
		if (Utils.inMap(S,q)) adjacents.add(q) ;
		q = new Tile(current.x+1, current.y) ;
		if (Utils.inMap(S,q)) adjacents.add(q) ;
		q = new Tile(current.x-1, current.y) ;
		if (Utils.inMap(S,q)) adjacents.add(q) ;
		return adjacents ;
	}

	public static boolean inMap(MyAgentState S, Tile tile) {
		int N = S.auxState().getIntProperty("worldSize") ;
		return 0<=tile.x && 0<=tile.y && tile.x < N && tile.y < N ;
	}

	public static int currentMazeNr(MyAgentState S) {
		return (Integer) S.val("maze") ;
	}

	public static int currentMazeNr(WorldModel wom) {
		var A = wom.elements.get(wom.agentId) ;
		return (Integer) A.properties.get("maze") ;
 	}

	public static boolean isFreeTile(MyAgentState S, Tile q) {
		int mazeNr = currentMazeNr(S) ;
		var maze = S.multiLayerNav.areas.get(mazeNr) ;
		return ! maze.isBlocking(q) ;
	}

	public static boolean isWall(MyAgentState S, Tile q) {
		int mazeNr = currentMazeNr(S) ;
		var maze = S.multiLayerNav.areas.get(mazeNr) ;
		return maze.isWall(q.x, q.y) ;
	}

}
