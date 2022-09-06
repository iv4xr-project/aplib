package nl.uu.cs.aplib.exampleUsages;

import nl.uu.cs.aplib.environments.ConsoleEnvironment;
import nl.uu.cs.aplib.mainConcepts.*;

import static nl.uu.cs.aplib.AplibEDSL.*;

import java.util.*;

/**
 * In this demo we create an an agent that tries to guess a magic number that
 * the user has in mind through a series of question/answers :)
 *
 * Run the main method to run this demo.
 * 
 * @author wish
 *
 */
public class GuessNumberGame {

    /**
     * We define a custom agent-state to keep track which numbers it knows for sure
     * cannot be the user's magic number.
     */
    static public class MyAgentState extends SimpleState<ConsoleEnvironment> {

        Random rnd = new Random();

        // b[i] = true means that i cannot be the magic number
        boolean[] excluded = { false, false, false, false, false, false, false, false, false, false, false };

        List<Integer> getPossibilities() {
            var candidates = new LinkedList<Integer>();
            for (int k = 0; k < 11; k++)
                if (!excluded[k])
                    candidates.add(k);
            return candidates;
        }

    }

    // just some help method
    static private Integer toInt(Object o) {
        try {
            return Integer.parseInt((String) o);
        } catch (Exception e) {
            return null;
        }
    }

    static public void main(String[] args) { // run this to run the demo

        // specifying the goal to solve:
        Goal g = goal("the-goal").toSolve((String p) -> p.equals("y") || p.equals("out of range"));

        // defining the actions for the agent:
        var asklb = action("askLowerBound").desc("Ask user to give a number less or equal that his secret number.")
                .do1((MyAgentState belief) -> {
                    var o = belief.env().ask("Type a number less or equal to your number:");
                    var i = toInt(o);
                    // the agent infers and adds new facts:
                    if (i != null) {
                        // System.out.println("## " + i) ;
                        for (int k = 0; k < Math.max(0, i); k++)
                            belief.excluded[k] = true;
                    }
                    return "";
                }).lift();

        var guess = action("guess")
                .desc("Guessing the secret number and ask the user to confirm if it is right or wrong.")
                .do1((MyAgentState belief) -> {
                    // the agent performs some inference:
                    var candidates = belief.getPossibilities();
                    if (candidates.isEmpty()) {
                        belief.env().println("Your number is NOT in [0..10]!");
                        return "out of range";
                    }
                    int x = candidates.get(belief.rnd.nextInt(candidates.size()));
                    var o = belief.env().ask("Is it " + x + "? (answer y/n)");
                    // pre-emptively scrapping the guessed number from candidate:
                    belief.excluded[x] = true;
                    return o;
                }).lift();

        // specifying the tactic to solve the goal:
        g.withTactic(SEQ(asklb, guess));

        // creating an agent, attaching state to it, and the above topgoal to it:
        GoalStructure topgoal = g.lift().maxbudget(100000);
        var belief = new MyAgentState();
        var agent = new BasicAgent()
        			.attachState(belief)
        			.attachEnvironment(new ConsoleEnvironment())
        			.setGoal(topgoal);

        // now, run the agent :
        belief.env().println("Think a secret number in the interval [0..10] ...");
        while (topgoal.getStatus().inProgress()) {
            // System.err.println("##" + topgoal.getStatus()) ;
            agent.update();
        }
        topgoal.printGoalStructureStatus();
        g.getTactic().printStatistics();

    }

}
