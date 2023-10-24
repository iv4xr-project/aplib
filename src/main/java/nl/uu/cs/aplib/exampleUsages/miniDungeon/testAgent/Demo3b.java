package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib.*;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.HealingPotion;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Scroll;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;
import onlineTestCaseGenerator.Sa1Solver;
import onlineTestCaseGenerator.Sa1Solver.Policy;

/**
 * In this Demo we use a generic algorithm, SA1, to let the agent search
 * for the right scroll to cleanse the shrine.
 * 
 * @author wish
 */
public class Demo3b {
	
	public static void main(String[] args) throws Exception {
		// Create an instance of the game, attach an environment to it:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4 ;
		config.randomSeed = 79371;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false ;
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState();
		var goalLib = new GoalLib();
		var tacticLib = new TacticLib() ;

		// create an agent:
		//String agentId = "Smeagol" ;
		String agentId = "Frodo" ;
		var agent = new TestAgent(agentId, "tester");	

		int explorationBudget = 20 ;

		var sa1Solver = new Sa1Solver<Void>(
				(S, e) -> Utils.isReachable((MyAgentState) S, e), 
				(S, e) -> Utils.distanceToAgent((MyAgentState) S, e), 
				S -> (e1, e2) -> Utils.distanceBetweenEntities((MyAgentState) S, e1, e2),
				eId -> SEQ(
						goalLib.smartEntityInCloseRange(agent,eId), 
						goalLib.entityInteracted(eId)), 
				eId -> SEQ(
						goalLib.smartEntityInCloseRange(agent,eId), 
						goalLib.entityInteracted(eId)), 
				S -> tacticLib.explorationExhausted(S),
				budget -> goalLib.smartExploring(agent,null,budget)) ;

		var G1 = sa1Solver.solver(agent, 
				"SM0", e -> e.type.equals("" + EntityType.SCROLL),
				S -> {
					var S_ = (MyAgentState) S ;
					var e = S.worldmodel.elements.get("SM0") ;
					if (e==null) return false ;
					var clean = (boolean) e.properties.get("cleansed") ;
					return clean ;
				}, 
				Policy.NEAREST_TO_AGENT, 
				explorationBudget);

		var G2 = sa1Solver.solver(agent, 
				"SI1", e -> e.type.equals("" + EntityType.SCROLL),
				S -> ((MyAgentState) S).gameStatus() 
						== 
						(agentId.equals("Frodo") ? GameStatus.FRODOWIN : GameStatus.SMEAGOLWIN), 
				Policy.NEAREST_TO_AGENT, 
				explorationBudget);

		// Now, attach the game to the agent, and give it the above goal:
		var G = SEQ(G1, 
				goalLib.entityInteracted("SM0"), 
				//goalLib.smartEntityInCloseRange(agent,"SI1")
				//goalLib.entityInteracted("SS1"),
				G2
				) ;
		agent.attachState(state).attachEnvironment(env).setGoal(G);

		Thread.sleep(1000);

		state.updateState(agentId);
		PrintUtils.printEntities(state);

		// Now we run the agent:
		System.out.println(">> Start agent loop...");
		int k = 0;
		while (G.getStatus().inProgress()) {
			agent.update();
			System.out.println("** [" + k + "] agent @" + Utils.toTile(state.worldmodel.position));
			// delay to slow it a bit for displaying:
			Thread.sleep(50);
			if (k >= 2000)
				break;
			k++;
		}		
		//System.exit(0);
	}

}
