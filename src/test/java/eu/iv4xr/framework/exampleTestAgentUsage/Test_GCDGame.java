package eu.iv4xr.framework.exampleTestAgentUsage;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static eu.iv4xr.framework.Iv4xrEDSL.*;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.exampleTestAgentUsage.TestWithWrappingEnv_GCDGame.GCDEnv;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.Environment.EnvOperation;

/**
 * This class demonstrates how to use iv4xr
 * {@link eu.iv4xr.framework.mainConcepts.TestAgent} to test a simple program,
 * which is a game provided by the class {@link GCDGame}. It implements a simple
 * game over an imaginary 2D world where the player tries to get to a position
 * (x,y) which is relatively prime to each other (their greatest common divisor
 * (gcd) is 1).
 * 
 * <p>
 * Although it is certainly possible to program an iv4xr testagent to directly
 * control GCDGame (because it is a Java class, and hence its methods and state
 * are actually directly accessible to an iv4xr agents) we will pretend that it
 * is not. We will pretend that GCDGame is some system that runs outside the
 * testagent's JVM, so the agent cannot just call its methods. To control such a
 * system, the architecture of iv4xr agents insists that agents interact with
 * the "world" outside it (in this case the "world" is our system under test)
 * through an interface called "environment" (an instance of the class
 * {@link nl.uu.cs.aplib.mainConcepts.Environment}, or its subclass). This
 * means that to test {@link GCDGame} we will also have to do it through an instance
 * of this Environment. More precisely, we will have to create our own subclass
 * of {@link nl.uu.cs.aplib.mainConcepts.Environment} which is custom-made to
 * facilitate interactions with {@link GCDGame}.
 * 
 * <p>
 * For those who are curious, in {@link TestWithWrappingEnv_GCDGame} we also
 * show an architecture where the testagent directly access the SUT's APIs.
 */
public class Test_GCDGame {

	// This static variable will hold an instance of the Program-under-test (an
	// instance
	// of GCDGame; we will pretend that this is a remote service, accessible through
	// this
	// static variable.
	static GCDGame gameUnderTest;

	////////// New Environment//////GCDEnv Class///////////
	/**
	 * Define an Environment to provide an interface between the test agent and the
	 * program-under-test.
	 */
	static class GCDEnv extends Environment {

		public GCDEnv() {
			super();
		}

		/**
		 * Return the current state of the game-under-test.
		 */
		@Override
		public Object[] observe(String agentId) {
			return (Object[]) this.sendCommand(agentId, null, "observe", null);
		}

		/**
		 * Implement the method to let agents to send commands to the
		 * program-under-test:
		 */
		@Override
		protected Object sendCommand_(EnvOperation cmd) {
			logger.info("Command " + cmd.command);
			switch (cmd.command) {
			case "up":
				gameUnderTest.up();
				return null;
			case "down":
				gameUnderTest.down();
				return null;
			case "right":
				gameUnderTest.right();
				return null;
			case "left":
				gameUnderTest.left();
				return null;
			case "observe":
				Object[] obs = new Object[4];
				obs[0] = gameUnderTest.x;
				obs[1] = gameUnderTest.y;
				obs[2] = gameUnderTest.gcd;
				obs[3] = gameUnderTest.win();
				return obs;
			}
			throw new IllegalArgumentException();
		}

	}

	////////// New State structure//////MyState Class///////////
	/**
	 * Define a new state-structure for the agent. For this example, we don't
	 * actually need a new state-structure, but let's just pretend that we do.
	 */
	static class MyState extends State {

		// int counter = 0 ;
		// String last = null ;
		// int result = 0 ;

		int x;
		int y;
		int gcd;
		boolean win;

		MyState() {
			super();
		}

		@Override
		public GCDEnv env() {
			return (GCDEnv) super.env();
		}

		@Override
		public void updateState(String agentId) {
			Object[] obs = env().observe(agentId);
			x = (int) obs[0];
			y = (int) obs[1];
			gcd = (int) obs[2];
			win = (boolean) obs[3];
			// System.out.println(">>> x=" + x) ;
		}

		@Override
		public String toString() {
			return "(" + x + "," + y + "), gcd=" + gcd;
		}
	}

	Tactic skip() {
		return action("skip").do1((MyState S) -> S).lift();
	}

	// Construct a tactic to auto-drive the player to position X,Y:
	Tactic navigateTo(int X, int Y) {
		Action up = action("action_up").do1((MyState S) -> {
			S.env().sendCommand(null, null, "up", null);
			Logging.getAPLIBlogger().info("new state: " + S);
			return S;
		});
		Action down = action("action_down").do1((MyState S) -> {
			S.env().sendCommand(null, null, "down", null);
			Logging.getAPLIBlogger().info("new state: " + S);
			return S;
		});
		Action right = action("action_up").do1((MyState S) -> {
			S.env().sendCommand(null, null, "right", null);
			Logging.getAPLIBlogger().info("new state: " + S);
			return S;
		});
		Action left = action("action_left").do1((MyState S) -> {
			S.env().sendCommand(null, null, "left", null);
			Logging.getAPLIBlogger().info("new state: " + S);
			return S;
		});

		return FIRSTof(up.on_((MyState S) -> S.y < Y).lift(), down.on_((MyState S) -> S.y > Y).lift(),
				right.on_((MyState S) -> S.x < X).lift(), left.on_((MyState S) -> S.x > X).lift(), skip());
	}

	/**
	 * A parameterized test-case to test GCDGame. Given X and Y, this specifies the
	 * expected gcd value, and whether the GCDGame should conclude a win or lose. We
	 * will use iv4xr test-agent to do the test.
	 */
	public void parameterizedGCDGameTest(int X, int Y, int expectedGCD, boolean expectedWinConclusion) {

		// (1) initialize the program-under-test:
		gameUnderTest = new GCDGame();
		Logging.getAPLIBlogger()
				.info("STARTING a new test. Initial state: (" + gameUnderTest.x + ", " + gameUnderTest.y + ")");

		// (2) Create a fresh state + environment for the test agent; attach the game to
		// the env:
		var state = (MyState) (new MyState().setEnvironment(new GCDEnv()));

		// (3) Create your test agent; attach the just created state to it:
		var agent = new TestAgent().attachState(state);

		var info = "test gcd(" + X + "," + Y + ")";

		// (4) Define what is the testing task as a goal (to be solved by the agent):
		var topgoal = testgoal("tg")
				// the goal is to drive the game to get it to position (X,Y):
				.toSolve((MyState S) -> S.x == X && S.y == Y)
				// specify the tactic to solve the above goal:
				.withTactic(navigateTo(X, Y))
				// assert the correctness property that must hold on the state where the goal is
				// solved;
				// we will check that the gcd field and win() have correct values:
				.oracle(agent,
						(MyState S) -> assertTrue_("", info, S.gcd == expectedGCD && S.win == expectedWinConclusion))
				// finally we lift the goal to become a GoalStructure, for technical reason.
				.lift();

		// (5) Attach the goal created above to your test-agent; well, and the
		// test-agent also need
		// a data-collector:
		var dataCollector = new TestDataCollector();
		agent.setTestDataCollector(dataCollector).setGoal(topgoal);

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
		parameterizedGCDGameTest(0, 0, 0, false);
		parameterizedGCDGameTest(1, 1, 1, true);
		parameterizedGCDGameTest(12, 0, 12, false);
		parameterizedGCDGameTest(0, 9, 9, false);
		Logging.getAPLIBlogger().setUseParentHandlers(false);
		parameterizedGCDGameTest(32 * 7, 32 * 11, 32, false); // Test_GCD(7966496,314080416) --> takes too long :)
		parameterizedGCDGameTest(7, 11 * 11, 1, true);
		Logging.getAPLIBlogger().setUseParentHandlers(true);
	}

}
