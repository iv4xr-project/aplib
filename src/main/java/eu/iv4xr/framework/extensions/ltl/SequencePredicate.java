package eu.iv4xr.framework.extensions.ltl;

import java.util.List;


/**
 * A general class representing a predicate over finite sequences. Since
 * we typically use such a sequence to represent an execution of some 
 * system through a series of states, elements of the sequence will be
 * called 'states'. These states can be thought as representing
 * the series of states passed by some execution of a system.
 * 
 * Note this class does not in itself restrict what a 'state' is. For example,
 * if we need to express a predicate over pairs of (transition,state), then
 * we can use a sequence of such pairs as our 'path'. 
 * 
 * There are two modes to check a sequence:
 * 
 *    (1) Invoke sat(sequence)
 *    (2) However, giving a sequence of states is not always possible. Assuming
 *        we can still give the states one at a time, we can do it like this:
 *        
 *        (a) invoke startChecking()
 *        (b) feed the state one at a time using checkNext(state)
 *        (c) at the end invoke endChecking()
 *        (d) invoke sat() to get the result.
 *        
 */
public abstract class SequencePredicate<State> {
	
	/**
     * Invoke this first before start checking this predicate on a sequence.
     */
    abstract public void startChecking() ;
	
	/**
     * Use this to check this predicate on a sequence of states by feeding the 
     * states one state at a time. The typical setup is if the execution under evaluation 
     * does not allow states to be serialized or cloned. This means that we cannot collect
     * those states in some collection and therefore the execution has to be checked
     * incrementally by feeding this predicate one state (the current state) at a time.
     */
    abstract public void checkNext(State state) ;
    
    /**
     * Call this to mark that the last state of the execution under evaluation has
     * been fed to this predicate. So, we can now call sat() to inspect whether this
     * predicate holds on the execution or not.
     */
    abstract public void endChecking() ;
    
    /**
     * Return the result of the checking whether this predicate holds on the last
     * sequence given to it. The the sequence was given one state at a time using
     * checkNext(), sat() should be called after endChecking() was called.
     * 
     * The method returns SAT if this predicate holds on the sequence (it is satisfied
     * by the sequence). 
     * UNSAT if the sequence does not satisfy this predicate.
     * UNKNOWN if neither SAT nor UNSAT can be decided.
     */
    public abstract SATVerdict sat();
    
    /**
     * Check is the given sequence of states (representing some execution) satisfies
     * this LTL formula. This method will implicitly call startChecking() and
     * endChecking(). A verdict is returned, saying whether this predicate holds on
     * the given sequence.
     * 
     * The method returns SAT if this predicate holds on the sequence (it is satisfied
     * by the sequence). 
     * UNSAT if the sequence does not satisfy this predicate.
     * UNKNOWN if neither SAT nor UNSAT can be decided.
     * 
     * Note that the states in the sequence typically represent the states of some
     * system at different points in time, and as such they should not contain shared 
     * sub-objects, or else the evaluation a sequence predicate may give an unsound
     * result.
     */
    public SATVerdict  sat(List<State> sequenceOfStates) {
    	startChecking() ;
    	for(State st : sequenceOfStates) {
    		checkNext(st) ;
    	}
    	endChecking() ;
    	return sat() ;
    }
    
	public enum SATVerdict {
	    SAT, UNSAT, UNKNOWN
	}

}
