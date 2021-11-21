package eu.iv4xr.framework.extensions.ltl;

/**
 * An LTL formula is evaluated on a sequence of pairs (states,transitions). For the LTL formula, 
 * transitions are represented by instances of this interface. Implementing this interface on
 * your class T will allow LTL formulas to be interpreted on instances of T as 'transitions'.
 */
public interface ITransition {
	
	/**
	 * An ID that identifies the transition.
	 */
	String getId() ;

}
