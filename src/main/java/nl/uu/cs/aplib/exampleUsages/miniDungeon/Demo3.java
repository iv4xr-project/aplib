package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib.*;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.HealingPotion;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Scroll;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib.* ;

/**
 * In this Demo we use a generic algorithm, SA1, to let the agent search
 * for the right scroll to cleanse the shrine.
 */
public class Demo3 {
	
		public static void main(String[] args) throws InterruptedException {
		// Create an instance of the game, attach an environment to it:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4 ;
		config.viewDistance = 4 ;
		config.randomSeed = 79373 ;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState() ;
		var goalLib = new GoalLib() ;
		
		// create an agent:
		var agent = new TestAgent("Frodo","Frodo")  ;
		
		// create and configure the SA1 algorithm:
		Function<Iv4xrAgentState, BiFunction<Vec3, Vec3, Boolean>> isReachable = S
				-> (p1,p2) -> {
					var t1 = toTile(p1) ;
					var t2 = toTile(p2) ;
					var path = adjustedFindPath((MyAgentState) S, t1.x,t1.y,t2.x,t2.y) ;
					return path!=null && path.size()>0 ;
				} ;
				
		var sa1Solver = new Sa1Solver(
				isReachable,
				eId -> SEQ(Demo2.SmartEntityTouched(agent, goalLib, eId), goalLib.EntityInteracted(eId).lift()), 
				eId -> SEQ(Demo2.SmartEntityTouched(agent, goalLib, eId), goalLib.EntityInteracted(eId).lift()),
				goalLib.tacticLib.explore()	
				) ;
		
		var G = sa1Solver.solver(agent, 
				"Shr", 
				e -> e.type.equals(Scroll.class.getSimpleName()), 
				S -> gameStatus((MyAgentState) S) == GameStatus.FRODOWIN , 
				Policy.NEAREST_TO_AGENT, 
				20) ;

		// Now, attach the game to the agent, and give it the above goal:
		agent. attachState(state)
			 . attachEnvironment(env)
			 . setGoal(G) ;

		Thread.sleep(1000);
		
		//state.updateState("Frodo");
		//printEntities(state) ;
		
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		while(G.getStatus().inProgress()) {
			agent.update();
			System.out.println("** [" + k + "] agent @" + toTile(state.worldmodel.position)) ;
			// delay to slow it a bit for displaying:
			Thread.sleep(200); 
			if (k>=1000) break ;
			k++ ;
		}	
		//G.printGoalStructureStatus();	
		//System.exit(0);	
	}

}
