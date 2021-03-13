package eu.iv4xr.framework.spatial;

import java.util.Collection;

/**
 * Describes objects than can intersect with a line segment.
 * 
 * @author Naraenda
 */
public interface LineIntersectable {
    /**
     * Returns a list of intersection between a line segment and an object.
     * 
     * @param l line segment
     * @return list of intersections.
     */
    Collection<Vec3> intersect(Line l);

    /**
     * Checks if a line segment intersects an object.
     * 
     * @param l line segment
     * @return
     */
    default Boolean intersects(Line l) {
        return intersect(l).size() > 0;
    }
}