# Replication Package I

**Paper:**  _Specification-based Automated Testing in Computer Games_

The project contains the MiniDungeon experiments (Section IV.A). SpaceEngineers experiments (IV.B) are packaged in a different project.

This research is based on a fork of the Aplib project. The research project is referred to as **onlineTestCaseGenerator**.
You can find the algorithm presented in the paper and various combinations of this algorithm with the _Random_ algorithm in the package `onlineTestCaseGenerator` in `./src/main/java/onlineTestCaseGenerator`  directory.

## Replication Instructions

The project is a Maven project. The programming language is Java. You need Java at least version 11, and you need Maven to build and re-run the experiments discussed in the paper.

To build the project just do `mvn compile` from the project root.

The experiments in IV.A are organized as JUnit test-methods, located in `./src/test/java`, in particular in the class named `Test_Experiement`. This setup enables us to conveniently run the experiments from the console through maven.  There are comments in the test-methods that briefly specifies what they do.

All test-methods (except the last one) demonstrate how our algorithm (let's call it **Online**, as in the paper) generates a test case from a given scenario specification, and under various setup, e.g. single maze or multi maze, with or without combination with _Random_. You can also re-run them on different instances of the same level. To produce distinct game instances for the tests, we can change the seed number in the experiments. This seed influences where monsters and potions are seeded, but not the structure of a game level (e.g. if the level has N monsters, this number stays the same).
In every test method, the game's instance is altered by changing the variable `seed`.
At the beginning of the test class (`Test_Experiement`), a comment is made on the list of seeds.

Some of the scenarios specify a specific item id or item type that the scenario has to visit. You can change them to try out different targets. These ids and types follow some convention. For instance, the item id for testing a healing potion is H_X. The healing potion's id, X, ranges from 0 to 3 in every maze.
The names of rage potions, scrolls, and shrines begin with R_X, S_X, and Sh_X, respectively.

To run a disjunctive or conjunctive scenario, the _type_ of the item needs to be given to the test agent.
The types of healing potion, rage potion, scroll are represented by `EntityType.HAELPOT`, `EntityType.RAGEPOT`, and `EntityType.SCROLL` respectively.

As examples, we describe some of the experiments as well as how to run them below.
A convenient way to re-run an experiment is by running the corresponding test method from maven. For example, you can use the console command below to run the experiment in `test2`:

```java
mvn test -Dtest="onlineTestCaseGenerator.Test_Experiement#test2"
```

Above, the test method that we want to run is called `test2`. This method runs the **Online** algorithm, which in turns  generates a 'test case'. As mentioned in the paper, generating a test case here means generating an execution that would fulfill a scenario. For `test2` the scenario is a _basic scenario_  targeting an item with a particular id (see Table II in the paper).

#### Getting code coverage

Invoking maven as in the above example simply runs the test method. It does not give you for example information on code-coverage. To get code coverage measurement, you can run the test/s from Eclipse with code coverage plugin installed. We used EclEmma. You can import the project as a Maven project into eclipse, then run e.g. the example `test2` mentioned above, with coverage enabled. The main logic of the game MiniDungeon (the SUT) is in the class `nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon`. After running the test in Eclipse, you can check Eclipse's output to see the code coverage on that class.

#### Other experiments

To run a basic scenario on a multi maze setup (L2,L4,L8 in Table III), you can run the experiment in `test7`:   

```java
mvn test -Dtest="onlineTestCaseGenerator.Test_Experiement#test7
```

`test14` below runs a basic scenario, but using the combination of our **Online** algorithm with _Random_:

```java
mvn test -Dtest="onlineTestCaseGenerator.Test_Experiement#test14
```

`test21` below runs _Random_ algorithm:

```java
mvn test -Dtest="onlineTestCaseGenerator.Test_Experiement#test21
```


Below is an overview of varios test methods in the class `Test_Experiement`, and which experiment/scenario it does.

| LTL pattern   | single Maze setup (L1)   | Multi-Maze  | Online+Random|
| ------------- |:-------------:| -----:| ----------:|
| Basic scenario | test2 | test7 | test11 - test14|
| Disjunctive scenario |   test3    |   test8 |  test12 - test24 |
| Conjunctive scenario | test4      |    test9 | test13 |
| Scenario chain | test5  | test5 |  test10 | test10|

Note: To activate online+Random in test10, variable random must change to true.


#### Getting Physical Coverage:

After running a test method, a CSV file containing the physical coverage is generated and placed in the project root.
The name of the generated CSV file is "visits.csv"

To generate the graphical heat map, the command below can be used.

```
python threeDheatmap.py MD visits.csv
```

The generated graphical heat map is named heatMap.
