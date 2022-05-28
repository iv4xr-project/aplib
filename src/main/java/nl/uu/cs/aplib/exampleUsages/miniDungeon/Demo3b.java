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
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.HealingPotion;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Scroll;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib.* ;

/**
 * In this Demo we use a generic algorithm, SA1, to let the agent search
 * for the right scroll to cleanse the shrine.
 */
public class Demo3b {
	
	    static boolean isReachable(MyAgentState S, WorldEntity e) {
	    	var aname = S.worldmodel.agentId ;
	        var player = S.worldmodel.elements.get(aname) ;
	        int player_maze = (int) player.properties.get("maze") ;
	        int e_maze = (int) e.properties.get("maze") ;
	        
			var t1 = toTile(player.position) ;
			var t2 = toTile(e.position) ;
			var path = adjustedFindPath(S, player_maze,t1.x,t1.y,e_maze,t2.x,t2.y) ;
			return path!=null && path.size()>0 ;
	    }
	    
	    static float distanceToAgent(MyAgentState S, WorldEntity e) {
	    	var aname = S.worldmodel.agentId ;
	        var player = S.worldmodel.elements.get(aname) ;
	        int player_maze = (int) player.properties.get("maze") ;
	        int e_maze = (int) e.properties.get("maze") ;
	        
	        if (e_maze == player_maze) {
	        	var p1 = player.position.copy() ;
		        var p2 = e.position.copy() ;
		        p1.y = 0 ;
		        p2.y = 0 ;
		        return  Vec3.distSq(p1, p2) ;
	        }
	        return Math.abs(e_maze - player_maze)*1000000 ;
	    }
	    	    
	    static float distanceBetweenEntities(MyAgentState S, WorldEntity e1, WorldEntity e2) {
	    	int e1_maze = (int) e1.properties.get("maze") ;
	        int e2_maze = (int) e2.properties.get("maze") ;
	        
	        if (e1_maze == e2_maze) {
	        	var p1 = e1.position.copy() ;
		        var p2 = e2.position.copy() ;
		        p1.y = 0 ;
		        p2.y = 0 ;
		        return  Vec3.distSq(p1, p2) ;
	        }
	        return Math.abs(e1_maze - e2_maze)*1000000 ;
	    }
	
	    
		public static void main(String[] args) throws Exception {
			// Create an instance of the game, attach an environment to it:
			MiniDungeonConfig config = new MiniDungeonConfig();
			config.numberOfHealPots = 4;
			config.viewDistance = 4 ;
			config.randomSeed = 79373;
			System.out.println(">>> Configuration:\n" + config);
			DungeonApp app = new DungeonApp(config);
			app.soundOn = false ;
			DungeonApp.deploy(app);
			MyAgentEnv env = new MyAgentEnv(app);
			MyAgentState state = new MyAgentState();
			var goalLib = new GoalLib();
			var tacticLib = new TacticLib() ;

			// create an agent:
			var agent = new TestAgent("Frodo", "player-frodo");	
			int explorationBudget = 20 ;
			
			var sa1Solver = new Sa1Solver<Void>(
					(S, e) -> isReachable((MyAgentState) S, e), 
					(S, e) -> distanceToAgent((MyAgentState) S, e), 
					S -> (e1, e2) -> distanceBetweenEntities((MyAgentState) S, e1, e2),
					eId -> SEQ(
							goalLib.smartFrodoEntityInCloseRange(agent,eId), 
							goalLib.entityInteracted(eId)), 
					eId -> SEQ(
							goalLib.smartFrodoEntityInCloseRange(agent,eId), 
							goalLib.entityInteracted(eId)), 
					S -> tacticLib.explorationExhausted(S),
					budget -> goalLib.smartFrodoExploring(agent,null,budget)) ;

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
					S -> gameStatus((MyAgentState) S) == GameStatus.FRODOWIN, 
					Policy.NEAREST_TO_AGENT, 
					explorationBudget);

			// Now, attach the game to the agent, and give it the above goal:
			var G = SEQ(G1, 
					    goalLib.entityInteracted("SM0"), 
					    //goalLib.smartFrodoEntityInCloseRange(agent,"SS1"),
						//goalLib.entityInteracted("SS1"),
					    G2) ;
			agent.attachState(state).attachEnvironment(env).setGoal(G);

			Thread.sleep(1000);

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
			//System.out.println("Frontiers: " + state.multiLayerNav.getFrontier()) ;
			//int maze = 0 ;
			//Tile frodoLoc = toTile(state.worldmodel.position) ;
			//System.out.println("Explor path: " + state.multiLayerNav.explore(new Pair<>(maze,frodoLoc))) ; ;
			
			//G.printGoalStructureStatus();
			System.exit(0);
		}

}
