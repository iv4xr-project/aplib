package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.util.*;

/**
 * GWZone is part of {@link GameWorldModel}. It represents a zone in a
 * game-world. A zone represents some physical closed area in the game-world,
 * populated by game-objects. "Closed" here means that the zone cannot be
 * entered/escaped except through some exit-points guarded by blockers (e.g. you
 * can imagine a door or a gate as a blocker). Blockers are also game objects
 * btw.
 * 
 * <p>
 * A blocker also connect the zone to another zone. Because of this, a blocker
 * is also a connector between two zones. There can be more than one blockers
 * connecting two zones. The implementation does not prevent you from connecting
 * more than two zones via one blocker, though that is a bit strange,
 * physically.
 * 
 * <p>
 * A blocker may have an open/blocking state.
 * 
 * <p>
 * A GWZone does not keep an explicit representation of "connectors" between
 * zones. So, if you have two zones connected by an unguarded corridor you need
 * to put a "blocker" between them, with a state that is always open.
 * 
 * <p>
 * We will not store the actual game-objects here, in
 * the zone. Instead we will only store their IDs. The actual game-objects are
 * stored in the model {@link GameWorldModel}, of which a zone is part of.
 *  {@link GameWorldModel} is also the one that keeps track which objects are blockers.
 * 
 * @author Samira, Wish.
 *
 */
public class GWZone {
	
	/**
	 * The id/name of the zone.
	 */
	public String id;

	/**
	 * Ids of objects inside the zone.
	 */
	public Set<String> members = new HashSet<>() ;

	public GWZone(String id) { this.id = id ; }
	
	public void addMembers(String ... members) {
		for (var id : members) this.members.add(id) ;
	}

}