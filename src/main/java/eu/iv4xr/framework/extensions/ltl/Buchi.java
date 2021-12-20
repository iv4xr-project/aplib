package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.utils.Pair;

/**
 * Represent a Buchi automaton. An "execution" of such an automaton
 * is a path through the automaton, starting with its initial state. Only a single
 * initial state is allowed. A Buchi automaton defines acceptance over infinite 
 * executions. So, it decides if a given infinite execution is accepted or else
 * rejected. As such, a Buchi automaton can be seen as representing some
 * predicate over infinite executions. 
 * 
 * <p>In the context of 'model checking' a Buchi automaton is used to check
 * if another program (called 'environment') can produce an execution that would
 * be accepted by the Buchi. To check this, the Buchi would the be executed
 * 'together' with the environment. A transition in the Buchi is only possible
 * if the transition relation of the Buchi itself allows this, and if furthermore
 * the current state of the environment satisfies the guarding condition of 
 * the Buchi.
 * 
 * <p>Obviously checking an infinite sequence by literally iterating over it is
 * not possible. So a Buchi automaton decides acceptance based on a finite
 * segment that we will call <i>witness</i>. An infinite sequence is 'accepted'
 * if it can be obtained by 'extending' a witness. There are two concepts
 * of 'extending', explained below.
 *
 * <p>Formally we define a Buchi automaton as a structure B = (s0,S,R,O,F) where
 * S is a finite set of states, s0 is the automaton initial state. R is some function
 * describing the transitions in the automaton. A transition connect an state s
 * to some destination state t in S. But a transition is also labeled by a state-predicate
 * p, which is meant to be evaluated on the state of the 'environment' mentioned before. 
 * When the Buchi is executed together with this environment, a transition in the Buchi can only
 * be taken if its condition p evaluates to true when evaluated on the state of
 * this environment.
 * 
 * <p>O and F are subsets of S defining
 * acceptance, where O defines Buchi's usual omega-acceptance, and F defines non-omega
 * acceptance. The F-component deviates from the standard definition of Buchi.
 * It is also not necessary as F-acceptance can also be expressed using O-acceptance.
 * However, the F-component makes modeling with a Buchi a bit more intuitive.
 * 
 * <p>Acceptance is defined as follows:
 * 
 * <ul>
 * <li>A finite execution sigma that ends in a state in F is a <i> witness</i>. Any
 * infinite execution tau, of which sigma is a prefix, is considered as accepted
 * by the Buchi automaton.
 * 
 * <li>A finite execution sigma of the form s0++[o]++s1++[o], where s0 is some prefix,
 * s1 is some middle part, and o is a state in O, is a <i>witness</i>. Notice that
 * such a sigma contains thus a cycle that passes o. Any infinite execution tau, that
 * can be obtained from sigma by repeating its o-cycle is considered as accepted
 * by the Buchi automaton.
 * </ul>
 * 
 * @author Wish
 */
public class Buchi {
	
	//public Set<String> states = new HashSet<>() ;

	/**
	 * Contain all states-names, along to their mapping to integer indices as their
	 * assigned ids. Model check will operate on those ids instead of names.
	 */
	Map<String,Integer> states = new HashMap<>() ;
	
	/**
	 * decoder[i] gives the name of the state with assigned-id i.
	 */
	String[] decoder ;
	
	public int initialState ;
	public int currentState ;
	public Set<Integer> omegaAcceptingStates = new HashSet<>() ;
	public Set<Integer> traditionalAcceptingStates = new HashSet<>() ;
	
	public Map<Integer,List<Pair<BuchiTransition,Integer>>> transitions = new HashMap<>() ;
	
	/**
	 * When 'executing' this Buchi (transitioning from one state to another) this
	 * list io used to keep track the execution so far (the sequence of states of
	 * the Buchi that is passed by the execution).
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
	
	public Buchi() { }
	
	/**
	 * Set the states of this Buchi to the given states. 
	 * The method then returns this Buchi.
	 */
	public Buchi withStates(String ... stateNames) {
		int N = stateNames.length ;
		decoder = new String[stateNames.length] ;
		states.clear(); 
		for(int i=0; i<stateNames.length; i++) {
			states.put(stateNames[i],i) ;
			decoder[i] = stateNames[i] ;
		}
		return this ;
	}
	
	/**
	 * Add the given transition to this Buchi. The method then returns this Buchi.
	 * 
	 * @param from  The source-state of the transition.
	 * @param to    The destination-state of the transition.
	 * @param transitionId  Some id to identify the transition.
	 * @param condition  The semantic condition specifying when the transition can be taken.
	 */
	public Buchi withTransition(String from, String to, String transitionId, Predicate<IExplorableState> condition) {
		int fromId = states.get(from) ;
		int toId = states.get(to) ;
		var transition = new BuchiTransition() ;
		transition.id = transitionId ;
		transition.condition = condition ;
		var transitionGroup = transitions.get(fromId) ;
		if (transitionGroup == null) {
			transitionGroup = new LinkedList<Pair<BuchiTransition,Integer>>() ;
			transitions.put(fromId,transitionGroup) ;
		}
		transitionGroup.add(new Pair<BuchiTransition,Integer>(transition,toId)) ;
		return this ;
	}
	
	/***
	 * Set the initial state of this Buchi. The method then returns this Buchi.
	 */
	public Buchi withInitialState(String sinit) {
		initialState = states.get(sinit) ;
		return this ;
	}
	
	/**
	 * Set the set of omega-accepting states. The method then returns this Buchi.
	 */
	public Buchi withOmegaAcceptance(String ... Ostates) {
		omegaAcceptingStates.clear();
		for(var o : Ostates) {
			omegaAcceptingStates.add(states.get(o)) ;
		}
		return this ;
	}

	/**
	 * Set the set of non-omega-accepting states. The method then returns this Buchi.
	 */
	public Buchi withNonOmegaAcceptance(String ... NonOstates) {
		traditionalAcceptingStates.clear();
		for(var o : NonOstates) {
			traditionalAcceptingStates.add(states.get(o)) ;
		}
		return this ;
	}
	

	
	/**
	 * Reset this Buchi to its initial state.
	 */
	public void reset() {
		currentState = initialState ;
		currentExecution.clear();
		currentExecution.add(currentState) ;
	}
	
	/**
	 * Execute a transition to a new state. In favor for speed, this method won't
	 * check if the transition is actually possible on the current state. The caller
	 * is responsible for checking this prior to calling this method.
	 */
	public void transitionTo(int newBuchiState) {
		currentState = newBuchiState ;
		currentExecution.add(currentState) ;
	}
	
	/**
	 * Just returning the total number of transitions that this Buchi has.
	 */
	public int numberOfTransitions() {
		int n = 0 ;
		for (var trgroup : transitions.values()) {
			n += trgroup.size() ;
		}
		return n ;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer() ;
		buf.append("States (" + states.size() + ") :") ;
		int k = 0 ;
		for(var st : states.entrySet()) {
			buf.append("\n   " + st.getValue() + ":") ;
			if (st.getValue() == this.initialState) {
				buf.append(">") ;
			}
			buf.append(st.getKey().toString()) ;
			if(this.omegaAcceptingStates.contains(st.getValue())) {
				buf.append("  (OA)") ;
			}
			if(this.traditionalAcceptingStates.contains(st.getValue())) {
				buf.append("  (A)") ;
			}
			k++ ;
		}
		buf.append("\nTransitions (" + this.numberOfTransitions() + ") :") ;
		for(var trgroup : transitions.entrySet()) {
			var from = decoder[trgroup.getKey()] ;
			for (var tr : trgroup.getValue()) {
				var to = decoder[tr.snd] ;
				var tr_ = tr.fst.id ;
				buf.append("\n   " + from + " ---" + tr_ + "--> " + to)  ;
			}
		}
		return buf.toString() ;
	}
	
	/**
	 * Construct a new Buchi that is a "clone" of this Buchi. he states and transitions
	 * will be cloned, but the underlying predicates that form the transitions' conditions
	 * are not cloned.
	 */
	public Buchi treeClone() {
		var B = new Buchi() ;
		for (var st : this.states.entrySet()) {
			B.states.put(st.getKey(), st.getValue()) ;
		}
		B.decoder = Arrays.copyOf(this.decoder,this.decoder.length) ;
		for (var st : this.omegaAcceptingStates) {
			B.omegaAcceptingStates.add(st) ;
		}
		for (var st : this.traditionalAcceptingStates) {
			B.traditionalAcceptingStates.add(st) ;
		}
		B.withInitialState(this.decoder[this.initialState]) ;
		for (var trgroup : this.transitions.entrySet()) {		
			List<Pair<BuchiTransition,Integer>> outArrows = new LinkedList<>() ;
			B.transitions.put(trgroup.getKey(), outArrows) ;
			for(var tr : trgroup.getValue()) {
				var trClone = new BuchiTransition() ;
				trClone.id = "" + tr.fst.id ;
				trClone.condition = tr.fst.condition ;
				outArrows.add(new Pair<BuchiTransition,Integer>(trClone,tr.snd)) ;
			}
		}
		return B;
	}
	
	/**
	 * Rename the states in this Buchi, by adding the given string as a suffix to
	 * each state-name. It then returns the resulting Buchi.
	 */
	Buchi appendStateNames(String suffix) {
		var states__ = this.states ;
		this.states = new HashMap<String,Integer>() ;
		for(var st : states__.entrySet()) {
			String newName = st.getKey() + suffix ;
			this.states.put(newName,st.getValue()) ;
		}
		for (int k=0; k<decoder.length; k++) {
			decoder[k] = decoder[k] + suffix ;
		}
		return this ;
	}
	
	Buchi insertNewState(String st) {
		var oldDecoder = this.decoder ;
		this.decoder = new String[this.decoder.length+1] ;
		this.decoder[0] = st ;
		for(int k=0; k<oldDecoder.length; k++) {
			this.decoder[k+1] = oldDecoder[k] ;
		}
		for (var st_ : this.states.entrySet()) {
			this.states.put(st_.getKey(), st_.getValue() + 1) ;
		}
		this.states.put(st,0) ;
		
		this.initialState++ ;
		
		var oldOmegaAcceptingStates = this.omegaAcceptingStates ;
		this.omegaAcceptingStates = new HashSet<>() ;
		for (var s : oldOmegaAcceptingStates) {
			this.omegaAcceptingStates.add(s+1) ;
		}
		var oldTraditionalAcceptingStates = this.traditionalAcceptingStates ;
		this.traditionalAcceptingStates = new HashSet<>() ;
		for (var s : oldTraditionalAcceptingStates) {
			this.traditionalAcceptingStates.add(s+1) ;
		}
		
		var oldTransitions = this.transitions ;
		this.transitions = new HashMap<>() ;
		transitions.put(0, new LinkedList<Pair<BuchiTransition,Integer>>()) ;
		for (var trgroup : oldTransitions.entrySet()) {
			transitions.put(trgroup.getKey()+1, trgroup.getValue()) ;
			for (var tr : trgroup.getValue()) {
				tr.snd = tr.snd + 1 ;
			}
		}
		return this ;
	}
	
	/**
	 * Cause this Buchi to backtrack to the previous state in the current execution.
	 */
	public void backtrackToPreviousState() {
		if (currentExecution.size()<=1) {
			throw new IllegalArgumentException("There is no previous state to backtrack.") ;
		}
		currentExecution.remove(currentExecution.size() - 1) ;
		currentState = currentExecution.get(currentExecution.size() - 1) ;		
	}
	
	/**
	 * Check which of transitions of this Buchi, that go from from it current state,
	 * would be semantically possible/enabled given the concrete state of the environment
	 * with which this Buchi is executed.
	 */
	public List<Pair<BuchiTransition,Integer>> getEnabledTransitions(IExplorableState state) {
	
		var availableTransitions = transitions.get(currentState) ;
		
		return availableTransitions.stream()
		       . filter(tr -> tr.fst.condition.test(state))
		       . collect(Collectors.toList()) ;
	}

}
