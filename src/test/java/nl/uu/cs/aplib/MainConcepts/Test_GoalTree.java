package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.MainConcepts.GoalTree.PrimitiveGoal;

import static org.junit.jupiter.api.Assertions.* ;

public class Test_GoalTree {
	
	@Test
	public void test_submitProposal() {
		var g = goal("")
				.toSolve_(i -> ((Integer) i ) == 0) 
				.withDistF(i -> 0d+ ((int) i)) ;
		
		g.propose((Integer) 1);
		assertTrue(g.getStatus().inProgress()) ;
		assertTrue(g.distance() == 1d) ;
		
		g.propose((Integer) 0);
		assertTrue(g.getStatus().success()) ;
		assertTrue(g.distance() == 0d) ;
		
	}

	@Test
	public void test_1_status_propagation() {

		var g1 = lift(goal("")) ;
		var g2 = FIRSTof(g1, lift(goal(""))) ;
		var g3 = FIRSTof(g2,lift(goal(""))) ;
		assertTrue(g2.getStatus().inProgress()) ;
		g1.setStatusToSuccess("");
		assertTrue(g2.getStatus().success()) ;
		assertTrue(g3.getStatus().success()) ;
		
		g1 = lift(goal("")) ;
		g2 = FIRSTof(g1, lift(goal(""))) ;
		g3 = FIRSTof(g2,lift(goal(""))) ;
		g1.setStatusToFail("");
		assertTrue(g2.getStatus().inProgress()) ;
		assertTrue(g3.getStatus().inProgress()) ;
		
		
		g1 = lift(goal("")) ;
		g2 = lift(goal("")) ;
		g3 = FIRSTof(g1,g2) ;
		var g4 = FIRSTof(g3,lift(goal(""))) ;
		g1.setStatusToFail("");
		g2.setStatusToSuccess("");
		assertTrue(g3.getStatus().success()) ;
		assertTrue(g4.getStatus().success()) ;
		
		g1 = lift(goal("")) ;
		g2 = lift(goal("")) ;
		g3 = FIRSTof(g1,g2) ;
		g4 = FIRSTof(g3,lift(goal(""))) ;
		g1.setStatusToFail("");
		g2.setStatusToFail("");
		assertTrue(g3.getStatus().failed()) ;
		assertTrue(g4.getStatus().inProgress()) ;	
	}

	@Test
	public void test_2_status_propagation() {

		var g1 = lift(goal("")) ;
		var g2 = SEQ(g1, lift(goal(""))) ;
		var g3 = SEQ(g2,lift(goal(""))) ;
		assertTrue(g2.getStatus().inProgress()) ;
		g1.setStatusToFail("");
		assertTrue(g2.getStatus().failed()) ;
		assertTrue(g3.getStatus().failed()) ;
		
		g1 = lift(goal("")) ;
		g2 = SEQ(g1, lift(goal(""))) ;
		g3 = SEQ(g2,lift(goal(""))) ;
		g1.setStatusToSuccess("");
		assertTrue(g2.getStatus().inProgress()) ;
		assertTrue(g3.getStatus().inProgress()) ;
		
		g1 = lift(goal("")) ;
		g2 = lift(goal("")) ;
		g3 = SEQ(g1,g2) ;
		var g4 = SEQ(g3,lift(goal(""))) ;
		g1.setStatusToSuccess("");
		g2.setStatusToFail("");
		assertTrue(g3.getStatus().failed()) ;
		assertTrue(g4.getStatus().failed()) ;
		
		g1 = lift(goal("")) ;
		g2 = lift(goal("")) ;
		g3 = SEQ(g1,g2) ;
		g4 = SEQ(g3,lift(goal(""))) ;
		g1.setStatusToSuccess("");
		g2.setStatusToSuccess("");
		assertTrue(g3.getStatus().success()) ;
		assertTrue(g4.getStatus().inProgress()) ;	
		
	}
	
	PrimitiveGoal g1 ;
	PrimitiveGoal g2 ;
	PrimitiveGoal g3 ;
	PrimitiveGoal g4 ;
	PrimitiveGoal g5 ;
	PrimitiveGoal g6 ;
	GoalTree a ;
	GoalTree b ;
	GoalTree c ;
	GoalTree d ;

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
	public void test_getNextPrimitiveGoalWorker() {
		setup() ;
		g1.setStatusToFail("");
		var h = g1.getNextPrimitiveGoal() ;
		//System.out.println(h.goal.getName()) ;
		assertTrue(h == g2) ;
		
		setup() ;
		g1.setStatusToSuccess("");
		h = g1.getNextPrimitiveGoal() ;
		//System.out.println(h.goal.getName()) ;
		assertTrue(h == g3) ;
		
		setup() ;
		g2.setStatusToFail("");
		assertTrue(g2.getNextPrimitiveGoal() == g6) ;

		setup() ;
		g2.setStatusToSuccess("");
		assertTrue(g2.getNextPrimitiveGoal() == g3) ;
		
		setup() ;
		g3.setStatusToFail("");
		assertTrue(g3.getNextPrimitiveGoal() == g4) ;
		setup() ;
		g3.setStatusToSuccess("");
		assertTrue(g3.getNextPrimitiveGoal() == g5) ;

		setup() ;
		g5.setStatusToFail("");
		assertTrue(g5.getNextPrimitiveGoal() == g6) ;
		setup() ;
		g5.setStatusToSuccess("");
		assertTrue(g5.getNextPrimitiveGoal() == null) ;

		setup() ;
		g6.setStatusToFail("");
		assertTrue(g6.getNextPrimitiveGoal() == null) ;
		setup() ;
		g6.setStatusToSuccess("");
		assertTrue(g6.getNextPrimitiveGoal() == null) ;
	}
	
	@Test
	public void test_demandedMinimumBudget() {
		setup() ;
		g1.goal.demandMinimumBudget(10) ;
		var topgoal = d ;
		//System.out.println(topgoal.demandedMinimumBudget()) ;
		assertTrue(topgoal.demandedMinimumBudget() == 10) ;
		g2.goal.demandMinimumBudget(10) ;
		assertTrue(topgoal.demandedMinimumBudget() == 20) ;
		g5.goal.demandMinimumBudget(10) ;
		assertTrue(topgoal.demandedMinimumBudget() == 30) ;
	}
	
	void printBudget() {
		System.out.println("g1 remaining: " + g1.remainingBudget) ;
		System.out.println("g2 remaining: " + g2.remainingBudget) ;
		System.out.println("g3 remaining: " + g3.remainingBudget) ;
		System.out.println("g4 remaining: " + g4.remainingBudget) ;
		System.out.println("g5 remaining: " + g5.remainingBudget) ;
		System.out.println("g6 remaining: " + g6.remainingBudget) ;
		System.out.println("a remaining: " + a.remainingBudget) ;
		System.out.println("b remaining: " + b.remainingBudget) ;
		System.out.println("c remaining: " + c.remainingBudget) ;
		System.out.println("d remaining: " + d.remainingBudget) ;
	}
	
	@Test
	public void test_1_withBudget() {
		g1 = lift(goal("g1")) ;
		g2 = lift(goal("g2")) ;
		g2.goal.demandMinimumBudget(10) ;
		g3 = lift(goal("g3")) ;
		d = SEQ(g1,g2,g3) ;
		d.withBudget(60) ;
		assertTrue(g1.remainingBudget == 20) ;
		assertTrue(g2.remainingBudget == 20) ;
		assertTrue(g3.remainingBudget == 20) ;
		d.withBudget(30) ;
		assertTrue(g1.remainingBudget == 10) ;
		assertTrue(g2.remainingBudget == 10) ;
		assertTrue(g3.remainingBudget == 10) ;
		d.withBudget(24) ;
		assertTrue(g1.remainingBudget == 7) ;
		assertTrue(g2.remainingBudget == 10) ;
		assertTrue(g3.remainingBudget == 7) ;
		d.withBudget(10) ;
		assertTrue(g1.remainingBudget == 0) ;
		assertTrue(g2.remainingBudget == 10) ;
		assertTrue(g3.remainingBudget == 0) ;
		
		boolean exceptionThrown = false ;
		try {
			d.withBudget(9) ;
		}
		catch(Exception e) { exceptionThrown = true ; }
		
		assertTrue(exceptionThrown) ;
	}

	@Test
	public void test_2_withBudget() {
		//System.out.println(">>> test_2_withBudget") ;
		g1 = lift(goal("g1")) ;
		g2 = lift(goal("g2")) ;
		g2.goal.demandMinimumBudget(10) ;
		g3 = lift(goal("g3")) ;
		d = FIRSTof(g1,g2,g3) ;
		g1.setStatusToFail("");
		d.withBudget(60) ;
		
		//System.out.println(">> g1 status:" + g1.getStatus()) ;
		//System.out.println(">> g1 remaining budget:" + g1.remainingBudget) ;
		assertTrue(g1.remainingBudget == 0) ;
		assertTrue(g2.remainingBudget == 30) ;
		assertTrue(g3.remainingBudget == 30) ;
		
		g2.setStatusToFail("");
		d.withBudget(60) ;
		assertTrue(g1.remainingBudget == 0) ;
		assertTrue(g2.remainingBudget == 0) ;
		assertTrue(g3.remainingBudget == 60) ;
	}
	
	@Test
	public void test_3_withBudget() {
		//System.out.println(">>> test_3_withBudget") ;
		g1 = lift(goal("g1")) ;
		g2 = lift(goal("g2")) ;
		a = FIRSTof(g1,g2) ;
		g3 = lift(goal("g3")) ;
        b = FIRSTof(a,g3) ;
		g1.goal.demandMinimumBudget(10) ;
		b.withBudget(60) ;

		assertTrue(g1.remainingBudget == 20) ;
		assertTrue(g2.remainingBudget == 20) ;
		//System.out.println(">> g3 remaining budget:" + g3.remainingBudget) ;
		assertTrue(g3.remainingBudget == 20) ;
		
		b.withBudget(24) ;
		//System.out.println(">> g1 remaining budget:" + g1.remainingBudget) ;
		//System.out.println(">> g2 remaining budget:" + g2.remainingBudget) ;
		//System.out.println(">> g3 remaining budget:" + g3.remainingBudget) ;
		assertTrue(g1.remainingBudget == 10) ;
		assertTrue(g2.remainingBudget == 6) ;
		assertTrue(g3.remainingBudget == 8) ;
		
		b.withBudget(10) ;
		assertTrue(g1.remainingBudget == 10) ;
		assertTrue(g2.remainingBudget == 0) ;
		assertTrue(g3.remainingBudget == 0) ;
	}
	
	static public void main(String[] args) { }

}
