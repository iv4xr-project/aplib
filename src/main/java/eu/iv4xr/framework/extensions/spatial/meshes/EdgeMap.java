package eu.iv4xr.framework.extensions.spatial.meshes;

import java.util.*;

public class EdgeMap extends HashMap<Integer, HashSet<Integer>> {
	private static final long serialVersionUID = -1791683183609681586L;

	public void put(Edge edge) {
        if(!this.containsKey(edge.i))
            this.put(edge.i, new HashSet<Integer>());
        if(!this.containsKey(edge.j))
            this.put(edge.j, new HashSet<Integer>());

        this.get(edge.i).add(edge.j);
        this.get(edge.j).add(edge.i);
    }

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

    public HashSet<Integer> neighbours(int i) {
        return this.getOrDefault(i, new HashSet<Integer>());
    }
}