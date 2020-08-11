package eu.iv4xr.framework.spatial.meshes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import eu.iv4xr.framework.spatial.Vec3;

/**
 * Represent a polygon, described by a set of vertices, stored in an array
 * called vertices. Each vertex is represented by just an integer (think of it
 * as the id of the vertex), so it does not describe the actual position of the
 * vertex in the space (another information is needed to map the vertices to
 * actual positions). Two vertices in the consecutive positions in the array
 * "vertices" are assumed to be connected with a line. So, for all i, vertices[i]
 * is connected to vertices[i+1], and the last one in the array, vertices[N-1]
 * is connected to vertices[0].
 * In this way the array describes a polygon.
 * 
 * @author Naraenda
 *
 */
public class Face implements Iterable<Integer> {
	
    public int[] vertices;

    public Face(int[] vertices) {
        this.vertices = vertices;
    }

    @Override
    public Iterator<Integer> iterator() {
        return Arrays.stream(vertices).boxed().iterator();
    }

    /**
     * Given a list that maps vertex-indices to their real positions, this method
     * calculate the center/middle point of this Face.
     */
    public Vec3 center(List<Vec3> vertexMap) {
    	int N = vertices.length ;
    	Vec3 avg = Vec3.zero();
        for (Integer v : vertices) {
        	Vec3 z = vertexMap.get(v) ;
            avg.x += z.x ;
            avg.y += z.y ;
            avg.z += z.z ;
        }
        avg = Vec3.div(avg, vertices.length);
        return avg ;
    }
    
    /**
     * Check if the given edge is an edge of this Face.
     */
    public boolean containsEdge(Edge e) {
    	int N_ = vertices.length - 1 ;
    	for (int k=0; k < N_ ; k++) {
    		Edge ex = new Edge(vertices[k],vertices[k+1]) ;
    		if (e.equals(ex)) return true ;
    	}
    	// last edge:
    	Edge ex = new Edge(vertices[N_], vertices[0]) ;
    	return e.equals(ex) ;
    }
    
    public String toString(ArrayList<Vec3> concreteVertices) {
    	var sb = new StringBuffer() ;
    	sb.append("#verts=" + vertices.length + " (") ;
    	int j=0 ;
    	for (int v : vertices) {
    		if (j>0) sb.append(", ") ;
    		sb.append("" + v) ;
    		j++ ;
    	}
    	sb.append(") ; positions: ") ;
    	j = 0 ;
    	for (int v : vertices) {
    		if (j>0) sb.append(", ") ;
    		sb.append("" + concreteVertices.get(v)) ;
    		j++ ;
    	}
    	return sb.toString() ;
    }
    
    /**
     * Check if two Faces are 'connected'. The method defines 'connected' to mean
     * that the two Faces share at least one edge. This works well if the
     * Facses are convex and have no overlapping edge (except if it is common).
     */
    public static boolean isConnected(Face a, Face b) {
        int[] a_ = a.vertices.clone();
        int[] b_ = b.vertices.clone();
        Arrays.sort(a_);
        Arrays.sort(b_);

        int i = 0;
        int j = 0;
        int common = 0;

        while (i < a_.length && j < b_.length) {
            if (a_[i] == b_[j]) {
                common++;
                i++; j++;
            } else if (a_[i] < b_[j]) {
                i++;
            } else if (a_[i] > b_[j]) {
                j++;
            }
        }

        return common >= 2;
    }
    
    
    /**
     * Test if the given point is inside this Face. We only look at the (X,Z) values. So, essentially
     * pretending the Face is 2D.
     * 
     * Algorithm: https://algorithmtutor.com/Computational-Geometry/Check-if-a-point-is-inside-a-polygon/
     * Which looks to be a generalization of an algorithm for triangle: https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
     */
    public boolean coversPointXZ(Vec3 point, ArrayList<Vec3> concreteVertices) {
    	int N = vertices.length ;
    	List<Float> d = new LinkedList<>() ;
    	for (int i=0; i<N; i++) {
    		var p1 = concreteVertices.get(vertices[i]) ;
    		var p2 = concreteVertices.get(vertices[(i + 1) % N]) ;
    		// calculate A, B and C
    		float a = -(p2.z - p1.z) ;
    		float b = p2.x - p1.x ;
    		float c = -(a * p1.x + b * p1.z) ;
    		d.add(a * point.x + b * point.z + c) ;

    	}
    	return d.stream().allMatch(value -> value >=0) 
    			|| d.stream().allMatch(value -> value <=0) ;
    }
}