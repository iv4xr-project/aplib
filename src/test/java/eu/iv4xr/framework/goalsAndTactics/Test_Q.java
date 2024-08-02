package eu.iv4xr.framework.goalsAndTactics;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


public class Test_Q {
	
	boolean withGraphics = true ;
	boolean supressLogging = false ;
	
	
	int getMazeId(WorldEntity e) {
		var mId = e.properties.get("maze") ;
		if (mId == null)
			return -1 ;
		return (Integer) mId ;
	}
	
	int getFrodoMazeId(TestAgent agent) {
		var st = (Iv4xrAgentState) agent.state() ;
		var frodo = st.worldmodel.elements.get("Frodo") ;
		return (Integer) frodo.properties.get("maze") ;
	}
	
	TestAgent constructAgent() throws Exception {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 2 ;
		config.numberOfRagePots = 2 ;
		// allowing a whole maze to be visible:
		config.viewDistance = 40 ;
		config.numberOfMaze = 3 ;
		config.numberOfScrolls = 3 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 2 ;
		//config.randomSeed = 79371;
		config.randomSeed = 9371;
		config.worldSize = 16 ;
		config.numberOfCorridors = 2 ;
		System.out.println(">>> Configuration:\n" + config);
		
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		//app.soundOn = true;
		
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
	
	
	
	XQalg<MDQstate> constructAlgorithm() throws Exception {
		
		var goalLib = new GoalLib();
		
		var alg = new XQalg<MDQstate>() ;
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
		
		// not setting exploreG --> surpress exploration:
		// 
		//alg.exploredG   = huristicLocation -> { 
		//	var A = alg.getAgent() ; 
		//	return goalLib.smartExploring(A,null,Integer.MAX_VALUE) ; } ;
		
			//alg.reachedG    = e -> goalLib.entityInCloseRange(e) ;
		alg.reachedG    = e -> { 
					var A = alg.getAgent() ; 
					return goalLib.smartEntityInCloseRange(A,e) ;
				} ;
		alg.interactedG = e -> goalLib.entityInteracted(e) ;
		alg.isInteractable   = e ->  e.id.contains("S")  ;
		//alg.isInteractable   = e -> 
		//		e.id.contains("S")  
		//		&& (getMazeId(e) == getFrodoMazeId(alg.agent))
		//		;
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			//var targetShrine = state.worldmodel.elements.get("SM0") ;
			var targetShrine = state.worldmodel.elements.get("SM1") ;
			//var targetShrine = state.worldmodel.elements.get("SM3") ;
			
			return targetShrine != null
					&& (Boolean) targetShrine.properties.get("cleansed") ;
		} ;
		alg.agentIsDead = state -> {
			var frodo = state.worldmodel.elements.get("Frodo") ;
			return frodo != null
					&& ((Integer) frodo.properties.get("hp")) <= 0 ;
		} ;
		alg.stateValueFunction = state -> {
			
			if (alg.topGoalPredicate.test(state)) {
				return alg.maxReward ;
			}
			if (alg.agentIsDead.test(state))
				return -100f ;
			/*
			var numOfScrollsInArea = (int) state.worldmodel.elements.values().stream()
				.filter(e -> e.type.equals("SCROLL"))
				.count();
			
			var scrollsInBag = (Integer) state.worldmodel.elements.get("Frodo")
					.properties.get("scrollsInBag") ;
			
			//return 10f - (float) numOfScrollsInArea - 0.5f * (float) scrollsInBag ;
			*/
			
			// using score does not work well, for deeper dungeons
			/*
			var frodo_score = (Integer) state.worldmodel.elements.get("Frodo")
					.properties.get("score") ;
			
			return (float) frodo_score ;
			*/
			var mazeNr = Utils.currentMazeNr((MyAgentState) state) ;
			
			float numberOfCleansedShrine = (float) state.worldmodel.elements.values()
				.stream()
				.filter(e -> Utils.isShrine(e) && e.getBooleanProperty("cleansed"))
				.count();
			
			return 100f * (mazeNr + numberOfCleansedShrine) ;
		} ;
		
		alg.getQstate = (trace,state) -> new MDQstate(state) ;
		
		alg.wipeoutMemory = agent -> {
			var state = (MyAgentState) agent.state() ;
			state.multiLayerNav.wipeOutMemory(); 
			return null ;
		} ;
		
		
		alg.maxDepth = 18 ;
		alg.maxNumberOfEpisodes = 60 ;
		alg.delayBetweenAgentUpateCycles = 2 ;
		alg.explorationBudget = 4000 ;
		alg.budget_per_task = 2000 ;
		alg.totalSearchBudget = 800000 ;
		alg.exploreProbability = 0.08f ;
		alg.gamma = 0.8f ;
		//alg.enableBackPropagationOfReward = 3 ; 
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
	void testXQalg() throws Exception {
		
		var alg = constructAlgorithm() ;
		
		//alg.runAlgorithmForOneEpisode();
		var R = alg.runAlgorithm(); 
		
		assumeTrue(R.winningplay != null) ;
		
		assumeTrue(R.goalAchieved) ;
		
		assertTrue(alg.terminationCondition()) ;
		assertTrue(alg.topGoalPredicate.test(alg.agentState())) ;
		assertTrue(alg.winningplay.size() > 0) ;
		assertTrue(R.goalAchieved) ;
		assertTrue(R.winningplay.size() > 0) ;
		assertTrue(R.totEpisodes > 0) ;
		
		int num_entries = 0 ;
		for (var q : alg.qtable.values()) {
			num_entries += q.values().size() ;
		}
		
		var bestPlay_ = alg.play(alg.maxDepth) ;
		
			
		var RR = alg.runWinningPlay() ;
		assertTrue (RR.topPredicateSolved) ;
		
		alg.log(">>> #states in qtable: " + alg.qtable.size() + ", #entries in qtable: " + num_entries);
		alg.log(">>> #episodes        : " + R.episodesValues.size()) ;
		alg.log(">>> #used turns      : " + alg.turn) ;	
		alg.log(">>> episode-values: " + R.episodesValues) ;
		
		alg.log(">>> winningplay : " + R.winningplay) ;
		alg.log(">>> best sequence (expected to be bad): " + bestPlay_) ;
	}
	
	/**
	 * Test that a winning play can be extracted from the model learned
	 * by Qalg.
	 */
	@Test
	public void test_model_Q() throws Exception {
		
		var alg = constructAlgorithm() ;
		alg.stopAfterGoalIsAchieved = false ;

		var R = alg.runAlgorithm();
		
		// for this setup, the goal should be solvable
		assumeTrue(R.goalAchieved) ;
		
		assertTrue(alg.terminationCondition()) ;
		assertTrue(alg.winningplay.size() > 0) ;
		assertTrue(R.goalAchieved) ;
		assertTrue(R.winningplay.size() > 0) ;
		assertTrue(R.totEpisodes > 0) ;
		
		// obtain the best play from the moddel ;
		var bestPlay_ = alg.play(alg.maxDepth) ;
		var bestPlay = bestPlay_.fst ;
		var rewardOfBestPlay = bestPlay_.snd ;
		
		//System.out.println(">>> tree fully explored: " + alg.mctree.fullyExplored);
		//System.out.println(alg.mctree) ;
		
		System.out.println(">>> best play according to the model: " + bestPlay_) ;		
		assumeTrue(rewardOfBestPlay >= alg.maxReward) ;
				
		var RR = alg.runTrace(bestPlay) ;
		assertTrue (RR.topPredicateSolved) ;
		
		int num_entries = 0 ;
		for (var q : alg.qtable.values()) {
			num_entries += q.values().size() ;
		}
		alg.log(">>> #states in qtable: " + alg.qtable.size() + ", #entries in qtable: " + num_entries);
		alg.log(">>> #episodes        : " + R.episodesValues.size()) ;
		System.out.println(">>> #episodes  =" + alg.totNumberOfEpisodes) ;
		System.out.println(">>> #used turns=" + alg.turn) ;
		System.out.println(">>> recorded winning play: " + R.winningplay) ;		
		System.out.println(">>> best play according to the model: " + bestPlay_) ;		
		System.out.println(">>> best play according to the model replay result: " + RR) ;	
		
	}

}
