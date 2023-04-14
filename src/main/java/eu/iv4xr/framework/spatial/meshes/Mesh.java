package eu.iv4xr.framework.spatial.meshes;

import java.util.ArrayList;
import eu.iv4xr.framework.spatial.Vec3;

/**
 * A simple container to store mesh-data. A mesh is basically a set of
 * convex-polygons/faces.
 * 
 * The array "vertices" contain the 3D locations/coordinates of the corners of
 * these faces. The index of this array can be seen as the id of a
 * corner/vertex. So if v is a vertex with index i, then vertices[i] specifies
 * the 3D location of v.
 * 
 * The field "edges" contains the set of edges over the vertices. When there is
 * an edge between vertex i and j, this means that they are connected. The
 * connection is bidirectional.
 * 
 * The field "faces" contains the set of faces that form this mesh.
 * 
 * @author Naraenda
 */
public class Mesh {
    public ArrayList<Vec3> vertices = new ArrayList<>();
    public ArrayList<Edge> edges = new ArrayList<>();
    public ArrayList<Face> faces = new ArrayList<>();

    public Mesh() {
    }

//    @Override
    public String toString() {
        System.out.println(">>> #vertices = " + vertices.size());
        var sb = new StringBuffer();
        int k = 0;
        for (Face face : faces) {
            if (k > 0)
                sb.append("\n");
            sb.append("Face-" + k + ": " + face.toString(vertices));
            k++;
        }
        return sb.toString();
    }

}
