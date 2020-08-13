package eu.iv4xr.framework.spatial;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestBox {
	
	@Test
	public void testHit() {
		// small box:
		var box = new Box(new Vec3(0,0,0), new Vec3(0.1f, 0.1f, 0.1f)) ;
		var line = new Line(new Vec3(-0.1f,0,-0.1f), new Vec3(0,0,0)) ;
		Vec3[] dummy = {} ;
		var intersections = box.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 1) ;
		System.out.println("** " + box.intersect(line)) ;
		assertTrue(intersections[0].equals(new Vec3(-0.05f,0,-0.05f))) ;
		
		
		// rectangular with center (1,0,0) and width 2 :
		box = new Box(new Vec3(1,0,0), new Vec3(2,2,2)) ;
		
		// lines that intersect two sides:
		
		line = new Line(new Vec3(-1,0,0), new Vec3(3,0,0)) ;
		System.out.println("** " + box.intersect(line)) ;
		intersections = box.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 2) ;
		assertTrue(intersections[0].equals(new Vec3(0,0,0))) ;
		assertTrue(intersections[1].equals(new Vec3(2,0,0))) ;
		
		line = new Line(new Vec3(0,0,0), new Vec3(2,0,0)) ;
		System.out.println("** " + box.intersect(line)) ;
		intersections = box.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 2) ;
		assertTrue(intersections[0].equals(new Vec3(0,0,0))) ;
		assertTrue(intersections[1].equals(new Vec3(2,0,0))) ;
		
		line = new Line(new Vec3(1,0,2), new Vec3(1,0,-2)) ;
		System.out.println("** " + box.intersect(line)) ;
		intersections = box.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 2) ;
		assertTrue(intersections[0].equals(new Vec3(1,0,-1))) ;
		assertTrue(intersections[1].equals(new Vec3(1,0,1))) ;
		
		// line that slide along on one of the sides (and intersecting two other sides)
		line = new Line(new Vec3(0,0,-1), new Vec3(0,0,1)) ;
		System.out.println("** " + box.intersect(line)) ;
		intersections = box.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 2) ;
		assertTrue(intersections[0].equals(new Vec3(0,0,-1))) ;
		assertTrue(intersections[1].equals(new Vec3(0,0,1))) ;
		
		// line that ends inside the rect; so it only intersects one side:
		line = new Line(new Vec3(1,-2,0), new Vec3(1,0,0)) ;
		System.out.println("** " + box.intersect(line)) ;
		intersections = box.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 1) ;
		assertTrue(intersections[0].equals(new Vec3(1,-1,0))) ;		
	}
	
	@Test
	public void testTouch() {
		// rectangular with center (1,0,0) and width 2 :
		var box = new Box(new Vec3(1,0,0), new Vec3(2,2,2)) ;
		
		// line that touches one side:
		var line = new Line(new Vec3(-1,3,3), new Vec3(0,0,0)) ;
		Vec3[] dummy = {} ;
		System.out.println("** " + box.intersect(line)) ;
		var intersections = box.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 1) ;
		//System.out.println("**** dist = " + Vec3.dist(intersections[0],new Vec3(0,0,0))) ;
		assertTrue(intersections[0].equals(new Vec3(0,0,0))) ;
		
		// line that touches one corner:
		line = new Line(new Vec3(-1,3,3), new Vec3(0,1,1)) ;
		System.out.println("** " + box.intersect(line)) ;
		intersections = box.intersect(line).toArray(dummy) ;
		assertTrue(intersections.length == 1) ;
		assertTrue(intersections[0].equals(new Vec3(0,1,1))) ;
	}
	
	@Test
	public void testMiss() {
		// rectangular with center (1,0,0) and width 2 :
		var box = new Box(new Vec3(1,0,0), new Vec3(2,2,2)) ;
		
		// line that misses the rectangle
		var line = new Line(new Vec3(0,2,2), new Vec3(2,2,2)) ;
		assertTrue(box.intersect(line).size() == 0) ;
		line = new Line(new Vec3(-1,4,2), new Vec3(2,2,2)) ;
		assertTrue(box.intersect(line).size() == 0) ;
	}

}
