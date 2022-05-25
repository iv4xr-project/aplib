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
	 * Set the the state of this obstacle to blocking or unblocking.
	 * 
	 * @param isBlocking If true then the obstacle will be set to its blocking state, 
	 * 			and else non-blocking.
	 */
	public void setBlockingState(Obstacle o, boolean isBlocking) ;
	
	/**
	 * Change the state of this obstacle to blocking.
	 */
	default public void toggleBlockingOn(Obstacle o) {
		setBlockingState(o,true) ;
	}

	/**
	 * Change the state of this obstacle to non-blocking.
	 */
	default public void toggleBlockingOff(Obstacle o) {
		setBlockingState(o,false) ;
	}
	

}
