package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import static nl.uu.cs.aplib.AplibEDSL.ABORT;
import static nl.uu.cs.aplib.AplibEDSL.DEPLOY;
import static nl.uu.cs.aplib.AplibEDSL.FAIL;
import static nl.uu.cs.aplib.AplibEDSL.FIRSTof;
import static nl.uu.cs.aplib.AplibEDSL.IF;
import static nl.uu.cs.aplib.AplibEDSL.REPEAT;
import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static nl.uu.cs.aplib.AplibEDSL.SUCCESS;
import static nl.uu.cs.aplib.AplibEDSL.WHILE;
import static nl.uu.cs.aplib.AplibEDSL.WHILEDO;
import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;
import static nl.uu.cs.aplib.AplibEDSL.lift;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.adjacent;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.mazeId;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.toTile;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.mainConcepts.Action.Abort;
import nl.uu.cs.aplib.utils.Pair;


import java.util.HashMap;
import java.util.HashSet;


public class GoalLibExtended  extends GoalLib{

	public static TacticLib tacticLib = new TacticLib() ;

	/**
	 * Explore the environment when there is no entity in the agent visibility
	 * range, until it sees a new entity
	 */
	public static  GoalStructure exploreTill() {
		var g = goal("explore").toSolve(belief -> false).withTactic(FIRSTof(TacticLibExtended.newExplore(), ABORT())).lift();
		// increasing the budget to make the agent movement smoother
		g.maxbudget(8);
		return FIRSTof( g, SUCCESS());
	}
	
	/**
	 * pick up the item and put it into bag
	 */
	public static GoalStructure pickUpItem(MyAgentState S, Pair p) {	
		System.out.println("Pick Up item ! ");		
		// the target might be already in the bag, in this case it should return true always
		String targetIDOrType = p.snd.toString();
		
		if(S.worldmodel != null) {
					List<WorldEntity> candidates = S.worldmodel.elements.values().stream()
					.filter(e ->	
					e.id.contains(targetIDOrType)				
							)
					.collect(Collectors.toList()) ;
			if(candidates.size() > 0) return SUCCESS();
		}
		
		
		//if it is not picked up before, add it to the bag
			return entityInteractedNew();
		
	}
	
	
	/**
	 * This method decide to use or not to use the selected item
	 * If the selected item is selected to be used then based on the type of item it be used
	 */
	public static GoalStructure useItem(MyAgentStateExtended S) {	
		String targetId = S.selectedItem != null ? S.selectedItem.id : "empty"; 
		System.out.println("Use item, get the selected item! " + targetId);
			// this is only for pot
			return SEQ(lift((Iv4xrAgentState b) -> useHeiuristic()),entityUsed(S,targetId));
		
	}
	
	
	/**
	 * based on the type of the entity, it will decide how to use it
	 */
	public static GoalStructure entityUsed(MyAgentStateExtended S,String targetId){
		//firstly it checks if the selected item is a pot, if not it is a scroll
		System.out.println("use the picked up item! ");	
	
		return  IF(
			 		(MyAgentStateExtended x) ->  GoalLibExtended.checkType(x),				 	
			 		scrollInteracted(S,targetId),
			 		useHealOrRagePot()
			 	);
	}
	
	/**
	 * This goal structure uses a healing or rage pot
	 */
	public static GoalStructure useHealOrRagePot() {
		return goal("use pots").toSolve((WorldEntity e)-> {
			System.out.println("Heal or Rage pot is used");
			return false;
		}).withTactic(FIRSTof(TacticLibExtended.useHealOrRagePot(), ABORT())).lift();
		
	}
	
	/**
	 * Check the type of the item
	 * @return
	 */
	public static boolean checkType(MyAgentStateExtended S) {
		System.out.print("tets" + S.selectedItem.id);
		if(S.selectedItem.type.contains(EntityType.SCROLL.toString())) {
			System.out.print("***If the selected item is a scroll!");
			return true;
		}
			
		return false;
	}
	
	
	/** Predicate to check the selected node is visible */
	public static Boolean useHeiuristic() {
		System.out.println("use heuristic to decide to use the selected item or not! ");				
		
				// Randomly decide to use or not to use
//				Random random = new Random();
//				var x  = random.nextBoolean();
//				System.out.println("***use heuristic: randomly decide! " + x);
//			    return  x;
			    
			  
			    
			    // Immediately use the item, always returns true
			     return true;
			    
			    // Return based on the type; e.g if it is a scroll use it immediately
//			    if (e.type.equals("" + EntityType.SCROLL)) {
//	    	   		return true; 
//	    	   	}
//	    	   	if (e.type.equals("" + EntityType.HEALPOT)) {	    	   	
//	    	   		return true;
//	    	   	}
		
		
	}
	
	
	/**
	 * Interact with the scroll/key is different than other items that can individually used
	 * If the selected item is a scroll, to use the item the agent should find a shrine to cleanse it.
	 * @return
	 */
	public static GoalStructure scrollInteracted (MyAgentStateExtended S, String eId) {
		
		//if the selected item is a scroll, to use it, the agent should first find a blocker/shrine to apply it
		//explore till finding a shrine
		//exploration with some heuristic
		System.out.println("scroll interacted! ");	
	
		//check if the shrine is already in the bag

		Action checkShrine = action("checking shrine").do1((MyAgentStateExtended A) -> {			
			if(A.worldmodel != null) {
				List<WorldEntity> candidates = A.worldmodel.elements.values().stream()
				.filter(e ->	
				e.type.contains("SHRINE")				
						)
				.collect(Collectors.toList()) ;
				if(candidates.size() > 0) {				
					A.selectedItem = candidates.get(0);
					return true;	
				}		
			}			
			return false;
			});
				
		var goal = goal("check the entities to find the shrine").toSolve((Boolean e) -> {
			return e;
		}).withTactic(SEQ(checkShrine.lift(),ABORT()));
		
		
		var g = SEQ( 
				
				FIRSTof(
				   goal.lift(), //Firstly check if the shine is seen
				
				  WHILEDO( // if not find the shrine 
						  (MyAgentState x) -> GoalLibExtended.checkExplore(x), 
						  SEQ(exploreTill(), findItem("SHRINE"))
				  )
				),
				//apply the selected scroll, to apply we need to navigate to the shrine 
				entityInCloseRange(S),
				entityInteractedNew(),
				FAIL()				
				)
				;
		
		return g;
	}
	

	
	/**
	 * Check the agent state to see if new items have been seen 
	 * @param <State>
	 * @return
	 */
	public static <State> GoalStructure newItemsFound() {
		 
		Goal goal = goal("observe new items").toSolve((List<WorldEntity> e)-> {
			System.out.println("newItemsFound goal structure");
			if (e.isEmpty()) {
				System.out.println("There is no new entity/neighbore");
				return false;
			}
			
			System.out.println("New items found");
			return true;
		}).withTactic(SEQ(TacticLibExtended.lookForItem(), ABORT()));
		return goal.lift();
	}
	
	/**
	 * Explore the game world till some items are found
	 * @return
	 */
	public static GoalStructure findNodes() {
		System.out.println(">>>>>>Explore the game world to find new neighbors");
		return SEQ(exploreTill(),newItemsFound());
	}
	
	/**
	 * check the newly observed items to see if the given item is found
	 * @param <State>
	 * @param S
	 * @return
	 */
	public static <State> GoalStructure findItem(String S) {
		Goal goal = goal("Update the neighbrs graph").toSolve((List<WorldEntity> e) -> {
			System.out.println("itemFound goal structure");
			if (e == null || e.isEmpty()) {
				System.out.println("There is no new entity/neighbore");
				return false;
			} 	
			System.out.println("looking item is found: ");				
			return true;
		}).withTactic(SEQ(TacticLibExtended.lookForSpecificItem(S), ABORT()));
		return goal.lift();
	}
	
	
	/**
	 * 
	 * @param S
	 * @return
	 */
	public static Boolean explorationExhausted(SimpleState S) {
		System.out.println("explorationExhausted goal");
		var tacticLib = new TacticLib();
		return tacticLib.explorationExhausted(S);
	}
	
	public static GoalStructure selectItem(Pair p) {
		
		Goal goal = goal("Select item").toSolve((Boolean e) -> {
			System.out.println("select item is done!!!!!!!");
			if (!e ) {
				System.out.println("No item is selected");
				return false;
			} 
			System.out.println("select item is done");
			return true;
		}).withTactic(SEQ(TacticLibExtended.selectItem(p), ABORT()));
		
		return goal.lift();
	}
	
	/**
	 * This goal causes the agent to interact with a given entity. It requires the agent
	 * to be standing next to the entity.
	 */

	public static GoalStructure entityInteractedNew() {
				
		var G = goal("Entity  is interacted.") 
				.toSolve(proposal -> true)
				.withTactic(
				   FIRSTof(					 
					  TacticLibExtended.interactTacNew(),
					  ABORT()) 
				  )
				;
		
		return G.lift();
	
	}
	
	public static GoalStructure makeBagEmpty(MyAgentStateExtended b) {
		// when the target is a scroll, and when the bag is full, this action
				// will use a heal or rage pot to create space. This action
				// always return a null proposal, as it is not meant to solve
				// the main-goal:
				String targetId = b.selectedItem.id;
		
				//Note: the scroll is also added
				Action useHealOrRagePot = action("use heal- or ragepot").do1(
						  (MyAgentState S) -> {		
							  System.out.println("interact with item");
							  var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
							  boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
							  boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;
							  boolean hasScroll = (int) player.properties.get("scrollsInBag") > 0 ;
							  if(hasRagePot)	
								   tacticLib.useRagePotAction().exec1(S) ;
							  else if(hasRagePot) 
							  tacticLib.useHealingPotAction().exec1(S) ;
							  else scrollInteracted(b,targetId);
							  return null ; 
						  })
						.on_((MyAgentState S) -> { 
							System.out.println("use Heal Or Rage Pot");
							WorldEntity e = S.worldmodel.getElement(targetId) ;
							if (e==null || ! e.type.equals(EntityType.SCROLL.toString())) {
								return false ;
							}
							var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
							boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
							boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;
							boolean hasScroll = (int) player.properties.get("scrollsInBag") > 0 ;
							int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
							int maxBagSize = (int) player.properties.get("maxBagSize") ;
							int freeSpace = maxBagSize - bagSpaceUsed ;				
							System.out.println("use Heal Or Rage Pot" + hasHealPot +  hasRagePot + hasScroll +  freeSpace + maxBagSize + bagSpaceUsed);
							return (hasHealPot || hasRagePot || hasScroll) && freeSpace==0  ;	
						}) ;
				var G = goal("Entity  is interacted.") 
						.toSolve(proposal -> true)
						.withTactic(
						   FIRSTof(
							  useHealOrRagePot.lift(),  
							  ABORT()) 
						  )
						;
				
				return G.lift();
	}
	
	/**
	 * Check if there is unexplored area
	 * @param S
	 * @return
	 */
	public static boolean checkExplore(MyAgentState S) {
		var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
		Tile agentPos = Utils.toTile(S.worldmodel.position) ;
		var path  = S.multiLayerNav.explore(Utils.loc3(Utils.mazeId(a),agentPos.x, agentPos.y));
		
		System.out.println("check Explore: " + path);
		if(path != null) return true;
		return false;		
	}
	
	
	/**
	 * Check if there is unexplored area Or a uncleansed shrine is seen, which means we have multilayer
	 * @param S
	 * @return
	 */
	public static boolean checkMaze(MyAgentStateExtended S) {
		System.out.println("Check maze!"); 
		var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
		var shrine = S.worldmodel.elements.values().stream().filter( s ->
	  	s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
		
		if(!shrine.isEmpty()) {
			  System.out.println("Shrine to cleanse is found!"); 
			  var clean = shrine.stream().filter(e -> !(boolean) e.properties.get("cleansed")).findFirst();
			  if(!clean.isEmpty()) {
				  System.out.println("Shrine is cleansed!" + clean); 
				  return true;
			  }
		}	
		return false;		
	}
	
	
	/**
	 * check if the target is selected, the bag and the current selected item should be checked
	 * @param b
	 * @return
	 */
	
	public static boolean checkTarget(MyAgentStateExtended S,Pair p){
		System.out.println("Check if the target is selected!");
		WorldEntity selectedItem = S.selectedItem;
		String targetIDOrType = p.snd.toString();
		
		//check if it is currently selected
		if(p.fst.toString().contains("id")) 
		{
			if(selectedItem.id.contains(targetIDOrType)) {
			System.out.println("targeted Id is in selected item!" + targetIDOrType + selectedItem.id);
			return true;}			
		}
		else if(p.fst.toString().contains("type")) {
			if(selectedItem.type.contains(targetIDOrType)) {
				System.out.println("targeted type is in selected item!" + selectedItem.id + targetIDOrType);
				return true;}			
		}		
		//check if it is in the bag
		var player = S.worldmodel.elements.get(S.worldmodel.agentId);
     	var bagItems = player.getPreviousState().properties.get("itemsInBag").toString().contains(targetIDOrType);
     	if(bagItems) {
     		System.out.println("target is in the bag!");
     		return true;
     		}
		
     	return false;
	}
	
	public static GoalStructure entityInCloseRange(MyAgentStateExtended b) {
			var G = goal("Entity is touched.") 
					.toSolve((Pair<MyAgentStateExtended,WorldModel> proposal) -> {	
						var S = proposal.fst ;
						String targetId = S.selectedItem.id;
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
					   FIRSTof(
							   TacticLibExtended.navigateToTac(),
							   TacticLibExtended.newExplore(),
					   		   ABORT()) 
					  )
					;
			
			return G.lift() ;		
		}
	
	/**
	 * This predicate checks if the agent is in the condition that needs to use survival heuristics
	 * The conditions ar, if the agent HP is low or he is in combat
	 */
	public static boolean survivalCheck(MyAgentState S) {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		if((hp>0 && hp<=10) || (hp>0 && S.adajcentMonsters().size()>0) ) 
			return true;
		return false;
		
	}
	
	/**
	 * Use the survival heuristic based on the situation
	 * check if we already have the items in bag, if not firstly select them
	 */
	
	public static GoalStructure survivalHeuristic(MyAgentStateExtended S) {	
		return SEQ(findItem("HEALPOT"),
				entityInCloseRange(S),entityInteractedNew(), useHealOrRagePot());
	}
	
	
	
	public static GoalStructure test(MyAgentStateExtended state) {

		var G = goal("test") 
				.toSolve((Boolean proposal) -> {	
					
					System.out.println(">>> checking goal test") ;
					return true; 
				})
				.withTactic(
				   FIRSTof(
						   
						   TacticLibExtended.test(),
				   		   ABORT()) 
				  )
				;
		
		return G.lift() ;	

	}
	


}
