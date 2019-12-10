# iv4xr Tutorial 1: Testing a Java Class with Test Agent
Author: Wishnu Prasetya

**Prerequisite:** you have read at least the following tutorials:

* [`Aplib` Concepts](../manual/aplibConcepts.md), explaining the main concepts behind iv4xr agents programming.
* [ `Aplib` Tutorial 1](../manual/tutorial_1.md), showing a simple example of creating an agent.
* [ `Aplib` Tutorial 2](../manual/tutorial_2.md), a brief introduction to _goal_ and _tactic_.

### What is iv4xr ?

`iv4xr` is an **agent-based framework for automated testing**. The original usecase of `iv4xr` is to test so-called _Extended Reality_ systems, such as computer games, simulators, and VR or AR based systems. Currently this is still an on-going project, but in any case `iv4xr` is actually generic enough to target other types of software, such as a class or a service.

**What is the difference between iv4xr and aplib?**
`aplib` is the underlying agent-library underneath `iv4xr`. `aplib` is a general purpose agent-library, whereas `iv4xr` is a framework/library specifically for testing. E.g. `iv4xr` adds testing-related functionalities to `aplib` agents so that these agents can be used to test other software.

### What is agent-based testing?

Well, it is just performing testing where you use software agents to drive the test. Whether this gives add values depends on what the agents have to offer. `iv4xr` test agents would allow you to program your testing tasks **declaratively**.

### Example: testing a Java class GCDGame

In this tutorial we will look how to use an `iv4xr` agent to test a Java class. The approach is a bit different if the program-under-test has to run on a different runtime environment than the one the test-agent uses. For the latter you need to consult another tutorial (to-do) or checks the example [TestWithRemotingEnv_GCDGame.java](../../src/test/java/eu/iv4xr/framework/exampleTestAgentUsage/TestWithRemotingEnv_GCDGame.java).

As the example, consider the class below. It implements a simple game called _GCD-game_. The game is played on an imaginary 2D grid space. The game maintains three variables/fields: (x,y,gcd). The fields x,y represent
the current position of the player in the 2D grid. Any non-negative x,y are valid. The greatest common divisor of these x and y is stored in the field gcd.
The player can move up, down, left or right, one square at at time. The player wins if he/she manage to find a square whose greatest common divisor is 1 (in other words, if x and y are relative prime to each other).

```java
public class GCDGame {
  int x ;
  int y ;
  int gcd ;
  public GCDGame() ... // creating an instance of the game, with random x,y
  public void up()
  public void down()
  public void right()
  public void left()
  public boolean win() { return gcd == 1 ; }
}
```

The full code is in [GCDGame.java](../../src/test/java/eu/iv4xr/framework/exampleTestAgentUsage/GCDGame.java).

Imagine now that we want to test this class. To do this with `iv4xr` we would need a test agent, which is an instance of the class [`TestAgent`](../../src/main/java/eu/iv4xr/framework/mainConcepts/TestAgent.java).

**For impatient ones:**
* 'Wrapping' approach, suitable for testing a Java class: see [TestWithWrappingEnv_GCDGame.java](../../src/test/java/eu/iv4xr/framework/exampleTestAgentUsage/TestWithWrappingEnv_GCDGame.java).
* 'Remoting' approach, suitable for testing an external program that does not run in the same JVM as the test agent: see [TestWithRemotingEnv_GCDGame.java](../../src/test/java/eu/iv4xr/framework/exampleTestAgentUsage/TestWithRemotingEnv_GCDGame.java).

To test `GCDGame` with a test agent, roughly the steps are as follows:

1. The agent will need a **custom 'Environment'** to interact with the program-under-test, which in the case is the class `GCDGame`. So, we need to define one.
1. The agent will also need a state to hold the Environment and whatever other information it needs to keep track, if there is any. So, we need to define this state.
1. We need to specify the testing task. This amounts to specifying at least one goal, a tactic on how to solve the goal, and the correctness property to check when the goal is solved.
1. We can now run the test agent and collect the findings.


### Step 1: Defining Your Custom Environment

Although an `iv4xr` agent can be made to directly call `GCDGame`, this is not the proper way to use an agent-based approach. In an agent-based architecture, agents should control the program-under-test (or any program, for that matters) **through** an 'Environment'. Every program-under-test will likely need its own custom Environment, which can be created by **subclassing** the class [`Environment`](../../src/test/java/nl/uu/cs/aplib/mainConcepts/Environment.java), or any of its subclasses provided in `aplib`. Here is an Environment for our example `GCDGame`:

<<**NOTE for IV4XR TEAM**: below is NOT how you should interface your 3D game/simulator to iv4xr. See instead the example [TestWithRemotingEnv_GCDGame.java](../../src/test/java/eu/iv4xr/framework/exampleTestAgentUsage/TestWithRemotingEnv_GCDGame.java).>>

```java
static class GCDEnv extends Environment {
  GCDGame gcdgameUnderTest;
  GCDEnv(GCDGame gcdgame) { gcdgameUnderTest = gcdgame; }
}
```

Above, the only thing we did is to make this custom Environment to wrap over program-under-test. Hence, a test agent can reach it through this environment.

### Step 2: Defining the State Structure for the Agent

Every agent will need a state which it can use to keep track whatever information that it wants to keep track. The state of a test-agent must be an instance of the class [`StateWithMessenger`](../../src/test/java/nl/uu/cs/aplib/agents/StateWithMessenger.java). A direct instance of this class will get a pointer/reference to an `Environment`, and since the Environment you created above wraps over the program-under-test, an agent can thus access the latter through its state. For our purpose to test `GCDGame` this will suffice. But hypothetically, if your agent needs to keep track more information, you will need to extend its state with new fields to hold this information. In other words, you would need a custom state, which you can do that by subclassing `StateWithMessenger`.

For the purpose of testing `GCDGame` we don't need to subclass `StateWithMessenger`. However, it is convenient for later if the default method `env()` of `StateWithMessenger` would return an instance of `GCDEnv` (our custom Environment) rather than `Environment`, as it would save us from having to do the type casting in the agent code. So, here is our custom state:

```java
static class MyState extends StateWithMessenger {
  MyState() { super(); }
  @Override
  public GCDEnv env() { return (GCDEnv) super.env(); } // forcing to return GCDEnv
}
```

**Creating an agent and equipping it with state and Environment.**
Having defined your Environment and agent-state, now you can create a test-agent and configure its state and Environment:

```java
// Create a new GCDgame that is to be tested:
var game = new GCDGame();
// Create a fresh state + environment for the test agent; attach the game to the env:
var state = (MyState) (new MyState().setEnvironment(new GCDEnv(game)));
// Create your test agent; attach the just created state to it:
var agent = new TestAgent().attachState(state);
```

### Step 3: Specifying Your Testing Task

Imagine a simple testing scenario for `GCDGame` where we drive the game to location (x,y) = (1,1). At this position we expect the valur of gcd to be 1 and the method `win()` to return true. We will later generalize this scenario, but let's for now just focus on this scenario. The scenario has two key parts:

* The agent needs to drive the program-under-test to a specific state, namely (x,y)=(1,1). This is called a **goal**.
* The correctness property to assert is: **when** (x,y)=(1,1) **then** gcd is expected to be 1 and `win()` is expected to be true. This is called **oracle**.

Notice that essentially a test checks if the state predicate _goal ⇒ oracle_ is valid.

Here is how we express the above test in code:

```java
var X = 1 ;
var Y = 1 ;
var expectedGCD = 1 ;
vat expectedWinConclusion = true ;

var topgoal = testgoal("tg")
	// formulate the goal to solve:
	. toSolve((MyState S) -> S.env().gcdgameUnderTest.x == X && S.env().gcdgameUnderTest.y == Y)
	// specify the tactic to solve the above goal:
	. withTactic( ... WE NEED a tactic ...)
	// assert the oracle :
	. oracle(agent, (MyState S) ->
				      assertTrue_("",info,
                         S.env().gcdgameUnderTest.gcd == expectedGC
                         && S.env().gcdgameUnderTest.win() == expectedWinConclusion))
	// finally we lift the goal to become a GoalStructure, for technical reason.
	. lift();
```

Now that the agent knows what logically the test is, it still has to figure out how to push around the program-under-test, to get it to the goal-state. Only then is the above oracle will be checked.

To move the program-under-test to a state satisfing the goal (in other words: to solve the goal), we need to supply a so-called _tactic_, which is still missing in the above code. A tactic that can drive the _GCDGame_ to position  (x,y)=(1,1) will do. There are two challenges in programming this tactic:

* When created, an instance of _GCDGame_ starts with a random (x,y).
* Yes, we can peek into the value of (x,y), but literally programming the navigation from the starting (x,y) to (1,1) one step at a time would be pretty dull to write.

Fortunately, every test agent comes with some automation that allows us to formulate this tactic more abstractly. Consider the one below:

```java
Action up = action("action_up").do1((MyState S) -> {
			S.env().gcdgameUnderTest.up();
			return S; });
Action down  = ... // similar
Action right = ...
Action left  = ...

Tactic tactic = FIRSTof(
   up.on_((MyState S) -> S.env().gcdgameUnderTest.y < Y).lift(),
   down.on_((MyState S) -> S.env().gcdgameUnderTest.y > Y).lift(),
   right.on_((MyState S) -> S.env().gcdgameUnderTest.x < X).lift(),
   left.on_((MyState S) -> S.env().gcdgameUnderTest.x > X).lift()
);
```
So, the tactic above will move the imaginary 'player' one square up if y is less than the target Y-position, else one square down if y is greater than the target Y, and so on.

With the above tactic, the agent will be able to automatically navigate to any specified (X,Y) position.

### Step4: Running the Test Agent

What remains is to tell our previous test agent to take the above formulated goal, and then we run the agent. We will run the agent until the goal is solved. If there is no certainty that the goal can be solved within a reasonable time, we may want to put some limit on the computation budget the agent is allowed to use; but we will not do so for this simple example.

Additionally, a test-agent will also need a data-collector to which it can send its findings (such as oracle violations).

So, here is the code:

```java
var dataCollector = new TestDataCollector();
agent. setTestDataCollector(dataCollector)
     . setGoal(topgoal);

// Ok, now we can run the agent to do the test:
while (!topgoal.getStatus().success()) {
  agent.update();
}

// verify that the agent didn't see anything wrong:
assertTrue(dataCollector.getNumberOfFailVerdictsSeen() == 0);
assertTrue(dataCollector.getNumberOfPassVerdictsSeen() == 1);
```

Well, basically we are done! That's how to do a test with an `iv4xr` test agent. However, you can get more by generalizing the above test, see below.

### Generalizing the test scenario

So far we have assumed a quite specific test scenario where we want to move the agent to (x,y)=(1,1). We can actually generalize the approch told above to test any target (X,Y) instead of just (1,1). In fact, the goal and oracle formulated above already assume such a generalized scenario. All we need to do to generalize now is to capture all we said above in some sort of _parameterized_ test, e.g. as follows:

```java
void parameterizedGCDGameTest(int X, int Y, int expectedGCD, boolean expectedWinConclusion) {
   ... define the tactic as above
   ... define the goal and oracle as above, attach the tactic to the goal
   ... attach the goal to the test agent
   ... run the test agent as above
   ... check the data-collector if errors were detected as above
}  
```

Given the above, now we can run multiple tests:

```java
@Test
/**
 * OK, let's now run a bunch of tests!
 */
 @Test // to run the tests below in JUnit
public void tests() {
  parameterizedGCDGameTest(0,0,0,false) ;
  parameterizedGCDGameTest(1,1,1,true) ;
  parameterizedGCDGameTest(12,0,12,false) ;
  parameterizedGCDGameTest(0,9,9,false) ;
  parameterizedGCDGameTest(32*7,32*11,32,false) ; 
  parameterizedGCDGameTest(7,11*11,1,true) ;
}
```
