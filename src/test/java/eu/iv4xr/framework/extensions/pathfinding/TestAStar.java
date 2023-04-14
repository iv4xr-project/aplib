package eu.iv4xr.framework.extensions.pathfinding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.utils.Pair;

public class TestAStar {
	
	float dist(Pair<Float,Float> p,Pair<Float,Float> q) {
		float dx = p.fst - q.fst ;
		float dy = p.snd - q.snd ;
		return (float) Math.sqrt((double) (dx*dx + dy*dy)) ;
	}
	
	Pair<Float,Float> node(float x, float y) {
		return new Pair<>(x,y) ;
	}
	
	
	/*
	 *  Edges are bidirectional:
	 *  
	         (1,1)---(2,1)---(3,1)
	        /  |       |    /
	       /   |       |   /
	      /    |       |  /
	 (0,0)---(1,0)---(2,0)   (3,0)----(4,0)
	  
	 */
	NavGraph<Pair<Float,Float>> G1() {
		var g = new NavGraph<Pair<Float,Float>>() ;
		g.heuristicDistance = (p,q) -> dist(p,q) ;
		g.neighborDistance = (p,q) -> dist(p,q) ;
		g.addBidirectionalEdge(node(0,0),node(1,0)) ;
		g.addBidirectionalEdge(node(0,0),node(1,1)) ;
		g.addBidirectionalEdge(node(1,0),node(2,0)) ;
		g.addBidirectionalEdge(node(1,0),node(1,1)) ;
		g.addBidirectionalEdge(node(1,1),node(2,1)) ;
		g.addBidirectionalEdge(node(2,1),node(2,0)) ;
		g.addBidirectionalEdge(node(2,1),node(3,1)) ;
		g.addBidirectionalEdge(node(2,0),node(3,1)) ;
		
		g.addBidirectionalEdge(node(3,0),node(4,0)) ;
		
		return g ;
	}
	
	boolean isPath(NavGraph<Pair<Float,Float>> G, List<Pair<Float,Float>> sigma) {
		if (sigma.size() <= 1) return true ;
		for(int k=0; k<sigma.size()-1; k++) {
			var p = sigma.get(k) ;
			var q = sigma.get(k+1) ;
			var ns = G.edges.get(p) ;
			if (ns==null || !ns.contains(q)) return false ;
		}
		return true ;
	}
	
	@Test
	public void test1() {
		var G1 = G1() ;
		var astar = new AStar<Pair<Float,Float>>() ;
		var p = node(0,0) ;
		var q = node(0,0) ;
		var path = astar.findPath(G1,p,q) ;
		System.out.println("path from " + p + " to " + q + ": " + path) ;
		assertTrue(path.size() == 1) ;
		assertTrue(path.contains(p)) ;
		
		q = node(1,0) ;
		path = astar.findPath(G1,p,q) ;
		System.out.println("path from " + p + " to " + q + ": " + path) ;
		assertTrue(path.size() == 2) ;
		assertTrue(path.contains(p)) ;
		assertTrue(path.contains(q)) ;
		
		q = node(2,0) ;
		path = astar.findPath(G1,p,q) ;
		System.out.println("path from " + p + " to " + q + ": " + path) ;
		assertTrue(path.size() == 3) ;
		assertTrue(path.contains(p)) ;
		assertTrue(path.contains(q)) ;
		assertTrue(isPath(G1,path)) ;
		
		q = node(3,1) ;
		path = astar.findPath(G1,p,q) ;
		System.out.println("path from " + p + " to " + q + ": " + path) ;
		assertTrue(path.size() == 4) ;
		assertTrue(path.contains(p)) ;
		assertTrue(path.contains(q)) ;
		assertTrue(isPath(G1,path)) ;
		
		q = node(4,0) ;
		path = astar.findPath(G1,p,q) ;
		assertTrue(path == null) ;
	}
	
	@Test
	public void test2() {
		// as in G1, but we add an edge (0,0) -- (3,1) with high cost:
		var G1 = G1() ;
		G1.addBidirectionalEdge(node(0,0), node(3,1));
		G1.neighborDistance = (p,q) -> 
		   p.equals(node(0,0)) && q.equals(node(3,1)) ? 10 : dist(p,q) ;
		var p = node(0,0) ;
		var q = node(3,1) ;
		var astar = new AStar<Pair<Float,Float>>() ;
		var path = astar.findPath(G1,p,q) ;
		System.out.println("path from " + p + " to " + q + ": " + path) ;
		assertTrue(path.size() == 4) ;
		assertTrue(path.contains(p)) ;
		assertTrue(path.contains(q)) ;
		assertTrue(isPath(G1,path)) ;	
		
		// as in G1, but we add an edge (0,0) -- (3,1) with low cost:
		G1 = G1() ;
		G1.addBidirectionalEdge(node(0,0), node(3,1));
		G1.neighborDistance = (a,b) -> 
		   a.equals(node(0,0)) && b.equals(node(3,1)) ? 2 : dist(a,b) ;
		p = node(0,0) ;
		q = node(3,1) ;
		path = astar.findPath(G1,p,q) ;
		System.out.println("path from " + p + " to " + q + ": " + path) ;
		assertTrue(path.size() == 2) ;
		assertTrue(path.contains(p)) ;
		assertTrue(path.contains(q)) ;
	}

}
