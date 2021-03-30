package eu.iv4xr.framework.interop;

import java.io.Serializable;
import java.util.Map;

/**
 * WorldEntity should not manage it's previous states and versions. Some other high-level object should do that.
 * linkPreviousState, getPreviousState, hasPreviousState, etc.
 *
 * wrappers for accessing properties seems unnecessary
 */
public interface IWorldEntity extends IPositionable {
    String getId();
    String getType();
    long getTimestamp();
    /**
     * If true then this entity is "dynamic", which means that its state may change at the runtime.
     * Note that an entity does not have to be moving (having velocity) to be dynamic.
     */
    boolean isDynamic();
    Map<String, Serializable> getProperties();
    Map<String, ? extends IWorldEntity> getElements();

}
