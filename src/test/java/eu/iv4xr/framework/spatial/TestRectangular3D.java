package eu.iv4xr.framework.spatial;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestRectangular3D {
	
	@Test
	public void testHit() {
		// rectangular with center (1,0,0) and width 2 :
		var rect = new Box(new Vec3(1,0,0), new Vec3(2,2,2)) ;
		
		// lines that intersect two sides:
		
		var line = new Line(new Vec3(-1,0,0), new Vec3(3,0,0)) ;
		Vec3[] dummy = {} ;
		System.out.println("** " + rect.intersect(line)) ;
		var intersections = rect.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 2) ;
		assertTrue(intersections[0].equals(new Vec3(0,0,0))) ;
		assertTrue(intersections[1].equals(new Vec3(2,0,0))) ;
		
		line = new Line(new Vec3(0,0,0), new Vec3(2,0,0)) ;
		System.out.println("** " + rect.intersect(line)) ;
		intersections = rect.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 2) ;
		assertTrue(intersections[0].equals(new Vec3(0,0,0))) ;
		assertTrue(intersections[1].equals(new Vec3(2,0,0))) ;
		
		line = new Line(new Vec3(1,0,2), new Vec3(1,0,-2)) ;
		System.out.println("** " + rect.intersect(line)) ;
		intersections = rect.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 2) ;
		assertTrue(intersections[0].equals(new Vec3(1,0,-1))) ;
		assertTrue(intersections[1].equals(new Vec3(1,0,1))) ;
		
		// line that slide along on one of the sides (and intersecting two other sides)
		line = new Line(new Vec3(0,0,-1), new Vec3(0,0,1)) ;
		System.out.println("** " + rect.intersect(line)) ;
		intersections = rect.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 2) ;
		assertTrue(intersections[0].equals(new Vec3(0,0,-1))) ;
		assertTrue(intersections[1].equals(new Vec3(0,0,1))) ;
		
		// line that ends inside the rect; so it only intersects one side:
		line = new Line(new Vec3(1,-2,0), new Vec3(1,0,0)) ;
		System.out.println("** " + rect.intersect(line)) ;
		intersections = rect.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 1) ;
		assertTrue(intersections[0].equals(new Vec3(1,-1,0))) ;		
	}
	
	@Test
	public void testTouch() {
		// rectangular with center (1,0,0) and width 2 :
		var rect = new Box(new Vec3(1,0,0), new Vec3(2,2,2)) ;
		
		// line that touches one side:
		var line = new Line(new Vec3(-1,3,3), new Vec3(0,0,0)) ;
		Vec3[] dummy = {} ;
		System.out.println("** " + rect.intersect(line)) ;
		var intersections = rect.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 1) ;
		assertTrue(intersections[0].equals(new Vec3(0,0,0))) ;
		
		// line that touches one corner:
		line = new Line(new Vec3(-1,3,3), new Vec3(0,1,1)) ;
		System.out.println("** " + rect.intersect(line)) ;
		intersections = rect.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 1) ;
		assertTrue(intersections[0].equals(new Vec3(0,1,1))) ;
	}
	
	@Test
	public void testMiss() {
		// rectangular with center (1,0,0) and width 2 :
		var rect = new Box(new Vec3(1,0,0), new Vec3(2,2,2)) ;
		
		// line that misses the rectangle
		var line = new Line(new Vec3(0,2,2), new Vec3(2,2,2)) ;
		assertTrue(rect.intersect(line).size() == 0) ;
		line = new Line(new Vec3(-1,4,2), new Vec3(2,2,2)) ;
		assertTrue(rect.intersect(line).size() == 0) ;
	}

}
