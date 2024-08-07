package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import static eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ.TPJconfigs.* ;
import nl.uu.cs.aplib.utils.Pair;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;

import static java.nio.file.StandardOpenOption.APPEND;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import eu.iv4xr.framework.goalsAndTactics.BasicSearch;
import eu.iv4xr.framework.goalsAndTactics.BasicSearch.AlgorithmResult;
import eu.iv4xr.framework.goalsAndTactics.XEvolutionary;
import eu.iv4xr.framework.goalsAndTactics.XMCTS;
import eu.iv4xr.framework.goalsAndTactics.XQalg; 
import eu.iv4xr.framework.goalsAndTactics.AQalg; 


/**
 * Comparison of a number of automated testing algorithms and programatic
 * play-testing.
 */
public class Experiment4 {
	
	public enum AlgorithmType {
		RANDOM, Q, HIGH_RANDOM, HIGH_Q, HIGH_MCTS, PROGRAMMATIC, HIGH_EVO
	}
	
	MiniDungeonConfig[] smallDungeons = {
		SamiraLevel2()
		//Mini1(),
		//Mini2(),
		//Small1(),
		//Small2()
	} ;
	
	MiniDungeonConfig[] QevalDungeons = {
		MDQ1Config()
		//, MDQ2Config()
		//, MDQ3Config()
		//, MDQ4Config()
	} ;
		
	int numberOfRepeatedRuns = 1 ;
	
	boolean withGraphics = true ;
	boolean supressLogging = true ;
	// in ms.
	int delayBetweenAgentUpateCycles = 2 ;
	boolean saveRunData = true ;
    String runDataFolder = "./tmp/TPJ/experiment4" ;
    
        
    /**
     * For keeping the results of a single algorithm run.
     */
    public static class Result1 {
		public String algName ;
		public String runId ;
		public int runtime ;
		public int usedTurn ;
		public int numEpisodes ;
		public Integer numOfVisitedStates = null ;
		public boolean topGoalSolved ;
		public Double rewardOfExtractedSolution = null ;
		public boolean invViolationDetected ;
		
		@Override
		public String toString() {
			String z = "** Alg-name: " + algName ;
			z += "\nRun " + runId ;
			z += "\n#turn:" + usedTurn ;
			z += "\n#episodes:" + numEpisodes ;
			z += "\ntime:" + runtime ;
			z += "\n#visisted abs-states:" + (numOfVisitedStates==null ? " not tracked" : "" + numOfVisitedStates) ;
			z += "\ntop-goal solved:" + topGoalSolved ;
			z += "\nreward of extracted solution:" +
					(rewardOfExtractedSolution==null ? " not tracked" : rewardOfExtractedSolution);
			z += "\ninv-violation detected:" + invViolationDetected ;
			return z ;
		}
	}
	
    /**
     * Averaged results of multiple algorithm runs.
     */
	public static class ResultMultiRuns {
		public String algName ;
		public String benchMarkName ;
		public int numOfRuns ;
		public double avrgRuntime ;
		public double stddevRuntime ;
		public double avrgUsedTurn ;
		public double stddevUsedTurn ;
		public double avrgNumEpisodes ;
		public double stddevNumEpisodes ;
		public Double avrgNumOfVisitedStates = null ;
		public Double stddevNumOfVisitedStates = null ;
		public int topGoalSolved ;
		public int invViolationDetected ;
		public Double avrgReward = null ;
		public Double stddevReward = null ;
		
		double avrg(List<Integer> data) {
			return avrgDouble(data.stream().map(i -> (double) i).collect(Collectors.toList())) ;
		}
		
		double avrgDouble(List<Double> data) {
			return data.stream().collect(Collectors.averagingDouble(i -> i)) ;
		}
		
		double stdDev(List<Integer> data) {
			return stdDevDouble(data.stream().map(i -> (double) i).collect(Collectors.toList())) ;
		}
		
		double stdDevDouble(List<Double> data) {
			var m = avrgDouble(data) ;
			var z = data.stream().map(i -> {
						double d = i - m ;
						return d*d ; })
						.collect(Collectors.averagingDouble(i -> i)) ;
			return Math.sqrt(z) ;
		}
		
		void caculate(List<Result1> results) {
			numOfRuns = results.size() ;
			avrgRuntime = avrg(results.stream().map(R -> R.runtime).collect(Collectors.toList()));
			stddevRuntime = stdDev(results.stream().map(R -> R.runtime).collect(Collectors.toList())) ;
			avrgUsedTurn = avrg(results.stream().map(R -> R.usedTurn).collect(Collectors.toList())) ;
			stddevUsedTurn = stdDev(results.stream().map(R -> R.usedTurn).collect(Collectors.toList())) ;
			avrgNumEpisodes = avrg(results.stream().map(R -> R.numEpisodes).collect(Collectors.toList())) ;
			stddevNumEpisodes = stdDev(results.stream().map(R -> R.numEpisodes).collect(Collectors.toList())) ;
			if (results.get(0).numOfVisitedStates != null) {
				avrgNumOfVisitedStates = avrg(results.stream().map(R -> R.numOfVisitedStates).collect(Collectors.toList())) ;				
				stddevNumOfVisitedStates = stdDev(results.stream().map(R -> R.numOfVisitedStates).collect(Collectors.toList())) ;				
			}
			if (results.get(0).rewardOfExtractedSolution != null) {
				avrgReward = avrgDouble(results.stream().map(R -> R.rewardOfExtractedSolution).collect(Collectors.toList())) ;				
				stddevNumOfVisitedStates = stdDevDouble(results.stream().map(R -> R.rewardOfExtractedSolution).collect(Collectors.toList())) ;				
			}
			topGoalSolved = (int) results.stream().filter(r -> r.topGoalSolved).count() ;
			invViolationDetected = (int) results.stream().filter(r -> r.invViolationDetected).count() ;
		}
		
		@Override
		public String toString() {
			String z = "** Alg-name: " + algName  + " on " + benchMarkName ;
			z += "\n#run: " + numOfRuns ;
			z += "\n#turn:" + avrgUsedTurn + " (" + stddevUsedTurn + ")" ;
			z += "\n#episodes:" + avrgNumEpisodes + " (" + stddevNumEpisodes + ")" ;
			z += "\ntime:" + avrgRuntime + " (" + stddevRuntime + ")" ;
			z += "\n#visisted abs-states:" 
					+ (avrgNumOfVisitedStates==null ? 
							" not tracked" : 
							"" + avrgNumOfVisitedStates + " (" + stddevNumOfVisitedStates + ")") ;
			z += "\ntop-goal solved:" + topGoalSolved ;
			z += "\nreward:"   
					+ (avrgReward==null ? 
							" not tracked" : 
							"" + avrgReward + " (" + stddevReward + ")") ;
			z += "\ninv-violation detected:" + invViolationDetected ;
			return z ;
		}
	}
	
	//@Test
	void test_highrandom() throws Exception {
		runOneAlgorithm(AlgorithmType.HIGH_RANDOM,
				smallDungeons,
				config -> {
					var algFactory = new TestAlgorithmsFactory() ;
					algFactory.withGraphics = withGraphics ;
					algFactory.supressLogging = supressLogging ;
					algFactory.delayBetweenAgentUpateCycles = this.delayBetweenAgentUpateCycles ;
					BasicSearch alg = algFactory.mkBasicSearch(config) ;
					// hyper parameters:
					alg.totalSearchBudget = 60000 ;
					alg.maxDepth = 9 ;
					alg.maxNumberOfEpisodes = 10 ;
					return alg ;
				}
		) ;
	}
	
	//@Test
	void test_programmaticPlay() throws Exception {
		runOneAlgorithm(AlgorithmType.PROGRAMMATIC,
				smallDungeons,
				config -> {
					var algFactory = new TestAlgorithmsFactory() ;
					algFactory.withGraphics = withGraphics ;
					algFactory.supressLogging = supressLogging ;
					algFactory.delayBetweenAgentUpateCycles = this.delayBetweenAgentUpateCycles ;
					BasicSearch alg = algFactory.mkProgrammaticAlg(config) ;
					// hyper parameters:
					alg.totalSearchBudget = 60000 ;
					return alg ;
				}
		) ;
	}
	
	//@Test
	void test_lowrandom() throws Exception {
		runOneAlgorithm(AlgorithmType.RANDOM,
				smallDungeons,
				config -> {
					var algFactory = new TestAlgorithmsFactory() ;
					algFactory.withGraphics = withGraphics ;
					algFactory.supressLogging = supressLogging ;
					algFactory.delayBetweenAgentUpateCycles = this.delayBetweenAgentUpateCycles ;
					BasicSearch alg = algFactory.mkActionLevelRandomAlg(config) ;
					// hyper parameters:
					alg.totalSearchBudget = 60000 ;
					return alg ;
				}
		) ;
	}
	
	//@Test
	void test_Evo() throws Exception {
		runOneAlgorithm(AlgorithmType.HIGH_EVO,
				smallDungeons,
				config -> {
					var algFactory = new TestAlgorithmsFactory() ;
					algFactory.withGraphics = withGraphics ;
					algFactory.supressLogging = supressLogging ;
					algFactory.delayBetweenAgentUpateCycles = this.delayBetweenAgentUpateCycles ;
					XEvolutionary alg = algFactory.mkEvoSearch(config) ;
					// hyper parameters:
					alg.totalSearchBudget = 60000 ;
					alg.maxDepth = 9 ;
					//alg.maxNumberOfEpisodes = 40 ;
					alg.explorationBudget = 4000 ;
					alg.budget_per_task = 2000 ;
					alg.maxPopulationSize = 8 ;
					alg.numberOfElitesToKeepDuringSelection = 4 ;
					alg.insertionProbability = 0.9f ;
					alg.onlyExtendWithNewGene = false ;
					//alg.extendAtRandomInsertionPoint = false ;
					return alg ;
				}
		) ;
	}
	
	//@Test
	void test_MCTS() throws Exception {
		runOneAlgorithm(AlgorithmType.HIGH_MCTS,
				smallDungeons,
				config -> {
					var algFactory = new TestAlgorithmsFactory() ;
					algFactory.withGraphics = withGraphics ;
					algFactory.supressLogging = supressLogging ;
					algFactory.delayBetweenAgentUpateCycles = this.delayBetweenAgentUpateCycles ;
					XMCTS alg = algFactory.mkMCTS(config) ;
					// hyper parameters:
					//alg.totalSearchBudget = 60000 ;
					//alg.maxDepth = 9 ;
					alg.totalSearchBudget = 600000 ;
					alg.maxDepth = 18 ;
					//alg.maxNumberOfEpisodes = 100 ;
					alg.explorationBudget = 4000 ;
					alg.budget_per_task = 2000 ;					
					return alg ;
				}
		) ;
	}
	
	//@Test
	void test_highQ() throws Exception {
		runOneAlgorithm(AlgorithmType.HIGH_Q,
				smallDungeons,
				config -> {
					var algFactory = new TestAlgorithmsFactory() ;
					algFactory.withGraphics = withGraphics ;
					algFactory.supressLogging = supressLogging ;
					algFactory.delayBetweenAgentUpateCycles = this.delayBetweenAgentUpateCycles ;
					XQalg alg = algFactory.mkQ(config) ;
					// hyper parameters:
					alg.totalSearchBudget = 60000 ;
					//not setting exploreG --> surpress exploration:
					alg.exploredG = null ;
					alg.maxDepth = 12 ;
					//alg.maxNumberOfEpisodes = 40 ;
					alg.explorationBudget = 4000 ;
					alg.budget_per_task = 2000 ;
					//alg.exploreProbability = 0.15f ;
					alg.enableBackPropagationOfReward = 5 ; 				
					return alg ;
				}
		) ;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void test_lowQ() throws Exception {
	runOneAlgorithm(AlgorithmType.Q,
			QevalDungeons,
				config -> {
					var algFactory = new TestAlgorithmsFactory() ;
					algFactory.withGraphics = withGraphics ;
					algFactory.supressLogging = supressLogging ;
					algFactory.delayBetweenAgentUpateCycles = this.delayBetweenAgentUpateCycles ;
					AQalg alg = algFactory.mkActionLevelQ(config) ;
					
					// hyper parameters:
					alg.exploredG   = null ;
					alg.gamma = 0.98f ;
					alg.exploreProbability = 0.08f ;
					alg.enableBackPropagationOfReward = 10 ; 
					alg.totalSearchBudget = 1000000 ;
					alg.maxNumberOfEpisodes = 600 ;
					alg.maxDepth = 800 ;
					alg.stopAfterGoalIsAchieved = false ;					
					return alg ;
				}
		) ;
	}

	/**
	 * A runner to run an test algorithm (an instance of {@link BasicSearch}) to test MD.
	 * @param algTy
	 * @param targetDungeons A set of configs to create MD levels.
	 * @param algConstructor A constructor of {@link BasicSearch}.
	 */
	@SuppressWarnings("rawtypes")
	public void runOneAlgorithm(AlgorithmType algTy,
			MiniDungeonConfig[] targetDungeons, 
			Function<MiniDungeonConfig,BasicSearch> algConstructor
			) 	
		throws Exception 
	{
		
		String generalReportFile = "exper4_results.txt" ;
		fileAppendWriteLn(runDataFolder, generalReportFile,"=======");
		String algName = algTy.toString() ;
		for (int i=0; i < targetDungeons.length ; i++) {
			var config = (MiniDungeonConfig) targetDungeons[i] ;
			var bmName = config.configname ;
			var R = new ResultMultiRuns() ;
			R.algName = algName ;
			R.benchMarkName = bmName ;
			List<Result1> results = new LinkedList<>() ;
			for (int runNr=0; runNr<numberOfRepeatedRuns; runNr++) {
				BasicSearch alg = algConstructor.apply(config) ;
				System.out.println(">> START of run " + algName) ;
				AlgorithmResult V = alg.runAlgorithm()  ;
				System.out.println(">> END of run " + algName) ;
				Result1 result1 = new Result1() ;
				result1.algName = algName ;
				result1.runId = "" + runNr ;
				result1.usedTurn = V.usedTurns ;
				result1.numEpisodes = V.totEpisodes ;
				result1.runtime = V.usedBudget ;
				result1.topGoalSolved = V.goalAchieved ;
				result1.invViolationDetected = V.foundError ;
				if (algTy == AlgorithmType.HIGH_MCTS) {
					XMCTS alg_ = (XMCTS) alg ;
					var bestSequence = alg_.obtainBestPlay() ;
					result1.numOfVisitedStates = alg_.size() ;
					result1.rewardOfExtractedSolution = (double) ((float) bestSequence.snd) ;
					System.out.println(">>> episode-values: " + V.episodesValues) ;
					System.out.println(">>> winningplay : " + V.winningplay) ;
					System.out.println(">>> best sequence: " + bestSequence) ;
				}
				if (algTy == AlgorithmType.HIGH_Q) {
					XQalg alg_ = (XQalg) alg ;
					var bestSequence = alg_.play(alg_.maxDepth) ;
					result1.numOfVisitedStates = alg_.qtable.size() ;
					result1.rewardOfExtractedSolution =  (double) ((float) bestSequence.snd);
					System.out.println(">>> episode-values: " + V.episodesValues) ;
					System.out.println(">>> winningplay : " + V.winningplay) ;
					System.out.println(">>> best sequence): " + bestSequence) ;
				}
				if (algTy == AlgorithmType.Q) {
					AQalg alg_ = (AQalg) alg ;
					var bestSequence = alg_.play(alg_.maxDepth) ;
					result1.numOfVisitedStates = alg_.qtable.size() ;
					result1.rewardOfExtractedSolution = (double) ((float) bestSequence.snd) ;
					System.out.println(">>> episode-values: " + V.episodesValues) ;
					System.out.println(">>> winningplay : " + V.winningplay) ;
					System.out.println(">>> best sequence: " + bestSequence) ;
				}
				results.add(result1) ;
			}
			R.caculate(results);
			System.out.println(R.toString()) ;
			
			// saving reports to files:
			String reportFileDetailed = "exper4_" + bmName + "_" + R.algName + ".txt";
			for (var r : results) {
				fileAppendWriteLn(runDataFolder,reportFileDetailed,r.toString());
			}
			fileAppendWriteLn(runDataFolder, generalReportFile,R.toString());
		}
			
	}
	
	void fileAppendWriteLn(String dir, String fname, String str) throws IOException {
		Files.writeString(
		        Path.of(dir, fname),
		        str + "\n",
		        CREATE, APPEND
		    );
	}

}
