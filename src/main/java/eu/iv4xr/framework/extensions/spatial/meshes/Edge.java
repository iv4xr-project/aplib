package eu.iv4xr.framework.extensions.spatial.meshes;

import java.util.Iterator;

/** 
 * This class contains two indices in the form of primitive integers that describe an edge.
 * 
 * @author Naraenda
 */
public class Edge implements Iterable<Integer> {
    int i;
    int j;

    public Edge(int v0, int v1) {
        if (v0 == v1)
            throw new IllegalArgumentException(
                "Edge cannot exist between equal indices.");
        if (v0 < 0)
            throw new IllegalArgumentException(
                "First vertex index cannot be negative");
        if (v1 < 0)
            throw new IllegalArgumentException(
                "Second vertex index cannot be negative");

        this.i = Math.min(v0, v1);
        this.j = Math.max(v0, v1);
    }

	@Override
	public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            int state = 0;        

            @Override
            public boolean hasNext() {
                return state <= 1;
            }

            @Override
            public Integer next() {
                var v = state == 0 ? i : j;
                state++;
                return v;
            }
        };
	}
}