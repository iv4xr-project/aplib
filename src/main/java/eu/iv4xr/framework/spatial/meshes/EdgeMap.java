package eu.iv4xr.framework.spatial.meshes;

import java.util.*;

/**
 * An instance of HashMap to store a set of connected vertices (so, a graph).
 * The connections/edges are undirected.
 * 
 * The method vertices() gives the set of all vertices stored in this data
 * structure. The method neighbors(i) returns the set of all neighbors of i;
 * these are vertices that are directly connected to i.
 * 
 * @author Naraenda
 *
 */
public class EdgeMap extends HashMap<Integer, HashSet<Integer>> {
    private static final long serialVersionUID = -1791683183609681586L;

    /**
     * Add the two vertices of an edge, along with its connectivity.
     */
    public void put(Edge edge) {
        if(!this.containsKey(edge.i))
            this.put(edge.i, new HashSet<Integer>());
        if(!this.containsKey(edge.j))
            this.put(edge.j, new HashSet<Integer>());

        this.get(edge.i).add(edge.j);
        this.get(edge.j).add(edge.i);
    }

    /**
     * Remove an edge from this data structure.
     */
    public void remove(Edge edge) {
        var vertI = this.get(edge.i);
        var vertJ = this.get(edge.j);

        vertI.remove(edge.j);
        vertJ.remove(edge.i);

        // If an vertex has no more edges, remove it from the listing.
        if (vertI.isEmpty())
            this.remove(edge.i);
        if (vertJ.isEmpty())
            this.remove(edge.j);
    }

    public Set<Integer> vertices() {
    	return keySet() ;
    }
    
    public HashSet<Integer> neighbours(int i) {
        return this.getOrDefault(i, new HashSet<Integer>());
    }
}