package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import eu.iv4xr.framework.goalsAndTactics.BasicSearch;

import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;

import java.util.LinkedList;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;


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
	 * Instantiate and deploy MD, and construct a TestAgent. Will only use Frodo.
	 * This will also configure the test agent to have a data-collector and to
	 * attach MD-invariants for checking.
	 */
	XTestAgent constructAgent(MiniDungeonConfig config) throws Exception {
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = withSound ;
		app.headless = ! withGraphics ;
		if(withGraphics) 
			DungeonApp.deploy(app);	
		System.out.println(">>> LAUNCHING MD") ;
		
		MyAgentEnv env = new MyAgentEnv(app);
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
		
		Thread.sleep(1000);				
		// add a single update:
		//agent.update();
		//System.out.println(">>> WOM: " + state.worldmodel) ;
		return agentFrodo ;
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
		
		
		alg.exploredG   = huristicLocation -> smartExplore(alg.agent) ;
		alg.reachedG    = e -> goalLib.smartEntityInCloseRange(alg.agent,e) ;
		alg.interactedG = e -> goalLib.entityInteracted(e) ;
		// constrain to only interact with scrolls and shrines:
		alg.isInteractable   = e -> e.id.contains("S") ;
		
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
	
	public ProgrammaticAlgorithm mkProgrammaticAlg(MiniDungeonConfig config) {
		var alg = new ProgrammaticAlgorithm() ;
		basicConfigure(config,alg) ;
		return alg ;	
	}
	
	
	public static class ProgrammaticAlgorithm extends BasicSearch {
		
	
		@Override
		public AlgorithmResult runAlgorithm() throws Exception {
			
			XTestAgent agent = (XTestAgent) this.agentConstructor.apply(null) ;
			var env_ = (MyAgentEnv) agent.env() ;
			var config = env_.app.dungeon.config ;
			var G = new ShrineCleanTester().cleanseAllShrines(agent, config.numberOfMaze) ;
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
			
			return R ;	
		}
	
	}
	
	
	
}
