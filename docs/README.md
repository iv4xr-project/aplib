# aplib and iv4xr-core Documentation

Aplib is the underlying agent-programming library of iv4xr-core. The core adds testing-related features on top of aplib. The entire core is provided a Java library. Using test-agent is explained in the core-part, but we recommend to first read the underlying concepts of aplib agent-programming and its tutorials (see below).

  * [Build instructions](../README.md)
  * [APIs Javadoc documentation](http://www.staff.science.uu.nl/~prase101/research/projects/iv4xr/aplib/apidocs/)
  * [Aplib manuals](README.md#aplib)
  * [iv4xr-core manuals](README.md#core)


### Agent programming with aplib <a name="aplib"></a>

* [Main concepts](./manual/aplibConcepts.md)
* [Tutorial 1: a minimalistic agent](./manual/tutorial_1.md)
 | [Tutorial 2: state and tactic](./manual/tutorial_2.md)
 | [Tutorial 3: subservient, autonomous, and multi agents](./manual/tutorial_3.md)
* The agents' execution loop is explained  in this draft: [I.S.W.B. Prasetya, _Aplib: Tactical Programming of Intelligent Agents_, draft. 2019.](https://arxiv.org/pdf/1911.04710)
* [Domain Specific Language (DSL) for formulating goals and tactics](./manual/DSL.md)
* [Prolog binding](./manual/prolog.md)
* [Few more examples](./Fewmore_simpleExamples.md)

`Aplib` is a Java library to program multi agent programs.
`Aplib` is inspired by the popular [Belief Desire Intention](https://en.wikipedia.org/wiki/Belief%E2%80%93desire%E2%80%93intention_software_model) (BDI) model of agent programming (e.g. `aplib` agents have 'goals' and run so-called 'deliberation' cycles).
`Aplib` provides an architecture and a design pattern for programming agents, enabling you to program agents more abstractly, in terms of goals and tactics. You will have to program the tactics yourself, but you do not have to worry about the underlying infrastructure such as tactic execution and support for inter-agent communication; these are provided by `aplib`. `Aplib` also tries to offer high level APIs, so that you can program
your agents as cleanly as possible, with least possible boilerplate code.

There are indeed many dedicated agent programming languages, but most of them do not have enough features, support, and tooling to facilitate integration with real life applications. We therefore intentionally develop `aplib` as a library in Java, so that by extension `aplib` programmers get first class access to Java's lrich library supports, software tools, and language features (e.g. rich data types and OO, whereas in contrast many dedicated agent programming languages only have a limited set of data types).

`Aplib` views an agent system as a system consisting of an '_environment_' where one or more agents operate to influence this environment towards certain goals. This _environment_ can be another program, or some hardware, or a human user interacting through some interface. While some environment may be passive, completely controlled by the agents, some others may be autonomous and non-deterministic, which makes the task of controlling it indeed more challenging for the agents.

`Aplib` allows **an agent to be programmed by specifying a goal that it has to solve, and a tactic to solve it**. There are 'combinators' (constructors) available to compose a complex goal from subgoals (or in other words, to break a complex goal into subgoals; providing a subgoal is
comparable to providing a hint for the agent). A tactic can be composed declaratively, by specifying when different actions that make up the strategy can be executed, without having to specify the exact order in which these actions are to be executed. There are also combinators available to compose a complex tactic from simpler ones.

### Agent-based testing with iv4xr-core <a name="core"></a>

The Core includes aplib. It adds testing related features on top of aplib. For example, with just aplib we can create agent. With the Core we can create a test-agent. A test-agent has some additional features, e.g. it can check oracles/invariants and collect their verdicts (pass/fail). It also can collect traces e.g. for data analyses and visualization,or to be checked against LTL properties.

Iv4xr test-agents can be used to test any target system **as long as there is an interface between it and the agents**. Since this interface depends on the technology used by the System under Test (SUT), the iv4XR does not offer pre-made interface; so, the SUT developers need to construct one first. Technically, this interface needs to implement an `aplib` Java Interface named `Environment`. The tutorials below should guide you through the steps of testing with iv4xr agents --it may be helpful to [first read the basics of aplib programming](README.md#aplib).

  * [Tutorial 1: testing a Java class with iv4xr](./iv4xr/testagent_tutorial_1.md)
  * [Tutorial 2: testing an external program with iv4xr](./iv4xr/testagent_tutorial_2.md)
  * Test Specification Language for specifying tests: [see aplib DSL](./manual/DSL.md)
