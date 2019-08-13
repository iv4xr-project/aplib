# Aplib's Prolog Backend

`aplib` provides a Prolog backend, namely [tuprolog](http://alice.unibo.it/xwiki/bin/view/Tuprolog/), that your agent can use to do reasoning. The access to this backend is through the class `nl.uu.cs.aplib.Agents.StateWithProlog` whose rootclass is `nl.uu.cs.aplib.MainConcepts.SimpleState`. Recall that an instance of `SimpleState` represents an agent state. Any agent would need a state to be attached to it, and it should be an instance of `SimpleState`, as in:

```java
var state = new StateWithProlog() ;
var agent = new BasicAgent() . attachState(state)
```

An instance of `StateWithProlog` holds an instance of Prolog-engine. This engine is hidden from you, but when programming actions for your agents, you can access it through methods `state.addFacts(..)` and `state.addRules(..)` to add facts and reasoning rules to the Prolog-engine. You can subsequently use the methods `state.query(..)` and `state.test(...)` to query the Prolog-engine.

A Prolog-engine is useful when your have a non-trivial state structure and some actions of your agent need to inspect if a certain complex predicate P(X,Y,Z) has a solution in the agent's current state (instances for X,Y,Z that would make P true). Using Prolog allows you to declaratively describe how P can be inferred using one ore more inference rules, and leave it to a Prolog-engine to do the hard work of search for its solution. To be able to make use of this, you of course will need to formulate the inference rules. Additionally, you need to encode fragments of your agent's state that would be relevant for the reasoning as Prolog facts and send these facts to the Prolog-engine.

There is an example of using Prolog in the class [` nl.uu.cs.aplib.ExampleUsages.FiveGame.FiveGame_withAgent`](../../src/main/java/nl/uu/cs/aplib/ExampleUsages/FiveGame/FiveGame_withAgent.java). This class contains an `aplib` agent for playing a simple board game called _Five Game_. It is played on a board of _N x N_ by two players, each takes turn to place a piece of their respective color on a still empty square on the board. The first player that can form a consecutive row or column or diagonal consisting of 5 pieces all of his color wins. The agent player is programmed to use some bits of Prolog reasoning as part of its strategy.

For example, to add reasoning rules we do the following:

```java
var state = new StateWithProlog() ;
state.addRules(
   "bossOf(A,C) :- bossOf(A,B), bossOf(B,C)",
   "isBoss(A) :- bossOf(A,B)"
)
```

Suppose now we add the following facts to the Prolog-engine:

```java
state.addFacts(
   "bossOf(mrcrab,bob)",
   "bossOf(bob,patrick)"
)
```

Now we can query the Prolog-engine, e.g. to check if patrick is a boss:

```java
state.test("isBoss(patrick)") // should return false
```

We can query the engine to give us a boss, if any:

```java
var solution = state.query("isBoss(X)","X") ;
var boss = stringVal(solution[0]) ; // should contain "mrcrab"
```

The class `StateWithProlog` is btw also a subclass of `StateWithMessenger`. So it also gives you access to inter-agent messaging.
