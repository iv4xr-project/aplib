package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import nl.uu.cs.aplib.Environments.ConsoleEnvironment;
import nl.uu.cs.aplib.MainConcepts.GoalTree.*;
import nl.uu.cs.aplib.MainConcepts.Test_BasicAgent.MyState;

public class Test_BasicAgent_budgeting {
	
	static class MyState extends SimpleState {
		int counter = 0 ;
		String last = null ;
		MyState(){ super() ; }
	}
	
	void sleepx(long time) {
		try { Thread.sleep(time) ; }
		catch(Exception e) { } ;
	}
	
	@Test
	public void test1() {
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);
	   
	    // an action that thinks too long:
	    var a0 = action("a0").do_((MyState S)->actionstate-> {
	    	      S.counter++ ; 
	    	      sleepx(1100); 
	    	      return S.counter ; }) ;
	    
		var topgoal = lift(goal("g").toSolve((Integer k) -> k==3) . withStrategy(lift(a0)))
				      .withBudget(100); 
		
		agent.setGoal(topgoal) ;
		// this agent will time out, but this won't be detected in the first update
		agent.update();
        assertTrue(topgoal.getStatus().inProgress()) ;
		agent.update();
        assertTrue(topgoal.getStatus().failed()) ;
	    assertTrue(agent.goal == null) ;
	    assertTrue(agent.currentGoal == null) ;
				      
	}

}
