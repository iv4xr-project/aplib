package eu.iv4xr.framework.extensions.pathfinding;

import java.util.List;

/**
 * Extension of a graph-based {@link Pathfinder2} that also provides methods supporting
 * exploration over the represented navigation graph. To do this, we have
 * concept of "seen nodes". A method to explore is provided, that will return
 * a list of nodes in the graph, that has been seen, and has at least one
 * neighbor that is still unseen.
 * 
 * <p>As in {@link Pathfinder2}, the graph is not made explicit.
 * E.g. it could be held in the state of an implementation of this interface.  
 * 
 * @author Wish
 */
public interface XPathfinder<NodeId> extends Pathfinder2<NodeId> {
	
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
	 * neighbor which has not been seen yet (unexplored).
	 */
	public List<NodeId> getFrontier() ;
	
	/**
	 * Return a path from the start node to either a frontier-node or an unexplored node
	 * (the implementation decides this). A frontier node is a node with at least
	 * one unexplored neighbor. Such a path should only traverse though explored nodes,
	 * except the last node which could be an unexplored node. It is up to the implementation 
	 * to guarantee (or at least make effort for it) that the returned path is navigable. 
	 * 
	 * <p>The chosen exploration target is expected to be the closest one to the given 
	 * start-node.
	 */
	default public List<NodeId> explore(NodeId startNode) {
		return explore(startNode,startNode) ;
	}
	
	/**
	 * Return a path from the start node to either a frontier-node or an unexplored node
	 * (the implementation decides this). A frontier node is a node with at least
	 * one unexplored neighbor. Such a path should only traverse though explored nodes,
	 * except the last node which could be an unexplored node. It is up to the implementation 
	 * to guarantee (or at least make effort for it) that the returned path is navigable. 
	 * 
	 * To help chosing which frontier or unexplored node to explore to, a 'heuristic node'
	 * is also given. The chosen exploration target is expected to be the closest one,
	 * among candidate targets, to the given heuristic node.
	 */
	public List<NodeId> explore(NodeId startNode, NodeId heuristicNode) ;
	
	
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
