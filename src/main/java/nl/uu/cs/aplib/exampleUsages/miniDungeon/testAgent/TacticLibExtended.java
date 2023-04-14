package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import static nl.uu.cs.aplib.AplibEDSL.action;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib.*;

public class TacticLibExtended extends TacticLib{

	public static TacticLib tacticLib = new TacticLib() ;

	public static Tactic newExplore() {

		List[] memorized = { null } ;
		final int memoryDuration = 10 ;
		int[] memoryCountdown = {0} ;
		
		Action alpha = action("explore")
				.do2((MyAgentStateExtended S) ->  (Tile nextTile) -> {
					WorldModel newwom = tacticLib.moveTo(S,nextTile) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentState S) -> {
					if (!S.agentIsAlive()) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					//System.out.println(">>> agent is " + S.worldmodel().agentId) ;
						
					//System.out.println("### explore is invoked agent @" + agentPos) ;
					
					List<Pair<Integer,Tile>> path = null ;
					if (tacticLib.delayPathReplan) {
						if (memoryCountdown[0] <= 0) {
							memoryCountdown[0] = memoryDuration ;
						}
						else {
							path = memorized[0] ;
							//System.out.println("### about to use memorized path: " + path) ;
							if (path.size()<=1) {
								//System.out.println("### memorized path is singleton or empty, dropping it") ;
								path = null ;
								memoryCountdown[0] = memoryDuration ;
							}
							else {
								path.remove(0) ;
								memoryCountdown[0] -- ;
								Tile next = path.get(0).snd ;
								if(!Utils.adjacent(agentPos,next)) {
									//System.out.println("### next node in memorized path is not adjacent; dropping it") ;
									path = null ;
									memoryCountdown[0] = memoryDuration ;
								}
							}
							//if (path!=null) System.out.println("### using memorized path->" + path.get(0)) ;
							
						}
					}
					if (path == null) {
													//System.out.println(">>> @maze " + Utils.mazeId(a) + ", tile: " + agentPos) ;
						    path = S.multiLayerNav.explore(Utils.loc3(Utils.mazeId(a),agentPos.x, agentPos.y)) ;
						
						
						if (path == null || path.isEmpty()) {
							//System.out.println(">>>> can't find an explore path!") ;
							return null ;
						}
						path.remove(0) ;
						memorized[0] = path ;
						//System.out.println("### calculated new path-> " + path.get(0)) ;
					}
					
					try {
						return path.get(0).snd ;
					}
					catch(Exception e) {
						System.out.println(">>> agent @" + agentPos + ", path: " + path) ;
						throw e ;
					}
					//return path.get(1).snd ;
				}) 
				;
		return alpha.lift() ;
		
	}
	
	
	/**
	 * Searching for specific item based on the Id or Type
	 * @param item
	 * @return
	 */
	public static Tactic lookForItem() {
		
		return action("update high level gragh with new visited neighbors/nodes")
                . do1((MyAgentStateExtended S)-> {
                    //State update   
                	//printing entities in the same time stamp
                	//get new observation
                	var newObserved = S.worldmodel.elements;
                	
                	
                	List<WorldEntity> candidates = S.worldmodel.elements.values().stream()
        					.filter(e ->
        					! S.triedItems.contains(e)
        					&& (e.id.contains("H0_")
        					|| e.id.contains("R0_")
        					|| e.id.contains("S0_")
        					|| e.id.contains("SH0_"))// this should be shrine
        							)
        					.collect(Collectors.toList()) ;
                	
                	//print all observed items
					/*
					 * S.worldmodel.elements.forEach ( (s,e) ->
					 * {System.out.println("All: Seen in the same time stamp " + e.id +
					 * " e timestam: " + e.timestamp + " curenttimestamp : " +
					 * S.worldmodel.timestamp + " id :" + e.id); });
					 */
                		 
                	candidates.forEach (	
                			e -> {System.out.println("Candidates: Seen in the same time stamp " + e.id + " e timestam: " +  e.timestamp + " curenttimestamp : " 
                                    + S.worldmodel.timestamp + " id :" + e.id); 
                                	}); 
                	
                	if(candidates.isEmpty()) {System.out.println("there is no entity in the new observation!! "); }
                	return candidates;
                }).lift();

	}
	
	
	/**
	 * Searching for specific item based on the Id or Type
	 * @param item
	 * @return
	 */
	public static Tactic lookForSpecificItem(String item) {
		
		return action("update high level gragh with new visited neighbors/nodes")
                . do1((MyAgentStateExtended S)-> {
                    //State update   
                	//printing entities in the same time stamp
                	//get new observation               	
                	var newObserved = S.worldmodel.elements;
                	
                	
                	List<WorldEntity> candidates = newObserved.values().stream()
					.filter(e ->
					e.id.contains(item)
					||
					e.type.contains(item)
							)
					.collect(Collectors.toList()) ;
                	
                	//print all observed items
					/*
					 * S.worldmodel.elements.forEach ( (s,e) ->
					 * {System.out.println("All: Seen in the same time stamp " + e.id +
					 * " e timestam: " + e.timestamp + " curenttimestamp : " +
					 * S.worldmodel.timestamp + " id :" + e.id); });
					 */
                	
                	candidates.forEach (	
                			e -> {System.out.println("Candidates: Seen in the same time stamp " + e.id + " e timestam: " +  e.timestamp + " curenttimestamp : " 
                                    + S.worldmodel.timestamp + " id :" + e.id); 
                                	}); 
                	
                	if(candidates.isEmpty()) {
                		System.out.println("there is no entity in the new observation!! "); 
                	}else {
                		S.selectedItem = candidates.get(0);
                		}
                	
                	
                	return candidates;
                }).lift();

	}
	
	public static Tactic selectItem( Pair target) {	
		return action("select item based on the heuristics")
                . do1((MyAgentStateExtended S)-> {
		//select items which is observed and it is not selected before
        //this is repeated as the same as the find item because I do not save them somewhere yet 
			
                	
                	List<WorldEntity> candidates = S.worldmodel.elements.values().stream()
        					.filter(e ->
        					! S.triedItems.contains(e)
        					&& (e.id.contains("H0_")
        					|| e.id.contains("R0_")
        					|| e.id.contains("S0_")
        					|| e.id.contains("SH0_"))// this should be shrine
        							)
        					.collect(Collectors.toList()) ;
            //get the player 
            var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
            //get the max bag size to control the selection
            int maxBagSize = (int) player.properties.get("maxBagSize") ;
            int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
			int freeSpace = maxBagSize - bagSpaceUsed ;
            System.out.println("Player max bag size: " + maxBagSize + "bag space used: " + bagSpaceUsed + "freeSpace: " + freeSpace + "bag size" + player.properties.get("bagSize"));
            System.out.println("get the bag property: " + player.properties.get("itemsInBag").toString());
            //if(player.properties.get("bagSize"))
            
            
            WorldEntity selectedItem = null;
			String selectedItemId ="";
			// Select one item from the list of unvisited items using heuristics
			
			if(!candidates.isEmpty()) {
				
				candidates.forEach(c -> {System.out.println("looking item is find, all candidiates: " + c.id);});
				
				if(target != null) {
					//Select based on the Id
					if(target.fst.equals("id")) {
						//look for the item with the given id
						List<WorldEntity> candidateId = candidates.stream()
								.filter(e -> e.id.contains(target.snd.toString())
										)
								.collect(Collectors.toList()) ;
						if(!candidateId.isEmpty()) {
							selectedItem =  candidateId.get(0);
							selectedItemId = selectedItem.id;
							System.out.println("looking item is found by id: " + selectedItem);
						}
					}
					//Select based on the type
					// If there are more than one item with the same type, select one randomly
					if(target.fst.equals("type")) {
						//look for the items with the given type
						List<WorldEntity> selectedItems = candidates.stream().filter(s -> s.type.matches(target.snd.toString())).collect(Collectors.toList());
						if(!selectedItems.isEmpty()) {
							//always returns the first item if there are more than one, this can
							// improve by selecting the nearest to the agent
							selectedItem  = selectedItems.get(0);
							selectedItemId = selectedItem.id;
							System.out.println("looking item is found by type: " + selectedItemId);						
						}
					}
				
					if(selectedItem != null) { 
						S.selectedItem = selectedItem;
						S.triedItems.add(selectedItemId) ;
						return true;
					}
				}

				// if not:
				// Select nearest to the agent position
				// just choose the closest one
				System.out.println("The item is not found, select nearest item to the agent position! ");
				selectedItem = getClosestsElement(candidates, S.worldmodel.position) ;
				selectedItemId = selectedItem.id;
				System.out.println("Add item to the list of tried items! " + selectedItemId );
				S.selectedItem = selectedItem;
				System.out.println("added to the seleted item: " + S.selectedItem);
				S.triedItems.add(selectedItemId) ;
				if(selectedItem != null) 	return true;			
			}
			// TODO: Select the nearest based on the type of the item
			
			return selectedItem;
                }).lift();
                
	}
	
	private static WorldEntity getClosestsElement(List<WorldEntity> candidates, Vec3 target) {
		WorldEntity closest = candidates.get(0) ;
		float minDistance = Float.MAX_VALUE ;
		System.out.println("get Closests Element: " + "target: " +  target );
		for (var e : candidates) {
			System.out.println("get Closests Element: " + "e: " +  e.position + " id: " + e.id + e.type);
			float distSq = Vec3.distSq(e.position, target) ;
			if (distSq < minDistance) {
				closest = e ;
				minDistance = distSq ;
			}
		}
		System.out.println("Selected closest item: " + closest);
		return closest ;
	}
	
	/**
	 * Construct an action that would guide the agent to a tile adjacent to the target entity.
	 */
	static Action navigateToAction() {
		// memorize path if instructed to, avoid invoking pathfinder every time 
		List[] memorized = { null } ;
		final int memoryDuration = 10 ;
		int[] memoryCountdown = {0} ;
		return action("move-to")
				.do2((MyAgentStateExtended S) ->  (Tile[] nextTile) -> {
					if (nextTile.length == 0) {
						return new Pair<>(S,S.env().observe(S.worldmodel().agentId)) ;
					}
					WorldModel newwom = tacticLib.moveTo(S,nextTile[0]) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentStateExtended S) -> {
					// return three possible values:
					//   (1) null --> the action is not enabled
					//   (2) empty array of tiles --> the agent is already next to the target
					//   (3) a singleton array of tile --> the next tile to move to
					//
					if (!S.agentIsAlive()) {
						return null ;
					}
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					String targetId = S.selectedItem.id;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;					
					if (e == null) {
						return null ;
					}
					
					Tile target = Utils.toTile(e.position) ;
					if (Utils.mazeId(a)==Utils.mazeId(e) && Utils.adjacent(agentPos,target)) {
						Tile[] nextTile = {} ;
						return nextTile ;
					}
					
					System.out.println("###### nav-action " + S.worldmodel.agentId) ;
					List<Pair<Integer,Tile>> path = null ;
					if (tacticLib.delayPathReplan) {
						if (memoryCountdown[0] <= 0) {
							memoryCountdown[0] = memoryDuration ;
						}
						else {
							path = memorized[0] ;
							if (path.size()<=1) {
								//System.out.println("### clear memorized path case 1") ;
								path = null ;
								memoryCountdown[0] = memoryDuration ;
							}
							else {
								path.remove(0) ;
								memoryCountdown[0] -- ;
								Tile next = path.get(0).snd ;
								if(!Utils.adjacent(agentPos,next)) {
									//System.out.println("### clear memorized path case 2") ;
									path = null ;
									memoryCountdown[0] = memoryDuration ;
								}
							}
							//if (path!=null) System.out.println("### using memorized path") ;
						}
					}			
					if (path == null) {
						//System.out.println("### calculating new path") ;
						path = adjustedFindPath(S, Utils.mazeId(a), agentPos.x, agentPos.y, Utils.mazeId(e),target.x, target.y) ;
						if (path == null || path.isEmpty()) {
							return null ;
						}
						path.remove(0) ;
						//System.out.println("### path=" + path) ;
						memorized[0] = path ;
					}
					Tile[] nextTile = {path.get(0).snd} ;
					return nextTile ;
				}) 
				;
	}
	
	
	/**
	 * Construct a tactic that would guide the agent to a tile adjacent to the target entity.
	 */
	public static Tactic navigateToTac() {
		return navigateToAction().lift() ;	
	}
	
	/**
	 * This constructs a "default" tactic to interact with an entity. The tactic is
	 * enabled if the entity is known in the agent's state/wom, and if it is 
	 * adjacent to the agent.
	 */
	public static Tactic interactTacNew() {
		var alpha = action("interact")
				.do2((MyAgentStateExtended S) ->  (Tile nextTile) -> {
					WorldModel newwom = tacticLib.moveTo(S,nextTile) ;
					return new Pair<>(S,newwom) ;
				})
				.on((MyAgentStateExtended S) -> {
					if (!S.agentIsAlive()) return null ;
					var a = S.worldmodel.elements.get(S.worldmodel().agentId) ;
					Tile agentPos = Utils.toTile(S.worldmodel.position) ;
					String targetId = S.selectedItem.id;
					WorldEntity e = S.worldmodel.elements.get(targetId) ;
					System.out.println("item to interact: " +targetId + S.selectedItem);
					
					if (e == null || Utils.mazeId(a)!=Utils.mazeId(e)) {
						return null ;
					}
					Tile target = Utils.toTile(e.position) ;
					if (Utils.adjacent(agentPos,target)) {
						
						var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
						int maxBagSize = (int) player.properties.get("maxBagSize") ;
			            int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
						int freeSpace = maxBagSize - bagSpaceUsed ;
			            System.out.println("Player max bag size: " + maxBagSize + "bag space used: " + bagSpaceUsed + "freeSpace: " + freeSpace + "bag size" + player.properties.get("bagSize"));
						
			            //Change the pickedUp property of the item to know this is picked up
			            e.properties.put("pickedUp", true);
			            
			            return target ;
					}
					return null ;
				}) 
				;
		return alpha.lift() ;
	}
	
	public static Tactic useHealOrRagePot() {
		/*
		 * return action("use selected potion") .do1((MyAgentState S) -> {
		 * System.out.println("use selected pot! "); var player =
		 * S.worldmodel.elements.get(S.worldmodel.agentId) ; boolean hasHealPot = (int)
		 * player.properties.get("healpotsInBag") > 0 ; boolean hasRagePot = (int)
		 * player.properties.get("ragepotsInBag") > 0 ; boolean hasScroll = (int)
		 * player.properties.get("scrollsInBag") > 0 ; if(hasRagePot) //if it is a rage
		 * pot tacticLib.useRagePotAction().exec1(S) ; else //if it is a healing pot
		 * tacticLib.useHealingPotAction().exec1(S) ; return null; }).lift() ;
		 */
		
		Action useHealOrRagePot = action("use heal- or ragepot").do1(
				  (MyAgentStateExtended S) -> {
					  var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
					  boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
					  boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;					  
					  if(hasRagePot)	
						   tacticLib.useRagePotAction().exec1(S) ;
					  else 
					  tacticLib.useHealingPotAction().exec1(S) ;					  
					  S.selectedItem.properties.put("used", true);
					  return player ; 
				  })
				.on_((MyAgentState S) -> { 					
					var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
					boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
					boolean hasRagePot = (int) player.properties.get("ragepotsInBag") > 0 ;
					int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
					int maxBagSize = (int) player.properties.get("maxBagSize") ;
					int freeSpace = maxBagSize - bagSpaceUsed ;
					return (hasHealPot || hasRagePot)  ;	
				}) ;
		return useHealOrRagePot.lift();
	}
	
	/**
	 * Use heal pot if the hp is low and the agent has seen a hp before
	 */
	public Predicate<MyAgentStateExtended> hasHealPot_and_HpLow = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int hp = (int) player.properties.get("hp") ;
		boolean hasHealPot = (int) player.properties.get("healpotsInBag") > 0 ;
		List<WorldEntity> candidatesHealPot = S.worldmodel.elements.values().stream()
				.filter(e ->
				! S.triedItems.contains(e)
				&& e.id.contains("H0_")
						)
				.collect(Collectors.toList()) ;
		if(hp>0 && hp<=10 && !candidatesHealPot.isEmpty() ) {
			//put the hp in the bag, means navigate to it!! I have it somewhere else
		}
		
		return true;
	} ;
	
}
