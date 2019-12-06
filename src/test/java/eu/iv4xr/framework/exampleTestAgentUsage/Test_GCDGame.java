package eu.iv4xr.framework.exampleTestAgentUsage;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static eu.iv4xr.framework.Iv4xrEDSL.* ;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector ;
import static eu.iv4xr.framework.mainConcepts.ObservationEvent.* ;


import static org.junit.jupiter.api.Assertions.* ;

import java.util.logging.Level;
import java.util.logging.Logger;

import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.agents.StateWithMessenger;
import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.Environment.EnvOperation;


/**
 * An example of using iv4xr test agents to test the correctness of the GCDGame.
 */
public class Test_GCDGame {
	
	//////////New State//////MyState Class/////////////
    // Define a new state-structure for the agent. For this example, we don't
	// actually need a new state-structure, but let's just pretend that we do.
    
	static class MyState extends StateWithMessenger {
		//int counter = 0 ;
		//String last = null ;
		MyState(){ super() ; }
		//int result = 0 ;
	}
	
	// this static variable will hold an instance of the Program-under-test (an instance
	// of GCDGame :
	static GCDGame gameUnderTest ;
	
	//////////New Environment//////GCDEnv Class///////////
	// Define your Environment to provide an interface between the test agent and the
	// Program-under-test:
	
	static class GCDEnv extends Environment {
		
		Logger logger = Logging.getAPLIBlogger() ;
		//int result=-1;
		int x ; 
		int y ;
		int gcd ;
		boolean win ;
		
		public GCDEnv() { super() ; }
		
		@Override
		public void refreshWorker() { 
			x = gameUnderTest.x ;
			y = gameUnderTest.y ;
			gcd = gameUnderTest.gcd ;
			win = gameUnderTest.win() ;
		}
		
		@Override
		protected Object sendCommand_(EnvOperation cmd) {
			logger.info("Command " + cmd.command);
			switch (cmd.command) {
			   case "up" : gameUnderTest.up() ; break ;
			   case "down" : gameUnderTest.down() ; break ;
			   case "right" : gameUnderTest.right() ; break ;
			   case "left" : gameUnderTest.left() ; break ;			
			}
			return null ;
		}
		
		@Override
		public String toString() { return "(" + x + "," + y + "), gcd=" + gcd ; }
	}

	
    // Construct a tactic to drive the player to position X,Y:
    Tactic navigateTo(int X, int Y) {
    	Action up = action("action_up")
    	   		 . do1((MyState S)-> { 
    	   			   S.env().sendCommand(null, null, "up",null);  
    	   			   S.env().refreshWorker() ;
    	   			   Logging.getAPLIBlogger().info("new state: " + S.env());
    	   			   return S ; }) ;
        Action down = action("action_down")
    	   		 . do1((MyState S)-> { 
    	   			   S.env().sendCommand(null, null, "down",null);  
    	   			   S.env().refreshWorker() ;
    	   			   Logging.getAPLIBlogger().info("new state: " + S.env());
    	   			   return S ; }) ;
    	Action right = action("action_up")
    	   		 . do1((MyState S)-> { 
    	   			   S.env().sendCommand(null, null, "right",null);  
    	   			   S.env().refreshWorker() ;
    	   			   Logging.getAPLIBlogger().info("new state: " + S.env());
    	   			   return S ; }) ;
        Action left = action("action_left")
    	   		 . do1((MyState S)-> { 
    	   			   S.env().sendCommand(null, null, "left",null);  
    	   			   S.env().refreshWorker() ;
    	   			   Logging.getAPLIBlogger().info("new state: " + S.env());
    	   			   return S ; }) ;
    	   
    	return FIRSTof(
		    	   up   .on_((MyState S) -> ((GCDEnv)S.env()).y < Y).lift(),
		    	   down .on_((MyState S) -> ((GCDEnv)S.env()).y > Y).lift(),
		    	   right.on_((MyState S) -> ((GCDEnv)S.env()).x < X).lift(),	  
		    	   left .on_((MyState S) -> ((GCDEnv)S.env()).x > X).lift() ) ;
    }
	
    // parameterized test-case to test GCDGame. Given X and Y, this specifies the expected gcd value,
    // and whether the GCDGame should conclude a win or lose.
    public void parameterizedGCDGameTest(int X, int Y, int expectedGCD, boolean expectedWinConclusion) {

		gameUnderTest = new GCDGame() ;
		
		var state = (MyState) (new MyState().setEnvironment(new GCDEnv())) ;
		Logging.getAPLIBlogger().info("initial state: (" + gameUnderTest.x + ", " + gameUnderTest.y + ")");
		
		var agent=new TestAgent().attachState(state);
		var info = "test gcd(" + X + "," + Y + ")" ;
	   
	    // setting goals and tactic
		var topgoal = testgoal("tg").toSolve((MyState S) -> ((GCDEnv) S.env()).x==X && ((GCDEnv) S.env()).y==Y ) 
				      . withTactic(navigateTo(X,Y)) 
				      . oracle(agent, (MyState S) -> {
				    	  GCDEnv myenv = (GCDEnv) S.env();
				    	  if (myenv.gcd == expectedGCD && myenv.win == expectedWinConclusion) return new VerdictEvent("",info,true) ;
				    	  else return new VerdictEvent("",info,false) ;
				          } )
				      . lift() ;
		
		var dataCollector = new TestDataCollector() ;
	    agent. setTestDataCollector(dataCollector)
	         . setGoal(topgoal);
	     
	    // running the agent
	    while (!topgoal.getStatus().success()) {
	    	agent.update();
	    }
	    // check that it didn't found any error:
	    assertTrue(dataCollector.getNumberOfPassVerdictsSeen() == 1);
	    assertTrue(dataCollector.getNumberOfFailVerdictsSeen() == 0);
	}

    
	@Test
	public void test_1() {
		parameterizedGCDGameTest(1,1,1,true) ;
	}
	
	@Test
	public void test_0() {
		parameterizedGCDGameTest(0,0,0,false) ;

	}
		
	@Test
	public void test_2() {
		parameterizedGCDGameTest(12,0,12,false) ;
	}
	
	@Test
	public void test_3() {
		parameterizedGCDGameTest(0,9,9,false) ;
	}
	
	@Test
	public void test_4() {
		Logging.getAPLIBlogger().setUseParentHandlers(false);  
		parameterizedGCDGameTest(32*7,32*11,32,false) ; // Test_GCD(7966496,314080416) --> takes too long :)
		Logging.getAPLIBlogger().setUseParentHandlers(true);  
	}
	
	@Test
	public void test_5() {
		Logging.getAPLIBlogger().setUseParentHandlers(false);  
		parameterizedGCDGameTest(7,11*11,1,true) ; 
		Logging.getAPLIBlogger().setUseParentHandlers(true);  
	}
	
	/*
	@Test
	public void test_2() {
		
		var state = (MyState) (new MyState().setEnvironment(new GCDEnv())) ;
		var agent=new TestAgent().attachState(state);
	 
	    //Setting goal and action
	    var a0 = action("a0")
	    		 . do1((MyState S)-> { 
	    			   S.result=((GCDEnv)S.env()).Test_GCD(12, 0);  
	    			   return S.result; })
	    		 . lift() ;
	    
		var topgoal = goal("g").toSolve((Integer k) -> k==12) 
				      . withTactic(a0) 
				      . lift() ;
	    agent.setGoal(topgoal);
	    agent.update();
	    assertTrue(state.result==12) ;
	    assertTrue(topgoal.getStatus().success()) ;
	    assertTrue(agent.goal == null) ;
	    
	    
	}
	@Test
	public void test_3() {
		
		var state = (MyState) (new MyState().setEnvironment(new GCDEnv())) ;
		var agent=new TestAgent().attachState(state);
	 
	    //Setting goal and action
	    var a0 = action("a0")
	    		 . do1((MyState S)-> { 
	    			   S.result=((GCDEnv)S.env()).Test_GCD(0, 9);  
	    			   return S.result; })
	    		 . lift() ;
	    
		var topgoal = goal("g").toSolve((Integer k) -> k==9) 
				      . withTactic(a0) 
				      . lift() ;
	    agent.setGoal(topgoal);
	    agent.update();
	    assertTrue(state.result==9) ;
	    assertTrue(topgoal.getStatus().success()) ;
	    assertTrue(agent.goal == null) ;
	    
	    
	}	

	@Test
	public void test_4() {
		
		var state = (MyState) (new MyState().setEnvironment(new GCDEnv())) ;
		var agent=new TestAgent().attachState(state);
	 
	    //Setting goal and action
	    var a0 = action("a0")
	    		 . do1((MyState S)-> { 
	    			   S.result=((GCDEnv)S.env()).Test_GCD(7966496,314080416);  
	    			   return S.result; })
	    		 . lift() ;
	    
		var topgoal = goal("g").toSolve((Integer k) -> k==32) 
				      . withTactic(a0) 
				      . lift() ;
	    agent.setGoal(topgoal);
	    agent.update();
	    assertTrue(state.result==32) ;
	    assertTrue(topgoal.getStatus().success()) ;
	    assertTrue(agent.goal == null) ;
	    
	    
	}	

*/
}

		

