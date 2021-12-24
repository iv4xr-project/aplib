# iv4xr-core Tutorial 3: Checking LTL Properties
Author: Wishnu Prasetya

**Prerequisite:** you have read at least the following tutorials:

* [`iv4xr` Tutorial 1](./testagent_tutorial_1.md), explaining the basic of how to use an `iv4xr` agent for testing.
* [`iv4xr` Tutorial 2](./testagent_tutorial_2.md), explaining an architecture where we pretend that the program to test is an external program. We will use this architecture in this tutorial.

LTL stands for 'Linear Temporal Logic'. We will not use the 'logic' part as it is really useful for our purpose (testing). The part of LTL that we will use is its concepts of sequence predicate and the notation/operators to express such a predicate. If we imagine a program with a state _s_, and _s.x_ denote the value of the variable _x_ in that state, a formula like _s.x>0_ is called a _state predicate_. It is a function that can be evaluated on a program state, and gives a true or false. A state predicate can be used to assert the correctness of a state, e.g. to say that after interacting with a program by pushing some button, we expect its state to satisfy the state predicate _s.x > 0_. A state predicate cannot however express properties over an entire execution.  We can see an execution of a program as a sequence of states that were passed by the program during the execution. With a sequence predicate we can say for example that throughout such an execution the value of s.x is always positive, or else that it eventually become negative (without being specific when this exactly happens).

In LTL we have operators like these:

  * ϕ~1~ = _[] s.x > 0_ ("**always** p")to say that the value of s.x is always positive.
  * ϕ~2~ = _<> s.x <= 0_ ("**eventually** p") to say that eventually the value of s.x will drop to 0 or less.
  * ϕ~3~ = _**X** s.x == 1_ ("**next** p") to say that in the next state the value of s.x would be 1.
  * They can be nested, e.g. ϕ~4~ = _<>[] s.x>2_ to say that eventually the value of s.x will be >2, and afterwhich it will remain so.

For example, given a sequence σ = [1,0,2,2,3,3,4] of the value of s.x, we have:

  * ϕ~1~.**sat**(σ) = UNSAT
  * ϕ~2~.**sat**(σ) = SAT
  * ϕ~3~.**sat**(σ) = UNSAT
  * ϕ~4~.**sat**(σ) = SAT

We can configure a test-agent so that it collects the **trace** of its execution when it is run. A trace is not literally a sequence of states passed during the execution, but rather a sequence of 'data point', and each data-point is a set of name-value pairs. Each data point can be thought as representing some selected part of the program actual state. E.g. it could store the value of s.x and s.z, but not s.y.

Since a trace is thus a sequence of values, we can subject it to checking against LTL properties, like in the example above. Let us see how we can get a trace from the test-agent and then how to check LTL properties on such a trace.


### Setup

Consider again the setup as in [`iv4xr` Tutorial 2](./testagent_tutorial_2.md) where we want to test a simple game called GCD game. Let us create an agent called "agentSmith", with some-role-name (which does not matter for this tutorial):

```java
var agent = new TestAgent("agentSmith","some role") ;
		agent . attachState(new MyState())
		      . attachEnvironment(new GCDEnv())
		      . setTestDataCollector(new TestDataCollector())
		      . setGoal(topgoal);
```

Well, before we get to that let's first create a test agent, and a goal for it. The goal does not matter for this tutorial; just something so that the agent has something to do:


```java
var agent = new TestAgent("agentSmith","some role") ;
```

Let's also define a goal. It does not matter what; for this example we just need the agent to do something. In this case the goal is to get the agent to the grid location (100,99) in the GCD-game:

```java
var topgoal = goal("tg")
			  .toSolve((MyState S) -> S.x == 100 && S.y == 99)
			  .withTactic(...)
			  .lift();
```

Let's attach a state and a proper environment (in this case: an instance of GCDEnv) to the agent, and we assign the above goal to the agent too:

```java
agent . attachState(new MyState())
	  . attachEnvironment(new GCDEnv())
	  . setTestDataCollector(new TestDataCollector())
      . setGoal(topgoal);
```

### Data-collector

To collect trace we will need to attach a data-collector to the agent. This is already done above through the method `setTestDataCollector()`.

### Instrumenter

We also need to attach an 'instrumenter', which is a point that extract values from a state (you decide which values/parts of the state to extract), and bundle them in a 'data-point'. Technically, this data point is a set of name-value pairs (more precisely, an instance of `Pair<String,Number>[]`). Suppose we already have a method that acts as such an instrumenter:

```java
Pair<String,Number>[] instrumenter(MyState st) { ... }
```

This is how to attach it to the agent:

```java
agent.withScalarInstrumenter(state -> instrumenter((MyState) state))
```

The following instrumented is used:

```java
Pair<String,Number>[] instrumenter(MyState st) {
	Pair<String,Number>[] out = new Pair[5] ;
	out[0] = new Pair<String,Number>("x",st.x) ;
	out[1] = new Pair<String,Number>("z",st.y) ;
	out[2] = new Pair<String,Number>("time",st.time) ;
	out[3] = new Pair<String,Number>("gcd",st.gcd) ;
	out[4] = new Pair<String,Number>("win",st.win ? 1 : 0) ;
	return out ;
}
```

### Running the agent and collecting the trace

The following will run the agent:

```java
while (!topgoal.getStatus().success()) agent.update() ;
```

After that, `agent.getTestDataCollector().getTestAgentScalarsTrace(agent.getId())` will give us the trace that was collected when the agent was ran above. However, let us clean this trace a bit so that we get a list/sequence of values:

```java
List<Map<String,Number>> trace = agent
				. getTestDataCollector()
				. getTestAgentScalarsTrace(agent.getId())
		        . stream()
		        . map(event -> event.values) . collect(Collectors.toList());
```

### LTL properties and checking them on a trace

Here are some examples of formulating LTL properties:

1. The value of x is never negative:

   `LTL<Map<String,Number>> ltl1 = always(st -> (int) st.get("x") >= 0)`

1. Eventually the value of x will become 100:

   `LTL<Map<String,Number>> ltl2 = eventually(st -> (int) st.get("x") == 100)`

1. The value of gcd will remain >1, until the agent wins.

   ```java
LTL<Map<String,Number>> ltl3 =
				now((Map<String,Number> st) -> (int) st.get("gcd") >1)
				.until(st -> (int) st.get("win") == 1)
```

Now, to check them on the trace obtained previously (above):

```java
assertTrue(ltl1.sat(trace) == SATVerdict.SAT) ;
assertTrue(ltl2.sat(trace) == SATVerdict.SAT) ;
assertTrue(ltl3.sat(trace) == SATVerdict.SAT) ;
```
