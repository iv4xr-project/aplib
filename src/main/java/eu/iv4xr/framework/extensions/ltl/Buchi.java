package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.utils.Pair;

/**
 * Represent a "variation" of Buchi automaton. An "execution" of such an automaton
 * is a path through the automaton, starting with its initial state. Only a single
 * initial state is allowed. 
 * 
 * <p>We only executions of finite length, though when a finite execution contains
 * cycles (which is the case if the execution contains nodes that appear multiple
 * times) then it also induces infinite execution(s).
 * 
 * <p>Formally our Buchi automaton can be thought as a structure B = (s0,S,R,O,F) where
 * S is a finite set of states, s0 is the automaton initial state. R is some function
 * describing the transitions in the automaton. O and F are subsets of S defining
 * acceptance, where O defines Buchi's usual omega acceptance, and F defines traditional
 * FSM acceptance.
 * 
 * <p>An execution sigma is accepted by B if either sigma contains a state in F, or
 * if sigma contains a cycle that visits some state in O multiple times.
 */
public class Buchi {
	
	public Set<String> states = new HashSet<>() ;

	Map<String,Integer> encoder ;
	String[] decoder ;
	
	public int initialState ;
	public int currentState ;
	public Set<Integer> omegaAcceptingStates = new HashSet<>() ;
	public Set<Integer> traditionalAcceptingStates = new HashSet<>() ;
	
	public Map<Integer,List<Pair<BuchiTransition,Integer>>> transitions = new HashMap<>() ;
	
	
	/**
	 * The execution so far.
	 */
	public List<Integer> currentExecution = new LinkedList<>() ;
	
	public static class BuchiTransition implements ITransition {
		
		public String id ;
		public Predicate<IExplorableState> condition ;
		
		@Override
		public String getId() {
			return id;
		}
		
	}
	
	public void reset() {
		currentState = initialState ;
		currentExecution.clear();
		currentExecution.add(currentState) ;
	}
	
	/**
	 * Take the transition to a new state. In favor for speed, it won't check if the transition is 
	 * actually possible on the current state. The caller is responsible for checking
	 * this prior to calling this method.
	 */
	public void transitionTo(int newBuchiState) {
		currentState = newBuchiState ;
		currentExecution.add(currentState) ;
	}
	
	public void backtrackToPreviousState() {
		if (currentExecution.size()<=1) {
			throw new IllegalArgumentException("There is no previous state to backtrack.") ;
		}
		currentExecution.remove(currentExecution.size() - 1) ;
		currentState = currentExecution.get(currentExecution.size() - 1) ;		
	}
	
	public List<Pair<BuchiTransition,Integer>> getEnabledTransitions(IExplorableState state) {
		var availableTransitions = transitions.get(currentState) ;
		
		return availableTransitions.stream()
		       . filter(tr -> tr.fst.condition.test(state))
		       . collect(Collectors.toList()) ;
	}
	

}
