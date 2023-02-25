package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.CSVUtility;

class Experiment3 {

	boolean saveRunData = false ;
	String dataFolder = "./tmp/TPJ/experiment3" ;

	boolean withGraphics = true ;
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
		
		runner.runAgent(runId, agent1, null, config, G1, null, testbudget, sleepBetweenUpdates);
		
		// checking oracles:

		return agent1.evaluateLTLs() ;
	}
	
	
	//int[] mazeSizes = { 20, 25, 30, 35, 40 } ; 
	int[] mazeSizes = { 20, 25 } ; 
	float[] buggyWallDetectionRateWithPlay = new float[mazeSizes.length] ;
	float[] buggyWallDetectionRateWithRandom = new float[mazeSizes.length] ;
	float[] buggyWallDetectionRateWithRandomPlus = new float[mazeSizes.length] ;
	int numberOfReruns = 3 ;
	
	@Test
	void wallExperiment() throws Exception {
		
		Random seed = new Random() ;
		
		for (int k=0; k<mazeSizes.length; k++) {
			int N = mazeSizes[k] ;
			int numberOfSuccess = 0 ;
			buggyWallDetectionRateWithPlay[k] = 0 ;
			buggyWallDetectionRateWithRandom[k] = 0 ;
			buggyWallDetectionRateWithRandomPlus[k] = 0 ;
			
			MiniDungeonConfig config =  TPJconfigs.MDconfig1() ;
			config.enableSmeagol = false ;
			config.numberOfMonsters = 1 ;
			config.numberOfMaze = 1 ;
			config.numberOfCorridors = 3 ;
			config.viewDistance = 6 ;
			config.worldSize = N ;
			config.numberOfHealPots = (int) (1.5f *config.numberOfMonsters) ;
			System.out.println(">>>> Starting N" + N) ;
			
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
				buggyWallDetectionRateWithPlay[k] = 1 ;
				status += "S " ;
				System.out.println(">>> WALL BUG FOUND!") ;
			}
			else {
				status += "F " ;
			}
			
			for (int j=0; j<numberOfReruns; j++) {
				System.out.println(">>>> N" + N + ", run " + j) ;
				// ==== (2) with random:
				frodo = Experiment1.mkFrodo() ;
				config.randomSeed = seed.nextLong() ;
				RandomPlayTester randomplay = new RandomPlayTester() ;
				ok = runTest("survivalTest_N" + N + "_random",
						frodo, 
						config,
						randomplay.simpleRandomPlay(),
						true) ;
				// check if the agent survive
				if (!ok) {
					buggyWallDetectionRateWithRandom[k] += 1 ;
					status += "S " ;
				}
				else status += "F " ;
				// ==== (3) with random plus:
				ok = runTest("survivalTest_N" + N + "_random",
						frodo, 
						config,
						randomplay.smarterRandomPlay(frodo),
						true) ;
				if (!ok) {
					buggyWallDetectionRateWithRandomPlus[k] += 1 ;
					status += "S " ;
				}
				else status += "F " ;
				System.out.println(">>>> N" + N + "_" + j + ":" + status) ;
			}
			buggyWallDetectionRateWithRandom[k] 
					= buggyWallDetectionRateWithRandom[k] / (float) numberOfReruns ;
			buggyWallDetectionRateWithRandomPlus[k]
					= buggyWallDetectionRateWithRandomPlus[k] / (float) numberOfReruns ;			
					
		}
		printResults() ;
		saveResults() ;
	}
	
	void printResults() {
		System.out.println("====") ;
		System.out.print("With wall-play: ") ;
		for (int k=0; k<mazeSizes.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" + buggyWallDetectionRateWithPlay[k]) ;
		}
		System.out.print("\nRandom: ") ;
		for (int k=0; k<mazeSizes.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" + buggyWallDetectionRateWithRandom[k]) ;
		}
		System.out.print("\nSmart-random: ") ;
		for (int k=0; k<mazeSizes.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" + buggyWallDetectionRateWithRandomPlus[k]) ;
		}
		System.out.println("\n====") ;
	}
	
	void saveResults() throws IOException {
		String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new java.util.Date());
		String filename = dataFolder
				+ File.separator
				+ "experiment3_wall_" + timestamp + ".csv" ;
		String[] columnNames = { "maze-size","with-wall-play", "random", "smart-random" } ;
		List<Number[]> data = new LinkedList<Number[]>() ;
		for (int k=0; k<mazeSizes.length; k++) {
			Number[] row = { 
				mazeSizes[k],
				buggyWallDetectionRateWithPlay[k],
				buggyWallDetectionRateWithRandom[k],
				buggyWallDetectionRateWithRandomPlus[k]
			} ;
			data.add(row) ;
		}
		CSVUtility.exportToCSVfile(',', columnNames,data,filename) ;
	}
	
}
