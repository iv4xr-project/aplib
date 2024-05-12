package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Maze;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;

/**
 * A runner for an aplib test-agent to test MiniDungeon. 
 */
public class MDTestAgentRunner {

	public boolean withGraphics = true ;
	public boolean withSound = false ;
	public boolean supressLogging = true ;
	public boolean verbosePrint = false ;
	public boolean stopAfterAllAgentsDie = true ;
    public boolean inTwoPlayersSetup_StopWhenAgent1GoalIsConcluded = true ;
    public boolean stopWhenInvriantFlagABug = false ;
    public boolean stopWhenGameOver = false ;
	public boolean withInstrumenter = true ;
	public boolean saveRunData = true ;
    public String runDataFolder = "./tmp" ;
	
	boolean includeWallBug = false ;
	
	int usedNumberOfTurns ;
	
	/**
	 * An instrumenter to collect data from a test run.
	 */
	static Pair<String,Number>[] stateInstrumenter(MyAgentState S) {
		Pair<String,Number>[] dataPoint = new Pair[9] ;
		Tile t = Utils.toTile(S.worldmodel.position) ;
		int mazeNr = Utils.currentMazeNr(S) ;
		Integer bagBefore = (Integer) S.before("bagUsed") ;
		boolean usingItem = bagBefore != null 
				&& bagBefore > (Integer) S.val("bagUsed") ;
		boolean pickingUpItem = bagBefore != null 
				&& bagBefore < (Integer) S.val("bagUsed") ;
		Integer hpBefore = (Integer) S.before("hp") ; 
		boolean figthing = hpBefore != null 
				&& hpBefore > (Integer) S.val("hp") ;
		dataPoint[0] = new Pair<>("turn",(Integer) S.val("aux","turn")) ;
		dataPoint[1] = new Pair<>("x",t.x) ;
		dataPoint[2] = new Pair<>("y",t.y) ;
		dataPoint[3] = new Pair<>("maze",mazeNr) ;
		dataPoint[4] = new Pair<>("hp", (Integer) S.val("hp")) ;
		dataPoint[5] = new Pair<>("score", (Integer) S.val("score")) ;
		
		dataPoint[6] = new Pair<>("use", usingItem? 1 : 0) ;
		dataPoint[7] = new Pair<>("pickup", pickingUpItem? 1 : 0) ;
		dataPoint[8] = new Pair<>("fight", figthing? 1 : 0) ;
		
		return dataPoint ;
	}
	
	
	/**
	 * Give the goal G to the agent and run it on MiniDungeon.
	 */
	public void runAgent(String runId,
			TestAgent agent, 
			MiniDungeonConfig config,
			GoalStructure G, 
			int budget,
			int sleep) throws Exception {
		
	    runAgent(runId,agent,null,config,G,null,budget,sleep) ;
	}
		
	/**
	 * Give the goals G1 and G2 to two agents and run it on MiniDungeon.
	 */
	public void runAgent(
			String runId,
			TestAgent agent1, 
			TestAgent agent2,
			MiniDungeonConfig config,
			GoalStructure G1,
			GoalStructure G2,
			int budget,
			int sleep) throws Exception {
		float oldProbabilityBuggyWall = Maze.probabilityBuggyWall ;
		if (! includeWallBug) {
			Maze.probabilityBuggyWall = 0 ;
		}
		try {
			runAgentWorker(runId,agent1,agent2,config,G1,G2,budget,sleep) ;
		}
		finally {
			Maze.probabilityBuggyWall = oldProbabilityBuggyWall ;
		}
	}
	
	public void runAgentWorker(
			String runId,
			TestAgent agent1, 
			TestAgent agent2,
			MiniDungeonConfig config,
			GoalStructure G1,
			GoalStructure G2,
			int budget,
			int sleep) throws Exception {
		
		System.out.println(">> Configuration:\n" + config);
		
		DungeonApp app = new DungeonApp(config);
		app.soundOn = withSound;
		app.headless = !withGraphics ;
		if(withGraphics) DungeonApp.deploy(app);
		
		// should be after create the agent, else the constructor sets the visibility again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}

		MyAgentEnv env1 = new MyAgentEnv(app);
		MyAgentEnv env2 = new MyAgentEnv(app);
		MyAgentState state1 = new MyAgentState();
		MyAgentState state2 = new MyAgentState();
		
		agent1 
		   . attachState(state1) 
		   . attachEnvironment(env1)  
		   . setGoal(G1) 
		   . setTestDataCollector(new TestDataCollector()) ;
		if (withInstrumenter) {
			agent1 . withScalarInstrumenter(S -> stateInstrumenter((MyAgentState) S)) ;
		}
		MD_invs invs1 = new MD_invs() ;
		MD_invs invs2 = null ;
		agent1
		   . addInv(invs1.allInvs) 
		   . resetLTLs() ;
		
		if (agent2 != null) {
			agent2
			   . attachState(state2) 
			   . attachEnvironment(env2)  
			   . setGoal(G2) 
			   . setTestDataCollector(new TestDataCollector()) ;
			if (withInstrumenter) {
				agent2.withScalarInstrumenter(S -> stateInstrumenter((MyAgentState) S)) ;
			}
			invs2 = new MD_invs() ;
			agent2
			   . addInv(invs2.allInvs) 
			   . resetLTLs() ;
		}
		
		Thread.sleep(1000);
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		int aterdieCount = 2 ;
		long time = System.currentTimeMillis() ;
		while(G1.getStatus().inProgress() || (G2 != null && G2.getStatus().inProgress())) {
			agent1.update();
			if (agent2 != null) {
				if (sleep>0) Thread.sleep(sleep); 
				agent2.update();
			}
			if (verbosePrint) {
				System.out.println("** [" + k + "/" + state1.val("aux","turn") + "]") ;
				System.out.print("   agent1 " + agent1.getId() 
					+ " @" + Utils.toTile(state1.worldmodel.position) 
				    + ", maze:" + state1.val("maze")
				    + ", hp:" + state1.val("hp")
				    + ", score:" + state1.val("score")
				    + ", goal-status:" + G1.getStatus()) ;
				if (state1.env().getLastOperation() != null) {
					System.out.println(", action:" + state1.env().getLastOperation().command) ;
				}
				else System.out.println("") ;
				if (agent2 != null) {
			       System.out.println("   agent2 " + agent2.getId() 
			       + " @" + Utils.toTile(state2.worldmodel.position)
			       + ", maze:" + state2.val("maze")
				   + ", hp:" + state2.val("hp")
				   + ", score:" + state2.val("score")
				   + ", goal-status:" + G2.getStatus());
			       if (state2.env().getLastOperation() != null) {
						System.out.println(", action:" + state2.env().getLastOperation().command) ;
					}
					else System.out.println("") ;
				}
				//System.out.println(">>> agent1 ragetimer: " + state1.val("rageTimer") + ", before: " + state1.before("rageTimer")) ;
			}
			// delay to slow it a bit for displaying:
			if (sleep>0) Thread.sleep(sleep); 
			if (k>=budget) break ;
			if (agent2 != null 
					&& inTwoPlayersSetup_StopWhenAgent1GoalIsConcluded
					&& !G1.getStatus().inProgress()) {
				break ;
			}
			if (stopAfterAllAgentsDie && !state1.agentIsAlive() 
					&& (agent2==null || !state2.agentIsAlive())) {
				aterdieCount-- ;
			}
			if (aterdieCount<=0) break ;
			if (stopWhenInvriantFlagABug 
					&& (invs1.bugFlagged
					    || (invs2 != null && invs2.bugFlagged))) {
				System.out.println(">> inv violation signaled: " + invs1.invViolated) ;
				break ;
			}
			if (stopWhenGameOver && ((GameStatus) state1.val("aux","status")) != GameStatus.INPROGRESS) {
				break ;
			}
			k++ ;
		}	
		usedNumberOfTurns = k ;
		time = System.currentTimeMillis() - time ;
		System.out.println(">> End of run. #turns=" + k + ", time=" + time) ;
		System.out.println("   Game status:" + state1.val("aux","status")) ;
		System.out.println("   Goal G1 status:" + G1.getStatus()
				+ ", " + agent1.getId() + " hp: " + state1.val("hp")
				+ ", score: " + state1.val("score")) ;
		if (agent2 != null) {
			System.out.println("   "
					+ "Goal G2 status:" + G2.getStatus()
					+ ", " + agent2.getId() + " hp: " + state2.val("hp")
					+ ", score: " + state2.val("score")) ;
		}
		//System.out.println(">>> Game status:" + state.gameStatus()) ;
		if (saveRunData && withInstrumenter) 
			saveRunData(runId,agent1,agent2,time) ;

	}
	
	void saveRunData(String runId,
			TestAgent agent1, 
			TestAgent agent2, 
			long usedTime) throws IOException {
		String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new java.util.Date());
		
		// saving data traces:
		String tracefile1 = runId + "_" + agent1.getId() + "_" + timestamp + ".csv" ;
		if (runDataFolder != null) {
		    tracefile1 = runDataFolder + File.separator + tracefile1 ;
		    agent1.getTestDataCollector().saveTestAgentScalarsTraceAsCSV(agent1.getId(),tracefile1) ;
		}
		if (agent2 != null) {
			String tracefile2 = runId + "_" + agent2.getId() + "_" + timestamp + ".csv" ;
			if (runDataFolder != null) {
			    tracefile2 = runDataFolder + File.separator + tracefile2 ;
			    agent2.getTestDataCollector().saveTestAgentScalarsTraceAsCSV(agent2.getId(),tracefile2) ;
			}
		}

		// writing general statistics info to a file:
		MyAgentState S1 = (MyAgentState) agent1.state() ;
		int numberOfTurns = (Integer) S1.val("aux","turn") ;
		boolean agent1Alive = (Integer) S1.val("hp") > 0 ;
		String info = "Run " + runId
				+ "\n#turn:" + numberOfTurns 
				+ "\ntime:" + usedTime
				+ "\ngamestatus:" + S1.val("aux","status")
				+ "\n" + agent1.getId() + " alive:" + agent1Alive ;
		if (agent2 != null) {
			MyAgentState S2 = (MyAgentState) agent2.state() ;
			boolean agent2Alive = (Integer) S2.val("hp") > 0 ;
			info += "\n" + agent2.getId() + " alive:" + agent2Alive ;
		}
		String infoFile = runId + "_" + timestamp + ".txt" ;
		if (runDataFolder != null) {
			infoFile = runDataFolder + File.separator + infoFile ;
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(infoFile));
        writer.write(info);
        writer.close();		
	}
	
}
