# Some Simple Examples
Author: Wishnu Prasetya

Below are several simple examples of how to write agents with `aplib`. They are just simple examples and won't do anything spectacular. It is recommended that you first go through the [Tutorial-1](./firstTutorial.md) and [Tutorial-2](./tutorial_2.md) to become familiar with some basic concepts (e.g. _action_ and _goal_) of `aplib` agent programming.


## Example: guess-number agent

This agent plays a simple number-guessing game. The user is asked to think of an integer x in the interval of 0..10. The agent then interacts with the user, trying to guess x.

#### Goal

The goal is to get the user to answer with "y" (short for "yes"), signalling that the agent has guessed x correctly,
or if the agent concludes that the user cheats by having x outside the 0..10 range:

```java
goal("the-goal").toSolve((String p) -> p.equals("y") || p.equals("out of range")) 		
```
#### Agent's state

Our agent will maintain a boolean array `excluded` and sets `excluded[i] = true` if it thinks that x cannot be equal to i. To support this, we will use the following state structure:

```java
static public class MyAgentState extends SimpleState {
   boolean[] excluded = ... // initialized to falses ;
   ...
```

#### Agent's actions

The action below checks if there are still numbers in 0..10 that could be x. It chooses one randomly, and offers it to the user, asking if it is indeed x.

```java
var guess = action("guess")
    . do_((MyAgentState belief) -> actionstate_ -> {
        // the agent performs some inference:
        var candidates = belief.getPossibilities() ;
        if(candidates.isEmpty()) {
           belief.env().println("Your number is NOT in [0..10]!") ;
           return "out of range" ;
        }
        int x = candidates.get(belief.rnd.nextInt(candidates.size())) ;
        var o = belief.env().ask("Is it " + x + "? (answer y/n)") ;
        belief.excluded[x] = true ;
        return o ;	 
    })
    ...
```

The action below asks if the user can give a hint in the form of a lower bound for x:

```java
var asklb = action("askLowerBound")
    . do_((MyAgentState belief) -> actionstate_ -> {
        var o = belief.env().ask("Type a number less or equal to your number:");
        var i = toInt(o) ;
        if (i!=null) {
           for (int k=0; k<Math.max(0,i); k++)
                belief.excluded[k] = true ;
        }
        return "" ;
      })
      ...
```

#### Tactic to solve the goal

To solve the previously given goal (or any goal), the agent will need a tactic. The following tactic will first ask a hint from the user (in the form of a lower bound for x), and then it will make a guess:

```java
SEQ(asklb,guess)
```

If after the `guess` action the goal is not solved, the agent will implicitly repeat the above tactic at the next tick, on and on, until the goal is solved.

#### Full code

The full code of this example can be found [here](../../src/main/java/nl/uu/cs/aplib/ExampleUsages/GuessNumberGame.java)
