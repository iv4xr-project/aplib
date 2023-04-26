package eu.iv4xr.framework.mainConcepts;

import eu.iv4xr.framework.spatial.Vec3;

public interface IPlayer {
  public Vec3 getPosition();
  public void setPosition(Vec3 position);
  public long getLastStutterTimestamp();
  public void setLastStutterTimestamp(long timeStamp);
}
