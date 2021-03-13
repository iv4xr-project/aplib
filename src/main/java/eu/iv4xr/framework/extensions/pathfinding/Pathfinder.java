package eu.iv4xr.framework.extensions.pathfinding;

import java.util.ArrayList;

/**
 * Common interface for pathfinding.
 * 
 * @author Naraenda
 */
public interface Pathfinder {
    /**
     * Finds a path in a navigatable environment.
     * 
     * @param graph: The environment to find a path in.
     * @param start: The starting position of the pathfinder.
     * @param goal: The goal of the pathfinder.
     * @return An arraylist with the path. Null if no path is found.
     */
    public ArrayList<Integer> findPath(Navigatable graph, int start, int goal);
}