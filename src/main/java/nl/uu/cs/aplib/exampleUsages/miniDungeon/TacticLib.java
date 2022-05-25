package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Monster;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TacticLib {
	
	public static Tile toTile(Vec3 p) {
		return new Tile((int)p.x, (int) p.z ) ;
	}
	
	public static boolean adjacent(Tile tile1, Tile tile2) {
		return (tile1.x == tile2.x && Math.abs(tile1.y - tile2.y) == 1)
				||
			   (tile1.y == tile2.y && Math.abs(tile1.x - tile2.x) == 1) ;
	}
	
	public static GameStatus gameStatus(MyAgentState state) {
		var aux = state.worldmodel.elements.get("aux") ;
		var status = (GameStatus) aux.properties.get("status") ;
		return status ;
	}
	
	public static boolean agentIsAlive(MyAgentState state) {
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
	
	static Pair<Integer,Tile> loc3(int mazeId, int x, int y) {
		return new Pair<>(mazeId, new Tile(x,y)) ;
	}
	
	public static int mazeId(WorldEntity e) {
		return (int) e.properties.get("maze") ;
	}
	
	public static List<Pair<Integer,Tile>> adjustedFindPath(MyAgentState state, 
			int maze0,
			int x0, int y0, 
			int maze1, int x1, int y1) {
		var nav = state.multiLayerNav ;
		boolean srcOriginalBlockingState  = nav.isBlocking(loc3(maze0,x0,y0)) ;
		boolean destOriginalBlockingState = nav.isBlocking(loc3(maze1,x1,y1)) ;
		nav.toggleBlockingOff(loc3(maze0,x0,y0)) ;
		nav.toggleBlockingOff(loc3(maze1,x1,y1)) ;
		var path = nav.findPath(loc3(maze0,x0,y0),loc3(maze1,x1,y1)) ;
		nav.setBlockingState(loc3(maze0,x0,y0),srcOriginalBlockingState) ;
		nav.setBlockingState(loc3(maze1,x1,y1),destOriginalBlockingState) ;
		return path ;
	}
	
	public static List<WorldEntity> adajcentMonsters(MyAgentState S) {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		Tile p = toTile(player.position) ;
		List<WorldEntity> ms = S.worldmodel.elements.values().stream()
				.filter(e -> e.type.equals(EntityType.MONSTER.toString())
						     && mazeId(player) == mazeId(e)
						 	 && adjacent(p,toTile(e.position)))
				.collect(Collectors.toList()) ;
		return ms ;
	}
	
	public static int manhattanDist(Tile t1, Tile t2) {
		return Math.abs(t1.x - t2.x) + Math.abs(t1.y - t2.y) ;
	}

	static int distTo(MyAgentState S, WorldEntity e) {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		Tile p = toTile(player.position) ;
		Tile target = toTile(e.position) ;
		var path = adjustedFindPath(S, mazeId(player), p.x, p.y, mazeId(e), target.x, target.y) ;
		if(path==null) return Integer.MAX_VALUE ;
		return path.size() - 1 ;
	}
	
	public static List<WorldEntity> nearItems(MyAgentState S, EntityType itemType, int withinDistance) {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		Tile p = toTile(player.position) ;
		List<WorldEntity> ms = S.worldmodel.elements.values().stream()
				.filter(e -> e.type.equals(itemType.toString())
						 	 && distTo(S,e) <= withinDistance
						 	 )
				.collect(Collectors.toList()) ;
		ms.sort((e1, e2) -> Integer.compare(distTo(S,e1),distTo(S,e2)));
		return ms ;
	}
	
	
	public static void printEntities(MyAgentState state) {
		int k = 0 ;
		for(var e : state.worldmodel.elements.values()) {
			if (e.type.equals("aux") || e.type.equals("WALL")) continue ;
			System.out.println(">>> " + e.id + " @" + mazeId(e) + "|" + e.position) ;
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
	
	public Tactic navigateTo(int mazeId, int x, int y) {
		var alpha = action("move-to")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return newwom ;
				})
				.on((MyAgentState S) -> {
					if (!agentIsAlive(S)) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = toTile(S.worldmodel.position) ;
					var path = adjustedFindPath(S, mazeId(a), agentPos.x, agentPos.y, mazeId, x, y) ;
					if (path == null) {
						return null ;
					}
					// the first element is the src itself, so we need to pick the next one:
					return path.get(1).snd ;
				}) 
				;
		return alpha.lift() ;
	}
	
	public Tactic navigateTo(String targetId) {
		var alpha = action("move-to")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return newwom ;
				})
				.on((MyAgentState S) -> {
					if (!agentIsAlive(S)) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = toTile(S.worldmodel.position) ;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;
					if (e == null) {
						return null ;
					}
					Tile target = toTile(e.position) ;
					//System.out.println("src: " + agentPos) ;
					//System.out.println("dest: " + target) ;
					// System.out.println(">>> calling pathfinder") ;
					var path = adjustedFindPath(S, mazeId(a), agentPos.x, agentPos.y, mazeId(e),target.x, target.y) ;
					if (path == null) {
						return null ;
					}
					// System.out.println("path: " + path) ; 
					// the first element is the src itself, so we need to pick the next one:
					return path.get(1).snd ;
				}) 
				;
		return alpha.lift() ;
	}
	
	public Tactic interact(String targetId) {
		var alpha = action("interact")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return newwom ;
				})
				.on((MyAgentState S) -> {
					if (!agentIsAlive(S)) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = toTile(S.worldmodel.position) ;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;
					if (e == null || mazeId(a)!=mazeId(e)) {
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
	
	public Predicate<MyAgentState> whenToUseHealPot = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
		return hp>0 && hp<=10 && hasHealPot ;
	} ;
		
	public Predicate<MyAgentState> whenToUseRagePot = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;
		return hp>0 && hasRagePot && adajcentMonsters(S).size()>1 ;
	} ;
	
	public Predicate<MyAgentState> whenToAttack = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		return hp>5 && adajcentMonsters(S).size()>0 ;
	} ;
	
	public Tactic useHealingPot() {
		var alpha = action("use healpot")
				.do1((MyAgentState S) -> {
					System.out.println(">>>> using HEALPOT") ;
					WorldModel newwom = S.env().action(S.worldmodel.agentId, Command.USEHEAL) ;
					return newwom ;
				})
				.on_(whenToUseHealPot)
				;
		return alpha.lift() ;
	}
	
	public Tactic attackMonster() {
		var alpha = action("attack")
				.do1((MyAgentState S) -> {
					var ms = adajcentMonsters(S) ;
					// just choose the first one:
					Tile m = toTile(ms.get(0).position) ;
					System.out.println(">>> Attack " + m) ;
					WorldModel newwom = moveTo(S,m) ;
					return newwom ;
				})
				.on_(whenToAttack)
				;
		return alpha.lift() ;
	}
	
	public Tactic useRagePot() {
		var alpha = action("use ragepot")
				.do1((MyAgentState S) -> {
					WorldModel newwom = S.env().action(S.worldmodel.agentId, Command.USERAGE) ;
					return newwom ;
				})
				.on_(whenToUseRagePot)
				;
		return alpha.lift() ;
	}
	
	public Action explore() {
		var alpha = action("explore")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return newwom ;
				})
				.on((MyAgentState S) -> {
					if (!agentIsAlive(S)) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = toTile(S.worldmodel.position) ;
					//System.out.println(">>> explore is invoked") ;
					var path = S.multiLayerNav.explore(loc3(mazeId(a),agentPos.x, agentPos.y)) ;
					if (path == null) {
						//System.out.println(">>>> can't find an explore path!") ;
						return null ;
					}
					return path.get(1).snd ;
				}) 
				;
		return alpha ;
	}
	
	public Tactic exploreTactic() {
		return explore().lift() ;
	}

}
