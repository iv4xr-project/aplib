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
	int[] mazeSizes = { 21, 31, 41, 51, 61 } ; 
	//int[] mazeSizes = { 21 } ; 
	//int[] mazeSizes = { 61 } ; 
	
	float[] bugDetectionRateWithPlay = new float[mazeSizes.length] ;
	float[] bugDetectionRateWithRandom = new float[mazeSizes.length] ;
	float[] bugDetectionRateWithRandomPlus = new float[mazeSizes.length] ;
	float[] usedTurnsWithPlay = new float[mazeSizes.length] ;
	float[] usedTurnsWithRandom = new float[mazeSizes.length] ;
	float[] usedTurnsWithRandomPlus = new float[mazeSizes.length] ;
	int numberOfReruns = 10 ;
	
	// change the seed of the config, until one is found that
	// would generate a maze with broken wall
	boolean findSeedWithBrokenWall(MiniDungeonConfig config, int maxNumberOfAttempts) {
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
		
		Random seed = new Random() ;
		
		for (int k=0; k<mazeSizes.length; k++) {
			int N = mazeSizes[k] ;
			bugDetectionRateWithPlay[k] = 0 ;
			bugDetectionRateWithRandom[k] = 0 ;
			bugDetectionRateWithRandomPlus[k] = 0 ;
			
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
			
			
			var play = new ShrineCleanTester() ;
			String status = "" ;
			play.tacticLib.delayPathReplan = true ;
			play.goalLib.tacticLib.delayPathReplan = true ;
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
				// check if the agent survive
				if (!ok) {
					bugDetectionRateWithRandom[k] += 1 ;
					usedTurnsWithRandom[k] += (Integer) ((MyAgentState) frodo.state()).val("aux","turn") ;
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
					status += "S " ;
				}
				else {
					status += "F " ;
					usedTurnsWithRandomPlus[k] += 10000 ; 
				}
				System.out.println(">>>> N" + N + "_" + j + ":" + status) ;
			}
			

			bugDetectionRateWithRandom[k] 
					= bugDetectionRateWithRandom[k] / (float) numberOfReruns ;
			usedTurnsWithRandom[k] = usedTurnsWithRandom[k] / (float) numberOfReruns ;
			bugDetectionRateWithRandomPlus[k]
					= bugDetectionRateWithRandomPlus[k] / (float) numberOfReruns ;
			usedTurnsWithRandomPlus[k] = usedTurnsWithRandomPlus[k] / (float) numberOfReruns ;
					
		}
		printResults() ;
		saveResults("wall") ;
	}
	
	void findingHealingPotionBugExperiment() throws Exception {
		
	}
	
	void printResults() {
		System.out.println("====") ;
		System.out.print("With programmed-play: ") ;
		for (int k=0; k<mazeSizes.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" 
					+ bugDetectionRateWithPlay[k] 
					+ " (#turns:" + usedTurnsWithPlay[k] +  ")") ;
		}
		System.out.print("\nRandom: ") ;
		for (int k=0; k<mazeSizes.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" 
					+ bugDetectionRateWithRandom[k]
					+ " (#turns:" + usedTurnsWithRandom[k] +  ")") ;
		}
		System.out.print("\nSmart-random: ") ;
		for (int k=0; k<mazeSizes.length; k++) {
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
		for (int k=0; k<mazeSizes.length; k++) {
			Number[] row = { 
				mazeSizes[k],
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
