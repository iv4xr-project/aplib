package nl.uu.cs.aplib.mainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult.Status;

import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.mainConcepts.ProgressStatus.ProgressStatus_;

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
    
    void executeAgent(BasicAgent agent, int numSteps) {
    	MyState state = (MyState) agent.state() ;
    	int k = 0;
        while (k < numSteps) {
            agent.update();
            System.out.println("** " + k + ": state.x = " + state.x);
            k++;
        }
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
        executeAgent(agent,4) ;

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
        executeAgent(agent,4) ;

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
        executeAgent(agent,6) ;

        assertTrue(state.x == 3);
        assertTrue(mygoal.status.success());

        mygoal.printGoalStructureStatus();

    }

    /**
     * Test WHILE(p,g) for the scenario where the loop terminates because the guard p
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
        executeAgent(agent,17) ;

        assertTrue(state.x == 9);
        assertTrue(mygoal.status.success());

        mygoal.printGoalStructureStatus();

    }
    
    List<Goal> getPrimgoals(GoalStructure G) {
    	List<Goal> collected = new LinkedList<>() ;
    	getPrimgoalsWorker(collected,G) ;
    	return collected ;
    }
    
    private void getPrimgoalsWorker(List<Goal> collected, GoalStructure G) {
    	if (G instanceof PrimitiveGoal) {
    		var pg = (PrimitiveGoal) G ;
    		collected.add(pg.goal) ;
    		return ;
    	}
    	for (var H : G.subgoals) {
    		getPrimgoalsWorker(collected,H) ;
    	}
    }
    
    /**
     * Test that the dynamically deployed goal using DEPLOYonce is added and executed.
     */
    @Test
    public void test1_DEPLOYonce() {
    	MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());
        
        Tactic increase = action("inc").do1((MyState st) -> {
            st.x++ ;
            return st;
        }).lift();
        
        Goal g10 = goal("reach 10").toSolve((MyState st) -> st.x == 10).withTactic(increase);
        
        GoalStructure g15 = DEPLOYonce(agent, (MyState st) -> {
        	  int currentx = st.x ;
        	  GoalStructure G = goal("reach +5")
        			  .toSolve((MyState st2) -> st2.x == currentx + 5)
        			  .withTactic(increase)
        			  .lift() ;
        	  return G ;
        	
        }) ;
        
        GoalStructure G = SEQ(g10.lift(), g15) ;
        agent.setGoal(G) ;
        
        assertTrue(getPrimgoals(G).size() == 2) ;
        
        executeAgent(agent,20) ;
        
        assertTrue(state.x == 15) ;
        assertTrue(G.status.success()) ;
        assertTrue(getPrimgoals(G).size() == 3) ; // one new goal has been added
        G.printGoalStructureStatus();     
    }
    
    /**
     * Check that repeatedly executing DEPLOYonce will only deploy the new goal once.
     */
    @Test
    public void test2_DEPLOYonce() {
    	MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());
        
        Tactic increase = action("inc").do1((MyState st) -> {
            st.x++ ;
            return st;
        }).lift();
        Tactic reset = action("inc").do1((MyState st) -> {
            st.x = 2 ;
            return st;
        }).lift();
        
        
        GoalStructure gDyn = DEPLOYonce(agent, (MyState st) -> {
        	  int currentx = st.x ;
        	  GoalStructure G = goal("reach +5")
        			  .toSolve((MyState st2) -> st2.x == currentx + 5)
        			  .withTactic(increase)
        			  .lift() ;
        	  GoalStructure Reset = goal("reset")
        			  .toSolve((MyState st2) -> true)
        			  .withTactic(reset)
        			  .lift() ;
        	  return SEQ(G,Reset,FAIL()) ; // will add three prim-goals
        	
        }) ;
        
        GoalStructure G = REPEAT(gDyn) ;
        agent.setGoal(G) ;
        //System.out.println(">>> " + getPrimgoals(G).size()) ;
        assertTrue(getPrimgoals(G).size() == 1) ;
        
        executeAgent(agent,80) ;
       
        assertTrue(state.x <= 5) ;
        assertTrue(G.status.inProgress()) ;
        // three new prim-goals were added, but no more than that,
        // despite the REPEAT:
        assertTrue(getPrimgoals(G).size() == 4) ; 
        G.printGoalStructureStatus();     
    }
    
    /**
     * Test that the dynamically deployed goal using DEPLOY is added and executed. Moreover,
     * the new goal is removed afterwards, for case(1): when the new goal succeeds.
     */
    @Test
    public void test1_DEPLOY() {
    	MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());
        
        Tactic increase = action("inc").do1((MyState st) -> {
            st.x++ ;
            return st;
        }).lift();
        
        Goal g10 = goal("reach 10").toSolve((MyState st) -> st.x == 10).withTactic(increase);
        
        GoalStructure g15 = DEPLOY(agent, (MyState st) -> {
        	  int currentx = st.x ;
        	  GoalStructure G = goal("reach +5")
        			  .toSolve((MyState st2) -> st2.x == currentx + 5)
        			  .withTactic(increase)
        			  .lift() ;
        	  return G ;
        	
        }) ;
        
        GoalStructure G = SEQ(g10.lift(), g15) ;
        agent.setGoal(G) ;
        
        //G.printGoalStructureStatus();     
        //System.out.println(">>> " + getPrimgoals(G).size()) ;
        assertTrue(getPrimgoals(G).size() == 5) ;
        
        executeAgent(agent,20) ;
        
        assertTrue(state.x == 15) ;
        assertTrue(G.status.success()) ;
        assertTrue(getPrimgoals(G).size() == 5) ; // 
        G.printGoalStructureStatus();     
    }
    
    /**
     * Test that the dynamically deployed goal using DEPLOY is added and executed. Moreover,
     * the new goal is removed afterwards, for case(2): when the new goal fails.
     */
    @Test
    public void test2_DEPLOY() {
    	MyState state = new MyState();
        BasicAgent agent = new BasicAgent().attachState(state).attachEnvironment(new Environment());
        
        Tactic increase = action("inc").do1((MyState st) -> {
            st.x++ ;
            return st;
        }).lift();
        
        Goal g10 = goal("reach 10").toSolve((MyState st) -> st.x == 10).withTactic(increase);
        
        GoalStructure g15 = DEPLOY(agent, (MyState st) -> {
        	  int currentx = st.x ;
        	  GoalStructure G = goal("reach +5")
        			  .toSolve((MyState st2) -> st2.x == currentx + 5)
        			  .withTactic(increase)
        			  .lift() ;
        	  return SEQ(G, FAIL()) ;
        	
        }) ;
        
        GoalStructure G = SEQ(g10.lift(), g15) ;
        agent.setGoal(G) ;
        
        //G.printGoalStructureStatus();     
        //System.out.println(">>> " + getPrimgoals(G).size()) ;
        assertTrue(getPrimgoals(G).size() == 5) ;
        
        executeAgent(agent,20) ;
        
        assertTrue(state.x == 15) ;
        assertTrue(G.status.failed()) ;
        assertTrue(getPrimgoals(G).size() == 5) ; // 
        G.printGoalStructureStatus();     
    }

}
