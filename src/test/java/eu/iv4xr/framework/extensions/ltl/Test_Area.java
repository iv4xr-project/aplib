package eu.iv4xr.framework.extensions.ltl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import static eu.iv4xr.framework.extensions.ltl.Area.* ;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;


public class Test_Area {
	
	RectangleArea A1 = new RectangleArea(new Vec3(0,0,0),2,1) ;
	RectangleArea A2 = new RectangleArea(new Vec3(0.5f,0,0), new Vec3(2,0,1)) ;
	RectangleArea A3 = new RectangleArea(new Vec3(-0.5f,0,0), new Vec3(2,0,1)) ;
	RectangleArea A4 = new RectangleArea(new Vec3(-0.5f,0,0), new Vec3(2.5f,0,1)) ;
	RectangleArea A5 = new RectangleArea(new Vec3(2,0,0), new Vec3(4,0,2)) ;
	
	Pair<Vec3,Vec3> vx0 = new Pair<>(new Vec3(-1,0,0), new Vec3(0,1,1)) ;
	Pair<Vec3,Vec3> vx1 = new Pair<>(new Vec3(0,0,0), new Vec3(1,1,1)) ;
	Pair<Vec3,Vec3> vx2 = new Pair<>(new Vec3(1,0,0), new Vec3(2,1,1)) ;
	
	Set<Pair<Vec3,Vec3>> voxelset(Pair<Vec3,Vec3> ... vxs) {
		Set<Pair<Vec3,Vec3>> set = new HashSet<>() ;
		for (var vx : vxs) set.add(vx) ;
		return set ;
	}
	
	<T> List<T> list(T ... xs) {
		List<T> z = new LinkedList<>() ;
		for(T a : xs) z.add(a) ;
		return z ;
	}
	
	@Test
	public void test1() {
		assertTrue(A1.voxels.size() == 2) ;
		assertTrue(A2.voxels.size() == 2) ;
		assertTrue(A3.voxels.size() == 3) ;
		assertTrue(A4.voxels.size() == 4) ;
		
		for (var vx : A1.voxels) System.out.println("" + vx) ;
		
		assertEquals(A1.voxels, voxelset(vx1,vx2)) ;
		assertEquals(A2.voxels, voxelset(vx1,vx2)) ;
		assertEquals(A3.voxels, voxelset(vx0,vx1,vx2)) ;
	}
	
	@Test
	public void test_union() {
		Area B1 = A1.union(A2) ;
		assertTrue(B1.voxels.size() == 2) ;
		assertEquals(B1.voxels, voxelset(vx1,vx2)) ;
		Area B2 = A1.union(A3) ;
		assertTrue(B2.voxels.size() == 3) ;
		assertEquals(B2.voxels, voxelset(vx0,vx1,vx2)) ;
		
		Area B3 = A1.union(A5) ;
		assertTrue(B3.voxels.size() == 6) ;
	}
	
	@Test
	public void test_intersect() {
		Area B1 = A1.intersect(A2) ;
		assertTrue(B1.voxels.size() == 2) ;
		assertEquals(B1.voxels, voxelset(vx1,vx2)) ;
		Area B2 = A1.intersect(A3) ;
		assertTrue(B2.voxels.size() == 2) ;
		assertEquals(B2.voxels, voxelset(vx1,vx2)) ;
		Area B3 = A1.intersect(A5) ;
		assertTrue(B3.voxels.size() == 0) ;
	}
	
	@Test
	public void test_minus() {
		Area B1 = A1.minus(A2) ;
		assertTrue(B1.voxels.size() == 0) ;
		Area B2 = A1.minus(A3) ;
		assertTrue(B2.voxels.size() == 0) ;
		Area B2b = A3.minus(A1) ;
		assertTrue(B2b.voxels.size() == 1) ;
		assertEquals(B2b.voxels, voxelset(vx0)) ;
		Area B3 = A1.minus(A5) ;
		assertTrue(B3.voxels.size() == 2) ;
	}
	
	@Test
	public void test_coverage_calculation() {
		List<Vec3> path = list(
				new Vec3(0,0,0.5f), // in A1
				new Vec3(1,0,0),    // in A1
				new Vec3(1,0,0.5f), // in the same voxel as (1,0,0)
				new Vec3(2,0,0.5f)  // outside A1
				) ;
		System.out.println("Covered: " + A1.covered(path)) ;
		assertEquals(A1.covered(path).size(),2) ;
		assertEquals(A1.coveredPortion(path),1.0f) ;			
	}

}
