package eu.iv4xr.framework.MainConcepts;

import static eu.iv4xr.framework.MainConcepts.TestDataCollector.*;

import java.util.*;

import eu.iv4xr.framework.MainConcepts.TestDataCollector.CoveragePointEvent;
import eu.iv4xr.framework.MainConcepts.TestDataCollector.TimeStampedObservationEvent;
import eu.iv4xr.framework.MainConcepts.TestDataCollector.VerdictEvent;
import nl.uu.cs.aplib.Agents.AutonomousBasicAgent;

public class FunctionalTestAgent extends AutonomousBasicAgent {
	
	protected String testDesc ;
	protected TestDataCollector testDataCollector ;
    		
	public FunctionalTestAgent setTestDesc(String desc) {
		testDesc = desc ; return this ;
	}

	public FunctionalTestAgent setTestDataCollector(TestDataCollector dc) {
		if (dc==null) throw new IllegalArgumentException() ;
		testDataCollector = dc ;
		dc.registerTestAgent(this.id);
		return this ;
	}
	
	public TestDataCollector getTestDataCollector() { return testDataCollector ; }
	
	/**
	 * Register a visit to e for the purpose of test-coverage tracking.
	 * 
	 * @param e Representing something of interest that we want to cover during testing.
	 */
	public void registerVisit(CoveragePointEvent e) {
		testDataCollector.registerVisit(id,e);
	}
	
	/**
	 * Register this event to be appended to a historical trace that this test agent keeps track.
	 * 
	 * @param event Some event whose occurence we want to keep track in a trace. The event will be time-stamped.
	 */
	public void registerEvent(TimeStampedObservationEvent event) {
		testDataCollector.registerEvent(id,event);
	}

	/**
	 * Register a verdict, which can be either a success or fail, or undecided. Verdicts will
	 * also be put in this agent trace.
	 * 
	 * @param verdict Representing the verdict.
	 */
	public void registerVerdict(VerdictEvent verdict) {
		testDataCollector.registerEvent(id,verdict);
	}

}
