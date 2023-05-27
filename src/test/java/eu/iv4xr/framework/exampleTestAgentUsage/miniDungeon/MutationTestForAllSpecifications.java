package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.goalsAndTactics.Sa3Solver3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.utils.Pair;

public class MutationTestForAllSpecifications {

	
	//=================== type 1 ====================
	
	/**
	 * Test LTL pattern type1: testing a heal pot 
	 * multi-seeds
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test1() throws Exception {
		var testId = new Test_SA3();
		Pair targetItemOrShrine = new Pair("id", "H0_0");
		Pair additionalFeature = null;//new Pair("maze", 0);
		int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int successed = 0 ;
		List<Long> totalTime = new ArrayList<Long>();
	//	var result  = testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature,179371);
		for(int seed: seeds) {
			System.out.println("seed number: " + seed);
			var result  = testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature,seed);
			if((boolean) result.fst) successed ++;
			totalTime.add((Long) result.snd);
		}
		System.out.println("success number: " + successed);
		totalTime.forEach(e -> System.out.println("time" + e));
	}
	
	/**
	 * Test LTL pattern Type1: testing a healing pot, with specific seed
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test2() throws Exception {
		var testId = new Test_SA3();
		Pair targetItemOrShrine = new Pair("id", "H0_0");
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature, seed);			
	}
	
	/**
	 * MultiMaze: 2 mazes
	 * Test LTL pattern type1: testing a heal pot 
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test7() throws Exception {
		var testId = new Test_MultiMaze();
		Pair targetItemOrShrine = new Pair("id", "H1_0");		
		Pair additionalFeature = new Pair("maze", 1);
		//int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int seed  = 98765  ;
		int maze = 2;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);
		
	}
	
    
	/**
	 * With Random
	 * Test LTL pattern Type1: testing a healing pot
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
		Pair targetItemOrShrine = new Pair("id", "R1_0");
		int maze  = 2;
		int seed  = 79371;
		Pair additionalFeature = new Pair("maze", 0);
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature, seed,maze);	
	}
	
	/**
	 * MultiMaze: 2 mazes
	 * Test LTL pattern type1: testing a heal pot 
	 * MultiSeeds
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test18() throws Exception {
		
		Pair targetItemOrShrine = new Pair("id", "H1_0");		
		Pair additionalFeature = new Pair("maze", 1);
		int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int successed = 0 ;
		int maze = 2;
		List<Long> totalTime = new ArrayList<Long>();
		
		for(int seed: seeds) {
			var testId = new Test_MultiMaze();
			System.out.println("seed number: " + seed);
			var result  = testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);
			if((boolean) result.fst) successed ++;
			totalTime.add((Long) result.snd);
		}
	 		
		System.out.println("success number: " + successed);
		totalTime.forEach(e -> System.out.println("time" + e));
		
	}
	
	
	//=================== type 2 ====================
	
	/**
	 * Test LTL pattern Type2: testing type scroll, with specific seed
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test3() throws Exception {
		var testId = new Test_SA3();
		Pair targetItemOrShrine = new Pair("type", EntityType.SCROLL);
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 24681;
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature,seed);			
	}
	
	
	/**
	 * MultiMaze: 2 mazes
	 * Test LTL pattern Type2: testing type scroll
	 * because in the first maze it can find the item and never goes to the next one
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test8() throws Exception {
		var testId = new Test_MultiMaze();
		Pair targetItemOrShrine = new Pair("type", EntityType.SCROLL);	
		// {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int seed  = 179371  ;
		int maze = 8 ; 
		Pair additionalFeature = new Pair("maze", 7);
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);		
	}
	
	
	/**
	 * With Random
	 * Test LTL pattern Type2: testing type scroll
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test12() throws Exception {
		var testId = new Test_IdOrTypeWithRandom();
		Pair targetItemOrShrine = new Pair("type", EntityType.SCROLL);
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 79371;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed);			
	}
	
	/**
	 * Test LTL pattern Type2: testing type scroll, with multi-seeds
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test15() throws Exception {
		var testId = new Test_SA3();
		Pair targetItemOrShrine = new Pair("type", EntityType.SCROLL);
		Pair additionalFeature = null;//new Pair("maze", 0);
		int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int successed = 0 ;
		List<Long> totalTime = new ArrayList<Long>();
		for(int seed: seeds) {
			System.out.println("seed number: " + seed);
			var result  = testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature,seed);
			if((boolean) result.fst) successed ++;
			totalTime.add((Long) result.snd);
		}
		System.out.println("success number: " + successed);
		totalTime.forEach(e -> System.out.println("time" + e));
	}
    
    /**
	 * MultiMaze: 2 mazes
	 * Multi-seeds
	 * Test LTL pattern Type2: testing type scroll
	 * because in the first maze it can find the item and never goes to the next one
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test19() throws Exception {
		Pair targetItemOrShrine = new Pair("type", EntityType.SCROLL);
		Pair additionalFeature = new Pair("maze", 1);
		int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int successed = 0 ;
		int maze = 2;
		List<Long> totalTime = new ArrayList<Long>();
		
		for(int seed: seeds) {
			var testId = new Test_MultiMaze();
			System.out.println("seed number: " + seed);
			var result  = testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);
			if((boolean) result.fst) successed ++;
			totalTime.add((Long) result.snd);
		}
	 		
		System.out.println("success number: " + successed);
		totalTime.forEach(e -> System.out.println("time" + e));		
	}
	
	//=================== type 3 ====================
	
	/**
	 * Test LTL pattern Type3: testing type scroll, with specific seed
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
	 * Test LTL pattern Type3: testing type scroll
	 * @throws Exception
	 */

	@Test
	public void test9() throws Exception {
		var testId = new Test_ForAllMultiMaze();
		Pair targetItemOrShrine = new Pair("type", EntityType.HEALPOT);
		Pair additionalFeature = null;
		//int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int maze  = 8;		
		int seed  = 13579;
		testId.testFullPlay("Frodo",targetItemOrShrine,additionalFeature, maze, seed);			
	}
	
	/**
	 * With Random
	 * Test LTL pattern Type3: testing type scroll
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test13() throws Exception {
		var testId = new Test_ForAllWithRandom();
		Pair targetItemOrShrine = new Pair("type", EntityType.SCROLL);
		Pair additionalFeature = null;
		testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature);			
	}
    
    /**
     * multi-seeds
	 * Test LTL pattern Type3: testing type scroll, with 
	 * @throws Exception
	 */
    @Disabled
	@Test
	public void test16() throws Exception {
    	var testId = new Test_ForAll();
		Pair targetItemOrShrine = new Pair("type", EntityType.SCROLL);
		Pair additionalFeature = null;//new Pair("maze", 0);
		int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int successed = 0 ;
		List<Long> totalTime = new ArrayList<Long>();
		for(int seed: seeds) {
			System.out.println("seed number: " + seed);
			var result  = testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature,seed);
			if((boolean) result.fst) successed ++;
			totalTime.add((Long) result.snd);
		}
		System.out.println("success number: " + successed);
		totalTime.forEach(e -> System.out.println("time" + e));
	}
    
    /**
	 * MultiMaze: 2 mazes
	 * Multi-seeds
	 * Test LTL pattern Type3: testing type scroll
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void test20() throws Exception {		
		Pair targetItemOrShrine = new Pair("type", EntityType.SCROLL);
		Pair additionalFeature = new Pair("maze", 0);
		int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int successed = 0 ;
		int maze = 2;
		List<Long> totalTime = new ArrayList<Long>();
		
		for(int seed: seeds) {
			var testId = new Test_ForAllMultiMaze();
			System.out.println("seed number: " + seed);
			testId.testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);
		}
	 						
	}
	//=================== type 4 ====================
	
	/**
	 * Test LTL pattern type4: testing a sequence where a rage pot and a
	 *  scroll, and a heal pot is used and the shrine is checked
	 */
    @Disabled
	@Test
	public void test5() throws Exception {
		var testId = new RunAllSpecifications();
		boolean random = false;
		//int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int seed =  24681;
		int maze = 3 ;
		Pair additionalFeature = new Pair("maze", 2);
		testId.testAll("Frodo",random,seed, maze, additionalFeature);			
	}
	
	/**
	 * NOTE: This has overlap with the previous test, it must be disabled
	 * Test LTL pattern type4: testing a sequence where a rage pot and a
	 *  scroll is used and the shrine is checked
	 */
	@Disabled
	@Test
	public void test6() throws Exception {
		var testId = new RunAllSpecifications();
		boolean random = false;
		int seed = 79371;
		int maze = 1 ;
		Pair additionalFeature = null;
		testId.testAll("Frodo",random, seed, maze, additionalFeature);			
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
		boolean random = true;
		int seed = 79371;
		int maze = 1 ;
		Pair additionalFeature = null;
		testId.testAll("Frodo", random, seed, maze, additionalFeature);			
	}
	
    
    /**
     * Multi-seeds
     * Multi-maze
	 * Test LTL pattern type4: testing a sequence where a rage pot and a
	 *  scroll, and a heal pot is used and the shrine is checked
	 */
	@Disabled
	@Test
	public void test17() throws Exception {
		var testId = new RunAllSpecifications();
		boolean random = false;
		int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int successed = 0 ;
		int maze = 1 ;
		Pair additionalFeature = null;
		List<Long> totalTime = new ArrayList<Long>();
		for(int seed: seeds) {
			System.out.println("seed number: " + seed);
			var result  = testId.testAll("Frodo", random, seed, maze, additionalFeature);
			if((boolean) result.fst) successed ++;
			totalTime.add((Long) result.snd);
		}
		System.out.println("success number: " + successed);
		totalTime.forEach(e -> System.out.println("time" + e));
	}

	
	
}
