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
	
	public Function<Iv4xrAgentState,BiFunction<Vec3,Vec3,Boolean>> reachabilityChecker ;
	public Function<String,GoalStructure> gCandidateIsInteracted ;
	public Function<String,GoalStructure> gTargetIsRefreshed ;
	public Tactic exploreTactic ;
	
	public Sa1Solver() { }
	
	public Sa1Solver(Function<Iv4xrAgentState, BiFunction<Vec3, Vec3, Boolean>> reachabilityChecker,
			Function<String, GoalStructure> gCandidateIsInteracted, 
			Function<String, GoalStructure> gTargetIsRefreshed,
			Tactic exploreTactic) {
		this.reachabilityChecker = reachabilityChecker;
		this.gCandidateIsInteracted = gCandidateIsInteracted;
		this.gTargetIsRefreshed = gTargetIsRefreshed;
		this.exploreTactic = exploreTactic;
	}
	
	public Sa1Solver(IInteractiveWorldGoalLib goalLib,
			IInteractiveWorldTacticLib tacticLib,
			Function<Iv4xrAgentState, BiFunction<Vec3, Vec3, Boolean>> reachabilityChecker) {
		this.reachabilityChecker = reachabilityChecker;
		this.gCandidateIsInteracted = id -> goalLib.entityInteracted(id) ;
		this.gTargetIsRefreshed = id -> goalLib.entityStateRefreshed(id);
		this.exploreTactic = tacticLib.explore() ;
	}
	
	
	
	
	/**
	 * Perform exploration for the given budget. It will explore until there is nothing left to
	 * explore, or until the budget runs out. The goal never fails.
	 */	
	GoalStructure pgExplore(int budget) {
		
		GoalStructure explr = goal("exploring (persistent-goal: aborted when it is terminated)").toSolve(belief -> false)
				.withTactic(FIRSTof(exploreTactic,
						    ABORT()))
				.lift()
				.maxbudget(budget)
				;
		
		return FIRSTof(explr, SUCCESS()) ;
		
	}
	
	/**
	 * Perform a single cycle explore; will abort the goal if there is no space to explore.
	 * This is essentially used to check whether exploration is exhausted.
	 */
	GoalStructure gExplore1() {
		return goal("can explore").toSolve(belief -> true)
				.withTactic(
					FIRSTof(exploreTactic, 
							ABORT()))
				.lift() ;
	}
	
	
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
		GoalStructure NearestInteracted = DEPLOY(agent,
				
				(Iv4xrAgentState belief) -> {
					
					
					List<WorldEntity> candidates = belief.worldmodel.elements.values()
							.stream()
							.filter(e -> selector.test(e) 
									&& ! visited.contains(e.id)
									&& reachabilityChecker.apply(belief).apply(belief.worldmodel.position, e.position)
									) 
							.collect(Collectors.toList());
					if (candidates.isEmpty()) {
						return FAIL() ;
					}
					
					switch(policy) {
						case NEAREST_TO_AGENT: 
							candidates.sort((e1,e2) -> Float.compare(Vec3.distSq(e1.position, belief.worldmodel.position),
							         Vec3.distSq(e2.position, belief.worldmodel.position)  
							   )) ;
							break ;
						case  NEAREST_TO_TARGET:
							var target = belief.worldmodel.elements.get(tId) ;
							// should not be null!
							if(target == null) throw new IllegalArgumentException() ;
							candidates.sort((e1,e2) -> Float.compare(Vec3.distSq(e1.position, target.position),
							         Vec3.distSq(e2.position, target.position)  
							   )) ;
							break ;
						case RANDOM:
							break ;
					}
					
					WorldEntity chosen = candidates.get(0) ;	
					if (policy == Policy.RANDOM) {
						chosen = candidates.get(rnd.nextInt(candidates.size())) ;
					}
					visited.add(chosen.id) ;
					return gCandidateIsInteracted.apply(chosen.id) ;
				}
		) ;
				
					
		var G = REPEAT(
				   IFELSE2(NearestInteracted, // deploy a dynamic goal
				           // if that worked then:
				           SEQ(gTargetIsRefreshed.apply(tId), /* check phi: */ lift(phi)),
				           // else:
				           IFELSE2(gExplore1(),
				        	  // then:
				        	  SEQ(pgExplore(incrementalExplorationBudget), FAIL()),
				        	  // else, there is nothing left to explore, we end the repeat-loop:
				        	  SUCCESS())
				 )) ;
		
		return SEQ(gTargetIsRefreshed.apply(tId), 
				   IFELSE(phi,
						  /* then: */ SUCCESS(),
						  // else:
						  SEQ(G,
							  // G might terminate without succesfully establishing phi, so we check it again:
							  lift(phi))
					)) ;
		
	}

}
