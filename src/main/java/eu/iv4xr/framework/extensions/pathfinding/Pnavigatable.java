package eu.iv4xr.framework.extensions.pathfinding;

import java.util.List;

/**
 * An extension of {@link Navigatable}. Like Navigatable, a Pnavigatable
 * represents a graph-like structure that we can navigate through. While
 * Navigatable provides methods e.g. for finding out who the neighbors of
 * a node, what the distance between neighbors, it does not provide a method
 * for finding a path between two nodes. Typically, a pathfinder algorithm
 * is then needed to find such a path.
 * 
 * <p>A Pnavigatable provides such a pathfinding method. This implies that
 * an implementation of this interface must thus internally implement a pathfinding
 * algorithm. You can for example plug-in {@link AStar}.
 *
 *  @author Wish
 */  
public interface Pnavigatable<NodeId> extends Navigatable<NodeId> {

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
