package eu.iv4xr.framework.spatial;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Representing a 3D rectangular shape. For now, we don't support rotation.
 */
public class Rectangular3D implements LineIntersectable {
	
	public Vec3 center ;
	
	/** 
	 * The x,y, and z width. Should be >0. So, from the center, the rectangle extend 1/2 its width
	 * to each of the corresponding direction.
	 */
	public Vec3 width ;

	@Override
	public Collection<Vec3> intersect(Line l) {
		float minX = center.x - width.x/2 ;
		float minY = center.y - width.y/2 ;
		float minZ = center.z - width.z/2 ;
		float maxX = center.x + width.x/2 ;
		float maxY = center.y + width.x/2 ;
		float maxZ = center.z + width.x/2 ;
		
		Collection<Vec3> intersections = new LinkedList<>() ;
		
		// calculate intersections between the line and the six surfaces of the rectangle-3D:
		
		// intersection with the infinite plane z = minZ :
		Vec3 i = intersectPlaneXY(l.a, l.b, minZ)  ;
		if (i != null) {
			if (minX <= i.x && i.x <= maxX && minY <= i.y && i.y <= maxY) {
				// the intersection point i lies in the finite plane between xmin,xmax and ymin,ymax
				intersections.add(i) ;
			}
		}
		// intersection with the infinite plane z = maxZ :
		i = intersectPlaneXY(l.a, l.b, maxZ)  ;
		if (i != null) {
			if (minX <= i.x && i.x <= maxX && minY <= i.y && i.y <= maxY) {
				// the intersection point i lies in the finite plane between xmin,xmax and ymin,ymax
				intersections.add(i) ;
			}
		}
		if (intersections.size() == 2) return intersections ;
		
		// intersection with the infinite plane y = minZ :
		i = intersectPlaneXZ(l.a, l.b, minY)  ;
		if (i != null) {
			if (minX <= i.x && i.x <= maxX && minZ <= i.z && i.z <= maxZ) {
			intersections.add(i) ;
		    }
		}
		if (intersections.size() == 2) return intersections ;
		
		i = intersectPlaneXZ(l.a, l.b, maxY)  ;
		if (i != null) {
			if (minX <= i.x && i.x <= maxX && minZ <= i.z && i.z <= maxZ) {
			intersections.add(i) ;
		    }
		}
		if (intersections.size() == 2) return intersections ;
		
		// intersection with the infinite plane x = minX :
		i = intersectPlaneYZ(l.a, l.b, minX)  ;
		if (i != null) {
			if (minY <= i.y && i.y <= maxY && minZ <= i.z && i.z <= maxZ) {
				intersections.add(i) ;
		    }
		}
		if (intersections.size() == 2) return intersections ;
		
		i = intersectPlaneYZ(l.a, l.b, maxX)  ;
		if (i != null) {
			if (minY <= i.y && i.y <= maxY && minZ <= i.z && i.z <= maxZ) {
				intersections.add(i) ;
		    }
		}
		return intersections ;
	} 
	
	
	/**
	 * Calculate the intersection of the line between p and q, and a plane XY
	 * where z = c.
	 */
	Vec3 intersectPlaneXY(Vec3 p, Vec3 q, float c) {
		Vec3 direction = Vec3.sub(q,p) ;
		float t = (c - p.z) / direction.z ;
		if (t<0 || t>1) {
			// the intersection lies outside the line (not between p and q)
			return null ;
		}
		return Vec3.add(p, Vec3.mul(direction, t)) ;
	}
	
	/**
	 * Calculate the intersection of the line between p and q, and a plane XZ
	 * where y = c.
	 */
	Vec3 intersectPlaneXZ(Vec3 p, Vec3 q, float c) {
		Vec3 direction = Vec3.sub(q,p) ;
		float t = (c - p.y) / direction.y ;
		if (t<0 || t>1) {
			// the intersection lies outside the line (not between p and q)
			return null ;
		}
		return Vec3.add(p, Vec3.mul(direction, t)) ;
	}
	
	/**
	 * Calculate the intersection of the line between p and q, and a plane YZ
	 * where x = c.
	 */
	Vec3 intersectPlaneYZ(Vec3 p, Vec3 q, float c) {
		Vec3 direction = Vec3.sub(q,p) ;
		float t = (c - p.x) / direction.x ;
		if (t<0 || t>1) {
			// the intersection lies outside the line (not between p and q)
			return null ;
		}
		return Vec3.add(p, Vec3.mul(direction, t)) ;
	}

}
