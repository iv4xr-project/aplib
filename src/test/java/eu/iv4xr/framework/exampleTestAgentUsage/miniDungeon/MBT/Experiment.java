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
	
	// in sec
	static int randomTimeBudget = 180 ;
	static int QTimeBudget = 180 ;
	
	// in #turns
	static int MD_taskBudget = 200 ;
	
	// ======================
	// running the random alg:
	// ======================
	
	public void runRandom(MiniDungeonConfig config) {
		MyRandomAlg.run_Random("Frodo",config,sound,graphics,delayBetweenUpdates(),randomTimeBudget) ;
	}
	
	
	//@Test
	public void runRandom() {
		// choose which one to run; uncomment:
		//runRandom(MDConfigs.miniMD()) ;
		//runRandom(MDConfigs.oneMazeStandardMD()) ;
		runRandom(MDConfigs.ML1()) ;
		//runRandom(MDConfigs.ML2()) ;
		//runRandom(MDConfigs.ML5()) ;
		//runRandom(MDConfigs.ML10()) ;
	}
	
	// ======================
	// running the Q alg
	// ======================
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void configureHyperParams(AQalg alg) {
		alg.gamma = 0.993f ;
		alg.exploreProbability = 0.15f ;
        alg.enableBackPropagationOfReward = 10 ; 
		alg.totalSearchBudget = QTimeBudget * 1000 ;
		alg.maxNumberOfEpisodes = 10000 ;
		alg.maxDepth = 800 ;
		alg.stopAfterGoalIsAchieved = false ;	
		alg.progressSamplingInterval = 10 ;
	}
	
	public void runQ(MiniDungeonConfig config, int maxDepth) throws Exception {
		var alg = MyQAlg.mkActionLevelQ(config, sound, graphics, delayBetweenUpdates()) ;
		configureHyperParams(alg) ;
		alg.maxDepth = maxDepth ;
		var R = alg.runAlgorithm() ;
		System.out.println("** Q-alg on " + config.configname) ;
		System.out.println("** " + R.showShort()) ;	
	}
	
	//@Test
	public void runQ() throws Exception {
		// choose which one to run; uncomment:
		//runQ(MDConfigs.miniMD(),100) ;
		//runQ(MDConfigs.oneMazeStandardMD()) ;
		//runQ(MDConfigs.ML1(),200) ;
		runQ(MDConfigs.ML2(),300) ;
		//runQ(MDConfigs.ML5()) ;
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
		
		
		var mymodel = MBT_MD_Model.MD_model1(MD_taskBudget, withSmartGotoNextMaze, withSurvival);
		var runner = new MBTRunner<MyAgentState>(mymodel);
		runner.inferTransitions = true;
		// runner.rnd = new Random() ;

		runner.actionSelectionPolicy = heuristic ;
		runner.stopSuiteGenerationOnFailedOrViolation = false ;

		var t0 = System.currentTimeMillis() ;
		var results = runner.generate(dummy -> MDRelauncher.agentRestart("Frodo", config, sound, graphics), 
						suiteSize, 
						testSequenceLength);
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
	
	@Test
	void runBaseMBT() {
		//runMBT(MDConfigs.miniMD(),ACTION_SELECTION.RANDOM, 40, 60, false, true) ;
		//runMBT(MDConfigs.oneMazeStandardMD(),ACTION_SELECTION.RANDOM, 20, 60, false, false) ;
		//runMBT(MDConfigs.ML1(),ACTION_SELECTION.RANDOM,  30, 60, false, true) ;
		//runMBT(MDConfigs.ML2(),ACTION_SELECTION.RANDOM,  30, 60, false, false) ;
		//runMBT(MDConfigs.ML5(),ACTION_SELECTION.RANDOM,  60, 60, false, false) ;
		runMBT(MDConfigs.ML10(),ACTION_SELECTION.RANDOM, 110, 60, true, true) ;
	}
}
