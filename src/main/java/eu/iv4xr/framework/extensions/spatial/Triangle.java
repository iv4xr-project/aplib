package eu.iv4xr.framework.extensions.spatial;

import java.util.ArrayList;
import java.util.Collection;

public class Triangle implements LineIntersectable {

    public boolean isBlocking = true;
    public Vec3 A;
    public Vec3 B;
    public Vec3 C;

    public Triangle(Vec3 a, Vec3 b, Vec3 c) {
        A = a;
        B = b;
        C = c;
    }

    @Override
    /**
     * Müller-Trumbore intersection:
     * https://en.wikipedia.org/wiki/M%C3%B6ller%E2%80%93Trumbore_intersection_algorithm
     */
    public Collection<Vec3> intersect(Line l) {
        // Adapted from https://stackoverflow.com/questions/42740765/intersection-between-line-and-triangle-in-3d/42752998#42752998
        var result = new ArrayList<Vec3>(2);
        var e1 = Vec3.sub(B, A);
        var e2 = Vec3.sub(C, A);
        var n = Vec3.cross(e1, e2);
        var det = -Vec3.dot(l.direction(), n);
        var idet = 1.0f/det;
        Vec3 ao = Vec3.sub(l.origin(), A);
        Vec3 dao = Vec3.cross(ao, l.direction());
        var u =  Vec3.dot(e2, dao) * idet;
        var v = -Vec3.dot(e1, dao) * idet;
        var t =  Vec3.dot(ao, n  ) * idet;
        if (det >= 0.000001f && t >= 0f && u >= 0f && v >= 0f && (u+v) <= 1.0f && t <= l.length())
            result.add(l.alongLine(t));
        return result;
    }

}