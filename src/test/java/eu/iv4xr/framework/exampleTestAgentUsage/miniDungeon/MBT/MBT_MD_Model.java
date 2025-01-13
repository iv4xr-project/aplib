package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import java.io.Serializable;
import java.util.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.mbt.MBTAction;
import eu.iv4xr.framework.extensions.mbt.MBTModel;
import eu.iv4xr.framework.extensions.mbt.MBTPostCondition;
import eu.iv4xr.framework.extensions.mbt.MBTRunner;
import eu.iv4xr.framework.extensions.mbt.MBTState;
import eu.iv4xr.framework.extensions.mbt.SimpleGame;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph;
import eu.iv4xr.framework.extensions.mbt.MBTRunner.ACTION_SELECTION;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;

public class MBT_MD_Model {
	
	public static int DELAY_BETWEEN_UPDATE = 10 ;
	
	public static Random rndx = new Random(373) ;
	
	public static void WAIT() {
		try {
			Thread.sleep(DELAY_BETWEEN_UPDATE);
		}
		catch(Exception e) { }
	}
	
	public static boolean inCombat(MyAgentState S) {
		return ! S.adajcentMonsters().isEmpty() ;
	}
	
	public static boolean justKilledAMonster(MyAgentState S) {
		var recentlyRemoved = (String[]) S.auxState().properties.get("recentlyRemoved") ;
		for (var e : recentlyRemoved) {
			if (e.startsWith("M")) return true ;
		}
		return false ;
	}
	
	public static int totAdjacentMonstersHp(MyAgentState S) {
		return S.adajcentMonsters().stream()
				.map(e -> int_(e.properties.get("hp")))
				.collect(Collectors.summingInt(x -> x)) ;
	}
	
	public static WorldEntity adjacentNonWallEntity(MyAgentState S, EntityType ty) {
		var z = TacticLib.nearItems(S,ty,1) ;
		return z.isEmpty() ? null : z.get(rndx.nextInt(z.size())) ;
	}
	
	
	public static boolean isAdjacentToNonWallObject(MyAgentState S) {
		var maze = Utils.currentMazeNr(S) ;
		var agentTile = Utils.toTile(S.worldmodel.position) ;
		for (var e : S.worldmodel.elements.values()) {
			if (e.type != null
					&& (Utils.isHealPot(e) || Utils.isRagePot(e) || Utils.isShrine(e) || Utils.isMonster(e) 
					|| e.type.equals(EntityType.FRODO.toString())
					|| e.type.equals(EntityType.SMEAGOL.toString())
					)
					&& Utils.mazeId(e) == maze 
					&& Utils.adjacent(Utils.toTile(e.position), agentTile)) {
				return true ;
			}
		}
		return false ;
	}
	
	public static WorldEntity adjacentWall(MyAgentState S) {
		var maze = Utils.currentMazeNr(S) ;
		var agentTile = Utils.toTile(S.worldmodel.position) ;
		for (var e : S.worldmodel.elements.values()) {
			if (Utils.isWall(e)
					&& Utils.mazeId(e) == maze 
					&& Utils.adjacent(Utils.toTile(e.position), agentTile)) {
				return e ;
			}
		}
		return null ;
	}
	
	public static WorldEntity adjacentShrine(MyAgentState S, ShrineType sty) {
		var e = adjacentNonWallEntity(S,EntityType.SHRINE) ;
		if (e == null) 
			return null ;
		return (ShrineType) e.properties.get("shrinetype") == sty ? e : null ;
	}
	

	public static WorldEntity adjacentPickableItem(MyAgentState S) {
		WorldEntity e = adjacentNonWallEntity(S, EntityType.SCROLL) ;
		if (e != null) return e ; 
		e = adjacentNonWallEntity(S, EntityType.RAGEPOT) ;
		if (e != null) return e ;
		return adjacentNonWallEntity(S, EntityType.HEALPOT) ;
	}
	
	/**
	 * Return the walls that have a neighboring free tile, so they can be 'touched'.
	 */
	public static List<WorldEntity> touchableWalls(MyAgentState S) {
		List<WorldEntity> WS = new LinkedList<>() ;
		for (var e : S.worldmodel.elements.values()) {
			if (Utils.isWall(e) && 
				Utils.mazeId(e) == Utils.currentMazeNr(S)) {
				var ntiles = Utils.adjacentTiles(S, Utils.toTile(e.position)) ;
				for (var t : ntiles) {
					if (Utils.isFreeTile(S, t)) {
						WS.add(e) ;
						break ;
					}
				}
			}
		}
		return WS ;	
	}
	
	public static List<WorldEntity> entitiesInSameMaze(MyAgentState S, EntityType ty, ShrineType sty) {
		int mazeNr = Utils.currentMazeNr(S) ;
		var Z = S.worldmodel.elements.values().stream()
					   .filter(e -> e.type != null && e.type.equals("" + ty) && Utils.mazeId(e) == mazeNr) ;
		if (sty != null)	   
			Z = Z.filter(e -> e.properties.get("shrinetype") != null
							  && ((ShrineType) e.properties.get("shrinetype")) == sty) ;
		
		return Z.collect(Collectors.toList()) ;
	}
	
	public static WorldEntity closedShrineInSameMaze(MyAgentState S) {
		int mazeNr = Utils.currentMazeNr(S) ;
		var Z = S.worldmodel.elements.values().stream()
					   .filter(e -> Utils.isShrine(e)
					      && Utils.mazeId(e) == mazeNr
					      && ! bool_(e.properties.get("cleansed"))) 
					   .collect(Collectors.toList()) ;
		return Z.isEmpty() ? null : Z.get(0) ;
	}
	
	public static int int_(Serializable val) {
		return (Integer) val ;
	}
	
	public static boolean bool_(Serializable val) {
		return (Boolean) val ;
	}
	
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> usepot(EntityType ty) {
		return new MBTAction<MyAgentState>("use-" + ty)
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					if (ty == EntityType.HEALPOT)
						S.env().action(S.worldmodel.agentId, Command.USEHEAL) ;
					else 
						S.env().action(S.worldmodel.agentId, Command.USERAGE) ;
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				})
				
				.addGuards(S -> 
					S.agentIsAlive() &&
					(ty == EntityType.HEALPOT ?
						int_(S.val("healpotsInBag")) > 0 
					  : 
					 	int_(S.val("ragepotsInBag")) > 0)) 
				
				.addPostConds(new MBTPostCondition<MyAgentState>("" + ty + " used", 
					S -> ty == EntityType.HEALPOT ?
						    int_(S.val("healpotsInBag")) == int_(S.before("healpotsInBag")) - 1 
							&& (int_(S.val("hp")) > int_(S.before("hp"))
							    || int_(S.before("hp")) >= int_(S.val("hpmax")) - 4 ) 
							:
							int_(S.val("ragepotsInBag")) == int_(S.before("ragepotsInBag")) - 1  
							&& int_(S.val("rageTimer")) == 9 
						))
				
				
				;
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> usepot_neg(EntityType ty) {
		var A = usepot(ty) ;
		A.name = "use-but-dont-have-" + ty ;
		A.guards.clear();
		A.postConditions.clear();
		A.addGuards(S -> 
		            ty == EntityType.HEALPOT ?
						int_(S.val("healpotsInBag")) == 0 
						: 
						int_(S.val("ragepotsInBag")) == 0) ;
		A.addPostConds(new MBTPostCondition<MyAgentState>("no item used", 
				 S -> int_(S.val("bagUsed")) == int_(S.before("bagUsed")))) ;
		return A ;
		
	}
	
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> pickUpItem(EntityType ty) {
		return new MBTAction<MyAgentState>("pickup-" + ty)
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var e = TacticLib.nearItems(S,ty,1).get(0) ;
					new TacticLib() . moveTo(S, Utils.toTile(e.position)) ;
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> 
					S.agentIsAlive() &&
					int_(S.val("bagUsed")) < int_(S.val("maxBagSize")) && adjacentNonWallEntity(S,ty) != null )
				
				.addPostConds(
						new MBTPostCondition<MyAgentState>("bag-cap-respected",
						S -> int_(S.val("bagUsed")) <= int_(S.val("maxBagSize"))),
						
						new MBTPostCondition<MyAgentState>("" + ty + " added to bag", 
						S -> {
							switch(ty) {
							case HEALPOT: return int_(S.val("healpotsInBag")) == int_(S.before("healpotsInBag")) + 1 ;
							case RAGEPOT: return int_(S.val("ragepotsInBag")) == int_(S.before("ragepotsInBag")) + 1 ;
							case SCROLL : return int_(S.val("scrollsInBag")) == int_(S.before("scrollsInBag")) + 1 ;
							}
							return true ;
						}
				)) ; 
	}
	
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> pickUpItem_neg() {
		return new MBTAction<MyAgentState>("pickup-item-but-full")
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var e = adjacentPickableItem(S) ;
					new TacticLib() . moveTo(S, Utils.toTile(e.position)) ;
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> adjacentPickableItem(S) != null && int_(S.val("bagUsed")) >= int_(S.val("maxBagSize")))
				.addPostConds(new MBTPostCondition<MyAgentState>("no item added", 
						S -> int_(S.val("bagUsed")) == int_(S.before("bagUsed")))) ; 
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> attackMonster() {
		return new MBTAction<MyAgentState>("attack-monster")
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					// just target the first monster in the list:
					var m = S.adajcentMonsters().get(0) ;
					new TacticLib() . moveTo(S, Utils.toTile(m.position)) ;
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() && inCombat(S))
				.addPostConds(
						new MBTPostCondition<MyAgentState>("hp-decrease",
						S -> justKilledAMonster(S) || int_(S.val("hp")) < int_(S.before("hp"))),
						
						new MBTPostCondition<MyAgentState>("monster-killed-or-hp-decrease", 
						S -> justKilledAMonster(S)
							|| totAdjacentMonstersHp(S)  < 
									S.adajcentMonsters().stream()
										.map(e -> int_(S.before(e.id,"hp")))
										.collect(Collectors.summingInt(x -> x)))) 
				 ; 
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> bumpWall(int budget,boolean withSurvival) {
		return new MBTAction<MyAgentState>("bump-wall")
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var walls = touchableWalls(S) ;
					var W = walls.get(rndx.nextInt(walls.size())) ;
					GoalStructure G = ExtraGoalLib.nextToWall(S,W) ;
					agent.dropAll() ;
					agent.setGoal(G) ;
					int k = 0 ;
					while (k < budget && G.getStatus().inProgress()) {
						agent.update() ;
						WAIT() ;
						k ++ ;
					}
					S.updateState(agent.getId()) ;
					S = (MyAgentState) agent.state() ;
					W = adjacentWall(S) ;
					if (W == null) {
						// G is not reached ... maybe too short budget, in this case
						// we don't bump. Just return true, so as not to signal that this
						// action fails:
						return true ;
					}
					// bump:
					new TacticLib() . moveTo(S, Utils.toTile(W.position)) ;
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() && ! isAdjacentToNonWallObject(S))
				.addPostConds(
						new MBTPostCondition<MyAgentState>("remain-in-same-position",
						S -> S.worldmodel.position.equals(S.positionBefore()))
				 ); 
	}
	
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> tryToCleanseShrine(int budget,boolean withSurvival) {
		return new MBTAction<MyAgentState>("cleanse-shrine")
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var shrine = closedShrineInSameMaze(S) ;
					GoalStructure G = withSurvival ?
							  (new GoalLib()).smartEntityInCloseRange(agent,shrine.id) 
							: (new GoalLib()).entityInCloseRange(shrine.id) ;
						agent.dropAll() ;
						agent.setGoal(G) ;
						int k = 0 ;
						while (k < budget && G.getStatus().inProgress()) {
							agent.update() ;
							WAIT() ;
							k ++ ;
						}
					S.updateState(agent.getId()) ;
					S = (MyAgentState) agent.state() ;
					if (! Utils.adjacent(Utils.toTile(shrine.position), Utils.toTile(S.worldmodel.position))) {
						// G is not reached ... maybe too short budget, in this case
						// we obviously can't use the shrine. Just return true, so as not to signal that this
						// action fails:
						return true ;
					}
					// bump:
					new TacticLib() . moveTo(S, Utils.toTile(shrine.position)) ;
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() && closedShrineInSameMaze(S) != null)
				.addPostConds(
						new MBTPostCondition<MyAgentState>("shrine could be cleansed",
						S -> closedShrineInSameMaze(S) != null 
						     || int_(S.val("scrollsInBag")) == int_(S.before("scrollsInBag")) - 1  )
				 ); 
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> teleport(ShrineType sty, int budget,boolean withSurvival) {
		return new MBTAction<MyAgentState>("teleport-" + sty)
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var shrine = entitiesInSameMaze(S, EntityType.SHRINE,sty).get(0) ;
					GoalStructure G = withSurvival ?
							  (new GoalLib()).smartEntityInCloseRange(agent,shrine.id) 
							: (new GoalLib()).entityInCloseRange(shrine.id) ;
						agent.dropAll() ;
						agent.setGoal(G) ;
						int k = 0 ;
						while (k < budget && G.getStatus().inProgress()) {
							agent.update() ;
							WAIT() ;
							k ++ ;
						}
					S.updateState(agent.getId()) ;
					S = (MyAgentState) agent.state() ;
					if (! Utils.adjacent(Utils.toTile(shrine.position), Utils.toTile(S.worldmodel.position))) {
						// G is not reached ... maybe too short budget, in this case
						// we obviously can't use the shrine. Just return true, so as not to signal that this
						// action fails:
						return true ;
					}
					// bump:
					new TacticLib() . moveTo(S, Utils.toTile(shrine.position)) ;
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() 
						    && ! entitiesInSameMaze(S, EntityType.SHRINE,sty).isEmpty()
							&& closedShrineInSameMaze(S) == null)
				.addPostConds(
						new MBTPostCondition<MyAgentState>("could be teleported to the next maze",
						S -> { 
							//System.out.println("------- pos: " + S.worldmodel.position) ;
							//System.out.println("------- maze: " + S.val("maze")) ;
							//System.out.println("------- pos-before: " + S.positionBefore()) ;
							//System.out.println("------- maze-before: " + S.before("maze")) ;
							
							return S.worldmodel.position.equals(S.positionBefore())  
						     || (int_(S.val("maze")) == int_(S.before("maze")) 
						                + (sty == ShrineType.MoonShrine ? 1 : -1)) ; 
							}
						)
						    
				 ); 
	}
	
	
	/**
	 * Travel to an entity, except Wall.
	 */
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> travelToNonWallObject(EntityType ty, ShrineType sty, 
			int budget,
			boolean withSurvival) {
		return new MBTAction<MyAgentState>("travel-to-" + (sty != null ? sty.toString() : ty.toString()))
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var candidates = entitiesInSameMaze(S,ty,sty);
					//System.out.println("------- agent: " + S.worldmodel.position);
					//System.out.println("------- maze: " + S.val("maze")) ;
					WorldEntity e = candidates.get(rndx.nextInt(candidates.size())) ;
					//System.out.println("------- travel-to target : " + e.id + ", maze:" + S.val(e.id,"maze")) ;
					GoalStructure G = withSurvival ?
						  (new GoalLib()).smartEntityInCloseRange(agent,e.id) 
						: (new GoalLib()).entityInCloseRange(e.id) ;
					agent.dropAll() ;
					agent.setGoal(G) ;
					int k = 0 ;
					while (k < budget && G.getStatus().inProgress()) {
						agent.update() ;
						WAIT() ;
						k ++ ;
					}
					S.updateState(agent.getId()) ;
					// return true, even if it fails to reach the target:
					return true ;
				}) 
				.addGuards(S -> {
					if (! S.agentIsAlive()) return false ;
					
				   // not enabled if the agent is already next to an entity ty
				   if (adjacentNonWallEntity(S,ty) != null)
					   return false ;
				   	   
				   return ! entitiesInSameMaze(S,ty,sty).isEmpty() ;
				})
				// no post-cond for travel
				; 
	}
	
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> alive() {
		var Z = new  MBTState<MyAgentState> ("alive") ;
		Z.addPredicates(S -> S.agentIsAlive()) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> gameover() {
		var Z = new  MBTState<MyAgentState> ("gameover") ;
		Z.addPredicates(S -> S.gameStatus() != GameStatus.INPROGRESS) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> inCombat() {
		var Z = new  MBTState<MyAgentState> ("in-combat") ;
		Z.addPredicates(S -> inCombat(S)) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> adjacentTo(EntityType ty) {
		var Z = new  MBTState<MyAgentState> ("adjacent-to-" + ty) ;
		Z.addPredicates(S -> adjacentNonWallEntity(S,ty) != null) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> adjacentToWall() {
		var Z = new  MBTState<MyAgentState> ("adjacent-to-WALL") ;
		Z.addPredicates(S -> adjacentWall(S) != null) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> inBag(EntityType ty) {
		var Z = new  MBTState<MyAgentState> ("in-bag-" + ty) ;
		Z.addPredicates(S -> {
			switch(ty) {
			case HEALPOT: return int_(S.val("healpotsInBag")) > 0 ;
			case RAGEPOT: return int_(S.val("ragepotsInBag")) > 0 ;
			case SCROLL : return int_(S.val("scrollsInBag")) > 0 ;
			}
			return false ;
		}) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> bagfull() {
		var Z = new  MBTState<MyAgentState> ("bag-full") ;
		Z.addPredicates(S -> int_(S.val("bagUsed")) >= int_(S.val("maxBagSize"))  ) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> enraged() {
		var Z = new  MBTState<MyAgentState> ("enraged") ;
		Z.addPredicates(S -> int_(S.val("rageTimer")) > 0 ) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> healed() {
		var Z = new  MBTState<MyAgentState> ("healed") ;
		Z.addPredicates(S -> 
		   S.before("hp") != null
		   && int_(S.val("hp")) > int_(S.before("hp"))) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> shrineCleaned() {
		var Z = new  MBTState<MyAgentState> ("shrine-cleaned") ;
		Z.addPredicates(S -> {
			var moon = entitiesInSameMaze(S,EntityType.SHRINE,ShrineType.MoonShrine) ;
			var imm = entitiesInSameMaze(S,EntityType.SHRINE,ShrineType.ShrineOfImmortals) ;
			moon.addAll(imm) ;
			var sh = moon.get(0) ;
			return S.before(sh.id,"cleansed") != null
					&& bool_(S.val(sh.id,"cleansed")) != bool_(S.before(sh.id,"cleansed")) ;
		}) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTModel<MyAgentState> MD_model0(int travelBudget, boolean withSurvival) {
		var model = new MBTModel<MyAgentState>("MD-model0") ;
		model.addStates(
				alive(),
				gameover(),
				inCombat(),
				adjacentTo(EntityType.HEALPOT),
				adjacentTo(EntityType.RAGEPOT),
				adjacentTo(EntityType.SCROLL),
				adjacentTo(EntityType.MONSTER),
				adjacentToWall(),
				inBag(EntityType.HEALPOT),
				inBag(EntityType.RAGEPOT),
				inBag(EntityType.SCROLL),
				bagfull(),
				healed(), 
				shrineCleaned(),
				enraged()
				) ;
		
		model.addActions(
				travelToNonWallObject(EntityType.RAGEPOT,null,travelBudget,withSurvival),
				pickUpItem(EntityType.RAGEPOT),
				usepot(EntityType.RAGEPOT)
				) ;
		
		return model  ;
	}
	
	// most complete model
	@SuppressWarnings("unchecked")
	static MBTModel<MyAgentState> MD_model1(int travelBudget, boolean withSurvival) {
		var model = MD_model0(travelBudget,withSurvival) ;
		model.name = "MD-model1" ;
		
		model.addActions(
				travelToNonWallObject(EntityType.HEALPOT,null,travelBudget,withSurvival),
				travelToNonWallObject(EntityType.SCROLL,null,travelBudget,withSurvival),
				travelToNonWallObject(EntityType.MONSTER,null,travelBudget,withSurvival),
				pickUpItem(EntityType.HEALPOT),
				pickUpItem(EntityType.SCROLL),
				pickUpItem_neg(),
				usepot(EntityType.HEALPOT),
				usepot_neg(EntityType.RAGEPOT),
				usepot_neg(EntityType.HEALPOT),
				attackMonster(),
				bumpWall(travelBudget,withSurvival),
				tryToCleanseShrine(travelBudget,withSurvival),
				teleport(ShrineType.MoonShrine, travelBudget,withSurvival)				
				) ;
		
		return model ;
		
	}
	
	// singleton to hold an instance of MD
	static DungeonApp miniDungeonInstance = null ;
	
	public static TestAgent agentRestart(String agentId, 
			MiniDungeonConfig config,
			boolean withSound,
			boolean withGraphics 
			)  {
		
		if (miniDungeonInstance==null ||! miniDungeonInstance.dungeon.config.toString().equals(config.toString())) {
			// there is no MD-instance yet, or if the config is different than the config of the
			// running MD-instance, then we create a fresh MD instance:
			DungeonApp app = null ;
			try {
				app = new DungeonApp(config);
			}
			catch(Exception e) {
				miniDungeonInstance = null ;
				return null ;
			}
			// setting sound on/off, graphics on/off etc:
			app.soundOn = withSound ;
			app.headless = ! withGraphics ;
			if(withGraphics) 
				DungeonApp.deploy(app);	
			System.out.println(">>> LAUNCHING a new instance of MD") ;
			miniDungeonInstance = app ;
		}
		else {
			// if the config is the same, we just reset the state of the running MD:
			miniDungeonInstance.keyPressedWorker('z');
			System.out.println(">>> RESETING MD") ;
		}
		
		var agent = new TestAgent(agentId, "tester"); 	
		agent.attachState(new MyAgentState())
			 .attachEnvironment(new MyAgentEnv(miniDungeonInstance)) ;
		
		// give initial state update to set it up
		agent.state().updateState(agent.getId()) ;
		return agent ;
	}
	
	// just a simple test to try out
	//@Test
	public void test0() throws Exception {
		
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 40;
		config.randomSeed = 79371;
		System.out.println(">>> Configuration:\n" + config);
		var agent = agentRestart("Frodo",config,false,true)  ; // "Smeagol"	
		
		//if (supressLogging) {
		//	Logging.getAPLIBlogger().setLevel(Level.OFF);
		//}

		var mymodel = MD_model0(200,false) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		//runner.rnd = new Random() ;
		
		// give initial state update, to setup the agent's initial state
		//agent.state().updateState(agent.getId()) ;
		
		var results = runner.generateTestSequence(agent,50) ;
		
		System.out.println(runner.showCoverage()) ;
		System.out.println(">>> failed actions:" + MBTRunner.getFailedActionsFromSeqResult(results)) ;
		System.out.println(">>> postcond violations:" + MBTRunner.getViolatedPostCondsFromSeqResult(results)) ;
	}
	
	
	
	
	// just a simple test to try out
	@Test
	public void test1() throws Exception {
		
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 40;
		config.randomSeed = 79371;
		config.numberOfMaze = 2 ;
		System.out.println(">>> Configuration:\n" + config);
		
		var mymodel = MD_model1(200,true) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		//runner.rnd = new Random() ;
		
		//runner.actionSelectionPolicy = ACTION_SELECTION.Q ;
		
		var results = runner.generate(dummy -> agentRestart("Frodo",config,false,true),20,60) ;
		
		System.out.println(runner.showCoverage()) ;
		System.out.println(">>> failed actions:" + MBTRunner.getFailedActionsFromSuiteResults(results)) ;
		System.out.println(">>> postcond violations:" + MBTRunner.getViolatedPostCondsFromSuiteResults(results)) ;
	}
	
}
