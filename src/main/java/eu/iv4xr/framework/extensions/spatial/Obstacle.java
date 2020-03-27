package eu.iv4xr.framework.extensions.spatial;

public class Obstacle<T> {
    public T obstacle;
    public Boolean isBlocking;

    public Obstacle(T obstacle) {
        this.obstacle = obstacle;
    }
}