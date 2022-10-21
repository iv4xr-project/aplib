package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.util.*;
import eu.iv4xr.framework.spatial.Vec3;

public class GWObject {
	
	public String id ;
	public String type = "" ;
	public Vec3 position ;
	public Vec3 extent ;
	public Vec3 velocity ;
	public boolean destroyed = false ;
	public Map<String,Object> properties = new HashMap<>() ;
	
	public GWObject(String id, String type) {
		this.id = id ;
		this.type = type ;
	}
	
	public GWObject(String id, String type, Vec3 position, Vec3 extent, Vec3 velocity) {
		this(id,type) ;
		this.position = position ;
		this.extent = extent ;
		this.velocity = velocity ;
	}
	
	@Override
	public String toString() {
		StringBuffer z = new StringBuffer() ;
		z.append("GWObject, type:" + type + ", id:" + id) ;
		if (destroyed) 
			z.append(" (destroyed)") ;
		z.append("\n   pos:" + this.position) ;
		z.append(", extent:" + this.extent) ;
		z.append(", velocity:" + this.velocity) ;
		z.append("\n   properties:") ;
		for (var e : properties.entrySet()) {
			z.append("\n     " + e.getKey() + ":" + e.getValue()) ;
		}
		return z.toString() ;
	}
	
	static boolean eqVec(Vec3 p, Vec3 q) {
		if (p==null) return (q==null) ;
		if (q==null) return false ;
		return p.equals(q) ;
	}
	
	@Override
	public boolean equals(Object o) {
		if (! (o instanceof GWObject)) return false ;
		var o_ = (GWObject) o ;
		if (! this.id.equals(o_.id)
			|| ! this.type.equals(o_.type)
			|| this.destroyed != o_.destroyed
			|| ! eqVec(this.position, o_.position)
			|| ! eqVec(this.extent, o_.extent)
			|| ! eqVec(this.velocity, o_.velocity)) {
			return false ;
		}
		for (var e : this.properties.entrySet()) {
			var x = e.getValue() ;
			var y = o_.properties.get(e.getKey()) ;
			if (x==null && y==null) continue ;
			if (x!=null) {
				if (! x.equals(y)) return false ;
			}
		}
		return this.properties.keySet().equals(o_.properties.keySet()) ;
	}
	
	@Override
    public int hashCode() {
        return Objects.hash(id, type, position, extent, velocity, destroyed, properties);
    }
	
	@Override
	public Object clone() {
		var o = new GWObject(this.id, this.type, this.position, this.extent, this.velocity) ;
		o.destroyed = this.destroyed ;
		for (var e : this.properties.entrySet()) {
			o.properties.put(e.getKey(),e.getValue()) ;
		}
		return o ;
	}
 
}
