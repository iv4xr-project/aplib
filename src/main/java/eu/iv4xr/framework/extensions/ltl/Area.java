package eu.iv4xr.framework.extensions.ltl;

import java.util.*;

import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;

/**
 * An "area" represents some 3D-space. It can be a "surface", but generally it
 * is a space. More precisely, it is defined as a set of disjoint voxels.
 * Typically, these voxels are of the same size, and their positions are all
 * defined with respect to the same world-origin (by default (0,0,0)).
 * 
 * <p>
 * Two main methods are provided: {@link #contains(Vec3)} checks if a location p
 * is within the area, and {@link #covered(List)} returns the list of voxels in the area
 * that are "visited" by the list of locations ps.
 * 
 * <p>
 * The primitive-form of areas is a Rectangle. Two areas can combined using
 * union, intersect, and substract operators. The requirement is that the
 * composed areas are made of voxels of the same size and have the same
 * world-origin.
 * 
 * <p>
 * NOTE: the implementation of Areas physically creates the set of voxels; so
 * they are NOT suitable for representing huge areas.
 * 
 * @author Wish
 *
 */
public abstract class Area {
	
	
	/**
	 * The location of the world's origin, which will be used to calculate the
	 * voxels. The default is the (0,0,0) location.
	 */
	public Vec3 origin = new Vec3(0,0,0) ;
	
	/**
	 * The assumed voxel-size. The default is 1.
	 */
	public float voxelSize = 1 ;
	
	/**
	 * The set of voxels of this area. Each represented by a pair (a,b) where a is
	 * the bottom coordinate of the voxel, and b is its top coordinate. "Bottom"
	 * means the corner of the voxel with the lowest (x,y,z), and top is the
	 * opposite.
	 */
	public HashSet<Pair<Vec3,Vec3>> voxels = new HashSet<>() ;
	
	/**
	 * Check if the given location is inside this area.
	 */
	abstract public boolean contains(Vec3 location) ;
	
	static boolean inVoxel(Vec3 p, Vec3 vxBottom, Vec3 vxTop) {
		return vxBottom.x <= p.x && p.x < vxTop.x 
			&& vxBottom.y <= p.y && p.y < vxTop.y
			&& vxBottom.z <= p.z && p.z < vxTop.z ;
	}
	
	/**
	 * Return the set of voxels in this area that are visited by the given list of
	 * locations. Each voxel is represented by a pair (a,b) where a is the bottom
	 * coordinate of the voxel, and b is its top coordinate. "Bottom" means the
	 * corner of the voxel with the lowest (x,y,z), and top is the opposite.
	 */
	public Set<Pair<Vec3,Vec3>> covered(List<Vec3> visits) {
		Set<Pair<Vec3,Vec3>> covered = new HashSet<>() ;
		for (Vec3 p : visits) {
			if (! this.contains(p))
				continue ;
			for (var vx : voxels) {
				if (inVoxel(p, vx.fst, vx.snd)) {
					covered.add(vx) ;
				}
			}
		}
		return covered ;
	}
	
	/**
	 * Return the proportion of the number of voxels visited by the given list of
	 * locations, with repect to the total number of voxels in this area.
	 * See also {@link #covered(List)}.
	 */
	public float coveredPortion(List<Vec3> visits) {
		float covered = (float) covered(visits).size() ;
		return covered / (float) voxels.size() ;
	}
	
	
	/**
	 * An instance of {@link Area} that represent a rectangle-shaped area.
	 * The height of this area is just one voxel-size. So, a Rectangle-area
	 * of size w*h will have exactly w*h*1 voxels, assuming voxel-size of 1.
	 * Voxel-size is stored in the field {@link Area#voxelSize}.
	 * 
	 * @author Wish
	 */
	public static class RectangleArea extends Area {
		
		/**
		 * The corner of the rectangle with the lowest (x,-,z).
		 */
		public Vec3 bottom ;
		
		/**
		 * The width of this rectangle (along z-axis).
		 */
		public float width ;
		
		/**
		 * The length of this rectangle (along x-axis).
		 */
		public float length ;
		
		/**
		 * The corner of the rectangle with the highest (x,-,z). It y-position
		 * should be the same as bottom.
		 */
		public Vec3 top ;
		
		/**
		 * A y coordinate (altitude) such that 
		 *   (1) y = origin.y + k*voxelSize, for some integer k.
		 *   (2) y <= bottom.y
		 *   (3) y + voxelSize > bottom.y
		 * 
		 * In other words, it is the closest altitude to bottom.y, which is a multiple of
		 * voxelsize, counting with respect to the world-origin.
		 */
		public float flooredAltitude ;
		
		public RectangleArea(Vec3 bottom, float length, float width) {
			this.bottom = bottom.copy() ;
			this.width = width ; this.length = length ;
			top = new Vec3(bottom.x + length, bottom.y, bottom.z + width) ;
			flooredAltitude = getClosetsMultiple(origin.y, bottom.y, voxelSize) ;
			calculateVoxels() ;
		}
		
		public RectangleArea(Vec3 bottom, Vec3 top) {
			this.bottom = bottom.copy() ; 
			this.top = top.copy() ;
			top.y = bottom.y ;
			width = top.z - bottom.z ;
			length = top.x - bottom.x ;
			if (width <= 0 || length <= 0) 
				throw new IllegalArgumentException() ;
			flooredAltitude = getClosetsMultiple(origin.y, bottom.y, voxelSize) ;
			calculateVoxels() ;
		}
		
		static private float getClosetsMultiple(float y0, float y, float m) {
			if (y == y0) {
				return y ;
			}
			float k = (y - y0) / m ;
			return floor(k)*m + y0 ;
		}
		
		private Vec3 getClosestVoxelBottom(Vec3 p) {
			return new Vec3(
					getClosetsMultiple(origin.x, p.x, voxelSize),
					getClosetsMultiple(origin.y, p.y, voxelSize),
					getClosetsMultiple(origin.z, p.z, voxelSize)) ;
		}
 		
		static float floor(float x) {
			return (float)  Math.floor((double) x);
		}
		
		static int ifloor(float x) {
			return (int)  Math.floor((double) x);
		}
		
		private void calculateVoxels() {
			voxels.clear(); 
			Vec3 bottom_ = getClosestVoxelBottom(bottom) ;
			for (float z = bottom_.z ; z < top.z ; z += voxelSize) {
				for (float x = bottom_.x ; x < top.x ; x += voxelSize) {
					Vec3 vxBot = new Vec3(x,flooredAltitude,z) ;
					Vec3 vxTop = new Vec3(x + voxelSize, flooredAltitude + voxelSize, z + voxelSize) ;
					voxels.add(new Pair<Vec3,Vec3>(vxBot,vxTop)) ;
				}
			}
		}
		
		@Override
		public boolean contains(Vec3 location) {
			return bottom.x <= location.x && location.x < top.x 
					&& bottom.z <= location.z && location.z < top.z
					&& flooredAltitude <= location.y && location.y < flooredAltitude + voxelSize ;
		}
	}
	
	/**
	 * Different operators to combine areas.
	 */
	public enum AreaCombinator { UNION, INTERSECTION, SUBTRACTION }
	
	/**
	 * Representing an area that is composed from two other areas
	 * using one of the combinators (see {@link AreaCombinator}.
	 */
	public static class CompositeArea extends Area {
		
		public AreaCombinator combinator ;
		public Area A1 ;
		public Area A2 ;
		
		public CompositeArea(AreaCombinator op, Area A1, Area A2) {
			this.combinator = op ;
			this.A1 = A2 ;
			this.A2 = A2 ;
			for (var vx : A1.voxels) {
				this.voxels.add(vx) ;
			}
			switch(op) {
			   case UNION:
				   for (var vx : A2.voxels) {
					   this.voxels.add(vx) ;
				   }
				   break ;
			   case INTERSECTION:
				   this.voxels.removeIf(vx -> ! A2.voxels.contains(vx)) ;
				   break ;
			   case SUBTRACTION:
				   this.voxels.removeIf(vx -> A2.voxels.contains(vx)) ;
			}
		}

		@Override
		public boolean contains(Vec3 location) {
			switch(combinator) {
			   case UNION:
				   return A1.contains(location) || A2.contains(location) ;
			   case INTERSECTION:
			       return A1.contains(location) && A2.contains(location)  ;
			   case SUBTRACTION:
				   return A1.contains(location) && ! A2.contains(location)  ;
			}
			return false ;
		}
		
	}
	
	// DSL:
	
	/**
	 * Construct a rectangle-area, with the given bottom and top corners.
	 */
	public static RectangleArea rect(Vec3 bottom, Vec3 top) {
		return new RectangleArea(bottom,top) ;
	}
	
	/**
	 * Construct an area which is the union of this area and A.
	 */
	public Area union(Area A) {
		return new CompositeArea(AreaCombinator.UNION,this,A) ;
	}
	
	/**
	 * Construct an area which is the intersection of this area and A.
	 */
	public Area intersect(Area A) {
		return new CompositeArea(AreaCombinator.INTERSECTION,this,A) ;
	}
	
	/**
	 * Construct an area which is obtained by subtracting A from this area.
	 */
	public Area minus(Area A) {
		return new CompositeArea(AreaCombinator.SUBTRACTION,this,A) ;
	}

}
