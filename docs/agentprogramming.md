# Agent Programming

This document explains the pure agent-programming part of iv4xr-core. For [the agent-based testing part, see here](./agentbasedtesting.md). In itself, this agent-programming part is generic: it can be used to build any agent-based program, not limited for testing. 

* [Main concepts](./manual/aplibConcepts.md)
* Tutorials: 
   * [tutorial 1: a minimalistic agent](./manual/tutorial_1.md)
   * [tutorial 2: state and tactic](./manual/tutorial_2.md)
   * [tutorial 3: subservient, autonomous, and multi agents](./manual/tutorial_3.md)
* The agents' execution loop is explained  in this draft: [I.S.W.B. Prasetya, _Aplib: Tactical Programming of Intelligent Agents_, draft. 2019.](https://arxiv.org/pdf/1911.04710)
* [Domain Specific Language (DSL) for formulating goals and tactics](./manual/DSL.md)
* [Prolog binding](./manual/prolog.md)
* [Few more examples](./Fewmore_simpleExamples.md)

Agent programming is mainly provided by the `aplib` part of iv4xr-core. Since the Core is a Java library, so is its aplib part.
`Aplib` (it stands for _Agent Programming Library_) is inspired by the popular [Belief Desire Intention](https://en.wikipedia.org/wiki/Belief%E2%80%93desire%E2%80%93intention_software_model) (BDI) model of agent programming (e.g. `aplib` agents have 'goals' and run so-called 'deliberation' cycles).
`Aplib` provides an architecture and a design pattern for programming agents, enabling you to program agents more abstractly, in terms of goals and tactics. You will have to program the tactics yourself, but you do not have to worry about the underlying infrastructure such as tactic execution and support for inter-agent communication; these are provided by `aplib`. `Aplib` also tries to offer high level APIs, so that you can program
your agents as cleanly as possible, with least possible boilerplate code.

There are indeed many dedicated agent programming languages, but most of them do not have enough features, support, and tooling to facilitate integration with real life applications. We therefore intentionally develop `aplib` as a library in Java, so that by extension `aplib` programmers get first class access to Java's lrich library supports, software tools, and language features (e.g. rich data types and OO, whereas in contrast many dedicated agent programming languages only have a limited set of data types).

`Aplib` views an agent system as a system consisting of an '_environment_' where one or more agents operate to influence this environment towards certain goals. This _environment_ can be another program, or some hardware, or a human user interacting through some interface. While some environment may be passive, completely controlled by the agents, some others may be autonomous and non-deterministic, which makes the task of controlling it indeed more challenging for the agents.

`Aplib` allows **an agent to be programmed by specifying a goal that it has to solve, and a tactic to solve it**. There are 'combinators' (constructors) available to compose a complex goal from subgoals (or in other words, to break a complex goal into subgoals; providing a subgoal is
comparable to providing a hint for the agent). A tactic can be composed declaratively, by specifying when different actions that make up the strategy can be executed, without having to specify the exact order in which these actions are to be executed. There are also combinators available to compose a complex tactic from simpler ones.