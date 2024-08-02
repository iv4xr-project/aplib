package eu.iv4xr.framework.goalsAndTactics;

import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.logging.Level;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;

public class Test_ActionLevelQ {
	
	boolean withGraphics = true ;
	boolean supressLogging = false ;
		
	TestAgent constructAgent() throws Exception {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 1 ;
		config.worldSize = 10 ;
		config.numberOfCorridors = 2 ;
		config.viewDistance = 40 ;
		config.numberOfMaze = 2 ;
		config.numberOfScrolls = 1 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 1 ;
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
		
		//var G = goal("dummy").toSolve(S -> true)
		//		.withTactic(action("dummy").do1(S -> true).lift())
		//		.lift() ;
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
	
	@SuppressWarnings("incomplete-switch")
	Action move(Command cmd) {
		var A = action("" + cmd)
				.do1((MyAgentState S) ->  {
					S.env().action(S.worldmodel.agentId, cmd) ;
					return S ;
				})
				.on_((MyAgentState S) -> {
					  var tile = Utils.toTile(S.worldmodel.position) ;
					  Tile tile2 = null ;
					  switch(cmd) {
					    case MOVEUP   : tile2 = new Tile(tile.x,tile.y+1) ; break ;
					    case MOVEDOWN : tile2 = new Tile(tile.x,tile.y-1) ; break ;
					    case MOVERIGHT: tile2 = new Tile(tile.x+1,tile.y) ; break ;
					    case MOVELEFT : tile2 = new Tile(tile.x-1,tile.y) ; break ;
					  }
					  return ! Utils.isWall(S,tile2) ;
					}) ; 
		return A ;
	}
	
	@SuppressWarnings("incomplete-switch")
	Action usePotion(Command cmd) {
		var A = action("" + cmd)
				.do1((MyAgentState S) ->  {
					S.env().action(S.worldmodel.agentId, cmd) ;
					return S ;
				})
				.on_((MyAgentState S) -> {	
					  var frodo = S.worldmodel.elements.get("Frodo") ;
					  switch(cmd) {
					    case USEHEAL :
					    	return (Integer) frodo.properties.get("healpotsInBag") > 0 ;
					    case USERAGE :
					    	return (Integer) frodo.properties.get("ragepotsInBag") > 0 ;
					  }
					  return false ;
					}) ; 
		return A ;
	}
	
	
	AQalg<MDQstate2> constructAlgorithm() throws Exception {
		
		var alg = new AQalg<MDQstate2>() ;
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
		
		alg.availableActions.put("w", move(Command.MOVEUP)) ;
		alg.availableActions.put("a", move(Command.MOVELEFT)) ;
		alg.availableActions.put("s", move(Command.MOVEDOWN)) ;
		alg.availableActions.put("d", move(Command.MOVERIGHT)) ;
		alg.availableActions.put("e", usePotion(Command.USEHEAL)) ;
		alg.availableActions.put("r", usePotion(Command.USERAGE)) ;
	
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			var targetShrine = state.worldmodel.elements.get("SM0") ;
			//var targetShrine = state.worldmodel.elements.get("SM1") ;
			return targetShrine != null
					&& (Boolean) targetShrine.properties.get("cleansed") ;
		} ;
		
		alg.agentIsDead = state -> {
			var frodo = state.worldmodel.elements.get("Frodo") ;
			return frodo != null
					&& ((Integer) frodo.properties.get("hp")) <= 0 ;
		} ;
		
		// attach a function that converts the agent state to a Q-state:
		alg.getQstate = (trace,currentState) -> {
			var env = (MyAgentEnv) alg.agent.env() ;
			return new MDQstate2(env.app.dungeon.config, 7, currentState) ;
		} ;
		
		// defining state-value function, rather than action-reward. The direct
		// reward of an action is then implicitly defined as the value of the
		// next state minus the value of the previous state.
		alg.stateValueFunction = state -> {
			if (alg.topGoalPredicate.test(state))
				return alg.maxReward ;
			if (alg.agentIsDead.test(state))
				return -100f ;
			
			//var numOfScrollsInArea = (int) state.worldmodel.elements.values().stream()
			//	.filter(e -> e.type.equals("SCROLL"))
			//	.count();
			
			var frodo = state.worldmodel.elements.get("Frodo") ;
			
			var scrollsInBag = (Integer) frodo.properties.get("scrollsInBag") ;
			
			var healpotsInBag = (Integer) frodo.properties.get("healpotsInBag") ;
			
			var score = (Integer) frodo.properties.get("score") ;
			
			var hp = (Integer) frodo.properties.get("hp") ;
			
			//return 10f - (float) numOfScrollsInArea - 0.5f * (float) scrollsInBag ;
			var r = (float) 10*(hp 
					+ (scrollsInBag ==1 ? 100 : (scrollsInBag ==2 ? -50 : 0))
					+ (healpotsInBag==1 ? 20 : 0))	
					;
			//System.out.println(">>>> reward = " + r ) ;
			return r ;
		} ;
		
		
		/*
		 This DOES NOT WORK because prev and current states are the same reference to the
		 same state object! 
		 
		alg.actionDirectRewardFunction = (z0, nextState) -> {
			var state  = z0.fst ;
			var action = z0.snd ;
			if (alg.topGoalPredicate.test(nextState))
				return alg.maxReward ;
			if (alg.agentIsDead.test(nextState))
				return -100f ;
			var score0 = (Integer) state.worldmodel.elements.get("Frodo")
					.properties.get("score") ;
			
			var scrollsInBag0 = (Integer) state.worldmodel.elements.get("Frodo")
					.properties.get("scrollsInBag") ;
			
			var healPotsInBag0 = (Integer) state.worldmodel.elements.get("Frodo")
					.properties.get("healpotsInBag") ;
			
			//var hp0 = (Integer) state.worldmodel.elements.get("Frodo")
			//		.properties.get("hp") ;
			
			var score1 = (Integer) nextState.worldmodel.elements.get("Frodo")
					.properties.get("score") ;
			
			var scrollsInBag1 = (Integer) nextState.worldmodel.elements.get("Frodo")
					.properties.get("scrollsInBag") ;
			
			var healPotsInBag1 = (Integer) nextState.worldmodel.elements.get("Frodo")
					.properties.get("healpotsInBag") ;
			
			//var hp1 = (Integer) nextState.worldmodel.elements.get("Frodo")
			//		.properties.get("hp") ;
			
			return (float)(50*(score1 - score0)
					+ 50*Math.abs(scrollsInBag1 - scrollsInBag0)
					+ 50*Math.abs(healPotsInBag1 - healPotsInBag0)
					) ;
		} ;
		*/
		
		
		alg.maxDepth = 800 ;
		alg.maxNumberOfEpisodes = 60 ;
		alg.delayBetweenAgentUpateCycles = 2 ;
		alg.totalSearchBudget = 1600000 ;
		alg.gamma = 0.95f ;
		alg.exploreProbability = 0.08f ;
		alg.enableBackPropagationOfReward = 20 ; 
		alg.stopAfterGoalIsAchieved = true ;
		alg.maxReward = 1000000 ;
		
		return alg ;	
	}

	/**
	 * Test that the algorithm can work to find a solution, and that
	 * the solution can be replayed. In this test, the search is stopped
	 * as soon as the goal state is reached. The trace to this state
	 * is then extracted, and we check if replaying it loads to the
	 * goal state.
	 */
	//@Test
	public void testAQalg() throws Exception {
				
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
		
		var RR = alg.runWinningPlay() ;
		assertTrue(RR.topPredicateSolved) ;
		
		var bestSequence = alg.play(alg.maxDepth) ;
		
		System.out.println(">>> Replayed the found winning play.");
		System.out.println(">>> " + RR);
			
		int num_entries = 0 ;
		for (var q : alg.qtable.values()) {
			num_entries += q.values().size() ;
		}
		alg.log(">>> #states in qtable: " + alg.qtable.size() 
				+ ", #entries in qtable: " + num_entries);
		alg.log(">>> episode-values: " + R.episodesValues) ;
		alg.log(">>> winningplay: " + R.winningplay) ;
		alg.log(">>> best sequence: " + bestSequence) ;		
	}

	/**
	 * Test that a winning play can be extracted from the model learned
	 * by Q-alg.
	 */
	@Test
	public void test_model_Q() throws Exception {
	
		var alg = constructAlgorithm();
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
