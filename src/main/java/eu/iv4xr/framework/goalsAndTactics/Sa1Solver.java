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

public class Sa1Solver {
	
	/**
	 * A function to check if an entity is reachable from the agent's position. The
	 * agent is not specified explicitly, but instead the function takes an agent-state
	 * as input. "The agent" is the agent that "owns" this state, and its id is
	 * embedded in that state.
	 */
	public BiFunction<Iv4xrAgentState,WorldEntity,Boolean> reachabilityChecker ;
	
	/**
	 * A function that returns the "distance" between an entity and the agent. 
	 */
	public BiFunction<Iv4xrAgentState,WorldEntity,Float> distanceToAgent ;
	public Function<Iv4xrAgentState,BiFunction<WorldEntity,WorldEntity,Float>> distanceBetweenEntities ;
	public Function<String,GoalStructure> gCandidateIsInteracted ;
	public Function<String,GoalStructure> gTargetIsRefreshed ;
	public Predicate<Iv4xrAgentState> explorationExhausted ;
	public Function<Integer,GoalStructure> exploring ;
	
	public Sa1Solver() { }
	
	public Sa1Solver(BiFunction<Iv4xrAgentState, WorldEntity, Boolean> reachabilityChecker,
			BiFunction<Iv4xrAgentState ,WorldEntity,Float> distanceToAgent,
			Function<Iv4xrAgentState ,BiFunction<WorldEntity,WorldEntity,Float>> distanceFunction,
			Function<String, GoalStructure> gCandidateIsInteracted, 
			Function<String, GoalStructure> gTargetIsRefreshed,
			Predicate<Iv4xrAgentState> explorationExhausted,
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
			Iv4xrAgentState belief, 
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

		
	
	/**
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
	 * This tries to make the environment to move to a state where the entity tId satisfies the 
	 * predicate phi as a goal. The algorithm is as follows:
	 * 
	 * <ol>
	 *   <li> The agent moves to tId until it can see it. 
	 *   <li> It checks the predicate phi. If it is satisfied we are done.
	 *   <li> Else, the agent check if there is a reachable entity e whose properties satisfy 
	 *   the given selector, and not yet tried before. If there is such an e, the agent
	 *   moves to it and interacts with it. Then it repeats from step-1.
	 *   <li> If there is no such e in step-3, the agent checks if there the world has some
	 *   unvisited place (has some area to explore). If so, the agent explores for some budget,
	 *   then we 
	 * it should be a "switch"). This e is interacted
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
