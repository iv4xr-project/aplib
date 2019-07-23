package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

public class Test_GoalTree {
	
	

	@Test
	public void test_1_status_propagation() {

		var g1 = lift(goal("")) ;
		var g2 = FIRSTof(g1, lift(goal(""))) ;
		var g3 = FIRSTof(g2,lift(goal(""))) ;
		assertTrue(g2.getStatus() == ProgressStatus.INPROGRESS) ;
		g1.setStatusToSuccess();
		assertTrue(g2.getStatus() == ProgressStatus.SUCCESS) ;
		assertTrue(g3.getStatus() == ProgressStatus.SUCCESS) ;
		
		g1 = lift(goal("")) ;
		g2 = FIRSTof(g1, lift(goal(""))) ;
		g3 = FIRSTof(g2,lift(goal(""))) ;
		g1.setStatusToFail();
		assertTrue(g2.getStatus() == ProgressStatus.INPROGRESS) ;
		assertTrue(g3.getStatus() == ProgressStatus.INPROGRESS) ;
		
		
		g1 = lift(goal("")) ;
		g2 = lift(goal("")) ;
		g3 = FIRSTof(g1,g2) ;
		var g4 = FIRSTof(g3,lift(goal(""))) ;
		g1.setStatusToFail();
		g2.setStatusToSuccess();
		assertTrue(g3.getStatus() == ProgressStatus.SUCCESS) ;
		assertTrue(g4.getStatus() == ProgressStatus.SUCCESS) ;
		
		g1 = lift(goal("")) ;
		g2 = lift(goal("")) ;
		g3 = FIRSTof(g1,g2) ;
		g4 = FIRSTof(g3,lift(goal(""))) ;
		g1.setStatusToFail();
		g2.setStatusToFail();
		assertTrue(g3.getStatus() == ProgressStatus.FAILED) ;
		assertTrue(g4.getStatus() == ProgressStatus.INPROGRESS) ;		
	}

	@Test
	public void test_2_status_propagation() {

		var g1 = lift(goal("")) ;
		var g2 = SEQ(g1, lift(goal(""))) ;
		var g3 = SEQ(g2,lift(goal(""))) ;
		assertTrue(g2.getStatus() == ProgressStatus.INPROGRESS) ;
		g1.setStatusToFail();
		assertTrue(g2.getStatus() == ProgressStatus.FAILED) ;
		assertTrue(g3.getStatus() == ProgressStatus.FAILED) ;
		
		g1 = lift(goal("")) ;
		g2 = SEQ(g1, lift(goal(""))) ;
		g3 = SEQ(g2,lift(goal(""))) ;
		g1.setStatusToSuccess();
		assertTrue(g2.getStatus() == ProgressStatus.INPROGRESS) ;
		assertTrue(g3.getStatus() == ProgressStatus.INPROGRESS) ;
		
		g1 = lift(goal("")) ;
		g2 = lift(goal("")) ;
		g3 = SEQ(g1,g2) ;
		var g4 = SEQ(g3,lift(goal(""))) ;
		g1.setStatusToSuccess();
		g2.setStatusToFail();
		assertTrue(g3.getStatus() == ProgressStatus.FAILED) ;
		assertTrue(g4.getStatus() == ProgressStatus.FAILED) ;
		
		g1 = lift(goal("")) ;
		g2 = lift(goal("")) ;
		g3 = SEQ(g1,g2) ;
		g4 = SEQ(g3,lift(goal(""))) ;
		g1.setStatusToSuccess();
		g2.setStatusToSuccess();
		assertTrue(g3.getStatus() == ProgressStatus.SUCCESS) ;
		assertTrue(g4.getStatus() == ProgressStatus.INPROGRESS) ;			
	}
	
	
	@Test
	public void test_getNextPrimitiveGoalWorker() {
		
		var g1 = lift(goal("g1")) ;
		var g2 = lift(goal("g2")) ;
		var g3 = lift(goal("g3")) ;
		var g4 = lift(goal("g4")) ;
		var g5 = lift(goal("g5")) ;
		var g6 = lift(goal("g6")) ;
		var a = FIRSTof(g1,g2) ;
		var b = FIRSTof(g3,g4) ;
		var c = SEQ(a,b,g5) ;
		var d = FIRSTof(c,g6) ;
		
		/*
		var h1f = g1.getNextPrimitiveGoal(ProgressStatus.FAILED) ;
		var h1s = g1.getNextPrimitiveGoal(ProgressStatus.SUCCESS) ;
		System.out.println(h1f.goal.getName()) ;
		System.out.println(h1s.goal.getName()) ;
		assertTrue(h1f == g2) ;
		assertTrue(h1s == g3) ;
		
		var h2f = g2.getNextPrimitiveGoal(ProgressStatus.FAILED) ;
		var h2s = g2.getNextPrimitiveGoal(ProgressStatus.SUCCESS) ;
		System.out.println(h2f.goal.getName()) ;
		System.out.println(h2s.goal.getName()) ;
		assertTrue(h2f == g6) ;
		assertTrue(h2s == g3) ;
		
		var h3f = g3.getNextPrimitiveGoal(ProgressStatus.FAILED) ;
		var h3s = g3.getNextPrimitiveGoal(ProgressStatus.SUCCESS) ;
		System.out.println(h3f.goal.getName()) ;
		System.out.println(h3s.goal.getName()) ;
		assertTrue(h3f == g4) ;
		assertTrue(h3s == g5) ;
				
		var h5f = g5.getNextPrimitiveGoal(ProgressStatus.FAILED) ;
		var h5s = g5.getNextPrimitiveGoal(ProgressStatus.SUCCESS) ;
		System.out.println(h5f.goal.getName()) ;
		System.out.println("" + h5s) ;
		assertTrue(h5f == g6) ;
		assertTrue(h5s == null) ;
		
		var h6f = g6.getNextPrimitiveGoal(ProgressStatus.FAILED) ;
		var h6s = g6.getNextPrimitiveGoal(ProgressStatus.SUCCESS) ;
		System.out.println("" + h6f) ;
		System.out.println("" + h6s) ;
		assertTrue(h6f == null) ;
		assertTrue(h6s == null) ;
		*/
		
	}
	
	@Test
	public void test_submitProposal() {
		var g = goal("")
				.toSolve(i -> ((Integer) i ) == 0) 
				.withDistF(i -> 0d+ ((int) i)) ;
		
		g.propose((Integer) 1);
		assertTrue(g.getStatus() == ProgressStatus.INPROGRESS) ;
		assertTrue(g.distance() == 1d) ;
		
		g.propose((Integer) 0);
		assertTrue(g.getStatus() == ProgressStatus.SUCCESS) ;
		assertTrue(g.distance() == 0d) ;
		
	}
	
	
	static public void main(String[] args) {
		
	}

}
