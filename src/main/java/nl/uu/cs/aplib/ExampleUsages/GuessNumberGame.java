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
		
	  // specifying the goal to solve:	
      Goal g = goal("the-goal")
        	   .toSolve((String p) -> p.equals("out of range") || p.equals("y")) ;	
		
      // defining the actions for the agent:
	  var asklb = action("askLowerBound")
		.desc("Ask user to give a number less or equal that his secret number.")
		.do1_((MyAgentState belief) -> actionstate_ -> {
		  var o = belief.env().ask("Type a number less or equal to your number:");
		  var i = toInt(o) ;
		  // the agent infers and adds new facts:
		  if (i!=null) {
			  //System.out.println("## " + i) ;
              for (int k=0; k<Math.max(0,i); k++)  belief.excluded[k] = true ;
		  }
		  return "" ;
	      })
		.lift() ;
	  
      var guess = action("guess")
    	.desc("Guessing the secret number and ask the user to confirm if it is right or wrong.")
        .do1_((MyAgentState belief) -> actionstate_ -> {
          // the agent performs some inference:
          var candidates = belief.getPossibilities() ;
          if(candidates.isEmpty()) {
        	 belief.env().println("Your number is NOT in [0..10]!") ;
    		 return "out of range" ;
          }
          int x = candidates.get(belief.rnd.nextInt(candidates.size())) ;
          var o = belief.env().ask("Is it " + x + "? (answer y/n)") ;
          // pre-emptively scrapping the guessed number from candidate:
          belief.excluded[x] = true ;
          return o ;	 
          })
        .lift() ;
        
      // specifying the strategy to solve the goal:  
      Strategy strategy = SEQ(asklb,guess) ;
      
      // creating the agent, and configuring it:
      GoalTree topgoal = lift(g.withStrategy(strategy)) ;
      var belief = new MyAgentState() ;
      belief.setEnvironment(new SimpleSystemConsoleEnv()) ;      
      var agent = new BasicAgent() . attachState(belief) . setGoal(topgoal) ;

      // now, run the agent :
      belief.env().println("Think a secret number in the interval [0..10] ...");
      while (topgoal.getStatus().inProgress()) {
    	  agent.update(); 
      }
      if(g.getStatus().sucess()) belief.env().println("Goal solved!") ;

	}

}
