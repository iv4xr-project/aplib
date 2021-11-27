package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.ltl.BasicModelChecker.MCStatistics;
import eu.iv4xr.framework.extensions.ltl.BasicModelChecker.Path;
import nl.uu.cs.aplib.utils.Pair;

/**
 * Provide an explicit-state bounded and lazy model checker that can be used to
 * check if a model M has an execution, of up to some maximum length, that would
 * satisfy a certain property. The property is encoded as a Buchi automaton B.
 * We implement the double DFS model checking algorithm, similar to the one used
 * by the SPIN model checker.
 * 
 * <p>An infinite execution of M satisfies this property if it is accepted by the 
 * Buchi automated B. B does not literally check infinite executions, as this would
 * be impossible. Instead, B is used to look for a witness that it can accept.
 * A witness is a finite execution, but it implicitly extensible to a set of
 * infinite exectutions, all of them would be accepted by B. The model-checker
 * only needs to find such a witness. 
 * 
 * <p> For practicality, the model-checking is made 'bounded'. This means that it only
 * searches for witness of some given maximum length. This allows us to handle
 * an M that has infinite state space.
 * 
 * <p>When a witness is found (so M can produce an infinite execution that satisfies B),
 * the witness will be returned. Note that failing
 * to find such a witness means that the negation of the property induced by B
 * therefore holds globally on all executions of M within the maximum depth from the
 * model's initial state.
 * 
 * <p>The model checker can be used to target any 'program' or 'model of program'
 * that implements the interface {@link ITargetModel}.
 * 
 * @author Wish
 * 
 */
public class BuchiModelChecker {
	
	/**
	 * The 'program' or 'model of a program' that we want to target in model-checking.
	 */
	public ITargetModel model ;
	
	/**
	 * Hold some basic statistics over the last model-checking run.
	 */
    public MCStatistics stats = new MCStatistics() ;
	
	
	public BuchiModelChecker(ITargetModel model) {
		this.model = model ;
	}
	
	/**
	 * Check if the target 'program' {@link #model} can produce an infinite execution that
	 * would be accepted by the given Buchi automaton. If so SAT is returned, and else UNSAT.
	 */
	public SATVerdict sat(Buchi buchi) {
		var path = find(buchi,Integer.MAX_VALUE) ;
		if(path == null) return SATVerdict.UNSAT ;
		return SATVerdict.SAT ;
	}
	
	/**
	 * Check if the target 'program' {@link #model} can produce an infinite execution that
	 * would be accepted by the given Buchi automaton. If so a witness is returned, and else
	 * null.
	 * 
	 * <p>A 'witness' is a finite execution, that can be 'extended' to a finite execution
	 * accepted by the Buchi. Further explanation about this can be found here: {@link Buchi}.
	 */
	public Path<Pair<IExplorableState,String>> find(Buchi buchi, int maxDepth) {
		
		model.reset();
		stats.clear(); 
		buchi.reset();
		
		IExplorableState targetInitialState = model.getCurrentState() ;
		Set<Pair<IExplorableState,Integer>> visitedStates = new HashSet<>() ;
		
		// although the model only has one initial state, its lock-step execution
		// with the buchi may have multiple starting states. We will first
		// quantify over these:
		
		var initialEnabledBuchiTransitions = buchi.getEnabledTransitions(targetInitialState) ;
		if (initialEnabledBuchiTransitions.isEmpty()) return null ;
		
		// quantify over all possible initial transitions of Buchi:
		for(var tr0 : initialEnabledBuchiTransitions) {
			
			buchi.transitionTo(tr0.snd);
			
			// combined initial state (of model x buchi) :
			Pair<IExplorableState,Integer> state = new Pair<>(targetInitialState,buchi.currentState) ;
			Path<Pair<IExplorableState,Integer>> pathSoFar = new Path<>() ;
			pathSoFar.addInitialState(state);
			
			Set<Pair<IExplorableState,Integer>> statesInPathToStartOfPossibleCycle = new HashSet<>() ;
			
			// start DFS on the initial state:
			Path<Pair<IExplorableState,Integer>> witnessPath = dfsBuchi(
					buchi,
					pathSoFar,
					statesInPathToStartOfPossibleCycle,
					visitedStates,
					state,
					maxDepth) ;
			
			// If not null, we found a satisfying execution and can return it.
			// However, first translate the path to show Buchi-states' names rather than their number;
			// then return it:
			if (witnessPath != null) {
				Path<Pair<IExplorableState,String>> witness_ = new Path<>() ;
				// compiler complains when using strea().map; so doing it the old way:
				for(var step : witnessPath.path) {
					witness_.addTransition(step.fst, new Pair<>(step.snd.fst, buchi.decoder[step.snd.snd]));
				}
				return witness_ ;
			}
			// back-track:
			buchi.backtrackToPreviousState();
			// loop back to start the DFS on then next initial state...
		}
		// if we come this far then no satisfying execution was found
		return null ;
	}
		
	/**
	 * The worker of our LTL model-checking algorithm. It is a depth-first search
	 * (DSF) algorithm. The same algorithm as used in the SPIN model checker.
	 * It actually perforsm a double-DFS search. To search a witness (an execution
	 * of the 'program' that would be accepted by the given Buchi), the algorithm
	 * uses DSF to find either a reachable state that would be non-omega-accepted by
	 * the Buchi, or to find a reachable cycle that is omega-accepted by the Buchi.
	 * In the second case, DFS is used to find an O-accepting state o, and then
	 * a second DSF is used if to check if there is a path that starts from o
	 * and cycle back to o (which means that we would then have omega acceptance).
	 * 
	 * @param buchi                              The Buchi automaton that defines
	 *                                           the property to check.
	 * @param pathSoFar                          The execution that leads to current
	 *                                           state to explore (the state
	 *                                           parameter below; so it is the path
	 *                                           up to and including that state).
	 *                                           
	 * @param statesInPathToStartOfPossibleCycle The set of states to the start of a
	 *                                           possible cycle over omega-accepting
	 *                                           states. The start of such cycle is
	 *                                           always an omega-accepting state. We
	 *                                           keep track of this information to
	 *                                           make cycle detection more
	 *                                           efficient.
	 * 
	 * @param visitedStates                      The set of states that have been
	 *                                           visited by the DFS.
	 * @param state                              The current state to explore next.
	 * @param remainingDepth                     The remaining depth. When this
	 *                                           becomes 0, the DFS will not explore
	 *                                           the given state (the recursion will
	 *                                           just return with null).
	 * 
	 * @return An execution path (whose length is at most the specified depth),
	 *         satisfying the property described by Buchi, if there is one.
	 */
	Path<Pair<IExplorableState,Integer>>  dfsBuchi(
			Buchi buchi, 
			Path<Pair<IExplorableState,Integer>> pathSoFar, 
			Set<Pair<IExplorableState,Integer>> statesInPathToStartOfPossibleCycle,
			Set<Pair<IExplorableState,Integer>> visitedStates,
			Pair<IExplorableState,Integer> state,
			int remainingDepth			
			) 
	{	
		if(remainingDepth==0) return null ;

		stats.numberOfTransitionsExplored++ ;
		
		//System.out.println(">>> #visited states: " + visitedStates.size()) ;
		//for(var st : visitedStates) {
		//	System.out.println("     " + st) ;
		//}
		
		if(visitedStates.contains(state)) {
			return null ;
		}
		// else the state is new
		stats.numberOfStatesExplored++ ;
		visitedStates.add(state) ;
		
		// checking non-omega acceptance:
		if(buchi.traditionalAcceptingStates.contains(state.snd)) {
			// This implies that the path-so-far is ACCEPTED by Buchi (via non-omega
			// acceptance).
			// Return the path:
			return pathSoFar.copy() ;	
		}
		
		// checking omega acceptance:
		if(statesInPathToStartOfPossibleCycle.contains(state)) {
			//This implies that the path-so-far is ACCEPTED by Buchi (via omega acceptance).
			// Return the path:
			return pathSoFar.copy() ;	
		}
		
		// now we recurse to successor-states:
		
		var enabledModelTransitions = model.availableTransitions() ;
		var enabledBuchiTransitions = buchi.getEnabledTransitions(model.getCurrentState()) ;
			
		for(var trBuchi : enabledBuchiTransitions) {
			
			buchi.transitionTo(trBuchi.snd);			
			
			for (var trModel : enabledModelTransitions) {
				model.execute(trModel);
				// so now we have executed both the model and the Buchi;
				// construct the combine next-state:
				var modelNextState = model.getCurrentState() ;
				Pair<IExplorableState,Integer> combinedNextState = new Pair<>(modelNextState,buchi.currentState) ;			
				
				// extending path-sofar:
				pathSoFar.addTransition(trModel, combinedNextState);
				
				// if the state is an omega accepting state of the Buchi, we then
				// start cycle detection:
				if(buchi.omegaAcceptingStates.contains(buchi.currentState)) {
					// we will do a fresh DFS run with its own tacking of visited states:
					Set<Pair<IExplorableState,Integer>> freshVisitedStates = new HashSet<>() ;
					Set<Pair<IExplorableState,Integer>> statesInPathSoFar = new HashSet<>() ;
					statesInPathSoFar.addAll(pathSoFar.getStateSequence()) ;
					// BUT REMOVE the combinedNextState from this set, the DFS recursion
					// below will wrongly say it finds a cycle:
					statesInPathSoFar.remove(combinedNextState) ;
					
					var path = dfsBuchi(buchi,
							pathSoFar,
							statesInPathSoFar, 
							freshVisitedStates, 
							combinedNextState, 
							remainingDepth-1) ;
					
					if (path != null) {
						// if a solution was found, return it:
						return path ;
					}
				}
				// at this point either cycle-detection above has returned with no
				// solution, or we are in a DFS that needs to explore to the next state.
				// Either case we need to recurse to the next state:
				
				var path = dfsBuchi(buchi,
						pathSoFar,
						statesInPathToStartOfPossibleCycle,
						visitedStates,
						combinedNextState,
						remainingDepth-1) ;
				if(path != null) {
					// a solving path is found! Return the path:
					return path ;
				}	
				pathSoFar.removeLastTransition();
				model.backTrackToPreviousState() ;
			}
			
			buchi.backtrackToPreviousState();
			
		}
		// if we reach this point then no solution was found:
		return null ;
	}
	

	

}
