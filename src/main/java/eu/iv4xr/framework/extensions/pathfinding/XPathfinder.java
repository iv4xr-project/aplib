package eu.iv4xr.framework.extensions.pathfinding;

import java.util.List;

public interface XPathfinder<NodeId> {
	
	/**
	 * Return a path between "from", to "to". The path is represented as a list, whose
	 * first element is the node "from", and its last element is "to".
	 * The method returns null if no path can be found between them. 
	 */
	public List<NodeId> findPath(NodeId from, NodeId to) ;
	
	default public boolean isReachableFrom(NodeId from, NodeId to) {
		return findPath(from,to) != null ;
	}
	
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
