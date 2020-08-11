package eu.iv4xr.framework.spatial.meshes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;

public class TestFace {

	
	Pair<Face,ArrayList<Vec3>> triangle() {
		var v0 = new Vec3(1,0,1) ;
		var v1 = new Vec3(3,0,1) ;
		var v2 = new Vec3(2,0,3) ;
		int[] face_ = {0,1,2} ;
		Face f = new Face(face_) ;
		ArrayList<Vec3> concretes = new ArrayList<>() ;
		concretes.add(v0) ;
		concretes.add(v1) ;
		concretes.add(v2) ;
		return new Pair(f,concretes) ;
		
	}
	
	Pair<Face,ArrayList<Vec3>> rectangle() {
		var v0 = new Vec3(-1,0,-1) ;
		var v1 = new Vec3(1,0,-1) ;
		var v2 = new Vec3(1,0,1) ;
		var v3 = new Vec3(-1,0,1) ;
		int[] face_ = {0,1,2,3} ;
		Face f = new Face(face_) ;
		ArrayList<Vec3> concretes = new ArrayList<>() ;
		concretes.add(v0) ;
		concretes.add(v1) ;
		concretes.add(v2) ;
		concretes.add(v3) ;
		return new Pair(f,concretes) ;
	}
	
	/**
	 * Test the method for deciding if a point is inside a face, assuming the face is a
	 * convex polygon. We only look at the X and Z coordinates.
	 */
	@Test
	public void coversPointXZ() {
		// test with triangle:
		var triangle_ = triangle() ;
		var triangle_face = triangle_.fst ;
		var vertices = triangle_.snd ;
		
		assertTrue(triangle_face.coversPointXZ(new Vec3(2,0,2), vertices)) ;
		assertTrue(triangle_face.coversPointXZ(new Vec3(1,0,1), vertices)) ;
		assertTrue(triangle_face.coversPointXZ(new Vec3(1.1f,0,1), vertices)) ;
		assertTrue(triangle_face.coversPointXZ(new Vec3(1.1f,0,1.1f), vertices)) ;
		assertTrue(triangle_face.coversPointXZ(new Vec3(2,10,3f), vertices)) ;
		assertTrue(triangle_face.coversPointXZ(new Vec3(2.1f,10,2.8f), vertices)) ;
		
		
		assertFalse(triangle_face.coversPointXZ(new Vec3(0,0,0), vertices)) ;
		assertFalse(triangle_face.coversPointXZ(new Vec3(0.9f,0,1), vertices)) ;
		assertFalse(triangle_face.coversPointXZ(new Vec3(1,10,0.9f), vertices)) ;
		assertFalse(triangle_face.coversPointXZ(new Vec3(3,10,3.1f), vertices)) ;
		assertFalse(triangle_face.coversPointXZ(new Vec3(2.1f,10,3f), vertices)) ;
		
		// test with a rectangle:
		var rect_ = rectangle() ;
		var rect_face = rect_.fst ;
		vertices = rect_.snd ;
		
		assertTrue(rect_face.coversPointXZ(new Vec3(-1,0,-1), vertices)) ;
		assertTrue(rect_face.coversPointXZ(new Vec3(-0.9f,0,-1), vertices)) ;
		assertTrue(rect_face.coversPointXZ(new Vec3(-1,10,-0.9f), vertices)) ;
		assertTrue(rect_face.coversPointXZ(new Vec3(-0.9f,10,-0.9f), vertices)) ;
		assertTrue(rect_face.coversPointXZ(new Vec3(1,0,1), vertices)) ;
		assertTrue(rect_face.coversPointXZ(new Vec3(0.9f,0,1), vertices)) ;
		assertTrue(rect_face.coversPointXZ(new Vec3(1,10,0.9f), vertices)) ;
		assertTrue(rect_face.coversPointXZ(new Vec3(0.9f,10,0.9f), vertices)) ;
		
		assertFalse(rect_face.coversPointXZ(new Vec3(-1,0,1.1f), vertices)) ;
		assertFalse(rect_face.coversPointXZ(new Vec3(-1.1f,10,1f), vertices)) ;
		assertFalse(rect_face.coversPointXZ(new Vec3(-1.1f,0,1.1f), vertices)) ;
		
		assertFalse(rect_face.coversPointXZ(new Vec3(1.1f,0,-1), vertices)) ;
		assertFalse(rect_face.coversPointXZ(new Vec3(1,10,-1.1f), vertices)) ;
		assertFalse(rect_face.coversPointXZ(new Vec3(1.1f,0,-1.1f), vertices)) ;
		
		
	}
}
