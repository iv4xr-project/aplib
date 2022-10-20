package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.ITargetModel;
import eu.iv4xr.framework.extensions.ltl.ITransition;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWTransition.GWTransitionType;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.utils.Pair;

/**
 * An EFSM-like model of a game-world.
 * 
 * @author Samira, Wish.
 *
 */
public class GameWorldModel implements ITargetModel {
	
	public GWState initialState ;
	
	public int count = 0 ;
	
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
	
	/**
	 * Create an instance of GameWorldModel with the specified state as the 
	 * initial state.
	 */
	public GameWorldModel(GWState initialState) {
		this.initialState = (GWState) initialState.clone() ;
		this.reset();
	}
	
	
	/**
	 * Add the given zones to this GameWorldModel.
	 */
	public void addZones(GWZone ... zones) {
		for (var zn : zones) this.zones.add(zn) ;
	}
	
	/**
	 * Mark the specified objects (identified by their ids) as blockers.
	 */
	public void markAsBlockers(String ... objectIds) {
		GWState state = getCurrentState() ;
		for (var id : objectIds) {
			if (! state.objects.keySet().contains(id)) {
				throw new IllegalArgumentException("Object with id " + id + " does not exists.") ;
			}
			this.blockers.add(id) ;
		}
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
	 */
	public void registerObjectLinks(String switcherId, String ... newAffectedIds) {
		Set<String> affected = objectlinks.get(switcherId) ;
		if (affected == null) {
			affected = new HashSet<>() ;
			objectlinks.put(switcherId, affected) ;
		}
		for (var y : newAffectedIds) {
			affected.add(y) ;
		}
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
	 * Check whether two objects are in the same zone.
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
		WorldEntity t = state.objects.get(destinationId) ;
		if (t.properties.get(GWState.DESTROYED) != null) {
			// the destination has been destroyed; travel is not possible:
			return false ;
		}
		String previousLocation = history.size()>1 ? history.get(1).fst.currentAgentLocation : null ;
		GWTransition previousTransition = history.get(0).snd ;
		GWTransitionType previousTransitionType = previousTransition != null ? previousTransition.type : null ;
		
		if (previousLocation != null && ! inTheSameZone(state.currentAgentLocation,destinationId)) {
			// the destination is not in the same zone as the current location;
			// direct travel is not possible:
			return false ;
		}
		if (previousLocation != null
			&& ! inTheSameZone(previousLocation,destinationId)) {
			// travel to cross two zones; only possible if the current
			// location is a blocker and it is non-blocking:
			return isBlocker(state.currentAgentLocation) && ! isBlocking(state.currentAgentLocation) ;
		}
		if (previousLocation != null 
			&& previousTransitionType == GWTransitionType.TRAVEL
			&& inTheSameZone(previousLocation,destinationId)) {
			// Two travels one after another, within the same zone will be disallowed,
			// for efficiency reason. Such a transition if pointless.
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
	
	public Function<WorldEntity,Function<Set<WorldEntity>,Void>> alpha ;
	
	public boolean StressingMode = false ;
	
	public boolean canInteract(String targetId) {
		GWState state = (GWState) getCurrentState() ;
		if (! targetId.equals(state.currentAgentLocation)) {
			// we can only interact with the object at the agent's current location:
			return false ;
		}
		//System.out.println(">>>>") ;
		WorldEntity target = state.objects.get(targetId) ;
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
			WorldEntity target = newState.objects.get(targetId) ;
			if(objectlinks.get(targetId) != null) {
				Set<WorldEntity> affected = objectlinks.get(targetId)
						.stream()
						.map(id -> newState.objects.get(id))
						.filter(e -> e.properties.get(GWState.DESTROYED) == null)
						.collect(Collectors.toSet()) ;
				alpha.apply(target).apply(affected) ;
			}
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
		count++ ;
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
	
	public void save(String filename) {
		// TODO
	}
	
	public static GameWorldModel loadGameWorldModelFromFile(String filename) {
		// TODO
		return null;
	}
	

}
