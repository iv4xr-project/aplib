package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import java.io.Serializable;
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
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;

public class MBT_MD_Model {
	
	public static int DELAY_BETWEEN_UPDATE = 100 ;
	
	public static void WAIT() {
		try {
			Thread.sleep(DELAY_BETWEEN_UPDATE);
		}
		catch(Exception e) { }
	}
	
	public static boolean inCombat(MyAgentState S) {
		return ! S.adajcentMonsters().isEmpty() ;
	}
	
	public static WorldEntity adjacentEntity(MyAgentState S, EntityType ty) {
		var z = TacticLib.nearItems(S,ty,1) ;
		return z.isEmpty() ? null : z.get(0) ;
	}
	
	public static WorldEntity adjacentShrine(MyAgentState S, ShrineType sty) {
		var e = adjacentEntity(S,EntityType.SHRINE) ;
		if (e == null) 
			return null ;
		return (ShrineType) e.properties.get("shrinetype") == sty ? e : null ;
	}
	

	public static WorldEntity adjacentItem(MyAgentState S) {
		WorldEntity e = adjacentEntity(S, EntityType.SCROLL) ;
		if (e != null) return e ; 
		e = adjacentEntity(S, EntityType.RAGEPOT) ;
		if (e != null) return e ;
		return adjacentEntity(S, EntityType.HEALPOT) ;
	}
	
	
	public static int int_(Serializable val) {
		return (Integer) val ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTAction<MyAgentState> doNothing() {
		return new MBTAction<MyAgentState>("do-nothing")
				.withAction(agent -> {
					var S = (MyAgentState) agent.state() ;
					S.env().action(S.worldmodel.agentId, Command.DONOTHING) ;
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> S.agentIsAlive())
				.addPostConds(new MBTPostCondition<MyAgentState>("state-the-same",
					S -> 
					S.val("hp").equals(S.before("hp"))
					&& S.val("score").equals(S.before("score"))
					&& S.val("itemsInBag").equals(S.before("itemsInBag"))
					&& S.val("rageTimer").equals(S.before("rageTimer")))
					)
				;
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
					ty == EntityType.HEALPOT ?
						int_(S.val("healpotsInBag")) > 0 
					: 
						int_(S.val("ragepotsInBag")) > 0  ) 
				.addPostConds(new MBTPostCondition<MyAgentState>("" + ty + " used", 
					S -> ty == EntityType.HEALPOT ?
							int_(S.val("healpotsInBag")) == int_(S.before("healpotsInBag")) - 1
							&& (int_(S.val("hp")) > int_(S.before("hp"))
							    || int_(S.before("hp")) == int_(S.val("hpmax")))
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
					S.updateState(agent.getId()) ;
					WAIT() ;
					return true ;
				}) 
				.addGuards(S -> 
					S.agentIsAlive() &&
					int_(S.val("bagUsed")) < int_(S.val("maxBagSize")) && adjacentEntity(S,ty) != null )
				
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
					var e = adjacentItem(S) ;
					new TacticLib() . moveTo(S, Utils.toTile(e.position)) ;
					S.updateState(agent.getId()) ;
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
					S.updateState(agent.getId()) ;
					// return true, even if it fails to reach the target:
					return true ;
				}) 
				.addGuards(S -> {
				   var Z = S.worldmodel.elements.values().stream()
						   .filter(e -> e.type.equals("" + ty) && Utils.mazeId(e) == Utils.currentMazeNr(S)) ;
				   
				   if (sty != null)
					   
					   Z = Z.filter(e -> ((ShrineType) e.properties.get("shrinetype")) == sty) ;
						   
				   return S.agentIsAlive() && Z.count() > 0 ;
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
	static MBTState<MyAgentState> inCombat() {
		var Z = new  MBTState<MyAgentState> ("in-combat") ;
		Z.addPredicates(S -> inCombat(S)) ;
		return Z ;
	}
	
	@SuppressWarnings("unchecked")
	static MBTState<MyAgentState> adjacentTo(EntityType ty) {
		var Z = new  MBTState<MyAgentState> ("adjacent-to-" + ty) ;
		Z.addPredicates(S -> adjacentEntity(S,ty) != null) ;
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
	static MBTModel<MyAgentState> MD_model0(int travelBudget, boolean withSurvival) {
		var model = new MBTModel<MyAgentState>("MD-model0") ;
		model.addStates(
				alive(),
				inCombat(),
				adjacentTo(EntityType.HEALPOT),
				adjacentTo(EntityType.RAGEPOT),
				adjacentTo(EntityType.SCROLL),
				inBag(EntityType.HEALPOT),
				inBag(EntityType.RAGEPOT),
				inBag(EntityType.SCROLL),
				bagfull(),
				healed(), 
				enraged()
				) ;
		
		model.addActions(
				travelToObject(EntityType.RAGEPOT,null,travelBudget,withSurvival),
				pickUpItem(EntityType.RAGEPOT),
				usepot(EntityType.RAGEPOT)
				) ;
		
		return model  ;
	}
	
	
	// just a simple test to try out
	@Test
	public void test0() throws Exception {
		
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 40;
		config.randomSeed = 79371;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		// graphics on/off:
		app.headless = false ;
		if(! app.headless) DungeonApp.deploy(app);
	
		var agent = new TestAgent("Frodo", "tester"); // "Smeagol"		
		agent.attachState( new MyAgentState())
			 .attachEnvironment( new MyAgentEnv(app)) ;
		
		//if (supressLogging) {
		//	Logging.getAPLIBlogger().setLevel(Level.OFF);
		//}

		
		var mymodel = MD_model0(200,false) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		//runner.rnd = new Random() ;
		
		// give initial state update, to setup the agent's initial state
		agent.state().updateState(agent.getId()) ;
		
		var results = runner.generateTestSequence(agent,50) ;
		
		System.out.println(runner.showCoverage()) ;
		System.out.println(">>> failed actions:" + MBTRunner.getFailedActionsFromSeqResult(results)) ;
		System.out.println(">>> postcond violations:" + MBTRunner.getViolatedPostCondsFromSeqResult(results)) ;
	}
	
	
}
