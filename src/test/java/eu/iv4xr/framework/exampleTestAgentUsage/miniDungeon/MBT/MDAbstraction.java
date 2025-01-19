package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;

/**
 * Contain bunch of functions over agent-state, to form basic terms for writing
 * state-predicates, guards and post-conditions for MBT models.
 */
public class MDAbstraction {

	public static int int_(Serializable val) {
		return (Integer) val ;
	}

	public static boolean bool_(Serializable val) {
		return (Boolean) val ;
	}

	public static boolean inCombat(MyAgentState S) {
		return ! S.adajcentMonsters().isEmpty() ;
	}

	public static boolean justKilledAMonster(MyAgentState S) {
		var recentlyRemoved = (String[]) S.auxState().properties.get("recentlyRemoved") ;
		for (var e : recentlyRemoved) {
			if (e.startsWith("M")) return true ;
		}
		return false ;
	}

	public static int hpAdjacentMonsters(MyAgentState S) {
		return S.adajcentMonsters().stream()
				.map(e -> int_(e.properties.get("hp"))).collect(Collectors.summingInt(x -> x)) ;
	}

	public static int oldHpAdjacentMonsters(MyAgentState S) {
		return S.adajcentMonsters().stream()
				.map(e -> int_(S.before(e.id,"hp"))).collect(Collectors.summingInt(x -> x)) ;
	}

	public static WorldEntity adjacentNonWallEntity(MyAgentState S, EntityType ty) {
		var z = TacticLib.nearItems(S,ty,1) ;
		return z.isEmpty() ? null : z.get(MBT_MD_Model.rndx.nextInt(z.size())) ;
	}

	public static boolean isAdjacentToNonWallObject(MyAgentState S) {
		var maze = Utils.currentMazeNr(S) ;
		var agentTile = Utils.toTile(S.worldmodel.position) ;
		for (var e : S.worldmodel.elements.values()) {
			if (e.type != null
					&& (Utils.isHealPot(e) || Utils.isRagePot(e) || Utils.isShrine(e) || Utils.isMonster(e) 
					|| e.type.equals(EntityType.FRODO.toString())
					|| e.type.equals(EntityType.SMEAGOL.toString())
					)
					&& Utils.mazeId(e) == maze 
					&& Utils.adjacent(Utils.toTile(e.position), agentTile)) {
				return true ;
			}
		}
		return false ;
	}

	public static WorldEntity adjacentWall(MyAgentState S) {
		var maze = Utils.currentMazeNr(S) ;
		var agentTile = Utils.toTile(S.worldmodel.position) ;
		for (var e : S.worldmodel.elements.values()) {
			if (Utils.isWall(e)
					&& Utils.mazeId(e) == maze 
					&& Utils.adjacent(Utils.toTile(e.position), agentTile)) {
				return e ;
			}
		}
		return null ;
	}

	public static WorldEntity adjacentShrine(MyAgentState S, ShrineType sty) {
		var e = adjacentNonWallEntity(S,EntityType.SHRINE) ;
		if (e == null) 
			return null ;
		return (ShrineType) e.properties.get("shrinetype") == sty ? e : null ;
	}

	public static WorldEntity adjacentPickableItem(MyAgentState S) {
		WorldEntity e = adjacentNonWallEntity(S, EntityType.SCROLL) ;
		if (e != null) return e ; 
		e = adjacentNonWallEntity(S, EntityType.RAGEPOT) ;
		if (e != null) return e ;
		return adjacentNonWallEntity(S, EntityType.HEALPOT) ;
	}

	/**
	 * Return the walls that have a neighboring free tile, so they can be 'touched'.
	 */
	public static List<WorldEntity> touchableWalls(MyAgentState S) {
		List<WorldEntity> WS = new LinkedList<>() ;
		for (var e : S.worldmodel.elements.values()) {
			if (Utils.isWall(e) && 
				Utils.mazeId(e) == Utils.currentMazeNr(S)) {
				var ntiles = Utils.adjacentTiles(S, Utils.toTile(e.position)) ;
				for (var t : ntiles) {
					if (Utils.isFreeTile(S, t)) {
						WS.add(e) ;
						break ;
					}
				}
			}
		}
		return WS ;	
	}

	public static List<WorldEntity> entitiesInSameMaze(MyAgentState S, EntityType ty, ShrineType sty) {
		int mazeNr = Utils.currentMazeNr(S) ;
		var Z = S.worldmodel.elements.values().stream()
					   .filter(e -> e.type != null && e.type.equals("" + ty) && Utils.mazeId(e) == mazeNr) ;
		if (sty != null)	   
			Z = Z.filter(e -> e.properties.get("shrinetype") != null
							  && ((ShrineType) e.properties.get("shrinetype")) == sty) ;
		
		return Z.collect(Collectors.toList()) ;
	}

	public static WorldEntity closedShrineInSameMaze(MyAgentState S) {
		int mazeNr = Utils.currentMazeNr(S) ;
		var Z = S.worldmodel.elements.values().stream()
					   .filter(e -> Utils.isShrine(e)
					      && Utils.mazeId(e) == mazeNr
					      && ! bool_(e.properties.get("cleansed"))) 
					   .collect(Collectors.toList()) ;
		return Z.isEmpty() ? null : Z.get(0) ;
	}

	public static int itemInBag(MyAgentState S, EntityType ty) {
		switch(ty) {
		   case HEALPOT: return int_(S.val("healpotsInBag")) ;
		   case RAGEPOT: return int_(S.val("ragepotsInBag")) ;
		   case SCROLL : return int_(S.val("scrollsInBag")) ;
		}
		throw new IllegalArgumentException() ;
	}

	public static int oldItemInBag(MyAgentState S, EntityType ty) {
		switch(ty) {
		   case HEALPOT: return int_(S.before("healpotsInBag")) ;
		   case RAGEPOT: return int_(S.before("ragepotsInBag")) ;
		   case SCROLL : return int_(S.before("scrollsInBag")) ;
		}
		throw new IllegalArgumentException() ;
	}

	public static int bagUsed(MyAgentState S) { return int_(S.val("bagUsed")) ;}
	public static int oldBagUsed(MyAgentState S) { return int_(S.before("bagUsed")) ;}
	public static int maxBagSize(MyAgentState S) { return int_(S.val("maxBagSize")) ;}

	public static int hp(MyAgentState S) { return int_ (S.val("hp")) ; }
	public static int hpmax(MyAgentState S) { return int_ (S.val("hpmax")) ; }
	public static int oldHp(MyAgentState S) { return int_ (S.before("hp")) ; }

	public static int maze(MyAgentState S) { return int_ (S.val("maze")) ; }
	public static int oldMaze(MyAgentState S) { return int_ (S.before("maze")) ; }

	public static int rageTimer(MyAgentState S) { return int_ (S.val("rageTimer")) ; }

	public static boolean cleansed(MyAgentState S, String shrineId) { return bool_(S.val(shrineId,"cleansed")) ;}
	public static boolean oldCleansed(MyAgentState S, String shrineId) { return bool_(S.before(shrineId,"cleansed")) ;}

	public static boolean gameover(MyAgentState S) { return S.gameStatus() != GameStatus.INPROGRESS ; }
	public static boolean playerWin(MyAgentState S) { return S.gameStatus() == GameStatus.FRODOWIN ; }

}
