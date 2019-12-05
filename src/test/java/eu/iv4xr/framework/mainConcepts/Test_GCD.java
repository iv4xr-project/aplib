package eu.iv4xr.framework.mainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import java.util.logging.Logger;

import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.mainConcepts.*;


public class Test_GCD {
	
	//////////New State//////MyState Class/////////////
	
	static class MyState extends SimpleState {
		int counter = 0 ;
		String last = null ;
		MyState(){ super() ;
			
		}
		int result = 0 ;
	}
	
	//////////New Environment//////GCDEnv Class///////////
	
	static class GCDEnv extends Environment {
		
		protected Logger logger = Logging.getAPLIBlogger() ;
		int result=-1;
		
		public GCDEnv() { super() ; }
		
		public int Test_GCD(int x,int y)
		{

			GCD g=new GCD();
			result=g.Calculate_GCD(x, y);
			return result;
		}		

	}

	@Test
	public void test_1() {
		
		var state = (MyState) (new MyState().setEnvironment(new GCDEnv())) ;
		var agent=new TestAgent().attachState(state);
	 
	    //Setting goal and action
	    var a0 = action("a0")
	    		 . do1((MyState S)-> { 
	    			   S.result=((GCDEnv)S.env()).Test_GCD(0, 0);  
	    			   return S.result; })
	    		 . lift() ;
	    
		var topgoal = goal("g").toSolve((Integer k) -> k==0) 
				      . withTactic(a0) 
				      . lift() ;
	    agent.setGoal(topgoal);
	    agent.update();
	    assertTrue(state.result==0) ;
	    assertTrue(topgoal.getStatus().success()) ;
	    assertTrue(agent.goal == null) ;
	    
	}
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


}

		

