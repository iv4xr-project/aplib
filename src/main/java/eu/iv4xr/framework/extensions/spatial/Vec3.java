package eu.iv4xr.framework.extensions.spatial;

/**
 * This is a simple 3D vector implementation using floats.
 * 
 * @author Naraenda
 */
public class Vec3 {
    public float x, y, z;

    /**
     * Construct a new vector with three floating point values.
     * 
     * @param x: The 'x' component
     * @param y: The 'y' component
     * @param z: The 'z' component
     */
    public Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Construct a vector containg the same constant value for
     * each of the components.
     * 
     * @param c: the constant value.
     */
    public Vec3(float c) {
        this(c, c, c);
    }

    /**
     * Copies a vector by value.
     * 
     * @return New instance of the current vector.
     */
    public Vec3 copy() {
        return new Vec3(this.x, this.y, this.z);
    }

    /**
     * @return Vec3 {0, 0, 0}
     */
    public static Vec3 zero() {
        return new Vec3(0);
    }   

    /**
     * @return Vec3 {1, 1, 1}
     */
    public static Vec3 one() {
        return new Vec3(1);
    }

    /**
     * @return A + B
     */
    public static Vec3 add(Vec3 a, Vec3 b) {
        return new Vec3(
            a.x + b.x, 
            a.y + b.y, 
            a.z + b.z);
    }

    /**
     * @return A - B
     */
    public static Vec3 sub(Vec3 a, Vec3 b) {
        return new Vec3(
            a.x - b.x, 
            a.y - b.y, 
            a.z - b.z);
    }

    /**
     * @return A * B
     */
    public static Vec3 mul(Vec3 a, Vec3 b) {
        return new Vec3(
            a.x * b.x, 
            a.y * b.y, 
            a.z * b.z);
    }

    /**
     * @param a Vector
     * @param c Scalar
     * @return c * A
     */
    public static Vec3 mul(Vec3 a, float c) {
        return new Vec3(
            a.x * c, 
            a.y * c, 
            a.z * c);
    }

    /**
     * @param a Vector
     * @param c Scalar
     * @return (1/c) * A
     */
    public static Vec3 div(Vec3 a, float c) {
        return new Vec3(
            a.x / c, 
            a.y / c, 
            a.z / c);
    }

    
    /**
     * @return A.x * B.x + A.y * B.y + A.z * B.z
     */
    public static float dot(Vec3 a, Vec3 b) {
        return (
            a.x * b.x + 
            a.y * b.y +
            a.z * b.z);
    }

    /**
     * @return The distance between two vectors.
     */
    public static float dist(Vec3 a, Vec3 b) {
        return Vec3.sub(a, b).length();
    }

    /**
     * @return The squared distance of this vector.
     */
    public float lengthSq() {
        return dot(this, this);
    }

    /**
     * @return The length of this vector.
     */
    public float length() {
        return (float) Math.sqrt(this.lengthSq());
    }

    /**
     * @return A normalized copy of this vector.
     */
    public Vec3 normalized() {
        var s = this.length();
        if (s == 0) 
            throw new ArithmeticException();
        return div(this, s);
    }
}