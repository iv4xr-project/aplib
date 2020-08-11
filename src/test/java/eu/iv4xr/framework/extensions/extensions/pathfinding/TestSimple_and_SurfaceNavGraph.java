package eu.iv4xr.framework.extensions.extensions.pathfinding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.SimpleNavGraph;
import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph;
import eu.iv4xr.framework.spatial.Vec3;
import eu.iv4xr.framework.spatial.meshes.Edge;
import eu.iv4xr.framework.spatial.meshes.Face;
import eu.iv4xr.framework.spatial.meshes.Mesh;

public class TestSimple_and_SurfaceNavGraph {
	
	
	Mesh mesh0() {
		/*
		           (5)
		          /   \
		         /  9  \
		       (3)-----(4)
		      /   \ 8  /
		     /  7  \  /
		   (0)-----(1)
		     \  6  /
		      \   /
		       (2)
		    
		 */
		Mesh mesh = new Mesh() ;
		var v0 = new Vec3(-2,0,0) ;
		var v1 = new Vec3(0,0,0) ;
		var v2 = new Vec3(-1,2,-2) ;
		var v3 = new Vec3(-1,0,2) ;
		var v4 = new Vec3(1,0,2) ;
		var v5 = new Vec3(0,2,4) ;
		mesh.vertices.add(v0) ;
		mesh.vertices.add(v1) ;
		mesh.vertices.add(v2) ;
		mesh.vertices.add(v3) ;
		mesh.vertices.add(v4) ;
		mesh.vertices.add(v5) ;
		mesh.edges.add(new Edge(0,1)) ;
		mesh.edges.add(new Edge(0,2)) ;
		mesh.edges.add(new Edge(2,1)) ;
		mesh.edges.add(new Edge(0,3)) ;
		mesh.edges.add(new Edge(3,1)) ;
		mesh.edges.add(new Edge(3,4)) ;
		mesh.edges.add(new Edge(3,5)) ;
		mesh.edges.add(new Edge(4,5)) ;
		mesh.edges.add(new Edge(4,1)) ;
		int[] f1 = {0,1,2} ;
		int[] f2 = {0,1,3} ;
		int[] f3 = {1,3,4} ;
		int[] f4 = {3,4,5} ;
		var F1 = new Face(f1) ;
		var F2 = new Face(f2) ;
		var F3 = new Face(f3) ;
		var F4 = new Face(f4) ;
		mesh.faces.add(F1) ;
		mesh.faces.add(F2) ;
		mesh.faces.add(F3) ;
		mesh.faces.add(F4) ;
		return mesh ;
	}
	
	//@Test
	public void test_conversion_mesh_to_SimpleNavGraph() {
		SimpleNavGraph navgraph = SimpleNavGraph.fromMeshFaceAverage(mesh0()) ;
		//System.out.println("" + navgraph.vertices.size()) ;
		assertTrue(navgraph.vertices.size() == 4) ;
		assertTrue(Vec3.dist(navgraph.vertices.get(0), new Vec3(-1f,0.66f,-0.66f)) < 0.1) ;
		assertTrue(Vec3.dist(navgraph.vertices.get(1), new Vec3(-1f,0f,0.66f)) < 0.1) ;
		assertTrue(Vec3.dist(navgraph.vertices.get(2), new Vec3(0f,0f,1.33f)) < 0.1) ;
		assertTrue(Vec3.dist(navgraph.vertices.get(3), new Vec3(0f,0.66f,2.66f)) < 0.1) ;
		assertTrue(checkConnection(0,navgraph,1)) ;
		assertTrue(checkConnection(1,navgraph,0,2)) ;
		assertTrue(checkConnection(2,navgraph,1,3)) ;
		assertTrue(checkConnection(3,navgraph,2)) ;
	}
	
	/**
	 * Check if k is connected to exactly the nodes listed in sucs.
	 */
	boolean checkConnection(int k, SimpleNavGraph navgraph, int ... sucs) {
		if (navgraph.edges.get(k).size() != sucs.length) return false ;
		for(int v : sucs) {
			if(! navgraph.edges.get(k).contains(v)) return false ;
		}
	return true ;	
	}
	
	String showConnections(int k, SimpleNavGraph navgraph) {
		StringBuffer sb = new StringBuffer() ;
		sb.append("next(" + k + ") =") ;
		int i = 0 ;
		for(int v : navgraph.edges.get(k)) {
			if (i>0) sb.append(",") ;
			sb.append(" " + v) ;
			i++ ;
		}
		return sb.toString() ;
	}
	
	@Test
	public void test_conversion_mesh_to_SurfaceNavGraph() {
		Mesh mesh = mesh0() ;
		SurfaceNavGraph navgraph = new SurfaceNavGraph(mesh) ;
		assertTrue(navgraph.vertices.size() == 10) ;
		// check if all vertices in the mesh are included in the navgraph
		for (int k=0; k<mesh.vertices.size(); k++) {
			assertTrue(navgraph.vertices.get(k).equals(mesh.vertices.get(k))) ;
		}
		// check the added center-points:
		assertTrue(Vec3.dist(navgraph.vertices.get(6), new Vec3(-1f,0.66f,-0.66f)) < 0.1) ;
		assertTrue(Vec3.dist(navgraph.vertices.get(7), new Vec3(-1f,0f,0.66f)) < 0.1) ;
		assertTrue(Vec3.dist(navgraph.vertices.get(8), new Vec3(0f,0f,1.33f)) < 0.1) ;
		assertTrue(Vec3.dist(navgraph.vertices.get(9), new Vec3(0f,0.66f,2.66f)) < 0.1) ;
		// check the connections:
		System.out.println("##  " + showConnections(0,navgraph)) ;
		System.out.println("##  " + showConnections(1,navgraph)) ;
		System.out.println("##  " + showConnections(2,navgraph)) ;
		System.out.println("##  " + showConnections(6,navgraph)) ;
		System.out.println("##  " + showConnections(7,navgraph)) ;
		
		assertTrue(checkConnection(0,navgraph,1,2,3,6,7)) ;
		assertTrue(checkConnection(1,navgraph,0,2,3,4,6,7,8)) ;
		assertTrue(checkConnection(2,navgraph,0,1,6)) ;
		assertTrue(checkConnection(3,navgraph,0,1,4,5,7,8,9)) ;
		assertTrue(checkConnection(4,navgraph,1,3,5,8,9)) ;
		assertTrue(checkConnection(5,navgraph,3,4,9)) ;
		
		assertTrue(checkConnection(6,navgraph,0,1,2,7)) ;
		assertTrue(checkConnection(7,navgraph,0,1,3,6,8)) ;
		assertTrue(checkConnection(8,navgraph,1,3,4,7,9)) ;
		assertTrue(checkConnection(9,navgraph,3,4,5,8)) ;
		
	}

}
