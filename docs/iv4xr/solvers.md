# Solvers

We will use the term _solver_ to refer to an algorithm that calculate a solution for some problem. The problem can be thought to be formulated as a predicate _P_ over some domain _D_. A solution of this problem is any _x_ ∊ _D_ such that _P(x)_ is true.

In terms of agent programming, _P_ can express a goal for an agent, but it does not have to. In any case, having solvers help the agent in figure out what to do next, if it can formulate its goal, or fragment of its goal, in terms of a problem that a solver can solve.

Iv4xr provides the following solvers.

### Pathfinder

A pathfinder solves the problem of finding a path in a graph _G_, that goes from node _p_ to node _q_ in the graph. The graph _G_ is often called _navigation graph_. It can be thought to model physical travel in a world (which can be a virtual world). When two nodes are connected by an edge, it means that in this world travel between those two nodes are possible and unhindered.

An implementation of A* is available. Check the classes in the package `eu.iv4xr.framework.extensions.pathfinding`:

* class [`AStar`](../../src/main/java/eu/iv4xr/framework/extensions/pathfinding/AStar.java) : implementation of A*.
* class [`SimpleNavGraph`](../../src/main/java/eu/iv4xr/framework/extensions/pathfinding/SimpleNavGraph.java) : a navigation graph where you can add obstacles like doors whose state can change between blocking and unblocking.
* class [`SurfaceNavGraph`](../../src/main/java/eu/iv4xr/framework/extensions/pathfinding/SurfaceNavGraph.java). This extends `SimpleNavGraph`. Firstly, it incorporates an A* pathfinder. Secondly it allows nodes in the graph to be marked as "has been seen"; exploiting this information the class offer an algorithm to do exploration (it goes after e.g. the closest unseen but reachable node).
* class [`Sparse2DTiledSurface_NavGraph`](../../src/main/java/eu/iv4xr/framework/extensions/pathfinding/Sparse2DTiledSurface_NavGraph.java): like `SurfaceNavGraph` but the graph is restricted to a grid-like graph. This makes it easier to construct the graph. In fact, you don't have to construct it, as it will be constructed on the fly (in contrast, both `SurfaceNavGraph` and `SimpleNavGraph` do not have this on-the-fly construction feature).
* class [`LayeredAreasNavigation`](../../src/main/java/eu/iv4xr/framework/extensions/pathfinding/LayeredAreasNavigation.java): to support navigation over multiple game level (e.g. navigation in a multi-storey building).

### Model checker

A Linear Temporal Logic (LTL) bounded model checker is provided in the class [`BuchiModelChecker`](../../src/main/java/eu/iv4xr/framework/extensions/ltl/BuchiModelChecker.java). This model checker can check if a program implementing the interface [`ITargetModel`](../../src/main/java/eu/iv4xr/framework/extensions/ltl/ITargetModel.java) has an execution that satisfies a given LTL property/formula 𝜙. Since it is a bounded model checker, the target program _TP_ does not have to be finite state. It can be any program as long as it implements `ITargetModel` (most notably, the interface requires that the states _TP_ should be cloneable, and that we have an ability to backtrack to the program's previous state). As such, it can be used to solve any problem that can be expressed as a pair (_TP_, 𝜙).

The model checker does not actually take an LTL formula, but it first translates the LTL formula to a Buchi automaton. which is more expressive. So rather than searching for a solution of (_TP_, 𝜙) we can search for a solution of (_TP_,_B_) where _B_ is a Buchi automaton; in this case a 'solution' means an execution of _TP_ that is permitted by _B_.

### SA1 Online Search

Imagine a setup in e.g. a game where the state of a gameobject/entity _g_ can be switched to a state satisfying some predicate 𝝋 by interacting with another entity _b_ of certain types (e.g. keys, or switches). Suppose we want to switch _g_ to 𝝋, but we don't know which _b_ would trigger this (or we do not want to fix the x connection e.g. because it might change). The SA1 algorithm construct a goal structure for a test agent that will drive it to search for the right _b_. Inevitably, this will involve trying different candidates until the agent manages to switch _g_.

The SA1 solver is implemented in the class
[`Sa1Solver`](../../src/main/java/eu/iv4xr/framework/extensions/goalsAndTactics/Sa1Solver.java).
