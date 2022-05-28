package eu.iv4xr.framework.exampleTestAgentUsage;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib.gameStatus;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib.toTile;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Demo3;

import eu.iv4xr.framework.goalsAndTactics.Sa1Solver;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Utils;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

public class TestMiniDungeon {

	
	@Test
	public void test3b() throws Exception {
		// Create an instance of the game, attach an environment to it:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.randomSeed = 79373;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState();
		var goalLib = new GoalLib();
		var tacticLib = new TacticLib();

		// create an agent:
		var agent = new TestAgent("Frodo", "player-frodo");
		int explorationBudget = 20;

		var sa1Solver = new Sa1Solver<Void>((S, e) -> Demo3.isReachable((MyAgentState) S, e),
				(S, e) -> Demo3.distanceToAgent((MyAgentState) S, e),
				S -> (e1, e2) -> Demo3.distanceBetweenEntities((MyAgentState) S, e1, e2),
				eId -> SEQ(goalLib.smartFrodoEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				eId -> SEQ(goalLib.smartFrodoEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				S -> tacticLib.explorationExhausted(S), budget -> goalLib.smartFrodoExploring(agent, null, budget));

		var G1 = sa1Solver.solver(agent, "SM0", e -> e.type.equals("" + EntityType.SCROLL), S -> {
			var S_ = (MyAgentState) S;
			var e = S.worldmodel.elements.get("SM0");
			if (e == null)
				return false;
			var clean = (boolean) e.properties.get("cleansed");
			return clean;
		}, Policy.NEAREST_TO_AGENT, explorationBudget);

		var G2 = sa1Solver.solver(agent, "SI1", e -> e.type.equals("" + EntityType.SCROLL),
				S -> gameStatus((MyAgentState) S) == GameStatus.FRODOWIN, Policy.NEAREST_TO_AGENT, explorationBudget);

		// Now, attach the game to the agent, and give it the above goal:
		var G = SEQ(G1, goalLib.entityInteracted("SM0"),
				// goalLib.smartFrodoEntityInCloseRange(agent,"SS1"),
				// goalLib.entityInteracted("SS1"),
				G2);
		agent.attachState(state).attachEnvironment(env).setGoal(G);

		Thread.sleep(1000);

		try {

			state.updateState("Frodo");
			Utils.printEntities(state);

			// Now we run the agent:
			System.out.println(">> Start agent loop...");
			int k = 0;
			while (G.getStatus().inProgress()) {
				agent.update();
				System.out.println("** [" + k + "] agent @" + toTile(state.worldmodel.position));
				// delay to slow it a bit for displaying:
				Thread.sleep(20);
				if (k >= 2000)
					break;
				k++;
			}
			
			// System.out.println("Frontiers: " + state.multiLayerNav.getFrontier()) ;
			// int maze = 0 ;
			// Tile frodoLoc = toTile(state.worldmodel.position) ;
			// System.out.println("Explor path: " + state.multiLayerNav.explore(new
			// Pair<>(maze,frodoLoc))) ; ;

			// G.printGoalStructureStatus();
			// System.exit(0);
			
			assertTrue(G.getStatus().success()) ;
			
		} catch (Exception e) {
			
			Assertions.fail("The SUT crashed.") ;

		}
		
	}
	
}
