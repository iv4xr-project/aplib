package eu.iv4xr.framework.extensions.pathfinding;


import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.spatial.*;
import eu.iv4xr.framework.spatial.meshes.*;

public class TestSimpleNavGraph {

    private SimpleNavGraph makeTriangle() {
        var n = new SimpleNavGraph();
        n.vertices = new ArrayList<Vec3>();
        n.edges = new EdgeMap();

        n.vertices.add(new Vec3(0, 0, 0));
        n.vertices.add(new Vec3(0, 1, 1));
        n.vertices.add(new Vec3(0, 0, 2));
        n.edges.put(new Edge(0, 1));
        n.edges.put(new Edge(1, 2));
        n.edges.put(new Edge(0, 2));

        return n;
    }

    @Test
    public void testNeighbors() {
        var n = makeTriangle();
        var ns = 0;
        for (var an : n.neighbours(0)) {
            ns++;
        }
        assertEquals(ns, 2);
    }

    @Test
    public void testTriangle() {
        var n = makeTriangle();
        var p = new AStar();
        var path = p.findPath(n, 0, 2);

        assertEquals(path.size(), 2);
        assertEquals(path.get(0), 0);
        assertEquals(path.get(1), 2);
    }

    @Test
    public void testBlocking() {
        var n = makeTriangle();
        var o = new Sphere(0.5f, new Vec3(0, 0, 1)) ;

        n.addObstacle(o);
        var p = new AStar();
        ArrayList<Integer> path;
        
        // Check with sphere not blocking
        n.toggleBlockingOff(o);
        path = p.findPath(n, 0, 2);

        assertEquals(2, path.size());
        assertEquals(0, path.get(0));
        assertEquals(2, path.get(1));

        // Check with sphere blocking
        n.toggleBlockingOn(o);
        path = p.findPath(n, 0, 2);

        assertEquals(path.size(), 3);
        assertEquals(0, path.get(0));
        assertEquals(1, path.get(1));
        assertEquals(2, path.get(2));

        // Check no route
        var o2 = new Sphere(0.3f, new Vec3(0, 0.5f, 1.5f)) ;
        n.addObstacle(o2);
        n.toggleBlockingOn(o2);

        path = p.findPath(n, 0, 2);
        assertEquals(null, path);
    }
}