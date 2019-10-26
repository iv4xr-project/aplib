package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.MainConcepts.GoalStructure.PrimitiveGoal;

import static org.junit.jupiter.api.Assertions.* ;

public class Test_GoalStructure {
	
	PrimitiveGoal g1 ;
	PrimitiveGoal g2 ;
	PrimitiveGoal g3 ;
	PrimitiveGoal g4 ;
	PrimitiveGoal g5 ;
	PrimitiveGoal g6 ;
	GoalStructure a ;
	GoalStructure b ;
	GoalStructure c ;
	GoalStructure d ;

	void setup() {
		g1 = lift(goal("g1")) ;
		g2 = lift(goal("g2")) ;
		g3 = lift(goal("g3")) ;
		g4 = lift(goal("g4")) ;
		g5 = lift(goal("g5")) ;
		g6 = lift(goal("g6")) ;
		a = FIRSTof(g1,g2) ;
		b = FIRSTof(g3,g4) ;
		c = SEQ(a,b,g5) ;
		d = FIRSTof(c,g6) ;
	}
	
	@Test
	public void test_submitProposal() {
		var g = goal("")
				.ftoSolve_(i -> (Double) i - 0) ; 
		
		g.propose(1d);
		assertTrue(g.getStatus().inProgress()) ;
		assertTrue(g.distance() == 1d) ;
		
		g.propose(0d);
		assertTrue(g.getStatus().success()) ;
		assertTrue(g.distance() == 0d) ;
		
	}

	@Test
	public void test_1_status_propagation() {

		var h1 = lift(goal("")) ;
		var h2 = FIRSTof(h1, lift(goal(""))) ;
		var h3 = FIRSTof(h2,lift(goal(""))) ;
		assertTrue(h2.getStatus().inProgress()) ;
		h1.setStatusToSuccess("");
		assertTrue(h2.getStatus().success()) ;
		assertTrue(h3.getStatus().success()) ;
		
		h1 = lift(goal("")) ;
		h2 = FIRSTof(h1, lift(goal(""))) ;
		h3 = FIRSTof(h2,lift(goal(""))) ;
		h1.setStatusToFail("");
		assertTrue(h2.getStatus().inProgress()) ;
		assertTrue(h3.getStatus().inProgress()) ;
		
		
		h1 = lift(goal("")) ;
		h2 = lift(goal("")) ;
		h3 = FIRSTof(h1,h2) ;
		var h4 = FIRSTof(h3,lift(goal(""))) ;
		h1.setStatusToFail("");
		h2.setStatusToSuccess("");
		assertTrue(h3.getStatus().success()) ;
		assertTrue(h4.getStatus().success()) ;
		
		h1 = lift(goal("")) ;
		h2 = lift(goal("")) ;
		h3 = FIRSTof(h1,h2) ;
		h4 = FIRSTof(h3,lift(goal(""))) ;
		h1.setStatusToFail("");
		h2.setStatusToFail("");
		assertTrue(h3.getStatus().failed()) ;
		assertTrue(h4.getStatus().inProgress()) ;	
	}

	@Test
	public void test_2_status_propagation() {

		var h1 = lift(goal("")) ;
		var h2 = SEQ(h1, lift(goal(""))) ;
		var h3 = SEQ(h2,lift(goal(""))) ;
		assertTrue(h2.getStatus().inProgress()) ;
		h1.setStatusToFail("");
		assertTrue(h2.getStatus().failed()) ;
		assertTrue(h3.getStatus().failed()) ;
		
		h1 = lift(goal("")) ;
		h2 = SEQ(h1, lift(goal(""))) ;
		h3 = SEQ(h2,lift(goal(""))) ;
		h1.setStatusToSuccess("");
		assertTrue(h2.getStatus().inProgress()) ;
		assertTrue(h3.getStatus().inProgress()) ;
		
		h1 = lift(goal("")) ;
		h2 = lift(goal("")) ;
		h3 = SEQ(h1,h2) ;
		var h4 = SEQ(h3,lift(goal(""))) ;
		h1.setStatusToSuccess("");
		h2.setStatusToFail("");
		assertTrue(h3.getStatus().failed()) ;
		assertTrue(h4.getStatus().failed()) ;
		
		h1 = lift(goal("")) ;
		h2 = lift(goal("")) ;
		h3 = SEQ(h1,h2) ;
		h4 = SEQ(h3,lift(goal(""))) ;
		h1.setStatusToSuccess("");
		h2.setStatusToSuccess("");
		assertTrue(h3.getStatus().success()) ;
		assertTrue(h4.getStatus().inProgress()) ;	
		
	}
	

	
	@Test
	public void test_getNextPrimitiveGoal_noBudgetCheck() {
		setup() ;
		g1.setStatusToFail("");
		var h = g1.getNextPrimitiveGoal_andAllocateBudget() ;
		//System.out.println(h.goal.getName()) ;
		assertTrue(h == g2) ;
		assertTrue(d.getStatus().inProgress()) ;
		
		setup() ;
		g1.setStatusToSuccess("");
		h = g1.getNextPrimitiveGoal_andAllocateBudget() ;
		//System.out.println(h.goal.getName()) ;
		assertTrue(h == g3) ;
		assertTrue(d.getStatus().inProgress()) ;
		
		setup() ;
		g2.setStatusToFail("");
		assertTrue(g2.getNextPrimitiveGoal_andAllocateBudget() == g6) ;
		assertTrue(d.getStatus().inProgress()) ;

		setup() ;
		g2.setStatusToSuccess("");
		assertTrue(g2.getNextPrimitiveGoal_andAllocateBudget() == g3) ;
		assertTrue(d.getStatus().inProgress()) ;
		
		setup() ;
		g3.setStatusToFail("");
		assertTrue(g3.getNextPrimitiveGoal_andAllocateBudget() == g4) ;
		assertTrue(d.getStatus().inProgress()) ;

		setup() ;
		g3.setStatusToSuccess("");
		assertTrue(g3.getNextPrimitiveGoal_andAllocateBudget() == g5) ;
		assertTrue(d.getStatus().inProgress()) ;

		setup() ;
		g5.setStatusToFail("");
		assertTrue(g5.getNextPrimitiveGoal_andAllocateBudget() == g6) ;
		assertTrue(d.getStatus().inProgress()) ;

		setup() ;
		g5.setStatusToSuccess("");
		assertTrue(g5.getNextPrimitiveGoal_andAllocateBudget() == null) ;
		assertTrue(d.getStatus().success()) ;

		setup() ;
		g6.setStatusToFail("");
		assertTrue(g6.getNextPrimitiveGoal_andAllocateBudget() == null) ;
		assertTrue(d.getStatus().failed()) ;
		
		setup() ;
		g6.setStatusToSuccess("");
		assertTrue(g6.getNextPrimitiveGoal_andAllocateBudget() == null) ;
		assertTrue(d.getStatus().success()) ;
	}
	
	@Test
	public void test_setting_maxbudget() {
		setup() ;
		g1.maxbudget(10) ;
		g2.maxbudget(20) ;
		var topgoal = d ;
		//System.out.println(topgoal.demandedMinimumBudget()) ;
		assertTrue(g1.getMaxBudgetAllowed() == 10) ;
		assertTrue(g2.getMaxBudgetAllowed() == 20) ;
		assertTrue(topgoal.getMaxBudgetAllowed() == Double.POSITIVE_INFINITY) ;
	}
	
	@Test
	public void test_1a_getDeepestFirstPrimGoal_andAllocateBudget() {	
		g1 = lift(goal("g1")) ;
		g1.maxbudget(10) ;
        PrimitiveGoal y = g1.getDeepestFirstPrimGoal_andAllocateBudget() ;
		assertTrue(y == g1) ;
		assertTrue(g1.bmax == 10) ;
		assertTrue(g1.budget == 10) ;
		
		g1 = lift(goal("g1")) ;
		g2 = lift(goal("g2")) ;
		var g = SEQ(g2,g1) ;
		g.maxbudget(10) ;
		g.budget = (5) ;
        y = g.getDeepestFirstPrimGoal_andAllocateBudget() ;
		assertTrue(y == g2) ;
		assertTrue(g.budget == 5) ;
		assertTrue(g1.budget == Double.POSITIVE_INFINITY) ;
		assertTrue(g2.budget == 5) ;
		
		g1 = lift(goal("g1")) ;
		g2 = lift(goal("g2")) ;
		g = FIRSTof(g2,g1) ;
		g.maxbudget(10) ;
		g.budget = (15) ;
		g2.maxbudget(5) ;
        y = g.getDeepestFirstPrimGoal_andAllocateBudget() ;
		assertTrue(y == g2) ;
		assertTrue(g.budget == 10) ;
		assertTrue(g1.budget == Double.POSITIVE_INFINITY) ;
		assertTrue(g2.budget == 5) ;
	}
	
	@Test
	public void test_1b_getDeepestFirstPrimGoal_andAllocateBudget() {	
		setup() ;
		d.maxbudget(10) ;
		g1.maxbudget(5) ;
		assertTrue(g1.bmax == 5) ;
		var y = d.getDeepestFirstPrimGoal_andAllocateBudget() ;
		
		assertTrue(y == g1) ;
		
		//System.out.println("*** d:" + d.budget) ;		
		assertTrue(d.budget == 10) ;
		assertTrue(c.budget == 10) ;
		assertTrue(a.budget == 10) ;
		//System.out.println("*** g1:" + g1.budget) ;
		//assertTrue(g1.parent.budget == 10) ;
		assertTrue(g1.budget == 5) ;
		
		setup() ;
		d.budget = 10 ;
		d.maxbudget(20) ;
		a.maxbudget(12) ;
		g1.maxbudget(5) ;
		y = d.getDeepestFirstPrimGoal_andAllocateBudget() ;
		assertTrue(y == g1) ;
		assertTrue(d.budget == 10) ;
		assertTrue(c.budget == 10) ;
		assertTrue(a.budget == 10) ;
		assertTrue(g1.budget == 5) ;
	}

	@Test
	public void test_getNextPrimitiveGoal_withBudgetCheck() {
		setup() ;
		var g = d ;
		g.maxbudget(20) ;
		g1.maxbudget(10) ;
		g2.maxbudget(8) ;
		g6.maxbudget(20) ;
		var y = d.getDeepestFirstPrimGoal_andAllocateBudget() ;
		assertTrue(y == g1) ;
		assertTrue(a.budget == 20) ;
		
		// call it on still open goal; it should throw an exception
		try {
			g1.getNextPrimitiveGoal_andAllocateBudget() ;
			assertTrue(false) ;
		}
		catch(IllegalArgumentException e) {
			// System.out.println("#####") ;
		}
		
		// consume all budget for g1, then call getDeepestFirstPrimGoal_andAllocateBudget:
		g1.registerConsumedBudget(10);
		g1.setStatusToFailBecauseBudgetExhausted();
		assertTrue(d.getStatus().inProgress()) ;
		
		var h = g1.getNextPrimitiveGoal_andAllocateBudget() ;
		//System.out.println(h.goal.getName()) ;
		assertTrue(h == g2) ;
		assertTrue(d.budget == 10) ;
		assertTrue(c.budget == 10) ;
		assertTrue(a.budget == 10) ;
		assertTrue(g2.budget == 8) ;
		
		//g.printGoalStructureStatus(); 
		
		g2.registerConsumedBudget(1);
		g2.setStatusToSuccess("g2 solved"); 
		h = g2.getNextPrimitiveGoal_andAllocateBudget() ;
		assertTrue(h==g3) ;
		assertTrue(d.budget == 9) ;
		assertTrue(c.budget == 9) ;
		assertTrue(b.budget == 9) ;
		assertTrue(g3.budget == 9) ;
				
		//g.printGoalStructureStatus(); 
		
		g3.setStatusToFail("g3 fails");
		h = g3.getNextPrimitiveGoal_andAllocateBudget() ;
		assertTrue(h==g4) ;
		g4.setStatusToFail("g4 fails");
		h = g4.getNextPrimitiveGoal_andAllocateBudget() ;
		assertTrue(h==g6) ;
		assertTrue(d.budget == 9) ;
		assertTrue(g6.budget == 9) ;
		
		g.printGoalStructureStatus(); 
		
	}
	
}
