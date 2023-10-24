package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ.MD_invs;
import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ.RandomPlayTester;
import eu.iv4xr.framework.extensions.ltl.LTL;
import eu.iv4xr.framework.extensions.ltl.SATVerdict;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.CoverterDot;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWState;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.Sa3Solver3;
import eu.iv4xr.framework.goalsAndTactics.SaSolver4MultiMaze;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLibExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MiniDungeonModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.ModelLearner;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.ModelLearnerExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentStateExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Specifications;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.BasicAgent;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.utils.Pair;


/**
 * Number of maze can be set up. The solver has the strategy to automatically navigate to 
 * the next maze if the target is not in the current maze
 * @author Shirz002
 *
 */
public class Test_Random {

	boolean withGraphics = true;
	boolean supressLogging = false;
	Pair<Integer, Tile> covertToMDLocation(Vec3 p) {
		int mapNr = Math.round(p.y);
		Tile t = new Tile(Math.round(p.x), Math.round(p.z));
		return new Pair<Integer, Tile>(mapNr, t);
	}

	@Test
	public void test0() throws Exception {
		Pair targetItemOrShrine = new Pair("id", "H1_0");
		Pair additionalFeature = new Pair("maze", 1);
		//int seeds[] = {179371, 24681  , 98765, 12345, 54321, 71825 , 13579, 86420, 56789, 43210};
		int seed  = 54321;
		int maze = 2; 
		testFullPlay("Frodo",targetItemOrShrine, additionalFeature, seed, maze);

	}

	GoalStructure exhaustiveExplore(TestAgent agent, GoalLib goalLib, TacticLib tacticLib) {
		return WHILE(S -> !tacticLib.explorationExhausted(S), goalLib.smartExploring(agent, null, Integer.MAX_VALUE));
	}

	List<String> getConnectedEnablersFromBelief(String shrine, MyAgentState S) {
		List<String> openers = new LinkedList<>();
		if (S.gwmodel != null) {
			for (var entry : S.gwmodel.objectlinks.entrySet()) {
				if (entry.getValue().contains(shrine)) {
					openers.add(entry.getKey());
					return openers;
				}
			}
		}
		return openers;
	}
	static boolean isImmortalShrine(WorldEntity e) {		
		var shTy = (ShrineType) e.properties.get("shrinetype") ;
		return shTy == ShrineType.ShrineOfImmortals ;
	}
	public Pair<Boolean, Long> testFullPlay(String agentId, Pair targetItemOrShrine, Pair additionalFeature, int seed, int maze) throws Exception {
		// Create an instance of the game, attach an environment to it:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.numberOfMonsters = 6;
		config.numberOfScrolls = 3;
		config.viewDistance = 4;
		config.numberOfMaze = maze;
		config.randomSeed = seed;
		config.enableSmeagol = false;
		// change the Ferado maxBagSize to 1, it is in the Entity.java should make it
		// here
		System.out.println(">>> Configuration:\n" + config);

		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics;
		if (withGraphics)
			DungeonApp.deploy(app);

		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentStateExtended();
		Specifications specifications = new Specifications();

		var goalLib = new GoalLib();
		var tacticLib = new TacticLib();
		var goalLibExtended = new GoalLibExtended();

		// create an agent:
		var agent = new TestAgent(agentId, "tester");
		//Pair targetItemOrShrine = new Pair("id", "H2_3");
		// should be after create the agent, else the constructor sets the visibility
		// again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}

		// Goal: find a shrine and cleanse it:
		// Pair targetItemOrShrine = new Pair("type","HEALPOT") ;
		
		
		//applying Random
		 RandomPlayTester randomplay = new RandomPlayTester() ;
		 var GRandom = randomplay.randomPlay(agent) ;
		 nl.uu.cs.aplib.mainConcepts.Utils.changeRandomGoal(GRandom,
				 info -> { 
					    var S = info.fst ;
					 	var player = S.worldmodel.elements.get(S.worldmodel.agentId);
					 	if( !S.agentIsAlive()) {System.out.println("Player died!"); return false;}
					 	if(player.getPreviousState() != null) {
					 	   var previousProperties = player.getPreviousState().properties;
						   var H0 = previousProperties.get("itemsInBag").toString().contains("H1_0");//targetItemOrShrine.snd.toString());
					 	   var H1  = previousProperties.get("itemsInBag").toString().contains("H0_1");
					 	   var H2  = previousProperties.get("itemsInBag").toString().contains("H0_2");
					 	   var H3  = previousProperties.get("itemsInBag").toString().contains("H0_3");
					 	//type 1 
					 	   if(H0)
					 		   return true;
					 	   
					 	   //type 2
//					 	   if(H0 || H1 || H2 || H3)
//					 		  return true;
					 	   //type 3
//					 	   if(H0 && H1 && H2 && H3)
//					 		  return true;
					 	  //type 4
//					 	   var S1  = previousProperties.get("itemsInBag").toString().contains("S0_1");
//					 	   var R1  = previousProperties.get("itemsInBag").toString().contains("R0_1");
//					 	   if(H0 && R1 && S1 )
//					 		   return true;
					 	  
					 	}
					 	return false;
				 }) ;
		 
		 var type1 = FIRSTof(GRandom, FAIL());
		 

//		 
//		 SEQ(GRandom1,GRandom2);
		 
		// var G0 = exhaustiveExplore(agent,goalLib,tacticLib) ;

		agent.attachState(state).attachEnvironment(env).setGoal(type1);
		GameWorldModel model = new GameWorldModel(new GWState());
		// GameWorldModel model =
		// GameWorldModel.loadGameWorldModelFromFile("./tmp/modelMD1.json") ;

		ModelLearnerExtended modelLearner = new ModelLearnerExtended();
		agent.attachBehaviorModel(model, (S, m) -> {
			modelLearner.learn((MyAgentState) S, (GameWorldModel) m);
			return null;
		});

		var specs = new Specifications();
		var psi1 = specs.spec1();
		var psi2 = specs.spec2();
		var psi3 = specs.spec3();
		var psi4 = specs.spec4();
		var psi5 = specs.spec5();
//		var psiSp1 = specs.scenarioSpec1();
//		var psiSp2 = specs.scenarioSpec2();
//		var psiSp3 = specs.scenarioSpec3();
//		var psiSp4 = specs.scenarioSpec4();
//		var psiSp5 = specs.scenarioSpec5();
//		var psiSp6 = specs.scenarioSpec6();
		agent.addLTL(psi1, psi2, psi3, psi4, psi5);
		agent.resetLTLs();

		
		var invs = new MD_invs() ;
		List<LTL<SimpleState>> invs2 = new LinkedList<>() ;
		for (var i : invs.allInvs) {
			var ltl = LTL.always(i) ;
			invs2.add(ltl) ;
			agent.addLTL(ltl) ;
		}
		
		Thread.sleep(1000);

		// why do we need this starting update?
		state.updateState(agentId);
		// Utils.printEntities(state);

		// Now we run the agent:
		int delay = 10;
		long time0 = System.currentTimeMillis();
		TestMiniDungeonWithAgent.runAndCheck(agent, type1, false, delay, 3000);

		System.out.println("=== exploration exhausted: " + tacticLib.explorationExhausted(state));

		Scanner scanner = new Scanner(System.in);
		// scanner.nextLine() ;
		// assertTrue(G.getStatus().success()) ;

		System.out.println(">>> exec-time = " + (System.currentTimeMillis() - time0));

		model.defaultInitialState.currentAgentLocation = "START" + agentId;
		model.copyDefaultInitialStateToInitialState();
		model.name = "MiniDungeon";
		model.supportUSE = true;  
		// attaching alpha to the model:
		model.alpha = (i,affected) -> S -> { MiniDungeonModel.alphaFunction(agentId,i,affected,S) ; return null ; } ;
		model.use_alpha = (S,i) -> { MiniDungeonModel.alphaUseFunction(agentId,i,S) ; return null; } ;
		String fileName1 = "./tmp/modelMD1.json" ;
		String fileName2 = "./tmp/modelMD1.dot" ;
		model.save(fileName1);
		CoverterDot.saveAs(fileName2, model, true, true) ;

		/* Check the specifications */
		var ok = agent.evaluateLTLs();

		System.out.println(">>>> LTL results: " + ok);
		System.out.println(">>>> psi1 : " + psi1.sat());
		System.out.println(">>>> psi2 : " + psi2.sat());
		System.out.println(">>>> psi3 : " + psi3.sat());
		System.out.println(">>>> psi4 : " + psi4.sat());
		System.out.println(">>>> psi5 : " + psi5.sat());
//		System.out.println(">>>> psi6 : " + psiSp1.sat());
//		System.out.println(">>>> psi7 : " + psiSp2.sat());
//		System.out.println(">>>> psi8 : " + psiSp3.sat());
//		System.out.println(">>>> psi10 : " + psiSp4.sat());
//		System.out.println(">>>> psi11 : " + psiSp5.sat());
//		System.out.println(">>>> psi12 : " + psiSp6.sat());
		for(var inv: invs2) {
			System.out.println(">>>> Inv : " + inv.sat());
			assertTrue(inv.sat() == SATVerdict.SAT	);
		}
		assertTrue(psi1.sat() == SATVerdict.SAT	
				&& psi2.sat() == SATVerdict.SAT
				&& psi3.sat() == SATVerdict.SAT
				&& psi4.sat() == SATVerdict.SAT
//				&& psiSp1.sat() == SATVerdict.SAT
//				&& psiSp2.sat() == SATVerdict.SAT
//				&& psiSp3.sat() == SATVerdict.SAT
				) ;
		
	
		
		var totalTime  = (System.currentTimeMillis() - time0) /1000;
		return new Pair<Boolean, Long>(GRandom.getStatus().success(),totalTime);
	}

}
