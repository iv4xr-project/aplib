# Some Simple Examples
Author: Wishnu Prasetya

Below are several simple examples of how to write agents with `aplib`. They are just simple examples and won't do anything spectacular. It is recommended that you first go through the [Tutorial](./firstTutorial.md) to become familiar with some basic concepts (e.g. _action_ and _goal_) of `aplib` agent programming.


## Example 1: a dumb doctor agent

In this example we will create an agent that pretends to be a doctor, to engage a patient/user in a conversation by posing questions.

#### Goal
The agent's goal is to make the patient happy. Being a dumb agent, it assumes the patient's happiness to increase after each round of question-ansnwer. Each round increases the patient happiness level by one, and the patient is assumed to be happy enough once his happiness level reaches 5. This translates to the following goal:

```java
goal("the-goal").toSolve((Integer happiness) -> happiness >= 5) ;
```

#### Agent's state

The agent will need at least one variable, which is an integer to keep track of the patient's happiness level. We can do this by defining this state structure:

```java
static public class DoctorBelief extends SimpleState {
   Integer patientHappiness = 0 ;
   ...
```

#### Agent's actions

Let's program the agent to have two kinds of actions. The first one asks an opening question "_How do you feel today?_" to the patient:

```java
var opening = action("opening")
     . do_((DoctorBelief belief) -> actionstate_ -> {
         belief.env().ask("How do you feel today?");
         return ++belief.patientHappiness ;
       })
     ...
```

The method `ask(question)` will ask the question to the patient through the Console and waits for the patient's response to the question. The action above does not store the patient's response though (so, it can't do anything smart with it either, being just a dumb agent).

Also notice that the action increases the agent's `patientHappiness` variable.

Other actions are actions to ask subsequent questions to the patient, e.g.:

```java
var a1 = action("a1")
    . do_((DoctorBelief belief) -> actionstate_ -> {
        belief.env().ask("Please explain a bit more...");
        return ++belief.patientHappiness ;
     })
    ...
```

We can create `a2`, `a3`, ... analogous to `a1`.

#### Strategy to solve the goal

Finally, to solve the given goal, a strategy will be needed.  The strategy below first tries the `opening` action, and then one of `a1`, `a2`, .... Notice that the `opening` action is only enabled when `patientHappiness==0` (which would only be true at the initial state).

```java
FIRSTof(opening.on_((DoctorBelief belief) -> belief.patientHappiness == 0) ,
    	ANYof(a1,a2,...))
```

The agent applies this strategy at every tick until it solves its goal.

#### Full code

The full code of this example can be found [here](../../src/main/java/nl/uu/cs/aplib/ExampleUsages/DumbDoctor.java)


## Example 2: guess-number agent

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

#### Strategy to solve the goal

To solve the previously given goal (or any goal), the agent will need a strategy. The following strategy will first ask a hint from the user (in the form of a lower bound for x), and then it will make a guess:

```java
SEQ(asklb,guess)
```

If after the `guess` action the goal is not solved, the agent will implicitly repeat the above strategy at the next tick, on and on, until the goal is solved.

#### Full code

The full code of this example can be found [here](../../src/main/java/nl/uu/cs/aplib/ExampleUsages/GuessNumberGame.java)
