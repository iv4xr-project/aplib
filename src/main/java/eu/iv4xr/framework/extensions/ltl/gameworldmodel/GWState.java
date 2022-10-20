package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.mainConcepts.WorldEntity;

/**
 * Representing the state of the game-world; or more precisely a state in
 * {@link GameWorldModel}. A state consists of game-objects that populate the
 * game-world, the agent current and previous positions.
 * 
 * @author Samira, Wish.
 */
public class GWState implements IExplorableState {
	
	public static final String DESTROYED = "destroyed_" ;
	
	/**
	 * The objects in the game.
	 */
	public Map<String,WorldEntity> objects = new HashMap<>() ;
	
	/**
	 * Current agent location, which is an id of an object, representing that the
	 * physical location of the agent is in the near vicinity of the object. (this
	 * also means that we cannot represent a location that is half-way towards the
	 * object)
	 */
	public String currentAgentLocation ;
	
	public GWState() { }
	
	public void addObjects(WorldEntity ... objects) {
		for (var e : objects) {
			this.objects.put(e.id,e) ;
		}
	}
	
	public void setAsCurrentLocation(WorldEntity e) {
		currentAgentLocation = e.id ;
	}
	
	public void markAsDestroyed(String id) {
		var e = objects.get(id)	;
		if (e==null) 
			throw new IllegalArgumentException("Object with id " + id + " does not exists.") ;
		e.properties.put(DESTROYED,true) ;
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
	
	@Override
	public IExplorableState clone() {
		GWState copy = new GWState() ;
		for (var e : objects.values()) {
			try {
				copy.objects.put(e.id, e.deepclone()) ;
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
		for (var z : this.objects.entrySet()) {
			WorldEntity e1 = objects.get(z.getKey()) ;
			WorldEntity e2 = st2.objects.get(z.getKey()) ;
			boolean quiteTheSame = e1.hasSameState(e2) ;
			if (!quiteTheSame) return false ;
		}
		return this.objects.keySet().equals(st2.objects.keySet()) ;
	}
}