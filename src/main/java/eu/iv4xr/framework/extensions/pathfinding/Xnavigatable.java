package eu.iv4xr.framework.extensions.pathfinding;

import java.util.List;


/**
 * Extension of {@link Pnavigatable} that also provides methods supporting
 * exploration over the represented navigation graph. To do this, we have
 * concept of "seen nodes". A method to explore is provided, that will return
 * a list of nodes in the graph, that has been seen, and has at least one
 * neighbor that is still unseen.
 * 
 * @author Wish
 */
public interface Xnavigatable<NodeId> extends Pnavigatable<NodeId> {
	
	/**
	 * Check is the given node is marked as "has been seen".
	 */
	public boolean hasbeenSeen(NodeId nd) ;

	/**
	 * Mark the given node as "has been seen".
	 */
	public void markAsSeen(NodeId id) ;

	/**
	 * Mark a bunch of nodes as "has been seen".
	 */
	default public void markAsSeen(List<NodeId> newlyseen) {
		for(NodeId p : newlyseen) {
			markAsSeen(p) ;
		}
	}
	
	/**
	 * Return frontier-nodes. A frontier is a seen node, with at least one
	 * neighbor which has not been seen yet.
	 */
	public List<NodeId> getFrontier() ;
	
	/**
	 * Return a path from the start node to a frontier-node. Such a path should only
	 * traverse though seen nodes. It is up to the implementation to decide
	 * which frontier-node is chosen, e.g. it could choose the one closest to the start
	 * node. 
	 */
	public List<NodeId> explore(NodeId startNode) ;
	
	/**
	 * When true then the pathfinder will consider all nodes in the graph to have been seen.
	 */
	public boolean usingPerfectMemoryPathfinding() ;
	
	/**
	 * When true then the pathfinder will consider all nodes in the graph to have been seen.
	 */
	public void setPerfectMemoryPathfinding(Boolean flag) ;
	
	/** 
	 * Clear the marking of "has been seen". So, after this all nodes are again considered
	 * as has not been seen.
	 */
	public void wipeOutMemory() ;

}
