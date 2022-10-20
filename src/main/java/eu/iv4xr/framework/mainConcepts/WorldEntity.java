package eu.iv4xr.framework.mainConcepts;

import java.io.*;
import java.util.*;

import eu.iv4xr.framework.spatial.Vec3;

public class WorldEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
     * A unique id identifying this entity.
     */
    public final String id;

    /**
     * The type-name of the entity, e.g. "door".
     */
    public final String type;

    /**
     * Represent the last time the state of this entity is sampled.
     */
    public long timestamp = -1;

    /**
     * The last time the state of this entity is sampled, after which its state is
     * always sampled to be the same as its current state (the start of stutter
     * period towards the current state). If the value is -1, if this time is not
     * known.
     */
    public long lastStutterTimestamp = -1;

    /**
     * The center position of this entity,
     */
    public Vec3 position;
    /**
     * Bounding box of this entity.
     */
    public Vec3 extent; // bounding box
    public Vec3 velocity;

    /**
     * If true then this entity is "dynamic", which means that its state may change
     * at the runtime. Note that an entity does not have to be moving (having
     * velocity) to be dynamic.
     */
    public final boolean dynamic;
    public Map<String, Serializable> properties = new HashMap<>();

    public Map<String, WorldEntity> elements = new HashMap<>();

    public WorldEntity(String id, String type, boolean dynamic) {
        this.id = id;
        this.type = type;
        this.dynamic = dynamic;
    }

    /**
     * To keep one single copy of the entity's previous state.
     */
    private WorldEntity previousState = null;

    private boolean equal_(Object a, Object b) {
        if (a == null)
            return b == null;
        return a.equals(b);
    }

    /**
     * Let e be non-null and represent the same entity as this entity (they have the
     * same ID), but its state is possibly different than this entity. This method
     * checks if both entity have the same state.
     * 
     * Dynamic entity is assumed not to change state. Else this method first check
     * the hash-value of both entities. If they are the same, this method performs
     * deep comparison of position, velocity, properties, and sub-entities. This
     * might be a bit expensive; override this method if a faster implementation is
     * wanted.
     */
    public boolean hasSameState(WorldEntity old) {

        // non-dynamic entity cannot change state:
        if (!this.dynamic)
            return true;
        // else:
        if (this.hashCode() != old.hashCode())
            return false;

        if (!(equal_(position, old.position) && equal_(velocity, old.velocity)
                && properties.size() == old.properties.size() && equal_(extent, old.extent)))
            return false;
        for (var P : properties.entrySet()) {
            var q = old.properties.get(P.getKey());
            var Pval = P.getValue() ;
            if (Pval==null ?  q!=null : ! Pval.equals(q))
                return false;
        }
        // so the entities have the same properties.. let's now check the children
        if (this.elements.size() != old.elements.size())
            return false;
        for (var elem_ : elements.entrySet()) {
            var elem2 = old.elements.get(elem_.getKey());
            if (elem2 == null)
                return false;
            var elem1 = elem_.getValue();
            if (!elem1.hasSameState(elem2))
                return false;
        }
        return true;
    }

    public Serializable getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    public boolean getBooleanProperty(String propertyName) {
        var V = getProperty(propertyName);
        if (V == null)
            return false;
        if (!(V instanceof Boolean))
            throw new IllegalArgumentException(id + " has no boolean property " + propertyName);
        return (boolean) V;
    }

    public String getStringProperty(String propertyName) {
        var V = getProperty(propertyName);
        if (V == null)
            return null;
        return V.toString();
    }

    public int getIntProperty(String propertyName) {
        var V = getProperty(propertyName);
        if (V == null || !(V instanceof Integer))
            throw new IllegalArgumentException(id + " has no integer property " + propertyName);
        return (int) V;
    }

    /**
     * This will link e as the previous state of this Entity. The previous state of
     * e is cleared to null (we only want to track the history of past state up to
     * length 1).
     * 
     * This method assume that e represents the same Entity as this Entity (e.g.
     * they have the same id).
     */
    public void linkPreviousState(WorldEntity e) {
        this.previousState = e;
        if (e != null)
            e.previousState = null;
    }

    /**
     * Return a WorldEntity representing this entity's previous state, if that is
     * tracked.
     */
    public WorldEntity getPreviousState() {
        return previousState;
    }

    /**
     * True if this entity has no previous state, or if its state differs from its
     * previous. Note that we only track 1x previous state (so there is no longer
     * chain of previous states).
     */
    public boolean hasChangedState() {
        if (previousState == null)
            return true;
        return !hasSameState(previousState);
    }

    public boolean hasPreviousState() {
        return previousState != null;
    }

    /**
     * Set the time-stamp of this Entity and its elements to the given time.
     */
    public void assignTimeStamp(long ts) {
        timestamp = ts;
        for (var e : elements.values())
            e.assignTimeStamp(ts);
    }

    /**
     * If true then this entity is a moving entity. This is defined as having a
     * non-null velocity. Note that the entity may still have a zero velocity; but
     * it will still be classified as "moving".
     */
    public boolean isMovingEntity() {
        return velocity != null;
    }

    /**
     * The hashcode of this Entity.
     */
    @Override
    public int hashCode() {
        return Objects.hash(position, velocity, extent, properties, elements);
    }

    public WorldEntity deepclone() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(this);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);
        var copied = (WorldEntity) in.readObject();
        return copied;
    }
    
    @Override
    public String toString() {
    	StringBuffer z = new StringBuffer() ;
    	z.append("Id:" + this.id);
    	z.append("\nPos:" + this.position + ", vel:" + this.velocity + ", extent:" + this.extent) ;
    	z.append("\nT:" + this.timestamp) ;
    	z.append("\nProperties:") ;
    	for (var p : this.properties.entrySet()) {
    		z.append("\n   " + p.getKey() + ":" + p.getValue().toString()) ;
    	}
    	z.append("\nSub-objs: ") ;
    	int k = 0 ;
    	for (var e : this.elements.values()) {
    		if (k>0) z.append(", ") ;
    		z.append(e.id) ;
    		k++ ;
    	}
    	return z.toString() ;
    }

}
