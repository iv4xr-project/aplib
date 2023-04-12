# Aplib's Prolog Backend

`aplib` provides a Prolog backend, namely [tuprolog](http://alice.unibo.it/xwiki/bin/view/Tuprolog/), that your agent can use to do reasoning. The access to this backend is through the class `nl.uu.cs.aplib.Agents.State` whose rootclass is `nl.uu.cs.aplib.mainConcepts.SimpleState`. Recall that an instance of `SimpleState` represents an agent state. Any agent would need a state to be attached to it, and it should be an instance of `SimpleState`, as in:

```java
var state = new State() ; // we will use a State instead of SimpleState
var agent = new BasicAgent() . attachState(state)
```

By default, an instance of `State` has no Prolog-engine, but you can add it:

```java
state.attachProlog()
```
(the class `State` also gives you access to inter-agent messaging)

You (or your agent) can now add facts and rules to the engine through methods `state.prolog().facts(..)` and `state.prolog().add(..)`. You can subsequently use the methods `state.prolog().query(..)` and `state.prolog().test(...)` to query the Prolog-engine.

A Prolog-engine is useful when the agent needs to make complicated inference to help it makes decisions.
A strategy (for making decision) can be encoded as inference rules and added to the Prolog-engine. Then, as the agent proceeds over its cycles it can push relevant information as facts to the engine.
Inference is done by performing queries to the engine.

For example, to add reasoning rules we do the following:

```java
var state = (new State()) . attachProlog() ;
// let's first introduce some predicate-names:
var bossOf = predicate("bossOf") ;
var isBoss = predicate("isBoss") ;
// adding rules to the Prolog-engine
state.prolog().add(
   // bossOf(A,C) :- bossOf(A,B), bossOf(B,C)
   rule(bossOf.on("A","C"))
      . impBy(bossOf.on("A","B"))
      . and(bossOf.on("B","C")),
   // isBoss(A) :- bossOf(A,B)
   rule(isBoss.on("A")).impBy(bossOf.on("A","B"))
)
```

Suppose now we add the following facts to the Prolog-engine:

```java
state.prolog().facts(
   bossOf.on("mrCrab","spongeBob"),
   bossOf.on("spongeBob","patrick")
)
```

Now we can query the Prolog-engine, e.g. to check if patrick is a boss:

```java
state.prolog().test(isBoss.on("patrick")) // should return false
```

We can query the engine to give us a boss, if any:

```java
var boss = state.prolog().query(isBoss.on("X")).str_("X") ; // should contain "spongeBob"
```

There is an example of using Prolog in the class [` nl.uu.cs.aplib.exampleUsages.fiveGame.FiveGame_withAgent`](../../src/main/java/nl/uu/cs/aplib/ExampleUsages/FiveGame/FiveGame_withAgent.java). This class contains an `aplib` agent for playing a simple board game called _Five Game_. It is played on a board of _N x N_ by two players, each takes turn to place a piece (a cross for one player, and a circle for the other) on a still empty square on the board. The first player that can form a consecutive row or column or diagonal consisting of 5 pieces all of his pieces wins. The agent player is programmed to use some bits of Prolog reasoning as part of its tactic.
