package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.CSVUtility;

/**
 * This experiments look into the ability of the test agent to find bugs.
 * It is a kind of mutation test. Note that some of the mutations have to
 * be activated manually by changing the source code (should be indicated
 * in the source code).
 * 
 * <p> Four bugs will be investigated:
 * <ol>
 * <li>Wall bug WMUT (can be activated programmatically)  
 * <li>Rage potion bug RMUT (requires manual activation)
 * <li>Shrine bug SMUT (requires manual activation)
 * <ol>
 * 
 * <p>We will compare goal-based playtests against Random and Adaptive Random
 * (Random Plus/Smart). Each bug will be test under different MD configurations
 * (e.g. maze's size) to simulate the depth of the bug. E.g. making the maze
 * larger would make WMUT to require a longer path to reach.
 * 
 * @author Wish
 *
 */
class Experiment3 {

	boolean saveRunData = false ;
	String dataFolder = "./tmp/TPJ/experiment3" ;

	boolean withGraphics = false ;
	boolean verbosePrint = false ;
	int sleepBetweenUpdates = 5 ;

	int testbudget = 10000 ;
	
	/**
	 * Run the given goal-structure. Return the result of LTL-checking at the end of
	 * the run. True means all LTLs are passed. Else false.
	 */
	boolean runTest(String runId, 
			TestAgent agent1, 
			MiniDungeonConfig config,
			GoalStructure G1, 
			boolean enableWallBug) throws Exception {
		
		var runner = new MDTestRunner() ;
		runner.runDataFolder = dataFolder ;
		runner.withGraphics = withGraphics ;
		runner.verbosePrint = verbosePrint ;
		runner.saveRunData = saveRunData ;
		
		runner.includeWallBug = enableWallBug ;
		runner.stopWhenInvriantFlagABug = true ;
		runner.stopWhenGameOver = true ;
		
		runner.runAgent(runId, agent1, null, config, G1, null, testbudget, sleepBetweenUpdates);
		
		// checking oracles:

		return agent1.evaluateLTLs() ;
	}
	
	// for wall-bug experiment:
	int[] wallBugExperimentMazeSizes = { 21, 31, 41, 51, 61 } ; 
	//int[] mazeSizes = { 21 } ; 
	//int[] mazeSizes = { 61 } ; 
	
	// for ragepotion-bug experiment
	int[] rageBugExperimentNumberOfRagePots = { 12,9,6,3,1 } ; 
	//int[] rageBugExperimentNumberOfRagePots = { 12 } ;
	
	// for the immortal-shrine experiment
	int [] immortalShrineExperimentNumberOfMazes = { 1,2,3,4,5 } ;
	//int [] immortalShrineExperimentNumberOfMazes = { 1 } ;
	
	float[] bugDetectionRateWithPlay = new float[wallBugExperimentMazeSizes.length] ;
	float[] bugDetectionRateWithRandom = new float[wallBugExperimentMazeSizes.length] ;
	float[] bugDetectionRateWithRandomPlus = new float[wallBugExperimentMazeSizes.length] ;
	float[] usedTurnsWithPlay = new float[wallBugExperimentMazeSizes.length] ;
	float[] usedTurnsWithRandom = new float[wallBugExperimentMazeSizes.length] ;
	float[] usedTurnsWithRandomPlus = new float[wallBugExperimentMazeSizes.length] ;
	int numberOfReruns = 10 ;
	
	private void initializeData(int rowsize) {
		bugDetectionRateWithPlay = new float[rowsize] ;
		bugDetectionRateWithRandom = new float[rowsize] ;
		bugDetectionRateWithRandomPlus = new float[rowsize] ;
		usedTurnsWithPlay = new float[rowsize] ;
		usedTurnsWithRandom = new float[rowsize] ;
		usedTurnsWithRandomPlus = new float[rowsize] ;
	}
	
	// change the seed of the config, until one is found that
	// would generate a maze with broken wall
	private boolean findSeedWithBrokenWall(MiniDungeonConfig config, int maxNumberOfAttempts) {
		Random seed = new Random() ;
		for (int k=0; k<maxNumberOfAttempts; k++) {
			var game = new MiniDungeon(config) ;
			if (game.mazes.get(0).hasBrokenWall()) return true ;
			config.randomSeed = seed.nextLong() ;
		}
		return false ;
	}
	
	@Disabled
	@Test
	void findingWallBugExperiment() throws Exception {
		
		long time = System.currentTimeMillis() ;
		Random seed = new Random() ;
		initializeData(wallBugExperimentMazeSizes.length) ;
		
		for (int k=0; k<wallBugExperimentMazeSizes.length; k++) {
			int N = wallBugExperimentMazeSizes[k] ;
			
			MiniDungeonConfig config =  TPJconfigs.MDconfig1() ;
			config.enableSmeagol = false ;
			config.numberOfMonsters = 2 ;
			config.numberOfMaze = 1 ;
			config.numberOfCorridors = 5 ;
			config.viewDistance = 6 ;
			config.worldSize = N ;
			
			System.out.println(">>>> Starting N" + N) ;
			
			var hasWallBug = findSeedWithBrokenWall(config,100) ;
			if (!hasWallBug) {
				throw new Exception("CANNOT generate buggy wall!") ;
			}
			
			String status = "" ;
			var frodo = Experiment1.mkFrodo() ;
			// (1) with wall-play
			boolean ok = runTest("wallTest_wallPlay_N" + N,
					frodo, 
					config,
					new WallsTester() . allWallsChecked(frodo),
					true
					) ;
			
			if (!ok) {
				bugDetectionRateWithPlay[k] = 1 ;
				status += "S " ;
				System.out.println(">>> WALL BUG FOUND!") ;
				usedTurnsWithPlay[k] = (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ;
			}
			else {
				status += "F " ;
				usedTurnsWithPlay[k] = 10000;
			}
			for (int j=0; j<numberOfReruns; j++) {
				System.out.println(">>>> N" + N + ", run " + j) ;
				if (j>0) {
					config.randomSeed = seed.nextLong() ;
					hasWallBug = findSeedWithBrokenWall(config,100) ;
					if (!hasWallBug) {
						throw new Exception("CANNOT generate buggy wall!") ;
					}
				}
				// ==== (2) with random:
				frodo = Experiment1.mkFrodo() ;
				RandomPlayTester randomplay = new RandomPlayTester() ;
				ok = runTest("survivalTest_N" + N + "_random",
						frodo, 
						config,
						randomplay.simpleRandomPlay(),
						true) ;
				if (!ok) {
					bugDetectionRateWithRandom[k] += 1 ;
					usedTurnsWithRandom[k] += (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ;
					System.out.println(">>> WALL BUG FOUND!") ;
					status += "S " ;
				}
				else {
					status += "F " ;
					usedTurnsWithRandom[k] += 10000 ; 
				}
				// ==== (3) with random plus:
				frodo = Experiment1.mkFrodo() ;
				ok = runTest("survivalTest_N" + N + "_random",
						frodo, 
						config,
						randomplay.smarterRandomPlay(frodo),
						true) ;
				if (!ok) {
					bugDetectionRateWithRandomPlus[k] += 1 ;
					usedTurnsWithRandomPlus[k] += (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ; 
					System.out.println(">>> WALL BUG FOUND!") ;
					status += "S " ;
				}
				else {
					status += "F " ;
					usedTurnsWithRandomPlus[k] += 10000 ; 
				}
				System.out.println(">>>> N" + N + "_" + j + ":" + status) ;
			}
			
			float N_ = (float) numberOfReruns ;
			bugDetectionRateWithRandom[k] = bugDetectionRateWithRandom[k] / N_ ;
			usedTurnsWithRandom[k] = usedTurnsWithRandom[k] / N_ ;
			bugDetectionRateWithRandomPlus[k] = bugDetectionRateWithRandomPlus[k] / N_ ;
			usedTurnsWithRandomPlus[k] = usedTurnsWithRandomPlus[k] / N_ ;		
		}
		time = System.currentTimeMillis() - time ;
		System.out.println(">>> Total experiment time = " + ((float) time/ 1000f)) ;
		printResults() ;
		saveResults("wall") ;
	}
	
	@Disabled
	@Test
	void findingRagePotionBugExperiment() throws Exception {
		long time = System.currentTimeMillis() ;
		Random seed = new Random() ;
		initializeData(rageBugExperimentNumberOfRagePots.length) ;
		for (int k=0; k<rageBugExperimentNumberOfRagePots.length; k++) {
			int R = rageBugExperimentNumberOfRagePots[k] ;
			
			MiniDungeonConfig config =  TPJconfigs.MDconfig1() ;
			config.numberOfRagePots = R ;
			config.enableSmeagol = false ;
			config.numberOfMonsters = 8 ;
			config.numberOfMaze = 1 ;
			config.numberOfCorridors = 5 ;
			config.viewDistance = 6 ;
			config.worldSize = 61 ;
			
			System.out.println(">>>> Starting R" + R) ;

			String status = "" ;
			
			for (int j=0; j<numberOfReruns; j++) {
				System.out.println(">>>> R" + R + ", run " + j) ;
				
				config.randomSeed = seed.nextLong() ;
				
				// (1) with wall-play
				var play = new ShrineCleanTester() ;
				play.setToUseMemorizedPathFinding(); 
				var frodo = Experiment1.mkFrodo() ;
				boolean ok = false ;
				ok = runTest("rageTest_ShrinePlay_R" + R,
						frodo, 
						config,
						play.cleanseAllShrines(frodo,config.numberOfMaze),
						false
						) ;	
				if (!ok) {
					bugDetectionRateWithPlay[k] += 1 ;
					status += "S " ;
					System.out.println(">>> RAGEPOT BUG FOUND!") ;
					usedTurnsWithPlay[k] += (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ;
				}
				else {
					status += "F " ;
					usedTurnsWithPlay[k] += 10000;
				}
				// ==== (2) with random:
				
				frodo = Experiment1.mkFrodo() ;
				RandomPlayTester randomplay = new RandomPlayTester() ;
				ok = runTest("rageTest_R" + R + "_random",
						frodo, 
						config,
						randomplay.simpleRandomPlay(),
						false) ;
				if (!ok) {
					bugDetectionRateWithRandom[k] += 1 ;
					usedTurnsWithRandom[k] += (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ;
					System.out.println(">>> RAGEPOT BUG FOUND!") ;
					status += "S " ;
				}
				else {
					status += "F " ;
					usedTurnsWithRandom[k] += 10000 ; 
				}
				// ==== (3) with random plus:
				frodo = Experiment1.mkFrodo() ;
				ok = runTest("survivalTest_R" + R + "_random",
						frodo, 
						config,
						randomplay.smarterRandomPlay(frodo),
						false) ;
				if (!ok) {
					bugDetectionRateWithRandomPlus[k] += 1 ;
					usedTurnsWithRandomPlus[k] += (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ; 
					System.out.println(">>> RAGEPOT BUG FOUND!") ;
					status += "S " ;
				}
				else {
					status += "F " ;
					usedTurnsWithRandomPlus[k] += 10000 ; 
				}
				System.out.println(">>>> R" + R + "_" + j + ":" + status) ;
			}
			
			float N_ = (float) numberOfReruns ;
			bugDetectionRateWithPlay[k] = bugDetectionRateWithPlay[k] / N_ ;
			usedTurnsWithPlay[k] = usedTurnsWithPlay[k] / N_ ;
			bugDetectionRateWithRandom[k] = bugDetectionRateWithRandom[k] / N_ ;
			usedTurnsWithRandom[k] = usedTurnsWithRandom[k] / N_ ;
			bugDetectionRateWithRandomPlus[k] = bugDetectionRateWithRandomPlus[k] / N_ ;
			usedTurnsWithRandomPlus[k] = usedTurnsWithRandomPlus[k] / N_ ;
		}
		time = System.currentTimeMillis() - time ;
		System.out.println(">>> Total experiment time = " + ((float) time/ 1000f)) ;
		printResults() ;
		saveResults("ragepot") ;
	}
	
	
	//@Disabled
	@Test
	void findingImmortalShrineBugExperiment() throws Exception {
		long time = System.currentTimeMillis() ;
		Random seed = new Random() ;
		initializeData(immortalShrineExperimentNumberOfMazes.length) ;
		for (int k=0; k<immortalShrineExperimentNumberOfMazes.length; k++) {
			int L = immortalShrineExperimentNumberOfMazes[k] ;
			
			MiniDungeonConfig config =  TPJconfigs.MDconfig1() ;
			config.enableSmeagol = false ;
			config.numberOfMonsters = 2 ;
			config.numberOfMaze = L ;
			config.numberOfCorridors = 4 ;
			config.numberOfScrolls = 3 ;
			config.worldSize = 17 ;
			
			System.out.println(">>>> Starting L" + L) ;

			String status = "" ;
			
			
			for (int j=0; j<numberOfReruns; j++) {
				System.out.println(">>>> L" + L + ", run " + j) ;
				if (j>0) config.randomSeed = seed.nextLong() ;
				
				// (1) with shrine-play
				var frodo = Experiment1.mkFrodo() ;
				boolean ok = false ;
				var play = new ShrineCleanTester() ;
				play.setToUseMemorizedPathFinding(); 
				ok = runTest("immortalShrineTest_shrinePlay_L" + L,
						frodo, 
						config,
						play.cleanseAllShrines(frodo,config.numberOfMaze),
						false
						) ;
				if (!ok) {
					bugDetectionRateWithPlay[k] = 1 ;
					status += "S " ;
					System.out.println(">>> IMMORTAL SHRINE BUG FOUND!") ;
					usedTurnsWithPlay[k] = (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ;
				}
				// ==== (2) with random:
				frodo = Experiment1.mkFrodo() ;
				RandomPlayTester randomplay = new RandomPlayTester() ;
				ok = runTest("immortalShrineTest_L" + L + "_random",
						frodo, 
						config,
						randomplay.simpleRandomPlay(),
						false) ;
				if (!ok) {
					bugDetectionRateWithRandom[k] += 1 ;
					usedTurnsWithRandom[k] += (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ;
					System.out.println(">>> IMMORTAL SHRINE BUG FOUND!") ;
					status += "S " ;
				}
				else {
					status += "F " ;
					usedTurnsWithRandom[k] += 10000 ; 
				}
				// ==== (3) with random plus:
				frodo = Experiment1.mkFrodo() ;
				ok = runTest("immortalShrineTest_L" + L + "_random",
						frodo, 
						config,
						randomplay.smarterRandomPlay(frodo),
						false) ;
				if (!ok) {
					bugDetectionRateWithRandomPlus[k] += 1 ;
					usedTurnsWithRandomPlus[k] += (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ; 
					System.out.println(">>> IMMORTAL SHRINE BUG FOUND!") ;
					status += "S " ;
				}
				else {
					status += "F " ;
					usedTurnsWithRandomPlus[k] += 10000 ; 
				}
				System.out.println(">>>> L" + L+ "_" + j + ":" + status) ;
			}
			
			float N_ = (float) numberOfReruns ;
			bugDetectionRateWithRandom[k] = bugDetectionRateWithRandom[k] / N_ ;
			usedTurnsWithRandom[k] = usedTurnsWithRandom[k] / N_ ;
			bugDetectionRateWithRandomPlus[k] = bugDetectionRateWithRandomPlus[k] / N_ ;
			usedTurnsWithRandomPlus[k] = usedTurnsWithRandomPlus[k] / N_ ;
		}
		time = System.currentTimeMillis() - time ;
		System.out.println(">>> Total experiment time = " + ((float) time/ 1000f)) ;
		printResults() ;
		saveResults("immortalShrine") ;
	}
	
	
	void printResults() {
		System.out.println("====") ;
		System.out.print("With programmed-play: ") ;
		for (int k=0; k<bugDetectionRateWithPlay.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" 
					+ bugDetectionRateWithPlay[k] 
					+ " (#turns:" + usedTurnsWithPlay[k] +  ")") ;
		}
		System.out.print("\nRandom: ") ;
		for (int k=0; k<bugDetectionRateWithRandom.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" 
					+ bugDetectionRateWithRandom[k]
					+ " (#turns:" + usedTurnsWithRandom[k] +  ")") ;
		}
		System.out.print("\nSmart-random: ") ;
		for (int k=0; k<bugDetectionRateWithRandomPlus.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" 
					+ bugDetectionRateWithRandomPlus[k]
					+ " (#turns:" + usedTurnsWithRandomPlus[k] +  ")") ;
		}
		System.out.println("\n====") ;
	}
	
	void saveResults(String bugType) throws IOException {
		String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new java.util.Date());
		String filename = dataFolder
				+ File.separator
				+ "experiment3_" + bugType + "_" + timestamp + ".csv" ;
		String[] columnNames = { "maze-size",
				"with-wall-play", "wpturns",
				"random", "rndturns",
				"smart-random", "srndturns" } ;
		List<Number[]> data = new LinkedList<Number[]>() ;
		for (int k=0; k<bugDetectionRateWithPlay.length; k++) {
			Number[] row = { 
				wallBugExperimentMazeSizes[k],
				bugDetectionRateWithPlay[k],
				usedTurnsWithPlay[k],
				bugDetectionRateWithRandom[k],
				usedTurnsWithRandom[k],
				bugDetectionRateWithRandomPlus[k],
				usedTurnsWithRandomPlus[k]
			} ;
			data.add(row) ;
		}
		CSVUtility.exportToCSVfile(',', columnNames,data,filename) ;
	}
	
}
