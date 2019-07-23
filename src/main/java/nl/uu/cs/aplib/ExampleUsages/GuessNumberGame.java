package nl.uu.cs.aplib.ExampleUsages;

import nl.uu.cs.aplib.Environments.SimpleSystemConsoleEnv;
import nl.uu.cs.aplib.MainConcepts.*;
import static nl.uu.cs.aplib.AplibEDSL.* ;

import java.util.*;

public class GuessNumberGame  {
	
	static public class MyAgentState extends SimpleState {
		
		Random rnd = new Random() ;
		
		boolean[] excluded = { false, false, false , false , false,
	                           false, false, false , false , false, false } ;
		
		List<Integer> getPossibilities() {
		  var candidates = new LinkedList<Integer>() ;
		  for(int k=0;k<11; k++) 
		     if(!excluded[k]) candidates.add(k) ;
		  return candidates ;
		}
		
		@Override
		public SimpleSystemConsoleEnv env() { return (SimpleSystemConsoleEnv) super.env() ; }
		
	}
	
	static private Integer toInt(Object o) {
		try {
			return Integer.parseInt((String) o) ;
		}
		catch(Exception e) { return null ; }
	}

	
	static public void main(String[] args) {
		
	  // specifying the goal:	
      Goal g = goal("the-goal")
        	   .toSolve(o -> { 
        	     var status = (String) o ;
        	     return (status.equals("out of range") || status.equals("y")) ; }) ;	
		
      // specifying the actions for the agent:
	  Action asklb = action("askLowerBound")
		.do_(agentstate_ -> actionstate_ -> {
		  var agentstate = (MyAgentState) agentstate_ ;
		  agentstate.env().println("Type a number less or equal to your number:");
		  var o = agentstate.env().readln() ;
		  var i = toInt(o) ;
		  if (i!=null) {
			  //System.out.println("## " + i) ;
              for (int k=0; k<Math.max(0,i); k++)  agentstate.excluded[k] = true ;
		  }
		  actionstate_.markAsCompleted();
		  return null ;
	  }) ;
	  
      Action guess = action("guess")
        .do_(agentstate_ -> actionstate_ -> {
          var agentstate = (MyAgentState) agentstate_ ;	
          var candidates = agentstate.getPossibilities() ;
          if(candidates.isEmpty()) {
             agentstate.env().println("Your number is NOT in [0..10]!") ;
    		 return "out of range" ;
          }
          int x = candidates.get(agentstate.rnd.nextInt(candidates.size())) ;
          agentstate.env().println("Is it " + x + "? (answer y/n)") ;
          var o = agentstate.env().readln() ;
          // pre-emptively scrapping the guessed number from candidate:
          agentstate.excluded[x] = true ;
          actionstate_.markAsCompleted();
          return o ;	 
      }) ;
        
      // specifying the strategy to solve the goal:  
      Strategy strategy = SEQ(lift(asklb),lift(guess)) ;
      
      // creating the agent, and configuring it:
      GoalTree topgoal = lift(g.withStrategy(strategy)) ;
      var state = (MyAgentState) (new MyAgentState() . setEnvironment(new SimpleSystemConsoleEnv())) ;      
      var agent = new BasicAgent() . attachState(state) . setGoal(topgoal) ;

      state.env().println("Think a number in the interval [0..10].");
      while (topgoal.getStatus() == ProgressStatus.INPROGRESS) {
    	  agent.update(); 
      }
      if(g.getStatus() == ProgressStatus.SUCCESS) 
    	  state.env().println("Goal solved!") ;

	}

}
