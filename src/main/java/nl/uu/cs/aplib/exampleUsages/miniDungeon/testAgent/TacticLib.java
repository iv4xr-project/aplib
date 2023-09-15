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

/**
 * Provide several basic actions and tactics.
 * 
 * <p>Keep in mind that the provided navigation and exploration
 * tactics/goals currently has no ability to deal with items that
 * block a corridor. The solution is for now to just generate
 * another dungeon where we have no corridors are not blocked by
 * items (use another random seed, for example). A better fix
 * would be to have a smarter navigation and exploration. TO DO.
 * 
 * @author wish
 *
 */
public class TacticLib implements IInteractiveWorldTacticLib<Pair<Integer,Tile>> {
	
	/**
	 * Distance in terms of path-length from the agent that owns S to the entity e. It uses
	 * adjustedFindPath to calculate the path.
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
		//var A = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		//int agentMaze = Utils.mazeId(A) ;
		List<WorldEntity> ms = S.worldmodel.elements.values().stream()
				.filter(e -> e.type.equals(itemType.toString())
						 	 //&& agentMaze == Utils.mazeId(e)
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
	
	/**
	 * Construct an action that would guide the agent to the given location.
	 */
	public Action navigateToAction(int mazeId, int x, int y) {
		return action("move-to")
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
	}
	
	/**
	 * If set to true then {@link #navigateToAction(String)}, and other tactics that use it,
	 * will not keep calculating path to the target entity. Instead, it stores the path,
	 * and then follows the stored path for several turns, or until the path becomes "disconnected"
	 * from the agent. This happens e.g. if the agent decides to move to some direction that is
	 * not along the saved path. If this happens,  {@link #navigateToAction(String) will recalculate
	 * the path.
	 */
	public boolean delayPathReplan = false ;
	
	/**
	 * Construct an action that would guide the agent to a tile adjacent to the target entity.
	 */
	Action navigateToAction(String targetId) {
		// memorize path if instructed to, avoid invoking pathfinder every time 
		List[] memorized = { null } ;
		final int memoryDuration = 10 ;
		int[] memoryCountdown = {0} ;
		return action("move-to")
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
					if (!S.agentIsAlive()) {
						return null ;
					}
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
					
					//System.out.println("###### nav-action " + S.worldmodel.agentId) ;
					List<Pair<Integer,Tile>> path = null ;
					if (delayPathReplan) {
						if (memoryCountdown[0] <= 0) {
							memoryCountdown[0] = memoryDuration ;
						}
						else {
							path = memorized[0] ;
							if (path.size()<=1) {
								//System.out.println("### clear memorized path case 1") ;
								path = null ;
								memoryCountdown[0] = memoryDuration ;
							}
							else {
								path.remove(0) ;
								memoryCountdown[0] -- ;
								Tile next = path.get(0).snd ;
								if(!Utils.adjacent(agentPos,next)) {
									//System.out.println("### clear memorized path case 2") ;
									path = null ;
									memoryCountdown[0] = memoryDuration ;
								}
							}
							//if (path!=null) System.out.println("### using memorized path") ;
						}
					}			
					if (path == null) {
						//System.out.println("### calculating new path") ;
						path = adjustedFindPath(S, Utils.mazeId(a), agentPos.x, agentPos.y, Utils.mazeId(e),target.x, target.y) ;
						if (path == null || path.isEmpty()) {
							return null ;
						}
						path.remove(0) ;
						//System.out.println("### path=" + path) ;
						memorized[0] = path ;
					}
					Tile[] nextTile = {path.get(0).snd} ;
					return nextTile ;
				}) 
				;
	}
	

	@Override
	public Tactic navigateToTac(Pair<Integer,Tile> location) {
		return navigateToAction(location.fst, location.snd.x, location.snd.y).lift() ;
	}
	
	/**
	 * Construct a tactic that would guide the agent to a tile adjacent to the target entity.
	 */
	@Override
	public Tactic navigateToTac(String targetId) {
		return navigateToAction(targetId).lift() ;	
	}
	
	
	/**
	 * Construct an action that would interact with an entity of the given
	 * id. The action's guard is left unconstrained (so the action would
	 * always be enabled). You can use the "on" method to add a guard.
	 */
	Action interactAction(String targetId) {
		return action("interact")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return new Pair<>(S,newwom) ;
				}) ;
	}
	
	/**
	 * This constructs a "default" tactic to interact with an entity. The tactic is
	 * enabled if the entity is known in the agent's state/wom, and if it is 
	 * adjacent to the agent.
	 */
	@Override
	public Tactic interactTac(String targetId) {
		var alpha = interactAction(targetId)
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
	
	public Predicate<MyAgentState> hasHealPot_and_HpLow = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
		return hp>0 && hp<=10 && hasHealPot ;
	} ;
		
	public Predicate<MyAgentState> hasRagePot_and_inCombat = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;
		/*
		System.out.println("#### player: " + player.id
				+ " , hp:" + hp
				+ " , has rage pot " + hasRagePot
				+ " , #adjacent-mosnters:" + S.adajcentMonsters().size()
				) ; */
		return hp>0 && hasRagePot && S.adajcentMonsters().size()>0 ;
	} ;
	
	public Predicate<MyAgentState> inCombat_and_hpNotCritical = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;	
		return hp>5 && S.adajcentMonsters().size()>0 ;
	} ;
	
	/**
	 * Construct an action that would use a healing pot. The action is left unguarded.
	 */
	public Action useHealingPotAction() {
		return action("use healpot")
			.do1((MyAgentState S) -> {
				logger.info(">>> " + S.worldmodel.agentId + " drinks HEALPOT ") ;
				WorldModel newwom = S.env().action(S.worldmodel.agentId, Command.USEHEAL) ;
				return new Pair<>(S,newwom) ;
			}) ;
	}
	
	
	/**
	 * Construct an action that would attack an adjacent monster. The action is
	 * unguarded.
	 */
	public Action attackMonsterAction() { 
		return action("attack")
			.do1((MyAgentState S) -> {
					var ms = S.adajcentMonsters() ;
					// just choose the first one:
					Tile m = Utils.toTile(ms.get(0).position) ;
					logger.info(">>> " + S.worldmodel.agentId + " attacks " + m) ;
					//System.out.println(">>> " + S.worldmodel.agentId + " attacks " + ms.get(0)) ;
					WorldModel newwom = moveTo(S,m) ;
					return new Pair<>(S,newwom) ;
				}) ;
	}
	
	/**
	 * Construct an action that would use a rage pot. The action is left unguarded.
	 */
	public Action useRagePotAction() {
		return action("use ragepot")
			.do1((MyAgentState S) -> {
					logger.info(">>> " + S.worldmodel.agentId + " drinks RAGEPOT ") ;
				    WorldModel newwom = S.env().action(S.worldmodel.agentId, Command.USERAGE) ;
					return new Pair<>(S,newwom) ;
				}) ;
	}

	@Override
	public boolean explorationExhausted(SimpleState S) {
		return ! exploreAction(null).isEnabled(S) ;
	}
	
	/**
	 * Construct an action that would explore the world, in the direction of the given
	 * location.
	 */
	Action exploreAction(Pair<Integer,Tile> heuristicLocation) {
		
		List[] memorized = { null } ;
		final int memoryDuration = 10 ;
		int[] memoryCountdown = {0} ;
		
		Action alpha = action("explore")
				.do2((MyAgentState S) ->  (Tile nextTile) -> {
					WorldModel newwom = moveTo(S,nextTile) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentState S) -> {
					if (!S.agentIsAlive()) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					//System.out.println(">>> agent is " + S.worldmodel().agentId) ;
						
					//System.out.println("### explore is invoked agent @" + agentPos) ;
					
					List<Pair<Integer,Tile>> path = null ;
					if (delayPathReplan) {
						if (memoryCountdown[0] <= 0) {
							memoryCountdown[0] = memoryDuration ;
						}
						else {
							path = memorized[0] ;
							//System.out.println("### about to use memorized path: " + path) ;
							if (path.size()<=1) {
								//System.out.println("### memorized path is singleton or empty, dropping it") ;
								path = null ;
								memoryCountdown[0] = memoryDuration ;
							}
							else {
								path.remove(0) ;
								memoryCountdown[0] -- ;
								Tile next = path.get(0).snd ;
								if(!Utils.adjacent(agentPos,next)) {
									//System.out.println("### next node in memorized path is not adjacent; dropping it") ;
									path = null ;
									memoryCountdown[0] = memoryDuration ;
								}
							}
							//if (path!=null) System.out.println("### using memorized path->" + path.get(0)) ;
							
						}
					}
					if (path == null) {
						if (heuristicLocation == null) {
							//System.out.println(">>> @maze " + Utils.mazeId(a) + ", tile: " + agentPos) ;
						    path = S.multiLayerNav.explore(Utils.loc3(Utils.mazeId(a),agentPos.x, agentPos.y)) ;
						}
						else
							path = S.multiLayerNav.explore(Utils.loc3(Utils.mazeId(a),agentPos.x, agentPos.y), heuristicLocation) ;
						
						if (path == null || path.isEmpty()) {
							//System.out.println(">>>> can't find an explore path!") ;
							return null ;
						}
						path.remove(0) ;
						memorized[0] = path ;
						//System.out.println("### calculated new path-> " + path.get(0)) ;
					}
					
					try {
						return path.get(0).snd ;
					}
					catch(Exception e) {
						//System.out.println(">>> agent @" + agentPos + ", path: " + path) ;
						throw e ;
					}
					//return path.get(1).snd ;
				}) 
				;
		return alpha ;
	}
	
	
	@Override
	public Tactic explore(Pair<Integer,Tile> heuristicLocation) {
		return exploreAction(heuristicLocation).lift() ;
	}


}
