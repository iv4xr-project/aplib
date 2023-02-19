package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import java.util.Random;
import java.util.function.Function;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldModel;

public class PvPPlayTester {
	
	public TacticLib tacticLib = new TacticLib() ;
	public GoalLib goalLib = new GoalLib() ;
	float fleeChance = 0.25f ;
	Random rnd = new Random() ;
	
	Tactic combatTactic(String opponent) {
		
		Tactic T = FIRSTof(
				tacticLib.interactTac(opponent)) ;
		
		return T ;
		
	}
	
	Action flee() {
		return action("flee").do2((MyAgentState S) -> (Tile t) -> 
			new Pair<>(S,tacticLib.moveTo(S,t)))
		.on((MyAgentState S) -> {
			if (rnd.nextFloat() > fleeChance) 
				return null ;
			var candidates = Utils.adjacentTiles(S,Utils.toTile(S.worldmodel.position)) ;
			for (Tile t : candidates) {
				if (Utils.isFreeTile(S,t)) {
					return t ;
				}
			}
			return null ;
		}) ;
	}
	
	GoalStructure opponentKilled(String opponent) {
		return goal("" + opponent + " killed")
		.toSolve((Pair<MyAgentState,WorldModel> Z) ->(Integer)  Z.snd.val(opponent,"hp") <= 0)
		.withTactic(
		   FIRSTof(Abort().on_((MyAgentState S) -> !S.agentIsAlive()).lift(),
				   SEQ(flee().lift(), flee().lift()),
				   tacticLib.interactTac(opponent),
				   ABORT()))
		.lift() ;	
	}
	
	/**
	 * Find and kill the given opponent.
	 */
	GoalStructure searchAndKill(TestAgent agent, String opponent) {
		GoalStructure G = 
		 REPEAT(SEQ(goalLib.smartEntityInCloseRange(agent, opponent),
				    FIRSTof(opponentKilled(opponent),
					    	lift((MyAgentState S) -> !S.agentIsAlive()))
				    )
				) ;
		return G ;
	}

}
