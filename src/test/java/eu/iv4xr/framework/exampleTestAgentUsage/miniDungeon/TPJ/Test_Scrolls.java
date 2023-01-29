package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.adjacent;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.mazeId;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.toTile;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon ;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils; 

public class Test_Scrolls {
	
	boolean withGraphics = true ;
	boolean supressLogging = true ;
	
	TacticLib tacticLib = new TacticLib() ;
	GoalLib goalLib = new GoalLib() ;
	
	static WorldEntity hasScroll(WorldModel wom) {
		for (var e : wom.elements.values()) {
			if (e.type.equals("" + EntityType.SCROLL)) {
				return e ;
			}
		}
		return null ;
	}
	
	public GoalStructure areaExplored(TestAgent agent) {
		GoalStructure explr = goal("exploring (persistent-goal: aborted when it is terminated)")
		   .toSolve(belief -> false)
		   .withTactic(FIRSTof(
			  tacticLib.useHealingPotAction().on_(tacticLib.hasHealPot_and_HpLow).lift(),
			  tacticLib.useRagePotAction().on_(tacticLib.hasRagePot_and_inCombat).lift(),
			  tacticLib.attackMonsterAction().on_(tacticLib.inCombat_and_hpNotCritical).lift(),
			  SEQ(addBefore(S -> { 
				    System.out.println(">>> deploying grab heal-pot.") ;
				    return goalLib.grabPot(agent, EntityType.HEALPOT) ;} )
				        .on_(goalLib.whenToGoAfterHealPot)
				        .lift(), 
				   ABORT()),
			  tacticLib.explore(null),
			  ABORT()))
		   .lift() ;
		
		// could use WHILE, but let's use REPEAT here:
		return REPEAT(
				FIRSTof(lift((MyAgentState S) -> tacticLib.explorationExhausted(S)) , 
				        SEQ(explr))) ;

	}
	
	public GoalStructure allScrollsTested(TestAgent agent, String shrine) {
		
		GoalStructure testScroll =
			SEQ(DEPLOY(agent,(MyAgentState S) -> {
					  var scroll = hasScroll(S.worldmodel) ;
					  return SEQ(goalLib.smartEntityInCloseRange(agent,scroll.id),
						         goalLib.entityInteracted(scroll.id)) ;
					   }),
				 goalLib.smartEntityInCloseRange(agent,shrine),
				 goalLib.entityInteracted(shrine)) ;
		
		return SEQ(areaExplored(agent),
				   // could use WHILE here, but lets use REPEAT:
				   REPEAT(FIRSTof(
						   lift((MyAgentState S) -> hasScroll(S.worldmodel) == null), 
						   SEQ(testScroll, FAIL()))) 
				   );
	}
	
	
    
	@Test
	public void testScrolls1() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig1() ;
		System.out.println(">>> Configuration:\n" + config);
		
		String agentId = "Frodo" ;		
		var agent = new TestAgent(agentId, "tester");
		GoalStructure G = allScrollsTested(agent,"SM0") ;
		int sleep = 0 ;
		boolean stopAfterAgentDie = true ;
		TPJUtils.runAgent(agent, config, G, 4000, sleep, stopAfterAgentDie, withGraphics, supressLogging);
		assertTrue(G.getStatus().success()) ;
		//(new Scanner(System.in)).nextLine();
	}
	
	@Test
	public void testScrolls2() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig1() ;
		System.out.println(">>> Configuration:\n" + config);
		
		String agentId = "Frodo" ;		
		var agent = new TestAgent(agentId, "tester");
		GoalStructure G = SEQ(
				allScrollsTested(agent,"SM0"),
				goalLib.entityInteracted("SM0"),
				allScrollsTested(agent,"SI1")
				);
		int sleep = 0 ;
		boolean stopAfterAgentDie = true ;
		TPJUtils.runAgent(agent, config, G, 4000, sleep, stopAfterAgentDie, withGraphics, supressLogging);
		assertTrue(G.getStatus().success()) ;
		
		//(new Scanner(System.in)).nextLine();
	}
	
}
