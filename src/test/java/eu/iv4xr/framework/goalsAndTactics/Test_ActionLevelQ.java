package eu.iv4xr.framework.goalsAndTactics;

import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;

import java.util.Arrays;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.Test_Q.MDQstate;
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
	
	/**
	 * A Q-state representation of MD-state. We use byte-array to keep its size small.
	 */
	static class MDQstate2 {
		byte[] state ;
		
		
		/**
		 * Construct a Q-state. 
		 * 
		 * <p> "windowSize" defines a rectangle area around the agent,
		 * where observation will be obtained. That is, only things within this rectangle,
		 * and moreover visible to the agent (within its visibility range) will be included
		 * in the constructed Q-state.
		 */
		MDQstate2(MiniDungeonConfig mdconfig, int windowSize, Iv4xrAgentState agentstate) {
			
			if (windowSize % 2 != 1) 
				throw new IllegalArgumentException("WindowSize should be an odd number.") ;
			
			int N = mdconfig.worldSize ;
			int N__ = N-2 ;
			int W = Math.min(N-2,windowSize) ;
			int half_W = (W-1)/2 ;
			var wom = agentstate.worldmodel ;
			// for now, using only Frodo:
			var frodo = wom.elements.get("Frodo") ;
			// 7 properties
			int agent_x = (int) frodo.position.x ;
			int agent_y = (int) frodo.position.z ;
			int agent_mazeId = (Integer) frodo.properties.get("maze") ;
			int hp = (Integer) frodo.properties.get("hp") ;
			int numOfScrollsInBag = (Integer) frodo.properties.get("scrollsInBag") ;
			int numOfHealPotsInBag = (Integer) frodo.properties.get("healpotsInBag") ;
			int numOfRagePotsInBag = (Integer) frodo.properties.get("ragepotsInBag") ;
			int numOfProperties = 7 ;
			int arraySize = numOfProperties + W * W ;
			state = new byte[arraySize] ;
			Arrays.fill(state, (byte) 0) ;
			state[0] = (byte) agent_x ;
			state[1] = (byte) agent_y ;
			state[2] = (byte) agent_mazeId ;
			state[3] = (byte) hp ;
			state[4] = (byte) numOfScrollsInBag ;
			state[5] = (byte) numOfHealPotsInBag ;
			state[6] = (byte) numOfRagePotsInBag ;
			int windowBottomLeft_x = Math.max(1, agent_x - half_W) ;
			int windowBottomLeft_y = Math.max(1, agent_y - half_W) ;
			int windowTopRight_x = Math.min(N__, agent_x + half_W) ;
			int windowTopRight_y = Math.min(N__, agent_y
					+ half_W) ;
			
			for (var e : wom.elements.values()) {
				var U = e.properties.get("maze") ;
				if (U == null) continue ;
				int e_mazeId = (Integer) U ;
				if (e_mazeId != agent_mazeId)
					continue ;
				int code = -1 ;
				if (e.id.startsWith("W")) {
					// wall
					code = 1 ;
				}
				else if (e.id.startsWith("H")) {
					code = 2 ;
				}
				else if (e.id.startsWith("R")) {
					code = 3 ;
				}
				else if (e.id.startsWith("S_")) {
					code = 4 ;
				}
				else if (e.id.startsWith("SS")) {
					code = 5 ;
				}
				else if (e.id.startsWith("SI")) {
					code = 6 ;
				}
				else if (e.id.startsWith("SM")) {
					// moonshrine
					var cleansed = (Boolean) e.properties.get("cleansed") ;
					if (cleansed)
						code = 5 ;
					else
						code = 7 ;
				}
				else if (e.id.startsWith("M")){
					int e_x = (int) e.position.x ;
					int e_y = (int) e.position.z ;
					if (Math.abs(e_x - agent_x) == 1 || Math.abs(e_y - agent_y) == 1) {
						code = 9 ;
					}
				}
				if (code >0) {
					int e_x = (int) e.position.x ;
					int e_y = (int) e.position.z ;
					if (windowBottomLeft_x <= e_x && e_x <= windowTopRight_x
							&& windowBottomLeft_y <= e_y && e_y <= windowTopRight_y) {
						int index = numOfProperties 
								+ (e_x - windowBottomLeft_x) 
								+ W * (e_y - windowBottomLeft_y) ;
						state[index] = (byte) code ;
					}
				}
			}		
		}
		
		
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof MDQstate2)) return false ;
			var o_ = (MDQstate2) o ;
			
			return Arrays.equals(this.state, o_.state) ;
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(state) ;
		}
	}
	
	TestAgent constructAgent() throws Exception {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 2 ;
		config.worldSize = 12 ;
		config.numberOfCorridors = 2 ;
		config.viewDistance = 40 ;
		config.numberOfMaze = 3 ;
		config.numberOfScrolls = 3 ;
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
	
	@Test
	public void test0() throws Exception {
				
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
		alg.maxNumberOfEpisodes = 400 ;
		alg.delayBetweenAgentUpateCycles = 5 ;
		alg.totalSearchBudget = 1600000 ;
		alg.gamma = 0.95f ;
		alg.exploreProbability = 0.08f ;
		alg.enableBackPropagationOfReward = 20 ; 
		alg.stopAfterGoalIsAchieved = false ;
		alg.maxReward = 1000000 ;
		
				
		//alg.runAlgorithmForOneEpisode();
		var R = alg.runAlgorithm(); 
		
		if (R.winningplay != null) {
			var replay = alg.runWinningPlay() ;
			System.out.println(">>> Replayed the found winning play.");
			System.out.println(">>> " + replay);
		}
		
		var bestSequence = alg.play(alg.maxDepth) ;
		
		alg.log(">>> #states in qtable: " + alg.qtable.size());
		int num_entries = 0 ;
		for (var q : alg.qtable.values()) {
			num_entries += q.values().size() ;
		}
		alg.log(">>> #entries in qtable: " + num_entries);
		alg.log(">>> episode-values: " + R.episodesValues) ;
		alg.log(">>> winningplay: " + R.winningplay) ;
		alg.log(">>> best sequence: " + bestSequence) ;
		
		

		
		//System.out.println(">>>> hit RET") ;
		//Scanner scanner = new Scanner(System.in);
		//scanner.nextLine() ;
		
	}

}
