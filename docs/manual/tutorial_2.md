# Aplib Tutorial 2:  State and tactic
Author: Wishnu Prasetya

This tutorial will show you:

* how to create a customized state for your agent
* how to create a more complex tactic

Imagine a 'doctor' agent that asks questions to its patient about how he/she feels.
The agent's goal is to make the patient happy. In this tutorial our goal is not
to try to make this agent intelligent, but to simply demonstrate several
more advanced features of `aplib`. We will therefore implement a dumb doctor
agent that simply counts the number of interactions it has with the patient and
uses this as the measure of how happy the user is.


### Formulating the goal

The goal of our doctor agent can be formulated as follows, where happiness represents the patient's happiness. The value 5 is just some threshold we choose as the minimum happiness level we want to have.

```java
Goal g = goal("the-goal").toSolve((Integer happiness) -> happiness >= 5) ;
```

### Agent's state/belief

Recall that an agent needs a state (and in agent programming the agent's state is also called its _belief_), which is an instance of `nl.uu.cs.aplib.mainConcepts.SimpleState`. The class `SimpleState` itself has no field other than a hidden field called `env` that points to the Environment that is attached to the agent.

For the doctor agent, we will need at least one variable to be incorporated in its state, which the agent can use to keep track the number of times it interacts with the patient. In other words, we need to **extend** `SimpleState`. Let's do it like this:

```java
static public class DoctorBelief extends SimpleState {
		Integer patientHappiness = 0 ;
	}
```

The new state representation (or state 'structure') is called `DoctorBelief`, which contains an integer field called `patientHappiness`, which is initialized to 0.

For later, it is also convenient to override the `env()` method of `SimpleState`, as follows:

```java
static public class DoctorBelief extends SimpleState {
		Integer patientHappiness = 0 ;

		@Override
		public ConsoleEnvironment env() { return (ConsoleEnvironment) super.env() ; }
	}
```

`env()` returns the Environment that is attached to the state. This would be an object of type `Environment`. In this example, we will use `ConsoleEnvironment`, which is a subclass of `Environment`. The above new definition simply returns whatever the old definition returns, but immediately casts the return environment to `ConsoleEnvironment`, which makes it a bit more convenient later.

### Actions

Before we formulate our tactic to solve the previously formulated goal, below is a number of actions that would be the building blocks for this tactic. For example, the action below ask a question to the patient:

```java  
var q1 = action("q1")
		.desc("To ask a 'smart' question to the patient :D")
		.do1((DoctorBelief belief) -> {
		   	belief.env().ask("Please explain a bit more...");
			  return ++belief.patientHappiness ;
		       })
		.lift();
```

The method `ask(s)` prints `s`, which is assumed to be a question or an instruction for the user, to the console and waits for the user answer (typed in the console).
Being a not-very-smart doctor, the agent just ignores the user's answer.
But it does increase the value of `patientHappiness` and returns the new
value to be tested on the previously shown goal.

We also add the following action, which prints the doctor's initial opening sentence towards the patient:

```java
var opening = action("opening")
			.desc("To say an opening question to the patient.")
			.do1((DoctorBelief belief) -> {
			  belief.env().ask("How do you feel today?");
			  return ++belief.patientHappiness ;
		  })
		  .lift()
		  ;
```

We can add few more actions, e.g. variations of `q1` :

```java
	  var q2 = action("q2")
			.desc("Another 'smart' question to pose to the patient ;)")
			.do_( ... )
			.lift() ;
```

### Tactic

The tactic below would apply the `opening` action, if it is enabled, and else one of the question-actions `q1` ... `qn`. The question-actions have no conditions, and are thus always enabled. On the other hand, the action `opening` does have a condition, namely that `patientHappiness` must be 0 (and consequently it is only enabled on the agent's first tick).

```java
var S = FIRSTof(
    		opening.on_((DoctorBelief belief) -> belief.patientHappiness == 0) ,
    		ANYof(q1,q2,...)
    	)) ;  
```

And now we can attach this tactic to our goal, and lift it to make a goal-tree:

```
var topgoal = g.withTactic(S).lift()
```

### Creating and running the doctor agent

The following code create and configure our doctor agent. Notice how we now attach the extended state structure, `DoctorBelief`, rather than the default (and empty) `SimpleState`, to the agent.

```java
var belief = new DoctorBelief() ;
belief.setEnvironment(new ConsoleEnvironment()) ;      
var doctorAgent = new BasicAgent()
                 . attachState(belief)
                 . setGoal(topgoal) ;
```

We can now run this agent as follows:

```java
while (topgoal.getStatus().inProgress()) {
    	  doctorAgent.update();
      }
```      

### The full example

The full code of `agent-X` can be found in [`nl.uu.cs.aplib.exampleUsages.DumbDoctor`](../../src/main/java/nl/uu/cs/aplib/exampleUsages/DumbDoctor.java). You can run the method `main` to run this demo. A sample of its interactions is shown below:

```
How do you feel today?
# A bit frustrated...
Please explain a bit more...
# I am working on a paper, the deadline is approaching.
I see... And why is that?
# Because it is almost sunday.
I see... And why is that?
# It just told you.
Please explain a bit more...
# Well, see... everything is ready, except a good title.
I am glad you are happier now :)

** Goal status:
   the-goal: SUCCESS. Solved by a1
   Budget:Infinity
   Consumed budget:192199.0
```
