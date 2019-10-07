package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import nl.uu.cs.aplib.Environments.ConsoleEnvironment;
import nl.uu.cs.aplib.MainConcepts.GoalStructure.*;
import nl.uu.cs.aplib.MainConcepts.Test_BasicAgent.MyState;
import nl.uu.cs.aplib.Utils.Time;

public class Test_BasicAgent_budgeting {
	
	static class MyState extends SimpleState {
		int counter = 0 ;
		String last = null ;
		MyState(){ super() ; }
	}
		
	static class MockedTime extends Time {
		long[] ticks ;
		int counter = 0 ;
		MockedTime() { super() ; }
		MockedTime(long ... ticks) { this.ticks = ticks ; }
		
        @Override
		public long currentTime() { return ticks[counter] ; }
        
        @Override
        public void sample() {
        	lastsample = currentTime() ; counter++ ; 
        }
	}
	
	void sleepx(long time) {
		try { Thread.sleep(time) ; }
		catch(Exception e) { } ;
	}
	
	@Test
	public void test_simplescenario() {
		// test with a single action and one simple goal, with the action exhausting the budget.
		
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);   
		
		// slightly over budget scenario:
	    var a0 = action("a0").do_((MyState S)->actionstate-> { S.counter++ ; return S.counter ; }) ;
	    
		var topgoal = lift(goal("g").toSolve((Integer k) -> k==3) . withStrategy(lift(a0)))
				      .withBudget(100); 
		
		agent.mytime = new MockedTime(0,101) ;
		agent.setGoal(topgoal) ;
		//System.out.println(">>> alocated topgoal " + topgoal.allocatedBudget) ;
		agent.update();
        assertTrue(topgoal.getStatus().failed()) ;
		assertTrue(agent.goal == null) ;
	    assertTrue(agent.currentGoal == null) ;		
	    
	    // just under the budget scenario:
	    state.counter = 0 ;
        a0 = action("a0").do_((MyState S)->actionstate-> { S.counter++ ; return S.counter ; }) ;
	    
		topgoal = lift(goal("g").toSolve((Integer k) -> k==3) . withStrategy(lift(a0)))
				  .withBudget(100); 
		
		agent.mytime = new MockedTime(0,99) ;
		agent.setGoal(topgoal) ;
		agent.update();
        assertTrue(topgoal.getStatus().inProgress()) ;
        
        // exactly at budget scenario:
	    state.counter = 0 ;
        a0 = action("a0").do_((MyState S)->actionstate-> { S.counter++ ; return S.counter ; }) ;
	    
		topgoal = lift(goal("g").toSolve((Integer k) -> k==3) . withStrategy(lift(a0)))
				  .withBudget(100); 
		
		agent.mytime = new MockedTime(0,100) ;
		agent.setGoal(topgoal) ;
		agent.update();
        assertTrue(topgoal.getStatus().failed()) ;
        
        // budget is exceeded, but the goal is solved scenario:
        a0 = action("a0").do_((MyState S)->actionstate-> { S.counter++ ; return S.counter ; }) ;
	    
		topgoal = lift(goal("g").toSolve((Integer k) -> true) . withStrategy(lift(a0)))
				  .withBudget(100); 
		
		agent.mytime = new MockedTime(0,200) ;
		agent.setGoal(topgoal) ;
		agent.update();
        assertTrue(topgoal.getStatus().success()) ;
	}
	
	@Test
	public void test_exhaustingbudget_on_multiplegoals() {
		// a goal of the form FIRSTof(g1,g2), where g1 exhausts its budget
		
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);
	   
		
	    // an action that thinks too long:
	    var a0 = action("a0").do_((MyState S)->actionstate-> {S.counter = 5 ; return S.counter ; }) ;
	    
	    var a1 = action("a1").do_((MyState S)->actionstate-> {S.counter++ ; return S.counter ; }) ;
	    
	    var g1 = lift(goal("g1").toSolve((Integer k) -> k==6) . withStrategy(lift(a0))) ;
	    var g2 = lift(goal("g2").toSolve((Integer k) -> k==7) . withStrategy(lift(a1))) ;
	    var topgoal = FIRSTof(g1,g2).withBudget(1000) ;
	    
	    agent.setGoal(topgoal) ;
	    agent.mytime = new MockedTime(0,501,510,520,530) ;
	    
	    System.out.println("===x 0 state:" + state.counter) ;
		System.out.println(">>> alocated topgoal " + topgoal.allocatedBudget) ;
		System.out.println(">>> togoal used budget " + topgoal.consumedBudget) ;
		System.out.println(">>> budget g1 " + g1.remainingBudget) ;
		System.out.println(">>> budget g2 " + g2.remainingBudget) ;
		System.out.println(">>> used budget g1 " + g1.consumedBudget) ;
		System.out.println(">>> used budget g2 " + g2.consumedBudget) ;
		System.out.println(">>> status top " + topgoal.getStatus()) ;
		System.out.println(">>> status g1 " + g1.getStatus()) ;
		System.out.println(">>> status g2 " + g2.getStatus()) ;
		assertTrue(agent.currentGoal == g1) ;

	    agent.update();
	    System.out.println("=== 1 state:" + state.counter) ;
		System.out.println(">>> alocated topgoal " + topgoal.allocatedBudget) ;
		System.out.println(">>> togoal used budget " + topgoal.consumedBudget) ;
		System.out.println(">>> budget g1 " + g1.remainingBudget) ;
		System.out.println(">>> budget g2 " + g2.remainingBudget) ;
		System.out.println(">>> used budget g1 " + g1.consumedBudget) ;
		System.out.println(">>> used budget g2 " + g2.consumedBudget) ;
		System.out.println(">>> status top " + topgoal.getStatus()) ;
		System.out.println(">>> status g1 " + g1.getStatus()) ;
		System.out.println(">>> status g2 " + g2.getStatus()) ;
		assertTrue(agent.currentGoal == g2) ;
		assertTrue(state.counter == 5) ;
	    assertTrue(g1.getStatus().failed()) ;
		assertTrue(topgoal.getStatus().inProgress()) ;
		assertTrue(topgoal.consumedBudget == 501) ;
		assertTrue(g1.consumedBudget == 501) ;
		assertTrue(g2.consumedBudget == 0) ;
			
		agent.update();
	    System.out.println("=== 2 state:" + state.counter) ;
		System.out.println(">>> alocated topgoal " + topgoal.allocatedBudget) ;
		System.out.println(">>> togoal used budget " + topgoal.consumedBudget) ;
		System.out.println(">>> budget g1 " + g1.remainingBudget) ;
		System.out.println(">>> budget g2 " + g2.remainingBudget) ;
		System.out.println(">>> used budget g1 " + g1.consumedBudget) ;
		System.out.println(">>> used budget g2 " + g2.consumedBudget) ;
		System.out.println(">>> status top " + topgoal.getStatus()) ;
		System.out.println(">>> status g1 " + g1.getStatus()) ;
		System.out.println(">>> status g2 " + g2.getStatus()) ;
		assertTrue(agent.currentGoal == g2) ;
		assertTrue(state.counter == 6) ;
	    assertTrue(g1.getStatus().failed()) ;
		assertTrue(topgoal.getStatus().inProgress()) ;
		assertTrue(topgoal.consumedBudget == 510) ;
		assertTrue(g1.consumedBudget == 501) ;
		assertTrue(g2.consumedBudget == 9) ;


		agent.update() ;
		System.out.println("=== 3 state:" + state.counter) ;
		System.out.println(">>> alocated topgoal " + topgoal.allocatedBudget) ;
		System.out.println(">>> togoal used budget " + topgoal.consumedBudget) ;
		System.out.println(">>> budget g1 " + g1.remainingBudget) ;
		System.out.println(">>> budget g2 " + g2.remainingBudget) ;
		System.out.println(">>> used budget g1 " + g1.consumedBudget) ;
		System.out.println(">>> used budget g2 " + g2.consumedBudget) ;
		System.out.println(">>> status top " + topgoal.getStatus()) ;
		System.out.println(">>> status g1 " + g1.getStatus()) ;
		System.out.println(">>> status g2 " + g2.getStatus()) ;
		assertTrue(state.counter == 7) ;
	    assertTrue(g1.getStatus().failed()) ;
		assertTrue(topgoal.getStatus().success()) ;
		assertTrue(agent.currentGoal == null) ;
		assertTrue(topgoal.consumedBudget == 520) ;
		assertTrue(g1.consumedBudget == 501) ;
		assertTrue(g2.consumedBudget == 19) ;
	}

}
