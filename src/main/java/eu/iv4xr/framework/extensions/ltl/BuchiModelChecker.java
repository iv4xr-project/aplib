package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.ltl.BasicModelChecker.Path;
import nl.uu.cs.aplib.utils.Pair;

/**
 * Provide an explicit-state bounded and lazy model checker that can be used to
 * check if a model M has an execution, of up to some maximum length, that would
 * also be accepted by a given Buchi automaton B. The automaton B can be seen as
 * capturing some property of interest about the executions of M. If a matching
 * execution can be found, it can be retrieved as a witness. Note that failing
 * to find such a witness means that the negation of the property induced by B
 * therefore holds globally on all states within the maximum depth from the
 * model's initial state.
 * 
 * @author Wish
 *
 */
public class BuchiModelChecker {
	
	public ITargetModel model ;
	
    public MCStatistics stats = new MCStatistics() ;
	
	public static class MCStatistics {
		public int numberOfStatesExplored = 0 ;
		public int numberOfTransitionsExplored = 0 ;
		
		public void clear() {
			numberOfStatesExplored = 0 ;
			numberOfTransitionsExplored = 0 ;
		}
		
		@Override
		public String toString() {
			return "Number of states explored: " + numberOfStatesExplored
					+ "\nNumber of transitions explored: " + numberOfTransitionsExplored ;
		}

	}
	
	public BuchiModelChecker(ITargetModel model) {
		this.model = model ;
	}
	
	public SATVerdict sat(Buchi buchi) {
		var path = find(buchi,Integer.MAX_VALUE) ;
		if(path == null) return SATVerdict.UNSAT ;
		return SATVerdict.SAT ;
	}
	
	public Path<Pair<IExplorableState,String>> find(Buchi buchi, int maxDepth) {
		
		model.reset();
		stats.clear(); 
		buchi.reset();
		
		IExplorableState targetInitialState = model.getCurrentState() ;
		var initialEnabledBuchiTransitions = buchi.getEnabledTransitions(targetInitialState) ;
		
		if (initialEnabledBuchiTransitions.isEmpty()) return null ;
		
		Collection<Pair<IExplorableState,Integer>> visitedStates = new HashSet<>() ;
		for(var tr0 : initialEnabledBuchiTransitions) {
			
			buchi.transitionTo(tr0.snd);
			
			Pair<IExplorableState,Integer> state = new Pair(targetInitialState,buchi.currentState) ;
			Path<Pair<IExplorableState,Integer>> pathSoFar = new Path<>() ;
			pathSoFar.addInitialState(state);
			
			Path<Pair<IExplorableState,Integer>> witnessPath = dfsBuchi(
					buchi,
					pathSoFar,
					visitedStates,
					state,
					maxDepth) ;
			
			if (witnessPath != null) {
				Path<Pair<IExplorableState,String>> witness_ = new Path<>() ;
				witness_.path = witnessPath.path.stream()
						.map(step -> new Pair(step.fst, // the transition leading to the new state
								              // the new state is a pair of state in target and state in Buchi:
								              new Pair(step.snd.fst, buchi.decoder[step.snd.snd])))
						.collect(Collectors.toList()) ;
				
				return witness_ ;
			}
			
			buchi.backtrackToPreviousState();
		}
		// if we come this far then no satisfying execution was found
		return null ;
	}
	
	Path<Pair<IExplorableState,Integer>>  dfsBuchi(
			Buchi buchi, 
			Path<Pair<IExplorableState,Integer>> pathSoFar, 
			Collection<Pair<IExplorableState,Integer>> visitedStates,
			Pair<IExplorableState,Integer> state,
			int remainingDepth			
			) {	
		// TODO
		throw new UnsupportedOperationException() ;
	
	}
	

}
