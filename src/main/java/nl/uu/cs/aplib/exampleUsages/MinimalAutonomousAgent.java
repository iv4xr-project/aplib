package nl.uu.cs.aplib.exampleUsages;

import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;

import java.util.Random;

import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.agents.AutonomousBasicAgent;
import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.environments.ConsoleEnvironment;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

public class MinimalAutonomousAgent {

    static public void main(String[] args) {

        // specifying the goal to solve:
        Goal g = goal("Guess a the magic number (4)").toSolve((Integer x) -> x == 4);

        Random rnd = new Random();

        // defining a single action as the goal solver:
        var guessing = action("guessing").do1((SimpleState<ConsoleEnvironment> belief) -> {
            int x = rnd.nextInt(5);
            belief.env().println("Proposing " + x + " ...");
            return x;
        }).lift();

        // attach the action to the goal, and make it a goal-tree:
        GoalStructure topgoal = g.withTactic(guessing).lift();

        System.err.println("** Main thread " + Thread.currentThread().getId());

        // configure logging:
        Logging.addSystemErrAsLogHandler();

        // creating an agent; attaching a fresh state to it, and attaching the above
        // goal to it:
        var agent = new AutonomousBasicAgent() ;
        
        agent.attachState(new State<ConsoleEnvironment>())
             .attachEnvironment(new ConsoleEnvironment())
        	 .setGoal(topgoal) ;
        agent.setSamplingInterval(1000);

        // run the agent, autonomously on its own thread:
        new Thread(() -> agent.loop()).start();

        // while this main thread waits until the goal is concluded:
        var gt = agent.waitUntilTheGoalIsConcluded();
        gt.printGoalStructureStatus();
        agent.stop();
    }

}
