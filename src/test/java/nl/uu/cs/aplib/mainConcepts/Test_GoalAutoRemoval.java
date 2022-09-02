package nl.uu.cs.aplib.mainConcepts;

import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.mainConcepts.Test_DynamicGoalStructure.MyState;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static nl.uu.cs.aplib.mainConcepts.Test_DynamicGoalStructure.* ;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class Test_GoalAutoRemoval {
	
	// NOTE: if the top-goal is concluded the agent won't bother
	// cleaning autoremove subgoals. This is expected behavior.
	
	@Test
	// simple cases where goals are auto-removed
	public void test_simpleremoval_cases() {
		MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());
		
		GoalStructure G1 = SUCCESS() ; 
		GoalStructure G2 = SUCCESS() ; G2.autoRemove = true ;
		GoalStructure H = FAIL() ;
		GoalStructure M = SEQ(G1,G2,H) ;
		
		 agent.setGoal(M);
	     executeAgent(agent,4) ;
	     
	     assertTrue(M.status.failed());
	     assertTrue(M.subgoals.contains(G1)) ;
	     assertFalse(M.subgoals.contains(G2)) ;
	     assertTrue(M.subgoals.contains(H)) ;     	
	     
	     System.out.println(">>>>") ;
	     
	     G1 = SUCCESS() ; G1.autoRemove = true ;
		 G2 = FAIL() ; 
		 M = FIRSTof(G1,G2) ;
		 H = FAIL() ;
		 GoalStructure N = SEQ(M,H) ;
			
		 agent.setGoal(N);
		 executeAgent(agent,4) ;
		 
		 assertTrue(M.status.success());
		 assertTrue(N.status.failed());
	     assertTrue(N.subgoals.contains(M)) ;
	     assertTrue(N.subgoals.contains(H)) ;
	     assertFalse(M.subgoals.contains(G1)) ;  
	     assertTrue(M.subgoals.contains(G2)) ;  

	}
	
	@Test
	// cases where auto-removed subgoals should NOT be removed
	public void test_no_removal_case() {
		MyState state = new MyState();
		BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());

		GoalStructure G1 = SUCCESS();
		GoalStructure G2 = SUCCESS();
		G2.autoRemove = true;
		GoalStructure M1 = FIRSTof(G1, G2);
		GoalStructure H = FAIL();
		GoalStructure M2 = SEQ(M1, H);

		agent.setGoal(M2);
		executeAgent(agent, 6);

		assertTrue(M1.status.success());
		assertTrue(M2.status.failed());
		assertTrue(M1.subgoals.contains(G1));
		assertTrue(M1.subgoals.contains(G2));
		assertTrue(M2.subgoals.contains(M1));
		assertTrue(M2.subgoals.contains(H));
	}
	
	@Test
	public void test_nested_autoremove() {
		MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());
		
		GoalStructure G1 = SUCCESS() ; G1.autoRemove = true ;
		GoalStructure M1 = FIRSTof(G1,FAIL()) ; M1.autoRemove = true ;
		GoalStructure H = FAIL() ;
		GoalStructure M2 = SEQ(M1,H) ;
		
		agent.setGoal(M2);
		executeAgent(agent, 6);

		assertTrue(M2.status.failed());
		assertFalse(M2.subgoals.contains(M1));
		assertTrue(M2.subgoals.contains(H));	
	}

}
