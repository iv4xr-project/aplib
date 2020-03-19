package eu.iv4xr.framework.extensions.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * A* pathfinding algorithm
 * 
 * @author Naraenda
 */
public class AStar implements Pathfinder {

	@Override
	public ArrayList<Integer> findPath(Navigatable graph, int start, int goal) {
        // Open nodes sorted by heuristic
        PriorityQueue<Priotisable<Integer>> open = new PriorityQueue<Priotisable<Integer>>();
        // Closed nodes with their associated measured distance
        HashMap<Integer, Float> closed = new HashMap<Integer, Float>();
        // Closed nodes with the shortest node towards it.
        HashMap<Integer, Integer> paths = new HashMap<Integer, Integer>();

        open.add(new Priotisable<Integer>(start, 0));
        closed.put(start, 0f);

        while (open.size() > 0) {
            var current = open.remove().item.intValue();

            // Check if goal is reached
            if (current == goal) {
                // Reconstruct path backwards
                var path = new ArrayList<Integer>();
                path.add(goal);
                while (current != start) {
                    current = paths.get(current);
                    path.add(current);
                }
                // Reverse path to get correct direction
                Collections.reverse(path);
                return path;
            }

            var distToCurrent = closed.get(current).floatValue();

            for (var next_ : graph.neighbours(current)) {
                // Unbox value
                int next = next_.intValue();

                // The distance from start to next
                var distToNext = distToCurrent + graph.distance(current, next);

                // Guard for negetive distances
                if (distToNext < 0) 
                    distToNext = 0;

                // The distance from next to goal
                var heurFromNext = graph.heuristic(next, goal);

                if (!closed.containsKey(next)) {
                    // Unexplored node
                    closed.put(next, distToNext);
                }
                else if (distToNext < closed.get(next).floatValue()) {
                    // Already explored, but shorter route
                    closed.replace(next, distToNext);
                } else {
                    // Already explored, but longer route
                    continue;
                };

                if (Float.isInfinite(distToNext))
                    continue;

                paths.put(next, current);

                if (!open.stream().anyMatch(p -> p.item.intValue() == next) || open.removeIf(p -> p.item.intValue() == next && p.priority > heurFromNext)){
                    // If not in open set, or already in open set with longer distance...
                    // put next neighbour in the open set
                    open.add(new Priotisable<Integer>(next, heurFromNext));
                }
            }
        }

		return null;
	}
}

/**
 * Wraps around a type to add a float value on which can be sorted.
 * @param <T> The type to wrap around.
 */
class Priotisable<T> implements Comparator<Priotisable<T>> {
    public float priority;
    public T item;

    /**
     * Wrap around an item to add a priority on which can be sorted.
     * @param item: The item to wrap around.
     * @param priority: The priority on which can be sorted.
     */
    public Priotisable(T item, float priority) {
        this.item = item;
        this.priority = priority;
    }

	@Override
	public int compare(Priotisable<T> o1, Priotisable<T> o2) {
		return Float.compare(o1.priority, o2.priority);
    }
}