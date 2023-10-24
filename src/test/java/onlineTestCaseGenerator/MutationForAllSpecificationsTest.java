package onlineTestCaseGenerator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.utils.Pair;
import onlineTestCaseGenerator.Sa3Solver3;

public class MutationForAllSpecificationsTest {

	
	//=================== basic scenario ====================

	//different seeds = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
	/**
	 * Test LTL pattern basic scenario: testing a healing pot, with specific seed
	 * @throws Exception
	 */
   
	@Test
	public void test2() throws Exception {
		var testId = new Test_IdOrType();
		Pair targetItemOrShrine = new Pair("id", "H0_0");
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature, seed);			
	}
	
	/**
	 * MultiMaze
	 * Test LTL pattern basic scenario: testing a heal pot 
	 * @throws Exception
	 */
 
    @Disabled
	@Test
	public void test7() throws Exception {
		var testId = new Test_MultiMaze();
		Pair targetItemOrShrine = new Pair("id", "H1_0");		
		Pair additionalFeature = new Pair("maze", 1);		
		int seed  = 79371  ;
		int maze = 2;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);
	}
	
    
	/**
	 * With Random
	 * Test LTL pattern basic scenario: testing a healing pot
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test11() throws Exception {
		var testId = new Test_IdOrTypeWithRandom();
		Pair targetItemOrShrine = new Pair("id", "H0_0");
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed);			
	}
	
	/**
	 * MultiMaze: 2 mazes with Random
	 * Test LTL pattern type1: testing a rage pot 
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test14() throws Exception {
		var testId = new Test_MultiMazeWithRandom();
		Pair targetItemOrShrine = new Pair("id", "H1_0");
		int maze  = 2;
		int seed  = 79371;
		Pair additionalFeature = new Pair("maze", 1);
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature, seed,maze);	
	}
	
	
	
	//=================== disjunctive scenario  ====================
	
	/**
	 * Test LTL pattern disjunctive scenario
	 * @throws Exception
	 */
   @Disabled
	@Test
	public void test3() throws Exception {
		var testId = new Test_IdOrType();
		Pair targetItemOrShrine = new Pair("type", EntityType.RAGEPOT);
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature,seed);			
	}
	
	
	/**
	 * MultiMaze: 2 mazes
	 * Test LTL pattern disjunctive scenario
	 * @throws Exception
	 */
  
   	@Disabled
	@Test
	public void test8() throws Exception {
		var testId = new Test_MultiMaze();
		Pair targetItemOrShrine = new Pair("type", EntityType.RAGEPOT);			
		int seed  = 79371  ;
		int maze = 2 ; 
		Pair additionalFeature = new Pair("maze", 1);
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);		
	}	 
	
	/**
	 * With Random
	 * Test LTL pattern disjunctive scenario
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test12() throws Exception {
		var testId = new Test_IdOrTypeWithRandom();
		Pair targetItemOrShrine = new Pair("type", EntityType.RAGEPOT);
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed);			
	}
	
	/**
	 * MultiMaze: 2 mazes with Random
	 * Test LTL pattern disjunctive scenario: testing a rage pot 
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test24() throws Exception {
		var testId = new Test_MultiMazeWithRandom();
		Pair targetItemOrShrine = new Pair("type", EntityType.RAGEPOT);
		int maze  = 2;
		int seed  = 79371;
		Pair additionalFeature = new Pair("maze", 1);
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature, seed,maze);	
	}
	
	   
	
	//=================== conjunctive scenario ====================
	
	/**
	 * Test LTL pattern conjunctive scenario
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test4() throws Exception {
		var testId = new Test_ForAll();
		Pair targetItemOrShrine = new Pair("type", EntityType.HEALPOT);
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed);					
	}
	
	/**
	 * MultiMaze: 2 mazes 
	 * Test LTL pattern conjunctive scenario
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test9() throws Exception {
		var testId = new Test_ForAllMultiMaze();
		Pair targetItemOrShrine = new Pair("type", EntityType.HEALPOT);
		Pair additionalFeature = new Pair("maze", 1);		
		int maze  = 2;		
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature, maze, seed);			
	}

	/** 
	 * With Random
	 * Test LTL pattern conjunctive scenario
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test13() throws Exception {
		var testId = new Test_ForAllWithRandom();
		Pair targetItemOrShrine = new Pair("type", EntityType.HEALPOT);
		Pair additionalFeature = null;
		int maze  = 2;
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);			
	}
    
    
   
	//=================== scenario chain ====================
	
	/**
	 * Test LTL pattern scenario chain: testing a sequence where a rage pot and a
	 *  scroll, and a heal pot is used and the shrine is checked
	 */
	@Disabled
	@Test
	public void test5() throws Exception {
		var testId = new RunAllSpecifications();
		boolean random = false;		
		int seed =  179371  ;
		int maze = 2 ;
		Pair additionalFeature = new Pair("maze", 1);
		testId.testAll("Frodo",random,seed, maze, additionalFeature);			
	}
	
	
	
	/**
	 * Whit Random
	 * Test LTL pattern type4: testing a sequence where a rage pot and a
	 *  scroll, and a heal pot is used and the shrine is checked
	 */
	@Disabled
	@Test
	public void test10() throws Exception {
		var testId = new RunAllSpecifications();
		boolean random = false;
		int seed = 179371;
		int maze = 2 ;
		Pair additionalFeature = new Pair("maze", 1);
		testId.testAll("Frodo", random, seed, maze, additionalFeature);			
	}
	
    
	//==================Random==========================
	
	/**
     * Multi-maze
	 * Random algorithm
	 */
	@Disabled
	@Test
	public void test21() throws Exception {
		var testId = new Test_Random();
		Pair targetItemOrShrine = new Pair("id", "H0_0");
		Pair additionalFeature = new Pair("maze", 0);		
		int seed = 79371 ;
		int successed = 0 ;
		int maze = 1;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);
	}
	

}
