package eu.iv4xr.framework.extensions.pathfinding;

import java.util.List;

/**
 * Just another interface for a graph-based pathfinder as an alternative 
 * to {@link Pathfinder}. In this interface, the graph is not made explicit.
 * E.g. it could be held in the state of an implementation of this interface.  
 *
 *  @author Wish
 */  
public interface Pathfinder2<NodeId> {

	/**
	 * Return a path between "from", to "to". The path is represented as a list, whose
	 * first element is the node "from", and its last element is "to".
	 * The method returns null if no path can be found between them. 
	 */
	public List<NodeId> findPath(NodeId from, NodeId to) ;
	
	default public boolean isReachableFrom(NodeId from, NodeId to) {
		return findPath(from,to) != null ;
	}
	
}
