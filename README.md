# iv4XR Core and Aplib (and iv4XR Framework)

**iv4XR  Core** This project contains _iv4XR Core_, which is the core of the agent-based automated testing 'Framework' for testing highly interactive systems such as computer games or computer simulators. Our own use case is to use the framework for automated testing of Extended Reality (XR) systems. Within this use case, the Framework has been piloted for testing 3D games; interfacing to other types of XR systems is work in progress. The difference between the Core and the Framework is shown in the picture below:

<img src="./docs/iv4xr-architecture.png" width="60%">

The Core provides the agent-based testing infrastructure. The pure agent-part is provided by a package (in the Core) called `aplib` (stands for Agent Programming Library). The Core adds testing-related functionalities and few other extra functionalities like world representation on top of `aplib`. In the picture above, 'setup-2' is a lean setup where we only use the Core to target a System Under Test (SUT), shown as 'setup-2' in the picture above. In contrast, 'setup-1' uses the entire Framework. The 'Framework' adds other testing-related tools, such as a model-based testing library, explorative testing using Testar, etc, which are then at your disposal.

iv4XR is intended to be generic: it can be used to target _any system_ as long as the agents can interface with the system (in a specific structure defined by iv4XR, this is explained later).

**Video** [here is a 10m video showing a demo of iv4XR](https://youtu.be/Hc8NP4NuHAk).

**Aplib** is the underlying agent programming library used by the iv4XR Framework. This library is general purpose, and can be used to program agents for purposes other than testing. Using iv4XR can be seen as a special case of using `aplib`, where you get some testing-specific extra capabilities, such as expressing test oracles.

**Using `aplib` and iv4XR Core.** `Aplib` is a Java library. The simplest setup to use it would be:

  1. create a Java method where you create one agent
  1. formulate a goal for the agent, and program a tactic to solve it.
  1. run the agent.

Using iv4XR is just the same, with an additional step that the interface to let agents to control the System under Test must also be setup.  

**Papers**

  * Extended abstract: [_Aplib: An Agent Programming Library for Testing Games_](http://ifaamas.org/Proceedings/aamas2020/pdfs/p1972.pdf), I. S. W. B. Prasetya,  Mehdi Dastani, in the International Conference on Autonomous Agents and Multiagent Systems (AAMAS), 2020.

  * Concepts behind agent-based automated testing:
  [_Tactical Agents for Testing Computer Games_](https://emas2020.in.tu-clausthal.de/files/emas/papers-h/EMAS2020_paper_6.pdf)
I. S. W. B. Prasetya, Mehdi Dastani, Rui Prada, Tanja E. J. Vos, Frank Dignum, Fitsum Kifetew,
in Engineering Multi-Agent Systems workshop (EMAS), 2020.

  * The agents' execution loop is explained  in this draft: [I.S.W.B. Prasetya, _Aplib: Tactical Programming of Intelligent Agents_, draft. 2019.](https://arxiv.org/pdf/1911.04710)



## Aplib: an Agent Programming Library


* [APIs Javadoc documentation](http://www.staff.science.uu.nl/~prase101/research/projects/iv4xr/aplib/apidocs/)
* [Tutorials and additional documentations](./docs/manual)
   * [Concepts](./docs/manual/aplibConcepts.md)
   * [Tutorial 1](./docs/manual/tutorial_1.md)
    | [Tutorial 2](./docs/manual/tutorial_2.md)
    | [Tutorial 3](./docs/manual/tutorial_3.md)
   * [Prolog binding](./docs/manual/prolog.md)
* The agents' execution loop is explained  in this draft: [I.S.W.B. Prasetya, _Aplib: Tactical Programming of Intelligent Agents_, draft. 2019.](https://arxiv.org/pdf/1911.04710)

`Aplib` is a Java library to program multi agent programs.
`Aplib` is inspired by the popular [Belief Desire Intention](https://en.wikipedia.org/wiki/Belief%E2%80%93desire%E2%80%93intention_software_model) (BDI) model of agent programming (e.g. `aplib` agents have 'goals' and run so-called 'deliberation' cycles).
`Aplib` provides an architecture and a design pattern for programming agents, enabling you to program agents more abstractly, in terms of goals and tactics. You will have to program the tactics yourself, but you do not have to worry about the underlying infrastructure such as tactic execution and support for inter-agent communication; these are provided by `aplib`. `Aplib` also tries to offer high level APIs, so that you can program
your agents as cleanly as possible, with least possible boilerplate code.



_Note:_ as it is now, `aplib` is still under development. As we go, we will add more supports to integrate AI into your agents.

There are indeed many dedicated agent programming languages, but most of them do not have enough features, support, and tooling to facilitate integration with real life applications. We therefore intentionally develop `aplib` as a library in Java, so that by extension `aplib` programmers get first class access to Java's lrich library supports, software tools, and language features (e.g. rich data types and OO, whereas in contrast many dedicated agent programming languages only have a limited set of data types).

`Aplib` views an agent system as a system consisting of an '_environment_' where one or more agents operate to influence this environment towards certain goals. This _environment_ can be another program, or some hardware, or a human user interacting through some interface. While some environment may be passive, completely controlled by the agents, some others may be autonomous and non-deterministic, which makes the task of controlling it indeed more challenging for the agents.

`Aplib` allows **an agent to be programmed by specifying a goal that it has to solve, and a tactic to solve it**. There are 'combinators' (constructors) available to compose a complex goal from subgoals (or in other words, to break a complex goal into subgoals; providing a subgoal is
comparable to providing a hint for the agent). A tactic can be composed declaratively, by specifying when different actions that make up the strategy can be executed, without having to specify the exact order in which these actions are to be executed. There are also combinators available to compose a complex tactic from simpler ones.



**Features:**

* **Fluent interface** style of APIs.
* Combinators for **high level goal and tactical programming**.
* **Subservient** agents (running on the same thread as `main`) as well as **autonomous** agents (running on their own threads).
* **Multi agent**: programming multiple autonomous agents controlling the a shared environment and communicating through channels.
* **Prolog binding**: allowing agents to do prolog-based reasoning.
* **Bounded LTL** for automated property based testing.


Planned features:

* Bounded model checking and runtime verification.
* Reinforcement learning
* Search algorithms for solving goals


#### Some code snippets:

* Specifying a goal:

```java
goal("Guess a the magic number (10)").toSolve((Integer x) -> x == 10)
```

* Specifying a tactic:

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

## iv4XR Core

The iv4XR Core is a library to do automated testing by using agents.
It can be used to test any target system **as long as there is an interface between it and the agents**. Since this interface depends on the technology used by the System under Test (SUT), the iv4XR does not offer pre-made interface; so, the SUT developers need to construct one first. Technically, this interface needs to implement a `aplib` Java Interface named `Environment`.

[Tutorials and examples](./docs/iv4xr)
   * [Testing a Java class](./docs/iv4xr/testagent_tutorial_1.md)
   * [Testing a (simulated) external program](./docs/iv4xr/testagent_tutorial_2.md)

## Building with Maven

You need Java-11 or higher.

You can run `mvn` (Maven) at the project root to do the things listed below. Maven will put artifacts it produces under the directory `./target` in the project root.

* To compile the project: `mvn compile`
* To run the project unit tests: `mvn test`
* To produce a jar of the project: `mvn package`. This will invoke `compile` and `test`, and then produce a jar containing the whole project. This is the jar you want to use if you want to include in your own project if you want to use `aplib`.
* To generate the javadoc APIs documentation: `mvn javadoc:javadoc`. The resulting documentations can be found in `./target/site/apicdocs`.
* To clean `./target`: `mvn clean`

#### Projects dir. structure

* `./src/main/java` : the root of `aplib` Java source code.
* `./src/test/java` : the root of Java source code of `aplib` unit tests.
* `./docs/manual` : contain some tutorials and documentations.
* `./libs` : external jars provided for convenience. You should not need these jars if you build using Maven. They are needed if you want to work on aplib itself and want to just link the jars immediately.

## License

Copyright (c) 2019, Utrecht University.

`Aplib` is an open source software. It can be used and distributed under the
[LGPL version 3 license](./lgpl-3.0.md).

#### Credits

Contributors:
* Wishnu Prasetya, Naraenda Prasetya, Saba Ansari, Samira Shirzadeh, Fitsum Kifetew.
