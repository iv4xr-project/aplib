package nl.uu.cs.aplib.mainConcepts;

import org.junit.jupiter.api.Test;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static org.junit.jupiter.api.Assertions.*;

import nl.uu.cs.aplib.environments.NullEnvironment;


public class Test_Goalstack {
	
	public static class MyState extends SimpleState {
        public int x = 0 ;
        public int y = 0 ;

        public MyState() {
            super();
        }
        
        @Override
        public void updateState(String agentId) {
        	// do nothing since we dont have a real env
        }
    }
	
	@Test
	void test_goalIsPushedExecuted() {
		
		var state = (MyState) (new MyState().setEnvironment(new NullEnvironment()));
        var agent = new BasicAgent().attachState(state);

        // testing update with no goal:
        agent.update();
        assertTrue(agent.goalstack.isEmpty());

        // ok let's now give a goal:
        var a0 = action("incr-x").do1((MyState S) -> {
            S.x++;
            return S;
        }).lift();
        
        var b = action("kick-y").do1((MyState S) -> {
        	        var G = lift("<copy x to y>", action("copy")
        	        		     .do1((MyState T) -> { T.y = T.x ; return true ;})) ;
            		agent.pushGoal(G) ;
            		return null ;
        		})
        		.on_((MyState S) -> S.y < S.x && S.x % 5 == 0)
        		.lift();
        
        
        
        var topgoal = goal("<main goal>").toSolve((MyState S) -> S.x == 11 && S.y==10)
        		.withTactic(FIRSTof(b,a0))
        		.lift();
        
        agent.setGoal(topgoal) ;
        
        assertTrue(agent.goalstack.stack.size() == 1) ;
        
        int k = 0 ;
        while (topgoal.getStatus().inProgress()) {
        	System.out.println(">>> k=" + k + "  x,y=" + state.x + "," + state.y) ;
        	agent.update();
        	k++ ;
        }
        System.out.println(">>> x=" + state.x + ", y=" + state.y) ;
        assertTrue(topgoal.getStatus().success()) ;
        assertTrue(state.x == 11 && state.y == 10) ;
        
        topgoal.printGoalStructureStatus();   
	}
	
	@Test
	void test_uncommittedGoalRetraction() {
		
		var state = (MyState) (new MyState().setEnvironment(new NullEnvironment()));
        var agent = new BasicAgent().attachState(state);

        // ok let's now give a goal:
        var a0 = action("incr-x").do1((MyState S) -> {
            S.x++;
            var G = lift("<make x 100>", action("x := 100")
       		     .do1((MyState T) -> { T.x = 100 ; return true ;})) ;
            agent.pushGoal(G) ;
            return S;
        }).lift();
        
        
        var topgoal = goal("<main goal>").toSolve((MyState S) -> S.x == 1)
        		.withTactic(a0)
        		.lift();
        
        agent.setGoal(topgoal) ;
        
        int k = 0 ;
        while (topgoal.getStatus().inProgress()) {
        	System.out.println(">>> k=" + k + "  x,y=" + state.x + "," + state.y) ;
        	agent.update();
        	k++ ;
        }
        System.out.println(">>> x=" + state.x + ", y=" + state.y) ;
        assertTrue(topgoal.getStatus().success()) ;
        assertTrue(state.x == 1) ;
        
        topgoal.printGoalStructureStatus();   
	}

}
