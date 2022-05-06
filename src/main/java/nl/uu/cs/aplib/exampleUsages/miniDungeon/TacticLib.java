package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import static nl.uu.cs.aplib.AplibEDSL.* ;

import java.util.List;

public class TacticLib {
	
	static Tile toTile(Vec3 p) {
		return new Tile((int)p.x, (int) p.z ) ;
	}
	
	static boolean adjacent(Tile tile1, Tile tile2) {
		return (tile1.x == tile2.x && Math.abs(tile1.y - tile2.y) == 1)
				||
			   (tile1.y == tile2.y && Math.abs(tile1.x - tile2.x) == 1) ;
	}
	
	static boolean agentIsAlive(MyAgentState state) {
		var a = state.worldmodel.elements.get(state.worldmodel().agentId) ;
		if (a==null) {
			throw new IllegalArgumentException() ;
		}
		var hp = (Integer) a.properties.get("hp") ;
		if (hp==null) {
			throw new IllegalArgumentException() ;
		}
		return hp>0 ;
	}
	
	static List<Tile> adjustedFindPath(MyAgentState state, int x0, int y0, int x1, int y1) {
		var nav = state.worldNavigation() ;
		nav.openDoor(x0,y0) ;
		nav.openDoor(x1,y1) ;
		var path = nav.findPath(x0,y0,x1,y1) ;
		nav.closeDoor(x0,y0) ;
		nav.closeDoor(x1,y1) ;
		return path ;
	}
	
	static void printEntities(MyAgentState state) {
		int k = 0 ;
		for(var e : state.worldmodel.elements.values()) {
			if (e.type.equals("aux") || e.type.equals("Wall")) continue ;
			System.out.println(">>> " + e.id + " @" + e.position) ;
			k++ ;
		}
		System.out.println(">>> #entities=" + k) ;
	}
	
	public WorldModel moveTo(MyAgentState state, Tile targetTile) {
		Tile t0 = toTile(state.worldmodel.position) ;
		if(!adjacent(t0,targetTile))
				throw new IllegalArgumentException() ;
		Command cmd = null ;
		//System.out.println(">>> moveTo start") ;
		if (targetTile.y > t0.y) 
			cmd = Command.MOVEUP ;
		else if (targetTile.y < t0.y) 
			cmd = Command.MOVEDOWN ;
		else if (targetTile.x > t0.x)
			cmd = Command.MOVERIGHT ;
		else
			cmd = Command.MOVELEFT ;
		var wom = state.env().action(state.worldmodel.agentId, cmd) ;
		//System.out.println(">>> moveTo end") ;
		return wom ;
	}
	
	public Tactic navigateTo(int x, int y) {
		var alpha = action("navigate-to")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return newwom ;
				})
				.on((MyAgentState S) -> {
					if (!agentIsAlive(S)) return null ;
					Tile agentPos = toTile(S.worldmodel.position) ;
					var path = adjustedFindPath(S,agentPos.x, agentPos.y, x, y) ;
					if (path == null) {
						return null ;
					}
					// the first element is the src itself, so we need to pick the next one:
					return path.get(1) ;
				}) 
				;
		return alpha.lift() ;
	}
	
	public Tactic navigateTo(String targetId) {
		var alpha = action("navigate-to")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return newwom ;
				})
				.on((MyAgentState S) -> {
					if (!agentIsAlive(S)) return null ;
					Tile agentPos = toTile(S.worldmodel.position) ;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;
					if (e == null) {
						return null ;
					}
					Tile target = toTile(e.position) ;
					//System.out.println("src: " + agentPos) ;
					//System.out.println("dest: " + target) ;
					// System.out.println(">>> calling pathfinder") ;
					var path = adjustedFindPath(S,agentPos.x, agentPos.y, target.x, target.y) ;
					if (path == null) {
						return null ;
					}
					// System.out.println("path: " + path) ; 
					// the first element is the src itself, so we need to pick the next one:
					return path.get(1) ;
				}) 
				;
		return alpha.lift() ;
	}
	
	Tactic interact(String targetId) {
		var alpha = action("navigate-to")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return newwom ;
				})
				.on((MyAgentState S) -> {
					if (!agentIsAlive(S)) return null ;
					Tile agentPos = toTile(S.worldmodel.position) ;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;
					if (e == null) {
						return null ;
					}
					Tile target = toTile(e.position) ;
					if (adjacent(agentPos,target)) {
						return target ;
					}
					return null ;
				}) 
				;
		return alpha.lift() ;
	}
	
	Tactic explore() {
		var alpha = action("explore")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return newwom ;
				})
				.on((MyAgentState S) -> {
					if (!agentIsAlive(S)) return null ;
					Tile agentPos = toTile(S.worldmodel.position) ;
					S.worldNavigation().openDoor(agentPos.x, agentPos.y) ;
					System.out.println(">>> explore is invoked") ;
					var candidates = S.worldNavigation().explore(agentPos.x, agentPos.y) ;
					S.worldNavigation().closeDoor(agentPos.x, agentPos.y) ;
					System.out.println(">>> explore is invoked, candidates: " + candidates) ;
					if (candidates.size() == 0) {
						return null ;
					}
					for(var t : candidates) {
						var path = adjustedFindPath(S,agentPos.x, agentPos.y, t.x, t.y) ;
						if (path != null && path.size()>1) {
							return path.get(1) ;
						}
					}
					return null ;
				}) 
				;
		return alpha.lift() ;
	}

}
