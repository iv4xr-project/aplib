package eu.iv4xr.framework.extensions.spatial;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TestSphere {

    @Test
    public void testHit() {
        var s = new Sphere(1, Vec3.zero());
        var l = new Line(new Vec3(0, -2, 0), new Vec3(0, 2, 0));

        assertTrue(s.intersects(l));
        assertEquals(s.intersect(l).size(), 2);
    }

    @Test
    public void testTouch() {
        var s = new Sphere(1, Vec3.zero());
        var l = new Line(new Vec3(0.99999f, -2, 0), new Vec3(0.99999f, 2, 0));

        assertTrue(s.intersects(l));
        assertEquals(s.intersect(l).size(), 1);
    }

    @Test
    public void testMiss() { 
        var s = new Sphere(1, Vec3.zero());
        var l = new Line(new Vec3(2, -2, 0), new Vec3(2, 2, 0));

        assertFalse(s.intersects(l));
        assertEquals(s.intersect(l).size(), 0);
    }
}