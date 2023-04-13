package nl.uu.cs.aplib.mainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static nl.uu.cs.aplib.mainConcepts.Test_DynamicGoalStructure.* ;

import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.mainConcepts.Test_DynamicGoalStructure.MyState;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class Test_AddRemoveGoal_Action {
	
	Action set2() {
		Action a = action("return2").do1((MyState S) -> {S.x = 2 ; return S; }) ;
		return a ;
	}
	
	@Test
	public void test0() {
		
		MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());

		Goal g1 = goal("g1")
				  .toSolve((MyState S) -> S.x==2)
				  .withTactic(
					 SEQ(addAfter((MyState S) -> SUCCESS()).lift()	, 
						 set2().lift())) ;
		
		PrimitiveGoal G1 = g1.lift() ;
		GoalStructure G2 = SEQ(G1,SUCCESS()) ;
		
		agent.setGoal(G2) ;
		executeAgent(agent,4) ;
		
		assertTrue(G2.status.success()) ;
		assertTrue(G1.status.success()) ;
		assertTrue(G2.subgoals.size() == 2) ;
	}
	
	@Test
	public void test1() {
		
		MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());

		Goal g1 = goal("g1")
				  .toSolve((MyState S) -> S.x==2)
				  .withTactic(
					 SEQ(addAfter((MyState S) -> FAIL()).lift()	, 
						 ABORT())) ;
		
		PrimitiveGoal G1 = g1.lift() ;
		GoalStructure G2 = FIRSTof(G1,SUCCESS()) ;
		
		agent.setGoal(G2) ;
		executeAgent(agent,4) ;
		
		assertTrue(G2.status.success()) ;
		assertTrue(G1.status.failed()) ;
		assertTrue(G2.subgoals.size() == 2) ;
	}
	
	@Test
	public void test2() {
		
		MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());

		Goal g1 = goal("g1")
				  .toSolve((MyState S) -> S.x==2)
				  .withTactic(
					 SEQ(addBefore((MyState S) -> SUCCESS()).lift()	, 
						 ABORT())) ;
		
		PrimitiveGoal G1 = g1.lift() ;
		GoalStructure G2 = FIRSTof(G1) ;
		GoalStructure G3 = REPEAT(G2) ;
		
		agent.setGoal(SEQ(G3,FAIL())) ;
		executeAgent(agent,10) ;
		
		assertTrue(G3.status.success()) ;
		assertTrue(G2.status.success()) ;
		assertTrue(G1.status.inProgress()) ;
		assertTrue(G3.subgoals.size() == 1) ;
		assertTrue(G2.subgoals.size() == 1) ;
	}

}
