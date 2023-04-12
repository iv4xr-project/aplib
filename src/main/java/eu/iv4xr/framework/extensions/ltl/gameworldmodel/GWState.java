package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import eu.iv4xr.framework.extensions.ltl.IExplorableState;


/**
 * Representing the state of the game-world; or more precisely a state in
 * {@link GameWorldModel}. A state consists of game-objects that populate the
 * game-world, the agent current and previous positions.
 * 
 * @author Samira, Wish.
 */
public class GWState implements IExplorableState {
		
	/**
	 * The objects in the game.
	 */
	public Map<String,GWObject> objects = new HashMap<>() ;
	
	/**
	 * Current agent location, which is an id of an object, representing that the
	 * physical location of the agent is in the near vicinity of the object. (this
	 * also means that we cannot represent a location that is half-way towards the
	 * object)
	 */
	public String currentAgentLocation ;
	
	public GWState() { }
	
	public void addObjects(GWObject ... objects) {
		for (var e : objects) {
			this.objects.put(e.id,e) ;
		}
	}
	
	public void setAsCurrentLocation(GWObject e) {
		currentAgentLocation = e.id ;
	}
	
	public void markAsDestroyed(String id) {
		var e = objects.get(id)	;
		if (e==null) 
			throw new IllegalArgumentException("Object with id " + id + " does not exists.") ;
		e.destroyed = true ;
	}
		
	@Override
	public String showState() {
		StringBuffer z =  new StringBuffer() ;
		z.append("Cur.location = " + currentAgentLocation) ;
		z.append("\nObjects: ====") ;
		for (var o : objects.values()) {
			z.append("\n") ;
			z.append(o.toString()) ;
		}
		return z.toString() ;
	}
	
	/**
	 * Clone the state of {@link #GameWorldModel}. Do note that values of the properties of the object 
	 * are not deep-cloned. This is fine if those values are primitive values. But if they are e.g. 
	 * arrays you will still need to clone them yourself too.
	 */
	@Override
	public IExplorableState clone() {
		GWState copy = new GWState() ;
		for (var e : objects.values()) {
			try {
				copy.objects.put(e.id, (GWObject) e.clone()) ;
			}
			catch (Exception excp) {
				throw new Error(excp) ;
			}
			copy.currentAgentLocation = this.currentAgentLocation ;
		}
		return copy ;
	}	
	
	@Override
    public int hashCode() {
        return Objects.hash(currentAgentLocation, objects);
    }
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GWState)) return false ;
		GWState st2 = (GWState) o ;
		if (! this.currentAgentLocation.equals(st2.currentAgentLocation))
			return false ;
		if (this.objects.keySet().size() != st2.objects.keySet().size())
			return false ;
		for (var e : this.objects.entrySet()) {
			GWObject x = e.getValue() ;
			String id = e.getKey() ;
			if (! st2.objects.keySet().contains(id))
				return false ;
			GWObject y = st2.objects.get(id) ;
			if (x==null && y==null)
				continue ;
			if (x != null && ! x.equals(y)) {
				return false ;
			}
		}
		return true ;
	}
}