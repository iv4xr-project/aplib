package eu.iv4xr.framework.spatial;

public class Obstacle<T> {
    public T obstacle;
    public Boolean isBlocking = false;

    /**
     * Wrap the given o as an Obstacle, marked initially as non-blocking.
     */
    public Obstacle(T obstacle) {
        this.obstacle = obstacle;
    }
}