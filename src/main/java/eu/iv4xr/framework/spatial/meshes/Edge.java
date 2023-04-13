package eu.iv4xr.framework.spatial.meshes;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import eu.iv4xr.framework.spatial.Vec3;

/**
 * This class contains two indices in the form of primitive integers that
 * describe an edge.
 * 
 * edge.get(0) gives the edge's end-point with the smaller index, and
 * edge.get(1) gives the end-point with the larger index.
 * 
 * @author Naraenda
 */
public class Edge implements Iterable<Integer> {
    public int i; // the node with the smaller index
    public int j; // the node with the bigger index

    public Edge(int v0, int v1) {
        if (v0 == v1)
            throw new IllegalArgumentException("Edge cannot exist between equal indices.");
        if (v0 < 0)
            throw new IllegalArgumentException("First vertex index cannot be negative");
        if (v1 < 0)
            throw new IllegalArgumentException("Second vertex index cannot be negative");

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

    /**
     * Given a list that maps vertex-indices to their real positions, this method
     * calculate the center/middle point of this edge.
     */
    public Vec3 center(List<Vec3> vertices) {
        var a = vertices.get(i);
        var b = vertices.get(j);
        return new Vec3((a.x + b.x) / 2f, (a.y + b.y) / 2f, (a.z + b.z) / 2f);
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Edge))
            return false;
        Edge o_ = (Edge) o;
        // check if the two end-points are the same; since i,j are sorted,
        // this check will do:
        return o_.i == i && o_.j == j;
    }
}