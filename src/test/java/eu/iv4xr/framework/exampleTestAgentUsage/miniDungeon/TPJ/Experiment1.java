package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;

import org.junit.jupiter.api.Test;


/**
 * Running all play-tests on various configurations. This is meant to get their
 * coverage data. The data can be exported From Eclipse.
 */
class Experiment1 {

	MiniDungeonConfig[] randomTestConfigs = { TPJconfigs.MDconfig1(), TPJconfigs.MDconfig2() } ;
	MiniDungeonConfig[] shrineTestConfigs = { 
			TPJconfigs.M2Config() , TPJconfigs.M4Config() ,
			TPJconfigs.M6Config() , TPJconfigs.M8Config() 
			
		} ;
	MiniDungeonConfig[] pvpTestConfigs = { TPJconfigs.PVPConfig1(),  TPJconfigs.PVPConfig2() } ;
	MiniDungeonConfig[] walkTestConfigs = { TPJconfigs.MDconfig1() } ;
	MiniDungeonConfig[] monsterCombatTestConfigs = { TPJconfigs.MonstersCombatConfig1() } ;
	
	boolean saveRunData = false ;
	String dataFolder = "./tmp/TPJ/experiment1" ;

	boolean withGraphics = true ;
	boolean verbosePrint = false ;
	int sleepBetweenUpdates = 5 ;

	int randomTestBudget = 3000 ;
	int shrineTestBudget = 5000 ;
	int wallTestBudget   = 8000 ;
	int pvpTestBudget = 2000 ;
	
	public static TestAgent mkFrodo() {
		return new TestAgent("Frodo","Player") ;
	}
	
	public static TestAgent mkSmeagol() {
		return new TestAgent("Smeagol","Player") ;
	}
	
	void runTest(String runId, TestAgent agent1, TestAgent agent2, 
			MiniDungeonConfig config,
			GoalStructure G1, 
			GoalStructure G2,
			int budget,
			boolean assertG1IsFinished,
			boolean assertLTLs) throws Exception {
		
		var runner = new MDTestRunner() ;
		runner.runDataFolder = dataFolder ;
		runner.withGraphics = withGraphics ;
		runner.verbosePrint = verbosePrint ;
		runner.saveRunData = saveRunData ;
		
		runner.runAgent(runId, agent1, agent2, config, G1, G2, budget, sleepBetweenUpdates);
		
		// checking oracles:
		if (assertG1IsFinished) {
			assertTrue(G1.getStatus().success(),"G1 check, " + runId) ;	
		}
		boolean ltlsOk = agent1.evaluateLTLs() ;
		if (agent2 != null) {
			ltlsOk = ltlsOk && agent2.evaluateLTLs() ;
		}
		if (assertLTLs) {
			assertTrue(ltlsOk,"LTLs check, " + runId) ;		
		}
	}
	
	@Disabled
	@Test
	void testRandom() throws Exception {
		for(int k=0; k<randomTestConfigs.length; k++) {
			MiniDungeonConfig config = randomTestConfigs[k] ;
			var frodo = mkFrodo() ;
			runTest("shrineTest_" + config.configname + "_"  + frodo.getId(),
					frodo,null, 
					config,
					new RandomPlayTester().randomPlay(frodo), // using smart random by default
					null,randomTestBudget,false,true) ;
			var smeagol = mkSmeagol() ;
			runTest("shrineTest_" + config.configname + "_"  + smeagol.getId(),
					smeagol,null, 
					config,
					new RandomPlayTester().randomPlay(smeagol),
					null,randomTestBudget,false,true) ;		
		}
	}
	
	//@Disabled
	@Test
	void testShrineWithSinglePlayer() throws Exception {
		for(int k=0; k<shrineTestConfigs.length; k++) {
			MiniDungeonConfig config = shrineTestConfigs[k] ;
			config.enableSmeagol = false ;
			var frodo = mkFrodo() ;
			runTest("shrineTest_twoplayers" + config.configname + "_"  + frodo.getId(),
					frodo,null, 
					config,
					new ShrineCleanTester().cleanseAllShrines(frodo, config.numberOfMaze),
					null,
					shrineTestBudget,
					true,true) ;
			var smeagol = mkSmeagol() ;
			config.enableSmeagol = true ;
			runTest("randomTest_" + config.configname + "_"  + smeagol.getId(),
					smeagol,null, 
					config,
					new ShrineCleanTester().cleanseAllShrines(frodo, config.numberOfMaze),
					null,
					shrineTestBudget,
					true,true) ;		
		}
	}
	
	//@Disabled
	@Test
	void testShrineWithTwoPlayers() throws Exception {
		// only run it for the first dungeon:
		MiniDungeonConfig config = shrineTestConfigs[0] ;
		var frodo = mkFrodo() ;
		var smeagol = mkSmeagol() ;
		runTest("shrineTest_twoplayers" + config.configname,
					smeagol, frodo, 
					config,
					new ShrineCleanTester().cleanseAllShrines(smeagol, config.numberOfMaze),
					new RandomPlayTester(144).randomPlay(frodo),
					shrineTestBudget,
					true,true) ;		
	}
	
	//@Disabled
	@Test
	void testWalls() throws Exception {
		// run only one test for wall:
		MiniDungeonConfig config = walkTestConfigs[0] ;
		config.enableSmeagol = false ;
		var frodo = mkFrodo() ;
		runTest("wallTest_" + config.configname,
				frodo, null, 
				config,
				new WallsTester() . allWallsChecked(frodo),
				null,
				wallTestBudget,
				true,true) ;		
	}
	
	//@Disabled
	@Test
	void testCombatWithMonsters() throws Exception {
		// run only one test for wall:
		MiniDungeonConfig config = monsterCombatTestConfigs[0] ;
		var frodo = mkFrodo() ;
		runTest("monsterCombatTest_" + config.configname + "_"  + frodo.getId(),
					frodo, null, 
					config,
					new ShrineCleanTester().cleanseAllShrines(frodo, config.numberOfMaze),
					null,
					shrineTestBudget,
					false,true) ;	
		var smeagol = mkSmeagol() ;
		runTest("monsterCombatTest_" + config.configname + "_"  + smeagol.getId(),
				smeagol, null, 
					config,
					new ShrineCleanTester().cleanseAllShrines(smeagol, config.numberOfMaze),
					null,
					shrineTestBudget,
					false,true) ;	
	}
	
	//@Disabled
	@Test
	void testPVP() throws Exception {
		// run only one test for wall:
		MiniDungeonConfig config = pvpTestConfigs[0] ;
		var smeagol = mkSmeagol() ;
		var frodo = mkFrodo() ;
		runTest("pvpCombatTest_" + config.configname + "_"  + smeagol.getId(),
				smeagol, frodo, 
				config,
				new PvPPlayTester().searchAndKill(smeagol, frodo.getId()),
				new RandomPlayTester(144).randomPlay(frodo),
				pvpTestBudget,
				true,true) ;	
		config = pvpTestConfigs[1] ;
		smeagol = mkSmeagol() ;
		frodo = mkFrodo() ;
		runTest("pvpCombatTest_" + config.configname + "_"  + smeagol.getId(),
				frodo, smeagol,
				config,
				new PvPPlayTester().searchAndKill(frodo, smeagol.getId()),
				new RandomPlayTester(144).simpleRandomPlay(),
				pvpTestBudget,
				true,true) ;	
	}
	
	
}
