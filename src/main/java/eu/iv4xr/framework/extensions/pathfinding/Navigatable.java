package eu.iv4xr.framework.extensions.pathfinding;

/**
 * Describes a graph-like structure that can be navigated by a pathfinder.
 * 
 * @author Naraenda
 */
public interface Navigatable<NodeId> {
    /**
     * An interable that can be used by pathfinders to explore a node's connections.
     * 
     * @param id the index of the vertex to inspect.
     * @return an iterable of the connected neighbours.
     */
    public Iterable<NodeId> neighbours(NodeId id);

    /**
     * The estimated distance between two arbitairy vertices.
     * 
     * @param from: the index of the vertex to travel from.
     * @param to: the index of the vertex to travel to.
     * @return the estimated distance.
     */
    public float heuristic(NodeId from, NodeId to);

    /**
     * The distance between two connected vertices.
     * 
     * @param from: the index of the vertex to travel from.
     * @param to: the index of the vertex to travel to.
     * @return the measured distance. NaN if nodes are not connected.
     */
    public float distance(NodeId from, NodeId to);
}