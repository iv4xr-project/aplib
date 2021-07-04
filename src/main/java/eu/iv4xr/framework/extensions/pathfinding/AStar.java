package eu.iv4xr.framework.extensions.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A* pathfinding algorithm
 * 
 * @author Naraenda
 */
public class AStar<NodeId> implements Pathfinder<NodeId> {

    public enum SearchMode { GREEDY, DIJKSTRA, HEURISTIC } ;
    
    public SearchMode searchMode = SearchMode.HEURISTIC ;
    
    /**
     * When this is set, this function will be use to calculate the heuristic distance between
     * two nodes, rather than using the default heuristic-method supplied by underlying navigation
     * graph.
     */
    public Function<Navigatable<NodeId>,BiFunction<NodeId,NodeId,Float>> dynamicHeuristicDistance = null ;
    
    
    float getHeuristicDistance(Navigatable<NodeId> graph, NodeId a, NodeId b) {
        if(dynamicHeuristicDistance == null) return graph.heuristic(a, b) ;
        return dynamicHeuristicDistance.apply(graph).apply(a, b) ;
    }
    
    @Override
    public ArrayList<NodeId> findPath(Navigatable<NodeId> graph, NodeId start, NodeId goal) {
        PriorityQueue<Priotisable<NodeId>> open = new PriorityQueue<Priotisable<NodeId>>(10,
                new PriotisableComperator<NodeId>());
        // Closed nodes with their associated measured distance
        HashMap<NodeId, Float> closed = new HashMap<NodeId, Float>();
        // Closed nodes with the shortest node towards it.
        HashMap<NodeId, NodeId> paths = new HashMap<NodeId, NodeId>();

        // FIX ... this looks wrong:
        // open.add(new Priotisable<NodeId>(start, 0));
        
        // The fDistance of the starting node does not matter actually, since it will be immediately
        // popped from the open-set anyway.
        float fDistance_ofStart = getHeuristicDistance(graph,start,goal) ;
        if(searchMode == SearchMode.DIJKSTRA) fDistance_ofStart = 0 ;
        open.add(new Priotisable<NodeId>(start,fDistance_ofStart));
        closed.put(start, 0f);

        while (open.size() > 0) {
            // remove the node with the lowest "priority" from the open-list. 
            NodeId current = open.remove().item ;

            // Check if goal is reached, the search stops, and the path to it is returned.
            // In particular note that we don't search further e.g. to find a better path.
            if (current.equals(goal)) {
                // Reconstruct path backwards
                var path = new ArrayList<NodeId>();
                path.add(goal);
                while (! current.equals(start)) {
                    current = paths.get(current);
                    path.add(current);
                }
                // Reverse path to get correct direction
                Collections.reverse(path);
                return path;
            }

            var distToCurrent = closed.get(current).floatValue();

            for (NodeId next : graph.neighbours(current)) {

                // Unbox value
                // int next = next_.intValue();

                float dn = graph.distance(current, next) ;
                if (dn == Float.POSITIVE_INFINITY) 
                    continue;
                
                // System.out.println("## current:" + current + ", next: " + next) ;

                // The distance from start to next
                var distToNext = distToCurrent + dn ;

                // Guard for negative distances
                if (distToNext < 0)
                    distToNext = 0;

                // The distance from next to goal
                var heurFromNext = getHeuristicDistance(graph,next,goal);

                if (!closed.containsKey(next)) {
                    // Unexplored node
                    closed.put(next, distToNext);
                } else if (distToNext < closed.get(next).floatValue()) {
                    // Already explored, but shorter route
                    closed.replace(next, distToNext);
                } else {
                    // Already explored, but longer route
                    continue;
                }
                ;

                if (Float.isInfinite(distToNext))
                    continue;

                paths.put(next, current);
                float fDistance ;
                switch(searchMode) {
                   case DIJKSTRA : fDistance = distToNext ; break ;
                   case GREEDY   : fDistance = heurFromNext ; break ;
                   default : /* heuristic-mode */ fDistance = distToNext + heurFromNext ;
                }
                
                if (!open.stream().anyMatch(p -> p.item.equals(next))
                        // FIX
                        // || open.removeIf(p -> p.item.equals(next) && p.priority > heurFromNext)) {
                        || open.removeIf(p -> p.item.equals(next) && p.priority > fDistance)) {
                    // If not in open set, or already in open set with longer distance...
                    // put next neighbour in the open set
                    
                    // FIX:
                    // open.add(new Priotisable<NodeId>(next, heurFromNext));
                    open.add(new Priotisable<NodeId>(next, fDistance));
                }
            }
        }

        return null;
    }
}

/**
 * Wraps around a type to add a float value on which can be sorted.
 * 
 * @param <T> The type to wrap around.
 */
class Priotisable<T> {
    public float priority;
    public T item;

    /**
     * Wrap around an item to add a priority on which can be sorted.
     * 
     * @param item: The item to wrap around.
     * @param priority: The priority on which can be sorted.
     */
    public Priotisable(T item, float priority) {
        this.item = item;
        this.priority = priority;
    }
}

class PriotisableComperator<T> implements Comparator<Priotisable<T>> {
    @Override
    public int compare(Priotisable<T> o1, Priotisable<T> o2) {
        return Float.compare(o1.priority, o2.priority);
    }
}