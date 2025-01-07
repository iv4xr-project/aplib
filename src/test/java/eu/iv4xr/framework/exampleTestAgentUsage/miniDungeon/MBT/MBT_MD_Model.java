package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import java.io.Serializable;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.mbt.MBTAction;
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

public class MBT_MD_Model {
	
	public static int DELAY_BETWEEN_UPDATE = 10 ;
	
	public static void WAIT() {
		try {
			Thread.sleep(DELAY_BETWEEN_UPDATE);
		}
		catch(Exception e) { }
	}
	
	public static boolean inCombat(MyAgentState S) {
		return ! S.adajcentMonsters().isEmpty() ;
	}
	
	public static WorldEntity adjacentHealPot(MyAgentState S) {
		var z = TacticLib.nearItems(S,EntityType.HEALPOT,1) ;
		return z == null ? null : z.get(0) ;
	}
	
	public static WorldEntity adjacentRagePot(MyAgentState S) {
		var z = TacticLib.nearItems(S,EntityType.RAGEPOT,1) ;
		return z == null ? null : z.get(0) ;
	}
	
	public static WorldEntity adjacentScroll(MyAgentState S) {
		var z = TacticLib.nearItems(S,EntityType.SCROLL,1) ;
		return z == null ? null : z.get(0) ;
	}
	
	public static WorldEntity adjacentItem(MyAgentState S) {
		WorldEntity e = adjacentScroll(S) ;
		if (e != null) return e ; 
		e = adjacentRagePot(S) ;
		if (e != null) return e ;
		return adjacentHealPot(S) ;
	}
	
	public static WorldEntity adjacentShrine(MyAgentState S) {
		var z = TacticLib.nearItems(S,EntityType.SHRINE,1) ;
		if (z.isEmpty()) return null ;
		return z.get(0) ;
	}
	
	public static WorldEntity adjacentWall(MyAgentState S) {
		var z = TacticLib.nearItems(S,EntityType.WALL,1) ;
		if (z.isEmpty()) return null ;
		return z.get(0) ;
	}
	
	public static int int_(Serializable val) {
		return (Integer) val ;
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
					agent.update() ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> 
					ty == EntityType.HEALPOT ?
						int_(S.val("healpotsInBag")) > 0 
					: 
						int_(S.val("ragepotsInBag")) > 0  ) 
				.addPostConds(new MBTPostCondition<MyAgentState>("" + ty + " used", 
					S -> ty == EntityType.HEALPOT ?
							int_(S.val("healpotsInBag")) == int_(S.before("healpotsInBag")) - 1
						 :
							int_(S.val("ragepotsInBag")) == int_(S.before("ragepotsInBag")) - 1  ))
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
						int_(S.val("ragepotsInBag")) == 0  ) ;
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
					agent.update() ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> {
					if (int_(S.val("bagUsed")) >= int_(S.val("maxBagSize"))) return false ;
					switch(ty) {
					case HEALPOT: return adjacentHealPot(S) != null ;
					case RAGEPOT: return adjacentRagePot(S) != null ;
					case SCROLL: return adjacentScroll(S) != null ;
					}
					return false ;
				})
				.addPostConds(new MBTPostCondition<MyAgentState>("" + ty + " added to bag", 
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
					var e = adjacentItem(S) ;
					new TacticLib() . moveTo(S, Utils.toTile(e.position)) ;
					agent.update() ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> adjacentItem(S) != null && int_(S.val("bagUsed")) >= int_(S.val("maxBagSize")))
				.addPostConds(new MBTPostCondition<MyAgentState>("no item added", 
						S -> int_(S.val("bagUsed")) == int_(S.before("bagUsed")))) ; 
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> travelToObject(EntityType ty, ShrineType sty, 
			int budget,
			boolean withSurvival) {
		return new MBTAction<MyAgentState>("travel-to-" + (sty != null ? sty.toString() : ty.toString()))
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					var Z = S.worldmodel.elements.values().stream()
							   .filter(e -> e.type.equals("" + ty) && Utils.mazeId(e) == Utils.currentMazeNr(S)) ;
					if (sty != null)	   
						Z = Z.filter(e -> ((ShrineType) e.properties.get("shrinetype")) == sty) ;
					WorldEntity e = Z.collect(Collectors.toList()).get(0) ;
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
					agent.update() ;
					// return true, even if it fails to reach the target:
					return true ;
				}) 
				.addGuards(S -> {
				   var Z = S.worldmodel.elements.values().stream()
						   .filter(e -> e.type.equals("" + ty) && Utils.mazeId(e) == Utils.currentMazeNr(S)) ;
				   
				   if (sty != null)
					   
					   Z = Z.filter(e -> ((ShrineType) e.properties.get("shrinetype")) == sty) ;
						   
				   return Z.count() > 0 ;
				})
				// no post-cond for travel
				; 
	}
	
	static MBTState<MyAgentState> inCombat() {
		var Z = new  MBTState<MyAgentState> ("in-combat") ;
		Z.addPredicates(S -> inCombat(S)) ;
		return Z ;
	}
	
	
	
}
