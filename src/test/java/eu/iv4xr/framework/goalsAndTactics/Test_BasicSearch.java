package eu.iv4xr.framework.goalsAndTactics;

import java.awt.Window;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.mainConcepts.GoalStructure; 

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test the basic search algorithm implemented in {@link BasicSearch}.
 */
public class Test_BasicSearch {
	
	boolean withGraphics = true ;
	boolean supressLogging = false ;
	
	TestAgent constructAgent() throws Exception {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.numberOfMaze = 3 ;
		//config.numberOfMonsters = 30 ;
		config.worldSize = 16 ;
		config.numberOfCorridors = 2 ;
		config.numberOfScrolls = 2 ;
		config.numberOfMonsters = 2 ;
		config.randomSeed = 79371;
		System.out.println(">>> Configuration:\n" + config);
		
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics ;
		if(withGraphics) DungeonApp.deploy(app);	
		System.out.println(">>> LAUNCHING MD") ;
		
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState();
		
		var agent = new TestAgent("Frodo", "tester");
		agent.attachState(state).attachEnvironment(env) ;
		
		var G = goal("dummy").toSolve(S -> true)
				.withTactic(action("dummy").do1(S -> true).lift())
				.lift() ;
		//agent.setGoal(G) ;
		
		// should be after create the agent, else the constructor sets the visibility again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}
		
		Thread.sleep(1000);
		
		// add a single update:
		//agent.update();
		
		//System.out.println(">>> WOM: " + state.worldmodel) ;
			
		return agent ;
	}
	
	@Test
	/**
	 * A simple scenario to test that BasicSearch works.
	 */
	public void testBasicSearch() throws Exception {
		
		var goalLib = new GoalLib();
		
		var alg = new BasicSearch() ;
		BasicSearch.DEBUG = !supressLogging ;

		
		alg.agentConstructor = dummy -> {
			try {
				return constructAgent() ;
			}
			catch(Exception e) {
				System.out.println(">>> FAIL to create a agent.") ;
				return null ;
			}
		} ;
		
		alg.closeEnv = dummy -> {
			var env = (MyAgentEnv) alg.agent.env() ;
			var win = SwingUtilities.getWindowAncestor(env.app);
			win.dispose();
			System.out.println(">>> DISPOSING MD") ;
			return null ;
		} ;
		
		// just a dummy goal to initialize the agent, so that the worldmodel is loaded:
		alg.initializedG = dummy -> {
			GoalStructure dummyG = goal("dummy").toSolve(S -> true)
			  .withTactic(action("dummy").do1(S -> true).lift())
			  .lift() ;
			return dummyG ;
		} ;
		
		
		alg.exploredG   = huristicLocation -> goalLib.exploring(null,Integer.MAX_VALUE) ;
		alg.reachedG    = e -> goalLib.entityInCloseRange(e) ;
		alg.interactedG = e -> goalLib.entityInteracted(e) ;
		alg.isInteractable   = e -> e.id.contains("S") ;
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			var shrine0 = state.worldmodel.elements.get("SM0") ;
			return shrine0 != null
					&& (Boolean) shrine0.properties.get("cleansed") ;
		} ;
		alg.agentIsDead = state -> {
			var frodo = state.worldmodel.elements.get("Frodo") ;
			return frodo != null
					&& ((Integer) frodo.properties.get("hp")) <= 0 ;
		} ;
		
		alg.maxDepth = 3 ;
		alg.maxNumberOfEpisodes = 10 ;
		alg.delayBetweenAgentUpateCycles = 10 ;
			
		//alg.runAlgorithmForOneEpisode();
		var R = alg.runAlgorithm();
		
		// the setup is simple, should be solvable:
		assumeTrue(R.goalAchieved) ;
		assertTrue(alg.terminationCondition()) ;
		assertTrue(alg.topGoalPredicate.test(alg.agentState())) ;
		assertTrue(alg.winningplay.size() > 0) ;
		assertTrue(R.goalAchieved) ;
		assertTrue(R.winningplay.size() > 0) ;
		assertTrue(R.totEpisodes > 0) ;
		
		// rerun the found winning-play:	
		var RR = alg.runWinningPlay() ;
		assertTrue(RR.topPredicateSolved);
		
		System.out.println(">>> winningplay: " + R.winningplay) ;
		System.out.println(">>> " + RR) ;
		
	}

}
