package nl.uu.cs.aplib.iv4xr.verification;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.Agents.AutonomousBasicAgent;

public class FunctionalTestAgent extends AutonomousBasicAgent {
	
	String testDesc ;
	
	public static class Coverage {
		Map<String,Integer> targets = new HashMap<String,Integer>() ;
		
		public Coverage() { }
		
		public void addTarget(String targetName) {
			targets.put(targetName,0) ;
		}
		
		public void registerVisit(String targetName) {
			if (! targets.containsKey(targetName)) {
				targets.put(targetName,1) ;
			}
			else {
				int k = targets.get(targetName) ;
				targets.put(targetName,k+1) ;
			}
		}
		
		public Map<String,Integer> getCoverageMap() { return targets ; }
		public List<String> getCovered() {
			return targets.entrySet()
			   .stream()
			   .filter(entry -> entry.getValue()>0) 
			   .map(entry -> entry.getKey())
			   .collect(Collectors.toList())
			   ;
		}
		
		public List<String> getUncovered() {
			return targets.entrySet()
					   .stream()
					   .filter(entry -> entry.getValue()<=0) 
					   .map(entry -> entry.getKey())
					   .collect(Collectors.toList())
					   ;			
		}
	}
	
	public static class Verdicts {
		List<Entry<String,Object>> verdicts = new LinkedList<Entry<String,Object>>() ;
		int failCount = 0 ;
		
		public void registerSuccess(String verdictname, Object o) {
			if (o instanceof Throwable) throw new IllegalArgumentException() ;
			verdicts.add(new AbstractMap.SimpleEntry(verdictname,o)) ;
		}
		
		public void registerFail(String verdictname, Object o) {
			if (! (o instanceof Throwable)) throw new IllegalArgumentException() ;
			verdicts.add(new AbstractMap.SimpleEntry(verdictname,o)) ;
			failCount++ ;
		}
		
		public void registerUndecided(String verdictname) {
			verdicts.add(new AbstractMap.SimpleEntry(verdictname,null)) ;
		}
		
		public List<Entry<String,Object>> getVerdicts() { return verdicts ; }
		
		public List<Entry<String,Object>> getFails() {
			return verdicts.stream()
			   .filter(entry -> { var v = entry.getValue() ; return v != null && v instanceof Throwable ; } )
			   .collect(Collectors.toList())
			   ;
		}
		
		public boolean success() { return failCount <= 0 ; }
		public boolean failed()  { return failCount > 0 ; }
	}
	
	public static class Trace {
		List<String> trace = new LinkedList<String>() ;
	}
	
	Coverage coverage = new Coverage() ;
	Verdicts verdicts = new Verdicts() ;
    Trace trace = new Trace() ;
	
	public FunctionalTestAgent setTestDesc(String desc) {
		testDesc = desc ; return this ;
	}
	
	public Coverage getCoverage() { return coverage ; }
	public Verdicts getVerdicts() { return verdicts ; }

}
