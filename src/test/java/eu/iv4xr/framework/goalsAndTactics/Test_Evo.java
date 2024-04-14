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


public class Test_Evo {
	
	boolean withGraphics = true ;
	boolean supressLogging = false ;
	
	TestAgent constructAgent() throws Exception {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.numberOfMaze = 3 ;
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
	
	
	@Test
	public void test0() throws Exception {
		
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
		
		
		alg.exploredG   = huristicLocation -> goalLib.exploring(null,Integer.MAX_VALUE) ;
		alg.reachedG    = e -> goalLib.entityInCloseRange(e) ;
		alg.interactedG = e -> goalLib.entityInteracted(e) ;
		alg.isInteractable   = e -> e.id.contains("S") ;
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			//var targetShrine = state.worldmodel.elements.get("SM0") ;
			var targetShrine = state.worldmodel.elements.get("SM1") ;
			return targetShrine != null
					&& (Boolean) targetShrine.properties.get("cleansed") ;
		} ;
		alg.agentIsDead = state -> {
			var frodo = state.worldmodel.elements.get("Frodo") ;
			return frodo != null
					&& ((Integer) frodo.properties.get("hp")) <= 0 ;
		} ;
		alg.rewardFunction = state -> {
			if (alg.topGoalPredicate.test(state))
				return alg.maxReward ;
			if (alg.agentIsDead.test(state))
				return -100f ;
			var frodo_score = (Integer) state.worldmodel.elements.get("Frodo")
					.properties.get("score") ;
			return (float) frodo_score ;
		} ;
		
		alg.wipeoutMemory = agent -> {
			var state = (MyAgentState) agent.state() ;
			state.multiLayerNav.wipeOutMemory(); 
			return null ;
		} ;
		
		
		alg.maxDepth = 9 ;
		//alg.maxNumberOfEpisodes = 40 ;
		alg.delayBetweenAgentUpateCycles = 10 ;
		alg.explorationBudget = 700 ;
		alg.budget_per_task = 700 ;
		alg.totalSearchBudget = 300000 ;
		
				
		//alg.runAlgorithmForOneEpisode();
		var R = alg.runAlgorithm();
		
		//alg.log(">>> tree fully explored: " + alg.mctree.fullyExplored);
		
		System.out.println(">>> winningplay: " + R.winningplay) ;

		
		//System.out.println(">>>> hit RET") ;
		//Scanner scanner = new Scanner(System.in);
		//scanner.nextLine() ;
		
	}

}
