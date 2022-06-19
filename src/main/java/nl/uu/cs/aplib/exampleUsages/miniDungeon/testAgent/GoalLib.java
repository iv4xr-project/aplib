package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.mainConcepts.Tactic.PrimitiveTactic;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib.*;

import eu.iv4xr.framework.goalsAndTactics.IInteractiveWorldGoalLib;
import eu.iv4xr.framework.mainConcepts.*;
import nl.uu.cs.aplib.utils.Pair;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;

import java.util.function.Predicate;

public class GoalLib implements IInteractiveWorldGoalLib<Pair<Integer,Tile>>{
	
	public TacticLib tacticLib = new TacticLib() ;
	
	/**
	 * This will search the maze to guide the agent to a tile next to the 
	 * specified entity ("touching" the entity). 
	 * 
	 * <p>The goal's tactic can also handle some critical situations that may 
	 * emerge during the search, e.g. if it is attacked by a monster, or
	 * when it gets low in the health.
	 */
	@Override
	public GoalStructure entityInCloseRange(String targetId) {
		
		var G = goal("Entity " + targetId + " is touched.") 
				.toSolve((Pair<MyAgentState,WorldModel> proposal) -> {
					var S = proposal.fst ;
					WorldModel previouswom = S.worldmodel ;
					WorldModel newObs = proposal.snd ;
					WorldEntity e = previouswom.getElement(targetId) ;
					if (e==null) {
						return false ;
					}
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					var solved =  mazeId(a) == mazeId(e) && adjacent(toTile(newObs.position),toTile(e.position)) ;
					//System.out.println(">>> checking goal") ;
					return solved; 
				})
				.withTactic(
				   FIRSTof(tacticLib.useHealingPot,
						   tacticLib.useRagePot,
						   tacticLib.attackMonster,
						   //tacticLib.navigateTo(targetId),
						   tacticLib.navigateNextTo(targetId),
						   tacticLib.explore(null),
						   //Abort().on_(S -> { System.out.println("### about to abort") ; return false;}).lift(), 
				   		   ABORT()) 
				  )
				;
		
		return G.lift() ;		
	}
	
	Predicate<MyAgentState> whenToGoAfterHealPot = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
		int maxBagSize = (int) player.properties.get("maxBagSize") ;
		var healPotsInVicinity = TacticLib.nearItems(S,EntityType.HEALPOT,4) ;
		return agentIsAlive(S) && maxBagSize-bagSpaceUsed >= 1 && healPotsInVicinity.size() > 0 ;
	} ;
	
	Predicate<MyAgentState> whenToGoAfteRagePot = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
		int maxBagSize = (int) player.properties.get("maxBagSize") ;
		var ragePotsInVicinity = TacticLib.nearItems(S,EntityType.RAGEPOT,4) ;
		return agentIsAlive(S) && maxBagSize-bagSpaceUsed >= 1 && ragePotsInVicinity.size() > 0 ;
	} ;
	
	/**
	 * A goal to send the agent to pick up a healing pot nearby. It is a dynamic goal,
	 * as it will pick any such pot (rather than a specific one decided upfront).
	 */
	GoalStructure grabHealPot(TestAgent agent) { 
		return DEPLOY(agent,
		  (MyAgentState S) -> {
			  var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
			  int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
			  var healPotsInVicinity = TacticLib.nearItems(S,EntityType.HEALPOT,5) ;
			  //System.out.println("===== checking deploy grab ") ;
			  if (bagSpaceUsed>0 || healPotsInVicinity.size() == 0) {
			      return FAIL() ;
			  }
			  var pot = healPotsInVicinity.get(0) ;
			  //System.out.println("===== deploy grab " + pot.id) ;
			  return SEQ(entityInCloseRange(pot.id),
					     entityInteracted(pot.id)) ;
		   }
	    ) ;
	}
	
	GoalStructure checkIfentityIsInCloseRange(String targetId) {
		
		return lift("Check if entity " + targetId + " is touched",(MyAgentState S) -> {
					WorldEntity e = S.worldmodel.getElement(targetId) ;
					if (e==null) {
						return false ;
					}
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					return mazeId(a)==mazeId(e) && adjacent(toTile(S.worldmodel.position),toTile(e.position)); 
				})	;	
	}
	
	/**
	 * A smarter version of entityInCloseRange(e) goal, that will also pick up a nearby
	 * healing potion along the way, if the bag is empty. 
	 * This is intended for Frodo. 
	 * 
	 * <p>TODO: Smeagol variant; it should immediately drink
	 */
	public GoalStructure smartFrodoEntityInCloseRange(
				TestAgent agent, 
			 	String targetId) { 
		
	
	   var G = INTERRUPTIBLE(
			       ((PrimitiveGoal) entityInCloseRange(targetId)).getGoal(),
			       HANDLE(whenToGoAfterHealPot, grabHealPot(agent))
			    ) ;
	   // Add an extra checing because an interuptible goal-struct always succeeds:
	   return SEQ(G, checkIfentityIsInCloseRange(targetId)) ;
	}
	
	/**
	 * This goal causes the agent to interact with a given entity. It requires the agent
	 * to be standing next to the entity.
	 */
	@Override
	public GoalStructure entityInteracted(String targetId) {
		
		var useHealPotTac = (PrimitiveTactic) tacticLib.useHealingPot ;
		useHealPotTac.on_((MyAgentState S) -> { 
			return false ;
		}) ;
		
		var bla = goal("xxx") ;
		
		
		var G = goal("Entity " + targetId + " is interacted.") 
				.toSolve(proposal -> true)
				.withTactic(
				   FIRSTof(tacticLib.interact(targetId),
						   ABORT()) 
				  )
				;
		
		return G.lift() ;
		
	}

	@Override
	public GoalStructure positionInCloseRange(Pair<Integer, Tile> p) {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public GoalStructure entityStateRefreshed(String entityId) {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public GoalStructure exploring(Pair<Integer, Tile> heuristicLocation, int budget) {
		GoalStructure explr = goal("exploring (persistent-goal: aborted when it is terminated)").toSolve(belief -> false)
				.withTactic(FIRSTof(
						tacticLib.useHealingPot,
						tacticLib.useRagePot,
						tacticLib.attackMonster,
						tacticLib.explore(heuristicLocation),
						ABORT()))
				.lift()
				.maxbudget(budget)
				;
		
		return explr ;
	}
	
	public GoalStructure smartFrodoExploring(TestAgent agent, 
			Pair<Integer, Tile> heuristicLocation, 
			int budget) {
		
		Goal explore = ((PrimitiveGoal) exploring(heuristicLocation,budget)).getGoal();
		Tactic originalExploreTac = explore.getTactic() ;
		
		Tactic abortToGetAHealPot = Abort().on_(whenToGoAfterHealPot).lift() ;
		
		Tactic newExploreTac = FIRSTof(
				  abortToGetAHealPot, 
				  originalExploreTac
				) ;

		return FIRSTof(
				  explore.withTactic(newExploreTac).lift(),
				  SEQ(grabHealPot(agent),FAIL())	
				) ;
	}

}
