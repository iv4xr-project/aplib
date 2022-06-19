package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.IInteractiveWorldTacticLib;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Monster;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TacticLib implements IInteractiveWorldTacticLib<Pair<Integer,Tile>> {
	
	/**
	 * Distance in terms of path-length from the agent that owns S to the entity e.
	 */
	static int distTo(MyAgentState S, WorldEntity e) {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		Tile p = Utils.toTile(player.position) ;
		Tile target = Utils.toTile(e.position) ;
		var path = TacticLib.adjustedFindPath(S, Utils.mazeId(player), p.x, p.y, Utils.mazeId(e), target.x, target.y) ;
		if(path==null) return Integer.MAX_VALUE ;
		return path.size() - 1 ;
	}
	
	/**
	 * Calculate a path from (x0,y0) in maze-0 to (x1,y1) in maze-1. The method
	 * will pretend that the source (x0,y0) and destination (x1,y1) are non-blocking
	 * (even if they are, e.g. if one of them is an occupied tile). 
	 */
	public static List<Pair<Integer,Tile>> adjustedFindPath(MyAgentState state, 
			int maze0,
			int x0, int y0, 
			int maze1, int x1, int y1) {
		var nav = state.multiLayerNav ;
		boolean srcOriginalBlockingState  = nav.isBlocking(Utils.loc3(maze0,x0,y0)) ;
		boolean destOriginalBlockingState = nav.isBlocking(Utils.loc3(maze1,x1,y1)) ;
		nav.toggleBlockingOff(Utils.loc3(maze0,x0,y0)) ;
		nav.toggleBlockingOff(Utils.loc3(maze1,x1,y1)) ;
		var path = nav.findPath(Utils.loc3(maze0,x0,y0),Utils.loc3(maze1,x1,y1)) ;
		nav.setBlockingState(Utils.loc3(maze0,x0,y0),srcOriginalBlockingState) ;
		nav.setBlockingState(Utils.loc3(maze1,x1,y1),destOriginalBlockingState) ;
		return path ;
	}
	
	/**
	 * Return the list of items near to the agent that owns the state S.
	 */
	public static List<WorldEntity> nearItems(MyAgentState S, EntityType itemType, int withinDistance) {
		//var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		//Tile p = toTile(player.position) ;
		List<WorldEntity> ms = S.worldmodel.elements.values().stream()
				.filter(e -> e.type.equals(itemType.toString())
						 	 && TacticLib.distTo(S,e) <= withinDistance
						 	 )
				.collect(Collectors.toList()) ;
		ms.sort((e1, e2) -> Integer.compare(TacticLib.distTo(S,e1),TacticLib.distTo(S,e2)));
		return ms ;
	}
	
	Logger logger = Logging.getAPLIBlogger() ;
	

	public WorldModel moveTo(MyAgentState state, Tile targetTile) {
		Tile t0 = Utils.toTile(state.worldmodel.position) ;
		if(!Utils.adjacent(t0,targetTile))
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
	
	@Override
	public Tactic navigateTo(Pair<Integer,Tile> location) {
		return navigateTo(location.fst, location.snd.x, location.snd.y) ;
	}
	
	public Tactic navigateTo(int mazeId, int x, int y) {
		var alpha = action("move-to")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentState S) -> {
					if (!S.agentIsAlive()) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					var path = adjustedFindPath(S, Utils.mazeId(a), agentPos.x, agentPos.y, mazeId, x, y) ;
					if (path == null) {
						return null ;
					}
					// the first element is the src itself, so we need to pick the next one:
					return path.get(1).snd ;
				}) 
				;
		return alpha.lift() ;
	}
	
	@Override
	public Tactic navigateTo(String targetId) {
		var alpha = action("move-to")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentState S) -> {
					if (!S.agentIsAlive()) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;
					if (e == null) {
						//System.out.println("%%%% uknown: " + targetId) ;
						return null ;
					}
					Tile target = Utils.toTile(e.position) ;
					//System.out.println("src: " + agentPos) ;
					//System.out.println("dest: " + target) ;
					// System.out.println(">>> calling pathfinder") ;
					var path = adjustedFindPath(S, Utils.mazeId(a), agentPos.x, agentPos.y, Utils.mazeId(e),target.x, target.y) ;
					if (path == null) {
						//System.out.println(">>>> can't find path to: " + targetId) ;
						return null ;
					}
					// System.out.println("path: " + path) ; 
					// the first element is the src itself, so we need to pick the next one:
					return path.get(1).snd ;
				}) 
				;
		return alpha.lift() ;
	}
	
	/**
	 * A variation of {@link navigateTo} that guides the agent up to a tile adjacent to
	 * the target.
	 */
	public Tactic navigateNextTo(String targetId) {
		var alpha = action("move-to")
				.do2((MyAgentState S) ->  (Tile[] nextTile) -> {
					if (nextTile.length == 0) {
						return new Pair<>(S,S.env().observe(S.worldmodel().agentId)) ;
					}
					WorldModel newwom = moveTo(S,nextTile[0]) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentState S) -> {
					// return three possible values:
					//   (1) null --> the action is not enabled
					//   (2) empty array of tiles --> the agent is already next to the target
					//   (3) a singleton array of tile --> the next tile to move to
					//
					if (!S.agentIsAlive()) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;
					if (e == null) {
						return null ;
					}
					Tile target = Utils.toTile(e.position) ;
					if (Utils.mazeId(a)==Utils.mazeId(e) && Utils.adjacent(agentPos,target)) {
						Tile[] nextTile = {} ;
						return nextTile ;
					}
					var path = adjustedFindPath(S, Utils.mazeId(a), agentPos.x, agentPos.y, Utils.mazeId(e),target.x, target.y) ;
					if (path == null) {
						return null ;
					}
					Tile[] nextTile = {path.get(1).snd} ;
					return nextTile ;
				}) 
				;
		return alpha.lift() ;
	}
	
	@Override
	public Tactic interact(String targetId) {
		var alpha = action("interact")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentState S) -> {
					if (!S.agentIsAlive()) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;
					if (e == null || Utils.mazeId(a)!=Utils.mazeId(e)) {
						return null ;
					}
					Tile target = Utils.toTile(e.position) ;
					if (Utils.adjacent(agentPos,target)) {
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
		return hp>0 && hasRagePot && S.adajcentMonsters().size()>1 ;
	} ;
	
	public Predicate<MyAgentState> whenToAttack = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		return hp>5 && S.adajcentMonsters().size()>0 ;
	} ;
	
	Action actionUseHealingPot = action("use healpot")
			.do1((MyAgentState S) -> {
				logger.info(">>> " + S.worldmodel.agentId + " drinks HEALPOT ") ;
				WorldModel newwom = S.env().action(S.worldmodel.agentId, Command.USEHEAL) ;
				return new Pair<>(S,newwom) ;
			}) ;
	
	Tactic useHealingPotWhenHpLow = actionUseHealingPot
			.on_(whenToUseHealPot)
			.lift() ;
	
	Tactic attackMonster = action("attack")
			.do1((MyAgentState S) -> {
					var ms = S.adajcentMonsters() ;
					// just choose the first one:
					Tile m = Utils.toTile(ms.get(0).position) ;
					logger.info(">>> " + S.worldmodel.agentId + " attacks " + m) ;
					WorldModel newwom = moveTo(S,m) ;
					return new Pair<>(S,newwom) ;
				})
			.on_(whenToAttack)
			.lift() ;
	
	
	Tactic useRagePot = action("use ragepot")
			.do1((MyAgentState S) -> {
					logger.info(">>> " + S.worldmodel.agentId + " drinks RAGEPOT ") ;
				    WorldModel newwom = S.env().action(S.worldmodel.agentId, Command.USERAGE) ;
					return new Pair<>(S,newwom) ;
				})
			.on_(whenToUseRagePot)
			.lift() ;

	@Override
	public boolean explorationExhausted(SimpleState S) {
		return ! exploreAction(null).isEnabled(S) ;
	}
	
	Action exploreAction(Pair<Integer,Tile> heuristicLocation) {
		Action alpha = action("explore")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentState S) -> {
					if (!S.agentIsAlive()) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					//System.out.println(">>> explore is invoked") ;
					List<Pair<Integer,Tile>> path ;
					if (heuristicLocation == null)
					    path = S.multiLayerNav.explore(Utils.loc3(Utils.mazeId(a),agentPos.x, agentPos.y)) ;
					else
						path = S.multiLayerNav.explore(Utils.loc3(Utils.mazeId(a),agentPos.x, agentPos.y), heuristicLocation) ;
					if (path == null) {
						//System.out.println(">>>> can't find an explore path!") ;
						return null ;
					}
					return path.get(1).snd ;
				}) 
				;
		return alpha ;
	}
	
	
	@Override
	public Tactic explore(Pair<Integer,Tile> heuristicLocation) {
		return exploreAction(heuristicLocation).lift() ;
	}


}
