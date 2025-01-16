package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import java.util.*;
import java.util.Random;

import eu.iv4xr.framework.extensions.mbt.MBTAction;
import eu.iv4xr.framework.extensions.mbt.MBTModel;
import eu.iv4xr.framework.extensions.mbt.MBTPostCondition;
import eu.iv4xr.framework.extensions.mbt.MBTState;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import static eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT.MDAbstraction.* ;

public class MBT_MD_Model {
	
	public static Random rndx = new Random(373) ;
	
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
					ExtraGoalLib.WAIT() ;
					return true ;
				})
				
				.addGuards(S ->  S.agentIsAlive() && ! MDAbstraction.gameover(S) && itemInBag(S,ty) > 0) 
				
				.addPostConds(new MBTPostCondition<MyAgentState>("" + ty + " used", 
					S -> itemInBag(S,ty) == oldItemInBag(S,ty) - 1
				         && (ty == EntityType.HEALPOT ?
				                 (hp(S) > oldHp(S) || oldHp(S) >= hpmax(S) - 4) 
							     :
							     rageTimer(S) == 9 
						    )))
				; 
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> usepot_neg(EntityType ty) {
		var A = usepot(ty) ;
		A.name = "use-but-dont-have-" + ty ;
		A.guards.clear();
		A.postConditions.clear();
		A.addGuards(S -> itemInBag(S,ty) == 0) ;
		A.addPostConds(new MBTPostCondition<MyAgentState>("no item used",  S -> bagUsed(S) == oldBagUsed(S))) ;
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
					ExtraGoalLib.WAIT() ;
					return true ;
				}) 
				.addGuards(S -> 
					S.agentIsAlive() && ! MDAbstraction.gameover(S) && bagUsed(S) < maxBagSize(S) && adjacentNonWallEntity(S,ty) != null )
				
				.addPostConds(
					new MBTPostCondition<MyAgentState>("bag-cap-respected", S -> bagUsed(S) <= maxBagSize(S)),
					new MBTPostCondition<MyAgentState>("" + ty + " added to bag", S -> itemInBag(S,ty) == oldItemInBag(S,ty) + 1 )) ; 
	}
	
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> pickUpItem_neg() {
		return new MBTAction<MyAgentState>("pickup-item-but-full")
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var e = adjacentPickableItem(S) ;
					new TacticLib() . moveTo(S, Utils.toTile(e.position)) ;
					S.updateState(agent.getId()) ;
					ExtraGoalLib.WAIT() ;
					return true ;
				}) 
				.addGuards(S -> adjacentPickableItem(S) != null && bagUsed(S) >= maxBagSize(S))
				.addPostConds(new MBTPostCondition<MyAgentState>("no item added", S -> bagUsed(S) == oldBagUsed(S))) ; 
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
					ExtraGoalLib.WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() && ! MDAbstraction.gameover(S) && MDAbstraction.inCombat(S))
				.addPostConds(
					new MBTPostCondition<MyAgentState>("hp-decrease", S -> justKilledAMonster(S) || hp(S) < oldHp(S)),
						
					new MBTPostCondition<MyAgentState>("monster-killed-or-hp-decrease", 
						S -> justKilledAMonster(S) || hpAdjacentMonsters(S)  < oldHpAdjacentMonsters(S))) 
				 ; 
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> bumpWall(int budget,boolean withSurvival) {
		return new MBTAction<MyAgentState>("bump-wall")
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var walls = touchableWalls(S) ;
					var W = walls.get(rndx.nextInt(walls.size())) ;
					
					ExtraGoalLib.executeGoal(agent, ExtraGoalLib.nextToWall(agent,W,withSurvival), budget) ;
					
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
					ExtraGoalLib.WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() && ! MDAbstraction.gameover(S) && ! isAdjacentToNonWallObject(S))
				.addPostConds(
						new MBTPostCondition<MyAgentState>("remain-in-same-position",
						S -> S.worldmodel.position.equals(S.positionBefore()))
				 ); 
	}
	
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> tryToCleanseShrine(int budget,boolean withSurvival) {
		
		var goallib = new GoalLib() ;
		
		return new MBTAction<MyAgentState>("cleanse-shrine")
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var shrine = closedShrineInSameMaze(S) ;
					GoalStructure G = SEQ(
						ExtraGoalLib.nextToEntity(agent, shrine.id, withSurvival),
						goallib.entityInteracted(shrine.id)) ;
					ExtraGoalLib.executeGoal(agent,G,budget) ;
					S.updateState(agent.getId()) ;
					ExtraGoalLib.WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() && ! MDAbstraction.gameover(S) && closedShrineInSameMaze(S) != null)
				.addPostConds(
						new MBTPostCondition<MyAgentState>("shrine could be cleansed",
						S -> closedShrineInSameMaze(S) != null 
						     || itemInBag(S,EntityType.SCROLL) == oldItemInBag(S,EntityType.SCROLL) - 1  )
				 ); 
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> teleport(ShrineType sty, int budget,boolean withSurvival) {
		
		var goallib = new GoalLib() ;
		
		return new MBTAction<MyAgentState>("teleport-" + sty)
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var shrine = entitiesInSameMaze(S, EntityType.SHRINE,sty).get(0) ;
					GoalStructure G = SEQ(
						ExtraGoalLib.nextToEntity(agent, shrine.id, withSurvival),
						goallib.entityInteracted(shrine.id)) ;
					ExtraGoalLib.executeGoal(agent,G,budget) ;		
					S.updateState(agent.getId()) ;
					ExtraGoalLib.WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() && ! MDAbstraction.gameover(S)
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
						     || maze(S) == oldMaze(S) + (sty == ShrineType.MoonShrine ? 1 : -1) ; 
							}
						)
						    
				 ); 
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> smartGoToNextMaze(int budget,boolean withSurvival) {
		
		return new MBTAction<MyAgentState>("smart-goto-next-maze")
				.withAction(agent -> {
					ExtraGoalLib.goToNextMaze(agent, budget, withSurvival) ;
					var S = (MyAgentState) agent.state() ;
					S.updateState(agent.getId()) ;
					ExtraGoalLib.WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive() && ! MDAbstraction.gameover(S) 
							    && maze(S) < S.env().app.dungeon.config.numberOfMaze - 1)
				;  
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
					GoalStructure G = ExtraGoalLib.nextToEntity(agent, e.id, withSurvival) ;
					ExtraGoalLib.executeGoal(agent,G,budget) ;				
					S.updateState(agent.getId()) ;
					ExtraGoalLib.WAIT() ;
					// return true, even if it fails to reach the target:
					return true ;
				}) 
				.addGuards(S -> {
				   if (! S.agentIsAlive() || MDAbstraction.gameover(S)) return false ;
			
				   // not enabled if the agent is already next to an entity ty
				   if (adjacentNonWallEntity(S,ty) != null) return false ;
				   	   
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
		Z.addPredicates(S -> MDAbstraction.gameover(S)) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> playerwin() {
		var Z = new  MBTState<MyAgentState> ("playerwin") ;
		Z.addPredicates(S -> MDAbstraction.playerWin(S)) ;
		return Z ;
	}
	
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> inCombat() {
		var Z = new  MBTState<MyAgentState> ("in-combat") ;
		Z.addPredicates(S -> MDAbstraction.inCombat(S)) ;
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
		Z.addPredicates(S -> itemInBag(S,ty) > 0) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> bagfull() {
		var Z = new  MBTState<MyAgentState> ("bag-full") ;
		Z.addPredicates(S -> bagUsed(S) >= maxBagSize(S)) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> enraged() {
		var Z = new  MBTState<MyAgentState> ("enraged") ;
		Z.addPredicates(S -> rageTimer(S) > 0 ) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> healed() {
		var Z = new  MBTState<MyAgentState> ("healed") ;
		Z.addPredicates(S -> S.before("hp") != null && hp(S) > oldHp(S)) ;
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
			return S.before(sh.id,"cleansed") != null && (cleansed(S,sh.id) != oldCleansed(S,sh.id)) ;
		}) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> atLastMaze() {
		var Z = new  MBTState<MyAgentState> ("at-last-maze") ;
		Z.addPredicates(S -> maze(S) == S.env().app.dungeon.config.numberOfMaze - 1) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTModel<MyAgentState> MD_model0(int travelBudget, boolean withSurvival) {
		var model = new MBTModel<MyAgentState>("MD-model0") ;
		model.addStates(
				alive(),
				gameover(),
				playerwin(),
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
				enraged(),
				atLastMaze()
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
	static MBTModel<MyAgentState> MD_model1(int travelBudget, 
			boolean useSmartGoToNextMaze,
			boolean withSurvival) {
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
		
		if (useSmartGoToNextMaze) {
			model.name = "MD-model1B" ;
			model.addActions(smartGoToNextMaze(travelBudget,withSurvival)) ;
		}
		
		return model ;
		
	}
	
}
