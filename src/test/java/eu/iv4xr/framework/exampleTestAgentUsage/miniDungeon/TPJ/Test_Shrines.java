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

import org.junit.jupiter.api.BeforeEach;
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

class Test_Shrines {
	
	MDTestRunner MDTestRunner = new MDTestRunner() ;
	
	@BeforeEach
	void testConfig() {
		MDTestRunner.withGraphics = false ;
		MDTestRunner.supressLogging = true ;
		MDTestRunner.stopAfterAllAgentsDie = true ;
		MDTestRunner.verbosePrint = false ;
	}
	
	@Test
	void testScrolls1() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig1() ;
		String agentId = "Frodo" ;		
		var agent = new TestAgent(agentId, "tester");
		var shrineCleanPlay = new ShrineCleanTester() ;
		GoalStructure G = shrineCleanPlay.cleanseShrine(agent,"SM0") ;
		int sleep = 0 ;
		//withGraphics = true ;
		MDTestRunner.runAgent("cleansingSM0",agent, config, G, 4000, sleep);
		assertTrue(G.getStatus().success()) ;
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine();
	}
	
	@Test
	void testScrolls2() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig1() ;
		String agentId = "Frodo" ;		
		var agent = new TestAgent(agentId, "tester");
		var shrineCleanPlay = new ShrineCleanTester() ;
		GoalStructure G = shrineCleanPlay.cleanseAllShrines(agent,2) ;
		int sleep = 0 ;
		MDTestRunner.runAgent("cleansingSI1",agent, config, G, 4000, sleep);
		assertTrue(G.getStatus().success()) ;
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine();
	}
	
	@Test
	void testScrolls2b() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig2() ;
		String agentId = "Smeagol" ;		
		var agent = new TestAgent(agentId, "tester");
		var shrineCleanPlay = new ShrineCleanTester() ;
		GoalStructure G = shrineCleanPlay.cleanseShrine(agent,"SM0") ;
		int sleep = 0 ;
		MDTestRunner.runAgent("SmeagolCleansingSM0",agent, config, G, 4000, sleep);
		assertTrue(G.getStatus().success()) ;
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine();
	}
	
}
