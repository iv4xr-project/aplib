package nl.uu.cs.aplib.mainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import nl.uu.cs.aplib.environments.ConsoleEnvironment;
import nl.uu.cs.aplib.mainConcepts.BasicAgent;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.*;
import nl.uu.cs.aplib.utils.Time;

public class Test_BasicAgent {

    static class MyState extends SimpleState {
        int counter = 0;
        String last = null;

        MyState() {
            super();
        }
        
        @Override
        public void updateState(String agentId) {
        	// do nothing since we dont have a real env
        }
    }

    @Test
    public void test_with_oneaction() {
        var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment()));
        var agent = new BasicAgent().attachState(state);

        // testing update with no goal:
        agent.update();
        assertTrue(agent.goal == null);
        assertTrue(agent.currentGoal == null);

        // ok let's now give a goal:
        var a0 = action("a0").do1((MyState S) -> {
            S.counter++;
            return S.counter;
        }).lift();
        var topgoal = goal("g").toSolve((Integer k) -> k == 2).withTactic(a0).lift();

        agent.setGoal(topgoal);
        agent.update();
        assertTrue(state.counter == 1);
        assertTrue(topgoal.getStatus().inProgress());

        agent.update();
        assertTrue(state.counter == 2);
        assertTrue(topgoal.getStatus().success());
        assertTrue(agent.goal == null);
        assertTrue(agent.currentGoal == null);
    }

    @Test
    public void test_abort() {
        var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment()));
        var agent = new BasicAgent().attachState(state);

        // scenario with just a single goal, which is aborted:
        var a0 = action("a0").do1((MyState S) -> {
            S.counter++;
            return S.counter;
        }).lift();

        var topgoal = goal("g").toSolve((Integer k) -> k == 2).withTactic(SEQ(a0, ABORT())).lift();

        agent.setGoal(topgoal);
        agent.update();
        assertTrue(state.counter == 1);
        assertTrue(topgoal.getStatus().inProgress());

        agent.update();
        assertTrue(topgoal.getStatus().failed());
        assertTrue(agent.goal == null);
        assertTrue(agent.currentGoal == null);

        // a scenario FIRSTof(g1,g2) ... g1 is aborted
        state.counter = 0;
        var g1 = goal("g1").toSolve((Integer k) -> true).withTactic(ABORT()).lift();
        var g2 = goal("g2").toSolve((Integer k) -> k == 1).withTactic(a0).lift();
        var topgoal2 = FIRSTof(g1, g2);

        agent.setGoal(topgoal2);

        agent.update();
        assertTrue(state.counter == 0);
        assertTrue(g1.getStatus().failed());
        assertTrue(topgoal2.getStatus().inProgress());

        assertTrue(agent.currentGoal.goal.name.equals("g2"));

        agent.update();
        topgoal2.printGoalStructureStatus();
        // System.out.println("*** " + state.counter) ;
        assertTrue(state.counter == 1);
        assertTrue(g1.getStatus().failed());
        assertTrue(g2.getStatus().success());
        assertTrue(topgoal2.getStatus().success());
    }

    @Test
    public void test_with_multipleActions() {
        var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment()));
        var agent = new BasicAgent().attachState(state);
        var a0 = action("a0").do1((MyState S) -> {
            S.counter++;
            S.last = "a0";
            return S.counter;
        }).on_((MyState S) -> S.counter % 2 == 0).lift();

        var a1 = action("a1").do1((MyState S) -> {
            S.counter++;
            S.last = "a1";
            return S.counter;
        }).on_((MyState S) -> S.counter % 2 == 1).lift();

        var topgoal = goal("g").toSolve((Integer k) -> k == 2).withTactic(FIRSTof(a0, a1)).lift();

        agent.setGoal(topgoal);
        agent.update();
        assertTrue(state.counter == 1);
        assertTrue(state.last.equals("a0"));
        assertTrue(topgoal.getStatus().inProgress());
        agent.update();
        assertTrue(state.counter == 2);
        assertTrue(state.last.equals("a1"));
        assertTrue(topgoal.getStatus().success());

        state.counter = 1;
        topgoal = goal("g").toSolve((Integer k) -> k == 2).withTactic(SEQ(a0, a1)).lift();
        agent.setGoal(topgoal);
        agent.update();
        assertTrue(state.counter == 1);
        assertTrue(topgoal.getStatus().inProgress());
    }

    @Test
    public void test_with_multipleGoals() {
        var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment()));
        var agent = new BasicAgent().attachState(state);
        var a0 = action("a0").do1((MyState S) -> {
            S.counter++;
            return S.counter;
        }).lift();

        var g1 = goal("g1").toSolve((Integer k) -> k == 1).withTactic(a0).lift();
        var g2 = goal("g2").toSolve((Integer k) -> k == 2).withTactic(a0).lift();
        var g3 = goal("g3").toSolve((Integer k) -> k == 3).withTactic(a0).lift();

        var topgoal = FIRSTof(SEQ(g1, g2), g3);

        agent.setGoal(topgoal);

        agent.update();
        assertTrue(state.counter == 1);
        assertTrue(topgoal.getStatus().inProgress());
        assertTrue(g1.getStatus().success());
        assertTrue(g2.getStatus().inProgress());

        agent.update();
        assertTrue(state.counter == 2);
        assertTrue(topgoal.getStatus().success());
        assertTrue(g1.getStatus().success());
        assertTrue(g2.getStatus().success());
        assertTrue(g3.getStatus().inProgress());
    }

    @Test
    public void test_persistentAction() {
        // testing of action that insist on multiple ticks to execute

        var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment()));
        var agent = new BasicAgent().attachState(state);
        var a0 = action("a0").do1((MyState S) -> {
            S.counter++;
            return S.counter;
        }).until((MyState S) -> S.counter == 2).lift();

        var a1 = action("a1").do1((MyState S) -> {
            S.counter = -1;
            return S.counter;
        }).lift();

        var topgoal = goal("g").toSolve((Integer k) -> k == -1).withTactic(SEQ(a0, a1)).lift();

        agent.setGoal(topgoal);

        agent.update();
        assertTrue(state.counter == 1);
        assertTrue(topgoal.getStatus().inProgress());

        agent.update();
        assertTrue(state.counter == 2);
        assertTrue(topgoal.getStatus().inProgress());

        agent.update();
        assertTrue(state.counter == -1);
        assertTrue(topgoal.getStatus().success());
    }

    // test whether H contains G
    private boolean contains(GoalStructure H, GoalStructure G) {
        if (H == G)
            return true;
        for (GoalStructure H2 : H.subgoals) {
            if (contains(H2, G))
                return true;
        }
        return false;
    }

    @Test
    public void test_addingAGoal_withAddAfter() {

        var g = goal("g").toSolve((Integer i) -> i == 0).withTactic(action("a").do1((MyState S) -> S.counter).lift())
                .lift();

        var g0 = goal("g0").toSolve((Integer i) -> i == 1).withTactic(action("a").do1((MyState S) -> S.counter).lift())
                .lift();

        var gnew = goal("new").toSolve((Integer i) -> i == 2)
                .withTactic(action("a").do1((MyState S) -> S.counter).lift()).lift();

        // Scenario 1: trying to add a goal on a singleton goal; should throw an
        // exception:
        var agent = new BasicAgent().attachState(new MyState()).setGoal(g);

        assertTrue(agent.currentGoal == g);

        try {
            agent.addAfter(gnew);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        // Scenario 2: the current goal is the child of a REPEAT node
        var grepeat = REPEAT(g);
        agent.setGoal(grepeat);
        assertTrue(agent.currentGoal == g);

        agent.addAfter(gnew);
        assertTrue(agent.currentGoal == g);
        assertTrue(contains(grepeat, gnew));
        assertTrue(contains(grepeat, g));
        assertTrue(grepeat.subgoals.size() == 1);
        var gseq = grepeat.subgoals.get(0);
        assertTrue(gseq.combinator == GoalsCombinator.SEQ);
        assertTrue(gseq.subgoals.size() == 2);
        assertTrue(gseq.subgoals.indexOf(g) == 0);
        assertTrue(gseq.subgoals.indexOf(gnew) == 1);

        // Scenario 3: the current goal is NOT the last child of some parent combinator
        // (and not REPEAT):
        gseq = SEQ(g0);
        agent.setGoal(gseq);
        assertTrue(agent.currentGoal == g0);

        agent.addAfter(gnew);
        assertTrue(agent.currentGoal == g0);
        assertTrue(contains(gseq, gnew));
        assertTrue(gseq.subgoals.indexOf(g0) == 0);
        assertTrue(gseq.subgoals.indexOf(gnew) == 1);

        // Scenario 4: the current goal is NOT the last child of some parent combinator
        // (and not REPEAT):
        gseq = SEQ(g0, g);
        agent.setGoal(gseq);
        assertTrue(agent.currentGoal == g0);

        agent.addAfter(gnew);
        assertTrue(agent.currentGoal == g0);
        assertTrue(contains(gseq, gnew));
        assertTrue(gseq.subgoals.indexOf(g0) == 0);
        assertTrue(gseq.subgoals.indexOf(gnew) == 1);
        assertTrue(gseq.subgoals.indexOf(g) == 2);

        // simulating an agent executing a composite goal, and in the middle perform
        // addAfter. We check simulated execution would indeed progress to the newly
        // added goal, and when it is solved, the status is correctly propagated:
        var gtop = SEQ(g, g0);
        agent.setGoal(gtop);
        agent.attachEnvironment(new Environment());
        MyState state = (MyState) agent.state;
        assertTrue(agent.currentGoal == g);

        // simulating solving g0; the current goal should advance to g:
        agent.update();
        assertTrue(agent.currentGoal == g0);

        // simulating adding gnew after g; the current goal should remain g:
        agent.addAfter(gnew);
        assertTrue(agent.currentGoal == g0);

        // simulating solving g0; the current goal should advance to gnew
        state.counter = 1;
        agent.update();
        assertTrue(agent.currentGoal == gnew);

        // simulating solving gnew; the whole top-goal should now be solved as well:
        state.counter = 2;
        agent.update();
        assertTrue(gnew.getStatus().success());
        assertTrue(gtop.getStatus().success());

        // another simulation with REPEAT:
        g0 = goal("g0").toSolve((Integer i) -> i == 1).withTactic(action("a").do1((MyState S) -> S.counter).lift())
                .lift();

        gnew = goal("new").toSolve((Integer i) -> i == 2).withTactic(action("a").do1((MyState S) -> S.counter).lift())
                .lift();

        gtop = REPEAT(g0);
        agent.setGoal(gtop);
        state.counter = 0;
        assertTrue(agent.currentGoal == g0);
        gtop.printGoalStructureStatus();

        // simulate trying to solve g0, and fail. Because of the REPEAT
        // the current goal should still be g0:
        agent.update();
        assertTrue(agent.currentGoal == g0);
        assertTrue(g0.getStatus().inProgress());

        // simulating adding gnew:
        agent.addAfter(gnew);
        assertTrue(agent.currentGoal == g0);

        // simulating solving g0, current goal should advance to gnew
        state.counter = 1;
        agent.update();
        assertTrue(agent.currentGoal == gnew);

        // simulating solving gnew; this should now solve the top-goal:
        state.counter = 2;
        agent.update();
        assertTrue(gnew.getStatus().success());
        assertTrue(gtop.getStatus().success());

        gtop.printGoalStructureStatus();

    }

    @Test
    public void test_addingAGoal_withAddBefore() {
        var g = goal("g").toSolve((Integer i) -> i == 0)
                .withTactic(action("return counter").do1((MyState S) -> S.counter).lift()).lift();

        var g0 = goal("g0").toSolve((Integer i) -> i == 1)
                .withTactic(SEQ(action("return counter").do1((MyState S) -> S.counter).lift(), ABORT())) // deliberately
                                                                                                         // make the
                                                                                                         // goal to fail
                                                                                                         // if it is not
                                                                                                         // solved at
                                                                                                         // 1st cycle

                .lift();

        var gnew = goal("new").toSolve((Integer i) -> i == 2)
                .withTactic(action("new").do1((MyState S) -> S.counter).lift()).lift();

        // Scenario 1: trying to add a goal on a singleton goal; should throw an
        // exception:
        var agent = new BasicAgent().attachState(new MyState()).setGoal(g);
        assertTrue(agent.currentGoal == g);
        try {
            agent.addBefore(gnew);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        // Scenario 2: trying to add a goal before the current goal:
        var gtop = SEQ(g, g0);
        gtop.budget = 10;
        agent.setGoal(gtop);
        assertTrue(agent.currentGoal == g);
        assertTrue(g.parent == gtop);
        var gnew2 = FIRSTof(gnew);
        agent.addBefore(gnew2);
        // gtop.printGoalStructureStatus();
        assertTrue(agent.currentGoal == g);
        assertTrue(gtop.subgoals.get(0).combinator == GoalsCombinator.REPEAT);
        assertTrue(contains(gtop, g));
        assertTrue(contains(gtop, gnew2));
        assertTrue(contains(gtop, gnew));
        assertTrue(gnew2.parent.combinator == GoalsCombinator.SEQ);
        assertTrue(gnew2.parent.parent.combinator == GoalsCombinator.REPEAT);
        assertTrue(gnew2.parent.parent.parent == gtop);
        assertTrue(gnew2.parent.subgoals.get(0) == gnew2);
        assertTrue(gnew2.parent.subgoals.get(1) == g);
        assertTrue(gnew2.parent.parent.budget == 10);
        assertTrue(gnew2.parent.parent.bmax == 10);
        assertTrue(gnew2.parent.budget == 10);

        // Scenario 3: trying to add a goal that was already added:
        agent.addBefore(gnew2);
        // gtop.printGoalStructureStatus();
        assertTrue(agent.currentGoal == g);
        assertTrue(g.parent.subgoals.size() == 2);
        assertTrue(g.parent.subgoals.get(0) == gnew2);
        assertTrue(g.parent.subgoals.get(1) == g);

        // Simulating an agent executing a composite goal, and in the middle perform
        // addBefore. We check simulated execution would indeed cycle back to the newly
        // added goal, and when the added REPEAT body is solved, this is properly
        // propagated:
        gtop = SEQ(g, g0);
        agent.setGoal(gtop);
        agent.attachEnvironment(new Environment());
        assertTrue(agent.currentGoal == g);
        MyState state = (MyState) agent.state;

        // simulating solving subgoal g, advancing the current goal to g0
        agent.update();
        assertTrue(agent.currentGoal == g0);

        // simulating adding a new goal before g0; g0 should remain current:
        agent.addBefore(gnew);
        assertTrue(agent.currentGoal == g0);

        // simulating 2x step; since state.counter==0, g0 will abort on it on the 2nd
        // step.
        // the new goal should now become the current goal:
        agent.update();
        agent.update();
        assertTrue(agent.currentGoal == gnew);

        // simulating solving the new goal, the current goal should advance to g0
        state.counter = 2;
        agent.update();
        assertTrue(gnew.getStatus().success());
        assertTrue(agent.currentGoal == g0);

        // simulating solving g0, this should solve the top-goal:
        state.counter = 1;
        agent.update();
        assertTrue(g0.getStatus().success());
        assertTrue(gtop.getStatus().success());
        // gtop.printGoalStructureStatus();
    }

    @Test
    public void test_removingAGoal() {
        var g = goal("g").withTactic(action("a").do1((MyState S) -> 0).lift()).lift();

        var g0 = goal("g0").withTactic(action("a").do1((MyState S) -> 0).lift()).lift();

        var g1 = goal("g1").withTactic(action("a").do1((MyState S) -> 0).lift()).lift();

        var g2 = REPEAT(SEQ(g0, g1));
        var g3 = REPEAT(g);

        var gRoot = REPEAT(FIRSTof(g2, g3));
        var agent = new BasicAgent().attachState(new MyState()).setGoal(g);
        agent.setGoal(gRoot);
        assertTrue(agent.currentGoal == g0);

        try {
            agent.remove(g0);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            agent.remove(g2);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            agent.remove(gRoot);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        agent.remove(g);
        assertTrue(contains(gRoot, g0));
        assertFalse(contains(gRoot, g));
        assertFalse(contains(gRoot, g3));

    }

}
