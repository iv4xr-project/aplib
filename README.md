

This research is based on a clone of the Aplib project. The research project is referred to as **onlineTestCaseGenerator**.

You may find the algorithm and various combinations of the algorithm with the _Random_ algorithm in the package `onlineTestCaseGenerator` in `./src/main/java/onlineTestCaseGenerator`  directory.

## Replication Instructions

All of the experiments' are organized as tests, located in `./src/test/java`, in particular in the class named `Test_Experiement`. 

To build the project just do `mvn compile` from the project root. You need Java at least version 11.

To produce distinct game layouts for the tests, we changed the seed number in the experiments. 
In every test method, the game's layout is altered by changing the variable seed.
At the beginning of the test class (`Test_Experiement`), a comment is made on the list of seeds. 
Every test function has the ability to modify the item id/type. For instance, the item id for testing a healing potion is H_X. The healing potion's id, X, rages from 0 to 3 in every maze. 
The names of rage potions, scrolls, and shrines begin with R_X, S_X, and Sh_X, respectively.

To run a disjunctive or conjunctive scenario, the _type_ of the item needs to be given to the test agent.
The parameters for determining healing potion, rage potion, scroll is represented by EntityType.HAELPOT, EntityType.RAGEPOT, EntityType.SCROLL respectively. 

We describe some of the test functions in this class as well as how to perform a test below. 

A convenient way to re-run an experiment is by running the corresponding test function from maven. For example, you can use the below command to run the experiment in `test2`:

```java
mvn test -Dtest="onlineTestCaseGenerator.Test_Experiement#test2" 
```

Above, the test function that we want to run is called `test2`. This function generates a test case from a _basic scenario_ (see Table 3 in the paper), targeting an item with a particular id. 


To run a multi maze setup (L2,L4,L8 in Table 3), the below command can be executed.    

```java
mvn test -Dtest="onlineTestCaseGenerator.Test_Experiement#test7

To run Random + our Online algorithm execute the command below:

```java
mvn test -Dtest="onlineTestCaseGenerator.Test_Experiement#test21


Physical Coverage:
After running a test function, a CSV file containing the physical coverage is generated in the root.
The name of the generated CSV file is "visits.csv"

To generate the graphical heat map, the below command can be used.

```java
python threeDheatmap.py MD visits.csv

The generated graphical heat map is named heatMap.


Below is the list of functions for each LTL pattern. 

| LTL pattern   | single Maze(L1)   | Multi-Maze  | Online+Random|
| ------------- |:-------------:| -----:| ----------:|
| Basic scenario | test2 | test7 | test11 - test14| 
| Disjunctive scenario |   test3    |   test8 |  test12 - test24 |
| Conjunctive scenario | test4      |    test9 | test13 |
| Scenario chain | test5  | test5 |  test10 | test10|


*To activate online+Random in test10, variable random must change to true. 



We cannot unfortunately  provide a replication package for the Space Engineers experiments, as the game cannot be packaged along (the user has to buy it via Steam, and subsuquently a number of dlls have to be put in their  `AppData` directory, which requires permission).




