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
	
	/**
	 * An invariant can set this flag to true to signal that it spots a violation.
	 * The test agent can use this flag to stop its test early rather than waiting
	 * until its complete run is completed. Normally invariants are checkes like
	 * other LTL formulas, so at the end of the run. This flag gives a way for the
	 * agent to break the test run early.
	 */
	public boolean bugFlagged = false ;
	/**
	 * The last invariant that causes bug-flagging.
	 */
	public String invViolated = "" ;
	
	boolean flagVerdict(Boolean verdict, String invName) {
		if (!verdict) {
			bugFlagged = true ;
			invViolated = invName ;			
		}
		return verdict ;
	}
	
	boolean hpInv(MyAgentState S) {
		int hp = (Integer) S.val("hp") ;
		Integer hpPrev = (Integer) S.before("hp") ;
		int hpmax = (Integer) S.val("hpmax") ;
		var ok = (hp>=0 || (hpPrev!=null && (hpPrev>0 || hp == hpPrev))) 
				&& hp <= hpmax && hpmax>0 ;
		return flagVerdict(ok,"hpInv") ;
	}
	
	boolean scoreInv(MyAgentState S) {
		int score = (Integer) S.val("score") ;
		Integer prev = (Integer) S.before("score") ;
		String B = Utils.otherPlayer(S) ;
		boolean otherAgentJustDead =
				S.worldmodel.elements.get(B) != null 
				&& (Integer) S.val(B,"hp") <=0 
				&& (Integer) S.before(B,"hp") > 0 ;
		var ok = prev!=null ? score >= prev || otherAgentJustDead : true ;		
		return flagVerdict(ok,"scoreInv") ;
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
				//bugFlagged = true ;
				System.out.println("## BUG!") ;
			}
			assertTrue(ok) ;
			flagVerdict(ok,"positionInv") ;
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
				return flagVerdict(1<=distSq && distSq<=2 , "teleportInv") ;
			}
			else if (prevMaze > mazeId){
				// teleporting down
				int N = (Integer) S.val("aux","worldSize") ;
				float distSq = Vec3.distSq(new Vec3(N-2,0,1),pos) ;
				return flagVerdict(1<=distSq && distSq<=2 , "teleportInv") ;
			}
		}
		return true ;
	}
	
	Predicate<SimpleState> ragingInv() {
		
		String[] didAnAction = { null } ;
		
		Predicate<SimpleState> inv = (SimpleState T) -> {
			
			MyAgentState S = (MyAgentState) T ;	
		
			boolean didAnAction_ = didAnAction[0] != null ;
			
			var cmd_ = S.env().getLastOperation() ;
			if (cmd_ == null) {
				didAnAction[0] = null ;
				return true ;
			}
			else {
				didAnAction[0] = cmd_.command ;
			}
			
			// no checking if the agent does not do any action:
			if (!didAnAction_) return true ;
			var gameStatus = (GameStatus) S.val("aux","status") ; 
			// no checking if the agent died or game is over:
			if (gameStatus != gameStatus.INPROGRESS || ! S.agentIsAlive()) return true ;

			var rageIHad = (Integer) S.before("ragepotsInBag") ;
			var rageIHaveNow = (Integer) S.val("ragepotsInBag") ;
			var rageTimeBefore = (Integer) S.before("rageTimer") ;
			var rageTimerNow = (Integer) S.val("rageTimer") ;

			boolean ok1 = rageIHad==null 
					|| rageIHad <= rageIHaveNow
					|| rageTimerNow == 9 ;

			boolean ok2 = rageTimeBefore == null 
					|| rageTimeBefore == 0 
					|| rageTimerNow == Math.max(rageTimeBefore-1 , 0) ;
					//|| rageTimerNow <= rageTimeBefore ;

			//System.out.println(">>>>>>>  action: " + cmd_.command) ;
			if (!ok1 || !ok2) {
				//System.out.println(">>> RAGE BUG action: " + cmd_.command) ;
				System.out.print  ("    violating ") ;
				if (!ok1) System.out.print("ok1") ;
				if (!ok2) System.out.print(" ok2") ;
				System.out.println(", agent: " + S.worldmodel.elements.get(S.worldmodel.agentId)) ;
				System.out.println("   ragePotInBag before: " + rageIHad) ;
				System.out.println("   rageTimer before: " + rageTimeBefore) ;

			}

			return flagVerdict(ok1 && ok2,"ragingInv") ;
		} ;
		
		return inv ;
	}
	
	private GameStatus iWin(MyAgentState S) {
		if (S.worldmodel.agentId.equals("Frodo")) return GameStatus.FRODOWIN ;
		else return GameStatus.SMEAGOLWIN ;
	}
	
	boolean immortalShrineInv(MyAgentState S) {
		int numberOfMazes = S.env().app.dungeon.config.numberOfMaze ;
		String ImmortalShrineId = "SI" + (numberOfMazes - 1) ; 
		if (S.worldmodel.elements.get(ImmortalShrineId) == null) 
			return true ;
		var scrollIHad = (Integer) S.before("scrollsInBag") ;
		var scrollIHaveNow = (Integer) S.val("scrollsInBag") ;
		var immortalStatusNow = (Boolean) S.val(ImmortalShrineId,"cleansed") ;
		var immortalStatusBefore = (Boolean) S.before(ImmortalShrineId,"cleansed") ;
		var gameStatus = (GameStatus) S.val("aux","status") ;
		var scoreBefore = (Integer) S.before("score") ;
		var scoreNow = (Integer) S.val("score") ;
		
		boolean ok = immortalStatusBefore == null || scrollIHad == null
				|| immortalStatusBefore          // shrine was already clean
				|| scrollIHad <= scrollIHaveNow  // not using a scroll
				|| !immortalStatusNow            // use scroll but the shrine is not cleansed
				|| (gameStatus == iWin(S)  && scoreNow == scoreBefore + 1000)  // the agent cleanse the shrine!
				;
		
		return flagVerdict(ok,"immortalShrineInv") ;
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
	

	
	
	/**
	 * Specification of move; specifying when it is possible/impossible to move.
	 * The spec is incomplete, but still pretty strong.
	 */
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
					//bugFlagged = true ;
					System.out.println(">>> BUG! " + S.worldmodel.agentId + " command: " + executedAction[0]) ;
					System.out.println(">>> expected pos:" + expectedLocation[0]  
							+ ", actual pos:" + pos) ;
					//System.out.println(">>>" + S.get(S.worldmodel.agentId)) ;
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
			return flagVerdict(locationCorrect,"moveSpec") ;
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
			ragingInv(),
			S -> immortalShrineInv((MyAgentState) S),
			moveSpec()
	) ;
	
	Predicate<SimpleState>[] selectedInvs(int ... selections) {
		Predicate<SimpleState>[] chosen = new Predicate[selections.length] ;
		for (int k=0; k<chosen.length; k++)
			chosen[k] = allInvs[selections[k]] ;
		return chosen ;
 	}

}
