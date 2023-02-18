package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils; 

public class Test_Walls {
	
	boolean withGraphics = false ;
	boolean supressLogging = true ;
	boolean stopAfterAgentDie = true ;	
	boolean verbosePrint = false ;
	
	TacticLib tacticLib = new TacticLib() ;
	GoalLib goalLib = new GoalLib() ;

	static enum Direction { NORTH, SOUTH, EAST, WEST } 
	
	static class AlgorithmState {
		Tile walkStartingLocation ;
		Direction walkDirection ;
		Map<Tile,Integer> visitCount = new HashMap<>() ;
		int N ; // maze size
		
		void registerVisit(Tile tile) {
			if (walkStartingLocation == null) walkStartingLocation = tile ;
			Integer cnt = visitCount.get(tile) ;
			if (cnt == null) 
				visitCount.put(tile,0) ;
			else
				visitCount.put(tile, cnt+1) ;
		}
		
		boolean shouldStop(MyAgentState S) {
			Tile currentTile = Utils.toTile(S.worldmodel.position) ;
			System.out.println("### checking should-stop, current: " + currentTile
					+ ", walk-start:" + walkStartingLocation);
			if (currentTile.equals(walkStartingLocation)) return true ;
			Integer cnt = visitCount.get(currentTile) ;
			System.out.println("   visit cnt at current:" + cnt) ;
			return cnt != null && cnt > 2 ;
		}
	}
	
	boolean leftIsWall(MyAgentState S, AlgorithmState st) {
		Tile left = leftTile(S,st.walkDirection) ;
		return Utils.isWall(S,left) 
			   || (left.x == st.N-2 && left.y == 1) 
			   || (left.x == 1 && left.y == 1)
				;
	}
	
	boolean frontIsWall(MyAgentState S, AlgorithmState st) {
		Tile front = frontTile(S,st.walkDirection) ;
		return Utils.isWall(S,front) 
				|| (front.x == st.N-2 && front.y == 1) 
				|| (front.x == 1 && front.y == 1)
				;
	}
	
	Tile frontTile(MyAgentState S, Direction walkDirection) {
		Tile front = Utils.toTile(S.worldmodel.position) ;
		switch(walkDirection) {
		   case NORTH: front.y++ ; break ;
		   case SOUTH: front.y-- ; break ;
		   case EAST: front.x++ ; break ;
		   case WEST: front.x-- ;
		}
		return front ;
	}
	
	Tile leftTile(MyAgentState S, Direction walkDirection) {
		Tile left = Utils.toTile(S.worldmodel.position) ;
		switch(walkDirection) {
		   case NORTH: left.x-- ; break ;
		   case SOUTH: left.x++ ; break ;
		   case EAST: left.y++ ; break ;
		   case WEST: left.y-- ;
		}
		return left ;
	}
	
	Tile rightTile(MyAgentState S, Direction walkDirection) {
		Tile right = Utils.toTile(S.worldmodel.position) ;
		switch(walkDirection) {
		   case NORTH: right.x++ ; break ;
		   case SOUTH: right.x-- ; break ;
		   case EAST: right.y-- ; break ;
		   case WEST: right.y++ ;
		}
		return right ;
	}
	
	Direction turnLeft(Direction walkDirection) {
		switch(walkDirection) {
		   case NORTH: return Direction.WEST ;
		   case SOUTH: return Direction.EAST ;
		   case EAST: return Direction.NORTH ;
		   case WEST: 
		   default: return Direction.SOUTH ;
		}

	}
	
	Direction turnRight(Direction walkDirection) {
		switch(walkDirection) {
		   case NORTH: return Direction.EAST ;
		   case SOUTH: return Direction.WEST ;
		   case EAST: return Direction.SOUTH ;
		   case WEST: 
		   default: return Direction.NORTH ;
		}
	}
	
	Action registerTile(AlgorithmState st) {
		return action("move forward")
				  .do1((MyAgentState S) -> {
					  st.registerVisit(Utils.toTile(S.worldmodel.position)) ;
					  return true ;
		})  ;
	}
	
	Action moveForward(AlgorithmState st) {
		return action("move forward")
		  .do1((MyAgentState S) -> {
			  Tile next = frontTile(S,st.walkDirection) ;
			  //st.registerVisit(next) ;
			  System.out.println("=== move forward to " + next + ", dir:" + st.walkDirection) ;
			  return tacticLib.moveTo(S,next) ;
		  })  ;
	}
	
	Action turnLeft(AlgorithmState st) {
		return action("turn left")
		  .do1((MyAgentState S) -> {
			  Tile next = leftTile(S,st.walkDirection) ;
			  //st.registerVisit(next) ;
			  st.walkDirection = turnLeft(st.walkDirection) ;
			  System.out.println("=== turn left to " + next + ", dir:" + st.walkDirection) ;
			  return tacticLib.moveTo(S,next) ;
		}) ;	
	}
	
	Action turnRight(AlgorithmState st) {
		return action("turn left")
		  .do1((MyAgentState S) -> {
			  Tile next = rightTile(S,st.walkDirection) ;
			  //st.registerVisit(next) ;
			  st.walkDirection = turnRight(st.walkDirection) ;
			  System.out.println("=== turn right to " + next + ", dir:" + st.walkDirection) ;
			  return tacticLib.moveTo(S,next) ;
		}) ;	
	}
	
	GoalStructure atNextTile(AlgorithmState st) {
	  return goal("At the next tile.").toSolve_(o -> true)
		.withTactic(
		   FIRSTof(
			 turnLeft(st).on_((MyAgentState S) -> ! leftIsWall(S,st)).lift(),
			 turnRight(st).on_((MyAgentState S) -> frontIsWall(S,st)).lift(),
			 moveForward(st).lift()
		   ))
        .lift() ;
	}

	
	GoalStructure walkDone(TestAgent agent, AlgorithmState st) {
	   return REPEAT(
			    FIRSTof(
				   lift((MyAgentState S) -> st.shouldStop(S)),
				   SEQ(lift("Tile registered",registerTile(st)),
					   adjacentWallsChecked(agent),
					   atNextTile(st), 
					   FAIL())
                )
			  ) ;
	}

	private GoalStructure wallChecked(Tile current, Tile wall) {
		Action checking = action("checking wall").do1((MyAgentState S) -> {
			if (!Utils.isWall(S,wall)) {
				return S.env().observe(S.worldmodel.agentId) ;
			}
			var obs = tacticLib.moveTo(S, wall) ;
			System.out.println(">>> checking wall " + wall) ;
			if (Utils.toTile(obs.position).equals(wall)) {
				System.out.println("   WALL BUG!") ;
			}
		    return obs ;
		}) ;
		
		Action restorePosition = action("go back to the base position").do1((MyAgentState S) 
				-> 
				Utils.toTile(S.worldmodel.position).equals(current) ? 
					S.env().observe(S.worldmodel.agentId) :
			        tacticLib.moveTo(S, current)) ;
		
		Goal g = goal("Wall " + wall + " is checked.")
		   .toSolve((WorldModel obs) -> Utils.toTile(obs.position).equals(current)) 
		   .withTactic(SEQ(checking.lift(), restorePosition.lift())) ;
		
		return g.lift() ;
	}
	
	Tile getTile(MyAgentState S, Direction dir) {
		Tile q = Utils.toTile(S.worldmodel.position) ;
		switch(dir) {
		   case NORTH: q.y++ ; break ; 
		   case SOUTH: q.y-- ; break ;
		   case EAST: q.x++ ; break ;
		   case WEST: q.x-- ;
		}
		return q ;
	}
	
	GoalStructure wallChecked(TestAgent agent, Direction dir) {
		return DEPLOY(agent, (MyAgentState S) -> {
			Tile current = Utils.toTile(S.worldmodel.position) ;
			Tile wallToCheck = getTile(S,dir) ;
			/*
			if (inMap(S,wallToCheck)) {
			   return wallChecked(current,wallToCheck) ;
			}
			else return SUCCESS() ;
			*/
			return FIRSTof(
					  lift((MyAgentState T) -> ! Utils.inMap(T,wallToCheck)),
					  wallChecked(current,wallToCheck)
				    );
		}) ;
	}
	

	GoalStructure adjacentWallsChecked(TestAgent agent) {
		return SEQ(wallChecked(agent,Direction.NORTH),
				   wallChecked(agent,Direction.SOUTH),
				   wallChecked(agent,Direction.WEST),
				   wallChecked(agent,Direction.EAST)) ;		
	}
	
	GoalStructure atTile(int mazeNr, Tile q) {
		return goal("At tile " + q + " in maze " + mazeNr)
		  .toSolve((Pair<MyAgentState,WorldModel> prop) -> {
			  //System.out.println("### goal: " + q) ;
			  return Utils.currentMazeNr(prop.snd) == mazeNr && q.equals(Utils.toTile(prop.snd.position)) ;
		  }
		  ) 
		  .withTactic(FIRSTof(
				  tacticLib.navigateToAction(mazeNr,q.x,q.y).lift(),
				  tacticLib.explore(null),
				  ABORT()))
		  .lift() ;	
	}
	
	Action initializeWalk(AlgorithmState st) {
		return action("initialize a walk")
				.do1((MyAgentState S) -> {
					Tile currentTile = Utils.toTile(S.worldmodel.position) ;
					st.N = S.auxState().getIntProperty("worldSize") ;
					st.walkStartingLocation = null ;
					//st.walkStartingLocation = currentTile ;
					for (Tile q : Utils.adjacentTiles(S,currentTile)) {
						if (Utils.isWall(S,q)) {
							if (q.x < currentTile.x) st.walkDirection = Direction.NORTH ;
							else if (q.x > currentTile.x) st.walkDirection = Direction.SOUTH ;
							else if (q.y < currentTile.y) st.walkDirection = Direction.WEST ;
							else st.walkDirection = Direction.EAST ;
						}
					}
					return true ;
				}) ;
	}
		
	Tile getUncheckedTile(MyAgentState S, AlgorithmState st) {
		var visited = st.visitCount.keySet() ;
		for (var e : S.worldmodel.elements.values()) {
			if (e.id.startsWith("W")) {
				Tile te = Utils.toTile(e.position) ;
				for (Tile q : Utils.adjacentTiles(S,te)) {
					if (Utils.isFreeTile(S,q) && !visited.contains(q)) {
						return q ;
					}
				}
			}
		}
		return null ;
	}
	
	GoalStructure nextToAWall(TestAgent agent, AlgorithmState st) {
		return DEPLOY(agent,(MyAgentState S) -> {
			Tile q = getUncheckedTile(S,st) ;
			if (q != null) {
				//System.out.println("### invoking atTile " + q) ;
				//return SEQ(atTile(currentMazeNr(S),q), FAIL()) ;
				return atTile(Utils.currentMazeNr(S),q) ;
			}
			return FAIL() ;
		}) ;
	}
	
	
	
	GoalStructure allWallsChecked(TestAgent agent) {
		AlgorithmState st = new AlgorithmState() ;
		//return allWallsChecked(agent) ;
		return REPEAT(
				  FIRSTof(lift((MyAgentState S) -> getUncheckedTile(S,st) == null),
						  SEQ(nextToAWall(agent,st), 
							  lift("walk initialized",initializeWalk(st)),
							  walkDone(agent,st),
							  FAIL() // to force REPEAT
						   )
				  )) ;
	}
	
	@Test
	public void testWalls() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig0() ;
		System.out.println(">>> Configuration:\n" + config);
		
		String agentId = "Frodo" ;		
		var agent = new TestAgent(agentId, "tester");
		
		//GoalStructure G = testWalls(agent) ;
		GoalStructure G = allWallsChecked(agent) ;
		
		/*
		GoalStructure G0 = SEQ(goalLib.smartEntityInCloseRange(agent,"W_0_0_9"),
				goalLib.entityInteracted("W_0_0_9"),
				goalLib.smartEntityInCloseRange(agent,"W_0_0_10"),
				goalLib.entityInteracted("W_0_0_10"));
		*/
		
		int sleep = 1 ;
		var state = TPJUtils.runAgent(agent, config, G, 8000, sleep, 
				stopAfterAgentDie, 
				withGraphics, 
				supressLogging,
				verbosePrint);
		
		//System.out.println("path = " + TacticLib.adjustedFindPath(state, 0,2,9,0,0,9)) ;
		//System.out.println("path = " + TacticLib.adjustedFindPath(state, 0,2,9,0,1,9)) ;

		assertTrue(G.getStatus().success()) ;
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine() ;
	}
	

}
