//package eu.iv4xr.framework.goalsAndTactics;
//
//import static nl.uu.cs.aplib.AplibEDSL.DEPLOY;
//import static nl.uu.cs.aplib.AplibEDSL.SEQ;
//import static nl.uu.cs.aplib.AplibEDSL.SUCCESS;
//import static nl.uu.cs.aplib.AplibEDSL.lift;
//
//import java.util.*;
//import java.util.function.BiFunction;
//import java.util.function.Function;
//import java.util.function.Predicate;
//import java.util.stream.Collectors;
//
//import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
//import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
//import eu.iv4xr.framework.mainConcepts.WorldEntity;
//import eu.iv4xr.framework.spatial.Vec3;
//import nl.uu.cs.aplib.mainConcepts.BasicAgent;
//import nl.uu.cs.aplib.mainConcepts.GoalStructure;
//import static nl.uu.cs.aplib.AplibEDSL.* ;
//
///**
// * This class implements a "solver" we call SA2. Imagine a test-agent and a
// * game-under-test (GUT). We want to move the GUT to a state where some game
// * object/entity o satisfies some predicate phi. The solver produces a
// * goal-structure, that when given to a test-agent for execution, it will do
// * just that. The algorithm implemented by the solver works by exploring the
// * world zone/room by zone until the test-agent sees o. Then it will try out
// * various interactables that it can reach until it manages to flip the state of
// * o to satisfy phi.
// *
// * <p>
// * SA2 is implemented as a subclass of SA1. However, unlike {@link SA1}, SA2
// * does try to actively unlock zones so that the test-agent can explore them. If
// * an approximate location of o is given, as a "heuristic location", the zones
// * will be tried, roughly, in the direction towards this heuristic location.
// *
// * <p>
// * The solver will need a bunch of ingredients to work. For example
// * goal-constructors that need to be given to this class-constructor. See
// * {@link #Sa1Solver(BiFunction, BiFunction, Function, Function, Function, Predicate, Function)}.
// * After constructed, you can invoke the method {@link #solver(BasicAgent,
// * String, Vec3, Predicate, Predicate, Predicate, BiFunction, BiFunction,
// * Predicate, Policy). This will produce a goal-structure that carries the above
// * mentioned solving algorithm.
// *
// * <p>Terminologies:
// *
// * <ul>
// *   <li> A game world is populated by game objects/entities. Each may have a state.
// *   <li> A zone/room is a closed area in the world. Travel within a zone is possible.
// *        However, access to a zone is usually guarded by "doors" or "gates". We will
// *        call these "blockers". A zone can only be entered through a blocker that
// *        guards it, and its state should be "open" (non-blocking).
// *   <li> Some gane objects can be interacted. They are called "interactables". Some
// *        of these can affect the state of other objects as well. Such interactables
// *        are called "enablers". When an enabler e can affect the state of an object
// *        o, we say that they are "connected". While it is reasonable to provide
// *        information wich objects are, or could be, enablers, it is usually harder
// *        to provide knowledge which objects are connected to which enablers. Typically,
// *        a test-agent will have to figure out, or discover, the latter type of relation.
// *   <li> An enabler that can open a door is called an "opener" of the door.
// * </ul>
// *
// * @author Samira, Wish. Based on Samira's algorithm in ATEST 2022.
// *
// * @param <NavgraphNode>
// */
//public class Sa2Solver<NavgraphNode> extends Sa1Solver<NavgraphNode> {
//
//	// some internal variables:
//
//	/**
//	 * The test-agent that will execute the solver (well, more precisely, the agent
//	 * that will execute the goal-structure produced by the solver).
//	 */
//	BasicAgent agent ;
//
//	/**
//	 * The id of the target game-object whose state we want to flip.
//	 */
//	String finalTargetId ;
//
//	/**
//	 * Approximate location of {@link #finalTargetId}.
//	 */
//	Vec3 heuristicLocation ;
//
//	/**
//	 * A filter specifying which game-objects are zones' blockers.
//	 */
//	Predicate<WorldEntity> blockersSelector ;
//
//	/**
//	 * A filter specifying which game-objects are enablers.
//	 */
//	Predicate<WorldEntity> enablersSelector ;
//
//	/**
//	 * A predicate that can decide if a blocker is in the open (non-blocking)
//	 * state.
//	 */
//	Predicate<WorldEntity> isOpen ;
//
//	/**
//	 * A function to wuery the agent's belief/state to see if it has information on
//	 * openners of a given blocker. So getConnectedEnablersFromBelief(o,S) queries
//	 * the state S and returns known openners of o, if such information is available.
//	 */
//	BiFunction<String,Iv4xrAgentState,List<String>> getConnectedEnablersFromBelief ;
//
//	/**
//	 * Suppose the agent wants to navigate to an object o, but it can't because
//	 * all paths from the agent position to o are closed. Suppose one of the
//	 * paths is only blocked by a single blocker, such that opening this blocker
//	 * would make o reachable. This block is called critical blocker towards o.
//	 */
//	BiFunction<String,Iv4xrAgentState,List<String>> getCriticalBlockerFromBelief ;
//	Predicate<Iv4xrAgentState> phi ;
//	Policy policy ;
//
//	Random rnd = new Random() ;
//
//	/**
//	 * To keep track of enablers that have been tried to flip the state of an
//	 * object. Members if this mapping are pairs (o,es) where o is an object,
//	 * and es is a set of enablers that have been tried to flip o.
//	 */
//	Map<String,Set<String>> triedEnablers = new HashMap<>() ;
//
//
//	public Sa2Solver() { super() ; }
//
//	/**
//	 * A constructor for SA2.
//	 *
//	 * @param reachabilityChecker
//	 * @param distanceToAgent
//	 * @param distanceFunction
//	 * @param gCandidateIsInteracted
//	 * @param gTargetIsRefreshed
//	 * @param explorationExhausted
//	 * @param gExploring
//	 */
//	public Sa2Solver(BiFunction<Iv4xrAgentState<NavgraphNode> , WorldEntity, Boolean> reachabilityChecker,
//			BiFunction<Iv4xrAgentState<NavgraphNode> ,WorldEntity,Float> distanceToAgent,
//			Function<Iv4xrAgentState<NavgraphNode> ,BiFunction<WorldEntity,WorldEntity,Float>> distanceFunction,
//			Function<String, GoalStructure> gCandidateIsInteracted,
//			Function<String, GoalStructure> gTargetIsRefreshed,
//			Predicate<Iv4xrAgentState<NavgraphNode>> explorationExhausted,
//			Function<Void,GoalStructure> gExploring) {
//
//		super(reachabilityChecker,
//				distanceToAgent,
//				distanceFunction,
//				gCandidateIsInteracted,
//				gTargetIsRefreshed,
//				explorationExhausted,
//				i -> gExploring.apply(null)
//				) ;
//	}
//
//	/**
//	 * Get currently known blockers which are still 'unsolved'. A blocker
//	 * is considered as 'solved' if at least one opener for it is discovered.
//	 */
//	List<String> unsolvedBlockers(Iv4xrAgentState S) {
//
//		return S.worldmodel.elements.values().stream()
//		.filter(e -> {
//			if (! blockersSelector.test(e)) return false ;
//			var ez = getConnectedEnablersFromBelief.apply(e.id,S) ;
//			return ez == null || ez.isEmpty() ;
//			})
//		.map(e -> e.id)
//		.collect(Collectors.toList()) ;
//
//	}
//
//	/**
//	 * Return blockers that have not been tried yet.
//	 */
//	boolean isUntriedBlocker(Iv4xrAgentState S, String blockerId) {
//		var ez = triedEnablers.get(blockerId) ;
//		return ez == null || ez.isEmpty() ;
//	}
//
//	/**
//	 * Check if the given blocker is still unsolved. See also
//	 * {@link #unsolvedBlockers(Iv4xrAgentState)}.
//	 */
//	boolean isUnsolvedBlocker(Iv4xrAgentState S, String blockerId) {
//		return unsolvedBlockers(S).contains(blockerId) ;
//	}
//
//	/**
//	 * Check if the given target still has untried enablers and are not themselves
//	 * openers for the target.
//	 */
//	boolean hasUntriedEnablers(Iv4xrAgentState S, String target) {
//		return ! untriedEnablers(S,target).isEmpty() ;
//	}
//
//	/**
//	 * Check if the given target still has untried enablers and are not themselves
//	 * openers for the target, and are reachable from the current agent's location.
//	 */
//	boolean hasUntriedReachableEnablers(Iv4xrAgentState S, String target) {
//		var candidates = untriedEnablers(S,target) ;
//		return candidates.stream().anyMatch(id -> reachabilityChecker.apply(S, S.worldmodel.getElement(id))) ;
//	}
//
//	/**
//	 * Get currently known enablers that have not been tried for affecting the
//	 * given target entity, and moreover are not themselves openers of the
//	 * target.
//	 */
//	List<String> untriedEnablers(Iv4xrAgentState S, String targetBlocker) {
//
//		Set<String> previouslyTriedEnablers = triedEnablers.get(targetBlocker) ;
//		if (previouslyTriedEnablers == null) {
//			previouslyTriedEnablers = new HashSet<String>() ;
//			triedEnablers.put(targetBlocker,previouslyTriedEnablers) ;
//		}
//		final Set<String> previouslyTriedEnablers_ = previouslyTriedEnablers ;
//		List<String> connectedEnablers = getConnectedEnablersFromBelief.apply(targetBlocker, S) ;
//
//		return S.worldmodel.elements.values().stream()
//		  . filter(e -> enablersSelector.test(e)
//					&& ! previouslyTriedEnablers_.contains(e.id)
//					&& (connectedEnablers == null || !connectedEnablers.contains(e.id)))
//		  . map(e -> e.id)
//		  . collect(Collectors.toList()) ;
//	}
//
//
//	private WorldEntity getClosestsElement(List<WorldEntity> candidates, Vec3 target) {
//		WorldEntity closest = candidates.get(0) ;
//		float minDistance = Float.MAX_VALUE ;
//		for (var e : candidates) {
//			float distSq = Vec3.distSq(e.position, target) ;
//			if (distSq < minDistance) {
//				closest = e ;
//				minDistance = distSq ;
//			}
//		}
//		return closest ;
//	}
//
//	/**
//	 * Choose a candidate blocker to open. This candidate is a closed blocker that needs
//	 * to be opened to allow the agent to explore more areas (in order to discover where
//	 * the final-target entity is).
//	 */
//	WorldEntity selectBlocker(Iv4xrAgentState S) {
//		//System.out.println(">>> invoking selectNode()") ;
//
//		List<WorldEntity> candidates = S.worldmodel.elements.values().stream()
//				.filter(e -> blockersSelector.test(e)
//						&& ! isOpen.test(e)  // only target closed blockers
//						&& ! e.id.equals(finalTargetId)
//						//&& isUnsolvedBlocker(S,e.id)
//						&& (isUntriedBlocker(S,e.id) || hasUntriedReachableEnablers(S,e.id))
//						// has one reachable enabler to try:
//						&& selectEnabler(S,e) != null)
//				.collect(Collectors.toList()) ;
//
//		//System.out.println(">>> selectNode #candidates:" + candidates.size()) ;
//		if (candidates.isEmpty())
//			return null ;
//
//		if (policy == Policy.RANDOM) {
//			return candidates.get(rnd.nextInt(candidates.size())) ;
//		}
//
//		Vec3 myHeuristicLocation = heuristicLocation ;
//		if (policy == Policy.NEAREST_TO_AGENT || myHeuristicLocation == null) {
//			myHeuristicLocation = S.worldmodel.position ;
//		}
//
//		return getClosestsElement(candidates,myHeuristicLocation) ;
//	}
//
//
//	/**
//	 * Select an enabler to check if it can affect the target.
//	 */
//	WorldEntity selectEnabler(Iv4xrAgentState S, WorldEntity target) {
//
//		//System.out.println("### invoking selectEnabler " + target.id) ;
//
//		// check first if the model has a solution:
//		List<WorldEntity> candidates = getConnectedEnablersFromBelief.apply(target.id,S).stream()
//		   . map(id -> S.worldmodel.elements.get(id))
//		   . filter(e -> reachabilityChecker.apply(S,e))
//		   .collect(Collectors.toList());
//
//		if (candidates.isEmpty()) {
//			// if it is empty get candidates from untried enablers:
//			candidates = untriedEnablers(S,target.id).stream()
//					. map(id -> S.worldmodel.elements.get(id))
//					. filter(e -> reachabilityChecker.apply(S,e)) // for now, only check reachable candidate
//					. collect(Collectors.toList()) ;
//		}
//
//		//System.out.println("    candidates: " + candidates) ;
//		if (candidates.isEmpty())
//			return null ;
//
//		if (policy == Policy.RANDOM) {
//			return candidates.get(rnd.nextInt(candidates.size())) ;
//		}
//
//		Vec3 myHeuristicLocation = target.position ;
//		if (policy == Policy.NEAREST_TO_AGENT || myHeuristicLocation == null) {
//			myHeuristicLocation = S.worldmodel.position ;
//		}
//		//System.out.println(">>>    heuristic loc: " + myHeuristicLocation) ;
//		return getClosestsElement(candidates,myHeuristicLocation) ;
//	}
//
//	GoalStructure lowerleverSolver(String targetEntity,
//			Vec3 heuristicLocation,
//			Predicate<Iv4xrAgentState> psi) {
//
//		  System.out.println("=== invoking low level search, target: " + targetEntity) ;
//
//		  GoalStructure search =
//		  // this "search" will be put as the body of an enclosing REPEAT-loop:
//		  DEPLOY(agent, (Iv4xrAgentState S) -> {
//				  WorldEntity target = S.worldmodel.elements.get(targetEntity) ;
//				  WorldEntity enabler = selectEnabler(S,target) ;
//
//				  if (enabler == null) {
//					  System.out.println("    cannot find any candidate enabler!") ;
//					  // should not happen...
//					  // to terminate the enclosing repeat:
//					  return SUCCESS() ;
//				  }
//
//				  System.out.println("    enabler to try: " + enabler.id) ;
//
//				  Set<String> previouslyTriedEnablers = triedEnablers.get(targetEntity) ;
//				  if (previouslyTriedEnablers == null) {
//					  previouslyTriedEnablers = new HashSet<String>() ;
//					  triedEnablers.put(targetEntity,previouslyTriedEnablers) ;
//				  }
//				  previouslyTriedEnablers.add(enabler.id) ;
//
//				  System.out.println("=== low-level search of " + targetEntity
//						  + " invokes interact " + enabler.id) ;
//
//				  return SEQ(gCandidateIsInteracted.apply(enabler.id),
//						     REPEAT(
//						       FIRSTof(gTargetIsRefreshed.apply(targetEntity),
//						    		   // un-lock mechanism if the above get the agent locked:
//						    		   unLock(enabler.id,targetEntity))
//						    	   ),
//						     lift(psi) // check psi
//						     ) ;
//			  }) ;
//
//		return SEQ(gTargetIsRefreshed.apply(targetEntity),
//				   FIRSTof(lift(psi),
//				           SEQ(REPEAT(search),
//				    	       lift(psi)))) ;
//	}
//
//	/**
//	 * Goal-constructor. This is used when the agent becomes locked on its way to some target
//	 * entity that it wants to check. The agent tries to change the state of the target, and in
//	 * doing so it interacts an enabler, then goes back to the target entity to see if its state
//	 * changes to something it wants. To check the latter, the agent will need to go back to the
//	 * target entity. It may happen that the aforementioned interaction closes something between
//	 * the agent and the target, and hence closing its only way back to the target. This goal-
//	 * constructor is meant to produce a goal that can unlock the midway-inhibitor. If possible,
//	 * by using another enabler.
//	 */
//	GoalStructure unLock(String excludeThisEnabler,String targetBlocker) {
//
//		List<String> tried = new LinkedList<>() ;
//
//		GoalStructure G = DEPLOY(agent,
//			(Iv4xrAgentState S) -> {
//				System.out.println("=== trying to unlock path to: " + targetBlocker + ", exclude enabler: " + excludeThisEnabler) ;
//				// get key blockers that would re-open the way to the target
//				List<String> criritcalBlockers = getCriticalBlockerFromBelief.apply(targetBlocker,S) ;
//				if (criritcalBlockers == null || criritcalBlockers.isEmpty()){
//					System.out.println("    cannot identify a critical on-the-way blocker.") ;
//					return SUCCESS() ; // to terminate the outer repeat-loop
//				}
//				// else we will just pick one
//				String selected = criritcalBlockers.get(0) ;
//				System.out.println("    crirtical blocker: on-the-way blocker " + selected) ;
//				WorldEntity selected_ = S.worldmodel.getElement(selected) ;
//				List<WorldEntity> openers  = getConnectedEnablersFromBelief.apply(selected, S)
//						.stream()
//						.filter(o -> ! o.equals(excludeThisEnabler) && ! tried.contains(o))
//						.map(id -> S.worldmodel.getElement(id))
//						.filter(e -> reachabilityChecker.apply(S,e))
//						.collect(Collectors.toList());
//
//				if (openers.isEmpty()) {
//					openers = S.worldmodel.elements.values().stream()
//							.filter(e -> enablersSelector.test(e)
//									     && ! e.id.equals(excludeThisEnabler)
//									     && ! tried.contains(e.id))
//							.filter(e -> reachabilityChecker.apply(S,e))
//							.collect(Collectors.toList())
//							;
//
//				}
//
//				WorldEntity selectedOpener = null ;
//				if (! openers.isEmpty()) {
//					// just choose the closest one
//					selectedOpener = getClosestsElement(openers, selected_.position) ;
//					tried.add(selectedOpener.id) ;
//				}
//				if (openers.isEmpty())
//					// if no candidate can be found we will just try the excluded enabler:
//					selectedOpener = S.worldmodel.elements.get(excludeThisEnabler) ;
//
//				System.out.println("    trying as an opener: " + selectedOpener.id) ;
//				return SEQ(gCandidateIsInteracted.apply(selectedOpener.id),
//						   gTargetIsRefreshed.apply(selected),
//						   FAIL() // to force the outer repeat-loop to try again...
//						   ) ;
//			}
//				) ;
//		return G ;
//	}
//
//
//	/**
//	 * SA2 solver. More precisely, this method produces a goal-structure that can
//	 * be given to a test-agent.
//	 *
//	 * <p> Describe this :) TODO.
//	 *
//	 * @param agent
//	 * @param tId
//	 * @param heuristicLocation
//	 * @param blockersSelector
//	 * @param enablersSelector
//	 * @param isOpen
//	 * @param getConnectedEnablersFromBelief
//	 * @param getCriticalBlockerFromBelief
//	 * @param phi
//	 * @param policy
//	 * @return
//	 */
//	public GoalStructure solver(BasicAgent agent,
//			String tId,
//			Vec3 heuristicLocation,
//			Predicate<WorldEntity> blockersSelector,
//			Predicate<WorldEntity> enablersSelector,
//			Predicate<WorldEntity> isOpen,
//			BiFunction<String,Iv4xrAgentState,List<String>> getConnectedEnablersFromBelief,
//			BiFunction<String,Iv4xrAgentState,List<String>> getCriticalBlockerFromBelief,
//			Predicate<Iv4xrAgentState> phi,
//			Policy policy
//			) {
//
//		this.agent = agent ;
//		this.finalTargetId = tId ;
//		this.heuristicLocation = heuristicLocation ;
//		this.blockersSelector = blockersSelector ;
//		this.enablersSelector = enablersSelector ;
//		this.isOpen = isOpen ;
//		this.getConnectedEnablersFromBelief = getConnectedEnablersFromBelief ;
//		//this.getEnablersInSameZoneFromBelief = getEnablersInSameZoneFromBelief ;
//		this.getCriticalBlockerFromBelief = getCriticalBlockerFromBelief ;
//		this.phi = phi ;
//		this.policy = policy ;
//
//		this.triedEnablers.clear();
//
//
//		GoalStructure search =
//			// this "search" will be put as the body of an enclosing REPEAT-loop:
//			DEPLOY(agent,
//				 	(Iv4xrAgentState S) -> {
//				    System.out.println("=== invoking top-level search") ;
//				 	if (phi.test(S))
//				 		return SUCCESS() ;
//
//				 	// ok so phi is not established yet...
//
//				 	// (1) favor exploration first, if possible:
//				 	//System.out.println("=== exploration exhausted: " + explorationExhausted.test(S)) ;
//				 	if (!explorationExhausted.test(S)) {
//				 		System.out.println("=== top-level search invokes explore") ;
//						return
//								SEQ(
//								// the budget "0" is ignored; this will explore exhaustively
//								exploring.apply(0),
//								FAIL() // to make the enclosing repeat-loop to continue iterating
//								) ;
//				 	}
//
//				 	WorldEntity target = S.worldmodel.getElement(tId) ;
//				 	// else, (2) if the target is found, and there are some untried enablers ,
//				 	// make an attempt to solve phi. For now, we will limit to try only
//				 	// reachable enablers:
//				 	if (target != null
//				 			&& reachabilityChecker.apply(S, target)
//				 			&& selectEnabler(S,target) != null) {
//				 		System.out.println("=== invoking solve(phi)") ;
//						return lowerleverSolver(tId,heuristicLocation,phi) ;
//				 	}
//				 	// else, (3) select a blocker to open:
//					WorldEntity e = selectBlocker(S) ;
//					if (e == null) {
//						// (3a) there is no more reachable blocker left to try
//						// we give up:
//						System.out.println("=== no more reachable blcoker to try. Stopping the search.") ;
//						return SUCCESS() ; // to break the outer repeat-loop
//					}
//
//					// (3b) we try to solve the blocker:
//					System.out.println("=== invoking unblock " + e.id) ;
//
//					return SEQ(// get to the blocker first, this then also checks
//							// if the agent can actually approach the blocker to observe it
//							// gTargetIsRefreshed.apply(e.id),
//							// NOTE: the above targetRefreshed is not needed because lowerleverSolver
//							// will do it anyway, and worse it triggers a looping behavior (not sure
//							// why) ... need to investigate this. TODO
//							// Anyway, not doing it seems to solve the problem.
//
//							// unblock it:
//							lowerleverSolver(e.id,
//									e.position,
//									T -> {
//										WorldEntity blocker = T.worldmodel.getElement(e.id) ;
//										//System.out.println("### " + e.id + " open: " + isOpen.test(blocker)) ;
//										return blocker != null && isOpen.test(blocker) ;
//									}),
//							// we can optionally force exploration here, but to keep it
//							// the same as in ATEST paper, let's not do that:
//							// exploringWithHeuristic.apply(heuristicLocation,incrementalExplorationBudget)
//							FAIL()  // to make the enclosing repeat-loop to continue iterating
//							) ;
//
//				 })
//		    ;
//
//		return SEQ(REPEAT(search),lift(phi)) ;
//	}
//
//}
