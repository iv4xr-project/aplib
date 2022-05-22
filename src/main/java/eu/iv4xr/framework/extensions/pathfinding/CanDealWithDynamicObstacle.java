package eu.iv4xr.framework.extensions.pathfinding;

/**
 * A feature of a navigation graph that allows obstacles to be added. An obstacle
 * has a state that can be toggled to set it to the blocking-state and non-blocking
 * state. When non-blocking it will not hamper navigation, otherwise it will.
 */
public interface CanDealWithDynamicObstacle<Obstacle> {
	
	public void addObstacle(Obstacle o) ;
	
	public void removeObstacle(Obstacle o) ;
	
	/**
	 * Return the state of this obstacle. True means that it is in the blocking state.
	 */
	public boolean isBlocking(Obstacle o) ;
	
	/**
	 * Change the state of this obstacle to blocking.
	 */
	public void toggleBlockingOn(Obstacle o) ;

	/**
	 * Change the state of this obstacle to non-blocking.
	 */
	public void toggleBlockingOff(Obstacle o) ;
	

}
