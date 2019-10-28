package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import nl.uu.cs.aplib.Environments.ConsoleEnvironment;
import nl.uu.cs.aplib.MainConcepts.GoalStructure.*;
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
	    var a0 = action("a0")
	    		 . do1((MyState S)-> {S.counter++ ; return S.counter ; })
	    		 . lift() ;
		var topgoal = goal("g").toSolve((Integer k) -> k==2) 
				      . withTactic(a0) 
				      . lift() ;
		
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
		
		// scenario with just a single goal, which is aborted:
		var a0 = action("a0")
				 . do1((MyState S)-> {S.counter++ ; return S.counter ; })
				 . lift() ;
		
		var topgoal =  goal("g").toSolve((Integer k) -> k==2) 
			          . withTactic(SEQ(a0, ABORT() )) 
			          . lift() ;
		
		agent.setGoal(topgoal);
		agent.update() ;    
		assertTrue(state.counter == 1) ;
		assertTrue(topgoal.getStatus().inProgress()) ;
		    
		agent.update() ;    
		assertTrue(topgoal.getStatus().failed()) ;
		assertTrue(agent.goal == null) ;
		assertTrue(agent.currentGoal == null) ; 
		
		
		// a scenario FIRSTof(g1,g2) ... g1 is aborted
		state.counter = 0 ;
		var g1 = goal("g1").toSolve((Integer k) -> true).withTactic(ABORT()).lift() ;
		var g2 = goal("g2").toSolve((Integer k) -> k==1).withTactic(a0).lift() ;
		var topgoal2 = FIRSTof(g1,g2) ;
		
		agent.setGoal(topgoal2);
		
		agent.update() ;    
		assertTrue(state.counter == 0) ;
		assertTrue(g1.getStatus().failed()) ;
		assertTrue(topgoal2.getStatus().inProgress()) ;
		
		assertTrue(agent.currentGoal.goal.name == "g2") ;
		
		agent.update();
		topgoal2.printGoalStructureStatus();
		//System.out.println("*** " + state.counter) ;
		assertTrue(state.counter == 1) ;
		assertTrue(g1.getStatus().failed()) ;
		assertTrue(g2.getStatus().success()) ;	
		assertTrue(topgoal2.getStatus().success()) ;	
	}
	
	
	@Test
	public void test_with_multipleActions() {
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new BasicAgent() .attachState(state);
		var a0 = action("a0")
				 . do1((MyState S)-> {S.counter++ ; S.last = "a0" ; return S.counter ; })
				 . on_((MyState S) -> S.counter % 2 == 0) 
				 . lift() ;
		
		var a1 = action("a1")
				. do1((MyState S)-> {S.counter++ ; S.last = "a1" ; return S.counter ; })
				. on_((MyState S) -> S.counter % 2 == 1) 
				. lift() ;	
		
		var topgoal = goal("g").toSolve((Integer k) -> k==2) 
				      . withTactic(FIRSTof(a0,a1))
				      . lift() ;
		
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
	    topgoal = goal("g").toSolve((Integer k) -> k==2) 
				  . withTactic(SEQ(a0,a1))
				  . lift() ;
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
				 . do1((MyState S)-> {S.counter++ ; return S.counter ; })
				 . lift();
			
		var g1 = goal("g1").toSolve((Integer k) -> k==1) . withTactic(a0) . lift() ;
		var g2 = goal("g2").toSolve((Integer k) -> k==2) . withTactic(a0) . lift()  ;
		var g3 = goal("g3").toSolve((Integer k) -> k==3) . withTactic(a0) . lift()  ;

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
				 .do1((MyState S)-> {S.counter++ ; return S.counter ; })
				 .until((MyState S)-> S.counter == 2)
				 .lift() ;
		
		var a1 = action("a1")
				 .do1((MyState S)-> {S.counter = -1 ; return S.counter ; }) 
				 .lift();
		
		var topgoal = goal("g").toSolve((Integer k) -> k==-1) 
				      . withTactic(SEQ(a0,a1))
				      . lift() ;
		
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
	
	
	// test whether H contains G
	private boolean contains(GoalStructure H, GoalStructure G) {
		if (H==G) return true ;
		for (GoalStructure H2: H.subgoals) {
			if (contains(H2,G)) return true ;
		}
		return false ;
	}
	
	@Test
	public void test_addingAGoal() {
		
		var g = goal("g")
				.toSolve((Integer i) -> i == 5)
				.withTactic(action("a")
						.do1((MyState S) -> { S.counter++ ; return S.counter ; })
						.lift())
				.lift() ;
		
		var g0 = goal("g0")
				.toSolve((Integer i) -> i == 2)
				.withTactic(action("a")
						.do1((MyState S) -> { S.counter++ ; return S.counter ; })
						.lift())
				.lift() ;
			
		var gnew = goal("new")
				.toSolve((Integer i) -> i == 4)
				.withTactic(action("a")
						.do1((MyState S) -> { S.counter++ ; return S.counter ; })
						.lift())
				.lift() ;
		
		// Scenario 1: trying to add a goal on a singleton goal; should throw an exception:
		var agent = new BasicAgent()
				.attachState(new MyState())
				.setGoal(g) ;
		
		assertTrue(agent.currentGoal == g) ;
		
		try {
			agent.addGoalStructure(gnew);
			assertTrue(false) ;
		}
		catch(IllegalArgumentException e) { assertTrue(true) ; }
		
		// Scenario 2: the current goal is the last child of some parent combinator:
		var grepeat = REPEAT(g) ;
		agent.setGoal(grepeat) ;
		assertTrue(agent.currentGoal == g) ;
		
		agent.addGoalStructure(gnew);
		assertTrue(agent.currentGoal == g) ;
		assertTrue(contains(grepeat,gnew)) ;
		assertTrue(grepeat.subgoals.indexOf(g) == 0) ;
		assertTrue(grepeat.subgoals.indexOf(gnew) == 1) ;

		// Scenario 3: the current goal is NOT the last child of some parent combinator:
		var gseq = SEQ(g0,g) ;
		agent.setGoal(gseq) ;
		assertTrue(agent.currentGoal == g0) ;
		
		agent.addGoalStructure(gnew);
		assertTrue(agent.currentGoal == g0) ;
		assertTrue(contains(gseq,gnew)) ;
		assertTrue(gseq.subgoals.indexOf(g0) == 0) ;
		assertTrue(gseq.subgoals.indexOf(gnew) == 1) ;
		assertTrue(gseq.subgoals.indexOf(g) == 2) ;
		
	}

}
