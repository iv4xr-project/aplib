package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import eu.iv4xr.framework.goalsAndTactics.IInteractiveWorldGoalLib;
import eu.iv4xr.framework.mainConcepts.*;
import nl.uu.cs.aplib.utils.Pair;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.* ;

import java.util.function.Predicate;

/**
 * Provide several basic goal-structures.
 * 
 * <p>Keep in mind that the provided navigation and exploration
 * tactics/goals currently has no ability to deal with items that
 * block a corridor. The solution is for now to just generate
 * another dungeon where we have no corridors are not blocked by
 * items (use another random seed, for example). A better fix
 * would be to have a smarter navigation and exploration. TO DO.
 * 
 * @author wish
 *
 */
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
						   tacticLib.navigateToTac(targetId),
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
		return S.agentIsAlive() 
				&& maxBagSize-bagSpaceUsed >= 1 
				&& healPotsInVicinity.size() > 0 ;
	} ;
	
	Predicate<MyAgentState> whenToGoAfterRagePot = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
		int maxBagSize = (int) player.properties.get("maxBagSize") ;
		int numRagePotsInBag = (int) player.properties.get("ragepotsInBag") ;
		var ragePotsInVicinity = TacticLib.nearItems(S,EntityType.RAGEPOT,4) ;
		return S.agentIsAlive() 
				&& numRagePotsInBag == 0
				&& maxBagSize-bagSpaceUsed >= 1 
				&& ragePotsInVicinity.size() > 0 ;
	} ;
	
	/**
	 * A goal to send the agent to pick up a heal or rage pot nearby. It is a dynamic goal,
	 * as it will pick any such pot (rather than a specific one decided upfront).
	 */
	GoalStructure grabPot(TestAgent agent, EntityType potionType) { 
		return DEPLOY(agent,
		  (MyAgentState S) -> {
			  var potsInVicinity = TacticLib.nearItems(S,potionType,5) ;
			  if (potsInVicinity.size() == 0) {
			      return FAIL() ;
			  }
			  var pot = potsInVicinity.get(0) ;
			  //System.out.println("===== deploy grab " + pot.id) ;
			  return SEQ(entityInCloseRange(pot.id),
					     entityInteracted(pot.id)) ;
		   }
	    ) ;
	}
	
	
	GoalStructure checkIfEntityIsInCloseRange(String targetId) {
		
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
	 * potion along the way, if the bag is empty. 
	 */
	public GoalStructure smartEntityInCloseRange(
				TestAgent agent, 
			 	String targetId) { 
		
	
	   var G = INTERRUPTIBLE(
			       ((PrimitiveGoal) entityInCloseRange(targetId)).getGoal(),
			       HANDLE(whenToGoAfterHealPot, grabPot(agent, EntityType.HEALPOT))
			       //HANDLE(whenToGoAfterRagePot, grabPot(agent, EntityType.RAGEPOT))
			    ) ;
	   // Add an extra checking because an interruptible goal-struct always succeeds:
	   return SEQ(G, checkIfEntityIsInCloseRange(targetId)) ;
	}
	
	/**
	 * This goal causes the agent to interact with a given entity. It requires the agent
	 * to be standing next to the entity.
	 */
	@Override
	public GoalStructure entityInteracted(String targetId) {
		
		// when the target is a scroll, and when the bag is full, this action
		// will use a heal or rage pot to create space. This action
		// always return a null proposal, as it is not meant to solve
		// the main-goal:
		Action useHealOrRagePot = action("use heal- or ragepot").do1(
				  (MyAgentState S) -> {
					  var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
					  boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
					  boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;
					  if(hasRagePot)	
						   tacticLib.useRagePotAction().exec1(S) ;
					  else tacticLib.useHealingPotAction().exec1(S) ;
					  return null ; 
				  })
				.on_((MyAgentState S) -> { 
					WorldEntity e = S.worldmodel.getElement(targetId) ;
					if (e==null || ! e.type.equals(EntityType.SCROLL.toString())) {
						return false ;
					}
					var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
					boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
					boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;
					int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
					int maxBagSize = (int) player.properties.get("maxBagSize") ;
					int freeSpace = maxBagSize - bagSpaceUsed ;
					return (hasHealPot || hasRagePot) && freeSpace==0  ;	
				}) ;

		var G = goal("Entity " + targetId + " is interacted.") 
				.toSolve(proposal -> true)
				.withTactic(
				   FIRSTof(
					  useHealOrRagePot.lift(),
					  tacticLib.interactTac(targetId),
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
						tacticLib.useHealingPotAction()
						   .on_(tacticLib.hasHealPot_and_HpLow)
						   .lift()
						,
						tacticLib.useRagePotAction()
						   .on_(tacticLib.hasRagePot_and_inCombat)
						   .lift()
						,
						tacticLib.attackMonsterAction()
						   .on_(tacticLib.inCombat_and_hpNotCritical)
						   .lift()
						,
						tacticLib.explore(heuristicLocation),
						ABORT()))
				.lift()
				.maxbudget(budget)
				;
		
		return explr ;
	}
	
	public GoalStructure smartExploring(TestAgent agent, 
			Pair<Integer, Tile> heuristicLocation, 
			int budget) {
		
		Goal exploreG = ((PrimitiveGoal) exploring(heuristicLocation,budget)).getGoal();
		
		Tactic originalExploreTac = exploreG.getTactic() ;
		
		Tactic abortToGetAHealPot = Abort().on_(whenToGoAfterHealPot).lift() ;
		Tactic abortToGetARagePot = Abort().on_(whenToGoAfterRagePot).lift() ;
		
		Tactic newExploreTac = FIRSTof(
				  abortToGetAHealPot, 
				  abortToGetARagePot,
				  originalExploreTac
				) ;

		return FIRSTof(
				  exploreG.withTactic(newExploreTac).lift(),
				  SEQ(grabPot(agent,EntityType.HEALPOT),FAIL()),
				  SEQ(grabPot(agent,EntityType.RAGEPOT),FAIL())	
				) ;
	}

}
