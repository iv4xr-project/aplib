package nl.uu.cs.aplib.iv4xr.verification;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.Agents.AutonomousBasicAgent;

public class FunctionalTestAgent extends AutonomousBasicAgent {
	
	String testDesc ;
	
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
	

	Verdicts verdicts = new Verdicts() ;

	
	public FunctionalTestAgent setTestDesc(String desc) {
		testDesc = desc ; return this ;
	}

	public Verdicts getVerdicts() { return verdicts ; }

}
