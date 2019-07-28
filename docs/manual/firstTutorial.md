# Aplib Tutorial: Coding a Minimalistic Agent-X
Author: Wishnu Prasetya

`Aplib` is implemented in Java-11, so you need it.

In a nutshell an agent is just a program. Agents can indeed be programmed to behave intelligently ---the so-called intelligent agents; but let’s for now focus on just plain agents. `Aplib` defines an agent as a stateful program that interacts with an environment with the purpose of solving some problem relevant for the environment. Environment is an important concept in `aplib` programming: without an environment the agent has no purpose.

There is however no quick Hello-World example to show you how to use `aplib`. Let me guide you on how to create a minimalistic agent; it won't do anything spectacular, it will do as an example.

Let's see how we can program an agent, let's call it _agent-X_ to guess a secret number. This secret number is the number 10, but the agent does not know that, so it really has to guess. The agent will also interact with the user (you), in this case by simply printing the number it proposes to the console.

### Creating an agent, 1st attempt

To create an agent we do:

```java
var agentX = new BasicAgent()
```

`nl.uu.cs.aplib.MainConcepts.BasicAgent` is the root class of all agents in `aplib`. That is, all agents are instances of `BasicAgent`. There are other types of agents in `aplib`, but let's not go into that yet.

But ..., the above agent is blank and will not be able to do anything. Let's try to create an agent with something:

```java
var agentX = new BasicAgent() ;
agentX.attachState(belief)   \\ attach a state-structure to the agent ;
agentX.setGoal(topgoal) ;    \\ set a goal for the agent to solve
```

Let's for now ignore where `belief` and `topgoal` come from. Both `attachState(s)` and `setGoal(g)` are methods of the class `BasicAgent`. Although a `BasicAgent` has its own internal state to support its own book keeping, it posses no state that semantically means something towards whatever the purpose we want to give to it. As the name says, the method `attachState(s)` will attach an object `s`, representing state, to the agent. This `s` must be an instance of `nl.uu.cs.aplib.MainConcepts.SimpleState`. Importantly, this state must also contain a pointer to an 'environment'.

The method `setGoal(g)` is used to set a 'goal' (more precisely, a 'goal-tree') for the agent; `g` must be an instance of `nl.uu.cs.aplib.MainConcepts.GoalTree`.


#### Intermezzo: fluent interface programming style

`attachState(s)` and `setGoal(g)` are just  setters, but they are implemented as follows:

```Java
class BasicAgent {
  SimpleState state ;
  GoalTree goal ;
  ...
  public BasicAgent attachState(SimpleState state) {
		this.state = state ; return this ; }
  public BasicAgent setGoal(GoalTree g) {
		this.goal = g ; ... ; return this ; }
}
```

Notice how they return the agent itself after doing their setter work. This allows us to write the previous code for creating the agent more cleanly as follows:

```java
var agentX = new BasicAgent()
             . attachState(belief)  
             . setGoal(topgoal) ;  
```

This style of programming is also called [*Fluent Interface*](https://www.martinfowler.com/bliki/FluentInterface.html), coined by Eric Evans and Martin Fowler.

Many APIs in `aplib` can be used in this Fluent Interface style.

(end intermezzo)

### Agent state

An agent state is an instance of `nl.uu.cs.aplib.MainConcepts.SimpleState`. For a start, such an instance does not contain any information other than a place holder an 'environment'. Below is how we can create an instance of an agent state. We will also attach a simple environment called `ConsoleEnvironment` to it:

```Java
var belief = new SimpleState() ;
belief.setEnvironment(new ConsoleEnvironment()) ;
```
Or, in the Fluent Interface style:

```Java
var belief = new SimpleState() . setEnvironment(new ConsoleEnvironment()) ;
```

For our agent-X, such minimalistic state will do.
But if for a more sophisticated agent we want to create a state that holds some information, e.g. an integer counter, we can do this by subclassing `SimpleState`:

```Java
class MyState extends SimpleState {
  int counter = 0 ;
  public MyState() { super() ; }
}
```
And to create an instance of this:

```Java
var belief = new MyState() . setEnvironment(new ConsoleEnvironment()) ;
```
### Environment

Once we create an object that is to be used for holding the agent state (see above), we need to link this object to the environment that the agent will ultimately interact with. This linking up is what you do above with the method `setEnvironment`.

An environment is an instance of `nl.uu.cs.aplib.MainConcepts.Environment`. This class does not provide any behaviour though. It is intended to be **subclassed** to implement an actual environment. As an example, `Aplib` provides one implementation called `nl.uu.cs.aplib.Environments.ConsoleEnvironment` that allows an agent to write to and read from the System console (and hence interacting with a human user through the console). This is the environment that we used in the previous examples.

`ConsoleEnvironment` provides the following APIs:

```Java
public void println(String str) { System.out.println(str) ; }
public String readln() { return  consoleInput.nextLine() ; }
public String ask(String s) { println(s) ; return readln() ; }
```

### Goal

To make an agent does something, we first need to formulate a *goal*. After setting up a goal for an agent, the agent would the have a purpose, namely to solve the goal. Later we will see how the agent proceeds to actually solve its goal. Let us first take a look at how to formulate a goal.

Abstractly, a goal is a 'predicate', which is a function from some x to `boolean`. In Java we can construct predicates using lambda-expression. For example:

```Java
Predicate<Integer> p1 = x -> x==10 ;
Predicate<Integer> p2 = x -> x>0 ;
```

`p1` a predicate that is true only on x which is equal to 10, whereas `p2` is a predicate that is true only on positive x.

Technically though, in `aplib` a goal is an instance of `nl.uu.cs.aplib.MainConcepts.Goal`, but we can easily turn a lambda expression such as above to internally become an instance of `Goal`:

```Java
var g10 = goal("Guess a the magic number (10)") ; // create a goal g10 with some descriptive name
g10.toSolve((Integer x) -> x == 10) ; // attach a predicate to the goal
```
Or, in the Fluent Interface style:

```Java
var g10 = goal("Guess a the magic number (10)") . toSolve((Integer x) -> x == 10) ;
```
This goal is solved if the agent manage to compute a 'proposal' x that satisfies the predicate attached to it. Note: although simple, the above predicate might not be easy for the agent to solve, especially if it does not know upfront what the solution is.

#### Goal-tree

To be more precise, an agent actually expects a `GoalTree` rather than a `Goal`. A `GoalTree` allows a complex goal such as `SEQ(g1,g2,g3)` to be expressed. Let's not discuss this now. For now it is sufficient to know that a `Goal` can be lifted to a `GoalTree`. If `g` is a `Goal`, `g.lift()` will construct a `GoalTree` that contains `g` as its only element.

### Action and strategy

A `BasicAgent` does not however have any knowledge how to solve any goal. We will need to program it to solve a goal. Of course, we can write a more sophisticated type of agents some built-in intelligence to make the task of programming them easier. But for now let's stick with `BasicAgent`.

When we specify a goal, we need to supply the corresponding 'strategy' to solve it. Note: when we have a goal-tree instead of just a single goal, every goal (leaf) in the tree requires a strategy.

A _strategy_ is composed from one or more _actions_. In the simple case, a strategy consists of just a single action.

An _action_ is a stateful program that operates on the agent state and its own state. It may produce a proposal to be passed on to the agent's goal to see if that solves the goal. The action may also change the state of the agent as well as its own state. Because the action has access to the agent's state, it can thus access the Environment through the agent's state.

Abstractly, we can formulate an action with a lambda-expression. For example, here is an action that generates a random integer between 0..11 and proposes it to solve the agent's current goal:

```Java
(SimpleState belief) -> actionstate_ -> rnd.nextInt(11)
```
The lambda-expression does not contain explicit code for checking the goal. When given such an action, under the hood the agent will pass the return value of the lambda-expression to its goal.

Suppose that the agent uses the `ConsoleEnvironment` as its environment.
Suppose, in addition to generating a random number we also want the action to print this number to this console. We can do this, but we will need a bit more coding:

```Java
(SimpleState belief) -> actionstate_ -> {
     int x = rnd.nextInt(11) ;
     ((ConsoleEnvironment) belief.env()).println("Proposing " + x + " ...");
     return x ;
     })
```

Internally though, an action must be an instance of `nl.uu.cs.aplib.MainConcepts.Action`. Using a method called `action(f)` we can turn a lambda expression such as above into an instance of `Action`.

```Java
var a0 = action((SimpleState belief) -> actionstate_ -> {
     int x = rnd.nextInt(11) ;
     ... ;
     return x ;
   })) ;
```

An action can be guarded too. For example, suppose our state `belief` is of type `MyState`, which has an integer field called `counter`, and we want `a0` above to be only **enabled** (executable) when this counter has an odd value, we can code this as follows:

```Java
var a0withCondition = action((MyState belief) -> actionstate_ -> {
     int x = rnd.nextInt(11) ;
     ... ;
     return x ;
   }))
   . on_((MyState belief) -> belief.counter % 2 != 0) ;
```


There is one more technical detail: an agent actually wants to have a strategy rather than an action. A 'strategy' is an instance of `nl.uu.cs.aplib.MainConcepts.Strategy`. Although conceptually a single action is also a strategy, technically we need to wrap it to become a `Strategy`. The method `lift()` will do that.  So... here is the incantation to turn the previous lambda-expression to a `Strategy`:

```Java
var guessing = action((SimpleState belief) -> actionstate_ -> {
     int x = rnd.nextInt(11) ;
     ... ;
     return x ;
   }))
   . lift() ;
```


#### Attaching a strategy to a Goal

Recall that previously we have created a goal called `g10`; here is now how we can attach the strategy `guessing` defined above, and then lift the resulting goal to a goal-tree:

```Java
GoalTree topgoal = g10.withStrategy(guessing).lift() ;
```

### Creating an agent, 2nd attempt

Now we have all the ingredients to create a working agent :) Here is the code for our `agent-X`:

```Java
// formulate a goal:
Goal g10 = goal("Guess a the magic number (10)").toSolve((Integer x) -> x == 10) ;

// defining a strategy as the goal solver:
var guessing = action("guessing")
   .do_((SimpleState belief) -> actionstate_ -> {
        int x = rnd.nextInt(11) ;
        ... ;
        return x ;
        })
        .lift() ;

// attach the strategy to the goal, and make it a goal-tree:
GoalTree topgoal = g10.withStrategy(guessing).lift() ;

// creating an agent; attaching a fresh state to it, and attaching the above goal to it:
var agentX = new BasicAgent()
            . attachState(new SimpleState()
            . setEnvironment(new ConsoleEnvironment()))
            . setGoal(topgoal) ;
```

An agent has a method `update()`. When invoked, it will search in its current strategies for actions which are enabled. If there is none `update()` will do nothing. If there are one or more enabled actions available, one is chosen randomly. Our `agent-X`, being a very simplistic agent, only has one action though, and it has no guard; so it is always enabled.

The easiest way to run an agent is by repeatedly calling its `update()` until the goal is solved. A goal can also be given a time budget. The goal would then be marked as failed if you have invoked `update()` too many times so that the budget is exhausted. Here is a simple loop, with intentional delay added that will run our `agent-X`:

```Java
while (topgoal.getStatus().inProgress()) {
   agentX.update();
   Thread.sleep(1500);
 }
```

### The full example

The full code of `agent-X` is below (ok well, below it is called `agent` rather than `agant-X` :D ). You can run the method `main` to run this demo. It will produce output that looks something like this:

```
Proposing 5 ...
Proposing 4 ...
Proposing 10 ...

** Goal status:
   Guess a the magic number (10): SUCCESS. Solved by guessing
   Budget:Infinity
   Consumed budget:4.0
```

```Java
package nl.uu.cs.aplib.ExampleUsages;

import static nl.uu.cs.aplib.AplibEDSL.*;
import nl.uu.cs.aplib.MainConcepts.*;
import nl.uu.cs.aplib.Environments.ConsoleEnvironment;

import java.util.Random;

public class MinimalDemo {
  static public void main(String[] args) {
    Goal g = goal("Guess a the magic number (10)").toSolve((Integer x) -> x == 10) ;

    Random rnd = new Random() ;

    // defining a single action as the goal solver:
    var guessing = action("guessing")
       .do_((SimpleState belief) -> actionstate_ -> {
            int x = rnd.nextInt(11) ;
            ((ConsoleEnvironment) belief.env()).println("Proposing " + x + " ...");
            return x ;
            })
            .lift() ;

    // attach the action to the goal, and make it a goal-tree:
    GoalTree topgoal = g.withStrategy(guessing).lift() ;

    // creating an agent; attaching a fresh state to it, and attaching the above goal to it:
    var agent = new BasicAgent()
                    . attachState(new SimpleState()
                    . setEnvironment(new ConsoleEnvironment()))
                    . setGoal(topgoal) ;

    // run the agent until it solves its goal:
    while (topgoal.getStatus().inProgress()) {
       agent.update();
       Thread.sleep(1500);
     }
     topgoal.printTreeStatus();
   }
}

```
