package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import eu.iv4xr.framework.extensions.ltl.LTL;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.AQalg;
import eu.iv4xr.framework.goalsAndTactics.BasicSearch;
import eu.iv4xr.framework.goalsAndTactics.BasicSearch.AlgorithmResult;
import eu.iv4xr.framework.goalsAndTactics.XEvolutionary;
import eu.iv4xr.framework.goalsAndTactics.XMCTS;
import eu.iv4xr.framework.goalsAndTactics.XQalg;

import static nl.uu.cs.aplib.AplibEDSL.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;


/**
 * This contains functions for creating instances of test-algorithms (subclasses of
 * {@link BasicSearch}) configured to target MD, and long with a deployed MD to
 * run the instantiated algorithm on it.
 */
public class TestAlgorithmsFactory {

	public boolean withGraphics = true ;
	public boolean withSound = false ;
	public boolean supressLogging = true ;
	// in ms:
	int delayBetweenAgentUpateCycles = 10 ;
	//public boolean verbosePrint = false ;
	//public boolean stopAfterAllAgentsDie = true ;
    //public boolean inTwoPlayersSetup_StopWhenAgent1GoalIsConcluded = true ;
    public boolean stopWhenInvriantFlagABug = false ;
    //public boolean stopWhenGameOver = false ;
	public boolean withInstrumenter = false ;
	// not used  --> ?
	boolean includeWallBug = false ;
	
	
	/**
	 * An extension of TestAgent that holds few additional information. 
	 * Among other thing, a reference to MD-invariants to facilitate faster
	 * violation check.
	 */
	public static class XTestAgent extends TestAgent {
		
		
		public MD_invs MDinvs_ ;
		boolean verbosePrint = false ;
		
		public  XTestAgent(String agentName, String role) {
			super(agentName,role) ;
 		}
		
		boolean invViolationDetected() {
			return MDinvs_.bugFlagged ;
		}
	}
	
	
	/**
	 * For the experiment we will maintain a single instance of MD. This avoids multi-episode
	 * runs to keep creating instances of MD. Since the MD-app is an awt-component, creating
	 * one seems create a new thread as well. So, keep creating MD-instances also trigger creations
	 * of threads, and this runs into a problem as there is a cap in the maximum number of 
	 * threads.
	 */
	public static DungeonApp miniDungeonInstance = null ;
	
	
	/**
	 * Instantiate and deploy MD, and construct a TestAgent. Will only use Frodo.
	 * This will also configure the test agent to have a data-collector and to
	 * attach MD-invariants for checking.
	 */
	public XTestAgent constructAgent(MiniDungeonConfig config) throws Exception {
		if (miniDungeonInstance==null ||! miniDungeonInstance.dungeon.config.toString().equals(config.toString())) {
			// there is no MD-instance yet, or if the config is different than the config of the
			// running MD-instance, then we create a fresh MD instance:
			DungeonApp app = new DungeonApp(config);
			// setting sound on/off, graphics on/off etc:
			app.soundOn = withSound ;
			app.headless = ! withGraphics ;
			if(withGraphics) 
				DungeonApp.deploy(app);	
			System.out.println(">>> LAUNCHING a new instance of MD") ;
			miniDungeonInstance = app ;
		}
		else {
			// if the config is the same, we just reset the state of the running MD:
			miniDungeonInstance.keyPressedWorker('z');
			System.out.println(">>> RESETING MD") ;
		}
		
		MyAgentEnv env = new MyAgentEnv(miniDungeonInstance);
		MyAgentState state = new MyAgentState();
		
		var agentFrodo = new XTestAgent("Frodo", "tester");
		agentFrodo.attachState(state).attachEnvironment(env) ;
		
		// should be after create the agent, else the constructor sets the visibility again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}
		
		agentFrodo . setTestDataCollector(new TestDataCollector()) ;
		if (withInstrumenter) {
			agentFrodo . withScalarInstrumenter(S -> 
				MDTestAgentRunner.stateInstrumenter((MyAgentState) S)) ;
		}
		MD_invs invs1 = new MD_invs() ;
		agentFrodo.MDinvs_ = invs1 ;
		agentFrodo . addInv(invs1.allInvs) 
			       . resetLTLs() ;
		
		var test1 = LTL.eventually((SimpleState S) -> {
			MyAgentState S_ = (MyAgentState) S ;
			var frodo = S_.worldmodel.elements.get("Frodo") ;
			int mazeId = Utils.mazeId(frodo) ;
			return mazeId == 1 ;
		}) ;
		
		var test2 = LTL.always((SimpleState S) -> false) ;
		
		//agentFrodo . addLTL(test1) .resetLTLs() ;
		
		Thread.sleep(1000);				
		// add a single update:
		//agent.update();
		//System.out.println(">>> WOM: " + state.worldmodel) ;
		return agentFrodo ;
	}
	
	/**
	 * A wrapper for a goal-based aplib test for MD as an instance of {@link BasicSearch}.
	 * Currently it can either do action-level random testing, or run a play-test to
	 * cleanse all shrines.
	 */
	public static class ProgrammaticAlgorithm extends BasicSearch {
		
		String playtestType ;
		
		public ProgrammaticAlgorithm(String playtestType) {
			super() ;
			this.playtestType = playtestType ;
		}
	
		@Override
		public AlgorithmResult runAlgorithm() throws Exception {
			
			XTestAgent agent = (XTestAgent) this.agentConstructor.apply(null) ;
			var env_ = (MyAgentEnv) agent.env() ;
			var config = env_.app.dungeon.config ;
			GoalStructure G = null ;
			if (playtestType.equals("action-level-random")) {
				var RT = new RandomPlayTester() ;
				RT.reallyRandom = true ;
				G = RT.randomPlay(agent) ;
			}
			else {
				G = new ShrineCleanTester().cleanseAllShrines(agent, config.numberOfMaze) ;
			}		
			agent.setGoal(G) ;
			var state = (MyAgentState) agent.state() ;
			turn = 0 ;
			long time0 = System.currentTimeMillis() ;
			long timeUsed = 0 ;
			while(G.getStatus().inProgress()) {
				agent.update();
				
				if (agent.verbosePrint) {
					System.out.println("** [" + turn + "/" + state.val("aux","turn") + "]") ;
					System.out.print("   agent1 " + agent.getId() 
						+ " @" + Utils.toTile(state.worldmodel.position) 
					    + ", maze:" + state.val("maze")
					    + ", hp:" + state.val("hp")
					    + ", score:" + state.val("score")
					    + ", goal-status:" + G.getStatus()) ;
					if (state.env().getLastOperation() != null) {
						System.out.println(", action:" + state.env().getLastOperation().command) ;
					}
					else System.out.println("") ;
				}
				// delay to slow it a bit for displaying:
				Thread.sleep(this.delayBetweenAgentUpateCycles); 
				if (!state.agentIsAlive()) {
					break ;
				}
				timeUsed = System.currentTimeMillis() - time0 ;
				if (timeUsed > this.totalSearchBudget)
					break ;
				turn++ ;
			}	
			this.foundError = ! agent.evaluateLTLs() ;
			totNumberOfEpisodes = 1 ;
			remainingSearchBudget = totalSearchBudget - (int) timeUsed ;
			if (topGoalPredicate.test(state)) {
				markThatGoalIsAchieved(new LinkedList<String>()) ;
			}
			AlgorithmResult R = new AlgorithmResult() ;
			R.algName = "Programmatic" ;
			R.totEpisodes = totNumberOfEpisodes ;
			R.usedTurns = turn ;
			R.usedBudget = (int) timeUsed ;
			R.goalAchieved = this.goalHasBeenAchieved() ;	
			R.foundError = this.foundError ;
			
			return R ;	
		}
	
	}
	
	/** 
	 * We will use explore that comes with full survival, to be on-par with 
	 * the programmatic play-test approach.
	 */
	static GoalStructure smartExplore(TestAgent agent) {
		var T = new ShrineCleanTester() ;
		T.useSurvivalTactic = true ;
		return T.areaExplored(agent) ;
	}
	
	/**
	 * Configure the basic functionalities of {@link #alg}. E.g. how to
	 * instantiate a test-agent, how to navigate, how to interact, etc.
	 * 
	 * <p>We will use the same smart navigateTo and 
	 * smart-explore that were used by the programmatic approach. If you
	 * want to suppress exploration (e.g. for faster search, because then
	 * the algorithm won't keep doing exploration to update its state),
	 * you can set alg.exploredG back to null.
	 * 
	 * <p>The top-goal is set to cleanse the immortal shrine (or in other
	 * words, to win the game).
	 * 
	 * <p>We will also constrain the algorithm to only interact with scrolls
	 * and shrines.
	 */
	void basicConfigure(MiniDungeonConfig config, BasicSearch alg) {
		
		var goalLib = new GoalLib();
		BasicSearch.DEBUG = !supressLogging ;

		alg.stopWhenErrorIsFound = this.stopWhenInvriantFlagABug ;
		
		alg.agentConstructor = dummy -> {
			try {
				return constructAgent(config) ;
			}
			catch(Exception e) {
				System.out.println(">>> FAIL to create a agent.") ;
				return null ;
			}
		} ;
		
		alg.closeEnv = dummy -> {
			// don't need to do anything special for closing:
			//var env = (MyAgentEnv) alg.agent.env() ;
			//var win = SwingUtilities.getWindowAncestor(env.app);
			//win.dispose();
			//System.out.println(">>> DISPOSING MD") ;
			return null ;
		} ;
		
		// just a dummy goal to initialize the agent, so that the worldmodel is loaded:
		alg.initializedG = dummy -> {
			GoalStructure dummyG = goal("dummy").toSolve(S -> true)
			  .withTactic(action("dummy").do1(S -> true).lift())
			  .lift() ;
			return dummyG ;
		} ;
		
		alg.maxReward = 1000000 ;
		
		alg.exploredG   = huristicLocation -> smartExplore(alg.agent) ;
		alg.reachedG    = e -> SEQ(goalLib.smartEntityInCloseRange(alg.agent,e), SUCCESS()) ;
		alg.interactedG = e -> SEQ(goalLib.entityInteracted(e), SUCCESS()) ;
		// constrain to only interact with scrolls and shrines:
		alg.isInteractable   = e -> e.id.contains("S") && !e.id.contains("SS");
		
		// we'll fix the goal at winning the game, so cleaning the immortal shrine:
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			var env_ = (MyAgentEnv) state.env();
			int lastMaze = env_.app.dungeon.config.numberOfMaze - 1 ;
			String immortalShrineId = "SI" + lastMaze ;
			var shrineImmortal = state.worldmodel.elements.get(immortalShrineId) ;
			return shrineImmortal != null
					&& (Boolean) shrineImmortal.properties.get("cleansed") ;
		} ;
		
		alg.agentIsDead = state -> {
			var frodo = state.worldmodel.elements.get("Frodo") ;
			return frodo != null
					&& ((Integer) frodo.properties.get("hp")) <= 0 ;
		} ;
		
		alg.wipeoutMemory = agent -> {
			var state = (MyAgentState) agent.state() ;
			state.multiLayerNav.wipeOutMemory(); 
			return null ;
		} ;
		
		alg.delayBetweenAgentUpateCycles = this.delayBetweenAgentUpateCycles ;
	}
	
	/**
	 * Create an instance of {@link BasicSearch}, already configured to target MD.
	 * 
	 * <p>This also deploys an instance of MD, connected to the algorithm. So, running
	 * the algorithm will run it on that instance of MD.
	 * 
	 * <p>The hyper paramaters of the algorithm are left unconfigured, so you need
	 * to set them accordingly.
	 */
	public BasicSearch mkBasicSearch(MiniDungeonConfig config) {
		var alg = new BasicSearch() ;
		basicConfigure(config,alg) ;
		return alg ;
		
	}
	
	/**
	 * A function that specifies the value of a given MD agent-state. Suitable for
	 * high-level algorithms.
	 */
	static float valueFunctionOfMDState1(BasicSearch alg, Iv4xrAgentState state) {
		if (alg.topGoalPredicate.test(state))
			return alg.maxReward ;
		if (alg.agentIsDead.test(state))
			return -100f ;
		var cleansed = (int) state.worldmodel.elements.values().stream()
				.filter(e -> Utils.isMoonShrine(e) && e.getBooleanProperty("cleansed")) 
				.count();
		var frodo =state.worldmodel.elements.get("Frodo") ;
		int mazeNr = Utils.mazeId(frodo) ;
		float value = (float) 1000*(cleansed + mazeNr) ;
		//var value = (Integer) frodo.properties.get("score") + 1000*mazeNr ;
		return (float) value ;
	}
	
	/**
	 * A function that specifies the value of a given MD agent-state. Suitable for
	 * low-level algorithms that operate at primitive action level.
	 */
	static float valueFunctionOfMDState2(BasicSearch alg, Iv4xrAgentState state) {
		if (alg.topGoalPredicate.test(state))
			return alg.maxReward ;
		if (alg.agentIsDead.test(state))
			return -100f ;
		var numOfScrollsInArea = (int) state.worldmodel.elements.values().stream()
			.filter(e -> e.type.equals("SCROLL"))
			.count();
		
		var frodo = state.worldmodel.elements.get("Frodo") ;
		
		var scrollsInBag = (Integer) frodo.properties.get("scrollsInBag") ;
		
		var healpotsInBag = (Integer) frodo.properties.get("healpotsInBag") ;
		
		var score = (Integer) frodo.properties.get("score") ;
		
		var hp = (Integer) frodo.properties.get("hp") ;
		
		//return 10f - (float) numOfScrollsInArea - 0.5f * (float) scrollsInBag ;
		var r1 = (float) (hp 
				//+ (scrollsInBag ==1 ? 100 : (scrollsInBag ==2 ? -50 : 0))
				//+ (scrollsInBag ==1 ? 100 : 0)
				//+ 100*scrollsInBag 
				+ 1000*(8 - (2*numOfScrollsInArea + scrollsInBag))
				+ (healpotsInBag==1 ? 20  : 0))	
				;
		var cleansed = (int) state.worldmodel.elements.values().stream()
				.filter(e -> Utils.isMoonShrine(e) && e.getBooleanProperty("cleansed")) 
				.count();
		
		int mazeNr = Utils.mazeId(frodo) ;
		float r2 = (float) 1000*(cleansed + mazeNr) ;
		
		//System.out.println(">>>> reward = " + r ) ;
		return r1 + r2 ;
	}
	
	public XEvolutionary mkEvoSearch(MiniDungeonConfig config) {
		var alg = new XEvolutionary() ;
		basicConfigure(config,alg) ;
		alg.stateValueFunction = state -> valueFunctionOfMDState1(alg,state) ;
		return alg ;	
	}
	
	public XMCTS mkMCTS(MiniDungeonConfig config) {
		XMCTS alg = new XMCTS() ;
		basicConfigure(config,alg) ;
		alg.stateValueFunction = state -> valueFunctionOfMDState1(alg,state) ;
		return alg ;	
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public XQalg mkQ(MiniDungeonConfig config) {
		XQalg alg = new XQalg() ;
		basicConfigure(config,alg) ;
		alg.stateValueFunction = state -> valueFunctionOfMDState1(alg,state) ;
		alg.getQstate = (trace,state) -> new MDQstate1((Iv4xrAgentState) state) ;
		return alg ;	
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AQalg mkActionLevelQ(MiniDungeonConfig config) {
		AQalg alg = new AQalg() ;
		basicConfigure(config,alg) ;
		alg.availableActions.put("w", move(Command.MOVEUP)) ;
		alg.availableActions.put("a", move(Command.MOVELEFT)) ;
		alg.availableActions.put("s", move(Command.MOVEDOWN)) ;
		alg.availableActions.put("d", move(Command.MOVERIGHT)) ;
		alg.availableActions.put("e", usePotion(Command.USEHEAL)) ;
		alg.availableActions.put("r", usePotion(Command.USERAGE)) ;
		
		// attach a function that converts the agent state to a Q-state:
		int observeWindowSize = 9 ;
		alg.getQstate = (trace,currentState) -> {
					var env = (MyAgentEnv) alg.agent.env() ;
					return new MDQstate2(env.app.dungeon.config, observeWindowSize, (Iv4xrAgentState) currentState) ;
		} ;
				
		alg.stateValueFunction = state -> valueFunctionOfMDState2(alg,state) ;

		return alg ;	
	}
	
	public ProgrammaticAlgorithm mkProgrammaticAlg(MiniDungeonConfig config) {
		var alg = new ProgrammaticAlgorithm("play-to-win") ;
		basicConfigure(config,alg) ;
		return alg ;	
	}
	
	public ProgrammaticAlgorithm mkActionLevelRandomAlg(MiniDungeonConfig config) {
		var alg = new ProgrammaticAlgorithm("action-level-random") ;
		basicConfigure(config,alg) ;
		return alg ;	
	}
	
	/**
	 * An (abstract) representation of MD state for Q-algorithm. This one is suitable
	 * for the high-level Q, and less suitable for low level Q.
	 */
	public static class MDQstate1 {
		
		public List<String> scrolls = new LinkedList<>() ;
		public List<String> closedShrines = new LinkedList<>() ;
		public int numberOfScrollsInbag = 0 ;
		public boolean alive ;
		
		MDQstate1() { }
		
		@SuppressWarnings("rawtypes")
		public MDQstate1(Iv4xrAgentState state) {
			var frodo = state.worldmodel.elements.get("Frodo") ;
			alive = ((Integer) frodo.properties.get("hp")) > 0 ;
			numberOfScrollsInbag = (Integer) frodo.properties.get("scrollsInBag") ;
			scrolls = state.worldmodel.elements.values().stream()
					.filter(e -> Utils.isScroll(e))
					.map(e -> e.id) 
					.collect(Collectors.toList()) ;
			scrolls.sort((s1,s2) -> s1.compareTo(s2)) ;
			closedShrines = state.worldmodel.elements.values().stream()
					.filter(e -> Utils.isShrine(e))
					.filter(e -> ! (Boolean) e.properties.get("cleansed"))
					.map(e -> e.id) 
					.collect(Collectors.toList()) ;
			closedShrines.sort((s1,s2) -> s1.compareTo(s2)) ;
		}
		
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof MDQstate1)) return false ;
			MDQstate1 o_ = (MDQstate1) o ;
			return this.scrolls.equals(o_.scrolls)
					&& this.closedShrines.equals(o_.closedShrines)
					&& this.numberOfScrollsInbag == o_.numberOfScrollsInbag 
					&& this.alive == o_.alive ;
		}
		
		@Override
	    public int hashCode() {
	        return scrolls.hashCode() 
	        		+ closedShrines.hashCode() 
	        		+ 31*numberOfScrollsInbag 
	        		+ (alive?1:0) ;
	    }
	}
	
	
	
	/**
	 * An (abstract) representation of MD state for Q-algorithm. This one is suitable
	 * for low-level Q. We use byte-array to keep its size small.
	 */
	public static class MDQstate2 {
		
		public byte[] state ;
			
		/**
		 * Construct a Q-state. 
		 * 
		 * <p> "windowSize" defines a rectangle area around the agent,
		 * where observation will be obtained. That is, only things within this rectangle,
		 * and moreover visible to the agent (within its visibility range) will be included
		 * in the constructed Q-state.
		 */
		public MDQstate2(MiniDungeonConfig mdconfig, int windowSize, Iv4xrAgentState agentstate) {
			
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
	
	/**
	 * Low-level move-command for MD. This is for low-level Q.
	 */
	@SuppressWarnings("incomplete-switch")
	static Action move(Command cmd) {
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

	/**
	 * Low-level use-pot command for MD. This is for low-level Q.
	 */
	@SuppressWarnings("incomplete-switch")
	static Action usePotion(Command cmd) {
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
	
}
