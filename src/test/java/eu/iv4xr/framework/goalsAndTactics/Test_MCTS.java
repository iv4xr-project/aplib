package eu.iv4xr.framework.goalsAndTactics;

import java.awt.Window;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * Test the Monte Carlo Tree Search algorithm (MCST) as implemented 
 * in {@link XMCTS}.
 */
public class Test_MCTS {
	
	boolean withGraphics = true ;
	boolean supressLogging = false ;
	
	TestAgent constructAgent() throws Exception {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 40 ;
		config.worldSize = 12 ;
		config.numberOfMaze = 3 ;
		config.numberOfScrolls = 2 ;
		config.numberOfCorridors = 3 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 1 ;
		//config.numberOfMonsters = 30 ;
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
	
	// construct and configure an instance of MCT
	XMCTS contructAlgorithm() {
		var goalLib = new GoalLib();
		
		var alg = new XMCTS() ;
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
				
		//alg.exploredG   = huristicLocation -> goalLib.exploring(null,Integer.MAX_VALUE) ;
		alg.reachedG    = e -> goalLib.entityInCloseRange(e) ;
		alg.interactedG = e -> goalLib.entityInteracted(e) ;
		alg.isInteractable   = e -> e.id.contains("S") && ! e.id.startsWith("SS");
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			//var targetShrine = state.worldmodel.elements.get("SM0") ;
			var targetShrine = state.worldmodel.elements.get("SM1") ;
			var ok = targetShrine != null
				 	&& (Boolean) targetShrine.properties.get("cleansed") ;
			return ok ;	
		} ;
		alg.agentIsDead = state -> {
			var frodo = state.worldmodel.elements.get("Frodo") ;
			return frodo != null
					&& ((Integer) frodo.properties.get("hp")) <= 0 ;
		} ;
		alg.stateValueFunction = state -> {
			if (alg.topGoalPredicate.test(state))
				return alg.maxReward ;
			if (alg.agentIsDead.test(state))
				return -10f ;
			
			//var frodo_score = (Integer) state.worldmodel.elements.get("Frodo")
			//		.properties.get("score") ;
			
			var cleansed = (int) state.worldmodel.elements.values().stream()
					.filter(e -> Utils.isMoonShrine(e) && e.getBooleanProperty("cleansed")) 
					.count();
			var mazeNr = Utils.currentMazeNr((MyAgentState) state) ;
			return (float) 100*(cleansed + mazeNr) ;
		} ;
		alg.wipeoutMemory = agent -> {
			var state = (MyAgentState) agent.state() ;
			state.multiLayerNav.wipeOutMemory(); 
			return null ;
		} ;
		
		
		alg.maxDepth = 10 ;
		alg.maxNumberOfEpisodes = 60 ;
		alg.delayBetweenAgentUpateCycles = 2 ;
		alg.explorationBudget = 4000 ;
		alg.budget_per_task = 2000 ;
		alg.totalSearchBudget = 2400000 ;
		alg.stopAfterGoalIsAchieved = true ;
		
		return alg ;
	}
	
	/**
	 * Test that the algorithm can work to find a solution, and that
	 * the solution can be replayed. In this test, the search is stopped
	 * as soon as the goal state is reached. The trace to this state
	 * is then extracted, and we check if replaying it loads to the
	 * goal state.
	 */
	@Test
	public void testMCTS() throws Exception {
		
		var alg = contructAlgorithm() ;
		
		var R = alg.runAlgorithm();
		
		// for this setup, the goal should be solvable
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
		
		//System.out.println(">>> tree fully explored: " + alg.mctree.fullyExplored);
		//System.out.println(alg.mctree) ;
		
		//System.out.println(">>> best play according to the model: " + alg.obtainBestPlay()) ;		
	}
	
	
	/**
	 * Test that a winning play can be extracted from the model learned
	 * by MCTS.
	 */
	@Test
	public void test_model_MCTS() throws Exception {
		
		var alg = contructAlgorithm() ;
		alg.stopAfterGoalIsAchieved = false ;

		var R = alg.runAlgorithm();
		
		// for this setup, the goal should be solvable
		assumeTrue(R.goalAchieved) ;
		
		assertTrue(alg.terminationCondition()) ;
		assertTrue(alg.winningplay.size() > 0) ;
		assertTrue(R.goalAchieved) ;
		assertTrue(R.winningplay.size() > 0) ;
		assertTrue(R.totEpisodes > 0) ;
		
		System.out.println(">>> winningplay: " + R.winningplay) ;
		
		//System.out.println(">>> tree fully explored: " + alg.mctree.fullyExplored);
		//System.out.println(alg.mctree) ;
		
		// obtain the best play from the moddel ;
		var bestPlay_ = alg.obtainBestPlay() ;
		var bestPlay = bestPlay_.fst ;
		var rewardOfBestPlay = bestPlay_.snd ;
		
		System.out.println(">>> best play according to the model: " + bestPlay_) ;		
		assumeTrue(rewardOfBestPlay >= alg.maxReward) ;
				
		var RR = alg.runTrace(bestPlay) ;
		assertTrue (RR.topPredicateSolved) ;
		
		System.out.println(">>> #episodes  =" + alg.totNumberOfEpisodes) ;
		System.out.println(">>> #used turns=" + alg.turn) ;
		System.out.println(">>> best play according to the model: " + bestPlay_) ;		
		System.out.println(">>> best play according to the model replay result: " + RR) ;	
		
	}

}
