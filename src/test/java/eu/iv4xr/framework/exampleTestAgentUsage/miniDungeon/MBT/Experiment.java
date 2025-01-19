package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import static nl.uu.cs.aplib.AplibEDSL.*;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.mbt.MBTRunner;
import eu.iv4xr.framework.extensions.mbt.MBTRunner.ACTION_SELECTION;
import eu.iv4xr.framework.goalsAndTactics.AQalg;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;

public class Experiment {
	
	//static MiniDungeonConfig mdconfig = Configs.miniMD() ;
	static boolean graphics = false ;
	static boolean sound = false ;
	
	// in ms
	static int delayBetweenUpdates() { return ExtraGoalLib.DELAY_BETWEEN_UPDATE ; } 
	
	/**
	 * Time budget; same for every algorithm, two minutes per number of maze.
	 */
	static int timeBudget(MiniDungeonConfig config) {
		return 2 * config.numberOfMaze * 60 ;
	}
	
	// in #turns
	static int MBT_taskBudget = 500 ;
	
	// ======================
	// running the random alg:
	// ======================
	
	public void runRandom(MiniDungeonConfig config) {
		MyRandomAlg.run_Random("Frodo",config,sound,graphics,
				timeBudget(config),
				delayBetweenUpdates()) ;
	}
	
	
	//@Test
	public void runRandom() {
		// choose which one to run; uncomment:
		//runRandom(MDConfigs.miniMD()) ;
		//runRandom(MDConfigs.oneMazeStandardMD()) ;
		//runRandom(MDConfigs.ML1()) ;
		//runRandom(MDConfigs.ML2()) ;
		runRandom(MDConfigs.ML5()) ;
		//runRandom(MDConfigs.ML10()) ;
	}
	
	// ======================
	// running the Q alg
	// ======================
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void configureHyperParams(MiniDungeonConfig config, AQalg alg) {
		alg.gamma = 0.993f ;
		alg.exploreProbability = 0.15f ;
        alg.enableBackPropagationOfReward = 10 ; 
		alg.totalSearchBudget = timeBudget(config) * 1000 ;
		alg.maxNumberOfEpisodes = 10000 ;
		alg.maxDepth = 800 ;
		alg.stopAfterGoalIsAchieved = false ;	
		alg.progressSamplingInterval = 10 ;
	}
	
	public void runQ(MiniDungeonConfig config, int maxDepth) throws Exception {
		var alg = MyQAlg.mkActionLevelQ(config, sound, graphics, delayBetweenUpdates()) ;
		configureHyperParams(config,alg) ;
		alg.maxDepth = maxDepth ;
		var R = alg.runAlgorithm() ;
		System.out.println("** Q-alg on " + config.configname) ;
		System.out.println("** " + R.showShort()) ;	
	}
	
	//@Test
	public void runQ() throws Exception {
		// choose which one to run; uncomment:
		//runQ(MDConfigs.miniMD(),400) ;
		//runQ(MDConfigs.oneMazeStandardMD(),400) ;
		//runQ(MDConfigs.ML1(),800) ;
		//runQ(MDConfigs.ML2(),1000) ;
		runQ(MDConfigs.ML5(),2000) ;
		//runQ(MDConfigs.ML10()) ;
	}
	
	// ======================
	// running MBT
	// ======================
	
	public void runMBT(MiniDungeonConfig config, 
			ACTION_SELECTION heuristic,
			int suiteSize,
			int testSequenceLength,
			boolean withSmartGotoNextMaze, 
			boolean withSurvival) {
		
		
		var mymodel = MBT_MD_Model.MD_model1(MBT_taskBudget, withSmartGotoNextMaze, withSurvival);
		var runner = new MBTRunner<MyAgentState>(mymodel);
		runner.inferTransitions = true;
		// runner.rnd = new Random() ;

		runner.actionSelectionPolicy = heuristic ;
		runner.stopSuiteGenerationOnFailedOrViolation = false ;
		runner.isGameOver = S -> ! S.agentIsAlive() || MDAbstraction.gameover(S) ;
		runner.additionalStepsAfterGameOver = 5 ;

		var t0 = System.currentTimeMillis() ;
		var results = runner.generate(dummy -> MDRelauncher.agentRestart("Frodo", config, sound, graphics), 
						suiteSize, 
						testSequenceLength,
						timeBudget(config) * 1000
						);
		int runtime = (int) ((System.currentTimeMillis() - t0)/1000) ;
		
		String mbt_type = "MBT (" + heuristic ;
		if (withSmartGotoNextMaze)
			mbt_type += ", with-smart-goto-next-maze" ;
		if (withSurvival)
			mbt_type += ", with-survival" ;
		mbt_type += ")" ;	
		
		System.out.println("** " + mbt_type + " on " + config.configname) ;
		System.out.println("** runtime: " + runtime + " s") ;
		System.out.println(runner.showCoverage(false));
		System.out.println("** failed actions:" + MBTRunner.getFailedActionsFromSuiteResults(results));
		System.out.println("** postcond violations:" + MBTRunner.getViolatedPostCondsFromSuiteResults(results));
	}
	
	//@Test
	void runBaseMBT() {
		//runMBT(MDConfigs.miniMD(),ACTION_SELECTION.RANDOM, 200, 30, false, false) ;
		//runMBT(MDConfigs.oneMazeStandardMD(),ACTION_SELECTION.RANDOM, 200, 30, false, false) ;
		//runMBT(MDConfigs.ML1(),ACTION_SELECTION.RANDOM,  200, 30, false, false) ;
		//runMBT(MDConfigs.ML2(),ACTION_SELECTION.RANDOM,  200, 40, false, false) ;
		runMBT(MDConfigs.ML5(),ACTION_SELECTION.RANDOM,  200, 90, false, false) ;
		//runMBT(MDConfigs.ML10(),ACTION_SELECTION.RANDOM, 200, 90, false, false) ;
	}
	
	@Test
	void runSmartMBT() {
		//runMBT(MDConfigs.miniMD(),ACTION_SELECTION.RANDOM, 200, 30, true, true) ;
		//runMBT(MDConfigs.oneMazeStandardMD(),ACTION_SELECTION.RANDOM, 200, 30, true, true) ;
		//runMBT(MDConfigs.ML1(),ACTION_SELECTION.RANDOM,  200, 30, true, true) ;
		//runMBT(MDConfigs.ML2(),ACTION_SELECTION.RANDOM,  200, 40, true, true) ;
		runMBT(MDConfigs.ML5(),ACTION_SELECTION.RANDOM,  200, 70, true, true) ;
		//runMBT(MDConfigs.ML10(),ACTION_SELECTION.RANDOM, 200, 90, true, true) ;
	}
	
	
}
