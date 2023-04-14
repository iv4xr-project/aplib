package eu.iv4xr.framework.spatial;

import java.io.Serializable;
import java.util.Objects;

/**
 * Representing discrete 2D vector/position, e.g. (0,0) and (1,2). But (1.5,1) is not
 * a valid instance of this class.
 */
public class IntVec2D implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public int x ;
	public int y ;
	public IntVec2D(int x, int y) {
		this.x = x ; this.y = y ;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof IntVec2D) {
			var o_ = (IntVec2D) o ;
			return this.x == o_.x && this.y == o_.y ;
		}
		return false ;
	}
	
	@Override
    public String toString() {
        return String.format("<%d,%d>", x, y);
    }
	
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
	
	public static float distSq(IntVec2D p, IntVec2D q) {
		float dx = (float) (p.x - q.x) ;
		float dy = (float) (p.y - q.y) ;
		return dx*dx + dy*dy ;
	}
	
	public static float dist(IntVec2D p, IntVec2D q) {
		return (float) Math.sqrt(distSq(p,q)) ;
	}

}
