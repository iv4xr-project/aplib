package eu.iv4xr.framework.mainConcepts;

public interface IWorldEntity {
  public long getTimestamp();
  public void setTimestamp(long timeStamp);
  public long getLastStutterTimestamp();
  public void setLastStutterTimestamp(long timeStamp);

  public String getId();
  public void setId(String id);

  public boolean equals(Object other);
}
