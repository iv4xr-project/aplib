package eu.iv4xr.framework.extensions.pathfinding;

import java.util.*;
import java.util.function.BiFunction;

/**
 * A simple implementation of {@link Navigatable}.
 */
public class NavGraph<NodeId> implements Navigatable<NodeId> {
	
	public Set<NodeId> nodes = new HashSet<>() ;
	public Map<NodeId,Set<NodeId>> edges = new HashMap<>() ;
	
	/**
	 * You can give a function to calculate a heuristic distance here. If this
	 * function is not given (null), the heuristic distance will always be 0.
	 */
	public BiFunction<NodeId,NodeId,Float> heuristicDistance ;
	
	/**
	 * You can give a function to calculate the actual distance between two neighbors here.
	 * If the function is not given (null), the distance between neighbors will be assumed
	 * to always be 1.
	 */
	public BiFunction<NodeId,NodeId,Float> neighborDistance ;

	@Override
	public Iterable<NodeId> neighbours(NodeId id) {
		return edges.get(id) ;
	}

	@Override
	public float heuristic(NodeId from, NodeId to) {
		if (heuristicDistance==null) return 0f ;
		return heuristicDistance.apply(from, to) ;
	}

	@Override
	public float distance(NodeId from, NodeId to) {
		if (neighborDistance == null) return 1f ;
		return neighborDistance.apply(from, to) ;
	}
	
	public void addEdge(NodeId from, NodeId to) {
		nodes.add(from) ;
		nodes.add(to) ;
		var ns = edges.get(from) ;
		if (ns == null) {
			ns = new HashSet<NodeId>() ;
			edges.put(from, ns) ;
		}
		ns.add(to) ;
	}
	
	public void addBidirectionalEdge(NodeId n1, NodeId n2) {
		addEdge(n1,n2) ;
		addEdge(n2,n1) ;
	}
	

}
