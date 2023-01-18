package eu.iv4xr.framework.goalsAndTactics;

import static nl.uu.cs.aplib.AplibEDSL.DEPLOY;
import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static nl.uu.cs.aplib.AplibEDSL.SUCCESS;
import static nl.uu.cs.aplib.AplibEDSL.lift;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.BasicAgent;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import static nl.uu.cs.aplib.AplibEDSL.* ;

public class Sa2Solver<NavgraphNode> extends Sa1Solver<NavgraphNode> {
	
	/**
	 * A goal constructor that would cause the agent to explore areas for
	 * some amount of budget. The exploration uses a location, called heuristic
	 * location, to prefer the direction to explore. Areas in the direction of
	 * this location are explored first.
	 * 
	 */
	public BiFunction<Vec3,Integer,GoalStructure> exploringWithHeuristic ;
	
	public Sa2Solver() { super() ; }
	
	public Sa2Solver(BiFunction<Iv4xrAgentState<NavgraphNode> , WorldEntity, Boolean> reachabilityChecker,
			BiFunction<Iv4xrAgentState<NavgraphNode> ,WorldEntity,Float> distanceToAgent,
			Function<Iv4xrAgentState<NavgraphNode> ,BiFunction<WorldEntity,WorldEntity,Float>> distanceFunction,
			Function<String, GoalStructure> gCandidateIsInteracted, 
			Function<String, GoalStructure> gTargetIsRefreshed,
			Predicate<Iv4xrAgentState<NavgraphNode>> explorationExhausted,
			BiFunction<Vec3,Integer,GoalStructure> exploringWithHeuristic) {
		
		super(reachabilityChecker,
				distanceToAgent,
				distanceFunction,
				gCandidateIsInteracted,
				gTargetIsRefreshed,
				explorationExhausted,
				null
				) ;
		this.exploring = budget -> exploringWithHeuristic.apply(null, budget) ;
		this.exploringWithHeuristic = exploringWithHeuristic ;
	}
	
	private WorldEntity getClosestsElement(List<WorldEntity> candidates, Vec3 target) {
		WorldEntity closest = candidates.get(0) ;
		float minDistance = Float.MAX_VALUE ;
		for (var e : candidates) {
			if (Vec3.distSq(e.position, target) < minDistance) {
				closest = e ;
			}
		}
		return closest ;
	}
	
	/**
	 * Choose a candidate to work. This candidate is either the final target entity,
	 * or a closed blocker that needs to be open to allow the agent to explore more
	 * areas (in order to discover where the target entity is).
	 *  
	 * @param S
	 * @param policy
	 * @param visited
	 * @param tId
	 * @param blockersSelector
	 * @param heuristicLocation
	 * @param rnd
	 * @return
	 */
	WorldEntity selectNode(Iv4xrAgentState S,
			Policy policy,
			Set<String>  visited, 
			String tId, 
			Predicate<WorldEntity> blockersSelector,
			Predicate<WorldEntity> isOpen,
			Vec3 heuristicLocation,
			Random rnd) {
		
		WorldEntity target = S.worldmodel.getElement(tId) ;
		if (tId != null) 
			return target ;
		
		List<WorldEntity> candidates = S.worldmodel.elements.values().stream()
				.filter(e -> blockersSelector.test(e)
						&& ! isOpen.test(e)  // only target closed blockers!
						&& ! visited.contains(e.id))
				.collect(Collectors.toList()) ;
				
		if (candidates.isEmpty())
			return null ;
		
		if (policy == Policy.RANDOM) {
			return candidates.get(rnd.nextInt(candidates.size())) ;
		}
		
		Vec3 myHeuristicLocation = heuristicLocation ;
		if (policy == Policy.NEAREST_TO_AGENT || myHeuristicLocation == null) {
			myHeuristicLocation = S.worldmodel.position ;
		}
		
		return getClosestsElement(candidates,myHeuristicLocation) ;
	}
	
	WorldEntity selectEnabler(Iv4xrAgentState S,
			Policy policy,
			Set<String>  visited, 
			WorldEntity targetBlocker, 
			Predicate<WorldEntity> enablersSelector,
			BiFunction<String,Iv4xrAgentState,List<String>> getConnectedEnablersFromBelief,
			BiFunction<String,Iv4xrAgentState,List<String>> getEnablersInSameZoneFromBelief,
			Random rnd) {
		
		List<WorldEntity> candidates = getConnectedEnablersFromBelief.apply(targetBlocker.id, S).stream()
				.map(id -> S.worldmodel.getElement(id))
				.collect(Collectors.toList()) ;
				
		
		if (candidates.isEmpty()) {
			candidates = getEnablersInSameZoneFromBelief.apply(targetBlocker.id, S).stream()
					.filter(id -> ! visited.contains(id))
					.map(id -> S.worldmodel.getElement(id))
					.collect(Collectors.toList()) ;
		}
		if (candidates.isEmpty()) {
			candidates = S.worldmodel.elements.values().stream()
					.filter(e -> enablersSelector.test(e)
							     && ! visited.contains(e.id)
							     && reachabilityChecker.apply(S,e)) 
					.collect(Collectors.toList())
					;
		}
				
		if (candidates.isEmpty())
			return null ;
		
		if (policy == Policy.RANDOM) {
			return candidates.get(rnd.nextInt(candidates.size())) ;
		}
		
		Vec3 myHeuristicLocation = targetBlocker.position ;
		if (policy == Policy.NEAREST_TO_AGENT || myHeuristicLocation == null) {
			myHeuristicLocation = S.worldmodel.position ;
		}
		
		return getClosestsElement(candidates,myHeuristicLocation) ;
	}
	
	GoalStructure lowerleverSolver(BasicAgent agent, 
			String targetEntity,  
			Vec3 heuristicLocation,
			Policy policy,
			Predicate<Iv4xrAgentState> psi,
			Predicate<WorldEntity> enablersSelector,
			BiFunction<String,Iv4xrAgentState,List<String>> getConnectedEnablersFromBelief,
			BiFunction<String,Iv4xrAgentState,List<String>> getEnablersInSameZoneFromBelief,
			BiFunction<String,Iv4xrAgentState,List<String>> getCriticalBlockerFromBelief,
			int incrementalExplorationBudget,
			Random rnd
			) {
		
		Set<String> visited2 = new HashSet<>() ;
		
		
		GoalStructure search = 
		  // this "search" will be put as the body of an enclosing REPEAT-loop:
		  DEPLOY(agent, (Iv4xrAgentState S) -> {
				  WorldEntity target = S.worldmodel.elements.get(targetEntity) ;
				  WorldEntity enabler = selectEnabler(S,policy,visited2,target,
						  enablersSelector,
						  getConnectedEnablersFromBelief,
						  getEnablersInSameZoneFromBelief,
						  rnd
						  ) ;
				  
				  if (enabler == null) {
					  if (explorationExhausted.test(S)) {
						  System.out.println(">>> low-level search EXHAUSTED explore") ;
						  // to terminate the enclosing repeat:
						  return SUCCESS() ;
					  }
					  System.out.println(">>> low-level search invokes explore") ;
					  return SEQ(exploringWithHeuristic.apply(heuristicLocation,incrementalExplorationBudget),
								 FAIL()) ; // to make the enclosing repeat-loop to continue iterating
				  }
				  
				  visited2.add(enabler.id) ;
				  
				  System.out.println(">>> low-level search invokes interact " + enabler.id) ;
				  
				  return SEQ(gCandidateIsInteracted.apply(enabler.id),
						     REPEAT(FIRSTof(gTargetIsRefreshed.apply(targetEntity),
						    		        // un-lock mechanism if the above get the agent locked:
						    		        SEQ(unLock(agent,
						    		        		enabler.id,
						    		        		getConnectedEnablersFromBelief,
						    		        		getCriticalBlockerFromBelief,
						    		        		targetEntity),
						    		            FAIL()) // make it fail so gTargetIsRefreshed can be tried again
						    		        )
						    	   ),
						     lift(psi) // check psi
						     ) ; 
			  }) ;
		return SEQ(gTargetIsRefreshed.apply(targetEntity), 
				   FIRSTof(lift(psi),
				           SEQ(REPEAT(search),
				    	       lift(psi)))) ;
	}
	
	/**
	 * Goal-constructor. This is used when the agent becomes locked on its way to some target
	 * entity that it wants to check. The agent tries to change the state of the target, and in
	 * doing so it interacts an entity, then goes back to the target entity to see if its state
	 * changes to something it wants. To check the latter, the agent will need to go back to the
	 * target entity. It may happen that the aforementioned interaction closes something between
	 * the agent and the target, and hence closing its only way back to the target. This goal-
	 * constructor is meant to produce a goal that can unlock the midway-inhibitor. If possible,
	 * by using another interactable.
	 * 
	 * @param agent
	 * @param excludeThisEnabler
	 * @param getConnectedEnablersFromBelief
	 * @param getCriticalBlockerFromBelief
	 * @param targetBlocker
	 * @return
	 */
	GoalStructure unLock(BasicAgent agent, 
			String excludeThisEnabler,
			BiFunction<String,Iv4xrAgentState,List<String>> getConnectedEnablersFromBelief,
			BiFunction<String,Iv4xrAgentState,List<String>> getCriticalBlockerFromBelief,
			String targetBlocker) {
		GoalStructure G = DEPLOY(agent,
			(Iv4xrAgentState S) -> { 
				// get key blockers that would re-open the way to the target
				List<String> criritcalBlockers = getCriticalBlockerFromBelief.apply(targetBlocker,S) ;
				if (criritcalBlockers == null || criritcalBlockers.isEmpty())
					return FAIL() ;
				// else we will just pick one
				String selected = criritcalBlockers.get(0) ;
				WorldEntity selected_ = S.worldmodel.getElement(selected) ;
				List<WorldEntity> openers  = getConnectedEnablersFromBelief.apply(selected, S)
						.stream()
						.map(id -> S.worldmodel.getElement(id))
						.collect(Collectors.toList());
				if (openers.isEmpty())
					return FAIL() ;
				
				List<WorldEntity> openers2 = openers.stream()
						.filter(o -> ! o.id.equals(excludeThisEnabler))
						.collect(Collectors.toList()) ;
				WorldEntity selectedOpener = null ;
				if (!openers2.isEmpty()) {
					// check if we can find an opener, which is NOT the one to be exluced:
					selectedOpener = getClosestsElement(openers2, selected_.position) ;
				}
				else {
					// if we can't then choose it anyway:
					selectedOpener = S.worldmodel.getElement(excludeThisEnabler) ;
				}
				return SEQ(gCandidateIsInteracted.apply(selectedOpener.id),
						   gTargetIsRefreshed.apply(targetBlocker)
						) ;
			}	
				) ;
		return G ;
	}
	
	public GoalStructure solver(BasicAgent agent, 
			String tId, 
			Vec3 heuristicLocation,
			Predicate<WorldEntity> blockersSelector,
			Predicate<WorldEntity> enablersSelector,
			Predicate<WorldEntity> isOpen,
			BiFunction<String,Iv4xrAgentState,List<String>> getConnectedEnablersFromBelief,
			BiFunction<String,Iv4xrAgentState,List<String>> getEnablersInSameZoneFromBelief,
			BiFunction<String,Iv4xrAgentState,List<String>> getCriticalBlockerFromBelief,
			Predicate<Iv4xrAgentState> phi,
			Policy policy,
			int incrementalExplorationBudget
			) {
		
		Random rnd = new Random() ;
		
		// keeping track blockers that were already tried, so that we dont try them multiple times:
		Set<String> visited = new HashSet<>() ;
		GoalStructure search = 
			// this "search" will be put as the body of an enclosing REPEAT-loop:
			DEPLOY(agent,
				 	(Iv4xrAgentState S) -> {
				    
				 	// select an entity to work on:
					WorldEntity e = selectNode(S,policy,visited,tId,blockersSelector,isOpen,heuristicLocation,rnd) ;
						
					// if there is no candidate entity available, explore:
				    if (e == null) {
				    	if (explorationExhausted.test(S)) {
				    		System.out.println(">>> top-level search EXHAUSTED explore") ;
				    		// to terminate the enclosing repeat:
							return SUCCESS() ;
				    	}
				    	System.out.println(">>> top-level search invokes explore") ;
						return 
								SEQ(
								exploringWithHeuristic.apply(heuristicLocation,incrementalExplorationBudget),
								FAIL() // to make the enclosing repeat-loop to continue iterating
								) ;
					}
				    // if the selected entity is the goal-entity tId, then call a solver for phi:
					if (e.id.equals(tId)) {	
						System.out.println(">>> invoking solve(phi)") ;
						return lowerleverSolver(agent,
								   tId,
								   heuristicLocation,
								   policy,
								   phi,
								   enablersSelector,
								   getConnectedEnablersFromBelief,
								   getEnablersInSameZoneFromBelief,
								   getCriticalBlockerFromBelief,
								   incrementalExplorationBudget,
								   rnd) ;
								   // lift(phi) --- no need to check this, the solver already checked that
					}
					// if the selected entity is a (closed) blocker, call a solver to unblock it, 
					// then explore
					else {
						
						visited.add(e.id) ;
						System.out.println(">>> invoking unblock " + e.id) ;

						return SEQ(// get to the blocker first, this then also checks
								   // if the agent can actually approach the blocker to observe it
								   gTargetIsRefreshed.apply(e.id), 
								   // then unblock it:
								   lowerleverSolver(agent,
										   e.id,
										   e.position,
										   policy,
										   T -> { 
											   WorldEntity blocker = T.worldmodel.getElement(e.id) ;
											   return blocker != null && isOpen.test(blocker) ;
										   },
										   enablersSelector,
										   getConnectedEnablersFromBelief,
										   getEnablersInSameZoneFromBelief,
										   getCriticalBlockerFromBelief,
										   incrementalExplorationBudget,
										   rnd),
								   // we can optionally force exploration here, but to keep it
								   // the same as in ATEST paper, let's not do that:
								   // exploringWithHeuristic.apply(heuristicLocation,incrementalExplorationBudget)
								   FAIL()  // to make the enclosing repeat-loop to continue iterating
								   ) ;
					}
				 })
		    ;
				  
		return FIRSTof(lift(phi),
				       SEQ(REPEAT(search), lift(phi))) ;
	}

}
