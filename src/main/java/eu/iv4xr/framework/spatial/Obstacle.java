package eu.iv4xr.framework.spatial;

public class Obstacle<T> {
    public T obstacle;
    public Boolean isBlocking;

    public Obstacle(T obstacle) {
        this.obstacle = obstacle;
    }
}