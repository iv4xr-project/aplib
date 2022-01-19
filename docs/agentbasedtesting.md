# Agent-based Testing with iv4xr-core 

**Prerequisite:** you have read the concepts and at least one tutorial on aplib agent programming:

* [Basic agent programming](./agentprogramming.md)

On top of its generic agent programming part (the aplib part), iv4xr-core adds testing related features. For example, with just aplib we can create agent. With the Core we can create a test-agent. A test-agent has some additional features, e.g. it can check oracles/invariants and collect their verdicts (pass/fail). It also can collect traces e.g. for data analyses and visualization,or to be checked against LTL properties.

Iv4xr test-agents can be used to test any target system **as long as there is an interface between it and the agents**. Since this interface depends on the technology used by the System under Test (SUT), the iv4XR does not offer pre-made interface; so, the SUT developers need to construct one first. Technically, this interface needs to implement an `aplib` Java Interface named `Environment`. 

The tutorials below should guide you through the steps of testing with iv4xr agents.

  * Tutorials:
     * [Tutorial 1: testing a Java class with iv4xr](./iv4xr/testagent_tutorial_1.md)
     * [Tutorial 2: testing an external program with iv4xr](./iv4xr/testagent_tutorial_2.md)
  * Reference for the Test Specification Language for specifying tests: [see aplib DSL](./manual/DSL.md)
  * [Collecting data and visualizing them](./iv4xr/datacollection.md)
  * [Using LTL properties](./iv4xr/testagent_tutorial_3.md)
  
