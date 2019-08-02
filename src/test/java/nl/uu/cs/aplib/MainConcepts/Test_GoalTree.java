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
		assertTrue(g2.getStatus().inProgress()) ;
		g1.setStatusToSuccess("");
		assertTrue(g2.getStatus().sucess()) ;
		assertTrue(g3.getStatus().sucess()) ;
		
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
		assertTrue(g3.getStatus().sucess()) ;
		assertTrue(g4.getStatus().sucess()) ;
		
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
		assertTrue(g3.getStatus().sucess()) ;
		assertTrue(g4.getStatus().inProgress()) ;	
		
	}
	
	GoalTree g1 ;
	GoalTree g2 ;
	GoalTree g3 ;
	GoalTree g4 ;
	GoalTree g5 ;
	GoalTree g6 ;
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
	public void test_submitProposal() {
		var g = goal("")
				.toSolve_(i -> ((Integer) i ) == 0) 
				.withDistF(i -> 0d+ ((int) i)) ;
		
		g.propose((Integer) 1);
		assertTrue(g.getStatus().inProgress()) ;
		assertTrue(g.distance() == 1d) ;
		
		g.propose((Integer) 0);
		assertTrue(g.getStatus().sucess()) ;
		assertTrue(g.distance() == 0d) ;
		
	}
	
	
	static public void main(String[] args) {
		
	}

}
