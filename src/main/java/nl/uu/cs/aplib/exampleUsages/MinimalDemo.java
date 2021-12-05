package nl.uu.cs.aplib.exampleUsages;

import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;

import java.util.Random;

import nl.uu.cs.aplib.environments.ConsoleEnvironment;
import nl.uu.cs.aplib.mainConcepts.BasicAgent;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

/**
 * This demo will create an agent whose goal is to guess a magic number (it is
 * the number 10, but the agent doesn't know that). The agent is not very smart,
 * so it simply repeatedly proposes a random number until it guesses the right
 * one.
 * 
 * Run the method main to run this demo.
 * 
 * @author wish
 *
 */
public class MinimalDemo {

    static public void main(String[] args) throws InterruptedException { // run this to run the demo
        // specifying the goal to solve:
        Goal g = goal("Guess a the magic number (10)").toSolve((Integer x) -> x == 10);

        Random rnd = new Random();

        // defining a single action as the goal solver:
        var guessing = action("guessing").do1((SimpleState belief) -> {
            int x = rnd.nextInt(11);
            ((ConsoleEnvironment) belief.env()).println("Proposing " + x + " ...");
            return x;
        }).lift();

        // attach the action to the goal, and make it a goal-structure:
        GoalStructure topgoal = g.withTactic(guessing).lift();

        // creating an agent; attaching a fresh state to it, and attaching the above
        // goal to it:
        var agent = new BasicAgent().attachState(new SimpleState().setEnvironment(new ConsoleEnvironment()))
                .setGoal(topgoal);

        // run the agent until it solves its goal:
        while (topgoal.getStatus().inProgress()) {
            agent.update();
            Thread.sleep(1500);
        }
        topgoal.printGoalStructureStatus();
    }
}
