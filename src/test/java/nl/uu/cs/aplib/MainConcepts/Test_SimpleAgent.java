package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import nl.uu.cs.aplib.Environments.ConsoleEnvironment;
import nl.uu.cs.aplib.MainConcepts.GoalTree.*;



public class Test_SimpleAgent {
	
	static class MyState extends SimpleState {
		int counter = 0 ;
		String last = null ;
		MyState(){ super() ; }
	}
	
	@Test
	public void test_with_oneaction() {
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);
	    
	    // testing update with no goal:
	    agent.update();
	    assertTrue(agent.goal == null) ;
	    assertTrue(agent.currentGoal == null) ;
	    
	    // ok let's now give a goal:
	    var a0 = action("a0").do_((MyState S)->actionstate-> {S.counter++ ; return S.counter ; }) ;
		var topgoal = lift(goal("g").toSolve((Integer k) -> k==2) . withStrategy(lift(a0))) ;
	    agent .setGoal(topgoal);
	    agent.update() ;    
	    assertTrue(state.counter == 1) ;
	    assertTrue(topgoal.getStatus().inProgress()) ;
	    
	    agent.update() ;    
	    assertTrue(state.counter == 2) ;
	    assertTrue(topgoal.getStatus().success()) ;
	    assertTrue(agent.goal == null) ;
	    assertTrue(agent.currentGoal == null) ;
	}
	
	@Test
	public void test2() {
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);
		var a0 = action("a0")
				 .do_((MyState S)->actionstate-> {S.counter++ ; S.last = "a0" ; return S.counter ; })
				 .on_((MyState S) -> S.counter % 2 == 0) ;
		var a1 = action("a1")
				.do_((MyState S)->actionstate-> {S.counter++ ; S.last = "a1" ; return S.counter ; })
				.on_((MyState S) -> S.counter % 2 == 1) ;
		var topgoal = lift(goal("g").toSolve((Integer k) -> k==2) 
				. withStrategy(FIRSTof(lift(a0),lift(a1)))) ;
		
		agent .setGoal(topgoal);
	    agent.update() ;    
	    assertTrue(state.counter == 1) ;
	    assertTrue(state.last.equals("a0")) ;
	    assertTrue(topgoal.getStatus().inProgress()) ;
	    agent.update() ;    
	    assertTrue(state.counter == 2) ;
	    assertTrue(state.last.equals("a1")) ;
	    assertTrue(topgoal.getStatus().success()) ;
	    
		
	}
	

}
