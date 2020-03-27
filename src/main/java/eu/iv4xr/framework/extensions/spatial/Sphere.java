package eu.iv4xr.framework.extensions.spatial;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Sphere with line intersections.
 * 
 * @author Naraenda 
 */
public class Sphere implements LineIntersectable {
    static float EPSILON = 0.0001f;

    public float radius;
    public Vec3  center;

    public Sphere(float radius, Vec3 center) {
        this.radius = radius;
        this.center = center;
    }

    /**
     * Line-sphere intersection:
     * https://en.wikipedia.org/wiki/Line%E2%80%93sphere_intersection
     */
    @Override
    public Collection<Vec3> intersect(Line l) {
        var res = new ArrayList<Vec3>();
        Vec3  oc   = Vec3.sub(l.origin(), center);
        float loc  = (float)Vec3.dot(l.direction(), oc);
        float d_sq = (float) 
            ( Math.pow(loc, 2)
            - Math.pow(oc.length(), 2) 
            + Math.pow(radius, 2));

        // Square root is negative => no intersection
        if (d_sq < 0)
            return res;
        float d_lh = -loc;

        // Value is (near) 0 => 1 intersection
        if (Math.abs(d_sq) < EPSILON){
            // Guard agains points outside [a,b] range
            if (d_lh > 0 && d_lh < l.length()) 
                res.add(l.alongLine(d_lh));
            
            return res;
        }

        // Value is positive => 2 intersections
        float d_sqrt = (float) Math.sqrt(d_sq);
        float d1 = d_lh + d_sqrt;
        if (d1 > 0 && d1 < l.length()) 
            res.add(l.alongLine(d1));
            
        float d2 = d_lh + d_sqrt;
        
        if (d2 > 0 && d2 < l.length()) 
            res.add(l.alongLine(d2));
        return res;
    }
}