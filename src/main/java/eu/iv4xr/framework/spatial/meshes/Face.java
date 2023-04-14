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
 * "vertices" are assumed to be connected with a line. So, for all i,
 * vertices[i] is connected to vertices[i+1], and the last one in the array,
 * vertices[N-1] is connected to vertices[0]. In this way the array describes a
 * polygon.
 * 
 * @author Naraenda
 *
 */
public class Face implements Iterable<Integer> {

    public int[] vertices;

    public Face(int[] vertices) {
        if (vertices.length < 3)
            throw new IllegalArgumentException();
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
        int N = vertices.length;
        Vec3 avg = Vec3.zero();
        for (Integer v : vertices) {
            Vec3 z = vertexMap.get(v);
            avg.x += z.x;
            avg.y += z.y;
            avg.z += z.z;
        }
        avg = Vec3.div(avg, vertices.length);
        return avg;
    }

    /**
     * Check if the given edge is an edge of this Face.
     */
    public boolean containsEdge(Edge e) {
        int N_ = vertices.length - 1;
        for (int k = 0; k < N_; k++) {
            Edge ex = new Edge(vertices[k], vertices[k + 1]);
            if (e.equals(ex))
                return true;
        }
        // last edge:
        Edge ex = new Edge(vertices[N_], vertices[0]);
        return e.equals(ex);
    }

    /**
     * The area of a triangle given its corners.
     */
    private static float triangleArea(Vec3 a, Vec3 b, Vec3 c) {
        float ab = Vec3.dist(a, b);
        float ac = Vec3.dist(a, c);
        float bc = Vec3.dist(b, c);
        // using Heron formula to calculate the area https://qr.ae/pN9xEB
        float s = (ab + ac + bc) / 2;
        return (float) Math.sqrt(s * (s - ab) * (s - ac) * (s - bc));
    }

    public float area(List<Vec3> vertexMap) {
        int N = vertices.length;
        if (N == 3)
            return triangleArea(vertexMap.get(vertices[0]), vertexMap.get(vertices[1]), vertexMap.get(vertices[2]));
        // if the face has more edges:
        float area = 0f;
        Vec3 center = center(vertexMap);
        for (int k = 0; k < N; k++) {
            Vec3 corner1 = vertexMap.get(k);
            Vec3 nextCorner = vertexMap.get((k + 1) % N);
            area += triangleArea(center, corner1, nextCorner);
        }
        return area;
    }

    public String toString(ArrayList<Vec3> concreteVertices) {
        var sb = new StringBuffer();
        sb.append("#verts=" + vertices.length + " (");
        int j = 0;
        for (int v : vertices) {
            if (j > 0)
                sb.append(", ");
            sb.append("" + v);
            j++;
        }
        sb.append(") ; positions: ");
        j = 0;
        for (int v : vertices) {
            if (j > 0)
                sb.append(", ");
            sb.append("" + concreteVertices.get(v));
            j++;
        }
        return sb.toString();
    }

    /**
     * Check if two Faces are 'connected'. The method defines 'connected' to mean
     * that the two Faces share at least one edge. This works well if the Facses are
     * convex and have no overlapping edge (except if it is common).
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
                i++;
                j++;
            } else if (a_[i] < b_[j]) {
                i++;
            } else if (a_[i] > b_[j]) {
                j++;
            }
        }

        return common >= 2;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Face (");
        for (int k = 0; k < vertices.length; k++) {
            if (k > 0)
                sb.append(",");
            sb.append("" + vertices[k]);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Return an unsigned distance from a point w to this Face. The distance is
     * defined as follows. Imagine first the 3D shape obtained by extruding this
     * Face along its normal vector. Let's call this shape the extruding prism of
     * this Face.
     * 
     * If the the point w is strictly inside this prism, its distance is defines as
     * its distance to the Face along the normal vector.
     * 
     * Otherwise, the point is either strictly outside the prism, or at the prism's
     * border. Then its distance to this Face is defined as its distance to the
     * closest edge of the Face.
     * 
     * Taken from:
     * https://www.iquilezles.org/www/articles/triangledistance/triangledistance.htm
     * 
     */
    public float distFromPoint(Vec3 w, ArrayList<Vec3> concreteVertices) {
        // Much of the information here can be pre-computed; we will not do so here
        // to favor simpler implementation.
        // But can be improved in the future.
        int N = vertices.length;

        var vert_0 = concreteVertices.get(vertices[0]);

        var line_0_to_1 = Vec3.sub(concreteVertices.get(vertices[1]), vert_0);
        var line_lastNode_to_0 = Vec3.sub(vert_0, concreteVertices.get(vertices[N - 1]));
        // the normal vector between the above two lines, which is also parallel with
        // the
        // face normal vector:
        var norm = Vec3.cross(line_0_to_1, line_lastNode_to_0);
        // System.out.println(">> normal: " + norm) ;

        // for inside outside test:
        List<Float> d = new LinkedList<>();

        // test is the point is inside the extruded prism over this Face:
        boolean inside_extruded_prism = true;
        int sign = 0; // 0 unknown, 1 positive, -1 negative

        for (int i = 0; i < N; i++) {
            var p = concreteVertices.get(vertices[i]);
            // System.out.println("### v" + i + " " + p) ;
            var p_next = concreteVertices.get(vertices[(i + 1) % N]);
            var line_next_to_p = Vec3.sub(p_next, p);
            var line_w_to_p = Vec3.sub(w, p);
            var test = Vec3.dot(Vec3.cross(line_next_to_p, norm), line_w_to_p);
            // System.out.println(">> " + test) ;
            if (sign == 0) {
                if (test > 0)
                    sign = 1;
                else
                    sign = -1;
            } else if ((sign > 0 && test <= 0) || (sign < 0 && test >= 0)) {
                // found a test with differring sign:
                inside_extruded_prism = false;
                break;
            }
        }
        // System.out.println(">> Inside extruded prism: " + inside_extruded_prism) ;
        if (inside_extruded_prism) {
            var z = Vec3.dot(Vec3.sub(w, vert_0), norm);
            return Math.abs(z / norm.length());
            // return (float) Math.sqrt((z*z/norm.lengthSq())) ;
        } else {
            // the point is outside the prism
            float distSq = Float.POSITIVE_INFINITY; // we will compare the square of distance instead
            // iterate over all sides, to find the minimum distance :
            for (int i = 0; i < N; i++) {
                // current side to consider: v(i+1) --> v(i)
                var p = concreteVertices.get(vertices[i]);
                var p_next = concreteVertices.get(vertices[(i + 1) % N]);

                // a complicated way to calculate the distance from the point w to the side:

                var line_next_to_p = Vec3.sub(p_next, p);
                var line_w_to_p = Vec3.sub(w, p);
                float line_next_to_p_lengthsq = line_next_to_p.lengthSq();
                float clamp_;
                if (line_next_to_p_lengthsq == 0) {
                    clamp_ = 1;
                } else {
                    clamp_ = clamp(Vec3.dot(line_next_to_p, line_w_to_p) / line_next_to_p_lengthsq);
                }
                float new_distSq = Vec3.sub(Vec3.mul(line_next_to_p, clamp_), line_w_to_p).lengthSq();
                // the square-distance of the point w to the current side pnext-->p
                distSq = Math.min(new_distSq, distSq);
            }
            return (float) Math.sqrt(distSq);
        }
    }

    /**
     * Clamping x between 0 and 1. That is, if x&le;0, we return 0, if x&ge;1 we return
     * 1, and else the original x is returned.
     */
    static float clamp(float x) {
        return x <= 0 ? 0 : (x >= 1 ? 1 : x);
    }

    /*
     * WP: COMMENTING THIS OUT. Ignoring the y-axis won't work when we e.g. have a
     * mesh over overlaping building floors.
     * 
     * Test if the given point is inside this Face. We only look at the (X,Z)
     * values. So, essentially pretending the Face is 2D.
     * 
     * Algorithm:
     * https://algorithmtutor.com/Computational-Geometry/Check-if-a-point-is-inside-
     * a-polygon/ Which looks to be a generalization of an algorithm for triangle:
     * https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in
     * -a-2d-triangle
     */
    /*
     * public boolean coversPointXZ(Vec3 point, ArrayList<Vec3> concreteVertices) {
     * int N = vertices.length ; List<Float> d = new LinkedList<>() ; for (int i=0;
     * i<N; i++) { var p1 = concreteVertices.get(vertices[i]) ; var p2 =
     * concreteVertices.get(vertices[(i + 1) % N]) ; // calculate A, B and C float a
     * = -(p2.z - p1.z) ; float b = p2.x - p1.x ; float c = -(a * p1.x + b * p1.z) ;
     * d.add(a * point.x + b * point.z + c) ;
     * 
     * } return d.stream().allMatch(value -> value >=0) || d.stream().allMatch(value
     * -> value <=0) ; }
     */

    // just for testing:
    public static void main(String[] args) {
        /*
         * A rectangle 2x2, with (0,0,0) at node 0, raised by y=2 at nodes 2,3
         * 
         * (2)----(3) | | | | (0)----(1)
         */
        var v0 = new Vec3(0, 0, 0);
        var v1 = new Vec3(2, 0, 0);
        var v2 = new Vec3(0, 2, 2);
        var v3 = new Vec3(2, 2, 2);

        int[] vertices = { 0, 1, 3, 2 }; // the order should make sure the edges are consecutivelly connected
        ArrayList<Vec3> concreteVertices = new ArrayList<>();
        concreteVertices.add(v0);
        concreteVertices.add(v1);
        concreteVertices.add(v3);
        concreteVertices.add(v2);

        Face face = new Face(vertices);

        var point = new Vec3(0, 0, 0);
        System.out.println("" + point + ", dist = " + face.distFromPoint(point, concreteVertices));
        point = new Vec3(1, 0, 1);
        System.out.println("" + point + ", dist = " + face.distFromPoint(point, concreteVertices));
        point = new Vec3(1, 2, 1);
        System.out.println("" + point + ", dist = " + face.distFromPoint(point, concreteVertices));
        point = new Vec3(0, 1, 0);
        System.out.println("" + point + ", dist = " + face.distFromPoint(point, concreteVertices));
        point = new Vec3(0, -1, 0);
        System.out.println("" + point + ", dist = " + face.distFromPoint(point, concreteVertices));

    }
}