# Aplib Concepts

Author: Wishnu Prasetya, Utrecht University, 2019


### Agent

We define an **agent-based system** as a system consisting of an **environment** and one or more agents trying to control the environment. This environment can be a physical device, or another software, or humans connected to the agents through some interface. Conceptually, such an environment can be regarded as an **stateful** and **autonomous** program ('autonomous' means that it can at anytime change its state on its own decision). It may even be non-deterministic.

An **agent**, in the nutshell, as a stateful program that interacts with an environment. The purpose of an agent is to solve some problem over the environment.

The initial state of the agent is its state when it is created. At this point, it is assumed the environment already exists.

The execution of the agent is a discrete tick-driven execution. When a tick comes, the agent samples the environment state, and then it does it own action which can change its own state as well as the environment. The environment is not required to run synchronously with the agent. All we say is that the agent will sample the environment state at every tick. Between ticks, there can be many times the environment changes its state.

We can distinguish between a **subservient** and an **autonomous** agent. A subservient agent has a master controlling when a tick is issued. If the master does not send a tick, the agent does nothing. An autonomous agent, as the name says, runs on its own without needing a master to tick it. The agent basically controls its own ticks. In implementation term, subservient agents can be run in one thread, the same thread as their master. On the other hand, autonomous agents require their own threads to run.

> Note: sending a tick to an `aplib` agent is implemented as invoking the agent's `update()` method.


### Simple goal and action

An agent can be given a goal. Without a goal, the agent will not do anything. A **goal** G is a **predicate** over some domain, say, U. When given a goal, the agent will seek to find a proposal x∈U that satisfies G. If such an x is found, the goal is solved and is detached from the agent.

For example a goal `(𝜆x → x=99)` is a goal that is solved only if the agent proposes 99. A goal `(𝜆x → isPrime(x))` is solved if the agent proposes a prime number.

When created, agents are in principle blank: they have no behavior, so they wouldn’t have a clue how to solve even a simple goal. When giving a goal G to an agent, we will require G to be accompanied by a “strategy” that acts as a solver. This strategy will be invoked at every tick until the goal is solved. We can also specify a time budget for solving G; when this budget is used up, the agent will stop trying and the goal is marked as failed. The goal is said to be **closed** if it is either solved or failed.

The simplest form of a strategy is called **action**. An action 𝛼 is a pair a → f where a is called the **guard** of 𝛼, and f specifies the **effect** of the action. Let’s first consider a simple setup. An agent A is given a goal G with action a → f as the strategy to solve G, and time budget B. The execution of A goes as follows.

 ##### algorithm _actionExec_(𝛼), where 𝛼 = a → f

When a tick comes:

1. If the budget B runs out, G is marked as **failed** and is detached from the agent. The algorithm is then done.

2. **Else**, the agent samples the environment state.

3. The guard a is evaluated on A’s state and the newly sampled environment’s state.

  * If a evaluates to true, the action 𝛼 is said to be **enabled** on the current state. The effect f is then executed. This may change the agent as well as the environment state, and it may produce a proposal x:

      * If G(x) is true, the goal is solved and marked as **success**. The solution can be stored within G itself, and then G is detached from the agent.

  * If a evaluates to false the agent does nothing. Note that in some future tick, the environment may change enough to cause the “unblocking” of a.

4. In all cases, before the turn ends, the budget B is subtracted with the time that has elapsed since the tick arrival.

### Complex goal (goal-tree)

Inevitably, there will be goals which are too difficult to solve with typical strategies. To mitigate this, we will allow the user to split a goal into smaller chunks, which at the lowest level are, hopefully, feasible to solve. To support this concept, we generalize the concept of goal to goal-tree. A **goal-tree** is conceptually is just a goal, but it has so-called _combinators_ to construct it from smaller goal-trees.

There are three combinators for building a goal-tree:

1. If G is a goal, **lift**(G) is a goal-tree, consisting of a single leaf, namely G. This goal-tree is solved/failed is G is solved/failed.

2. If h0, h1, ... are goal-trees, then 𝛨 = **SEQ**(h0,h1,...) is also a goal-tree. 𝛨 is solved if all h0, h1, ... are solved, and are moreover solved in that specific order. If one of them fails, 𝛨 fails.

3. If h0, h1, ... are goal-trees, then 𝛨 = **FIRSTof**(h0,h1,...) is also a goal-tree. The subgoal h0, h1, ... are tried sequentially in that order. 𝛨 is solved if one of h0, h1, … is solved. 𝛨 fails of all the subgoals h0, h1, … fail.


So, a goal-tree 𝛨 would have goals as leaves, each would require an action to be specified as its “solver”. To emphasize the distinction between leaf and non-leaf goal-tree, those goals in the leaves are also called **primitive goals**.

Rather than giving a goal to an agent, we now give a goal-tree. The goal-tree that is given to an agent is called **top-level** goal-tree (or simply top-level goal).

Note that only primitive goals of a top-level goal-tree will trigger computation by the agent (in the sense of executing some strategies). Not all primitive goals will eventually be tried (e.g. when a subgoal in a **SEQ** fails, the **SEQ** node fails; the remaining subgoals of the same **SEQ** node does not have to be inspected). However, when a primitive goal is picked up by the agent, the previously defined execution model of (primitive) goals insists that the agent stays on this goal over multiple ticks if needed, until either the primitive goal is solved, or until the allocated budget for that goal runs out, or until the agent executes an abort action (will be explained later). In other words, the execution of a goal cannot be interrupted by another goal.


### Budgeting

When specifying a top-level goal-tree, the user can also specify a time budget B that is available for the agent to solve the goal-tree. This is specified at the top level only. On the other hand, each primitive goal can specify minimum budget that it would ideally require. It is up to the agent to regulate how it distributes the starting budget it receives at the top level down to every subgoal.

Regardless of how the agent distributes the budget, when a primitive goal G is picked up by the agent, the agent must decide how much fraction of B it will give to G. Suppose a budget B’ ≤ B is given to G. If this B’ is less than the minimum demanded by G, the agent decides whether to still try G, or to declare G as failed. If the agent decides to do G anyway, it will then start solving G, which can potentially takes multiple ticks to do. The execution time of every tick will be subtracted from B’ and B. If B’ is exhausted, the goal G is marked as failed, and the agent will switch to another primitive goal, if there is any left.

### Strategy

Recall that each primitive goal must be accompanied by a 'strategy'. The simplest type of strategy consists of only one action. We have discussed this before. A more powerful strategy can be written by composing smaller strategies. The following are combinators to construct strategies:

1. If 𝛼 = a → f is an action, **lift**(𝛼) is a strategy.

2. If S0, S1, ... are strategies, T = **SEQ**(S0,S1,...) is also a strategy.

3. **FIRSTof**(S0,S1,...) is also a strategy.

4. **ANYof**(S0,S1,...) is also a strategy.


To solve a primitive goal, we can now use a strategy rather than just an action.

Informally, **SEQ**(S0,S1,...) is a strategy that invokes the substrategies S0,S1,... in sequence; so after Sk is “completed”, the agent will continue with Sk+1. T = **FIRSTof**(S0,S1,...) is also a strategy that first evaluates its sub-strategies in the current state. They will be evaluated in the order as they appear, so S0,S1,... and so on. (And they will only be evaluated first, not executed). T will execute **the first** Sk that is enabled in the current state (has an action that is enabled in the current state). In contrast, T = **ANYof**(S0,S1,...) will choose **any** of the sub-strategies which are enabled.

Just like action, a strategy is executed repeatedly until the goal is solved, or until its budget runs out. Due to the presence of SEQs in a strategy, its execution may require multiple actions to be executed over multiple ticks. When such an intra-strategy sequence reaches its end, we say that the strategy has executed one “iteration”. If this does not solve the goal, at the next tick the agent will reset the sequence, and starts a new iteration. Defining strategy execution more precisely requires a bit more technicalities. We will first introduce some auxiliary concepts.  

Consider a strategy _Troot_ is given to the agent to solve some primitive goal G. If S is some sub-strategy of _Troot_ that the agent is just worked on, and managed to complete it. Let’s define **next**(S) specifying which strategy within _Troot_ the agent should execute next. It is defined as follows:

1. **If** S is a **leaf** strategy, so it is of the form lift(𝛼) where 𝛼 is an action, and furthermore 𝛼 is not marked as **completed**, **next**(S) = S. By default, when an action is executed in the current tick, at the end of the tick the action is considered as “completed”. An exception will be explained later, to make it possible to let a single action to run repetitively over multiple ticks.

2. **Otherwise** (so, S is either a non-leaf, or it is a leaf and marked as completed), we have the following cases:

   * If S is a child of T = **FIRSTof**(...) or T = **ANYof**(...), then **next**(S) = **next**(T).

   * If S is a child of T = **SEQ**(...), we have two subcases: (a) S is a k-th child, and **not** the last child of T, then **next**(S) = the k+1th child of T; and (b) S is the **last child** of T, then **next**(S) = **next**(T).

   * S = _Troot_ (so it has no parent); then **next**(S) = null.


Let’s write **first**(S,u) to denote the set of actions (the leaves) under S which can be executed as S’s first actions, and are moreover **enabled** on the current state u. We define it as follows:

1. **first**(**lift**(𝛼),u) = { 𝛼 } if 𝛼 is enabled on u, else it is ∅.

2. **first**(SEQ(S0,...),u) = **first**(S0,u).

3. **first**(**ANYof**(S0,S1,...),u) = **first**(S0,u) ∪ **first**(S1,u) ∪ ...

4. If T = **FIRSTof**(S0,S1,...), **first**(T) = **first**(S0,u), if it is non-empty, else it is  **first**(S1,u), if it is non-empty, etc. If the **first** of all Sk is empty,  then **first**(T) is also empty.


Now we can define the execution of _Troot_ as follows. We will maintain a variable called _currentStrategy_ to point to the current node/sub-strategy of Troot the agent is currently using.


##### Algorithm _strategy-dispacther_(Troot)

Initially: _currentStrategy = Troot_

When a tick arrives:

1. Let _candidates_ := **first**(_currentStrategy_)

2. **if** _candidates_ is empty, the tick is done.

3. **Otherwise**, the agent chooses one action 𝛼 from _candidates_ and executes it according to the algorithm _actionExec_(𝛼).
Before the tick is done, we update _currentStrategy_ as follows:

   * **if** 𝛼 is marked as **completed**, then _currentStrategy_ := **next**(𝛼), **else** _currentStrategy_ = 𝛼.

   * if _currentStrategy = null_, we cycle back to _currentStrategy_ := _Troot_.

Choosing one action from a set of candidates in step-3 is called  **deliberation**. The easiest deliberation method is to just choose randomly, but indeed it would be interesting to explore more intelligent deliberation.

### Abort action

Sometimes it is better to just give up on a goal, rather than wasting computing budget on it. To allow this, a strategy can include a special action called abort (which can also be guarded). If invoked, it will mark the current goal as being failed. Note that this does not necessarily mean that the topgoal will fail as well. E.g. if the topgoal is g = **FIRSTof**(g1,g2), if g1 fails, g2 might still succeed and hence still solving g.

### Persistent action

Other than the top-level implicit iterative execution of a strategy, so far we have no means to program a strategy that requires an inner loop. We can still write **SEQ**(S,S,..) that will do S k number of times, but this k must be known upfront. So far there is no direct way to have a strategy that repeatedly does S until some condition holds, and then T.
To facilitate such a strategy we will add one more construct: if f is an effect and a is a predicate, f **until** a specifies an action.

The action f **until** a is always enabled. When executed, the effect f is executed. After that, just before the tick ends, the guard a is tested. If it is true, the action is marked as **completed**, and else it is considered as incomplete. While it is still incomplete, at the next tick the agent will persist on executing it again.


### Intelligent agents

`aplib` does not provide any built-in AI (this would require [Artificial General Intelligence](https://en.wikipedia.org/wiki/Artificial_general_intelligence)/AGI which Science is still far from). Instead, `aplib` provides some design patterns where you can build intelligence into your agents:

* **Action level.** Actions can access Prolog through the class ``StateWithProlog``, allowing them to do Prolog-based inference on their state/belief.

* **Declarative strategy programming.** Our concept of strategy allows some degree of declarativeness when programming a goal solver. By using guarded actions we 'only' need to specify when an action are executable, without having to program the specific order in which the actions have to be invoked.

* **Deliberation.** A strategy is essentially a set of actions where you declaratively specify when each action can be executed. You can shift more responsibility to the agent by letting it more space to make the decision of which action to do at each current state, rather than that it always follow your strict programming. `Aplib` agents delegate the deliberation process to an instance of a class called `Deliberation`. The standard implementation is to just choose actions randomly, if there are indeed multiple actions enabled in the current state. Each agent has by default an instance of `Deliberation`, but you can reconfigure it uses your own implementation of `Deliberation`, e.g. a subclass that implements a smarter deliberating process, e.g. to make the decisions trainable with machine learning (e.g. with RL).

### Multi agent

In a multi agent setup we have mutiple agents operating on the same shared environment. To facilitate cooperation between these agents, `aplib` allows agents to **asynchronously** send messages to each other. To facilitate message routing, `aplib` uses a so-called _communication node_. To be able to send and receive a message an agent need to register to such a node. Each agent should have a unique ID too, and a role. Three modes of messaging are available:

* **Singlecast** is for sending a message to a single target agent.
* **Broadcase** is to send a message to all agents registered to the same communication node.
* **Rolecast** is to send a message to all agents registered to the same communication node, and sharing a specified role.

### Verifying correctness

The semantic of an agent-based system can be defined in terms of how the state of its environment evolves. For example if we assume that this environment only allows its state to be changed through a set of _atomic_ primitives ('atomic' here means that during the invocation of such a primitive it can assume to have exclusive access to the environment, that no other primitive can in the mean time changes the environment state), then we can use LTL to express the correctness of an agent-based system. With proper instrumentation on the environment, the following verification methods are thinkable, all would require at least three-valued LTL (with judgement of valid, invalid, and inconclusive):

* Runtime verification of LTL properties, wrapped around the environment.

* Using LTL properties as oracles during testing (with the extra challenge of searching for executions that would elimnate 'inconclusive' judgement).

* Bounded model checking. `aplib` allows autonomous agents to be deployed as subservient agents, and hence be ticked under the full control of a model checker. If we can also control the pace of the environment, we can basically do model checking.
