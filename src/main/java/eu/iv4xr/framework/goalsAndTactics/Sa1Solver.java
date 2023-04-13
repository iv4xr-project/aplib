package eu.iv4xr.framework.goalsAndTactics;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.*;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.*;
import static nl.uu.cs.aplib.AplibEDSL.* ;

/**
 * This class implements a "solver" we call SA1. Imagine a test-agent and a
 * game-under-test (GUT). We want to move the GUT to a state where some game
 * object/entity o satisfies some predicate phi. The solver produces a
 * goal-structure, that when given to a test-agent for execution, it will do
 * just that. The algorithm implemented by the solver is to simply explore the
 * world to discover candidates interactables, and then try them one by one to
 * see if one would flip the state of o to phi. Some heuristic is used to
 * determine which candidates are tried first, e.g. by their distance to tId, or
 * by their distance to the test-agent.
 * 
 * <p>
 * SA1 requires the target object o to be in the same zone as where the test
 * agent is when it starts the solver. In particular, SA1 does not actively
 * trying to open/unlock access to zones. 
 * 
 * <p>
 * The solver will need a bunch of ingredients to work. For example
 * goal-constructors that need to be given to this class-constructor. See
 * {@link #Sa1Solver(BiFunction, BiFunction, Function, Function, Function, Predicate, Function)}.
 * After constructed, you can invoke the method
 * {@link #solver(BasicAgent, String, Predicate, Predicate, Policy, int)}. This
 * will produce a goal-structure that carries the above mentioned solving
 * algorithm.
 * 
 * @author Samira, Wish. Based on Samira's algorithm in ATEST 2021.
 *
 * @param <NavgraphNode>
 */
public class Sa1Solver<NavgraphNode>  {
	
	/**
	 * A function to check if an entity is reachable from the agent's position. The
	 * agent is not specified explicitly, but instead the function takes an agent-state
	 * as input. "The agent" is the agent that "owns" this state, and its id is
	 * embedded in that state.
	 */
	public BiFunction<Iv4xrAgentState<NavgraphNode> ,WorldEntity,Boolean> reachabilityChecker ;
	
	/**
	 * A function that returns the "distance" between an entity and the agent. 
	 */
	public BiFunction<Iv4xrAgentState<NavgraphNode> ,WorldEntity,Float> distanceToAgent ;
	/**
	 * A function that returns the "distance" between two entities.
	 */
	public Function<Iv4xrAgentState<NavgraphNode> ,BiFunction<WorldEntity,WorldEntity,Float>> distanceBetweenEntities ;
	/**
	 * A goal constructor that given the id of an entity, constructs a goal that would
	 * make the entity interacted.
	 */
	public Function<String,GoalStructure> gCandidateIsInteracted ;
	/**
	 * A goal constructor that given the id of an entity, constructs a goal that would
	 * make the agent to refresh its knowledge on the entity.
	 */
	public Function<String,GoalStructure> gTargetIsRefreshed ;
	/**
	 * A predicate  for checking if exploration is exhausted in the current state.
	 * "Exhausted" means that there is no reachable place left in the current state
	 * where the agent can explore to.
	 */
	public Predicate<Iv4xrAgentState<NavgraphNode>> explorationExhausted ;
	/**
	 * A goal constructor that would cause the agent to explore areas for
	 * some amount of budget.
	 */
	public Function<Integer,GoalStructure> exploring ;
	
	public Sa1Solver() { }
	
	/**
	 * A constructor for the solver.
	 * 
	 * @param reachabilityChecker  A function to check if an entity is reachable from the agent's position.
	 * 				See also {@link #reachabilityChecker}.
	 * 
	 * @param distanceToAgent  A function that returns the "distance" between an entity and the agent. 
	 * @param distanceFunction A function that returns the "distance" between two entities.
	 * 
	 * @param gCandidateIsInteracted A goal constructor that given the id of an entity, constructs a goal that would
	 * make the entity interacted.
	 * 
	 * @param gTargetIsRefreshed A goal constructor that given the id of an entity, constructs a goal that would
	 * make the agent to refresh its knowledge on the entity.
	 * 
	 * @param explorationExhausted  A predicate  for checking if exploration is exhausted in the current state.
	 * "Exhausted" means that there is no reachable place left in the current state
	 * where the agent can explore to.
	 * 
	 * @param exploring  A goal constructor that would cause the agent to explore areas for
	 * some amount of budget.
	 */
	public Sa1Solver(BiFunction<Iv4xrAgentState<NavgraphNode> , WorldEntity, Boolean> reachabilityChecker,
			BiFunction<Iv4xrAgentState<NavgraphNode> ,WorldEntity,Float> distanceToAgent,
			Function<Iv4xrAgentState<NavgraphNode> ,BiFunction<WorldEntity,WorldEntity,Float>> distanceFunction,
			Function<String, GoalStructure> gCandidateIsInteracted, 
			Function<String, GoalStructure> gTargetIsRefreshed,
			Predicate<Iv4xrAgentState<NavgraphNode>> explorationExhausted,
			Function<Integer,GoalStructure> exploring) {
		this.reachabilityChecker = reachabilityChecker;
		this.distanceBetweenEntities = distanceFunction ;
		this.distanceToAgent = distanceToAgent ;
		this.gCandidateIsInteracted = gCandidateIsInteracted;
		this.gTargetIsRefreshed = gTargetIsRefreshed;
		this.explorationExhausted = explorationExhausted ;
		this.exploring = exploring;
	}
	
	
	Random rnd = new Random() ;
	
	WorldEntity nextCandidate(
			Iv4xrAgentState<NavgraphNode> belief, 
			List<String> visited,
			Predicate<WorldEntity> selector,
			String tId,
			Policy policy) {
		
		List<WorldEntity> candidates = belief.worldmodel.elements.values()
				.stream()
				.filter(e -> selector.test(e) 
						&& ! visited.contains(e.id)
						&& reachabilityChecker.apply(belief,e)
						) 
				.collect(Collectors.toList());
		if (candidates.isEmpty()) {
			return null ;
		}
		
		WorldEntity chosen = null ;
				
		if (policy == Policy.RANDOM) {
			chosen = candidates.get(rnd.nextInt(candidates.size())) ;
			return chosen ;
		}
		
		switch(policy) {
			case NEAREST_TO_AGENT: 
				candidates.sort((e1,e2) -> Float.compare(
						distanceToAgent.apply(belief,e1), 
						distanceToAgent.apply(belief,e2))) ;
				break ;
			case  NEAREST_TO_TARGET:
				var target = belief.worldmodel.elements.get(tId) ;
				// should not be null!
				if(target == null) throw new IllegalArgumentException() ;
				candidates.sort((e1,e2) -> Float.compare(
						(float) distanceBetweenEntities.apply(belief).apply(e1,target),
						(float) distanceBetweenEntities.apply(belief).apply(e2,target)
				   )) ;
				break ;
			case RANDOM:
				break ;
		}
		
		chosen = candidates.get(0) ;	
		return chosen ;
	}

		
	
	/*
	 * Perform exploration for the given budget. It will explore until there is 
	 * nothing left to explore, or until the budget runs out. 
	 * The goal is intentionally made to always fail.
	 */	
	/*
	GoalStructure pgExplore(int budget) {
		
		GoalStructure explr = goal("exploring (persistent-goal: aborted when it is terminated)").toSolve(belief -> false)
				.withTactic(FIRSTof(explore.lift(),
						    ABORT()))
				.lift()
				.maxbudget(budget)
				;
		
		return explr ;
		
	}
	*/
	
	public enum Policy { NEAREST_TO_AGENT, NEAREST_TO_TARGET, RANDOM } 
	
	
	/**
	 * Produces SA1 solver. More precisely, this method produces a goal structure G
	 * that tries to "solve" a given predicate phi on some target game object/entity
	 * tId. To actually solve phi, you need to give G to a test-agent who will then
	 * execute it. When executed, G tries to move the game-under-test to a state
	 * where the game object/entity tId satisfies the predicate phi as a goal. The
	 * algorithm is as follows:
	 * 
	 * <ol>
	 * <li>The agent moves to tId until it can see it.
	 * <li>It checks the predicate phi. If it is satisfied we are done.
	 * <li>Else, the agent check if there is a reachable entity e whose properties
	 * satisfy the given selector, and not yet tried before. If there is such an e,
	 * the agent moves to it and interacts with it. Then it repeats from step-1.
	 * <li>If there is no such e in step-3, the agent checks if the world has some
	 * unvisited place (has some area to explore). If so, the agent explores for
	 * some budget, then we go back to step-3.
	 * <li>If there is no more place to explore, the search fails.
	 * </ol>
	 * 
	 * @param agent                        The agent that will execute the
	 *                                     goal-structure produced by this method.
	 * @param tId                          The id of the target game object/entity.
	 * @param selector                     A predicate to filter which objects would
	 *                                     be tried in order to flip the state of
	 *                                     tId.
	 * @param phi                          A predicate on tId that we want to
	 *                                     achieve/establish.
	 * @param policy                       A seach policy: check candidates closest
	 *                                     to tId first, or closest to the executing
	 *                                     agent first, or random.
	 * 
	 * @param incrementalExplorationBudget Time budget for each exploration round.
	 * 
	 * @return A goal-structure; when executed on the above agent would try to move
	 *         the game-under-test to a state where phi holds on tId.
	 */
	public GoalStructure solver(BasicAgent agent, 
			String tId, 
			Predicate<WorldEntity> selector,
			Predicate<Iv4xrAgentState> phi,
			Policy policy,
			int incrementalExplorationBudget
			) {
		
		
		Random rnd = new Random() ;
		
		List<String> visited = new LinkedList<>() ;
			
		/*
		  Deploy a dynamic goal to interact with a "nearest" reachable candidate interactable. Abort
		  if there is no such interactable.
		  "Nearest" is determined by the used policy.
		 */
		GoalStructure search = DEPLOY(agent,
				
				(Iv4xrAgentState belief) -> {
					
					WorldEntity chosen = nextCandidate(belief,visited,selector,tId,policy) ;
					
					if (chosen == null) {
						if (explorationExhausted.test(belief)) {
							// to terminate the repeat:
							return SUCCESS() ;
						}
						else {
							return exploring.apply(incrementalExplorationBudget) ;
						}
						
					}
					visited.add(chosen.id) ;
					return SEQ(
							gCandidateIsInteracted.apply(chosen.id),
							gTargetIsRefreshed.apply(tId), 
							/* check phi: */ 
							lift(phi)) ;
				}
				
				
		) ;
				
			
		return SEQ(gTargetIsRefreshed.apply(tId), 
				   TRYIF(phi,
						  /* then: */ SUCCESS(),
						  // else:
						  SEQ(REPEAT(search),
							  // G might terminate without succesfully establishing phi, so we check it again:
							  lift(phi))
					)) ;
		
	}

}
