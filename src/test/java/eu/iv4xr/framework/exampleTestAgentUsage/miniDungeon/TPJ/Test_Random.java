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

public class Test_Random {
	
	boolean withGraphics = false ;
	boolean supressLogging = true ;
	boolean stopAfterAgentDie = true ;	
	boolean verbosePrint = false ;
	
	
	@Test
	public void testRandom1() throws Exception {

		MiniDungeonConfig config = TPJconfigs.MDconfig0();
		
		config = new MiniDungeonConfig() ;		
		System.out.println(">>> Configuration:\n" + config);

		String agentId = "Frodo";
		// create an agent:
		var agent = new TestAgent(agentId, "tester");
		
		var randomtester = new RandomPlayTester() ;
		randomtester.reallyRandom = true ;

		GoalStructure G = randomtester.randomPlay(agent) ;
		int sleep = 0 ;
		TPJUtils.runAgent(agent, config, G, 4000, sleep, 
				stopAfterAgentDie, 
				withGraphics, 
				supressLogging,
				verbosePrint);
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine();
	}
	

	@Test
	public void testRandom2() throws Exception {

		MiniDungeonConfig config = TPJconfigs.MDconfig1();
		System.out.println(">>> Configuration:\n" + config);

		String agentId = "Frodo";
		// create an agent:
		var agent = new TestAgent(agentId, "tester");

		var randomtester = new RandomPlayTester() ;
		randomtester.reallyRandom = true ;
		GoalStructure G = randomtester.randomPlay(agent);
		int sleep = 0 ;
		//withGraphics = true ;
		//verbosePrint = true ;
		TPJUtils.runAgent(agent, config, G, 4000, sleep,
				stopAfterAgentDie,
				withGraphics, 
				supressLogging,
				verbosePrint);
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine();
	}
	
	@Test
	public void smarterTestRandom1() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig0() ;
		System.out.println(">>> Configuration:\n" + config);
		
		String agentId = "Frodo" ;		
		// create an agent:
		var agent = new TestAgent(agentId, "tester");
		
		var randomtester = new RandomPlayTester() ;
		GoalStructure G = randomtester.randomPlay(agent);
		int sleep = 0 ;
		TPJUtils.runAgent(agent,config,G,3000,sleep,
				stopAfterAgentDie,
				withGraphics,
				supressLogging,
				verbosePrint) ;		
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine() ;
	}
	
	@Test
	public void smarterTestRandom2() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig1() ;
		System.out.println(">>> Configuration:\n" + config);
		
		String agentId = "Frodo" ;		
		// create an agent:
		var agent = new TestAgent(agentId, "tester");
		
		var randomtester = new RandomPlayTester() ;
		GoalStructure G = randomtester.randomPlay(agent);
		int sleep = 0 ;
		TPJUtils.runAgent(agent,config,G,3000,sleep,
				stopAfterAgentDie,
				withGraphics,
				supressLogging,
				verbosePrint) ;		
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine() ;
	}
	

}
