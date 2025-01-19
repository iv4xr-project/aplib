package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.*;

import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.utils.Pair;

public class ExtraGoalLib {
	
	
	/**
	 * Bring the agent to a location next to a non-wall entity.
	 */
	static public GoalStructure nextToEntity(TestAgent agent, String targetId, boolean withSurvival) {
		
		var goallib = new GoalLib() ;
		
		if (withSurvival)
			
			return goallib.smartEntityInCloseRange(agent, targetId) ;
		
		else {
			var G = ((PrimitiveGoal) goallib.entityInCloseRange(targetId)) ;
			G.getGoal().withTactic(
				FIRSTof(goallib.tacticLib.navigateToTac(targetId),
						goallib.tacticLib.explore(null),
						ABORT())
			) ;
			return G ;
		}
	}
	
	/**
	 * Bring the agent to a location next to the given wall. 
	 * The wall should be in the same maze as the agent.
	 */
	static public GoalStructure nextToWall(TestAgent agent, WorldEntity W, boolean withSurvival) {
		
		var S = (MyAgentState) agent.state() ;
		
		var Wmaze = mazeId(W) ;
		var adjacents = Utils.adjacentTiles(S, Utils.toTile(W.position)) ;
		adjacents = adjacents.stream()
					.filter(t -> Utils.isFreeTile(S, t))
					.collect(Collectors.toList()) ;
		
		if (adjacents.isEmpty())
			return null ;
		
		var freeTile = adjacents.get(0) ;
		
		var goallib = new GoalLib() ;
		
		var grabHealPot = action("Push goal grab healpot")
				 .do1((MyAgentState T) -> { 
					 agent.pushGoal(goallib.grabPot(agent, EntityType.HEALPOT));
					 return null ; })
				 .on_(goallib.whenToGoAfterHealPot)
				 .lift() ;
		
		var G = goal("Entity " + W.id + " is touched.") 
				.toSolve((Pair<MyAgentState,WorldModel> proposal) -> {
					WorldModel newObs = proposal.snd ;
					var a = newObs.elements.get(S.worldmodel.agentId) ;
					var agentTile = Utils.toTile(a.position) ;
					var solved =  mazeId(a) == Wmaze && agentTile.equals(freeTile) ;
					return solved; 
				}) ;
		if (withSurvival)
				G.withTactic(
				   FIRSTof(grabHealPot,
						   
						   goallib.tacticLib.useHealingPotAction()
						   	  .on_(goallib.tacticLib.hasHealPot_and_HpLow)
						   	  .lift()
						   ,
						   goallib.tacticLib.useRagePotAction()
						   	  .on_(goallib.tacticLib.hasRagePot_and_inCombat)
						   	  .lift()
						   ,
						   goallib.tacticLib.attackMonsterAction()
						      .on_(goallib.tacticLib.inCombat_and_hpNotCritical)
						      .lift(),
						   goallib.tacticLib.navigateToAction(Wmaze,freeTile.x,freeTile.y).lift(),
						   goallib.tacticLib.explore(null),
						   //Abort().on_(S -> { System.out.println("### about to abort") ; return false;}).lift(), 
				   		   ABORT()) 
				  )
				;
		
		else G.withTactic(FIRSTof(
				goallib.tacticLib.navigateToAction(Wmaze,freeTile.x,freeTile.y).lift(),
				goallib.tacticLib.explore(null),
				ABORT()
				)) ;
		
		return G.lift() ;		
	}
	
	public static int DELAY_BETWEEN_UPDATE = 0 ;


	public static void WAIT() {
		if (DELAY_BETWEEN_UPDATE <= 0) return ;
		try {
			Thread.sleep(DELAY_BETWEEN_UPDATE);
		}
		catch(Exception e) { }
	}
	
	/** 
	 * Execute a goal-structure. Returns the budget that remains.
	 */
	static int executeGoal(TestAgent agent, GoalStructure G, int budget) {
		agent.dropAll() ;
		agent.setGoal(G) ;
		var S = (MyAgentState) agent.state() ;
		while (budget > 0 
				&& S.agentIsAlive() 
				&& ! MDAbstraction.gameover(S)
				&& G.getStatus().inProgress()) {
			agent.update() ;
			WAIT() ;
			budget -- ;
		}
		return budget ;
	}
	
	
	static boolean goToNextMaze(TestAgent agent, int taskBudget, boolean withSurvival) {
		var S = (MyAgentState) agent.state() ;
		int maze = Utils.currentMazeNr(S) ;
		int numOfMazes = S.env().app.dungeon.config.numberOfMaze ;
		if (maze == numOfMazes - 1) return true ;
		
		int numberOfScrolls = S.env().app.dungeon.config.numberOfScrolls ;
		
		int nextMaze = maze + 1 ;
		
		String shrineId = "SM" + maze ;
		var shrine = S.get(shrineId) ;
		
		
		var goallib = new GoalLib() ;
		
		int trial = 0 ;
		
		while (trial < numberOfScrolls 
				&& S.agentIsAlive()
				&& ! MDAbstraction.gameover(S)) {
			boolean cleansed = (Boolean) S.val(shrineId,"cleansed") ;
			if (cleansed) {
				if (distanceToAgent(S,shrine) > 1) {
					var G = nextToEntity(agent,shrineId,withSurvival) ;
					executeGoal(agent,G,taskBudget) ;
				}
				executeGoal(agent,goallib.entityInteracted(shrineId),5) ;
				// should now be in the next maze
				return true ;
			}
			
			GoalStructure H = null ;
			boolean hasScroll = (Integer) S.val("scrollsInBag") > 0 ;
			if (! hasScroll) {
				// get a scroll:
				WorldEntity scroll = null ;
				for (var e : S.worldmodel.elements.values()) {
					if (isScroll(e) && mazeId(e)==maze) {
						scroll = e ;
						break ;
					}
				}
				if (scroll == null) {
					// should not happen!! then the shrine should have been cleansed
					return false ;
				}
				H = SEQ(nextToEntity(agent,scroll.id,withSurvival), 
						goallib.entityInteracted(scroll.id),
						nextToEntity(agent,shrineId,withSurvival),
						goallib.entityInteracted(shrineId)
						) ;	
			}
			else {
				H = SEQ(nextToEntity(agent,shrineId,withSurvival),
						goallib.entityInteracted(shrineId)
						) ;	
			}
			executeGoal(agent,H,taskBudget) ;
			trial++ ;
		}
		return false ;
	}


	
	

}
