package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils; 

public class Test_Walls {
	
	MDTestAgentRunner MDTestRunner = new MDTestAgentRunner() ;
	
	@BeforeEach
	public void testConfig() {
		MDTestRunner.withGraphics = false ;
		MDTestRunner.supressLogging = true ;
		MDTestRunner.stopAfterAllAgentsDie = true ;
		MDTestRunner.verbosePrint = false ;
		//MDTestRunner.includeWallBug = true ;
	}
	
	
	@Test
	public void testWalls() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig0() ;	
		String agentId = "Frodo" ;		
		var agent = new TestAgent(agentId, "tester");
		
		//GoalStructure G = testWalls(agent) ;
		GoalStructure G =  new WallsTester() . allWallsChecked(agent) ;
		
		/*
		GoalStructure G0 = SEQ(goalLib.smartEntityInCloseRange(agent,"W_0_0_9"),
				goalLib.entityInteracted("W_0_0_9"),
				goalLib.smartEntityInCloseRange(agent,"W_0_0_10"),
				goalLib.entityInteracted("W_0_0_10"));
		*/
		
		int sleep = 1 ;
		MDTestRunner.runAgent("walltest",agent, config, G, 8000, sleep);
		
		//System.out.println("path = " + TacticLib.adjustedFindPath(state, 0,2,9,0,0,9)) ;
		//System.out.println("path = " + TacticLib.adjustedFindPath(state, 0,2,9,0,1,9)) ;

		assertTrue(G.getStatus().success()) ;
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine() ;
	}
	

}
