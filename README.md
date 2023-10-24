

This research is based on a clone of the Aplib project. The research project is referred to as **onlineTestCaseGenerator**.

You may find the algorithm and various combinations of the algorithm with the _Random_ algorithm in the package `onlineTestCaseGenerator` in `./src/main/java/onlineTestCaseGenerator`  directory.

## Replication Instructions

All of the experiments' are organized as tests, located in `./src/test/java`, in particular in the class named `MutationForAll`. 

To produce distinct game layouts for the tests, we changed the seed number in the experiments. 
In every test method, the game's layout is altered by changing the variable seed.
At the beginning of the test class (`MutationForAll`), a comment is made on the list of seeds. 
Every test function has the ability to modify the item id/type. For instance, the item id for testing a healing pot is H_X. The healing potion's id, X, rages from 0 to 3 in every maze. 
The names of rage potions, scrolls, and shrines begin with R_X, S_X, and Sh_X, respectively.

To test a disjunctive or conjunctive scenario, the _type_ of the item needs to be given to the test agent.
The parameters for determining healing potion, rage potion, scroll is represented by EntityType.HAELPOT, EntityType.RAGEPOT, EntityType.SCROLL respectively. 

We describe some of the test methods in this class as well as how to perform a test below.

A convenient way to re-run an experiment is by running the corresponding test function from maven. For example, you can use the below command to run the experiment in `test2`:

```java
mvn test -Dtest="onlineTestCaseGenerator.MutationForAllSpecificationsTest#test2" 
```

Above, the test function that we want to run is called `test2`. This function generates a test case from a _basic scenario_ (see Table 3 in the paper), targeting an item with a particular id. 


Test the fundamental situation using a multi-maze arrangement in function test7.    
mvn test -Dtest="onlineTestCaseGenerator.MutationForAllSpecificationsTest#test7

To test Random + our Online algorithm run the command below:
mvn test -Dtest="onlineTestCaseGenerator.MutationForAllSpecificationsTest#test21


physical coverage:
After running a test method, a csv file containing the physical coverage is generated in the main directory.
The name of the CSV file is "visits.csv"

To generate the graphical heat map, the below command line can be used.

python threeDheatmap.py MD visits.csv

The generated graphical heat map is named heatMap.


(make a table which what different tests do)
 
* Disjunctive scenario: which test?
* Disjunctive scenario: which test?



We cannot unfortunately  provide a replication package for the Space Engineers experiments, as the game cannot be packaged along (the user has to buy it via Steam, and subsuquently a number of dlls have to be put in their  `AppData` directory, which requires permission).




