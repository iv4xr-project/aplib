# DSL for Constructing Goals and Tactics

'DSL' stands for Domain Specific Language.

**Basic terminology.** A **goal** can be given to an agent, and represents what the agent has to achieve/solve. A goal can also be a composition of other goals; this is called a **goal-structure**. A goal-structure can for example be composed of *n* goals which have to be solved in sequence.

A **tactic** is a program for solving a goal. Every leaf-goal in a goal structure must have a tactic associated with it. In turn, a tactic can be composed from **actions**. An action can be guarded, which defines if it is executable or not.

For explanation on how tactics are executed to solve a goal (or in other words: agents' execution model), [see this document ](aplibConcepts.md).

Most of the contsructs below are defined in:

  * [AplibEDSL.java](../../src/main/java/nl/uu/cs/aplib/AplibEDSL.java)
  * [Iv4xrEDSL.java](../../src/main/java/eu/iv4xr/framework/Iv4xrEDSL.java)


### Goal

The syntax of goals is shown below.

_goal_ ::=
  * **goal(** _name_ **).toSolve(** _predicate_ **).withTactic(** _tactic_ **)**

A _predicate_ is a Java-function that returns a boolean. It can be constructed using a lambda-expression. For example `(int x) -> x==10` is a predicate that is true when given a integer value 10. If this predicate is used as a goal, it specifies when the goal is considered as solved.

A goal needs a tactic; it specifies how to solve it.  The syntax of tactics is given later.

A goal-structure is a composition of goals, e.g. it can be a sequence of sub-goals that should be achieved in the order as the sequence specifies.

_goal-structure_ ::=
  * _goal_**.lift()**
  * **SUCESS(** _comment_ **)**
  * **FAIL(** _comment_ **)**
  * **lift(** _predicate_ **)**
  * **lift(** _action_ **)**
  * **FIRSTof(** _goal-structure_+ **)**
  * **SEQ(** _goal-structure_+ **)**
  * **IF(** _predicate_ **,** _goal-structure_ **,** _goal-structure **)**
  * **TRYIF(** _predicate_ **,** _goal-structure_ **,** _goal-structure **)**  
  * **WHILE(** _predicate_ **,** _goal-structure_ **)**
  * **REPEAT(** _goal-structure_+ **)**
  * **DEPLOYonce(** _agent_ **,** _GoalFunction_ )  -- Note: a goal function is a function that evaluates the current agent state and constructs a goal.
  * **DEPLOY(** _agent_ **,** _GoalFunction_ )  

The syntax for assigning a goal structure to an agent:

 * _agent_**.setGoal(** _goal-structure_ **)**

An agent can also dynamically add and remove goals in its goal-structure (the goal-structure that was given to the agent to solve). This using the following actions:

 * addAfter(** _GoalFunction_ **)**
 * addBefore(** _goal-_GoalFunction_ **)**
 * _agent_**. remove(** _goal-structure_ **)**

A testing-goal is a special kind of goal. It can only be given to a test-agent (an instance of the class `eu.iv4xr.framework.mainConcepts.TestAgent`).
It is just an ordinary goal, but additionally it allows an 'invariant' to be specified. When the goal is solved, this invariant will also be checked if it also holds. If it does not then we have a violation.

The syntax of testing goals:

 * **testgoal(** _name_ **,** _test-agent_ **).toSolve(** _predicate_ **).invariant(** _verdictfunction_ **).withTactic(** _tactic_ **)** : this creates a new test-goal. A test-goal is an ordinary goal enriched with some testing-related feature. In the above this syntax we can specify an invariant/predicate to check when the goal is achieved. The invariant is expressed as a function that evaluates the agent state, and returns a verdict (pass/fail).

  _verdictfunction_ ::= **assertTrue_(** _assertion-name_ **,** _assertion-info_ **,** _predicate_ **)**

* **assertTrue_(** _test-agent_ **,** _assertion-name_ **,** _assertion-info_ **,** _predicate_ **)**  

   This is actually a test-goal with a trivial 'true' as the goal to solve (so it will always be solved), and the asserted predicate as the invariant to check. Effectively, this checks whether the current agent state satisfies the invariant.

### Tactic and action

The syntax of tactics and actions is shown below.

_tactic_ ::=

* _action_**.lift()**
* **ABORT()**
* **ANYof(** _tactic_+ **)**
* **FIRSTof(** _tactic_+ **)**
**SEQ(** _tactic_+ **)**

_action_ ::=

* **action(** _name_ **).do1(** _function_ **).on(** _predicate_ **)**
* **action(** _name_ **).do2(** _bifunction_ **).on(** _query_ **)**

A _function_ is a Java-function, which can be formed with a lambda expression. For example `(int x) -> x*x` constructs a function that returns _2x_ given an integer _x_ as input. A bi-function is a Java-function that takes two arguments. Similarly, it can be constructed using a lambda-expression.


# DSL for expressing LTL specifications/properties

'LTL' stands for _Linear Temporal Logic_.
An LTL formula is essentially a sequence predicate. It can be evaluated on a sequence of values to give a judgement whether or not the sequence satisfies the formula. [LTL is explained here.](./LTL.md)
