package eu.iv4xr.framework.goalsAndTactics;

import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.mainConcepts.Tactic;

/**
 * 
 * This interface lists/suggests some commonly useful tactics for an agent to
 * go about in an interactive world.
 * 
 * @author Wish
 */
public interface IInteractiveWorldTacticLib<Location> {
	

	/**
	 * Construct a tactic to auto-navigate to the given location. It is enabled
	 * when this navigation is possible, and else the tactic is not
	 * enabled.
	 */
	public Tactic navigateToTac(Location location) ;
	
	/**
	 * Construct a tactic to auto-navigate to a world-entity with the given id.
	 * It is enabled when the entity exists in the current state, and
	 * if navigation to it is possible. Else the tactic is not enabled.
	 */
	public Tactic navigateToTac(String entityId) ;
	
	/**
	 * Construct a tactic to interact with a world-entity with the given id. It is
	 * enabled when the exists in the current state, and
	 * if interacting with it is possible. Else the tactic is not enabled.
	 */
	public Tactic interactTac(String entityId) ;
	
	/**
	 * Construct a tactic to auto-explore the world. It is enabled when there is a
	 * place to explore, and else it is disabled. If a heuristic-location is given,
	 * the exploration is driven towards that location.
	 */
	public Tactic explore(Location heuristicLocation) ;

	/**
	 * Return true when in the given state (representing the current state) 
	 * there is no more place to explore.
	 */
	public boolean explorationExhausted(SimpleState state) ;
		
	
	
}
