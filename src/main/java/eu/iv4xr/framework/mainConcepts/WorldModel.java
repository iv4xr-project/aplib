package eu.iv4xr.framework.mainConcepts;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import eu.iv4xr.framework.spatial.Vec3;

/**
 * This describes a fragment of a virtual world in terms of how it is
 * structurally populated by in-world entities. This fragment can represent what
 * an agent currently sees. We can also use the same representation to represent
 * the agent's belief on how the world is structured; this may incorporate its
 * past knowledge which may no longer be up-to-date.
 */
public class WorldModel<P extends IPlayer, WE extends IWorldEntity> implements Serializable {
	private static final long serialVersionUID = 1L;

    public PlayerRecord player;
    /**
     * Represent the last time this WorldModel is updated with fresh sampling. Note
     * that sampling may only update the state of some of the entities, rather than
     * all, because the agent can only see some part of the world.
     */
    public long timestamp = -1;

    /**
     * In-world entities that populate this World Model.
     */
    public class WorldEntityRecord {
        public WE current = null;
        public WE previous = null;

        public WorldEntityRecord(WE current, WE previous) {
            this.current = current;
            this.previous = previous;
        }

        public WorldEntityRecord(WE current) {
            this.current = current;
        }
    }

    public class PlayerRecord {
        public P current = null;
        public P previous = null;

        public PlayerRecord(P current, P previous) {
            this.current = current;
            this.previous = previous;
        }

        public PlayerRecord(P current) {
            this.current = current;
        }
    }

    public Map<String, WorldEntityRecord> elements = new HashMap<>();
    
    public WorldModel() {
    }

    public List<WE> getCurrentElements() {
    	return elements.values().stream().map(e -> e.current).filter(e -> e != null).collect(Collectors.toList());
    }
    /**
     * Increase the time stamp by one unit.
     */
    public void increaseTimeStamp() {
        timestamp++;
    }

    /**
     * Search a top-level entity with the given id. Note that this method does NOT
     * search recursively in the set of sub-entities.
     */
    public WE getElement(String id) {
    	if (!elements.containsKey(id)) {
    		return null;
    	}
        return elements.get(id).current;
    }

    public WE getBeforeElement(String id) {
    	if (!elements.containsKey(id)) {
    		return null;
    	}
        return elements.get(id).previous;
    }

    public void putElement(String id, WorldEntityRecord worldEntityRecord) {
        elements.put(id, worldEntityRecord);
    }
    
    public void removeElement(String id) {
    	WE element = getElement(id);
    	if (element == null) {
    		return;
    	}
    	
    	putElement(id, new WorldEntityRecord(null, element));
    }
    
    /**
     * True if an entity with the given id is in this WorldModel.
     */
    public boolean contains(String id) {
    	return elements.get(id) != null ;
    }

    /**
     * Checks if an entity is "recent". The entity is recent if it is just recently
     * observed by the agent. Technically, it is recent if it has the same timestamp 
     * and this WorldModel.
     */
    public boolean recent(String eId) {
    	return recent(getElement(eId)) ;
    }
    
    /**
     * Checks if an entity is "recent". The entity is recent if it is just recently
     * observed by the agent. Technically, it is recent if it has the same timestamp 
     * and this WorldModel.
     */
    public boolean recent(WE e) {
    	return e.getTimestamp() == this.timestamp ;
    }

    public WE before(WE e) {
    	if (e.getLastStutterTimestamp() >= 0) {
    		// then the state at the previous sampling time must be the same
    		// as the current state:
    		return e ;
    	}
    	// else, case-1: e changes when it was sampled at e.timestamp
      return getBeforeElement(e.getId());
    	// or case-2: e has no previous state
    }

    /**
     * Return the position of the agent at the last sampling time.
     * 
     * <p>The method requires the agent to also have its own 
     * WorldEntity in this WorldModel.
     */
    public Vec3 positionBefore() {
    	P a = player.current;
    	if (a.getLastStutterTimestamp() >= 0) {
    		// then the state at the previous sampling time must be the same
    		// as the current state:
    		return a.getPosition() ;
    	}
    	// else, case-1: e changes when it was sampled at e.timestamp
      P prev = player.previous;
    	if (prev != null) {
    		return prev.getPosition() ;
    	}
    	// or case-2: e has no previous state
    	return null ;
    }
    

    /**
     * This will add or update an entity e in this WorldModel. The entity can be an
     * entity observed by the agent that owns the WorldModel, or it can also be
     * information sent by another agent.
     * 
     * <p>IMPORTANT: this update may have some side effect on e itself, so e should be
     * a fresh instance. In particular, if e is sent from another agent, it should
     * be cloned first before it is sent.
     * 
     * <p>Case-1: a version ex of e is already in this WorldModel, its state will be
     * updated with e if e's timestamp is more recent and if e's state is different.
     * More specifically, this means that we replace ex with e, and then link ex as
     * e's previous state.
     * <p>
     * Case-2: e is more recent, but its state is the same as ex. We do not add e to
     * the world model; we update ex' timestamp to that of e.
     * 
     * <p>Case-3: e is older than ex, but more recent than ex.previousState. In this
     * case we replace ex.previousState with e.
     * 
     * <p>Case-4: e has no copy in this WorldModel. It will then be added.
     * 
     * <p>The method returns the Entity f which then represents e in the WorldModel.
     * Note that e reflects some state change in the WorldModel if and only if the
     * returned f = e (pointer equality).
     */
    public WE updateEntity(WE e) {
        if (e == null)
            throw new IllegalArgumentException("Cannot update a null entity in a World Model.");
        String id = e.getId();
        WE current = getElement(id);
        if (current == null) {
            // e is new:
            elements.put(id, new WorldEntityRecord(e));
            return e;
        } else {
            // case (1) e is at least as recent as "current":
            if (e.getTimestamp() >= current.getTimestamp()) {
                // check first if there is a state change
                if (e.equals(current)) {
                    var startTimeStutter = current.getLastStutterTimestamp();
                    if (startTimeStutter < 0)
                        startTimeStutter = current.getTimestamp();
                    // keep current; just update its timestamp:
                    current.setTimestamp(e.getTimestamp());
                    // update the stutter-timestamp as well:
                    current.setLastStutterTimestamp(startTimeStutter);
                    return current;
                } else {
                    // the entity has changes its state
                    // (its start-stutter-time should already be initialized to -1)
                    putElement(id, new WorldEntityRecord(e, current));
                    // System.out.println("%%% updating " + e.id) ;
                    return e;
                }
            } else { // case (2): e is older than current:
                WE prev = getBeforeElement(current.getId());
                // System.out.println(">>> current: " + current.id + ", e: " + e.id
                // + ", prev: " + prev) ;
                if (prev == null || e.getTimestamp() > prev.getTimestamp()) {
                    if (!e.equals(current))
                        putElement(id, new WorldEntityRecord(current, e));
                }
                // System.out.println(">>> prev: " + current.getPreviousState()) ;
                return current;
            }
        }
    }

    /**
     * This will merge a sampled (and more recent) observation (represented as
     * another WorldModel) made by the agent into WorldModel.
     * 
     * <p>IMPORTANT: do not use this to merge WorldModel from other agents. Use instead
     * {@link updateEntity} for this purpose. Also note if an agent want to send
     * information about an entity it knows to share it with another agent, it
     * should send a copy of the entity as {@link updateEntity} may have side effect
     * on the entity.
     * 
     * <p>This method will basically add all entities in the new observation into this
     * WorldModel. More precisely, if an entity e was not in this WorldModel, it
     * will be added. But if e was already in the WorldModel, it will not be
     * literally be added anew. Instead, we update e's state in the WorldModel to
     * reflect the new information. The old state (so, the state before the update)
     * will still be linked to the new state, just in case the agent needs it. Only
     * one past-state will be stored though.
     * 
     * <p> The method will check if the given observation is at least as recent as this
     * WorldModel. It fails if this is not the case. If the check passes, the the
     * timestamp of this WorldModel will be updated to that of the observation. All
     * entities that were added (so, all entities in observation) will get this new
     * timestamp as well to reflect the fact that their states are freshly sampled.
     * 
     * <p>The method returns the list of entities that were identified as changing the
     * state of this WorldModel (e.g. either because they are new or because their
     * states are different).
     * 
     * <p>IMPORTANT: note that the implemented merging algorithm is additive. That is,
     * it adds entities into the target WorldModel or updates existing ones, but it
     * will NEVER REMOVE an entity. 
     */
    public List<WE> mergeNewObservation(WorldModel<P, WE> observation) {
        // check if the observation is not null
        if (observation == null)
            throw new IllegalArgumentException("Null observation received");
        if (observation.timestamp < this.timestamp)
            throw new IllegalArgumentException("Cannot merge an older WorldModel into a newer one.");

        // update agent's info:
        if (!this.player.current.equals(observation.player.current)) {
        	this.player = new PlayerRecord(observation.player.current, this.player.current);
        }

        // Add the newly seen entities, or incorporate their change. We will also
        // maintain those entities that induce state change in this WorldModel.
        List<WE> impactEntities = new LinkedList<>();
        for (WorldEntityRecord wer : observation.elements.values()) {
            WE e = wer.current;
            var f = this.updateEntity(e);
            if (e.equals(f)) {
                // System.out.println("%%% updating " + e.id) ;
                // if they are equal, then e induces some state change in the WorldModel.
                impactEntities.add(e);
            }
        }

        // now update the time stamp of this WorldMap to that of the received
        // observation:
        this.timestamp = observation.timestamp;

        // System.out.println("%%% #impactEntities =" + impactEntities.size()) ;
        return impactEntities;
    }

    /**
     * This is used to merge an older observation into this one. E.g. it can be an
     * observation sent by another agent.
     */
    public void mergeOldObservation(WorldModel<P, WE> observation) {
        // check if the observation is not null
        if (observation == null)
            throw new IllegalArgumentException("Null observation received");
        if (observation.timestamp >= this.timestamp)
            throw new IllegalArgumentException("This method expect an older WorldModel to be merged into a newer one.");

        for (WorldEntityRecord wer : observation.elements.values()) {
            WE e = wer.current;
            this.updateEntity(e);
        }
    }

    /**
     * A query method that checks whether the given entity can block movement. If
     * so, it returns true, and else false. This method should be implemented by the
     * subclass. So, override this.
     */
    public boolean isBlocking(WorldEntity e) {
        throw new UnsupportedOperationException();
    }
}
