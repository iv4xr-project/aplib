package nl.uu.cs.aplib.mainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;

public class Test_DynamicGoalStructure {

    static class MyState extends SimpleState {
        int x = 0;

        void reset() {
            x = 0;
        }
        
        @Override
        public void updateState(String agentId) {
        	// do nothing since we dont have a real env
        }
    }

    static Goal mk_goal1() {
        Tactic increase = action("incr").do1((MyState st) -> {
            st.x++;
            return st;
        }).lift();
        return goal("g1").toSolve((MyState st) -> st.x > 2).withTactic(increase);
    }

    static Goal mk_goal2() {
        Tactic decrease = action("decr").do1((MyState st) -> {
            st.x--;
            return st;
        }).lift();
        return goal("g2").toSolve((MyState st) -> st.x < -2).withTactic(decrease);
    }

    @Test
    public void test_IFELSE_goalstructure() {

        // attach a state and environment; the environment is here just a dummy that
        // does not do anything
        // relevant for the test:
        MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());

        Goal g1 = mk_goal1();
        Goal g2 = mk_goal2();

        // testing when we branch to the then-part:
        GoalStructure mygoal = IFELSE((MyState st) -> st.x == 0, g1.lift(), g2.lift());

        agent.setGoal(mygoal);

        int k = 0;
        while (k < 4) {
            agent.update();
            System.out.println("** " + k + ": state.x = " + state.x);
            k++;
        }
        assertTrue(mygoal.status.success());
        assertTrue(g1.status.success());
        assertFalse(g2.status.success());

        mygoal.printGoalStructureStatus();

        // testing when we branch to the else-part:

        state.reset();
        g1 = mk_goal1();
        g2 = mk_goal2();
        mygoal = IFELSE((MyState st) -> st.x != 0, g1.lift(), g2.lift());

        agent.setGoal(mygoal);

        k = 0;
        while (k < 4) {
            agent.update();
            System.out.println("** " + k + ": state.x = " + state.x);
            k++;
        }
        assertTrue(mygoal.status.success());
        assertFalse(g1.status.success());
        assertTrue(g2.status.success());

        mygoal.printGoalStructureStatus();

    }

    /**
     * Test WHILE(p,g)for the scenario where the loop terminates because g succeeds.
     */
    @Test
    public void test_WHILE_goalstructure_exit_by_bodysuccess() {
        MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());

        Tactic increase = action("incr").do1((MyState st) -> {
            st.x++;
            return st;
        }).lift();
        Goal g0 = goal("multiple3").toSolve((MyState st) -> st.x % 3 == 0).withTactic(increase);

        GoalStructure mygoal = WHILEDO((MyState st) -> st.x < 9, SEQ(g0.lift()));

        agent.setGoal(mygoal);

        state.x = 1;

        int k = 0;
        while (k < 6) {
            agent.update();
            System.out.println("** " + k + ": state.x = " + state.x);
            k++;
        }

        assertTrue(state.x == 3);
        assertTrue(mygoal.status.success());

        mygoal.printGoalStructureStatus();

    }

    /**
     * Test WHILE(p,g) for the scenario where the loop terminates because the guard
     * becomes false.
     */
    @Test
    public void test_WHILE_goalstructure_exit_by_guard_false() {
        MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());

        Tactic increase = action("incr").do1((MyState st) -> {
            st.x++;
            return st;
        }).lift();
        Goal g0 = goal("multiple3").toSolve((MyState st) -> st.x % 3 == 0).withTactic(increase);

        GoalStructure mygoal = WHILEDO((MyState st) -> st.x < 9, SEQ(g0.lift(), FAIL()));

        agent.setGoal(mygoal);

        state.x = 1;

        int k = 0;
        while (k < 17) {
            agent.update();
            System.out.println("** " + k + ": state.x = " + state.x);
            k++;
        }

        assertTrue(state.x == 9);
        assertTrue(mygoal.status.success());

        mygoal.printGoalStructureStatus();

    }

}
