package eu.iv4xr.framework.extensions.pathfinding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.SimpleNavGraph;
import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph;
import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph.VertexType;
import eu.iv4xr.framework.spatial.Box;
import eu.iv4xr.framework.spatial.Line;
import eu.iv4xr.framework.spatial.Obstacle;
import eu.iv4xr.framework.spatial.Sphere;
import eu.iv4xr.framework.spatial.Vec3;
import eu.iv4xr.framework.spatial.meshes.Edge;
import eu.iv4xr.framework.spatial.meshes.Face;
import eu.iv4xr.framework.spatial.meshes.Mesh;

public class TestSimple_and_SurfaceNavGraph {
    
    /**
     * Construct a Mesh that looks like this:
     * 
     *                        v5(0,2,4)
     *                       / \
     *                      /   \
     *                     /     \
     *                    /       \
     *                   /  face4  \
     *                  /           \
     *                 /             \
     *                /               \
     *               /                 \
     *      (-1,0,2)v3-----------------v4(1,0,2)
     *             /  \               /
     *            /    \             /
     *           /      \   face3   /
     *          /        \         /
     *         /  face2   \       /
     *        /            \     /
     *       /              \   /
     *      /                \ /
     *    v0(-2,0,0)---------v1(0,0,0)
     *      \               /
     *       \             /
     *        \   face1   /
     *         \         /
     *          \       /
     *           \     /
     *            \   /
     *             \ /
     *              v2(-1,2,-2)
     */
    Mesh mesh0() {
        Mesh mesh = new Mesh();
        var v0 = new Vec3(-2, 0, 0);
        var v1 = new Vec3(0, 0, 0);
        var v2 = new Vec3(-1, 2, -2);
        var v3 = new Vec3(-1, 0, 2);
        var v4 = new Vec3(1, 0, 2);
        var v5 = new Vec3(0, 2, 4);
        mesh.vertices.add(v0);
        mesh.vertices.add(v1);
        mesh.vertices.add(v2);
        mesh.vertices.add(v3);
        mesh.vertices.add(v4);
        mesh.vertices.add(v5);
        mesh.edges.add(new Edge(0, 1));
        mesh.edges.add(new Edge(0, 2));
        mesh.edges.add(new Edge(2, 1));
        mesh.edges.add(new Edge(0, 3));
        mesh.edges.add(new Edge(3, 1));
        mesh.edges.add(new Edge(3, 4));
        mesh.edges.add(new Edge(3, 5));
        mesh.edges.add(new Edge(4, 5));
        mesh.edges.add(new Edge(4, 1));
        int[] f1 = { 0, 1, 2 };
        int[] f2 = { 0, 1, 3 };
        int[] f3 = { 1, 3, 4 };
        int[] f4 = { 3, 4, 5 };
        var F1 = new Face(f1);
        var F2 = new Face(f2);
        var F3 = new Face(f3);
        var F4 = new Face(f4);
        mesh.faces.add(F1);
        mesh.faces.add(F2);
        mesh.faces.add(F3);
        mesh.faces.add(F4);
        return mesh;
    }

    @Test
    public void test_conversion_mesh_to_SimpleNavGraph() {
        SimpleNavGraph navgraph = SimpleNavGraph.fromMeshFaceAverage(mesh0());
        // System.out.println("" + navgraph.vertices.size()) ;
        assertTrue(navgraph.vertices.size() == 4);
        assertTrue(Vec3.dist(navgraph.vertices.get(0), new Vec3(-1f, 0.66f, -0.66f)) < 0.1);
        assertTrue(Vec3.dist(navgraph.vertices.get(1), new Vec3(-1f, 0f, 0.66f)) < 0.1);
        assertTrue(Vec3.dist(navgraph.vertices.get(2), new Vec3(0f, 0f, 1.33f)) < 0.1);
        assertTrue(Vec3.dist(navgraph.vertices.get(3), new Vec3(0f, 0.66f, 2.66f)) < 0.1);
        assertTrue(checkConnection(0, navgraph, 1));
        assertTrue(checkConnection(1, navgraph, 0, 2));
        assertTrue(checkConnection(2, navgraph, 1, 3));
        assertTrue(checkConnection(3, navgraph, 2));
    }

    /**
     * Check if k is connected to exactly the nodes listed in sucs.
     */
    boolean checkConnection(int k, SimpleNavGraph navgraph, int... sucs) {
        var neighbors = (Collection<Integer>) navgraph.neighbours(k);
        if (neighbors.size() != sucs.length)
            return false;
        for (int v : sucs) {
            if (!neighbors.contains(v))
                return false;
        }
        return true;
    }

    String showConnections(int k, SimpleNavGraph navgraph) {
        StringBuffer sb = new StringBuffer();
        sb.append("next(" + k + ") =");
        int i = 0;
        for (int v : navgraph.neighbours(k)) {
            if (i > 0)
                sb.append(",");
            sb.append(" " + v);
            i++;
        }
        return sb.toString();
    }

    /**
     * When mesh0 above is converted to a surface Nav-graph, center points are added. The resulting nav-graph
     * should look like below. Added center points are v6...v9. Each center point will be connected to the
     * three corners of the triangle it is in, and also to neighboring center-points.
     * 
     * 
     *                        v5(0,2,4)
     *                       / \
     *                      /   \
     *                     /     \
     *                    /       \
     *                   /    v9   \
     *                  /           \
     *                 /             \
     *                /               \
     *               /                 \
     *      (-1,0,2)v3-----------------v4(1,0,2)
     *             /  \               /
     *            /    \             /
     *           /      \     v8    /
     *          /        \         /
     *         /    v7    \       /
     *        /            \     /
     *       /              \   /
     *      /                \ /
     *    v0(-2,0,0)---------v1(0,0,0)
     *      \               /
     *       \             /
     *        \           /
     *         \    v6   /
     *          \       /
     *           \     /
     *            \   /
     *             \ /
     *              v2(-1,2,-2)
     */
    @Test
    public void test_conversion_mesh_to_SurfaceNavGraph() {
        Mesh mesh = mesh0();
        SurfaceNavGraph navgraph = new SurfaceNavGraph(mesh, 0f);
        // check if all vertices in the mesh are included in the navgraph
        assertTrue(navgraph.vertices.size() == 10);
        assertTrue(navgraph.numberOfBaseVertices == 6);
        for (int k = 0; k < mesh.vertices.size(); k++) {
            assertTrue(navgraph.vertices.get(k).equals(mesh.vertices.get(k)));
        }
        // check if all the facses in the mesh are included in the navgraph
        assertTrue(navgraph.faces.size() == 4);
        for (int f = 0; f < mesh.faces.size(); f++) {
            assertTrue(navgraph.faces.get(f) == mesh.faces.get(f));
        }
        // check the added center-points:
        assertTrue(Vec3.dist(navgraph.vertices.get(6), new Vec3(-1f, 0.66f, -0.66f)) < 0.1);
        assertTrue(Vec3.dist(navgraph.vertices.get(7), new Vec3(-1f, 0f, 0.66f)) < 0.1);
        assertTrue(Vec3.dist(navgraph.vertices.get(8), new Vec3(0f, 0f, 1.33f)) < 0.1);
        assertTrue(Vec3.dist(navgraph.vertices.get(9), new Vec3(0f, 0.66f, 2.66f)) < 0.1);
        // check the connections:
        navgraph.perfect_memory_pathfinding = true;

        System.out.println("**  " + showConnections(0, navgraph));
        System.out.println("**  " + showConnections(1, navgraph));
        System.out.println("**  " + showConnections(2, navgraph));
        System.out.println("**  " + showConnections(6, navgraph));
        System.out.println("**  " + showConnections(7, navgraph));

        assertTrue(checkConnection(0, navgraph, 1, 2, 3, 6, 7));
        assertTrue(checkConnection(1, navgraph, 0, 2, 3, 4, 6, 7, 8));
        assertTrue(checkConnection(2, navgraph, 0, 1, 6));
        assertTrue(checkConnection(3, navgraph, 0, 1, 4, 5, 7, 8, 9));
        assertTrue(checkConnection(4, navgraph, 1, 3, 5, 8, 9));
        assertTrue(checkConnection(5, navgraph, 3, 4, 9));

        assertTrue(checkConnection(6, navgraph, 0, 1, 2, 7));
        assertTrue(checkConnection(7, navgraph, 0, 1, 3, 6, 8));
        assertTrue(checkConnection(8, navgraph, 1, 3, 4, 7, 9));
        assertTrue(checkConnection(9, navgraph, 3, 4, 5, 8));
        // check vertices type:
        for (int v = 0; v <= 5; v++) {
            assertTrue(navgraph.verticesType.get(v) == VertexType.BORDER);
        }
        for (int v = 6; v <= 9; v++) {
            assertTrue(navgraph.verticesType.get(v) == VertexType.CENTRE);
        }
    }

    @Test
    public void test_pathFinding_on_SimpleNavGarph() {
        Mesh mesh = mesh0();
        SimpleNavGraph navgraph = SimpleNavGraph.fromMeshFaceAverage(mesh0());
        var pathfinder = new AStar();
        var path = pathfinder.findPath(navgraph, 0, 1);
        System.out.println("** " + path);
        assertTrue(checkPath(path, 0, 1));
        path = pathfinder.findPath(navgraph, 0, 3);
        System.out.println("** " + path);
        assertTrue(checkPath(path, 0, 1, 2, 3));

    }

    boolean checkPath(List<Integer> pathToCheck, int... expectedPath) {
        System.out.println("** path to check: " + pathToCheck);
        if (pathToCheck.size() != expectedPath.length)
            return false;
        for (int k = 0; k < expectedPath.length; k++) {
            if (pathToCheck.get(k) != expectedPath[k])
                return false;
        }
        return true;
    }

    /**
     * Test vertex to vertex path-finding on SurfaceNavGraph, under perfect memory.
     * We also test different path preferences (CENTER/BORDER), as well as with
     * obstacles.
     */
    @Test
    public void test_SurfaceNavGraph_vertex2vertex_pathPinfing() {
        Mesh mesh = mesh0();
        SurfaceNavGraph navgraph = new SurfaceNavGraph(mesh, 0f);
        navgraph.setPathFinder(new AStar());
        // insist on perfect memory:
        navgraph.perfect_memory_pathfinding = true;

        // check some path finding, should prefer center vertices:
        var path = navgraph.findPath(2, 2);
        assertTrue(checkPath(path, 2));
        path = navgraph.findPath(2, 0);
        assertTrue(checkPath(path, 2, 0));
        path = navgraph.findPath(2, 3);
        // System.out.println(path) ;
        assertTrue(checkPath(path, 2, 6, 7, 3));
        path = navgraph.findPath(2, 5);
        //System.out.println(path) ;
        assertTrue(checkPath(path, 2, 1, 8, 9, 5));

        // switch to prefer border vertices:
        navgraph.travelPreferrence = SurfaceNavGraph.PREFER_BORDER;
        path = navgraph.findPath(2, 3);
        //System.out.println(path) ;
        assertTrue(checkPath(path, 2, 6, 7, 3));
        path = navgraph.findPath(2, 4);
        assertTrue(checkPath(path, 2, 1, 4));
        path = navgraph.findPath(2, 5);
        // System.out.println(path) ;
        assertTrue(checkPath(path, 2, 6, 7, 3, 5));
        // System.out.println(path) ;

        navgraph.travelPreferrence = SurfaceNavGraph.PREFER_CENTER;
        // add an obstacle at vertex 8
        var box = new Box(new Vec3(0f, 0f, 1.33f), new Vec3(0.3f, 0.3f, 0.3f));
        navgraph.addObstacle(box);
        // set the box first to be non-blocking
        navgraph.toggleBlockingOff(box);
        path = navgraph.findPath(2, 5);
        assertTrue(checkPath(path, 2, 1, 8, 9, 5));
        // and now let's making the box blocking; we should now avoid node 8:
        navgraph.toggleBlockingOn(box);
        path = navgraph.findPath(2, 5);
        assertTrue(checkPath(path, 2, 6, 7, 3, 5));
        // System.out.println(path) ;
    }

    /**
     * Test vertex to vertex, memory-based path-finding (exploration) on
     * SurfaceNavGraph.
     */
    @Test
    public void test_SurfaceNavGraph_vertexlevel_exploration() {
        Mesh mesh = mesh0();
        SurfaceNavGraph navgraph = new SurfaceNavGraph(mesh, 0f);
        navgraph.setPathFinder(new AStar());

        // since no nodes are initially "seen", exploration should not be possible:
        assertTrue(navgraph.explore(0) == null);
        assertTrue(navgraph.explore(1) == null);
        assertTrue(navgraph.explore(2) == null);
        assertTrue(navgraph.explore(5) == null);

        // mark these as seen:
        navgraph.markAsSeen(0, 1, 2, 3);
        // check that this will heuristically also mark connected center points as
        // marked:
        assertTrue(navgraph.seenVertices.get(6));
        assertTrue(navgraph.seenVertices.get(7));
        assertTrue(navgraph.seenVertices.get(8));
        assertTrue(navgraph.seenVertices.get(9));
        // in this example only 4,5 should remain as unseen:
        assertFalse(navgraph.seenVertices.get(4));
        assertFalse(navgraph.seenVertices.get(5));

        // path from 2 to 3 should be possible:
        List<Integer> path = navgraph.findPath(2, 3);
        assertTrue(checkPath(path, 2, 6, 7, 3));
        // however, path from 2 to 3 should NOT be possible
        path = navgraph.findPath(2, 5);
        assertTrue(path == null);

        var frontiers = navgraph.getFrontierVertices();
        assertTrue(frontiers.toString().equals("[<1,4>, <3,4>, <8,4>, <9,4>]"));
        System.out.println("** Frontiers: " + frontiers);
        // check an explore-path from node 2; it should lead to 4:
        path = navgraph.explore(2);
        assertTrue(checkPath(path, 2, 1, 4));

        // check now if things still work in combination with obstacle
        // add an obstacle to block node 4
        // add an obstacle at node 4
        var box = new Box(new Vec3(1, 0, 2), new Vec3(0.3f, 0.3f, 0.3f));
        navgraph.addObstacle(box);
        navgraph.toggleBlockingOn(box);
        // explore should now give a path that leads to 5 (because 4 is now blocked):
        path = navgraph.explore(2);
        assertTrue(path.get(path.size() - 1) == 5);
        // System.out.println(path) ;
        // put another obstacle to block node 5:
        var box2 = new Box(new Vec3(0, 2, 4), new Vec3(0.3f, 0.3f, 0.3f));
        navgraph.addObstacle(box2);
        navgraph.toggleBlockingOn(box2);
        // explore should now have no path left:
        path = navgraph.explore(2);
        assertTrue(path == null);
    }

    @Test
    public void test_SurfaceNavgrapg_getNearestUnblockedVertex() {
        Mesh mesh = mesh0();
        SurfaceNavGraph navgraph = new SurfaceNavGraph(mesh, 0f);

        // without obstacles:

        Vec3 location = new Vec3(0, 0, 0);
        Integer vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 1);

        location = new Vec3(-0.1f, 0, -0.1f);
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 1);

        location = new Vec3(-1f, 0, 2f);
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 3);

        location = new Vec3(-1f, 0, 1f);
        var face1 = navgraph.faces.get(1);
        // for (int v : face1.vertices) {
        // System.out.println(" >> vert " + v + ", at " + navgraph.vertices.get(v)) ;
        // }
        System.out.println("## dist face 1: " + face1.distFromPoint(location, navgraph.vertices));
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 7);

        location = new Vec3(-0.2f, 0, 0.1f);
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 1);

        location = new Vec3(-1.7f, 0, 0.5f);
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 0);

        // elevated:
        location = new Vec3(-1f, 0.2f, 1f);
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 7);

        location = new Vec3(-1f, 0.22f, 1f);
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == null);

        location = new Vec3(-1f, -0.22f, 1f);
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == null);

        // now with obstacle at node-1
        var box = new Box(new Vec3(0, 0, 0), new Vec3(0.01f, 0.01f, 0.01f));
        navgraph.addObstacleInBlockingState(box);
        location = new Vec3(-0.1f, 0, -0.1f);
        // System.out.println("## #obstacles = " + navgraph.obstacles.size()) ;
        // System.out.println("## obs1 blocking: " +
        // navgraph.obstacles.get(0).isBlocking) ;
        // System.out.println("## blocking: " + box.intersect(new Line(location, new
        // Vec3(0,0,0)))) ;
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 6);

        location = new Vec3(-0.2f, 0, 0.2f);
        vertex = navgraph.getNearestUnblockedVertex(location, 0.2f);
        System.out.println("** location: " + location + ", closest vertex: " + vertex);
        assertTrue(vertex == 6);
    }

    /**
     * Test path-finding between any Vec-3 start and goal locations (rather than
     * vertex to vertex).
     */
    @Test
    public void test_SurfaceNavgraph_vec3location_pathfinding() {
        Mesh mesh = mesh0();
        SurfaceNavGraph navgraph = new SurfaceNavGraph(mesh, 0f);

        // start location near node-2, and location near node-5
        var loc_a = new Vec3(-1, 2, -1.8f);
        var loc_b = new Vec3(0, 2f, 3.8f);

        // perfect memory, no obstacle ;
        navgraph.perfect_memory_pathfinding = true;
        var path = navgraph.findPath(loc_a, loc_b, 0.2f);

        assertTrue(checkPath(path, 2, 1, 8, 9, 5));

        // perfect memory, adding an obstacle blocking node 8
        var box = new Box(new Vec3(0f, 0f, 1.33f), new Vec3(0.1f, 0.1f, 0.1f));
        navgraph.addObstacleInBlockingState(box);
        path = navgraph.findPath(loc_a, loc_b, 0.2f);
        assertTrue(checkPath(path, 2, 6, 7, 3, 5));

        // now with memory, considering 0,1,2,3 to be seen; which will leave 4,5
        // as unexplored. Path-finding should fail to find a path to b (near node 5):
        navgraph.perfect_memory_pathfinding = false;
        navgraph.markAsSeen(0, 1, 2, 3);

        path = navgraph.findPath(loc_a, loc_b, 0.2f);
        assertTrue(path == null);

        // however, finding a path to a location near 9 should work because 9 is
        // considered
        // as explored
        loc_b = new Vec3(0f, 0.66f, 3f);
        path = navgraph.findPath(loc_a, loc_b, 0.25f);
        System.out.println("** path: " + path);
        assertTrue(path.get(0) == 2 && path.get(path.size() - 1) == 9);
    }

    /**
     * Test exploration from a Vec3 location.
     */
    @Test
    public void test_SurfaceNavgraph_fromVec3Location_exploration() {
        Mesh mesh = mesh0();
        SurfaceNavGraph navgraph = new SurfaceNavGraph(mesh, 0f);

        // since no nodes is initially marked as seen, exploration should not be
        // possible:
        assertTrue(navgraph.explore(new Vec3(0, 0, 0), 0.2f) == null);
        assertTrue(navgraph.explore(new Vec3(-1, 0, 2), 0.2f) == null);

        // mark these as seen:
        navgraph.markAsSeen(0, 1, 2, 3);
        // note that these will mark all the center-nodes to be seen as well
        // So... only 4 and 5 are left as unseen
        var path = navgraph.explore(new Vec3(0, 0, 0), 0.2f);
        System.out.println(">>> path: " + path);
        assertTrue(path.get(0) == 1 && path.get(path.size() - 1) == 4);

        path = navgraph.explore(new Vec3(-0.2f, 0, 0), 0.2f);
        System.out.println(">>> path: " + path);
        assertTrue(path.get(0) == 1 && path.get(path.size() - 1) == 4);

        // (-1,2,-2) near node 2
        path = navgraph.explore(new Vec3(-1, 2, -1.8f), 0.2f);
        System.out.println(">>> path: " + path);
        assertTrue(path.get(0) == 2 && path.get(path.size() - 1) == 4);
    }

}