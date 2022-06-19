package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;
import java.util.logging.Level;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.Logging;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Demo3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.PrintUtils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;


/**
 * This simulates a playtest of the game MiniDungeon. It uses an iv4xr-agent
 * to automatically playe the game.
 * 
 * The test sets up an instance of MiniDungeon with two mazes, then runs the
 * agent. It tests whether cleansing the final shrine indeed wins the game.
 */
public class TestMiniDungeon {
	
	boolean withGraphics = false ;
	boolean supressLogging = true ;

	
	@Test
	public void test3b() throws Exception {
		// Create an instance of the game, attach an environment to it:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.randomSeed = 79373;
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
		var agent = new TestAgent("Frodo", "player-frodo");
		
		// should be after create the agent, else the constructor sets the visibility again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}
		
		int explorationBudget = 20;
		

		var sa1Solver = new Sa1Solver<Void>((S, e) -> Demo3.isReachable((MyAgentState) S, e),
				(S, e) -> Demo3.distanceToAgent((MyAgentState) S, e),
				S -> (e1, e2) -> Demo3.distanceBetweenEntities((MyAgentState) S, e1, e2),
				eId -> SEQ(goalLib.smartFrodoEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				eId -> SEQ(goalLib.smartFrodoEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				S -> tacticLib.explorationExhausted(S), budget -> goalLib.smartFrodoExploring(agent, null, budget));

		// Goal-1: find the first shrine and cleanse it:
		var G1 = sa1Solver.solver(agent, "SM0", e -> e.type.equals("" + EntityType.SCROLL), S -> {
			var S_ = (MyAgentState) S;
			var e = S.worldmodel.elements.get("SM0");
			if (e == null)
				return false;
			var clean = (boolean) e.properties.get("cleansed");
			return clean;
		}, Policy.NEAREST_TO_AGENT, explorationBudget);

		// Goal-2: find the final shrine and clease it; check if then Frodo wins:
		var G2 = sa1Solver.solver(agent, 
				
				"SI1", 
				
				e -> e.type.equals("" + EntityType.SCROLL),
				
				S -> MyAgentState.gameStatus((MyAgentState) S) == GameStatus.FRODOWIN, 
				
				Policy.NEAREST_TO_AGENT, explorationBudget);

		// Now, attach the game to the agent, and give it the above goal:
		var G = SEQ(G1, goalLib.entityInteracted("SM0"),
				// goalLib.smartFrodoEntityInCloseRange(agent,"SS1"),
				// goalLib.entityInteracted("SS1"),
				G2);
		agent.attachState(state).attachEnvironment(env).setGoal(G);

		Thread.sleep(1000);
		

		try {

			state.updateState("Frodo");
			//Utils.printEntities(state);
			
			// Now we run the agent:
			//runAndCheck(agent,G,false,20,2000) ;
			runAndCheck(agent,G,false,0,2000,
					always(S -> intProp(S,"Frodo","hp") <= intProp(S,"Frodo","hpmax")),
					always(S -> intProp(S,"Frodo","hp") > 0 ),
					always(S -> intProp(S,"Frodo","maze") <= 1 ),
					always(S -> intProp(S,"Smeagol","hp") > 0 ),
					always(S -> IMP(intProp(S,"Frodo","hp") <= 0 
							        && intProp(S,"Smeagol","hp") <= 0,
							        S.auxState().properties.get("status").equals(GameStatus.MONSTERSWIN))),
					always(S -> intProp(S,"Frodo","scrollsInBag")
							+ intProp(S,"Frodo","healpotsInBag")
							+ intProp(S,"Frodo","ragepotsInBag")
							<= intProp(S,"Frodo","maxBagSize")
							),
					// should not walk through wall:
					always(S -> {
						Tile p = getPos(S,"Frodo") ;
						int N = config.worldSize ;
						return p.x > 0 && p.x < N-1 && p.y > 0 && p.y < N-1 ; 
					}),
					eventually(S -> intProp(S,"Frodo","scrollsInBag") > 0),
					eventually(S -> intProp(S,"Frodo","healpotsInBag") > 0),
					eventually(S -> intProp(S,"Frodo","hp") < intProp(S,"Frodo","hpmax")),
					eventually(S -> intProp(S,"Frodo","maze") == 1 )
					
					) ;
			
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
	
	
	public static int intProp(MyAgentState S, String eId, String propName) {
		var e = S.worldmodel.elements.get(eId) ;
		return (int) e.properties.get(propName) ;
	}

	public static Tile getPos(MyAgentState S, String eId) {
		var e = S.worldmodel.elements.get(eId) ;
		return Utils.toTile(e.position) ;
	}

	public static boolean IMP(boolean p, boolean q) {
		return !p || q ;
	}
	
	
	public static class Specification {
		Predicate<MyAgentState> p ;
	}
	
	public static class Always extends Specification { }

	public static class Eventually extends Specification { }
	
	public static Always always(Predicate<MyAgentState> p) {
		Always phi = new Always() ; phi.p = p ; return phi ;
	}
	
	public static Eventually eventually(Predicate<MyAgentState> p) {
		Eventually phi = new Eventually() ; phi.p = p ; return phi ;
	}

	
	public static void runAndCheck(TestAgent agent, 
			GoalStructure G,
			boolean verbose, 
			int delay, int budget,
			Specification ... phis	
			) throws Exception 
	{
		System.out.println(">> Start agent loop...");
		var state = (MyAgentState) agent.state() ;
		int k = 0;
		boolean[] eventuallySpecResults = new boolean[phis.length] ;
		for(int s=0; s < phis.length; s++) {
			eventuallySpecResults[s] = false ;
		}
		
		int lastTurn = state.auxState().getIntProperty("turn") ;
		
		while (G.getStatus().inProgress()) {
			agent.update();
			
			int turn = state.auxState().getIntProperty("turn") ;
			
			if (verbose) 
				System.out.println("** [" + k + "/" + turn + "] agent @" + Utils.toTile(state.worldmodel.position));

			if (turn > lastTurn) {
				
				// check:
				for (int s=0; s<phis.length; s++) {
					Specification spec = phis[s] ;
					if (spec instanceof Always) {
						//System.out.println("checking") ;
						assertTrue(spec.p.test(state)) ;
					}
					else {
						eventuallySpecResults[s] = eventuallySpecResults[s] || spec.p.test(state) ;
					}
					
				}
				
				lastTurn = turn ;
			}
			// delay to slow it a bit for displaying:
			
			if (delay>0) {
				Thread.sleep(delay);
			}
			if (k >= budget)
				break;
			k++;
		}
		
		for (int s=0; s < phis.length; s++) {
			
			if (phis[s] instanceof Eventually) {
				assertTrue(eventuallySpecResults[s]) ;
			}
			
		}
		
		
		
	}
	
}
