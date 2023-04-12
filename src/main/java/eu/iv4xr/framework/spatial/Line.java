package eu.iv4xr.framework.spatial;

/**
 * A partial line that lies between point a and b.
 * 
 * @author Naraenda
 */
public class Line {
    /**
     * Point A, also used as origin point.
     */
    public Vec3 a;

    /**
     * Point B
     */
    public Vec3 b;

    /**
     * Constructs a line between two point.
     * 
     * @param a Point A
     * @param b Point B
     */
    public Line(Vec3 a, Vec3 b) {
        this.a = a;
        this.b = b;
    }

    /**
     * @return Normalized vector from origin A to B.
     */
    public Vec3 direction() {
        return Vec3.sub(b, a).normalized();
    }

    /**
     * @return The origin A.
     */
    public Vec3 origin() {
        return a;
    }

    /**
     * @param d Distance from the origin.
     * @return Origin() + d * Direcition()
     */
    public Vec3 alongLine(float d) {
        return Vec3.add(origin(), Vec3.mul(direction(), d));
    }

    /**
     * @return Distance between A and B.
     */
    public float length() {
        return Vec3.dist(a, b);
    }

}