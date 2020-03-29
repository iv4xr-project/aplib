package eu.iv4xr.framework.extensions.pathfinding;

import java.util.ArrayList;

import eu.iv4xr.framework.extensions.spatial.*;
import eu.iv4xr.framework.extensions.spatial.meshes.*;

/**
 * A simple navitable graph which edges can be blocked by obstacles.
 * 
 * @author Naraenda
 */
public class NavGraph implements Navigatable {
    public EdgeMap edges;
    public ArrayList<Vec3> vertices;
    
    // TODO: Use a more optimal datastructure for better performance
    public ArrayList<Obstacle<LineIntersectable>> obstacles;

    public void addObstacle(LineIntersectable obstacle) {
        obstacles.add(new Obstacle<LineIntersectable>(obstacle));
    }

    public NavGraph(EdgeMap edges, ArrayList<Vec3> vertices, ArrayList<Obstacle<LineIntersectable>> obstacles){
        this.edges = edges;
        this.vertices = vertices;
        this.obstacles = obstacles;
    }

    public NavGraph() {
        this.edges = new EdgeMap();
        this.vertices = new ArrayList<Vec3>();
        this.obstacles = new ArrayList<Obstacle<LineIntersectable>>();
    }

    @Override
    public Iterable<Integer> neighbours(int id) {
        return edges.neighbours(id);
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
     * Generates a navigation graph from a mesh where a vertex
     * in the navigation graph corresponds to the average center
     * of a face in the mesh. This works with any mesh, but only
     * meshes that consist out of convex faces will guarantee to
     * produce a usefull result.
     * 
     * @param mesh a convex mesh.
     * @return a new nav graph constructed from the given mesh.
     */
    public static NavGraph fromMeshFaceAverage(Mesh mesh) {
        NavGraph g = new NavGraph();
        EdgeMap edgeMap = g.edges;

        ArrayList<Face> faces = mesh.faces;

        // Get center of each face as vertex
        for (int i = 0; i < faces.size(); i++) {
            Face f = faces.get(i);
            Vec3 avg = Vec3.zero();
            for (Integer v : f) {
                avg = Vec3.add(avg, mesh.vertices.get(v));
            }
            avg = Vec3.div(avg, f.vertices.length);
            g.vertices.add(avg);
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