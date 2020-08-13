package eu.iv4xr.framework.spatial.meshes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;

public class TestFace {

	
	Pair<Face,ArrayList<Vec3>> triangle() {
		var v0 = new Vec3(-2,0,0) ;
		var v1 = new Vec3(0,0,0) ;
		var v2 = new Vec3(-1,0,2) ;
		int[] face_ = {0,1,2} ;
		Face f = new Face(face_) ;
		ArrayList<Vec3> concretes = new ArrayList<>() ;
		concretes.add(v0) ;
		concretes.add(v1) ;
		concretes.add(v2) ;
		return new Pair(f,concretes) ;
	}
	
	/* A rectangle 2x2, with (0,0,0) at node 0, raised by y=2 at nodes 2,3
	  
	   (2)----(3)
	    |      |
	    |      |
	   (0)----(1)
	 */
	Pair<Face,ArrayList<Vec3>> rectangle() {
		
		 	var v0 = new Vec3(0,0,0) ;
		 	var v1 = new Vec3(2,0,0) ;
		 	var v2 = new Vec3(0,2,2) ;
		 	var v3 = new Vec3(2,2,2) ;
		 	
		 	int[] vertices = {0,1,3,2} ; // the order should make sure the edges are consecutivelly connected
		 	ArrayList<Vec3> concreteVertices = new ArrayList<>() ;
		 	concreteVertices.add(v0) ;
		 	concreteVertices.add(v1) ;
		 	concreteVertices.add(v3) ;
		 	concreteVertices.add(v2) ;
		 	Face face = new Face(vertices) ;
		 	return new Pair(face,concreteVertices) ;
	}
	
	/**
	 * Test the method for calculating the 3D distance between a point to a Face.
	 */
	@Test
	public void test1distFromPoint_to_rectangleface() {
		
	 	var testdata = rectangle() ;
	 	var face = testdata.fst ;
	 	var concreteVertices = testdata.snd ;
	 	
	 	var point = new Vec3(0,0,0) ;
	 	var dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist == 0) ;
	 	
		point = new Vec3(1,1,1) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist==0) ;
	 	
	 	point = new Vec3(1,0,1) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist>0 && dist <1) ;
	 	
	 	point = new Vec3(1,2,1) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist>0 && dist <1) ;
	 	
	 	point = new Vec3(0,1,0) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist>0 && dist <1) ;
	 	
	 	point = new Vec3(0,-1,0) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist>0.5 && dist <2) ;
	 	
	 	point = new Vec3(-1,-10,0) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist>10 && dist <11) ;
	 	
	}
	
	@Test
	public void test2_distFromPoint_to_triangleface() {
		
	 	var testdata = triangle() ;
	 	var face = testdata.fst ;
	 	var concreteVertices = testdata.snd ;
	 	
	 	var point = new Vec3(0,0,0) ;
	 	var dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist == 0) ;
	 	
	 	point = new Vec3(-1,0,1) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist == 0) ;
	 	
	 	point = new Vec3(-1,0,2) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist == 0) ;
	 	
	 	point = new Vec3(-0.2f,0,0.1f) ;
	 	dist = face.distFromPoint(point,concreteVertices) ;
	 	System.out.println("** " + point + ", dist = " + dist) ;
	 	assertTrue(dist == 0) ;
	
	}
}
