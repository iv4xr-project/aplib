package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import nl.uu.cs.aplib.Environments.ConsoleEnvironment;
import nl.uu.cs.aplib.MainConcepts.GoalTree.*;
import nl.uu.cs.aplib.Utils.Time;



public class Test_BasicAgent {
	
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
	public void test_abort() {
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);
		var a0 = action("a0")
				 .do_((MyState S)->actionstate-> {S.counter++ ; return S.counter ; }) ;
		
		var topgoal = lift(goal("g").toSolve((Integer k) -> k==2) 
			          . withStrategy(SEQ(lift(a0), ABORT() ))) ;
		
		agent .setGoal(topgoal);
		agent.update() ;    
		assertTrue(state.counter == 1) ;
		assertTrue(topgoal.getStatus().inProgress()) ;
		    
		agent.update() ;    
		assertTrue(topgoal.getStatus().failed()) ;
		assertTrue(agent.goal == null) ;
		assertTrue(agent.currentGoal == null) ; 
	}
	
	@Test
	public void test_with_multipleActions() {
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
	    
	    state.counter = 1 ;
	    topgoal = lift(goal("g").toSolve((Integer k) -> k==2) 
				  . withStrategy(SEQ(lift(a0),lift(a1)))) ;
		agent .setGoal(topgoal);
	    agent.update() ;  
	    assertTrue(state.counter == 1) ;
	    assertTrue(topgoal.getStatus().inProgress()) ;
	}
	
	
	@Test
	public void test_with_multipleGoals() {
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);
		var a0 = action("a0")
				 .do_((MyState S)->actionstate-> {S.counter++ ; return S.counter ; }) ;
		
		
		var g1 = lift(goal("g1").toSolve((Integer k) -> k==1) . withStrategy(lift(a0))) ;
		var g2 = lift(goal("g2").toSolve((Integer k) -> k==2) . withStrategy(lift(a0))) ;
		var g3 = lift(goal("g3").toSolve((Integer k) -> k==3) . withStrategy(lift(a0))) ;

		var topgoal = FIRSTof(SEQ(g1,g2), g3) ;
			
		agent .setGoal(topgoal);
		
	    agent.update() ;  		
	    assertTrue(state.counter == 1) ;	
	    assertTrue(topgoal.getStatus().inProgress()) ;	
	    assertTrue(g1.getStatus().success()) ;		
	    assertTrue(g2.getStatus().inProgress()) ;	
				
	    agent.update() ;  		
	    assertTrue(state.counter == 2) ;	
	    assertTrue(topgoal.getStatus().success()) ;	
	    assertTrue(g1.getStatus().success()) ;					
	    assertTrue(g2.getStatus().success()) ;	
	    assertTrue(g3.getStatus().inProgress()) ;			
	}
	
	@Test
	public void test_persistentAction() {
		// testing of action that insist on multiple ticks to execute
		
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);
		var a0 = action("a0")
				 .do_((MyState S)->actionstate-> {S.counter++ ; return S.counter ; })
				 .until_((MyState S)->actionstate-> S.counter == 2)
				 .lift() ;
		
		var a1 = action("a1")
				 .do_((MyState S)->actionstate-> {S.counter = -1 ; return S.counter ; }) 
				 .lift();
		
		var topgoal = lift(goal("g").toSolve((Integer k) -> k==-1) 
				      . withStrategy(SEQ(a0,a1))) ;
		
		agent .setGoal(topgoal);
		
	    agent.update() ;  
	    assertTrue(state.counter == 1) ;
		assertTrue(topgoal.getStatus().inProgress()) ;
		
	    agent.update() ;  
	    assertTrue(state.counter == 2) ;
		assertTrue(topgoal.getStatus().inProgress()) ;	
		
		agent.update() ;  
	    assertTrue(state.counter == -1) ;
		assertTrue(topgoal.getStatus().success()) ;	
		
		
	}

}
