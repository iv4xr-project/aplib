# Aplib: an Agent Programming Library

`Aplib` is a Java library to program multi agent programs.

Note: as it is now, `aplib` is still under development.

`Aplib` is inspired by [2APL](http://apapl.sourceforge.net/) which is based on [Belief Desire Intention](https://en.wikipedia.org/wiki/Belief%E2%80%93desire%E2%80%93intention_software_model) (BDI) model of agent programming. This BDI model has its influence in `aplib`, though we do not try to be a BDI purist either. What is important in `aplib` design is its programmability. There are indeed many dedicated agent programming languages, but most of them do not have enough support to be integrated with real life applications. We therefore intentionally develop `aplib` as a library in Java, knowing that the latter already has large and rich supports in terms of library, software tools, and user base. Compared to e.g. [JADE](https://jade.tilab.com/) (Java Agent Development Framework) `aplib` is of much lighter weight, but on the other than `aplib` tries to exploit features from more recent Java, e.g. lambda expression, to make agent programming cleaner.

An _agent_ is essentially a program that is used to influence an environment towards a certain goal. This environment itself may be autonomous and non-deterministic, which makes the task of controlling it even more challenging for the agent.

`Aplib` allows an agent to be programmed by specifying a goal that it has to solve, and a strategy to solve it (these correspond to _desire_ and _intent_ in BDI terminology, whereas _belief_ corresponds to the agent's state ). There are combinators available to compose a complex goal from subgoals, comparable to providing hints for the agent. A strategy can be composed declaratively, by specifying when different actions that make up the strategy can be executed, without having to specify the exact order in which these actions are to be executed. There are also combinators available to compose a complex strategy from simpler ones.

List of features:

* **Fluent interface** style of APIs.
* **Subservient** agents (running on the same thread as `main`) as well as **autonomous** agents (running on their own threads).
* **Multi agent**: programming multiple autonomous agents controlling the a shared environment and communicating through channels.

Planned features:

* Reinforcement learning
* Environment for controlling 3D games

Some cone snippets:

* Specifying a goal:

```java
goal("Guess a the magic number (10)").toSolve((Integer x) -> x == 10)
```

* Creating and configuring an agent in the _fluent interface_ style:

```java
new AutonomousBasicAgent()
    . attachState(new SimpleState() .setEnvironment(new ConsoleEnvironment()))
    . setGoal(topgoal)
    . setSamplingInterval(1000)
```

* Launching an autonomous agent on a new thread:

```java
new Thread(() -> agent.loop()) . start()
```
