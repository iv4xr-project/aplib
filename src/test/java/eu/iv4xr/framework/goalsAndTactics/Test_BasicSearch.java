package eu.iv4xr.framework.goalsAndTactics;

import java.util.Scanner;
import java.util.logging.Level;

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


public class Test_BasicSearch {
	
	boolean withGraphics = true ;
	boolean supressLogging = false ;
	
	TestAgent constructAgent() throws Exception {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.numberOfMaze = 3 ;
		config.randomSeed = 79371;
		System.out.println(">>> Configuration:\n" + config);
		
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics ;
		if(withGraphics) DungeonApp.deploy(app);		
		
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
		
		// just a dummy goal to initialize the agent, so that the worldmodel is loaded:
		alg.initializedG = dummy -> {
			GoalStructure dummyG = goal("dummy").toSolve(S -> true)
			  .withTactic(action("dummy").do1(S -> true).lift())
			  .lift() ;
			return dummyG ;
		} ;
		
		
		alg.exploredG = huristicLocation -> goalLib.exploring(null,Integer.MAX_VALUE) ;
		alg.reachedG = e -> goalLib.entityInCloseRange(e.id) ;
		alg.interactedG = e -> goalLib.entityInteracted(e.id) ;
		alg.isInteractable = e -> e.id.contains("S") ;
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			var shrine0 = state.worldmodel.elements.get("SM0") ;
			return shrine0 != null
					&& (Boolean) shrine0.properties.get("cleansed") ;
		} ;
		alg.maxDepth = 3 ;
		alg.maxNumberOfRuns = 10 ;
		alg.delayBetweenAgentUpateCycles = 10 ;

		
		//alg.runAlgorithmForOneEpisode();
		alg.runAlgorithm();
		

		
		//System.out.println(">>>> hit RET") ;
		//Scanner scanner = new Scanner(System.in);
		//scanner.nextLine() ;
		
	}

}
