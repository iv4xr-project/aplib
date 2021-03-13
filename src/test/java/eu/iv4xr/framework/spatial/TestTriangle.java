package eu.iv4xr.framework.spatial;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.spatial.Line;
import eu.iv4xr.framework.spatial.Triangle;
import eu.iv4xr.framework.spatial.Vec3;

public class TestTriangle {

    @Test
    public void testHit() {
        var t = new Triangle(new Vec3(-1, 0, -1), new Vec3(1, 0, 0), new Vec3(-1, 0, 1));
        var l = new Line(new Vec3(0, -2, 0), new Vec3(0, 2, 0));

        assertTrue(t.intersects(l));
    }

    @Test
    public void testMiss() {
        var t = new Triangle(new Vec3(-1, 0, -1), new Vec3(1, 0, 0), new Vec3(-1, 0, 1));
        var l = new Line(new Vec3(1, -2, 1), new Vec3(1, 2, 1));
        assertFalse(t.intersects(l));

        var l2 = new Line(new Vec3(0, -4, 0), new Vec3(0, -2, 0));
        assertFalse(t.intersects(l2));
    }
}