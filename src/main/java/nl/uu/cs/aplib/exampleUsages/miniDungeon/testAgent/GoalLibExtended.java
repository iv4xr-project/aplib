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

import org.junit.internal.runners.statements.Fail;


import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.mainConcepts.Action.Abort;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.utils.Pair;
import onlineTestCaseGenerator.Sa1Solver.Policy;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;


public class GoalLibExtended  extends GoalLib{

	private static final TestAgent TestAgent = null;
	private static final boolean WorldEntity = false;
	public static TacticLib tacticLib = new TacticLib() ;
	public static GoalLib goalLib = new GoalLib() ;
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
	public static GoalStructure pickUpItem(MyAgentStateExtended S, Pair p) {	
		System.out.println("Pick Up item ! " + S.selectedItem);		
		// the target might be already in the bag, in this case it should return true always
		String targetIDOrType = p.snd.toString();
		
		if(S.worldmodel != null) {
					List<WorldEntity> candidates = S.worldmodel.elements.values().stream()
					.filter(e ->	
					e.id.contains(targetIDOrType)				
							)
					.collect(Collectors.toList()) ;
					System.out.println("Pick Up item candidate! " + candidates.toString());	
			if(candidates.size() > 0) return SUCCESS();
		}
		
		
		//if it is not picked up before, add it to the bag
			return entityInteractedNew();
		
	}
	
	
	
	public static boolean resetSelectedItem(MyAgentStateExtended S) {
		System.out.println("reset things" + S.selectedItemTempt 
				 + S.selectedItem + S.selectedItem.getBooleanProperty("used"));
	
		if(S.selectedItemTempt.equals(S.selectedItem)  && S.selectedItemTempt.getBooleanProperty("used")) {
			return false;
		}else {
			S.selectedItem = S.selectedItemTempt;
			System.out.println("reset the item!!");
			return true;
		}
		
	}
	
	/**
	 * This method decide to use or not to use the selected item
	 * If the selected item is selected to be used then based on the type of item it be used
	 */
	public static GoalStructure useItem(MyAgentStateExtended S) {
		if(S.worldmodel != null) {
			var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
			System.out.println("useItem" + "current" + a.properties.get("itemsInBag").toString());
			
		}
		String targetId = S.selectedItem != null ? S.selectedItem.id : "empty"; 
		System.out.println("Use item, get the selected item! " + targetId );
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
			 		scrollInteracted(S),
			 		useHealOrRagePot(S)
			 	);
	}
	
	/**
	 * This goal structure uses a healing or rage pot
	 */
	public static GoalStructure useHealOrRagePot(MyAgentStateExtended S) {
				
		var useHealOrRagePot =  goal("use pots").toSolve((WorldEntity e)-> {
			System.out.println("grabed pot is used! " + " current HP: " + e.getIntProperty("hp"));
			return true;
		}).withTactic(FIRSTof(TacticLibExtended.useHealOrRagePot(), ABORT())).lift();
		
		return useHealOrRagePot ;
	}
	
	
	/**
	 * This goal structure uses a healing or rage pot
	 */
	public static GoalStructure useHeal(MyAgentStateExtended S) {
		
		return  goal("use healing pot").toSolve((WorldEntity e)-> {			
			return true;
		}).withTactic(FIRSTof(TacticLibExtended.useHeal(), ABORT())).lift();
		

	}
	
	/**
	 * Check the type of the item
	 * @return
	 */
	public static boolean checkType(MyAgentStateExtended S) {
		
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
	public static GoalStructure scrollInteracted (MyAgentStateExtended S) {
		
		//if the selected item is a scroll, to use it, the agent should first find a blocker/shrine to apply it
		//explore till finding a shrine
		//exploration with some heuristic
		//System.out.println("scroll interacted! ");	
	
		//check if the shrine is already in the bag
		Action checkShrine = action("checking shrine").do1((MyAgentStateExtended A) -> {			
			if(A.worldmodel != null) {
				List<WorldEntity> candidates = A.worldmodel.elements.values().stream()
				.filter(e ->	
				e.type.contains("SHRINE")		
						)
				.collect(Collectors.toList()) ;	
				System.out.println("it checks the seen shrine!!" + candidates.size());
				candidates.forEach(e -> System.out.print("shrine candidiates which are seen! " + e));
				if(candidates.size() > 0) {
					candidates = candidates.stream().filter(e-> !(boolean) e.getProperty("cleansed")).collect(Collectors.toList());
					candidates.forEach(e -> System.out.print("shrine candidiates which are not cleansed! " + e.id));
					if(!candidates.isEmpty()) {
						A.selectedItem = candidates.get(0); System.out.println("uncleansed shrine!!" + candidates.get(0).id);
						return true;	
						}
				}		
			}			
			return false;
			});
				
		var goal = goal("check the entities to find the shrine").toSolve((Boolean e) -> {			
			return e;
		}).withTactic(SEQ(checkShrine.lift(),ABORT()));
		
		
		
		//mark the selected scroll as used
		
		
		var gMark = lift("Marked the selected scroll",(MyAgentStateExtended AS) -> {
			if(!AS.triedItems.isEmpty()) {
				WorldEntity markAsUsed = AS.triedItems.stream().filter(j -> j.id.equals(S.selectedItem.id)).findAny().get();
				System.out.println("==Marked the selected scroll" + markAsUsed.id  );
				markAsUsed.properties.put("used", true);
			}
			return true;
		});	
		
		
		
		var g = SEQ( 
				gMark,
				FIRSTof(
				   goal.lift(), //Firstly check if the shrine is seen
				
				  WHILEDO( // if not find the shrine 
						  (MyAgentState x) -> GoalLibExtended.checkExplore(x), 
						  SEQ(exploreTill(), findItem("SHRINE"))
				  )
				),
				//apply the selected scroll, to apply we need to navigate to the shrine 
				smartEntityInCloseRange(S),
				entityInteractedNew(),
				FAIL()				
				)
				;
		
		return g;
	}
	


GoalStructure checkIfEntityIsInCloseRange2(String targetId) {
		
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
	 * Check the agent state to see if new items have been seen 
	 * @param <State>
	 * @return
	 */
	public static <State> GoalStructure newItemsFound() {
		 
		Goal goal = goal("observe new items").toSolve((List<WorldEntity> e)-> {
			System.out.println(">>new Items Found:: ");
			if (e.isEmpty()) {
				System.out.println("There is no new entity/neighbore");
				return false;
			}
			
			return true;
		}).withTactic(SEQ(TacticLibExtended.lookForItem(), ABORT()));
		return goal.lift();
	}
	
	/**
	 * Explore the game world till some items are found
	 * @return
	 */
	public static GoalStructure findNodes(MyAgentStateExtended S) {
		System.out.println(">>>>>>Explore the game world to find new neighbors");
		//return SEQ(exploreTill(),newItemsFound());
		
	return	IF(
			//this is only to use heal pot
			   (MyAgentStateExtended x) -> GoalLibExtended.survivalCheck(x), // if we need the pots
			  IF(
					 (MyAgentStateExtended x) -> findItemPredicate(S, EntityType.HEALPOT.toString()),
					 SEQ( survivalHeuristic(S, EntityType.HEALPOT)  , exploreTill(),newItemsFound()),
					 SEQ(exploreTill(),newItemsFound())
					  ) ,
			  SEQ(exploreTill(),newItemsFound())
		  );
		
	}
	
	/**
	 * check the newly observed items to see if the given item is found
	 * @param <State>
	 * @param S
	 * @return
	 */
	public static <State> GoalStructure findItem(String S) {
		Goal goal = goal("Update the neighbrs graph").toSolve((List<WorldEntity> e) -> {
			if (e == null || e.isEmpty()) {
				System.out.println("There is no new entity/neighbore");
				return false;
			} 	
			System.out.println(">> looking item is found: ");				
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
		var tacticLib = new TacticLib();
		return tacticLib.explorationExhausted(S);
	}
	
	
	/**
	 * Select an item to navigate forward and explore the world as much as possible
	 * @param p
	 * @return
	 */
	public static GoalStructure selectItemToNavigate(Pair p) {
		
		Goal goal = goal("Select item to navigate").toSolve((Boolean e) -> {
			System.out.println("select item is done!!!!!!!");
			if (!e ) {
				System.out.println("No item is selected");
				return false;
			} 
			return true;
		}).withTactic(SEQ(TacticLibExtended.selectItemToNavigate(p), ABORT()));
		
		return goal.lift();
	}
	
	/**
	 * Check the tried items to see if teh target item was tried before but not used to be selected
	 * @param p
	 * @return
	 */
	public static GoalStructure selectTargetedItem(Pair p, Pair additionalFeature) {
		
		Goal goal = goal("Select targeted item").toSolve((Boolean e) -> {
			if (!e ) {
				System.out.println("No item is selected");
				return false;
			} 
			System.out.println("select item is done");
			return true;
		}).withTactic(SEQ(TacticLibExtended.selectTargetedItem(p, additionalFeature), ABORT()));
		
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
							  var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
							  boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
							  boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;
							  boolean hasScroll = (int) player.properties.get("scrollsInBag") > 0 ;
							  if(hasRagePot)	
								   tacticLib.useRagePotAction().exec1(S) ;
							  else if(hasRagePot) 
							  tacticLib.useHealingPotAction().exec1(S) ;  
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
							//System.out.println("use Heal Or Rage Pot" + hasHealPot +  hasRagePot + hasScroll +  freeSpace + maxBagSize + bagSpaceUsed);
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
		
		System.out.println(">> Scheck Explore: " + path );
		if(path != null) {System.out.println("path is not null: " + path ); return true; }
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
			  var clean = shrine.stream().filter(e -> !(boolean) e.properties.get("cleansed") ).findAny();
			  System.out.println("cleanss" + clean);
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
	
	public static boolean checkTarget(MyAgentStateExtended S,Pair p, Pair additionalFeature){
		System.out.println("Check if the target is selected!");
		WorldEntity selectedItem = S.selectedItem;
		String targetIDOrType = p.snd.toString();
		
		//check if it is currently selected
		if(p.fst.toString().contains("id")) 
		{
			if(selectedItem.id.contains(targetIDOrType)) {
				System.out.println("targeted Id is in selected item!" + targetIDOrType + selectedItem.id);
				return true;
			}			
		}
		else if(p.fst.toString().contains("type")) {		
			if(selectedItem.type.contains(targetIDOrType)) {
				//it is based on the type: there might be more than one 
				if(additionalFeature != null) {	
				    var currentItem =  selectedItem.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd);				    				    
				    System.out.println("selected item matches the additional feature!" + currentItem);					    
					if(!currentItem) return false;
					return true;
				}

				System.out.println("targeted type is in selected item!" + selectedItem.id + targetIDOrType);
				return true;
				}			
		}		
		//check if it is in the bag. This happens if we have id 
		var player = S.worldmodel.elements.get(S.worldmodel.agentId);
     	var bagItems = player.getPreviousState().properties.get("itemsInBag").toString().contains(targetIDOrType);
     	if(bagItems && selectedItem.equals(targetIDOrType)) {
     			System.out.println("target is in the bag!" + player.getPreviousState().properties.get("itemsInBag").toString());
     		return true;
     		}
     	return false;
	}
	
	
	public static boolean checkTargetInTriedItem(MyAgentStateExtended S,Pair p, Pair additionalFeature){
		
		System.out.println("Check if the target is in Tried Item!");		
		String targetIDOrType = p.snd.toString();
		
		//check if it is currently selected
		if(p.fst.toString().contains("id")) 
		{
			if(S.triedItems.contains(targetIDOrType)) {
				System.out.println("targeted Id is in selected item!" );
				return true;
			}			
		}
		else if(p.fst.toString().contains("type")) {
			
			boolean currentItem = false;
			if(S.triedItems != null) {
			for( WorldEntity item: S.triedItems ) {
				if(item.type.contains(targetIDOrType)) {
					//System.out.println("item : " + item.id + item.getStringProperty(additionalFeature.fst.toString()) + item.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd));
					//it is based on the type: there might be more than one 
					if(additionalFeature != null) {	
						var additional = item.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd);
						var used = (boolean) item.properties.get("used");
					    if(additional && !used) {			    				    
						    System.out.println("selected item matches the additional feature!" );					    
							currentItem = true; 
						}
					}
				}
			}
			
			if(!currentItem) return false;
			return true;
		}		}

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
							   tacticLib.useHealingPotAction()
							   	  .on_(tacticLib.hasHealPot_and_HpLow)
							   	  .lift()
							   ,
							   TacticLibExtended.navigateToTac(),
							   TacticLibExtended.newExplore(),
					   		   ABORT()) 
					  )
					;
			
			return G.lift() ;		
		}
	
	
	/**
	 * smart entity with survival heuristic
	 * @param b
	 * @return
	 */
	public static GoalStructure smartEntityInCloseRange(MyAgentStateExtended b  ,TestAgent agent, 
		 	String targetId) {
		var G = goal("Entity is touched. smartEntityInCloseRange") 
				.toSolve((Pair<MyAgentStateExtended,WorldModel> proposal) -> {	
					var S = proposal.fst ;
					
					//System.out.println(">>> checking goal" +targetId ) ;
					WorldModel previouswom = S.worldmodel ;
					WorldModel newObs = proposal.snd ;
					WorldEntity e = previouswom.getElement(targetId) ;						
					if (e==null) {
						return false ;
					}
			
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					var solved =  mazeId(a) == mazeId(e) && adjacent(toTile(newObs.position),toTile(e.position)) ;
					
					return solved; 
				})
				.withTactic(
				   FIRSTof(						 
						   tacticLib.attackMonsterAction()
						      .on_(tacticLib.inCombat_and_hpNotCritical)
						      .lift(),
						      tacticLib.navigateToTac(targetId),
							  tacticLib.explore(null),
				   		   ABORT()) 
				  ).lift()
				;
		
		return SEQ(
				IF(
						//this is only to use heal pot
				  (MyAgentStateExtended x) -> GoalLibExtended.survivalCheck(x), // if we need the pots
				  FIRSTof(survivalHeuristic(b, EntityType.HEALPOT),survivalHeuristic(b, EntityType.RAGEPOT) ),
				  SUCCESS()
				  ),
				
				G
				)
				;		
	}
	
	
	
	/**
	 * smart entity with survival heuristic
	 * @param b
	 * @return
	 */
	public static GoalStructure smartEntityInCloseRange(MyAgentStateExtended b ) {
		var G = goal("Entity is touched. smartEntityInCloseRange without id") 
				.toSolve((Pair<MyAgentStateExtended,WorldModel> proposal) -> {	
					var S = proposal.fst ;
					String targetId = S.selectedItem.id;
					System.out.println(">>> checking goal :: "  + targetId) ;
					WorldModel previouswom = S.worldmodel ;
					WorldModel newObs = proposal.snd ;
					WorldEntity e = previouswom.getElement(targetId) ;						
					if (e==null) {
						return false ;
					}
			
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;					
					var solved =  mazeId(a) == mazeId(e) && adjacent(toTile(newObs.position),toTile(e.position)) ;
					
					return solved; 
				})
				.withTactic(
				   FIRSTof(						 
						   tacticLib.attackMonsterAction()
						      .on_(tacticLib.inCombat_and_hpNotCritical)
						      .lift(),
							   TacticLibExtended.navigateToTac(),
							   TacticLibExtended.newExplore(),
				   		   ABORT()) 
				  ).lift()
				;
		
		return SEQ(
				IF(		
				  (MyAgentStateExtended x) -> GoalLibExtended.survivalCheck(x), // if we need the pots
				  SEQ(FIRSTof(survivalHeuristic(b, EntityType.HEALPOT),survivalHeuristic(b, EntityType.RAGEPOT) ) , lift((MyAgentStateExtended a) -> GoalLibExtended.resetSelectedItem(a))),
				  SUCCESS()
				  ),
				
				G
				)
				;		
	}
	
	
	
	/**
	 * A smarter version of entityInCloseRange(e) goal, that will also pick up a nearby
	 * potion along the way, if the bag is empty. 
	 */
	public static GoalStructure smartEntityInCloseRange(
			TestAgent agent,MyAgentStateExtended A) {
		
		String targetId = A.selectedItem != null ? A.selectedItem.id : "empty"; 
		System.out.print("smart entityt new neew : " + targetId);
		 var G1 = ((PrimitiveGoal) entityInCloseRange(A)) ;
		 var originalTactic = G1.getGoal().getTactic();
		 
		 var grabHealPot = action("Push goal grab healpot")
				 .do1((MyAgentState S) -> { 
					 agent.pushGoal(grabPotNew(agent, EntityType.HEALPOT));
					 return null ; })
				 .on_(whenToGoAfterHealPotNew)
				 .lift() ;
		 

		 
		 return G1
		   .getGoal()
		   .withTactic(
			   FIRSTof(
				 grabHealPot,
				 originalTactic
				 ))
		   .lift() ;
	}
	
	
	
	/**
	 * A goal to send the agent to pick up a heal or rage pot nearby. It is a dynamic goal,
	 * as it will pick any such pot (rather than a specific one decided upfront).
	 */
	public static GoalStructure grabPotNew(TestAgent agent, EntityType potionType) { 
		return DEPLOY(agent,
		  (MyAgentStateExtended S) -> {
			  var potsInVicinity = TacticLib.nearItems(S,potionType,5) ;
			  if (potsInVicinity.size() == 0) {
			      return FAIL() ;
			  }
			  		 
			  var candidates = potsInVicinity.stream().filter(element -> !S.triedItems.contains(element) && element != S.selectedItem).collect(Collectors.toList());	  
			  var pot = candidates.get(0) ;	
			  System.out.println("===== deploy grab " + pot.id) ;
			  pot.properties.put("used", true);
			  S.triedItems.add(pot);
			  System.out.println("===== deploy grab " + pot.id + pot.properties.get("used")) ;
			  return SEQ(goalLib.entityInCloseRange(pot.id),
					  goalLib.entityInteracted(pot.id)) ;
		   }
	    ) ;
	}
	
	
	
	public static Predicate<MyAgentStateExtended> whenToGoAfterHealPotNew = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
		int maxBagSize = (int) player.properties.get("maxBagSize") ;
		var healPotsInVicinity = TacticLib.nearItems(S,EntityType.HEALPOT,4) ;
		healPotsInVicinity = healPotsInVicinity.stream().filter(element -> !S.triedItems.contains(element) && element != S.selectedItem).collect(Collectors.toList());	  
		return S.agentIsAlive() 
				&& maxBagSize-bagSpaceUsed >= 1 
				&& healPotsInVicinity.size() > 0 ;
	} ;
	
	
	
	/**
	 * This predicate checks if the agent is in the condition that needs to use survival heuristics
	 * The conditions are, if the agent HP is low and there exist a 
	 */
	public static boolean survivalCheck(MyAgentStateExtended S) {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		System.out.print("survaival check" + hp);
		List<WorldEntity> candidates = S.worldmodel.elements.values().stream()
				.filter(e ->
				(e.type.equals(""+EntityType.HEALPOT)
						|| e.type.equals("" + EntityType.RAGEPOT)
				))
				.collect(Collectors.toList()) ;
		
		if((hp>=0 && hp<15) && !candidates.isEmpty() ) {
			System.out.println("agent healt is less than 15, survival is activated. Health is: " + hp);
			//before setting the new selected item, if there is an item which is already selected, we put it in selectTemp
			if(S.selectedItem != null) { S.selectedItemTempt = S.selectedItem; System.out.println("replace with temp");}
			return true;
		}			
		return false;
	}
	
	/**
	 * Use the survival heuristic based on the situation
	 * check if we already have the items in bag, if not firstly select them
	 */
	
	public static GoalStructure survivalHeuristic(MyAgentStateExtended S, EntityType entity) {	
		
		// if the bag is full, with the scrolls means that it can not add any other pot
		// firstly make a space for that
		// if it is full but there is a pot in the bag that is not a problem. 
		return 
				SEQ(
//					IF((MyAgentStateExtended b) ->checkBagSize(S, EntityType.SCROLL.toString()),
//							scrollInteracted(S),
//							SUCCESS()),
		
					FIRSTof(
					//firstly check if there exist a heal or rage pot in the bag to use
					IF((MyAgentStateExtended b) ->checkBag(S,entity.toString()),
							useHeal(S),
							FAIL()
							),
					IF(
							(MyAgentStateExtended b) ->checkSelectItem(S,entity.toString()),//if it is selected but not in the bag
							SEQ(entityInCloseRange(S), entityInteractedNew(),useHealOrRagePot(S)),
							
							IF(
									(MyAgentStateExtended b) -> findItemPredicate(b, EntityType.HEALPOT.toString()),
									SEQ(
										findItem(EntityType.HEALPOT.toString()),entityInCloseRange(S),entityInteractedNew(),useHealOrRagePot(S)
									),
									SUCCESS()
//									SEQ(
//										findItem(EntityType.RAGEPOT.toString()),entityInCloseRange(S),entityInteractedNew(),useHealOrRagePot(S)
//									)		
							)
					)
				)
				);
	}
	
	
	
	/**
	 * Check if there exist a heal or rage pot in the bag
	 * @param S
	 * @return
	 */
	public static boolean checkBag(MyAgentStateExtended S, String entityType) {
		System.out.println("Check bag!"); 
		var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
		//var itemIsInBag = S.selectedItem.type.equals(EntityType);
		LinkedList<String> itemsInBag = (LinkedList<String>) a.properties.get("itemsInBag");
		boolean itemIsInBag = false;
		System.out.println("Item in the bag!" + a.properties.get("itemsInBag"));
		if(itemsInBag == null) return false;
		for(String item: itemsInBag) {
			//two pots can be use as a survival, 
			List<WorldEntity> itemFound = S.triedItems.stream().filter(e -> e.id.equals(item) && (e.type.equals(entityType) || e.type.equals(EntityType.RAGEPOT.toString()))).collect(Collectors.toList());			
			if(!itemFound.isEmpty() )
				itemIsInBag = true;
		}
		System.out.println("Item in the bag!" + itemIsInBag + S.selectedItem);
		
		if(itemIsInBag) return true;	
		return false;		
	}
	
	/**
	 * Check if a heal or rage pot is already selected to use
	 * @param S
	 * @return
	 */
	public static boolean checkSelectItem(MyAgentStateExtended S, String EntityType) {
		System.out.println("Check selected Item!"); 
		var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
		if(S.selectedItem == null) return false;
		int agentMaze = (int) a.properties.get("maze");
		var itemSelected = S.selectedItem.type.equals(EntityType);
		var notUsed = S.selectedItem.getBooleanProperty("used");
		var sameMaze = S.selectedItem.properties.get("maze").equals(agentMaze);
		System.out.println("Item is selected!" + itemSelected + S.selectedItem + agentMaze + sameMaze);
		
		if(itemSelected && sameMaze && !notUsed) return true;	
		return false;		
	}
	
	
	/**
	 * Check if specific type of item or id exist
	 * @param S
	 * @return
	 */
	public static boolean findItemPredicate(MyAgentStateExtended S, String item) {
		System.out.println("Find item predicate!"); 
		var newObserved = S.worldmodel.elements;
    	
    	
    	List<WorldEntity> candidates = newObserved.values().stream()
		.filter(e ->
		e.id.contains(item)
		||
		e.type.contains(item)
				)
		.collect(Collectors.toList()) ;
    	
		
    	
		if(candidates.size() > 0 ) return true;	
		//if(candidates.size() < 0 && !checkExplore(S)) return false;
		return false;		
	}
	
	/**
	 * Check if the bag size is full and it is filled by scrolls
	 * @param S
	 * @return
	 */
	public static boolean checkBagSize(MyAgentStateExtended S, String EntityType) {
		System.out.println("Check bag size!"); 
		var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
		LinkedList<String> itemsInBag = (LinkedList<String>) a.properties.get("itemsInBag");
		boolean itemIsInBag = false;
		int maxBagSize = (int) a.properties.get("maxBagSize") ;
        int bagSpaceUsed = itemsInBag.size() ; // only the scrolls
		int freeSpace = maxBagSize - bagSpaceUsed ;
		System.out.println("Item in the bag!" + a.properties.get("itemsInBag") + freeSpace);
		if(freeSpace == 0) return true;
		
		return false;		
	}
	

}
