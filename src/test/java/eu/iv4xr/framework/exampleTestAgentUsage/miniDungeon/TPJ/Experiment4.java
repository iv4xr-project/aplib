package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.utils.Pair;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.APPEND;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ.BasicSearchAlgForMD.Result1; 

/**
 * Comparison of a number of automated testing algorithms and programatic
 * play-testing.
 */
public class Experiment4 {
	
	Pair[] MDconfigs = {
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
		
		String generalReportFile = "exper4_results.txt" ;
		
		for (int i=0; i < MDconfigs.length ; i++) {
			var config = (MiniDungeonConfig) MDconfigs[i].fst ;
			var bmName = (String) MDconfigs[i].snd ;
			var R = new ResultMultiRuns() ;
			R.benchMarkName = bmName ;
			List<Result1> results = new LinkedList<>() ;
			for (int runNr=0; runNr<numberOfRepeatedRuns; runNr++) {
				var alg = new BasicSearchAlgForMD() ;
				if (runNr==0) {
					R.algName = alg.algName ;
				}
				alg.withGraphics = withGraphics ;
				alg.supressLogging = supressLogging ;
				alg.saveRunData = saveRunData ;
				alg.saveRunData = false ;
				alg.runDataFolder = runDataFolder ;
				alg.basicConfigure(config);
				alg.alg.totalSearchBudget = 60000 ;
				alg.alg.maxDepth = 9 ;
				alg.alg.maxNumberOfEpisodes = 10 ;
				alg.alg.delayBetweenAgentUpateCycles = 10 ;
				var result1 = alg.runAlgorithm("" + bmName + "_" + runNr) ;
				results.add(result1) ;
			}
			R.caculate(results);
			System.out.println(R.toString()) ;
			
			// saving reports to files:
			String reportFileDetailed = "exper4_" + bmName + "_" + R.algName + ".txt";
			for (var r : results) {
				Files.writeString(
				        Path.of(runDataFolder, reportFileDetailed),
				        "\n" + r.toString(),
				        CREATE, APPEND
				    );
			}
			Files.writeString(
			        Path.of(runDataFolder, generalReportFile),
			        "\n" + R.toString(),
			        CREATE, APPEND
			    );
		}
			
	}

}
