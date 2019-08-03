package nl.uu.cs.aplib.ExampleUsages;

import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;

import java.util.Random;

import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.Agents.AutonomousBasicAgent;
import nl.uu.cs.aplib.Agents.StateWithMessanger;
import nl.uu.cs.aplib.Environments.ConsoleEnvironment;
import nl.uu.cs.aplib.MainConcepts.BasicAgent;
import nl.uu.cs.aplib.MainConcepts.Goal;
import nl.uu.cs.aplib.MainConcepts.GoalTree;
import nl.uu.cs.aplib.MainConcepts.SimpleState;

public class MinimalAutonomousAgent {
	
	static public void main(String[] args) {
		
		// specifying the goal to solve:
		  Goal g = goal("Guess a the magic number (10)").toSolve((Integer x) -> x == 4) ;
		  
		  Random rnd = new Random() ;
		   
		  // defining a single action as the goal solver:
		  var guessing = action("guessing")
				.do_((SimpleState belief) -> actionstate_ -> { 
					int x = rnd.nextInt(5) ;
					((ConsoleEnvironment) belief.env()).println("Proposing " + x + " ...");
					return x ;
					})
				.lift() ;
		
		  // attach the action to the goal, and make it a goal-tree:
		  GoalTree topgoal = g.withStrategy(guessing).lift() ;
		  
		  System.err.println("** Main thread " + Thread.currentThread().getId()) ;
		  
		  // configure logging:
		  Logging.addSystemErrAsLogger();

	      // creating an agent; attaching a fresh state to it, and attaching the above goal to it:
		  var agent = new AutonomousBasicAgent()
                    . attachState(new StateWithMessanger() .setEnvironment(new ConsoleEnvironment())) 
                    . setGoal(topgoal) 
                    . setSamplingInterval(1000) 
                    ;
		  
		  // run the agent, autonously on its own thread:
		  new Thread(() -> agent.loop()) . start();
		  
		  // while this main thread waits until the goal is concluded:
		  var gt = agent.waitUntilTheGoalIsConcluded() ;
		  gt.printTreeStatus();
		  agent.stop();
		
	}

}
