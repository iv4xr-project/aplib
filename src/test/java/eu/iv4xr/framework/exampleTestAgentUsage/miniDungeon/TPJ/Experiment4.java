package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
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

/**
 * Comparison of a number of automated testing algorithms and programatic
 * play-testing.
 */
public class Experiment4 {
	
	enum AlgorithmType {
		RANDOM, Q, HIGH_RANDOM, HIGH_Q, HIGH_MCTS, PROGRAMMATIC
	}
	
	Pair[] SmallDungeons_MDconfigs = {
		new Pair<>(TPJconfigs.MDSmallconfig0(),"mini-1"),
		new Pair<>(TPJconfigs.MDSmallconfig1(),"mini-2"),
		new Pair<>(TPJconfigs.MDSmallconfig2(),"small-1"),
		new Pair<>(TPJconfigs.MDSmallconfig3(),"small-2"),
	} ;
		
	int numberOfRepeatedRuns = 3 ;
	
	boolean withGraphics = true ;
	boolean supressLogging = true ;
	boolean saveRunData = true ;
    String runDataFolder = "./tmp" ;
    
        
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
		
		double avrg(List<Integer> data) {
			return data.stream().collect(Collectors.averagingDouble(i -> (double) i)) ;
		}
		
		double stdDev(List<Integer> data) {
			var m = avrg(data) ;
			var z = data.stream().map(i -> {
						double d = (double) i - m ;
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
							"not tracked" : 
							"" + avrgNumOfVisitedStates + " (" + stddevNumOfVisitedStates + ")") ;
			z += "\ntop-goal solved:" + topGoalSolved ;
			z += "\ninv-violation detected:" + invViolationDetected ;
			return z ;
		}
	}
	
	@Test
	void test_highrandom() throws Exception {
		runOneAlgorithm(AlgorithmType.HIGH_RANDOM,
				SmallDungeons_MDconfigs,
				config -> {
					var algFactory = new TestAlgorithmsFactory() ;
					algFactory.withGraphics = withGraphics ;
					algFactory.supressLogging = supressLogging ;
					BasicSearch alg = algFactory.mkBasicSearch(config) ;
					// hyper parameters:
					alg.totalSearchBudget = 60000 ;
					alg.maxDepth = 9 ;
					alg.maxNumberOfEpisodes = 10 ;
					alg.delayBetweenAgentUpateCycles = 10 ;
					return alg ;
				}
		) ;
	}

	@SuppressWarnings("rawtypes")
	void runOneAlgorithm(AlgorithmType algTy,
			Pair[] targetLevels, // pairs of MD-config, and a string-name for it
			Function<MiniDungeonConfig,BasicSearch> algConstructor
			) 	
		throws Exception 
	{
		
		String generalReportFile = "exper4_results.txt" ;
		fileAppendWriteLn(runDataFolder, generalReportFile,"=======");
		String algName = algTy.toString() ;
		for (int i=0; i < targetLevels.length ; i++) {
			var config = (MiniDungeonConfig) targetLevels[i].fst ;
			var bmName = (String) targetLevels[i].snd ;
			var R = new ResultMultiRuns() ;
			R.algName = algName ;
			R.benchMarkName = bmName ;
			List<Result1> results = new LinkedList<>() ;
			for (int runNr=0; runNr<numberOfRepeatedRuns; runNr++) {
				var algFactory = new TestAlgorithmsFactory() ;
				algFactory.withGraphics = withGraphics ;
				algFactory.supressLogging = supressLogging ;
				BasicSearch alg = algFactory.mkBasicSearch(config) ;
				// hyper parameters:
				alg.totalSearchBudget = 60000 ;
				alg.maxDepth = 9 ;
				alg.maxNumberOfEpisodes = 10 ;
				alg.delayBetweenAgentUpateCycles = 10 ;
				
				System.out.println(">> START of run " + algName) ;
				alg.runAlgorithm()  ;
				System.out.println(">> END of run " + algName) ;
				Result1 result1 = new Result1() ;
				result1.algName = algName ;
				result1.runId = "" + runNr ;
				result1.usedTurn = alg.turn ;
				result1.numEpisodes = alg.totNumberOfEpisodes ;
				result1.runtime = alg.totalSearchBudget - alg.getRemainingSearchBudget() ;
				result1.topGoalSolved = alg.goalHasBeenAchieved() ;
				// TODO:
				// R.invViolationDetected = ....
				
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
