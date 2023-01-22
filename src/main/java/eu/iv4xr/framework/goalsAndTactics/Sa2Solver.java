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
	

	BasicAgent agent ;
	String finalTargetId ;
	Vec3 heuristicLocation ;
	Predicate<WorldEntity> blockersSelector ;
	Predicate<WorldEntity> enablersSelector ;
	Predicate<WorldEntity> isOpen ;
	BiFunction<String,Iv4xrAgentState,List<String>> getConnectedEnablersFromBelief ;
	//BiFunction<String,Iv4xrAgentState,List<String>> getEnablersInSameZoneFromBelief ;
	BiFunction<String,Iv4xrAgentState,List<String>> getCriticalBlockerFromBelief ;
	Predicate<Iv4xrAgentState> phi ;
	Policy policy ;
	
	Random rnd = new Random() ;
	
	Map<String,Set<String>> triedEnablers = new HashMap<>() ;
	
	
	public Sa2Solver() { super() ; }
	
	public Sa2Solver(BiFunction<Iv4xrAgentState<NavgraphNode> , WorldEntity, Boolean> reachabilityChecker,
			BiFunction<Iv4xrAgentState<NavgraphNode> ,WorldEntity,Float> distanceToAgent,
			Function<Iv4xrAgentState<NavgraphNode> ,BiFunction<WorldEntity,WorldEntity,Float>> distanceFunction,
			Function<String, GoalStructure> gCandidateIsInteracted, 
			Function<String, GoalStructure> gTargetIsRefreshed,
			Predicate<Iv4xrAgentState<NavgraphNode>> explorationExhausted,
			Function<Void,GoalStructure> gExploring) {
		
		super(reachabilityChecker,
				distanceToAgent,
				distanceFunction,
				gCandidateIsInteracted,
				gTargetIsRefreshed,
				explorationExhausted,
				i -> gExploring.apply(null)
				) ;
	}
	
	/**
	 * Get currently known blockers which are still 'unsolved'. A blocker
	 * is considered as 'solved' if at least one opener for it is discovered.
	 */
	List<String> unsolvedBlockers(Iv4xrAgentState S) {
		
		return S.worldmodel.elements.values().stream()
		.filter(e -> { 
			if (! blockersSelector.test(e)) return false ;
			var ez = getConnectedEnablersFromBelief.apply(e.id,S) ;
			return ez == null || ez.isEmpty() ; 
			})
		.map(e -> e.id)
		.collect(Collectors.toList()) ;
		
	}
	
	/**
	 * Return the blockers that has not been tried yet.
	 */
	boolean isUntriedBlocker(Iv4xrAgentState S, String blockerId) {
		var ez = triedEnablers.get(blockerId) ;
		return ez == null || ez.isEmpty() ;
	}
	
	/**
	 * Check is the given blocker is still unsolved.
	 */
	boolean isUnsolvedBlocker(Iv4xrAgentState S, String blockerId) {
		return unsolvedBlockers(S).contains(blockerId) ;
	}
	
	/**
	 * Check if the given target still has untried and non-affector enablers
	 * to try.
	 */
	boolean hasUntriedEnablers(Iv4xrAgentState S, String target) {
		return ! untriedEnablers(S,target).isEmpty() ;
	}
	
	/**
	 * Get currently known enablers that have not been tried for affecting the
	 * given target entity, and moreover are not themselves affectors of the 
	 * target.
	 */
	List<String> untriedEnablers(Iv4xrAgentState S, String targetBlocker) {
		
		Set<String> previouslyTriedEnablers = triedEnablers.get(targetBlocker) ;
		if (previouslyTriedEnablers == null) {
			previouslyTriedEnablers = new HashSet<String>() ;
			triedEnablers.put(targetBlocker,previouslyTriedEnablers) ;
		}
		final Set<String> previouslyTriedEnablers_ = previouslyTriedEnablers ;
		List<String> connectedEnablers = getConnectedEnablersFromBelief.apply(targetBlocker, S) ;
		
		return S.worldmodel.elements.values().stream()
		  . filter(e -> enablersSelector.test(e) 
					&& ! previouslyTriedEnablers_.contains(e.id)
					&& (connectedEnablers == null || !connectedEnablers.contains(e.id)))
		  . map(e -> e.id)
		  . collect(Collectors.toList()) ;
	}

	
	private WorldEntity getClosestsElement(List<WorldEntity> candidates, Vec3 target) {
		WorldEntity closest = candidates.get(0) ;
		float minDistance = Float.MAX_VALUE ;
		for (var e : candidates) {
			float distSq = Vec3.distSq(e.position, target) ;
			if (distSq < minDistance) {
				closest = e ;
				minDistance = distSq ;
			}
		}
		return closest ;
	}
	
	/**
	 * Choose a candidate blocker to open. This candidate is a closed blocker that needs 
	 * to be opened to allow the agent to explore more areas (in order to discover where 
	 * the final-target entity is).
	 */
	WorldEntity selectBlocker(Iv4xrAgentState S) {
		//System.out.println(">>> invoking selectNode()") ;
		
		List<WorldEntity> candidates = S.worldmodel.elements.values().stream()
				.filter(e -> blockersSelector.test(e)
						&& ! isOpen.test(e)  // only target closed blockers
						&& ! e.id.equals(finalTargetId)
						//&& isUnsolvedBlocker(S,e.id)
						&& isUntriedBlocker(S,e.id)
						// has one reachable enabler to try:
						&& selectEnabler(S,e) != null)
				.collect(Collectors.toList()) ;
				
		//System.out.println(">>> selectNode #candidates:" + candidates.size()) ;
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
	
	
	/**
	 * Select an enabler to check if it can affect the target.
	 */	
	WorldEntity selectEnabler(Iv4xrAgentState S, WorldEntity target) {
		
		//System.out.println("### invoking selectEnabler " + target.id) ;
		
		// check first if the model has a solution:
		List<WorldEntity> candidates = getConnectedEnablersFromBelief.apply(target.id,S).stream()
		   . map(id -> S.worldmodel.elements.get(id)) 
		   . filter(e -> reachabilityChecker.apply(S,e))
		   .collect(Collectors.toList());
		
		if (candidates.isEmpty()) {
			// if it is empty get candidates from untried enablers:
			candidates = untriedEnablers(S,target.id).stream()
					. map(id -> S.worldmodel.elements.get(id))
					. filter(e -> reachabilityChecker.apply(S,e)) // for now, only check reachable candidate
					. collect(Collectors.toList()) ;
		}
		
		//System.out.println("    candidates: " + candidates) ;		
		if (candidates.isEmpty())
			return null ;
		
		if (policy == Policy.RANDOM) {
			return candidates.get(rnd.nextInt(candidates.size())) ;
		}
		
		Vec3 myHeuristicLocation = target.position ;
		if (policy == Policy.NEAREST_TO_AGENT || myHeuristicLocation == null) {
			myHeuristicLocation = S.worldmodel.position ;
		}
		//System.out.println(">>>    heuristic loc: " + myHeuristicLocation) ;
		return getClosestsElement(candidates,myHeuristicLocation) ;
	}
	
	GoalStructure lowerleverSolver(String targetEntity,
			Vec3 heuristicLocation,
			Predicate<Iv4xrAgentState> psi) {
				
		
		  GoalStructure search = 
		  // this "search" will be put as the body of an enclosing REPEAT-loop:
		  DEPLOY(agent, (Iv4xrAgentState S) -> {
				  WorldEntity target = S.worldmodel.elements.get(targetEntity) ;
				  WorldEntity enabler = selectEnabler(S,target) ;
				  
				  if (enabler == null) {
					  // should not happen...
					  // to terminate the enclosing repeat:
					  return SUCCESS() ;
				  }				  
				  
				  Set<String> previouslyTriedEnablers = triedEnablers.get(targetEntity) ;
				  if (previouslyTriedEnablers == null) {
					  previouslyTriedEnablers = new HashSet<String>() ;
					  triedEnablers.put(targetEntity,previouslyTriedEnablers) ;
				  }
				  previouslyTriedEnablers.add(enabler.id) ;
				  
				  System.out.println("=== low-level search of " + targetEntity
						  + " invokes interact " + enabler.id) ;
				  
				  return SEQ(gCandidateIsInteracted.apply(enabler.id),
						     REPEAT(
						       FIRSTof(gTargetIsRefreshed.apply(targetEntity),
						    		   // un-lock mechanism if the above get the agent locked:
						    		   unLock(enabler.id,targetEntity))
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
	 * doing so it interacts an enabler, then goes back to the target entity to see if its state
	 * changes to something it wants. To check the latter, the agent will need to go back to the
	 * target entity. It may happen that the aforementioned interaction closes something between
	 * the agent and the target, and hence closing its only way back to the target. This goal-
	 * constructor is meant to produce a goal that can unlock the midway-inhibitor. If possible,
	 * by using another enabler.
	 */
	GoalStructure unLock(String excludeThisEnabler,String targetBlocker) {
		GoalStructure G = DEPLOY(agent,
			(Iv4xrAgentState S) -> { 
				System.out.println("=== trying to unlock path to: " + targetBlocker + ", exclude enabler: " + excludeThisEnabler) ;
				// get key blockers that would re-open the way to the target
				List<String> criritcalBlockers = getCriticalBlockerFromBelief.apply(targetBlocker,S) ;
				if (criritcalBlockers == null || criritcalBlockers.isEmpty()){
					System.out.println("    cannot identify a critical on-the-way blocker.") ;
					return SUCCESS() ; // to terminate the outer repeat-loop
				}
				// else we will just pick one
				String selected = criritcalBlockers.get(0) ;
				System.out.println("    crirtical blocker: on-the-way blocker " + selected) ;
				WorldEntity selected_ = S.worldmodel.getElement(selected) ;
				List<WorldEntity> openers  = getConnectedEnablersFromBelief.apply(selected, S)
						.stream()
						.filter(o -> ! o.equals(excludeThisEnabler))
						.map(id -> S.worldmodel.getElement(id))
						.filter(e -> reachabilityChecker.apply(S,e))
						.collect(Collectors.toList());
				
				if (openers.isEmpty()) {
					openers = S.worldmodel.elements.values().stream()
							.filter(e -> enablersSelector.test(e) && ! e.id.equals(excludeThisEnabler))
							.filter(e -> reachabilityChecker.apply(S,e))
							.collect(Collectors.toList())
							;
							
				}
				
				WorldEntity selectedOpener = null ;
				if (! openers.isEmpty()) {
					// just choose the closest one
					selectedOpener = getClosestsElement(openers, selected_.position) ;
				}
				if (openers.isEmpty())
					// if no candidate can be found we will just try the excluded enabler:
					selectedOpener = S.worldmodel.elements.get(excludeThisEnabler) ;
				
				System.out.println("    trying as an opener: " + selectedOpener.id) ;
				return SEQ(gCandidateIsInteracted.apply(selectedOpener.id),
						   gTargetIsRefreshed.apply(selected),
						   FAIL() // to force the outer repeat-loop to try again...
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
			BiFunction<String,Iv4xrAgentState,List<String>> getCriticalBlockerFromBelief,
			Predicate<Iv4xrAgentState> phi,
			Policy policy
			) {
		
		this.agent = agent ;
		this.finalTargetId = tId ;
		this.heuristicLocation = heuristicLocation ;
		this.blockersSelector = blockersSelector ;
		this.enablersSelector = enablersSelector ;
		this.isOpen = isOpen ;
		this.getConnectedEnablersFromBelief = getConnectedEnablersFromBelief ;
		//this.getEnablersInSameZoneFromBelief = getEnablersInSameZoneFromBelief ;
		this.getCriticalBlockerFromBelief = getCriticalBlockerFromBelief ;
		this.phi = phi ;
		this.policy = policy ;

		this.triedEnablers.clear();
		

		GoalStructure search = 
			// this "search" will be put as the body of an enclosing REPEAT-loop:
			DEPLOY(agent,
				 	(Iv4xrAgentState S) -> {
				    System.out.println("=== invoking top-level search") ;
				 	if (phi.test(S))
				 		return SUCCESS() ;
				 	
				 	// ok so phi is not established yet...
				 	
				 	// (1) favor exploration first, if possible:
				 	//System.out.println("=== exploration exhausted: " + explorationExhausted.test(S)) ;
				 	if (!explorationExhausted.test(S)) {
				 		System.out.println("=== top-level search invokes explore") ;
						return 
								SEQ(
								// the budget "0" is ignored; this will explore exhaustively		
								exploring.apply(0),
								FAIL() // to make the enclosing repeat-loop to continue iterating
								) ;
				 	}
				 	
				 	WorldEntity target = S.worldmodel.getElement(tId) ;
				 	// else, (2) if the target is found, and there are some untried enablers ,
				 	// make an attempt to solve phi. For now, we will limit to try only
				 	// reachable enablers:
				 	if (target != null 
				 			&& reachabilityChecker.apply(S, target)
				 			&& selectEnabler(S,target) != null) {
				 		System.out.println("=== invoking solve(phi)") ;
						return lowerleverSolver(tId,heuristicLocation,phi) ;
				 	}
				 	// else, (3) select a blocker to open:
					WorldEntity e = selectBlocker(S) ;
					if (e == null) {
						// (3a) there is no more reachable blocker left to try
						// we give up:
						System.out.println("=== no more reachable blcoker to try. Stopping the search.") ;
						return SUCCESS() ; // to break the outer repeat-loop
					}
						
					// (3b) we try to solve the blocker:	
					System.out.println("=== invoking unblock " + e.id) ;

					return SEQ(// get to the blocker first, this then also checks
							// if the agent can actually approach the blocker to observe it
							gTargetIsRefreshed.apply(e.id), 
							// then unblock it:
							lowerleverSolver(e.id,
									e.position,
									T -> { 
										WorldEntity blocker = T.worldmodel.getElement(e.id) ;
										return blocker != null && isOpen.test(blocker) ;
									}),
							// we can optionally force exploration here, but to keep it
							// the same as in ATEST paper, let's not do that:
							// exploringWithHeuristic.apply(heuristicLocation,incrementalExplorationBudget)
							FAIL()  // to make the enclosing repeat-loop to continue iterating
							) ;
					
				 })
		    ;
				  
		return SEQ(REPEAT(search),lift(phi)) ;
	}

}
