package eu.iv4xr.framework.extensions.ltl;

/**
 * An LTL formula is evaluated on a sequence of 'states' (to be more precise, a sequence of
 * pairs (states,transitions)). For the LTL formula, states are represented by instances of 
 * this interface. Implementing this interface on your class X will allow LTL formulas to 
 * be interpreted in a sequence of instances of X. 
 */
public interface IState {
	
	/**
	 * Produce a string representation of the state for the purpose of printing it
	 * e.g. when needed to print a witnessing execution trace that contains the state.
	 * It is not required that the string representation fully reflects the state.
	 */
	String showState() ;
	
}
