# Aplib: an Agent Programming Library

`Aplib` is a Java library to program multi agent programs. It provides an architecture and a design pattern for programming agents, enabling you to program agents more abstractly, in terms of goals and strategies. You will have to program the strategies yourself, but you do not have to worry about the underlying infrastructure such as strategy execution and support for inter-agent communication; these are provided by `aplib`. `Aplib` also tries to offer high level APIs, so that you can program
your agents as cleanly as possible, with least possible boilerplate code.

_Note:_ as it is now, `aplib` is still under development. As we go, we will add more supports to integrate AI into your agents.

There are indeed many dedicated agent programming languages, but most of them do not have enough features, support, and tooling to facilitate integration with real life applications. We therefore intentionally develop `aplib` as a library in Java, so that by extension `aplib` programmers get first class access to Java's lrich library supports, software tools, and language features (e.g. rich data types and OO, whereas in contrast many dedicated agent programming languages only have a limited set of data types).

`Aplib` views an agent system as a system consisting of an '_environment_' where one or more agents operate to influence this environment towards certain goals. This _environment_ can be another program, or some hardware, or a human user interacting through some interface. While some environment may be passive, completely controlled by the agents, some others may be autonomous and non-deterministic, which makes the task of controlling it indeed more challenging for the agents.

`Aplib` allows **an agent to be programmed by specifying a goal that it has to solve, and a strategy to solve it**. There are 'combinators' (constructors) available to compose a complex goal from subgoals (or in other words, to break a complex goal into subgoals; providing a subgoal is
comparable to providing a hint for the agent). A strategy can be composed declaratively, by specifying when different actions that make up the strategy can be executed, without having to specify the exact order in which these actions are to be executed. There are also combinators available to compose a complex strategy from simpler ones.

`Aplib` is inspired by the popular [Belief Desire Intention](https://en.wikipedia.org/wiki/Belief%E2%80%93desire%E2%80%93intention_software_model) (BDI) model of agent programming (e.g. `aplib` agents have 'goals' and run so-called 'deliberation' cycles). However, we decide not to implement a BDI logic to favor simplicity and programmability. You will not lose expressiveness (Java aside, `aplib` is Turing complete).


**Features:**

* **Fluent interface** style of APIs.
* Combinators for **high level goal and strategy programming**.
* **Subservient** agents (running on the same thread as `main`) as well as **autonomous** agents (running on their own threads).
* **Multi agent**: programming multiple autonomous agents controlling the a shared environment and communicating through channels.
* **Prolog binding**: allowing agents to do prolog-based reasoning.
* **Bounded LTL** for automated property based testing.


Planned features:

* Bounded model checking and runtime verification.
* Reinforcement learning
* Search algorithms for solving goals
* Environment for controlling 3D games


#### Some code snippets:

* Specifying a goal:

```java
goal("Guess a the magic number (10)").toSolve((Integer x) -> x == 10)
```

* Specifying a strategy:

```java
FIRSTof(guessLowerbound.on_((Belief belief) -> ! belief.feelingVeryLucky() ,
        ANYof(speculate1,
              speculate2,
              ...)
```        

* Creating and configuring an agent in the _fluent interface_ style:

```java
new AutonomousBasicAgent()
    . attachState(new StateWithMessanger() .setEnvironment(new ConsoleEnvironment()))
    . setGoal(topgoal)
    . setSamplingInterval(1000)
```

* Launching an autonomous agent on a new thread:

```java
new Thread(() -> agent.loop()) . start()
```

#### Building with Maven

You need Java-11 or higher.

You can run `mvn` (Maven) at the project root to do the things listed below. Maven will put artifacts it produces under the directory `./target` in the project root.

* To compile the project: `mvn compile`
* To run the project's unit tests: `mvn test`
* To produce a jar of the project: `mvn package`. This will invoke `compile` and `test`, and then produce a jar containing the whole project. This is the jar you want to use if you want to include in your own project if you want to use `aplib`.
* To generate the javadoc APIs documentation: `mvn javadoc:javadoc`. The resulting documentations can be found in `./target/site/apicdocs`.
* To clean `./target`: `mvn clean`

#### Projects dir. structure

* `./src/main/java` : the root of `aplib` Java source code.
* `./src/test/java` : the root of Java source code of `aplib` unit tests.
* `./docs/manual` : contain some tutorials and documentations.
* `./libs` : external jars provided for convenience. You should not need these jars if you build using Maven. They are needed if you want to work on aplib itself and want to just link the jars immediately.

#### License

Copyright (c) 2019, Utrecht University.

`Aplib` is an open source software. It can be used and distributed under the
[LGPL version 3 license](./lgpl-3.0.md).

#### Credits

Contributors:
* Wishnu Prasetya
