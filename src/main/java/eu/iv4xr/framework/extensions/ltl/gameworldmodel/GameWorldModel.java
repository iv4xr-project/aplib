package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.ITargetModel;
import eu.iv4xr.framework.extensions.ltl.ITransition;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWTransition.GWTransitionType;
import nl.uu.cs.aplib.utils.Pair;

/**
 * An EFSM-like model of a game-world. The model can be used to capture the
 * layout and logic of the non-mobile part of the game-world. The game-world is
 * modeled to consists of a zones. A zone is inhabited by game-objects, each may
 * have its own state/properties. A zone represent an area in the game-world
 * where travel within it is unrestriced/unhindered. Traveling between two zone
 * must go through a game-object called "blocker". We can imagine a blocker to
 * be a door or a corridor that connects two zones. As such, this blocker is a
 * member of the zones it connects. A blocker has blocking-state, which is
 * either blocking of open. Only when it is open, then travel between the zones
 * that it connects is possible.
 * 
 * <p>
 * Objects in the model can be interacted with. Interacting with a game-object
 * can change the state of other game-objects. A table called
 * {@link #objectlinks} keep tracks of which objects would be affected. If (i,O)
 * is in the table, then interacting with i may affect the state of objects in
 * O. The exact effect is specified by the function {@link #alpha}.
 * 
 * <p>
 * This class also implements the interface {@link ITargetModel}, so it can be
 * targeted by a model checker if we need to perform queries or analyses on the
 * model. Part of the deal is that this requires the state of the model to be
 * cloneable. A cloning function is provided, but keep in mind the following
 * limitation. The state of the model consists of the state of the game-objects
 * included in the model. The state of an object includes its properties, which
 * are represented as a mapping of property-names to property-values. E.g. if an
 * object o has properties named "p1" and "p2", their values could be v1 and v2 respectively.
 * When cloning o, the mapping is clones, but the values v1 and v2 are NOT deep-cloned.
 * This is fine if they are primitive values. If for example v1 is an array, then
 * you have to take caution. In this case we should not update the property p1 of o
 * by directly side-effecting the array v1. Instead, make a clone of the array itself,
 * and apply the update to the clone. Then make p1 points to the clone.
 * 
 * 
 * @author Samira, Wish.
 *
 */
public class GameWorldModel implements ITargetModel {
	
	public GWState defaultInitialState ;
	
	public GWState initialState ;
	
	//public int count = 0 ;
	
	/**
	 * The history of the the states, stored in reverse order (the head of the list
	 * is the current state, the last state in the list is the oldest).
	 * Each k-th entry in the history is a pair (st,tr) where st is a state, and tr
	 * is the transition that was taken to get to that state from its previous state
	 * (so, that is the k+1 st state). The exception is the last state in the history,
	 * which is the model initial state, and has no tr.
	 */
	public List<Pair<GWState,GWTransition>> history = new LinkedList<>() ;
	
	public Set<GWZone> zones = new HashSet<>() ;
	public Map<String,Set<String>> objectlinks = new HashMap<>() ;	
	public Set<String> blockers = new HashSet<>();
	
	GameWorldModel() { } 
	
	/**
	 * Create an instance of GameWorldModel with the specified state as the 
	 * initial state.
	 */
	public GameWorldModel(GWState initialState) {
		this.setInitialState(initialState);
		this.defaultInitialState = (GWState) initialState.clone() ;
	}
	
	public void setInitialState(GWState initialState) {
		this.initialState = (GWState) initialState.clone() ;
		this.reset();
	}
	
	public void copyDefaultInitialStateToInitialState() {
		setInitialState(defaultInitialState) ;
	}
	
	/**
	 * Add the given zones to this GameWorldModel. It returns this model.
	 */
	public GameWorldModel addZones(GWZone ... zones) {
		for (var zn : zones) this.zones.add(zn) ;
		return this ;
	}
	
	/**
	 * Mark the specified objects (identified by their ids) as blockers.
	 * The method returns this model.
	 */
	public GameWorldModel markAsBlockers(String ... objectIds) {
		GWState state = getCurrentState() ;
		for (var id : objectIds) {
			if (! state.objects.keySet().contains(id)) {
				throw new IllegalArgumentException("Object with id " + id + " does not exists.") ;
			}
			this.blockers.add(id) ;
		}
		return this ;
	}
	
	public GWZone getZone(String zoneId) {
		for (var zn : zones) {
			if (zn.id.equals(zoneId)) return zn ;
		}
		return null ;
	}
	
	/**
	 * When we register (i,o1,o2,..) we are saying that interacting with
	 * the object i will affect o1, o2, ... These connections will be added
	 * to [{@link #objectlinks}.
	 * 
	 * <p>The method returns this model.
	 */
	public GameWorldModel registerObjectLinks(String switcherId, String ... newAffectedIds) {
		Set<String> affected = objectlinks.get(switcherId) ;
		if (affected == null) {
			affected = new HashSet<>() ;
			objectlinks.put(switcherId, affected) ;
		}
		for (var y : newAffectedIds) {
			affected.add(y) ;
		}
		return this ;
	}
	
	public boolean isBlocker(String id) {
		return blockers.contains(id) ;
	}
	
	public static String IS_OPEN_NAME = "isOpen" ;
	
	public boolean isBlocking(String id) {
		var blocker = getCurrentState().objects.get(id) ;
		boolean open = (Boolean) blocker.properties.get(IS_OPEN_NAME) ;
		return ! open ;
	}
	
	/**
	 * Get the zone where a object is located. Note that a non-blocker will be in 
	 * exactly one zone. A blocker is in either one or two zones.
	 */
	public Set<String> zonesOf(String objectId) {	
		return zones.stream()
				.filter(zn -> zn.members.contains(objectId))
				.map(zn -> zn.id)
				.collect(Collectors.toSet()) ;
	}
	
	/**
	 * Check whether two objects are in the same zone. This is the case if there is one
	 * zone Z that contains both objects.
	 */
	public boolean inTheSameZone(String obj1, String obj2) {
		var zones1 = zonesOf(obj1) ;
		var zones2 = zonesOf(obj2) ;
		return !zones1.isEmpty() && zones1.stream().anyMatch(zn -> zones2.contains(zn)) ;
	}
		
	public boolean canTravelTo(String destinationId) {
		GWState state = getCurrentState() ;
		if (destinationId.equals(state.currentAgentLocation)) {
			// forbid travel to the current-position ... pointless
			return false ;
		}
		GWObject t = state.objects.get(destinationId) ;
		if (t.destroyed) {
			// the destination has been destroyed; travel is not possible:
			return false ;
		}
		String previousLocation = history.size()>1 ? history.get(1).fst.currentAgentLocation : null ;
		GWTransition previousTransition = history.get(0).snd ;
		GWTransitionType previousTransitionType = previousTransition != null ? previousTransition.type : null ;
		
		// special case:
		if (previousLocation != null 
				&& previousTransitionType == GWTransitionType.TRAVEL
				&& inTheSameZone(previousLocation,destinationId)) {
				// Two travels one after another, within the same zone will be disallowed,
				// for efficiency reason. Such a transition if pointless.
				return false ;
		}
		
		if (! inTheSameZone(state.currentAgentLocation,destinationId)) {
			// CASE (1): the destination is not in the same zone as the current location;
			// direct travel is not possible:
			return false ;
		}
		// ELSE: CASE (2a), current and destination are in the same zone.
		// travel is possible if current-location is NOT a blocker, or it is an open blocker:
		if (! isBlocker(state.currentAgentLocation) || ! isBlocking(state.currentAgentLocation)) {
			return true ;
		}
		// else, CASE (2b): current and destination are in the same zone,
		// current is a blocker and it is closed ...
		
		if (previousLocation != null
			&& ! inTheSameZone(previousLocation,destinationId)) {
			// travel to cross two zones; this is not possible since the current location 
			// is a closed blocker:
			return false ;
		}
		if (previousLocation != null
				&& previousLocation.equals(state.currentAgentLocation)) {
			// the previous location is the same as current, and the current location is
			// still a closed blocker,
			// forbid this:
			return false ;
		}
		
		// System.out.println(">>>>> ") ;
		// else travel is possible:
		return true ;
	}
	
	public void travelTo(String destinationId) {
		if (canTravelTo(destinationId)) {
			// travel is possible:
			GWState newState =(GWState) getCurrentState().clone() ;
			newState.currentAgentLocation = destinationId ;
			GWTransition tr = new GWTransition(GWTransitionType.TRAVEL, destinationId) ;
			history.add(0,new Pair<GWState,GWTransition>(newState,tr)) ;
			return ;
		}
		throw new IllegalArgumentException("Travel to " + destinationId + " is not possible (unreachable).") ;
	}
	
	/**
	 * The semantic function of interaction. Suppose S is the current state. Consider an
	 * interaction on an object i, and suppose this interaction is possible (defined by {@link #canInteract(String)n}).
	 * Suppose that i is linked to object o and q (defined by {@link #objectlinks}). The semantic
	 * of the interaction is implemented through alpha(i,{o,q},S). This semantic may update the
	 * state S to S', which would then yield the next state of the model.
	 */
	public BiFunction<String,Set<String>,Function<GWState,Void>> alpha ;
	
	public boolean StressingMode = false ;
	
	public boolean canInteract(String targetId) {
		GWState state = getCurrentState() ;
		if (! targetId.equals(state.currentAgentLocation)) {
			// we can only interact with the object at the agent's current location:
			return false ;
		}
		//System.out.println(">>>>") ;
		GWObject target = state.objects.get(targetId) ;
		if (target.destroyed) 
			return false ;
		GWTransition previousTransition = history.get(0).snd ;
		if (previousTransition == null || previousTransition.type == GWTransitionType.TRAVEL)
			return true ;
		// else the prev-transition is an INTERACT.
		// Normally, we won't allow interacting twice one after another; pointless, 
		// unless we are in the stressing-mode:
		if (!StressingMode && previousTransition.target.equals(targetId)) {
			return false ;
		}
		return true ;
	}
	
	public void interact(String targetId) {
		if (canInteract(targetId)) {
			GWState newState = (GWState) getCurrentState().clone() ;
			GWObject target = newState.objects.get(targetId) ;
			alpha.apply(targetId, objectlinks.get(targetId)).apply(newState) ;
			/*
			if(objectlinks.get(targetId) != null) {
				Set<GWObject> affected = objectlinks.get(targetId)
						.stream()
						.map(id -> newState.objects.get(id))
						.filter(e -> ! e.destroyed)
						.collect(Collectors.toSet()) ;
				alpha.apply(target,affected) ;
			}
			*/
			GWTransition tr = new GWTransition(GWTransitionType.INTERACT, targetId) ;
			history.add(0,new Pair<GWState,GWTransition>(newState,tr)) ;
			return ;
		}
		throw new IllegalArgumentException("Interact with " +  targetId + " is not allowed.") ;
	}

	/**
	 * Reset this model to its initial-state.
	 */
	@Override
	public void reset() {
		history.clear();
		Pair<GWState,GWTransition> st0 = new Pair<>((GWState) initialState.clone(), null) ;
		history.add(st0) ;
	}
	
	/**
	 * Reset this model; use the given state as the new initial-state.
	 */
	public void reset(GWState newInitialState) {
		this.initialState = newInitialState ;
		reset() ;
	}

	@Override
	public GWState getCurrentState() {
		return history.get(0).fst ;
	}

	@Override
	public boolean backTrackToPreviousState() {
		//System.out.println(">>> backtrack") ;
		if (history.size() == 1) 
			return false ;
		history.remove(0) ;
		return true ;
	}

	@Override
	public List<ITransition> availableTransitions() {
		
		GWState state = (GWState) getCurrentState() ;
		List<ITransition> choices = new LinkedList<>() ;

		Set<GWZone> zns = zonesOf(state.currentAgentLocation)
				.stream()
				.map(id -> getZone(id))
				.collect(Collectors.toSet()) ;
		
		for (var zn : zns) {
			for (String o : zn.members) {
				if (canTravelTo(o)) {
					GWTransition tr = new GWTransition(GWTransitionType.TRAVEL,o) ;
					choices.add(tr) ;
				}
			}
		}
		
		if (canInteract(state.currentAgentLocation)) {
			GWTransition tr = new GWTransition(GWTransitionType.INTERACT,state.currentAgentLocation) ;
			choices.add(tr) ;
		}
		
		return choices ;
	}

	@Override
	public void execute(ITransition tr) {
		//System.out.println(">>> " + tr.getId()) ;
		//count++ ;
		GWTransition tr_ = (GWTransition) tr ;
		switch(tr_.type) {
			case TRAVEL   : travelTo(tr_.target) ; break ;
			case INTERACT : interact(tr_.target) ; break ;
		}
		
	}
	
	@Override
	public String toString() {
		StringBuffer z = new StringBuffer() ;
		z.append("**** State:\n") ;
		z.append(history.get(0).fst.showState()) ;
		z.append("\n**** blockers: " + this.blockers) ;
		z.append("\n**** Zones:") ;
		for (var zn : zones) {
			z.append("\n   " + zn.id + ":" + zn.members) ;
		}
		z.append("\n**** Object-links:") ;
		for (var ez : objectlinks.entrySet()) {
			z.append("\n   " + ez.getKey() + " --> " + ez.getValue()) ;
		}
		return z.toString() ;		
	}
	
	static class GameWorldModelBase { 
		public String initialAgentLocation ;
		public List<GWObject> objects ;
		public Set<String> blockers ;
		public Set<GWZone> zones ;
		public Map<String,Set<String>> objectlinks ;
	}
	
	/**
	 * Save this model as a JSON-object to a file. The alpha-component is not
	 * saved though, as it is a function.
	 */
	public void save(String filename) throws JsonIOException, IOException {
		GameWorldModelBase base = new GameWorldModelBase() ;
		base.initialAgentLocation = this.defaultInitialState.currentAgentLocation ;
		base.objects = this.defaultInitialState.objects.values().stream().collect(Collectors.toList()) ;
		base.blockers = this.blockers ;
		base.zones = this.zones ;
		base.objectlinks = this.objectlinks ;
		
		FileWriter fwriter = new FileWriter(filename) ;
		
		Gson gson = new GsonBuilder()
			    . setPrettyPrinting()
			    . serializeNulls()
			    . create(); 
		//System.out.println(">>>> " + gson.toJson(base)) ;
		gson.toJson(base, fwriter);
		
		fwriter.flush();
		fwriter.close();
		
	}
	
	/**
	 * Read saved model (in JSON) from a file, and return the corresponding
	 * GameWorldModel object. The alpha-component is left null, as it
	 * cannot be saved, being a function.
	 */
	public static GameWorldModel loadGameWorldModelFromFile(String filename) throws IOException {
		Gson gson = new Gson();
		Reader reader = Files.newBufferedReader(Paths.get(filename));
		GameWorldModelBase base = gson.fromJson(reader,GameWorldModelBase.class);
		GameWorldModel model = new GameWorldModel() ;
		model.defaultInitialState = new GWState() ;
		model.defaultInitialState.currentAgentLocation = base.initialAgentLocation ;
		for (var o : base.objects) {
			model.defaultInitialState.objects.put(o.id, o) ;
		}
		model.zones = base.zones ;
		model.blockers = base.blockers ;
		model.objectlinks = base.objectlinks ;
		model.copyDefaultInitialStateToInitialState() ;
		return model ;
	}
	

}
