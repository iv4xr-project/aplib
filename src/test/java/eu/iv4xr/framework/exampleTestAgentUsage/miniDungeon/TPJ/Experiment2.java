package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.CSVUtility;

/**
 * This experiment looks into the survivability of the test agent using
 * survival tactic, and compares it with runs without survival tactic,
 * and runs with random.
 * 
 * @author Wish
 */
class Experiment2 {
	
	boolean saveRunData = false ;
	String dataFolder = "./tmp/TPJ/experiment2" ;

	boolean withGraphics = false ;
	boolean verbosePrint = false ;
	int sleepBetweenUpdates = 5 ;

	int testbudget = 2000 ;
	
	void runTest(String runId, TestAgent agent1,
			MiniDungeonConfig config,
			GoalStructure G1, 
			boolean assertG1IsFinished,
			boolean assertLTLs) throws Exception {
		
		var runner = new MDTestRunner() ;
		runner.runDataFolder = dataFolder ;
		runner.withGraphics = withGraphics ;
		runner.verbosePrint = verbosePrint ;
		runner.saveRunData = saveRunData ;
		
		runner.runAgent(runId, agent1, null, config, G1, null, testbudget, sleepBetweenUpdates);
		
		// checking oracles:
		if (assertG1IsFinished) {
			assertTrue(G1.getStatus().success(),"G1 check, " + runId) ;	
		}
		if (assertLTLs) {
			assertTrue(agent1.evaluateLTLs(),"LTLs check, " + runId) ;		
		}
	}
	
	@Disabled
	@Test
	void view() throws Exception {
		MiniDungeonConfig config =  TPJconfigs.BigAreaConfig() ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 20 ;
		config.numberOfHealPots = (int) (1.8f *config.numberOfMonsters) ;
		config.viewDistance = 100 ;
		config.randomSeed = new Random() .nextLong() ;
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		DungeonApp.deploy(app);
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine() ;
	}
	
	//@Test
	void survival() throws Exception {
		// run only one test for wall:
		MiniDungeonConfig config =  TPJconfigs.BigAreaConfig() ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 30 ;
		config.numberOfHealPots = (int) (1.8f *config.numberOfMonsters) ;
		var frodo = Experiment1.mkFrodo() ;
		var play = new ShrineCleanTester() ;
		play.tacticLib.delayPathReplan = true ;
		play.goalLib.tacticLib.delayPathReplan = true ;
		//play.useSurvivalTactic = false ;
		runTest("survivalTest_M" + config.numberOfMonsters,
				frodo, 
				config,
				play.cleanseAllShrines(frodo, config.numberOfMaze),
				true,true) ;		
	}
	
	int[] numberOfMonsters = { 5, 10, 15, 20, 25, 30, 35, 40 } ;
	//int[] numberOfMonsters = { 5, 10} ;
	int numberOfReruns = 10 ;
	
	float[] survivalRateWithSurvivalTactics = new float[numberOfMonsters.length] ;
	float[] survivalRateWithoutSurvivalTactics = new float[numberOfMonsters.length] ;
	float[] survivalRateRandom = new float[numberOfMonsters.length] ;
	
	@Disabled
	@Test
	void survivalExperiment() throws Exception {
		
		Random seed = new Random() ;
		
		for (int k=0; k<numberOfMonsters.length; k++) {
			int M = numberOfMonsters[k] ;

			survivalRateWithSurvivalTactics[k] = 0 ;
			survivalRateWithoutSurvivalTactics[k] = 0 ;
			survivalRateRandom[k] = 0 ;
			
			MiniDungeonConfig config =  TPJconfigs.BigAreaConfig() ;
			config.enableSmeagol = false ;
			config.numberOfMonsters = M ;
			config.numberOfHealPots = (int) (1.5f *config.numberOfMonsters) ;
			System.out.println(">>>> Starting M" + M) ;
			
			
			for (int j=0; j<numberOfReruns; j++) {
				System.out.println(">>>> M" + M + ", run " + j) ;
				
				// ==== (1) with survival-tactic:
				config.randomSeed = seed.nextLong() ;
				var frodo = Experiment1.mkFrodo() ;
				var play = new ShrineCleanTester() ;
				String status = "" ;
				play.setToUseMemorizedPathFinding();
				//play.useSurvivalTactic = false ;
				runTest("survivalTest_M" + M + "_withSurvivalTactic",
						frodo, 
						config,
						play.cleanseAllShrines(frodo, config.numberOfMaze),
						false,true) ;
				// check if the agent survive
				if (((MyAgentState)frodo.state()).agentIsAlive()) {
					survivalRateWithSurvivalTactics[k] += 1 ;
					status += "S " ;
				}
				else status += "F " ;
				// ==== (2) without survival-tactic:
				frodo = Experiment1.mkFrodo() ;
				play = new ShrineCleanTester() ;
				play.tacticLib.delayPathReplan = true ;
				play.goalLib.tacticLib.delayPathReplan = true ;
				play.useSurvivalTactic = false ;
				runTest("survivalTest_M" + M + "_withoutSurvivalTactic",
						frodo, 
						config,
						play.cleanseAllShrines(frodo, config.numberOfMaze),
						false,true) ;
				// check if the agent survive
				if (((MyAgentState)frodo.state()).agentIsAlive()) {
					survivalRateWithoutSurvivalTactics[k] += 1 ;
					status += "S " ;
				}
				else status += "F " ;
				// ==== (3) with random:
				frodo = Experiment1.mkFrodo() ;
				RandomPlayTester randomplay = new RandomPlayTester() ;
				runTest("survivalTest_M" + M + "_random",
						frodo, 
						config,
						randomplay.simpleRandomPlay(),
						false,true) ;
				// check if the agent survive
				if (((MyAgentState)frodo.state()).agentIsAlive()) {
					survivalRateRandom[k] += 1 ;
					status += "S " ;
				}
				else status += "F " ;
				System.out.println(">>>> M" + M + "_" + j + ":" + status) ;
			}
			survivalRateWithSurvivalTactics[k] 
					= survivalRateWithSurvivalTactics[k] / (float) numberOfReruns ;
			survivalRateWithoutSurvivalTactics[k]
					= survivalRateWithoutSurvivalTactics[k] / (float) numberOfReruns ;
			survivalRateRandom[k]
					= survivalRateRandom[k] / (float) numberOfReruns ;			
		}
		
		printResults() ;
		saveResults() ;
	}
	
	
	void printResults() {
		System.out.println("====") ;
		System.out.print("With Survival-tactic: ") ;
		for (int k=0; k<numberOfMonsters.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" + survivalRateWithSurvivalTactics[k]) ;
		}
		System.out.print("\nWITHOUT Survival-tactic: ") ;
		for (int k=0; k<numberOfMonsters.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" + survivalRateWithoutSurvivalTactics[k]) ;
		}
		System.out.print("\nRandom: ") ;
		for (int k=0; k<numberOfMonsters.length; k++) {
			if (k>0) System.out.print(", ") ;
			System.out.print("" + survivalRateRandom[k]) ;
		}
		System.out.println("\n====") ;
	}
	
	void saveResults() throws IOException {
		String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new java.util.Date());
		String filename = dataFolder
				+ File.separator
				+ "experiment2_" + timestamp + ".csv" ;
		String[] columnNames = { "#monsters","withSurvival", "WITHOUTSurvival", "random" } ;
		List<Number[]> data = new LinkedList<Number[]>() ;
		for (int k=0; k<numberOfMonsters.length; k++) {
			Number[] row = { 
				numberOfMonsters[k],
				survivalRateWithSurvivalTactics[k],
				survivalRateWithoutSurvivalTactics[k],
				survivalRateRandom[k]
			} ;
			data.add(row) ;
		}
		CSVUtility.exportToCSVfile(',', columnNames,data,filename) ;
	}
	
}
