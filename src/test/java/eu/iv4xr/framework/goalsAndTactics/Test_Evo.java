package eu.iv4xr.framework.goalsAndTactics;

import java.awt.Window;
import java.util.Scanner;
import java.util.logging.Level;

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
 * Test the evolutionary search-algorithm implemented in {@link XEvolutionary}.
 */
public class Test_Evo {
	
	boolean withGraphics = true ;
	boolean supressLogging = false ;
	
	TestAgent constructAgent() throws Exception {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 40;
		config.numberOfMaze = 3 ;
		config.numberOfScrolls = 2 ;
		config.numberOfRagePots = 0 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 3 ;
		config.worldSize = 10 ;
		config.numberOfCorridors = 2 ;
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
	public void testXEvolutionary() throws Exception {
		
		var goalLib = new GoalLib();
		
		var alg = new XEvolutionary() ;
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
		alg.isInteractable   = e -> e.id.contains("S") && ! e.id.startsWith("SS") ;
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			var targetShrine = state.worldmodel.elements.get("SM0") ;
			//var targetShrine = state.worldmodel.elements.get("SS1") ;
			//var targetShrine = state.worldmodel.elements.get("SM1") ;
			return targetShrine != null
					&& (Boolean) targetShrine.properties.get("cleansed") ;
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
				return -100f ;
			var frodo =state.worldmodel.elements.get("Frodo") ;
			int mazeNr = Utils.mazeId(frodo) ;
			var value = (Integer) frodo.properties.get("score") + 1000*mazeNr ;
			return (float) value ;
		} ;
		
		alg.wipeoutMemory = agent -> {
			var state = (MyAgentState) agent.state() ;
			state.multiLayerNav.wipeOutMemory(); 
			return null ;
		} ;
		
		// sensitive to hyper parameters...
		alg.maxDepth = 8 ;
		//alg.maxNumberOfEpisodes = 40 ;
		alg.delayBetweenAgentUpateCycles = 2 ;
		alg.explorationBudget = 4000 ;
		alg.budget_per_task = 2000 ;
		alg.totalSearchBudget = 600000 ;
		alg.maxPopulationSize = 8 ;
		alg.numberOfElitesToKeepDuringSelection = 4 ;
		alg.insertionProbability = 0.5f ;
		alg.onlyExtendWithNewGene = false ;
		
		//alg.extendAtRandomInsertionPoint = false ;
		
				
		//alg.runAlgorithmForOneEpisode();
		var R = alg.runAlgorithm();
		
		// the setup is simple... should be solvable
		assumeTrue(R.goalAchieved) ;
		
		assertTrue(alg.terminationCondition()) ;
		assertTrue(alg.topGoalPredicate.test(alg.agentState())) ;
		assertTrue(alg.winningplay.size() > 0) ;
		assertTrue(R.goalAchieved) ;
		assertTrue(R.winningplay.size() > 0) ;
		assertTrue(R.totEpisodes > 0) ;
		
		var RR = alg.runWinningPlay() ;
		assertTrue(RR.topPredicateSolved);
		
		//alg.log(">>> tree fully explored: " + alg.mctree.fullyExplored);
		
		System.out.println(">>> winningplay: " + R.winningplay) ;
		System.out.println(">>> episodes-values: " + R.episodesValues) ;
		System.out.println(">>> " + RR) ;
		
	}

}
