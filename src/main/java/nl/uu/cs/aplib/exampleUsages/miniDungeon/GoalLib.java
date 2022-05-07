package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.*;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import eu.iv4xr.framework.mainConcepts.*;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib.* ;

import java.util.Scanner;

public class GoalLib {
	
	public TacticLib tacticLib = new TacticLib() ;
	
	/**
	 * This will search the maze to guide the agent o a tile next to the 
	 * specified entity ("touching" the entity). 
	 * 
	 * <p>The goal's tactic can also handle some critical situations that may 
	 * emerge during the search, e.g. if it is attacked by a monster, or
	 * when it gets low in the health.
	 */
	public Goal EntityTouched(String targetId) {
		
		var G = goal("Entity " + targetId + " is touched.") 
				.toSolve((WorldModel wom) -> {
					WorldEntity e = wom.elements.get(targetId) ;
					if (e==null) return false ;
					return adjacent(toTile(wom.position),toTile(e.position)) ;
				})
				.withTactic(
				   FIRSTof(tacticLib.useHealingPot(),
						   tacticLib.useRagePot(),
						   tacticLib.attackMonster(),
						   tacticLib.navigateTo(targetId),
						   tacticLib.explore(),
						   //Abort().on_(S -> { System.out.println("### about to abort") ; return false;}).lift(), 
				   		   ABORT()) 
				  )
				;
		
		return G ;		
	}
	
	/**
	 * This goal causes the agent to interact with a given entity. It requires the agent
	 * to be standing next to the entity.
	 */
	public Goal EntityInteracted(String targetId) {
		
		var G = goal("Entity " + targetId + " is interacted.") 
				.toSolve((WorldModel wom) -> true)
				.withTactic(
				   FIRSTof(tacticLib.interact(targetId),
						   ABORT()) 
				  )
				;
		
		return G ;
		
	}

}
