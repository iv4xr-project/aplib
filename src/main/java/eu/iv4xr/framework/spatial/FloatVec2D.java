package eu.iv4xr.framework.spatial;

import java.io.Serializable;
import java.util.Objects;

public class FloatVec2D implements Serializable {

    public float x ;
    public float y ;

    // Constructor
    public FloatVec2D(float x, float y) {
        this.x = x ;
        this.y = y ;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FloatVec2D) {
            var o_ = (FloatVec2D) o ;
            return this.x == o_.x && this.y == o_.y ;
        }
        return false ;
    }

    @Override
    public String toString() {
        return String.format("<%f,%f>", x, y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public static float distSq(FloatVec2D p, FloatVec2D q) {
        float dx = (float) (p.x - q.x) ;
        float dy = (float) (p.y - q.y) ;
        return dx*dx + dy*dy ;
    }

    public static float dist(FloatVec2D p, FloatVec2D q) {
        return (float) Math.sqrt(distSq(p,q)) ;
    }
}
