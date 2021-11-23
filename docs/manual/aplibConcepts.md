# Aplib Concepts

Author: Wishnu Prasetya, Utrecht University, 2019


### Agent

We define an **agent-based system** as a system consisting of an **environment** and one or more agents trying to control the environment. This environment can be a physical device, or another software, or humans connected to the agents through some interface. Conceptually, such an environment can be regarded as a **stateful** and **autonomous** program ('autonomous' means that it can at anytime change its state on its own decision). It may even be non-deterministic.

An **agent**, in the nutshell, as a stateful program that interacts with an environment. It has a purpose/goal, which can be as simple as to observe certain things in the environment, or a more complicated goal of nudging/moving the environment towards a certain state,

The initial state of the agent is its state when it is created. At this point, it is assumed the environment already exists.

The execution of the agent is a discrete tick-driven execution. When a tick comes, the agent samples the environment state, and then it does its own action which can change its own state as well as the environment. The environment is not required to run synchronously with the agent. All we say is that the agent will sample the environment state at every tick. Between ticks, there can be multiple times the environment changes its state.

We can distinguish between a **subservient** and an **autonomous** agent. A subservient agent has a master controlling when a tick is issued. If the master does not send a tick, the agent does nothing. An autonomous agent, as the name says, runs on its own without needing a master to tick it. The agent basically controls its own ticks. In implementation term, subservient agents can be run in one thread, the same thread as their master. On the other hand, autonomous agents require their own threads to run.

> Note: sending a tick to an `aplib` agent is implemented as invoking the agent's `update()` method.


### Simple goal and action

An agent can be given a goal. Without a goal, the agent will not do anything. A **goal** G is a **predicate** over some domain, say, U. When given a goal, the agent will seek to find a proposal x∈U that satisfies G. If such an x is found, the goal is solved and is detached from the agent.

For example a goal `(𝜆x → x=99)` is a goal that is solved only if the agent proposes 99. A goal `(𝜆x → isPrime(x))` is solved if the agent proposes a prime number.

When created, agents are in principle blank: they have no behavior, so they wouldn’t have a clue how to solve even a simple goal. When giving a goal G to an agent, we will require G to be accompanied by a “tactic” that acts as a solver. This tactic will be invoked at every tick until the goal is solved. We can also specify a computation "budget" for solving G; when this budget is used up, the agent will stop trying and the goal is marked as failed. The goal is said to be **closed** if it is either solved or failed.

The simplest form of a tactic is called **action**. An action 𝛼 is a pair a ▷ f where a is called the **guard** of 𝛼, and f specifies the **effect** of the action. Let’s first consider a simple setup. An agent A is given a goal G with action a ▷ f as the tactic to solve G, and computation budget B. The execution of A goes as follows.

 ##### algorithm _actionExec_(𝛼), where 𝛼 = a ▷ f

When a tick comes:

1. If the budget B runs out, G is marked as **failed** and is detached from the agent. The algorithm is then done.

2. **Else**, the agent samples the environment state.

3. The guard a is evaluated on A’s state and the newly sampled environment’s state.

  * If a evaluates to true, the action 𝛼 is said to be **enabled** on the current state. The effect f is then executed:

      * This execution costs something: the budget B is subtracted with some amount. This amount is determined by the "cost function" associated to the agent. A simple cost function can for example defines that this cost is a fixed cost of 1 unit.

      * Executing the effect f may change the agent's as well as the environment's state.

      * It may also produce a proposal x, which the agent then passes to the goal G to be inspected. If G(x) is true, the goal is solved and marked as **success**. The solution can be stored within G itself, and then G is detached from the agent.

  * If a evaluates to false, the action's effect will not be executed. Hence, the agent does nothing. Note that in some future tick, the environment may change enough to cause the “unblocking” of a.

##### Query guarded actions

Rather than having an action guarded by a predicate, we can also have actions of the form 𝛼 = q ▷ f where q is a query on the agent state.

A predicate returns true or false. E.g. the predicate `(𝜆state → isPrime(m(state))` is true on states where `m(state)` would result
a prime number. In some situation, the f part of 𝛼 maybe interested in not only knowing that it is enabled, but also in the value of `m(state)` that solves the predicate. Of course it can then invoke m again, but this is wasteful.

In the second form, q is a 'query', which is a function that can be evaluated on the agent state to search for some value. The action 𝛼 is said to be enabled if q(state) results in a non-null value; otherwise 𝛼 is not enabled. So, when the agent checks if 𝛼 is executable, it will invoke q and checks if it returns a non-null value. This value is also stored in the action state, which can be inspected by f, without having to re-execute q.

##### Distance-based goals

We have said that a goal is a predicate. However a predicate only return either a true or a false. If it gives a false, it does not give further clue on how far a given proposal is from being a solution. We will therefore also allow a goal to be formulated as a function g(x) that returns a real number. A proposal x is consider a solution only if |g(x)| returns a value which is less than some pre-determined 𝜀. The value of g(x) represents thus the 'distance' to a real solution.
The agent can inspect the state of its current goal to know the distance of its last proposal to being a solution, and can use this information to decide its next action.

### Complex goal (goal-structure)

Inevitably, there will be goals which are too difficult to solve with typical tactics. To mitigate this, we will allow the user to split a goal into smaller chunks, which at the lowest level are, hopefully, feasible to solve. To support this concept, we generalize the concept of goal to goal-structure. A **goal-structure** is conceptually is just a goal, but it has so-called _combinators_ to construct it from smaller goal-structures.

There are several combinators for building a goal-structure:

1. If G is a goal, **lift**(G) is a goal-structure, consisting of a single leaf, namely G. This goal-structure is solved/failed is G is solved/failed.

2. If h0, h1, ... are goal-structures, then 𝛨 = **SEQ**(h0,h1,...) is also a goal-structure. 𝛨 is solved if all h0, h1, ... are solved, and are moreover solved in that specific order. If one of them fails, 𝛨 fails.

3. If h0, h1, ... are goal-structures, then 𝛨 = **FIRSTof**(h0,h1,...) is also a goal-structure. The subgoal h0, h1, ... are tried sequentially in that order. 𝛨 is solved if one of h0, h1, … is solved. 𝛨 fails of all the subgoals h0, h1, … fail.

4. If h is a goal structure, **REPEAT**(h) is also a goal structure. If h is solved, so is **REPEAT**(h). If h fails, it will be tried again if the goal structure **REPEAT**(h) has some budget left. Else the latter also fails.

So, a goal-structure 𝛨 would have goals as leaves, each would require an action to be specified as its “solver”. To emphasize the distinction between leaf and non-leaf goal-structure, those goals in the leaves are also called **primitive goals**.

Rather than giving a goal to an agent, we now give a goal-structure. The goal-structure that is given to an agent is called **top-level** goal-structure (or simply top-level goal).

Note that only primitive goals of a top-level goal-structure will trigger computation by the agent (in the sense of executing some tactics). Not all primitive goals will eventually be tried (e.g. when a subgoal in a **SEQ** fails, the **SEQ** node fails; the remaining subgoals of the same **SEQ** node does not have to be inspected). However, when a primitive goal is picked up by the agent, the previously defined execution model of (primitive) goals insists that the agent stays on this goal over multiple ticks if needed, until either the primitive goal is solved, or until the allocated budget for that goal runs out, or until the agent executes an abort action (will be explained later). In other words, the execution of a goal cannot be interrupted by another goal.


##### Dynamically adjusting the goal structures

`Aplib` agents do have some methods to dynamically insert a new goal to its current goal structure, or to remove some substructure.

1. Inserting a new goal structure **after** the current subgoal. For example suppose the topgoal is:

   > **FIRSTof**(SEQ(g1,g2),g3)

   Suppose g1 is the current goal. Inserting a new goal structure G at the **after** position would modify the topgoal to:

   > **FIRSTof**(SEQ(g1,**G**,g2),g3)

   This is useful when the solver of g1 wants to plan ahead and insert G as an intermediate goal towards ultimately solving g2. Note that this insertion happens **dynamically** because it is decided at the runtime by g1's solver.

2. Inserting a new goal structure **before** the current subgoal. Consider again this as the topgoal:

   > **FIRSTof**(SEQ(g1,g2),g3)

   Suppose g1 is the current goal. Inserting a new goal structure G at the **before** position would modify the topgoal to:

   > **FIRSTof**(SEQ(**REPEAT**(**SEQ**(G,g1)),g2),g3)

   This is useful when g1 turns out to be too hard to be solved directly. The solver can then decide to introduce G **before** g1. When g1 fails (well, it has too hard), the introduction of the **REPEAT** node will try the underlying **SEQ** node. In turn, this will first focus on solving G. If after solving G, g1 can be solved, the entire **REPEAT** node will be solved as well. If g1 still fails, the **REPEAT** node will retry SEQ(G,g1), until it runs out its budget. The budget set for the **REPEAT** node is the same as the budget that the original parent of g1 had.

3. There is also a method to remove a goal structure, can be useful to cancel some future goal.   


### Budgeting

Every goal structure G (including if it is a goal) has two budget related attributes:

* `bmax` specifying the maximum computation budget G can have. If left unspecified, this is +∞.
* `budget`, representing G's current budget.

Executing an action's effect consumes some of G's `budget`. Furthermore, execution any action within G is only possible if `budget` > 0.

Suppose now an agent is given G0 as the goal structure to work on. So, G0 is the top level goal. We can specify its initial computation budget B0. However. the budget that G0 will actually get will be capped by its `bmax`. So, initially `G0.budget` would be `min(B0,G0.bmax)`. If no B0 is specified, then it is assumed to be +∞.

At any time, the agent will focus at just one goal. This is its current goal. Note that when a goal is current, then all its ancestors are also current. The converse is not always true: when a goal structure is current, not all of its children would be current (though: exactly one of its children is current).

When a goal g becomes current, some budget is allocated to it. In otherwords, `g.budget` will have to be set to a new value. This value would be the minimum of `g.bmax` and the budget C available at its parent. If the parent was already current, then this C would be this parent's current budget (so, `g.parent.budget`). Else, if the parent is also just becoming current, its new budget is recursively determined in the same was as that of g.

Suppose g is the current goal. Suppose an action f of g is executed (that is, its effect is executed). This costs some budget. The default is a fixed cost of 1 unit. This will be subtracted from `g.budget`, but additionally also from the budget of all ancestors of g.

If the budget of a current goal structure becomes 0 or less while it is still unsolved, it is declared as failed.

### Tactic

Recall that each primitive goal must be accompanied by a 'tactic'. The simplest type of tactic consists of only one action. We have discussed this before. A more powerful tactic can be written by composing smaller tactics. The following are combinators to construct tactics:

1. If 𝛼 = a → f is an action, **lift**(𝛼) is a tactic.

2. If T0, T1, ... are tactics, T = **SEQ**(T0,T1,...) is also a tactic.

3. **FIRSTof**(T0,T1,...) is also a tactic.

4. **ANYof**(T0,T1,...) is also a tactic.


To solve a primitive goal, we can now use a tactic rather than just an action.

Informally, **SEQ**(T0,T1,...) is a tactic that invokes the subtactics T0,T1,... in sequence; so after T<sub>k</sub> is “completed”, the agent will continue with T<sub>k+1</sub>. T = **FIRSTof**(T0,T1,...) is also a tactic that first evaluates its sub-tactics in the current state. They will be evaluated in the order as they appear, so T0,T1,... and so on. (And they will only be evaluated first, not executed). T will execute **the first** T<sub>k</sub> that is enabled in the current state (has an action that is enabled in the current state). In contrast, T = **ANYof**(T0,T1,...) will choose **any** of the sub-tactics which are enabled.

Just like action, a tactic is executed repeatedly until the goal is solved, or until its budget runs out. Due to the presence of SEQs in a tactic, its execution may require multiple actions to be executed over multiple ticks. When such an intra-tactic sequence reaches its end, we say that the tactic has executed one “iteration”. If this does not solve the goal, at the next tick the agent will reset the sequence, and starts a new iteration. Defining tactic execution more precisely requires a bit more technicalities. We will first introduce some auxiliary concepts.  

Consider a tactic _Troot_ is given to the agent to solve some primitive goal G. If T is some sub-tactic of _Troot_ that the agent is just worked on, and managed to complete it, we need to define **next**(T), which should be the tactic within _Troot_ that the agent should execute next. This is defined as follows:


1. T is **not** a leaf.

   * If T is a child of U = **FIRSTof**(...) or U = **ANYof**(...), then **next**(T) = **next**(U).

   * If T is a child of U = **SEQ**(...), we have two subcases: (a) T is a k-th child, and **not** the last child of U, then **next**(T) = the k+1th child of U; and (b) T is the **last child** of U, then **next**(T) = **next**(U).

   * T = _Troot_ (so it has no parent); then **next**(T) = null.

1. **If** T is a **leaf** tactic, so it is of the form lift(𝛼) where 𝛼 is an action. By default, when an action is executed in the current tick, at the end of the tick the action is considered as “completed”. But there is an exception that will be explained later, that will make it possible to let a single action to run repetitively over multiple ticks. So we have two subcases:

  * 𝛼 is marked as completed. Then **next**(T) is defined the same as case 1 above, when T is not a leaf.

  * 𝛼 is **not** marked as **completed**. Then **next**(T) = T.


Let’s write **first**(T,u) to denote the set of actions (the leaves) under T which can be executed as T’s first actions, and are moreover **enabled** on the current state u. We define it as follows:

1. **first**(**lift**(𝛼),u) = { 𝛼 } if 𝛼 is enabled on u, else it is ∅.

2. **first**(SEQ(T0,...),u) = **first**(T0,u).

3. **first**(**ANYof**(T0,T1,...),u) = **first**(T0,u) ∪ **first**(T1,u) ∪ ...

4. If T = **FIRSTof**(T0,T1,...), **first**(T) = **first**(T0,u), if it is non-empty, else it is  **first**(T1,u), if it is non-empty, etc. If the **first** of all S<sub>k</sub> is empty,  then **first**(T) is also empty.


Now we can define the execution of _Troot_ as follows. We will maintain a variable called _currentTactic_ to point to the current node/sub-tactic of Troot the agent is currently using.


##### Algorithm _tactic-dispacther_(Troot)

Initially: _currentTactic = Troot_

When a tick arrives:

1. Let _candidates_ := **first**(_currentTactic_)

2. **if** _candidates_ is empty, the tick is done.

3. **Otherwise**, the agent chooses one action 𝛼 from _candidates_ and executes it according to the algorithm _actionExec_(𝛼).
Before the tick is done, we update _currentTactic_ as follows:

   * **if** 𝛼 is marked as **completed**, then _currentTactic_ := **next**(𝛼), **else** _currentTactic_ = 𝛼.

   * if _currentTactic = null_, we cycle back to _currentTactic_ := _Troot_.

Choosing one action from a set of candidates in step-3 is called  **deliberation**. The easiest deliberation method is to just choose randomly, but indeed it would be interesting to explore more intelligent deliberation.

##### Abort action

Sometimes it is better to just give up on a goal, rather than wasting computing budget on it. To allow this, a tactic can include a special action called abort (which can also be guarded). If invoked, it will mark the current goal as being failed. Note that this does not necessarily mean that the topgoal will fail as well. E.g. if the topgoal is G = **FIRSTof**(g1,g2), if g1 fails, g2 might still succeed and hence still solving G.

##### Persistent action

Other than the top-level implicit iterative execution of a tactic, so far we have no means to program a tactic that requires an inner loop. We can still write **SEQ**(T,T,..) that will do T k number of times, but this k must be known upfront. So far there is no direct way to have a tactic that repeatedly does T until some condition holds, and then to continue with another tactic, say, U.
To facilitate such a tactic we will add one more construct: if f is an effect and a is a predicate, f **until** a specifies an action.

The action f **until** a is always enabled. When executed, the effect f is executed. After that, just before the tick ends, the guard a is tested. If it is true, the action is marked as **completed**, and else it is considered as incomplete. While it is still incomplete, at the next tick the agent will persist on executing it again.


### Intelligent agents

`aplib` does not provide any built-in AI (this would require [Artificial General Intelligence](https://en.wikipedia.org/wiki/Artificial_general_intelligence)/AGI which Science is still far from). Instead, `aplib` provides some design patterns where you can build intelligence into your agents:

* **Action level.** Actions can access Prolog through the class ``StateWithProlog``, allowing them to do Prolog-based inference on their state/belief.

* **Declarative tactic programming.** Our concept of tactic allows some degree of declarativeness when programming a goal solver. By using guarded actions we 'only' need to specify when an action are executable, without having to program the specific order in which the actions have to be invoked.

* **Deliberation.** A tactic is essentially a set of actions where you declaratively specify when each action can be executed. You can shift more responsibility to the agent by letting it more space to make the decision of which action to do at each current state, rather than that it always follow your strict programming. `Aplib` agents delegate the deliberation process to an instance of a class called `Deliberation`. The standard implementation is to just choose actions randomly, if there are indeed multiple actions enabled in the current state. Each agent has by default an instance of `Deliberation`, but you can reconfigure it uses your own implementation of `Deliberation`, e.g. a subclass that implements a smarter deliberating process, e.g. to make the decisions trainable with machine learning (e.g. with RL).

### Multi agent

In a multi agent setup we have mutiple agents operating on the same shared environment. To facilitate cooperation between these agents, `aplib` allows agents to **asynchronously** send messages to each other. To facilitate message routing, `aplib` uses a so-called _communication node_. To be able to send and receive a message an agent need to register to such a node. Each agent should have a unique ID too, and a role. Three modes of messaging are available:

* **Singlecast** is for sending a message to a single target agent.
* **Broadcase** is to send a message to all agents registered to the same communication node.
* **Rolecast** is to send a message to all agents registered to the same communication node, and sharing a specified role.

### Verifying correctness

The semantic of an agent-based system can be defined in terms of how the state of its environment evolves. For example if we assume that this environment only allows its state to be changed through a set of _atomic_ primitives ('atomic' here means that during the invocation of such a primitive it can assume to have exclusive access to the environment, that no other primitive can in the mean time changes the environment state), then we can use LTL to express the correctness of an agent-based system. `Aplib` does provide an implementation of LTL. It allows LTL formulas to be constructed and evaluated either on a sequence of states, or they can be evaluated incrementally by giving them one state at a time (so, there is no need to keep track of a 'history' of states, which can be expensive). LTL properties can be used for these purposes, for example:

* To do runtime verification. We can hook LTL formulas as correctness specifications. We can adjust the agent to make it send its current state to these formulas at each tick.

* We can use LTL formulas as oracles during testing.

* Bounded model checking. `aplib` allows autonomous agents to be deployed as subservient agents, and hence they can be ticked under the full control of a model checker. If we can also control the pace of the environment, we can basically do model checking.
