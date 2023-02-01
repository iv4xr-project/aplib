package eu.iv4xr.framework.mainConcepts;

import java.io.Serializable;
import java.util.*;

import eu.iv4xr.framework.spatial.Vec3;

/**
 * This describes a fragment of a virtual world in terms of how it is
 * structurally populated by in-world entities. This fragment can represent what
 * an agent currently sees. We can also use the same representation to represent
 * the agent's belief on how the world is structured; this may incorporate its
 * past knowledge which may no longer be up to date.
 */
public class WorldModel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
     * The id of the agent that owns this World Model.
     */
    public String agentId;

    /**
     * The position of the agent that owns this World Model.
     */
    public Vec3 position; // agent's position
    public Vec3 velocity; // agent's velocity
    public Vec3 extent; // agent's dimension (x,y,z size/2)

    /**
     * Represent the last time this WorldModel is updated with fresh sampling. Note
     * that sampling may only update the state of some of the entities, rather than
     * all, because the agent can only see some part of the world.
     */
    public long timestamp = -1;

    /**
     * In-world entities that populate this World Model.
     */
    public Map<String, WorldEntity> elements = new HashMap<>();

    public WorldModel() {
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
    public WorldEntity getElement(String id) {
        return elements.get(id);
    }
    
    /**
     * True if an entity with the given id is in this WorldModel.
     */
    public boolean contains(String id) {
    	return elements.get(id) != null ;
    }
    
    /**
     * Return the value of the given property of an entity e with the given
     * entity id. Return null if the property does not exist in e.
     * 
     * <p>The method requires e to exists in this WorldModel. 
     */
    public Serializable val(String eId, String propertyName) {
        return elements.get(eId).getProperty(propertyName) ;
    }
    
    /**
     * Return the value of the given property of an entity a that represents
     * the agent that owns this WorldModel. Return null if the property 
     * does not exist in e.
     * 
     * <p>The method requires the agent to have its own WorldEntity representation
     * in this WorldModel.
     */
    public Serializable val(String propertyName) {
        return elements.get(this.agentId).getProperty(propertyName) ;
    }
    
    /**
     * Checks if an entity is "recent". The entity is recent if it is just recently
     * observed by the agent. Technically, it is recent if it has the same timestamp 
     * and this WorldModel.
     */
    public boolean recent(String eId) {
    	return elements.get(eId).timestamp == this.timestamp ;
    }
    
    /**
     * Checks if an entity is "recent". The entity is recent if it is just recently
     * observed by the agent. Technically, it is recent if it has the same timestamp 
     * and this WorldModel.
     */
    public boolean recent(WorldEntity e) {
    	return e.timestamp == this.timestamp ;
    }
    
    /**
     * Get the value of a property of a WorldEntity e at the sampling time <b>
     * before</b> its e.timestamp. Returns null if e does not have the property,
     * or if has no such "before" state.
     * 
     * <p>The method requires e to exist in this WorldModel. 
     */
    public Serializable before(WorldEntity e, String propertyName) {
    	if (e.lastStutterTimestamp >= 0) {
    		// then the state at the previuous sampling time must be the same
    		// as the current state:
    		return e.getProperty(propertyName) ;
    	}
    	// else, case-1: e changes when it was sampled at e.timestamp
    	WorldEntity prev = e.getPreviousState() ;
    	if (prev != null) {
    		return prev.properties.get(propertyName) ;
    	}
    	// or case-2: e has no previous state
    	return null ;
    }
    
    /**
     * Let e be the WorldEntity with the given id. This method
     * returns the value of a property of a WorldEntity e at the sampling time <b>
     * before</b> its e.timestamp. It returns null if e does not have the property,
     * or if has no such "before" state.
     * 
     * <p>The method requires e to exist in this WorldModel. 
     */
    public Serializable before(String eId, String propertyName) {
    	return before(elements.get(eId),propertyName) ;
    }
    
    /**
     * Let a be the WorldEntity representing the agent that owns this WorldModel.
     * This method returns the value of a property of a at the sampling time <b>
     * before</b> its a.timestamp. It Returns null if a does not have the property,
     * or if has no such "before" state.
     * 
     * <p>The method requires a to exist in this WorldModel. 
     */
    public Serializable before(String propertyName) {
    	return before(elements.get(this.agentId),propertyName) ;
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
    public WorldEntity updateEntity(WorldEntity e) {
        if (e == null)
            throw new IllegalArgumentException("Cannot update a null entity in a World Model.");
        var current = elements.get(e.id);
        if (current == null) {
            // e is new:
            elements.put(e.id, e);
            return e;
        } else {
            // case (1) e is at least as recent as "current":
            if (e.timestamp >= current.timestamp) {
                // check first if there is a state change
                if (e.hasSameState(current)) {
                    var startTimeStutter = current.lastStutterTimestamp;
                    if (startTimeStutter < 0)
                        startTimeStutter = current.timestamp;
                    // keep current; just update its timestamp:
                    current.assignTimeStamp(e.timestamp);
                    // update the stutter-timestamp as well:
                    current.lastStutterTimestamp = startTimeStutter;
                    return current;
                } else {
                    // the entity has changes its state
                    // (its start-stutter-time should already be initialized to -1)
                    elements.put(e.id, e);
                    e.linkPreviousState(current);
                    // System.out.println("%%% updating " + e.id) ;
                    return e;
                }
            } else { // case (2): e is older than current:
                var prev = current.getPreviousState();
                // System.out.println(">>> current: " + current.id + ", e: " + e.id
                // + ", prev: " + prev) ;
                if (prev == null || e.timestamp > prev.timestamp) {
                    if (!e.hasSameState(current))
                        current.linkPreviousState(e);
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
    public List<WorldEntity> mergeNewObservation(WorldModel observation) {
        // check if the observation is not null
        if (observation == null)
            throw new IllegalArgumentException("Null observation received");
        if (observation.timestamp < this.timestamp)
            throw new IllegalArgumentException("Cannot merge an older WorldModel into a newer one.");

        // update agent's info:
        this.position = observation.position;
        this.velocity = observation.velocity;
        this.extent = observation.extent;

        // Add the newly seen entities, or incorporate their change. We will also
        // maintain those entities that induce state change in this WorldModel.
        List<WorldEntity> impactEntities = new LinkedList<>();
        for (WorldEntity e : observation.elements.values()) {
            var f = this.updateEntity(e);
            if (e == f) {
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
    public void mergeOldObservation(WorldModel observation) {
        // check if the observation is not null
        if (observation == null)
            throw new IllegalArgumentException("Null observation received");
        if (observation.timestamp >= this.timestamp)
            throw new IllegalArgumentException("This method expect an older WorldModel to be merged into a newer one.");

        for (WorldEntity e : observation.elements.values()) {
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
