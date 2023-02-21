package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

public class MD_invs {
	
	boolean hpInv(MyAgentState S) {
		int hp = (Integer) S.val("hp") ;
		Integer hpPrev = (Integer) S.before("hp") ;
		int hpmax = (Integer) S.val("hpmax") ;
		return (hp>=0 || (hpPrev!=null && (hpPrev>0 || hp == hpPrev))) 
				&& hp <= hpmax && hpmax>0 ;
	}
	
	boolean scoreInv(MyAgentState S) {
		int score = (Integer) S.val("score") ;
		Integer prev = (Integer) S.before("score") ;
		String B = Utils.otherPlayer(S) ;
		boolean otherAgentJustDead =
				S.worldmodel.elements.get(B) != null 
				&& (Integer) S.val(B,"hp") <=0 
				&& (Integer) S.before(B,"hp") > 0 ;
		return prev!=null ? score >= prev || otherAgentJustDead : true ;		
	}
	
	boolean positionInv(MyAgentState S) {
		Vec3 pos = S.worldmodel.position ;
		int N = (Integer) S.val("aux","worldSize") ;
		int mazeId = (Integer) S.val("maze") ;
		Vec3 old = S.positionBefore() ;
		if (old != null) {
			int prevMaze = (Integer) S.before("maze") ;
			boolean ok = 
					1<=pos.x && pos.x <= N-2  
					&& 1 <=pos.z && pos.z <= N-2 
				    // player can only teleport, or move one sq at a time:
				    && (Math.abs(mazeId - prevMaze) == 1
				       || (mazeId == prevMaze && 
				          ((Math.abs(pos.x - old.x) <= 1 && pos.z == old.z)
				           || (Math.abs(pos.z - old.z) <= 1 && pos.x == old.x)))) 
			
				   ;
			//System.out.println("## old:" + old + ", new:" + pos) ;
			//System.out.println("## old-maze:" + prevMaze + ", new-maze:" + mazeId) ;
			if (!ok) {
				System.out.println("## BUG!") ;
			}
			assertTrue(ok) ;
			return ok ;
		}
		return true ;
	}
	
	
	boolean teleportInv(MyAgentState S) {
		int mazeId = (Integer) S.val("maze") ;
		Integer prevMaze = (Integer) S.before("maze") ;
		if (prevMaze !=null) {
			Vec3 pos = S.worldmodel.position ;
			if (prevMaze < mazeId) {
				// teleporting up:
				float distSq = Vec3.distSq(new Vec3(1,0,1),pos) ;
				return 1<=distSq && distSq<=2 ;
			}
			else if (prevMaze > mazeId){
				// teleporting down
				int N = (Integer) S.val("aux","worldSize") ;
				float distSq = Vec3.distSq(new Vec3(N-2,0,1),pos) ;
				return 1<=distSq && distSq<=2 ;
			}
		}
		return true ;
	}
	
	WorldEntity getEntityAtTile(MyAgentState S, Tile t) {
		int mazeId = (Integer) S.val("maze") ;
		for (var e : S.worldmodel.elements.values()) {
			if (e.position == null) continue ;
			int m = e.getIntProperty("maze") ;
			Tile tile_e = Utils.toTile(e.position)  ;
			if (m == mazeId && t.equals(tile_e)) {
				return e ;
			}
		}
		return null ;
	}
	
	
	Predicate<SimpleState> moveSpec() {
		
		Vec3[] expectedLocation = { null } ;
		String[] executedAction = { null } ;

		return (SimpleState T) -> {

			MyAgentState S = (MyAgentState) T ;	
			Vec3 pos = S.worldmodel.position ;
			var cmd_ = S.env().getLastOperation() ;
			String cmd = null ;
			if (cmd_ != null) cmd = cmd_.command ;
			
			// check if the current location is as expected:
			boolean locationCorrect = true ;
			if (expectedLocation[0] != null) {
				//System.out.println(">>> checking") ;
				int hp = (Integer) S.val("hp") ;
				locationCorrect =   hp <= 0 || pos.equals(expectedLocation[0]) ;
				if (!locationCorrect) {
					System.out.println(">>> BUG! " + S.worldmodel.agentId + " command: " + executedAction[0]) ;
					System.out.println(">>> expected pos:" + expectedLocation[0]  
							+ ", actual pos:" + pos) ;
					System.out.println(">>>" + S.get(S.worldmodel.agentId)) ;
					System.out.println(">>>") ;
					//System.out.println(">>>" + S.get("Frodo")) ;
				}
			}
			
			//System.out.println(">>> " + cmd + ", expected pos:" + expectedLocation[0]  
			//		+ ", actual pos:" + pos) ;
	
			
			// calculate the next expected location:
			var gstatus = (GameStatus) S.val("aux","status") ;
			if (cmd==null) {
				expectedLocation[0] = null ;
				executedAction[0] = null ;
			}
			else if(! S.agentIsAlive() || gstatus != GameStatus.INPROGRESS) {
				expectedLocation[0] = S.worldmodel.position.copy() ;
				executedAction[0] = cmd + ", but the agent is dead or the game is over.";
			}
			else {
				Vec3 moveToLoc = null ;
				int bagSpace = (Integer) S.val("maxBagSize") - (Integer) S.val("bagUsed") ;
				executedAction[0] = cmd ;
				switch (cmd) {
				case "MOVEUP"   : moveToLoc = new Vec3(pos.x,0,pos.z+1)  ; break ;
				case "MOVEDOWN" : moveToLoc = new Vec3(pos.x,0,pos.z-1)  ; break ;
				case "MOVELEFT" : moveToLoc = new Vec3(pos.x-1,0,pos.z)  ; break ;
				case "MOVERIGHT" : moveToLoc = new Vec3(pos.x+1,0,pos.z)  ; break ;
				}
				if (moveToLoc == null) {
					expectedLocation[0] = S.worldmodel.position.copy() ;
				}
				else {
					WorldEntity inFrontOfMe = getEntityAtTile(S,Utils.toTile(moveToLoc)) ;
					if (inFrontOfMe == null) {
						expectedLocation[0] = moveToLoc ;
						executedAction[0] = cmd + " to a free square." ;
					}
					else if (Utils.isWall(inFrontOfMe)) {
						// the agent tries to bump a wall:
						expectedLocation[0] = pos.copy() ;
						executedAction[0] = cmd + " against a wall." ;
						//expectedLocation[0] = null ;
					}
					else if (Utils.isScroll(inFrontOfMe) 
							|| Utils.isHealPot(inFrontOfMe)
							|| Utils.isRagePot(inFrontOfMe)
							) {
						if(bagSpace>0) {
							expectedLocation[0] = moveToLoc ;
							executedAction[0] = cmd + " to grab a " + inFrontOfMe.type ;
						}
						else {
							expectedLocation[0] = pos.copy() ;
							executedAction[0] = cmd + " to try to grab a " 
							+ inFrontOfMe.type 
							+ " but the bag is full." ;
						}
					}
					else {
						// other cases, ... for now, make no expectation:
						expectedLocation[0] = null ;
					}
				}
			}
			//assertTrue(locationCorrect) ;
			return locationCorrect ;
		} ;
	}
	
	Predicate<SimpleState>[] wrap_(Predicate<SimpleState> ... predicates) {
		return predicates ;
	}

	@SuppressWarnings("unchecked")
	Predicate<SimpleState>[] allInvs = wrap_(
			S -> hpInv((MyAgentState) S),
			S -> scoreInv((MyAgentState) S),
			S -> positionInv((MyAgentState) S),
			S -> teleportInv((MyAgentState) S),
			moveSpec()
			
	) ;
	
	Predicate<SimpleState>[] selectedInvs(int ... selections) {
		Predicate<SimpleState>[] chosen = new Predicate[selections.length] ;
		for (int k=0; k<chosen.length; k++)
			chosen[k] = allInvs[selections[k]] ;
		return chosen ;
 	}

}
