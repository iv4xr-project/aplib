package eu.iv4xr.framework.mainConcepts;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

import static org.junit.jupiter.api.Assertions.* ;

import static eu.iv4xr.framework.Iv4xrEDSL.* ;
import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;


public class Test_TestGoal {
	
	static public class Proposal {
		int x = 0 ;
		Proposal(int y) { x = y ; }
	}
	
	@Test
	public void test_TestGoal() {
		
		var pass = new VerdictEvent("inv0","...",true) ;
		var fail = new VerdictEvent("inv0","...",false) ;
		
		
		var agent = new TestAgent("A1","") . setTestDataCollector(new TestDataCollector()) ;
		//agent.attachState(state) ;
		var tgoal = testgoal("tg") ;
		tgoal.toSolve((Proposal p) -> p.x >= 10) ;
		tgoal.oracle(agent,((Proposal p) -> { System.out.println("xxxxx") ; return p.x >= 12 ? pass : fail ; })) ;
		
		assertTrue(agent.getTestDataCollector().getNumberOfPassVerdictsSeen() == 0 ) ;
		assertTrue(agent.getTestDataCollector().getNumberOfFailVerdictsSeen() == 0 ) ;
		
		tgoal.propose_(new Proposal(20)) ; // should result in success

		assertTrue(agent.getTestDataCollector().getNumberOfPassVerdictsSeen() == 1 ) ;
		assertTrue(agent.getTestDataCollector().getNumberOfPassVerdictsSeen("A1") == 1 ) ;
		assertTrue(agent.getTestDataCollector().getNumberOfFailVerdictsSeen() == 0 ) ;
		
		tgoal.propose_(new Proposal(10)) ; // should result in fail
		
		assertTrue(agent.getTestDataCollector().getNumberOfPassVerdictsSeen() == 1 ) ;
		assertTrue(agent.getTestDataCollector().getNumberOfPassVerdictsSeen("A1") == 1 ) ;
		assertTrue(agent.getTestDataCollector().getNumberOfFailVerdictsSeen() == 1 ) ;
		assertTrue(agent.getTestDataCollector().getNumberOfFailVerdictsSeen("A1") == 1 ) ;

	}

}
