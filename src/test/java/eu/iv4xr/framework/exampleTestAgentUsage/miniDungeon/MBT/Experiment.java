package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import static nl.uu.cs.aplib.AplibEDSL.*;

import org.junit.jupiter.api.Test;


import eu.iv4xr.framework.goalsAndTactics.AQalg;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

public class Experiment {
	
	//static MiniDungeonConfig mdconfig = Configs.miniMD() ;
	static boolean graphics = true ;
	static boolean sound = false ;
	
	// in ms
	static int delayBetweenUpdates = 0 ;
	
	// in sec
	static int randomTimeBudget = 180 ;
	static int QTimeBudget = 180 ;
	
	// ======================
	// running the random alg:
	// ======================
	
	public void runRandom(MiniDungeonConfig config) {
		MyRandomAlg.run_Random("Frodo",config,sound,graphics,delayBetweenUpdates,randomTimeBudget) ;
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
		var alg = MyQAlg.mkActionLevelQ(config, sound, graphics, delayBetweenUpdates) ;
		configureHyperParams(alg) ;
		alg.maxDepth = maxDepth ;
		var R = alg.runAlgorithm() ;
		System.out.println("** Q-alg on " + config.configname) ;
		System.out.println("** " + R.showShort()) ;	
	}
	
	@Test
	public void runQ() throws Exception {
		// choose which one to run; uncomment:
		runQ(MDConfigs.miniMD(),100) ;
		//runQ(MDConfigs.oneMazeStandardMD()) ;
		//runQ(MDConfigs.ML1()) ;
		//runQ(MDConfigs.ML2()) ;
		//runQ(MDConfigs.ML5()) ;
		//runQ(MDConfigs.ML10()) ;
	}
}
