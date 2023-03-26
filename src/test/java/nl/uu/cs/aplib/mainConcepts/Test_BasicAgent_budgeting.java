package nl.uu.cs.aplib.mainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import nl.uu.cs.aplib.environments.ConsoleEnvironment;
import nl.uu.cs.aplib.mainConcepts.BasicAgent;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.*;
import nl.uu.cs.aplib.mainConcepts.Test_BasicAgent.MyState;
import nl.uu.cs.aplib.utils.Time;

public class Test_BasicAgent_budgeting {

    static class MyState extends SimpleState {
        int counter = 0;
        String last = null;

        MyState() {
            super();
        }
    }

    void sleepx(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
        }
        ;
    }

    @Test
    public void test_simplescenario() {
        // test with a single action and one simple goal, with the action exhausting the
        // budget.

        var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment()));
        var agent = new BasicAgent().attachState(state);

        // an over budget scenario:
        var a0 = action("a0").do1((MyState S) -> {
            S.counter++;
            return S.counter;
        });

        var topgoal = goal("g").toSolve((Integer k) -> k == 3).withTactic(a0.lift())
        				.lift()
        				.maxbudget(2);

        agent.setGoal(topgoal);
        // System.out.println(">>> alocated topgoal " + topgoal.allocatedBudget) ;
        agent.update();
        // we should now still be within budget:
        assertTrue(topgoal.getStatus().inProgress());

        // the next update will exhaust the budget:
        agent.update();
        assertTrue(topgoal.getStatus().failed());
        assertTrue(agent.goalstack.isEmpty());

        // exactly at budget scenario:
        state.counter = 0;
        a0 = action("a0").do1((MyState S) -> {
            S.counter++;
            return S.counter;
        });

        topgoal = goal("g").toSolve((Integer k) -> k == 3).withTactic(a0.lift())
        			.lift()
        			.maxbudget(3);

        agent.setGoal(topgoal);
        agent.update();
        assertTrue(topgoal.getStatus().inProgress());
        agent.update();
        assertTrue(topgoal.getStatus().inProgress());
        agent.update();
        assertTrue(topgoal.getStatus().success());
        assertTrue(agent.goalstack.isEmpty()) ;
    }

    @Test
    public void test_exhaustingbudget_on_multiplegoals() {

        // a goal of the form FIRSTof(g1,g2), where g1 exhausts its budget

        var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment()));
        var agent = new BasicAgent().attachState(state);

        // an action that isn't working towards its goal, hence wasting budget:
        var a0 = action("a0").do1((MyState S) -> {
            S.counter = 5;
            return S.counter;
        });

        var a1 = action("a1").do1((MyState S) -> {
            S.counter++;
            return S.counter;
        });

        var g1 = goal("g1").toSolve((Integer k) -> k == 6).withTactic(a0.lift())
        		.lift()
        		.maxbudget(2);
        var g2 = goal("g2").toSolve((Integer k) -> k == 7).withTactic(a1.lift()).lift();
        var topgoal = FIRSTof(g1, g2).maxbudget(10);

        agent.setGoal(topgoal);

        topgoal.printGoalStructureStatus();

        assertTrue(agent.goalstack.currentPrimitiveGoal() == g1);

        agent.update();
        assertTrue(agent.goalstack.currentPrimitiveGoal() == g1);
        assertTrue(g1.getStatus().inProgress());

        agent.update();
        assertTrue(agent.goalstack.currentPrimitiveGoal() == g2);
        assertTrue(g1.getStatus().failed());
        assertTrue(g2.getStatus().inProgress());

        agent.update();
        assertTrue(g2.getStatus().inProgress());

        agent.update();
        assertTrue(g2.getStatus().success());
        assertTrue(agent.goalstack.isEmpty());

        topgoal.printGoalStructureStatus();
    }

}
