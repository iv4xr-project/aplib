package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.adjacent;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.mazeId;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.toTile;

import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;

public class ExtraGoalLib {
	
	// wall should be in the same maze as the agent
	public static GoalStructure nextToWall(MyAgentState S, WorldEntity W) {
		var Wmaze = mazeId(W) ;
		var adjacents = Utils.adjacentTiles(S, Utils.toTile(W.position)) ;
		adjacents = adjacents.stream()
					.filter(t -> Utils.isFreeTile(S, t))
					.collect(Collectors.toList()) ;
		
		if (adjacents.isEmpty())
			return null ;
		
		var freeTile = adjacents.get(0) ;
		
		var tacticLib = new TacticLib() ;
		
		var G = goal("Entity " + W.id + " is touched.") 
				.toSolve((Pair<MyAgentState,WorldModel> proposal) -> {
					WorldModel newObs = proposal.snd ;
					var a = newObs.elements.get(S.worldmodel.agentId) ;
					var agentTile = Utils.toTile(a.position) ;
					var solved =  mazeId(a) == Wmaze && agentTile.equals(freeTile) ;
					return solved; 
				})
				.withTactic(
				   FIRSTof(tacticLib.useHealingPotAction()
						   	  .on_(tacticLib.hasHealPot_and_HpLow)
						   	  .lift()
						   ,
						   tacticLib.useRagePotAction()
						   	  .on_(tacticLib.hasRagePot_and_inCombat)
						   	  .lift()
						   ,
						   tacticLib.attackMonsterAction()
						      .on_(tacticLib.inCombat_and_hpNotCritical)
						      .lift(),
						   tacticLib.navigateToAction(Wmaze,freeTile.x,freeTile.y).lift(),
						   tacticLib.explore(null),
						   //Abort().on_(S -> { System.out.println("### about to abort") ; return false;}).lift(), 
				   		   ABORT()) 
				  )
				;
		
		return G.lift() ;		
	}

}
