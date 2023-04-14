package eu.iv4xr.framework.spatial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

//import eu.iv4xr.framework.spatial.Vec3;

public class TestVec3 {

    @Test
    public void testLength() {
        assertEquals(2, new Vec3(0, 0, 2).length());
        assertEquals(2, new Vec3(0, 2, 0).length());
        assertEquals(2, new Vec3(2, 0, 0).length());
        assertEquals(5, new Vec3(3, 4, 0).length());
    }

    @Test
    public void testDistSq() {
    	assertEquals(2, Vec3.distSq(new Vec3(0,0,0), new Vec3(1,1,0)));
    	assertEquals(3, Vec3.distSq(new Vec3(0,0,0), new Vec3(1,1,1)));
        assertEquals(3, Vec3.distSq(new Vec3(0,0,0), new Vec3(1,1,-1)));
        assertEquals(4, Vec3.distSq(new Vec3(0,0,0), new Vec3(2,0,0)));
    }

    @Test
    public void testDist() {
        float epsilon = 0.0001f;
    	assertEquals(Math.sqrt(2), Vec3.dist(new Vec3(0,0,0), new Vec3(1,1,0)),epsilon);
    	assertEquals(Math.sqrt(3), Vec3.dist(new Vec3(0,0,0), new Vec3(1,1,1)),epsilon);
        assertEquals(Math.sqrt(3), Vec3.dist(new Vec3(0,0,0), new Vec3(1,1,-1)),epsilon);
        assertEquals(2, Vec3.dist(new Vec3(0,0,0), new Vec3(2,0,0)),epsilon);
    }

    @Test
    public void testDot() {
        assertEquals(0, Vec3.dot(new Vec3(0,0,0), new Vec3(1,1,0)));
        assertEquals(1, Vec3.dot(new Vec3(0,0,1), new Vec3(0,0,1)));
        assertEquals(-1, Vec3.dot(new Vec3(0,0,1), new Vec3(0,0,-1)));
        assertEquals(5, Vec3.dot(new Vec3(0,1,3), new Vec3(0,2,1)));
        assertEquals(0, Vec3.dot(new Vec3(0,0,1), new Vec3(0,1,0)));
        assertEquals(0, Vec3.dot(new Vec3(0,0,1), new Vec3(1,0,0)));
    }

    @Test
    public void testNormalize() {
        assertEquals(1, new Vec3(0, 3, 4).normalized().length());
    }

    @Test
    public void testIsEquals() {
        Vec3 v1 = new Vec3(1, 2, 3);
        Vec3 v2 = new Vec3(1, 2, 3);
        Vec3 v3 = new Vec3(1, 2, 3.01f);
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
        assertEquals(v1, v1);
        assert(!v1.equals(v3));
        assert(!v1.equals(null));
        assert(!v1.equals(new Object()));
    }

    @Test
    public void testAdd() {
        Vec3 v1 = new Vec3(1, 2, 3);
        Vec3 v2 = new Vec3(1, 2, 3);
        Vec3 v3 = new Vec3(2, 4, 6);
        assertEquals(v3, Vec3.add(v1,v2));
    }

    @Test
    public void testSub() {
        Vec3 v1 = new Vec3(1, 2, 3);
        Vec3 v2 = new Vec3(1, 2, 3);
        Vec3 v3 = new Vec3(0, 0, 0);
        assertEquals(v3,Vec3.sub(v1,v2));
    }

    @Test
    public void testScale() {
        Vec3 v1 = new Vec3(1, 2, 3);
        Vec3 v2 = new Vec3(2, 4, 6);
        assertEquals(v2, Vec3.mul(v1,2));
    }

    @Test
    public void testCross() {
        Vec3 v1 = new Vec3(1, 2, 3);
        Vec3 v2 = new Vec3(1, 2, 3);
        Vec3 v3 = new Vec3(0, 0, 0);
        assertEquals(v3, Vec3.cross(v1,v2));
    }
}