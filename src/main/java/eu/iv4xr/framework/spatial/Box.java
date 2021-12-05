package eu.iv4xr.framework.spatial;

import java.util.Collection;
import java.util.HashSet;

/**
 * Representing a 3D rectangular shape. For now, we don't support rotation.
 */
public class Box implements LineIntersectable {

    public Vec3 center;

    /**
     * The x,y, and z width. Should be >0. So, from the center, the rectangle extend
     * 1/2 its width to each of the corresponding direction.
     */
    public Vec3 width;

    float epsilon = 0.0001f;

    public Box(Vec3 center, Vec3 width) {
        this.center = center;
        this.width = width;
    }

    /**
     * In typical cases, if a line intersects with a 3D rectangle, it will intersect
     * at one point (if the line ends somewhere inside the rectangle), or at two
     * points (if the line pass through the whole rectangle).
     * 
     * There are two a bit exceptional case:
     * 
     * (1) the line touches one of the rectangle corner. Although technically it
     * intersects with three of the rectangle's surfaces, the intersection points
     * will all be the the same, namely the corner. So, we will only return this
     * single intersection point.
     * 
     * (2) the line slides along one of the rectangle's surfaces. Technically, it
     * will then intersects at infinite number of points. However we will only
     * return the points where the line would go through other surfaces (the one
     * that it does NOT slide on).
     * 
     * Bearing those special cases in mind, this method will therefore return at
     * most 2 intersection points.
     */
    @Override
    public Collection<Vec3> intersect(Line l) {
        float minX = center.x - width.x / 2;
        float minY = center.y - width.y / 2;
        float minZ = center.z - width.z / 2;
        float maxX = center.x + width.x / 2;
        float maxY = center.y + width.y / 2;
        float maxZ = center.z + width.z / 2;

        Collection<Vec3> intersections = new HashSet<>();

        // calculate intersections between the line and the six surfaces of the
        // rectangle-3D:

        // intersection with the infinite plane z = minZ :
        Vec3 i = intersectPlaneXY(l.a, l.b, minZ);
        if (i != null) {
            if (between(minX, i.x, maxX) && between(minY, i.y, maxY)) {
                // the intersection point i lies in the finite plane between xmin,xmax and
                // ymin,ymax
                intersections.add(i);
            }
        }
        // intersection with the infinite plane z = maxZ :
        i = intersectPlaneXY(l.a, l.b, maxZ);
        if (i != null) {
            if (between(minX, i.x, maxX) && between(minY, i.y, maxY)) {
                // the intersection point i lies in the finite plane between xmin,xmax and
                // ymin,ymax
                intersections.add(i);
            }
        }
        if (intersections.size() == 2)
            return intersections;

        // intersection with the infinite plane y = minZ :
        i = intersectPlaneXZ(l.a, l.b, minY);
        if (i != null) {
            if (between(minX, i.x, maxX) && between(minZ, i.z, maxZ)) {
                intersections.add(i);
            }
        }
        if (intersections.size() == 2)
            return intersections;

        i = intersectPlaneXZ(l.a, l.b, maxY);
        if (i != null) {
            if (between(minX, i.x, maxX) && between(minZ, i.z, maxZ)) {
                intersections.add(i);
            }
        }
        if (intersections.size() == 2)
            return intersections;

        // intersection with the infinite plane x = minX :
        i = intersectPlaneYZ(l.a, l.b, minX);
        if (i != null) {
            if (between(minY, i.y, maxY) && between(minZ, i.z, maxZ)) {
                intersections.add(i);
            }
        }
        if (intersections.size() == 2)
            return intersections;

        i = intersectPlaneYZ(l.a, l.b, maxX);
        if (i != null) {
            if (between(minY, i.y, maxY) && between(minZ, i.z, maxZ)) {
                intersections.add(i);
            }
        }
        return intersections;
    }

    /**
     * Check if x is between the given lower and upperbound, with additional epsilon
     * as margin.
     */
    boolean between(float lowerbound, float x, float upperbound) {
        return lowerbound - epsilon <= x && x <= upperbound + epsilon;
    }

    /**
     * Calculate the intersection of the line between p and q, and a plane XY where
     * z = c.
     */
    Vec3 intersectPlaneXY(Vec3 p, Vec3 q, float c) {
        Vec3 direction = Vec3.sub(q, p);
        if (direction.z == 0) {
            // The line is parallel with the plane, so it can't intersect the plane.
            // One special case is of the line is literally on the plane. We will call
            // the line as having to intersection. It will intersect two other planes
            // though.
            return null;
        }
        float t = (c - p.z) / direction.z;
        if (t < 0 || t > 1) {
            // the intersection lies outside the line (not between p and q)
            return null;
        }
        return Vec3.add(p, Vec3.mul(direction, t));
    }

    /**
     * Calculate the intersection of the line between p and q, and a plane XZ where
     * y = c.
     */
    Vec3 intersectPlaneXZ(Vec3 p, Vec3 q, float c) {
        Vec3 direction = Vec3.sub(q, p);
        if (direction.y == 0)
            return null;
        float t = (c - p.y) / direction.y;
        if (t < 0 || t > 1) {
            // the intersection lies outside the line (not between p and q)
            return null;
        }
        return Vec3.add(p, Vec3.mul(direction, t));
    }

    /**
     * Calculate the intersection of the line between p and q, and a plane YZ where
     * x = c.
     */
    Vec3 intersectPlaneYZ(Vec3 p, Vec3 q, float c) {
        Vec3 direction = Vec3.sub(q, p);
        if (direction.x == 0)
            return null;
        float t = (c - p.x) / direction.x;
        if (t < 0 || t > 1) {
            // the intersection lies outside the line (not between p and q)
            return null;
        }
        return Vec3.add(p, Vec3.mul(direction, t));
    }

    // just for testing few things
    public static void main(String[] args) {
        var box = new Box(new Vec3(0f, 0f, 0f), new Vec3(0.01f, 0.01f, 0.01f));
        var line = new Line(new Vec3(-0.1f, 0f, -0.1f), new Vec3(0f, 0f, 0f));
        System.out.println("Intersections: " + box.intersect(line));
    }

}
