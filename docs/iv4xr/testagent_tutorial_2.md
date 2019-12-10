# iv4xr Tutorial 2: Testing an External Program with Test Agent
Author: Wishnu Prasetya

**Prerequisite:** you have read at least the following tutorials:

* [`iv4xr` Tutorial 1](./testagent_tutorial_1.md), explaining the basic of how to use an `iv4xr` agent to test a Java class.

In this Tutorial-2 we will show how to do the same as in Tutorial-1, but where the program-under-test is an external program.

The main difference is in how you should build your custom Environment to let your test-agent control the program-under-test. As this is different, this also impacts the way you write goals and tactics.

**For impatient ones,** see the code in this example: [TestWithRemotingEnv_GCDGame.java](../../src/test/java/eu/iv4xr/framework/exampleTestAgentUsage/TestWithRemotingEnv_GCDGame.java).

### Example: GCDGame

Let's consider the same example as in Tutorial-1, namely the GCD-game. However we will now pretend that it is an external program. The game is played on an imaginary 2D grid space.
The game maintains three variables/fields: (x,y,gcd). The fields x,y represent
the current position of the player in the 2D grid. Any non-negative x,y are valid. The greatest common divisor of these x and y is stored in the field gcd.
The player can move up, down, left or right, one square at at time. The player wins if he/she manage to find a square whose greatest common divisor is 1 (in other words, if x and y are relative prime to each other).

Below is the signature of the game, but keep in mind that we will pretend this game to run externally now.

```java
public class GCDGame {
  int x ; int y ; int gcd ;
  public GCDGame() ... // creating an instance of the game, with random x,y
  public void up()
  public void down()
  ...
  public boolean win()
}
```

### Defining the Environment

Every program-under-test will likely need its own custom Environment, which can be created by **subclassing** the class [`Environment`](../../src/test/java/nl/uu/cs/aplib/mainConcepts/Environment.java), or any of its subclasses provided in `aplib`.

Since the program-under-test now runs externally, your test-agent will not be able to directly access it state. Let's therefore introduce some fields in our custom Environment, which is intended to keep track of the relevant part of the program-under-test's state, e.g. as follows:

```java
static class GCDEnv extends Environment {
  int x ; int y ; int gcd ; boolean win ;
... }
```
You will need a way to sync the above information with the actual program-under-test's state. Additionally you need a way to let the agent control the program-under-test. To do these, there are two methods of `Environment` that you should override: `refreshWorker()` and `sendCommand_(cdm)`.

Here is a template to do it:  

```java
static class GCDEnv extends Environment {
		int x ; int y ; int gcd ; boolean win ;

		@Override
		public void refreshWorker() {
			x = ... get x from the GCDGame instance under test
			y = ...
			gcd = ... get gcd from the GCDGame instance under test
			win = ... get win() from the GCDGame instance under test
		}

		@Override
		protected Object sendCommand_(EnvOperation cmd) {
			switch (cmd.command) {
			   case "up"   : gameUnderTest.up() ; break ;
			   case "down" : gameUnderTest.down() ; break ;
               ...		
			}
			// we'll re-sync this Environment after the command:
			refreshWorker() ; return null ;
		}
}
```

### Defining Goal and Oracle

Goals, oracles, and tactics can in principle be defined in the same way as in Tutorial-1. However, the precise code to get to information on the state of the program-under-test, or to tell it to do something is a bit different.

```java
var topgoal = testgoal("tg")
    // specify the goal:
    . toSolve((MyState S) -> S.env().x==X && S.env().y==Y )
    // specify a tactic to solve the goal:
    . withTactic(...........)
    // assert the property to check:
    . oracle(agent, (MyState S) -> assertTrue_("",info,
         S.env().gcd == expectedGCD && S.env().win == expectedWinConclusion))
    // finally we lift the goal to become a GoalStructure, for technical reason.
    . lift() ;
```

### Defining the Tactic and Actions

Very much like defining a goal above, tactics and actions can in principle be defined in the same way as in Tutorial-1. However, the precise code to get to information on the state of the program-under-test, or to tell it to do something is a bit different. It is now time for you to study this in the real code. See: example: [TestWithRemotingEnv_GCDGame.java](../../src/test/java/eu/iv4xr/framework/exampleTestAgentUsage/TestWithRemotingEnv_GCDGame.java).
