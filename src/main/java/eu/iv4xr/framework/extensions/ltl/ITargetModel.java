package eu.iv4xr.framework.extensions.ltl;

import java.util.List;

public interface ITargetModel {

	/**
	 * Reset the model to its initial state.
	 */
	public void reset() ;

	/**
	 * Get a clone of the current state.
	 */
	public IExplorableState getCurrentState() ;

	/**
	 * Rall back to the previous state. Return false if there is no previous state.
	 */
	public boolean backTrackToPreviousState() ;

	/**
	 * Return a list of all available transitions from the current state.
	 */
	public List<ITransition> availableTransitions() ;

	/**
	 * Execute a transition. The transition must be one of the transitions returned
	 * by {@link #availableTransitions()}.
	 */
	public void execute(ITransition tr) ;

}
