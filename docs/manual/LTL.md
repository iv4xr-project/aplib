# LTL

`aplib` (aka iv4xr-core) comes with support for LTL and also provides an implementation of LTL bounded model checking.

LTL stands for [_Linear Temporal Logic_](https://en.wikipedia.org/wiki/Linear_temporal_logic). An LTL formula is essentially a sequence predicate. It can be evaluated on a sequence of values to give a judgement whether or not the sequence satisfies the formula. Available LTL operators/constructs are listed below. They are defined in the class [LTL.java](../../src/main/java/eu/iv4xr/framework/extensions/ltl/LTL.java)

An LTL formula can be constructed as follows:

_ltl_ ::=

  * **now(** _state-predicate_ **)**
  * **ltlAnd(** _ltl_ + **)**
  * **ltlOr(** _ltl_ + **)**
  * **ltlNot(** _ltl_ **)**
  * **next(** _ltl_ **)**
  * **eventually(** _ltl_ **)**
  * _ltl_**.until(** _ltl_ **)**
  * _ltl_**.weakUntil(** _ltl_ **)**
  * **always(** _ltl_ **)**

For example we can write: **eventually**(**now**(`state -> state.h > 0`)) to express a predicate that is true on a sequence of states where the value of `state.h` eventually becomes greater than 0.


The syntax to check an LTL-formula on a finite sequence:

_ltl_.**sat**(_sequence_)

For example, the sequence could be a trace collected from the execution of a test-agent. The semantic of this is that of finite-sequence semantic of LTL. This is a variation of the standard infinite-sequence semantic of LTL, but where future formulas such as **next**(_p_) and **eventually**(_p_) only holds on a sequence if the eventuality of _p_ is present in the sequence.

It is also possible to check LTL formulas on-line on a running agent (the formulas will then be checked on the sequence of states that the execution induces). See the following methods of the class [TestAgent](../../src/main/java/eu/iv4xr/framework/mainConcepts/TestAgent.java):

* _testagent_.`addLTL`(..)
* _testagent_.`resetLTLs`()
* _testagent_.`evaluateLTLs`()

In addition to LTL, we also provide 'bounded'-LTL. A bounded-LTL formula F is essentially a tuple (p,q,phi,n) where p and q are state predicates and phi is an LTL formula. Given a sequence, F holds on the sequence if every pq-segment in the sequence satisfies the LTL phi. A pq-segment is a segment that starts with a value satisfying p and ends in a value satisfying q and is maximal in the sense that it does not contain another pq-segment inside it, and its length is at most n.

  _bounded-LTL_ ::= **new BoundedLTL()**
     **. when(** _state-predicate_ **)**
     **. until(** _state-predicate_ **)**
     **. thereis(** _ltl_ **)**
     **. withMaxLength(** _number_ **)**

To check it on a sequence, the syntax is similar to LTL:

_bounded-LTL_.**sat**(sequence)

 # Model checker

A bounded model checker is used to verify that all executions of a problem, up to a certain depth/length, satisfy a given LTL formula. To be subjected to model checking, the program has to implement the interface [`ITargetModel`](../../src/main/java/eu/iv4xr/framework/extensions/ltl/ITargetModel.java).
An instance of `ITargetModel` can be thought to have a state, and there are transitions that can be executed. When a transition is executed, and it will move the model from its current state to another state.

Becase we do bounded model checking, the state space of the `ITargetModel` does not have to be finite.

Some key methods of `ITargetModel` that have to be provided to implement this interface are:

   * **getCurrentState**(): return the current state of the `ITargetModel`.
   * **execute**(_tr_): execute the transition _tr_ on the `ITargetModel`.
   * **backTrackToPreviousState**(): roll back the state of the `ITargetModel` to its previous state. This is an important method for model checking. Not that a program does not generally comes with an ability to roll back its state, so you have to implement this ability, e.g. by keeping track of a history of states along an execution.

Model checking is implemented by the classes [`BasicModelChecker`](../../src/main/java/eu/iv4xr/framework/extensions/ltl/BasicModelChecker.java)  and [`BuchiModelChecker`](../../src/main/java/eu/iv4xr/framework/extensions/ltl/BuchiModelChecker.java).

The basic model-checker can be used to check if there is a state satisfying a certain predicate reachable (from the `ITargetModel`'s initial state) through an execution up to a certain length:

```java
BasicModelChecker mc = new BasicModelChecker(M) // M is an instance of ITargetModel
System.out.println("" + mc.sat(p,k)) // check if a state satisfying a state predicated p is reachable within bound k
var tc = mc.find(p,k) // give the sequence of transitions that leads to p (if it is reachable)
```

The basic model checker can only be used to check the reachability of a given state predicate.
The Buchi model checker can be used to check if there is an execution of length maximum k that would satisfy a given LTL formula:

```java
BuchiModelChecker mc = new BuchiModelChecker(M) // M is an instance of ITargetModel
System.out.println("" + mc.sat(phi,k)) // check if there is an execution of length max. k that satisfy the LTL formula phi
var tc = mc.find(p,k) // give the sequence of transitions that satisfies the LTL phi
```
