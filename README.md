This research is based on a clone of the Aplib project. The research project is referred to as onlineTestCaseGenerator.


You may find the algorithm and various combinations of the algorithm with the Random algorithm in the main directory.

All of the experiment's tests are included in a class named MutationForAll in the test directory. 

To produce distinct game layouts for the test, we changed the seed number in this experiment. 
In every test method, the game's layout is altered by changing the variable seed.
At the beginning of the test class, a comment is made on the list of seeds. 
Every test function has the ability to modify the item id/type. For instance, the item id for testing a healing pot is H_X. The healing potion's id, X, rages from 0 to 3 in every maze. 
The names of rage potions, scrolls, and shrines begin with R_X, S_X, and Sh_X, respectively.
To test a disjunctive/conjunctive scenario, the type of the item needs to be given to the test agnet.
Tha parameters for determining healing potion, rage potion, scroll is represented by EntityType.HAELPOT, EntityType.RAGEPOT, EntityType.SCROLL respectively. 


We describe some of the test methods in this class as well as how to perform a test below.

To run a test method in maven, we use the below command:
mvn test -Dtest="onlineTestCaseGenerator.MutationForAllSpecificationsTest#test2" 

The test function that we want to run is called test2. This technique uses a particular item id to test a basic scenario. 
 


Test the fundamental situation using a multi-maze arrangement in function test7.    
mvn test -Dtest="onlineTestCaseGenerator.MutationForAllSpecificationsTest#test7

To test Random+ algorithm run the command below:
mvn test -Dtest="onlineTestCaseGenerator.MutationForAllSpecificationsTest#test21


physical coverage:
After running a test method, a csv file containing the physical coverage is generated in the main directory.
The name of the CSV file is "visits.csv"

To generate the graphical heat map, the below command line can be used.

python threeDheatmap.py MD visits.csv

The generated graphical heat map is named heatMap.







