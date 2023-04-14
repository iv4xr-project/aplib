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

public class Test_PvP {
	
	MDTestRunner MDTestRunner = new MDTestRunner() ;
	
	@BeforeEach
	public void testConfig() {
		MDTestRunner.withGraphics = false ;
		MDTestRunner.supressLogging = true ;
		MDTestRunner.stopAfterAllAgentsDie = true ;
		MDTestRunner.verbosePrint = false ;
	}
	
	@Test
	public void testPVP1() throws Exception {

		MiniDungeonConfig config = TPJconfigs.MDconfig0();
		
		config = new MiniDungeonConfig() ;		
		String agentId1 = "Frodo";
		var frodo = new TestAgent(agentId1, "tester");
		String agentId2 = "Smeagol";
		var smeagol = new TestAgent(agentId2, "tester");
			
		var pvp1 = new PvPPlayTester() ;
		var pvp2 = new PvPPlayTester() ;
		pvp2.goalLib.tacticLib.delayPathReplan = true ;

		GoalStructure G1 = pvp1.searchAndKill(frodo,smeagol.getId()) ;
		GoalStructure G2 = pvp2.searchAndKill(smeagol,frodo.getId()) ;
		//GoalStructure G2 = pvp2.goalLib.smartEntityInCloseRange(smeagol,frodo.getId()) ;
		
		
		int sleep = 0 ;
		MDTestRunner.runAgent("pvp1",frodo, smeagol, config, G1, G2, 65, sleep) ;
		assertTrue(frodo.evaluateLTLs()) ;
		assertTrue(smeagol.evaluateLTLs()) ;	
		//System.out.println(">>> check: " + states.snd.worldmodel.elements.get("M0_0")) ;
		//(new Scanner(System.in)).nextLine();
	}
	

	@Test
	public void testPVP2() throws Exception {

		MiniDungeonConfig config = TPJconfigs.MDconfig0();
		
		config = new MiniDungeonConfig() ;		
		config.numberOfHealPots = 16 ;
		config.numberOfMonsters = 0 ;
		System.out.println(">>> Configuration:\n" + config);

		String agentId1 = "Frodo";
		var frodo = new TestAgent(agentId1, "tester");
		String agentId2 = "Smeagol";
		var smeagol = new TestAgent(agentId2, "tester");
			
		var pvp1 = new PvPPlayTester() ;
		var random = new RandomPlayTester() ;
		random.reallyRandom = true ;
		
		GoalStructure G1 = pvp1.searchAndKill(frodo,smeagol.getId()) ;
		GoalStructure G2 = random.randomPlay(smeagol) ;
		
		
		int sleep = 0 ;
		MDTestRunner.runAgent("pvp2",frodo,smeagol,config,G1,G2,600, sleep);
		assertTrue(frodo.evaluateLTLs()) ;
		assertTrue(smeagol.evaluateLTLs()) ;	
		//System.out.println(">>> check: " + states.snd.worldmodel.elements.get("M0_0")) ;
		//(new Scanner(System.in)).nextLine();
	}
	
	
}
