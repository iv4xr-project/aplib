package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import eu.iv4xr.framework.goalsAndTactics.BasicSearch;

import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;


/**
 * Creating an instance {@link BasicSearch}, configured for MD.
 */
public class BasicSearchAlgForMD {

	public boolean withGraphics = true ;
	public boolean withSound = false ;
	public boolean supressLogging = true ;
	//public boolean verbosePrint = false ;
	//public boolean stopAfterAllAgentsDie = true ;
    //public boolean inTwoPlayersSetup_StopWhenAgent1GoalIsConcluded = true ;
    public boolean stopWhenInvriantFlagABug = false ;
    //public boolean stopWhenGameOver = false ;
	public boolean withInstrumenter = false ;
	public boolean saveRunData = true ;
    public String runDataFolder = "./tmp" ;
	// not used  --> ?
	boolean includeWallBug = false ;

	String algName ;    	
	BasicSearch alg ;
	
	/**
	 * This only creates an instance of this class with an un-configured instance
	 * of {@link BasicSearch}. Invoke {@link #basicConfigure(MiniDungeonConfig)}
	 * to have the basic functions of the algorithm configured, e.g. how to
	 * instantiate a test-agent, how to navigate, how to interact, etc.
	 */
	public BasicSearchAlgForMD() {
		algName = "HIGH-RANDOM" ;
		alg = new BasicSearch() ;
	}

	/**
	 * Instantiate and deploy MD, and construct a TestAgent. Will only use Frodo.
	 * This will also configure the test agent to have a data-collector and to
	 * attach MD-invariants for checking.
	 */
	TestAgent constructAgent(MiniDungeonConfig config) throws Exception {
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = withSound ;
		app.headless = ! withGraphics ;
		if(withGraphics) 
			DungeonApp.deploy(app);	
		System.out.println(">>> LAUNCHING MD") ;
		
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState();
		
		var agentFrodo = new TestAgent("Frodo", "tester");
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
	public void basicConfigure(MiniDungeonConfig config) {
		
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
	}
	
	
	public static class Result1 {
		public String algName ;
		public String runId ;
		public int runtime ;
		public int usedTurn ;
		public int numEpisodes ;
		public Integer numOfVisitedStates = null ;
		public boolean topGoalSolved ;
		public boolean invViolationDetected ;
		
		@Override
		public String toString() {
			String z = "** Alg-name: " + algName ;
			z += "\nRun " + runId ;
			z += "\n#turn:" + usedTurn ;
			z += "\n#episodes:" + numEpisodes ;
			z += "\ntime:" + runtime ;
			z += "\n#visisted abs-states:" + (numOfVisitedStates==null ? "not tracked" : "" + numOfVisitedStates) ;
			z += "\ntop-goal solved:" + topGoalSolved ;
			z += "\ninv-violation detected:" + invViolationDetected ;
			return z ;
		}
	}
	
	/**
	 * Run the algorithm, report the result at the end. Saving it to a file
	 * if configured to do so.
	 */
	public Result1 runAlgorithm(String runId) throws Exception {
		System.out.println(">> Start of run " + this.algName) ;
		alg.runAlgorithm()  ;
		Result1 R = new Result1() ;
		R.algName = this.algName ;
		R.runId = runId ;
		R.usedTurn = alg.turn ;
		R.numEpisodes = alg.totNumberOfEpisodes ;
		R.runtime = alg.totalSearchBudget - alg.getRemainingSearchBudget() ;
		R.topGoalSolved = alg.goalHasBeenAchieved() ;
		// TODO:
		// R.invViolationDetected = ....
		System.out.println(">> End of run") ;
		System.out.println(R.toString()) ;
		//System.out.println(">>> Game status:" + state.gameStatus()) ;
		if (saveRunData) {
			String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new java.util.Date());
			String reportFile = runId + "_" + timestamp + ".txt" ;
			Files.writeString(
			        Path.of(runDataFolder, reportFile),
			        R.toString() + "\n",
			        CREATE, WRITE
			    );
			// saving data traces:
			if (withInstrumenter) {
				String tracefile = runId + "_" + timestamp + ".csv" ;
			    tracefile = Paths.get(runDataFolder,tracefile).toString() ;
			    alg.agent.getTestDataCollector().saveTestAgentScalarsTraceAsCSV(alg.agent.getId(),tracefile) ;
			}
		}
		
		return R ;
	}
	
	
}
