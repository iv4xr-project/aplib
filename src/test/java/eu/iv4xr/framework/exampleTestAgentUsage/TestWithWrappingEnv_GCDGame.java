package eu.iv4xr.framework.exampleTestAgentUsage;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import static eu.iv4xr.framework.Iv4xrEDSL.*;
import static nl.uu.cs.aplib.AplibEDSL.*;
import eu.iv4xr.framework.mainConcepts.*;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.agents.StateWithMessenger;
import nl.uu.cs.aplib.mainConcepts.*;
import static eu.iv4xr.framework.mainConcepts.ObservationEvent.* ;

/**
 * This class demonstrates how to use iv4xr
 * {@link eu.iv4xr.framework.mainConcepts.TestAgent} to test a simple class. The
 * class-under-test is the class {@link GCDGame}, which implement a simple game
 * over an imaginary 2D world where the player tries to get to a position (x,y)
 * which is relatively prime to each other (their greatest common divisor (gcd)
 * is 1).
 * 
 * <p>
 * The architecture of iv4xr agents insists that agents interact with the
 * "world" outside it through an interface called "environment" (an instance of
 * the class {@link nl.uu.cs.aplib.mainConcepts.Environment}, or its subclass).
 * This means, to test {@link GCDGame} we will also have to do it through an
 * instance of this Environment. More precisely, we will have to create our own
 * subclass of {@link nl.uu.cs.aplib.mainConcepts.Environment} which is
 * custom-made to facilitate interaction with {@link GCDGame}. To make the
 * interfacing easy, here we will just wrap the instance of {@link GCDGame} that
 * is to be tested inside our instance of Environment, so that the test-agent
 * can directly access this GCDGame through the Environment.
 * 
 * <p>
 * While the above mentioned wrapping approach gives a simple interfacing, note
 * that if the program-under-test is for example a service running elsewhere
 * then this wrapping approach will of course not work; we will then have to
 * really implement some interface to enable the Environment to communicate with
 * the service-under-test.
 *
 */
public class TestWithWrappingEnv_GCDGame {
	
	/**
	 * Define an Environment to provide an interface between the test agent and the
	 * program-under-test. Here, we will choose to simply wrap the environment over
	 * the program-under-test.
	 */
	static class GCDEnv extends Environment {
		/**
		 * The instance of GCDGame that is to be tested, wrapped inside this
		 * Environment.
		 */
		GCDGame gcdgameUnderTest;
		GCDEnv(GCDGame gcdgame) { gcdgameUnderTest = gcdgame; }
		@Override
		public String toString() {
			return "(" + gcdgameUnderTest.x + "," + gcdgameUnderTest.y + "), gcd=" + gcdgameUnderTest.gcd + ", win=" + gcdgameUnderTest.win();
		}
	}

	/**
	 * Define a new state-structure for the agent. For this example, we don't
	 * actually need a new state-structure, but let's just pretend that we do.
	 */
	static class MyState extends StateWithMessenger {
		MyState() { super(); }
		@Override
		public GCDEnv env() { return (GCDEnv) super.env(); }
	}


    // Construct a tactic to auto-drive the player to position X,Y:
	Tactic navigateTo(int X, int Y) {
		Action up = action("action_up").do1((MyState S) -> {
			S.env().gcdgameUnderTest.up();
			Logging.getAPLIBlogger().info("UP. New state: " + S.env());
			return S;
		});
		Action down = action("action_down").do1((MyState S) -> {
			S.env().gcdgameUnderTest.down();
			Logging.getAPLIBlogger().info("DOWN. New state: " + S.env());
			return S;
		});
		Action right = action("action_right").do1((MyState S) -> {
			S.env().gcdgameUnderTest.right();
			Logging.getAPLIBlogger().info("RIGHT. New state: " + S.env());
			return S;
		});
		Action left = action("action_left").do1((MyState S) -> {
			S.env().gcdgameUnderTest.left();
			Logging.getAPLIBlogger().info("LEFT. New state: " + S.env());
			return S;
		});

		return FIRSTof(up.on_((MyState S) -> S.env().gcdgameUnderTest.y < Y).lift(),
				down.on_((MyState S) -> S.env().gcdgameUnderTest.y > Y).lift(),
				right.on_((MyState S) -> S.env().gcdgameUnderTest.x < X).lift(),
				left.on_((MyState S) -> S.env().gcdgameUnderTest.x > X).lift());
	}

	/**
	 * A parameterized test-case to test GCDGame. Given X and Y, this specifies the
	 * expected gcd value, and whether the GCDGame should conclude a win or lose. We
	 * will use iv4xr test-agent to do the test.
	 */
	public void parameterizedGCDGameTest(int X, int Y, int expectedGCD, boolean expectedWinConclusion) {

		// (1) Create a new GCDgame that is to be tested:
		var game = new GCDGame();
		Logging.getAPLIBlogger().info("STARTING a new test. Initial state: (" + game.x + ", " + game.y + ")");

		// (2) Create a fresh state + environment for the test agent; attach the game to the env:
		var state = (MyState) (new MyState().setEnvironment(new GCDEnv(game)));
		
		// (3) Create your test agent; attach the just created state to it:
		var agent = new TestAgent().attachState(state);
		
		
		var info = "test gcd(" + X + "," + Y + ")";

		// (4) Define what is the testing task as a goal (to be solved by the agent):
		var topgoal = testgoal("tg")
				// the goal is to drive the game to get it to position (X,Y):
				. toSolve((MyState S) -> S.env().gcdgameUnderTest.x == X && S.env().gcdgameUnderTest.y == Y)
				// specify the tactic to solve the above goal:
				. withTactic(navigateTo(X, Y))
				// assert the correctness property that must hold on the state where the goal is solved; 
				// we will check that the gcd field and win() have correct values:
				. oracle(agent, (MyState S) -> 
				      assertTrue_("",info,
				    	S.env().gcdgameUnderTest.gcd == expectedGCD
						&& S.env().gcdgameUnderTest.win() == expectedWinConclusion))
				// finally we lift the goal to become a GoalStructure, for technical reason.
				. lift();

		// (5) Attach the goal created above to your test-agent; well, and the test-agent also need
		// a data-collector:
		var dataCollector = new TestDataCollector();
		agent. setTestDataCollector(dataCollector)
		     . setGoal(topgoal);

		// (6) Ok, now we can run the agent to do the test:
		while (!topgoal.getStatus().success()) {
			agent.update();
		}
		
		// (7) And finally we verify that the agent didn't see anything wrong:
		assertTrue(dataCollector.getNumberOfFailVerdictsSeen() == 0);
		assertTrue(dataCollector.getNumberOfPassVerdictsSeen() == 1);
		Logging.getAPLIBlogger().info("TEST END.");
	}


	@Test
	/**
	 * OK, let's now run a bunch of tests!
	 */
	public void tests() {
		parameterizedGCDGameTest(0,0,0,false) ;
		parameterizedGCDGameTest(1,1,1,true) ;
		parameterizedGCDGameTest(12,0,12,false) ;
		parameterizedGCDGameTest(0,9,9,false) ;
		Logging.getAPLIBlogger().setUseParentHandlers(false);  
		parameterizedGCDGameTest(32*7,32*11,32,false) ; // Test_GCD(7966496,314080416) --> takes too long :)
		parameterizedGCDGameTest(7,11*11,1,true) ; 
		Logging.getAPLIBlogger().setUseParentHandlers(true);  
	}

}
