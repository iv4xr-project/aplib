package eu.iv4xr.framework.interop;

import eu.iv4xr.framework.spatial.Vec3;

public interface IPositionable {
    Vec3 getPosition();

    Vec3 getVelocity();

    /**
     * agent's dimension (x,y,z size/2)
     */
    Vec3 getExtent();
}
