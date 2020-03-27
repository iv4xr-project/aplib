package eu.iv4xr.framework.extensions.spatial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestVec3 {
    
    @Test
    public void testLength() {
        assertEquals(2, new Vec3(0, 0, 2).length());
        assertEquals(2, new Vec3(0, 2, 0).length());
        assertEquals(2, new Vec3(2, 0, 0).length());

        assertEquals(5, new Vec3(3, 4, 0).length());
    }

    @Test
    public void testNormalize() {
        assertEquals(1, new Vec3(0, 3, 4).normalized().length());
    }
}