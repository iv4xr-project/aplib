package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
		if (properties.size()==0) {
			if (position == null) {
				return Objects.hash(id,destroyed);
			}
			return Objects.hash(id, position, extent, velocity, destroyed);
		}
		if (properties.size()==1) {
			Object v = null ;
			for (var w : properties.values()) {
				v = w ;
				break ;
			}
			if (position == null) {
				return Objects.hash(id,destroyed,v);
			}
			return Objects.hash(id, position, extent, velocity, destroyed,v);
		}
		if (position == null) {
			return Objects.hash(id, destroyed, properties);
		}
        return Objects.hash(id, position, extent, velocity, destroyed, properties);
    }
	
	private Object mkCopy(Serializable o) {
		try {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(o);
        //De-serialization of object
        ByteArrayInputStream bis = new   ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);
        return in.readObject();
		}
		catch(Exception e) { 
			return null ;
		}
		
	}
	
	private Object mkCopy2(Object v) {
		if (v instanceof Boolean) {
			return (boolean) v ;
		}
		if (v instanceof Integer) {
			return (int) v ;
		}
		if (v instanceof String) {
			return "" + (String) v ;
		}
		if (v instanceof Long) {
			return (long) v ;
		}
		if (v instanceof Short) {
			return (short) v ;
		}
		if (v instanceof Float) {
			return (float) v ;
		}
		if (v instanceof Double) {
			return (double) v ;
		}
		throw new IllegalArgumentException("Don't know how to clone") ;
	}
	
	/**
	 * Clone this object. Do note that values of the properties of the object (in {@link #properties})
	 * are not deep-cloned. This is fine if those values are primitive values. But if they are e.g. 
	 * arrays you will still need to clone them yourself too.
	 */
	@Override
	public Object clone() {
		var o = new GWObject(this.id, this.type, this.position, this.extent, this.velocity) ;
		o.destroyed = this.destroyed ;
		for (var e : this.properties.entrySet()) {
			// cloning via serialization is too expensive!
			//Object val2 = mkCopy((Serializable) e.getValue()) ;
			// using a faster copy: 
			// Object val2 = mkCopy2(e.getValue()) ;
			
			// RISKY optimization, not cloning the value at all! Will save
			// some CPU time, but make sure that the alpha-function does cloning
			// when it performs updates! 
			var val2 = e.getValue() ;
			o.properties.put(e.getKey(),val2) ;
		}
		return o ;
	}
 
}
