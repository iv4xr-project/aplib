# DSL for Constructing Goals and Tactics

'DSL' stands for Domain Specific Language.

_tactic_ ::=
  * _action_**.lift()**
  * **ABORT()**
  * **ANYof(** _tactic_+ **)**
  * **FIRSTof(** _tactic_+ **)**
  * **SEQ(** _tactic_+ **)**

_action_ ::=
  * **action(** _name_ **).do1(** _function_ **).on(** _predicate_ **)**
  * **action(** _name_ **).do2(** _bifunction_ **).on(** _query_ **)**

_goal_ ::=
  * **goal(** _name_ **).toSolve(** _predicate_ **).withTactic(** _tactic_ **)**

_goal-structure_ ::=
  * _goal_**.lift()**
  * **SUCESS(** _comment_ **)**
  * **FAIL(** _comment_ **)**
  * **lift(** _predicate_ **)**
  * **lift(** _action_ **)**
  * **FIRSTof(** _goal-structure_+ **)**
  * **SEQ(** _goal-structure_+ **)**
  * **IFELSE(** _predicate_ **,** _goal-structure_ **,** _goal-structure **)**
  * **WHILEDO(** _predicate_ **,** _goal-structure_ **)**
  * **REPEAT(** _goal-structure_+ **)**
  * **DEPLOYonce(** _agent_ **,** _GoalFunction_ )  -- Note: a goal function is a function that evaluates the current agent state and constructs a goal.

Syntax for assignning a goal structure to an agent:

 * _agent_**.setGoal(** _goal-structure_ **)**

Syntax for dynamically adding and removing a sub-goal within the goal-structure that the agent has now:

 * _agent_**. addAfter(** _goal-structure_ **)**
 * _agent_**. addBefore(** _goal-structure_ **)**
 * _agent_**. remove(** _goal-structure_ **)**


Syntax of testing goals:

 * **testgoal(** _name_ **,** _test-agent_ **).toSolve(** _predicate_ **).invariant(** _verdictfunction_ **).withTactic(** _tactic_ **)** : this creates a new test-goal. A test-goal is an ordinary goal enriched with some testing-related feature. In the above this syntax we can specify an invariant/predicate to check when the goal is achieved. The invariant is expressed as a function that evaluates the agent state, and returns a verdict (pass/fail).

  _verdictfunction_ ::= **assertTrue_(** _assertion-name_ **,** _assertion-info_ **,** _predicate_ **)**

* **assertTrue_(** _test-agent_ **,** _assertion-name_ **,** _assertion-info_ **,** _predicate_ **)**  

# DSL for expressing LTL specifications/properties

'LTL' stands for _Linear Temporal Logic_.

_sequence-predicate_ ::=

  * **now(** _env-predicate_ **)**
  * **ltlAnd(** _sequence-predicate_ + **)**
  * **ltlNot(** _sequence-predicate_ **)**
  * **next(** _sequence-predicate_ **)**
  * **eventually(** _sequence-predicate_ **)**
  * _sequence-predicate_**.ltlUntil(** _sequence-predicate_ **)**
  * **always(** _sequence-predicate_ **)**

_bounded-LTL-predicate_ ::=

  **new BoundedLTL()**
  **. when(** _env-predicate_ **)**
  **. until(** _env-predicate_ **)
  **. thereis(** _sequence-predicate_ **)**
  **. withMaxLength(** _number_ **)**
