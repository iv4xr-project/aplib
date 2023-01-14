package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static eu.iv4xr.framework.extensions.ltl.LTL.eventually;
import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.Logging;
import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;
import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.LTL;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.CoverterDot;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWObject;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWState;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWTransition;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.LabRecruitsModel;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.ModelLearner;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.PrintUtils;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MiniDungeonModel;

import nl.uu.cs.aplib.mainConcepts.GoalStructure;


/**
 * This simulates a playtest of the game MiniDungeon. It uses an iv4xr-agent
 * to automatically play the game.
 * 
 * The test sets up an instance of MiniDungeon with two mazes, then runs the
 * agent. It tests whether cleansing the final shrine indeed wins the game.
 */
public class Test_MiniDungeon_ModelConstruction {
	
	boolean withGraphics = true ;
	boolean supressLogging = true ;
	
	String mainplayer = "Frodo" ;

	
	//@Test
	public void testConstructModel() throws Exception {
		testFullPlay() ;
	}
	
	@Test
	public void testDOTConversion1() throws Exception {
		GameWorldModel model = GameWorldModel.loadGameWorldModelFromFile("./tmp/modelMD0.json") ;
		System.out.println(model) ;
		CoverterDot.saveAs("./tmp/modelMD0.dot", model, true, true) ;
	}
	
	//@Test
	public void testDOTConversion2() throws Exception {
		GameWorldModel model = LabRecruitsModel.mk_ButtonDoor1Level() ;
		System.out.println(model) ;
		CoverterDot.saveAs("./tmp/modelButtonDoors1.dot", model, true, true) ;
	}
	
	//@Test
	public void test_constructed_model() throws Exception {
		GameWorldModel model = GameWorldModel.loadGameWorldModelFromFile("./tmp/modelMD0.json") ;
		
		// attaching alpha to the model:
		model.alpha = (i,affected) -> S -> { MiniDungeonModel.alphaFunction(mainplayer,i,affected,S) ; return null ; } ;

		System.out.println(">>> model: \n" + model) ;

		var mc = new BuchiModelChecker(model) ;
		LTL<IExplorableState> solved = eventually(S -> {
			GWState st = (GWState) S ;
			GWObject finalShrine =  st.objects.get("SI2") ;
			boolean finalShrineCleansed = (Boolean)finalShrine.properties.get("cleansed") ;
			return st.currentAgentLocation.equals(finalShrine.id) && finalShrineCleansed ;
		});
		var sequence = mc.findShortest(solved,20) ;
		System.out.println(">>> ============== solution: " + sequence.path.size()) ;
		int k = 0 ;
		for (var step : sequence.path) {
			System.out.print(">> step " + k + ":") ;
			GWTransition tr = (GWTransition) step.fst ;
			if (tr!=null) System.out.print("" + tr.getId()) ;
			System.out.println("") ;
			var S = (GWState) sequence.getStateSequence().get(k).fst ;
			var P = S.objects.get(mainplayer) ;
			var SM0 = S.objects.get("SM0") ;
			//System.out.println("   state: @ " + S.currentAgentLocation 
			//		+ ", player has " + P.properties.get("bagslot1")
			//		+ ", SM0 cleansed " + SM0.properties.get("cleansed")
			//		+ ", SM0 open " + SM0.properties.get(GameWorldModel.IS_OPEN_NAME)) ;
			k++ ;	
		}
		
		
		MiniDungeonConfig config = myMiniDungeonConfiguration() ;
		System.out.println(">>> Configuration:\n" + config);
		
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics ;
		if(withGraphics) DungeonApp.deploy(app);
		
		
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState();
		
		var agent = new TestAgent("Frodo", "tester");
		GoalStructure G = MiniDungeonModel.convert2ToGoalStructure(agent,sequence) ;
		agent.attachState(state).attachEnvironment(env).setGoal(G);

		Thread.sleep(1000);
		
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
	    k = 0 ;
		while(G.getStatus().inProgress()) {
			agent.update();
			System.out.println("** [" + k + "] agent @" + Utils.toTile(state.worldmodel.position)) ;
			// delay to slow it a bit for displaying:
			Thread.sleep(10); 
			if (k>=2000) break ;
			k++ ;
		}	
		assertTrue(G.getStatus().success())	 ;
		state.updateState(mainplayer);
		var finalShrine = state.worldmodel.elements.get("SI2") ;
		System.out.println(">> final shrine: " + finalShrine) ;
		assertTrue((Boolean) finalShrine.properties.get("cleansed")) ;
		//(new Scanner(System.in)).nextLine() ;
		
	}
	
	MiniDungeonConfig myMiniDungeonConfiguration() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 4 ;
		config.numberOfMaze = 3 ;
		config.randomSeed = 1393;
		return config ;
	}
	
	public void testFullPlay() throws Exception {
		// Create an instance of the game, attach an environment to it:
		MiniDungeonConfig config = myMiniDungeonConfiguration() ;
		System.out.println(">>> Configuration:\n" + config);
		
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics ;
		if(withGraphics) DungeonApp.deploy(app);
		
		
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState();
		var goalLib = new GoalLib();
		var tacticLib = new TacticLib();

		// create an agent:
		String agentId = "Frodo" ;
		var agent = new TestAgent(agentId, "tester");
		
		// should be after create the agent, else the constructor sets the visibility again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}
		
		int explorationBudget = 20;
		
		var sa1Solver = new Sa1Solver<Void>((S, e) -> Utils.isReachable((MyAgentState) S, e),
				(S, e) -> Utils.distanceToAgent((MyAgentState) S, e),
				S -> (e1, e2) -> Utils.distanceBetweenEntities((MyAgentState) S, e1, e2),
				eId -> SEQ(goalLib.smartEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				eId -> SEQ(goalLib.smartEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				S -> tacticLib.explorationExhausted(S), budget -> goalLib.smartExploring(agent, null, budget));

		// Goal-1: find the first shrine and cleanse it:
		var G1 = sa1Solver.solver(agent, 
				"SM0", 
				e -> e.type.equals("" + EntityType.SCROLL), 
				S -> { var S_ = (MyAgentState) S;
					   var e = S.worldmodel.elements.get("SM0");
					   if (e == null)
						   return false;
					   var clean = (boolean) e.properties.get("cleansed");
					   return clean; }, 
				Policy.NEAREST_TO_AGENT, explorationBudget);

		// Goal-2:
		var G2 = sa1Solver.solver(agent, 
				"SM1", 
				e -> e.type.equals("" + EntityType.SCROLL), 
				S -> { var S_ = (MyAgentState) S;
					   var e = S.worldmodel.elements.get("SM1");
					   if (e == null)
						   return false;
					   var clean = (boolean) e.properties.get("cleansed");
					   return clean; }, 
				Policy.NEAREST_TO_AGENT, explorationBudget);
		
		// Goal-3: find the final shrine and clease it; check if then Frodo wins:
		var G3 = sa1Solver.solver(agent, 
				"SI2", 
				e -> e.type.equals("" + EntityType.SCROLL),
				S -> ((MyAgentState) S).gameStatus() 
				     == 
				     (agentId.equals("Frodo") ? GameStatus.FRODOWIN : GameStatus.SMEAGOLWIN), 
				Policy.NEAREST_TO_AGENT, explorationBudget);

		// Now, attach the game to the agent, and give it the above goal:
		var G = SEQ(G1, 
				goalLib.entityInteracted("SM0"),
				G2,
				goalLib.entityInteracted("SM1"),
				G3
				);
		agent.attachState(state).attachEnvironment(env).setGoal(G);
		
		GameWorldModel model = new GameWorldModel(new GWState()) ;
		ModelLearner modelLearner = new ModelLearner() ;
		agent.attachBehaviorModel(model, (S,m) -> {
			modelLearner.learn((MyAgentState) S, (GameWorldModel) m) ;
			return null ;
			}) ;

		Thread.sleep(1000);
		
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		while(G.getStatus().inProgress()) {
			agent.update();
			System.out.println("** [" + k + "] agent @" + Utils.toTile(state.worldmodel.position)) ;
			// delay to slow it a bit for displaying:
			Thread.sleep(10); 
			if (k>=2000) break ;
			k++ ;
		}	
		assertTrue(G.getStatus().success())	 ;
		
		model.defaultInitialState.currentAgentLocation = "START" + agentId ;
		model.copyDefaultInitialStateToInitialState();
		model.name = "MiniDungeon" ;
		
		System.out.println(">>> model: \n" + model) ;
		
		model.save("./tmp/modelMD0.json");

	}
	
	
}
