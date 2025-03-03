## Replication of the MBT Experiment

This document explains the steps to replicate the experiment published in the paper _Model-based Testing Computer Games: Does It
Work?_, I.S.W.B. Prasetya, in the
21th Workshop on Advances in Model Based Testing (A-MOST) 2025.

The above paper presents an online Model Based Testing test generation approach to test computer games.

Related information:

* Conceptual explanation of how our online MBT for computer games work: see the above paper.
* Documentation on creating and running the MBT generator: [see here](./src/main/java/eu/iv4xr/framework/extensions/mbt/README.md).
* Conceptual explanation on how the underlying agent-based framework `aplib` works: see the paper [_Aplib: Tactical agents for testing computer games_](https://link.springer.com/chapter/10.1007/978-3-030-66534-0_2), by Prasetya, I. S. W. B., Mehdi Dastani, Rui Prada, Tanja EJ Vos, Frank Dignum, and Fitsum Kifetew.
In International Workshop on Engineering Multi-Agent Systems. Cham: Springer International Publishing, 2020.
* The MBT approach is currently provided as a branch of `aplib` called `mbt`. This located at https://github.com/iv4xr-project/aplib/tree/mbt
* The main branch of `aplib`: https://github.com/iv4xr-project/aplib

### Re-running the experiment

We need:

* Eclipse IDE with Maven plugin.
* Java at least version 11.

This project is a Maven project. Import it this project root (not this current directory, but really aplib root) as a Maven project into the Eclipse IDE. This should also automatically download all dependencies of the project.

The experiment is implemented as Java JUnit tests. This allows you to run it as JUnit tests. The experiment is coded in the class [`Experiment`](./test/java/eu/iv4xr/framework/exampleTestAgentUsage/miniDungeon/MBT/Experiment.java). The following tests are provided:

* `runRandom()` will run the random-algorithm.
* `runQ()` will run the Q-algorithm.
* `runBaseMBT()` will run the base/vanilla MBT.
* `runSmartMBT()` will run the MBT+.

To run any of this methods, just uncomment the `@Test` attribute. Inside each method, you can choose the configuration you want to run (uncomment the one that you want).

Generated coverage can be checked by running the test/s with coverage (EclEmma) turned on.

### Available data

In the directory `data` under the project root, the file `mbt-md-results.xlsx` contains data measured from the original experiment for the above mentioned paper.
