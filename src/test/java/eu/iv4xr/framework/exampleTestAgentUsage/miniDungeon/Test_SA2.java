package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;
import java.util.logging.Level;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWState;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.Sa2Solver;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.ModelLearner;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.utils.Pair;

public class Test_SA2 {
	
	boolean withGraphics = true ;
	boolean supressLogging = true ;
	
	Pair<Integer,Tile> covertToMDLocation(Vec3 p) {
		int mapNr = Math.round(p.y) ;
		Tile t = new Tile(Math.round(p.x), Math.round(p.z)) ;
		return new Pair<Integer,Tile>(mapNr,t) ;
	}
	
	@Test
	public void test0() throws Exception {
		testFullPlay("Frodo") ;
	}
	
	List<String> getConnectedEnablersFromBelief(String shrine, MyAgentState S) {
		List<String> openers = new LinkedList<>() ;
		if (S.gwmodel != null) {
			for (var entry : S.gwmodel.objectlinks.entrySet()) {
				if (entry.getValue().contains(shrine)) {
					openers.add(entry.getKey()) ;
					return openers ;
				}
			}
		}
		return openers ;
	}
	
	public void testFullPlay(String agentId) throws Exception {
		// Create an instance of the game, attach an environment to it:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.randomSeed = 79371;
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
		var agent = new TestAgent(agentId, "tester");
		
		// should be after create the agent, else the constructor sets the visibility again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}
		
		int explorationBudget = 20;
		

		var sa2Solver = new Sa2Solver<Void>((S, e) -> Utils.isReachable((MyAgentState) S, e),
				(S, e) -> Utils.distanceToAgent((MyAgentState) S, e),
				S -> (e1, e2) -> Utils.distanceBetweenEntities((MyAgentState) S, e1, e2),
				eId -> SEQ(goalLib.smartEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				eId -> SEQ(goalLib.smartEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				S -> tacticLib.explorationExhausted(S), 
				(heuristicLocation,budget) -> goalLib.smartExploring(agent, covertToMDLocation(heuristicLocation), budget));

		// Goal-1: find the first shrine and cleanse it:
		String targetShrine = "SM0" ;
		var G = sa2Solver.solver(agent, 
				targetShrine, 
				new Vec3(1,0,1),
				e -> e.type.equals("" + EntityType.SHRINE),
				e -> e.type.equals("" + EntityType.SCROLL), 
				e -> e.type.equals("" + EntityType.SHRINE) && (boolean) e.properties.get("cleansed"),
				(shrine,S) -> getConnectedEnablersFromBelief(shrine, (MyAgentState) S),
				// well the same as above, as the enabling scroll is always in the same level:
				(shrine,S) -> getConnectedEnablersFromBelief(shrine, (MyAgentState) S),
				// we can't close a shrine again once it is cleaned, so this is not needed:
				(shrine,S) -> null,
				S -> { var S_ = (MyAgentState) S;
					   var e = S.worldmodel.elements.get(targetShrine);
					   if (e == null)
						   return false;
					   var clean = (boolean) e.properties.get("cleansed");
					   return clean; }, 
				Policy.NEAREST_TO_TARGET, 
				explorationBudget);

		
		agent.attachState(state).attachEnvironment(env).setGoal(G);
		GameWorldModel model = new GameWorldModel(new GWState()) ;
		ModelLearner modelLearner = new ModelLearner() ;
		agent.attachBehaviorModel(model, (S,m) -> {
			modelLearner.learn((MyAgentState) S, (GameWorldModel) m) ;
			return null ;
			}) ;

		Thread.sleep(1000);

		// why do we need this starting update?
		state.updateState(agentId);
		//Utils.printEntities(state);

		// Now we run the agent:
		int delay = 20 ;
		TestMiniDungeonWithAgent.runAndCheck(agent,G,false,delay,2000) ;

		Scanner scanner = new Scanner(System.in);
		scanner.nextLine() ;
		assertTrue(G.getStatus().success()) ;
			
		
		
	}

}
