package eu.iv4xr.framework.extensions.pathfinding;

import java.util.ArrayList;

import eu.iv4xr.framework.spatial.*;
import eu.iv4xr.framework.spatial.meshes.*;

/**
 * A simple navigatable graph with edges that can be blocked by obstacles. The
 * graph consists only of 3D vertices connected with edges.
 * 
 * @author Naraenda
 */
public class SimpleNavGraph implements Navigatable {

	/**
	 * The set of vertices that form the navigation graph. The index of the vertex is its
	 * id. So if v = vertices(i), then i is its id, and vertices(i) actually returns the
	 * 3D position of the vertex.
	 */
	public ArrayList<Vec3> vertices;
		
	/**
	 * Describe the connectivity (edges) between the vertices.
	 */
    public EdgeMap edges;
    

    
    // TODO: Use a more optimal datastructure for better performance
    public ArrayList<Obstacle<LineIntersectable>> obstacles;

    public void addObstacle(LineIntersectable obstacle) {
        obstacles.add(new Obstacle<LineIntersectable>(obstacle));
    }

    public SimpleNavGraph(EdgeMap edges, ArrayList<Vec3> vertices, ArrayList<Obstacle<LineIntersectable>> obstacles){
        this.edges = edges;
        this.vertices = vertices;
        this.obstacles = obstacles;
    }

    public SimpleNavGraph() {
        this.edges = new EdgeMap();
        this.vertices = new ArrayList<Vec3>();
        this.obstacles = new ArrayList<Obstacle<LineIntersectable>>();
    }

    @Override
    public Iterable<Integer> neighbours(int id) {
        return edges.neighbours(id);
    }
    
    public Vec3 position(int i) {
    	return vertices.get(i) ;
    }

    @Override
    public float heuristic(int from, int to) {
        return Vec3.dist(vertices.get(from), vertices.get(to));
    }

    @Override
    public float distance(int from, int to) {
        Vec3 a = vertices.get(from);
        Vec3 b = vertices.get(to);

        // Check if there is a collision on path
        Line l = new Line(a, b);
        
        if(obstacles != null && obstacles.stream()
        .filter(o -> o.isBlocking)
        .anyMatch(o -> o.obstacle.intersects(l))) {
            return Float.POSITIVE_INFINITY;
        }

        return Vec3.dist(a, b);
    }

    /**
     * Generates a SimpleNavGraph from a mesh where a vertex
     * in the navigation graph corresponds to the average center
     * of a face in the mesh. This works with any mesh, but only
     * meshes that consist out of convex faces will guarantee to
     * produce a useful result.
     * 
     * @param mesh a convex mesh.
     * @return a SimpleNavGraph constructed from the given mesh.
     */
    public static SimpleNavGraph fromMeshFaceAverage(Mesh mesh) {
        SimpleNavGraph g = new SimpleNavGraph();
        EdgeMap edgeMap = g.edges;

        ArrayList<Face> faces = mesh.faces;

        // Get center of each face as vertex
        for (int i = 0; i < faces.size(); i++) {
            Face f = faces.get(i);
            g.vertices.add(f.center(mesh.vertices));
        }

        // Connect common edges in faces to create dual map
        for (int i = 0; i < faces.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (Face.isConnected(faces.get(i), faces.get(j))) {
                    edgeMap.put(new Edge(i, j));
                }
            }
        }
        return g;
    }
}