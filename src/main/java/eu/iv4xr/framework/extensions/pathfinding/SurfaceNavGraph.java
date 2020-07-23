package eu.iv4xr.framework.extensions.pathfinding;

import java.util.*;

import eu.iv4xr.framework.spatial.Line;
import eu.iv4xr.framework.spatial.Obstacle;
import eu.iv4xr.framework.spatial.Vec3;
import eu.iv4xr.framework.spatial.meshes.*;

/**
 * A navigation-graph over a 3D-surface. The surface is described by
 * a set of (convex) polygons/Faces. The navigation-graph over this
 * surface is formed by the corners and centers of the Faces. Corners
 * are connected to each other, as they are specified by each Face.
 * Additionally, each corner of a Face f is connected to f's center
 * point. For each two Faces f1 and f2 that are connected (having a
 * common edge), we also connect their center-points.
 * 
 * This navigation-graph also supports few additional features:
 * 
 *   (1) It includes a PathFinder to calculate a path from one vertex
 *   in the nav-graph to another. By default, an A* PathFinder is used,
 *   though you can change that.
 *   
 *   (2) An option to do memory-based navigation. This option is by 
 *   default turned-on. Under this navigation mode, all vertices 
 *   are initially marked as unseen. An agent can then incrementally
 *   mark vertices as "seen". Navigation is only possible over the 
 *   part of the graph that were previously seen. 
 *   
 *   (3) An option to prefer travel through the faces' center points,
 *   or to travel along border edges. This is done my increasing the
 *   cost/distance of going to unpreferred vertices.
 * 
 *   (4) This navigation-graph additionally marks vertices which are on
 *   the border-edges. A border-edge is an edge that is not shared by
 *   multiple Faces. The type of a vertex can asked via  
 *   verticesType.get(i), which will return the type of vertex i. This
 *   returns either BORDER (if i is a border vertex), CENTER (if it is
 *   the center of a Face), or OTHER.
 * 
 * @author Wish
 *
 */
public class SurfaceNavGraph extends SimpleNavGraph {
	
	/**
	 * A vertex is a BORDER-vertex if it lies in a border-edge. An edge is a
	 * border-egde if it is not shared by two or more Faces.
	 * 
	 * A vertex is a CENTRE-vertex if it is the centre of a Face.
	 * 
	 * Other vertices have type OTHER.
	 */
	enum VertexType { BORDER, CENTRE, OTHER } 

	/**
	 * The convex-polygons/faces that overlay the vertices of this navigation graph.
	 * Each Face is defined by corners, which are vertices of this navigation graph.
	 * A Face describes a (small) surface that is physically navigatable. This
	 * navigation graph itself does not specify how to navigate to an arbitrary
	 * point in a Face (except when it is one of its corners), but knowing the
	 * surface the user of this class at least infer alternate locations to navigate
	 * to. It is up to the user how to use such information.
	 */
    public ArrayList<Face> faces ;
    
    /**
     * verticesType.get(i) specifies the type of vertex i. It is either CENTER, BORDER,
     * or OTHER.
     */
    public ArrayList<VertexType> verticesType ;
    
    /**
     * If seenVertices.get(i) is true it means that vertex i is considered to have been
     * "seen", for the purpose of memory-based navigation.
     */
    public ArrayList<Boolean> seenVertices ;
    
    
    /**
     * This maps the Faces to their corresponding center-points. The center-point
     * is represented by their id/index in the vertices array.
     */
    HashMap<Face,Integer> faceToCenterIdMap ;
    
    /**
     * The number of vertices that are corners of the Faces (so, vertices which
     * are not centers). These vertices will also have id between 0 and 
     * numberOfBaseVertices-1.
     */
    public int numberOfBaseVertices ;
    
    
    public static final int PREFER_CENTER = 0 ;
    public static final int PREFER_BORDER = -1 ;
    public static final int NO_PREFERENCE = 1 ;
    
    /**
	 * Specify preference when trying to find a path over the nav-graph. When this
	 * preference is PREFER_CENTER, we prefer a path that goes through the centers
	 * of the faces. This is done by applying penalty to the cost of traversal to
	 * non-center vertices. When this preference is PREFER_BORDER, we prefer a path
	 * that goes through border-vertices.
	 * 
	 * PREFER_CENTER is the default.
	 */
    public int travelPreferrence = PREFER_CENTER ;
    
    
    Pathfinder pathfinder ;
    
    public SurfaceNavGraph(Mesh mesh) {
        super();
        // copying the vertices and faces:
        vertices.addAll(mesh.vertices) ;
        numberOfBaseVertices = vertices.size() ;
        faces = new ArrayList<>() ;
        faces.addAll(mesh.faces) ;
        // copying the edges, and as we do build the reachability relation as well:
        for (Edge e : mesh.edges) {
        	this.edges.put(e);
        }
        
        // calculating the types of the current vertices:
        verticesType = new ArrayList<>() ;
        for(int k=0; k<numberOfBaseVertices; k++) {
        	verticesType.add(VertexType.OTHER) ;
        }
        Set<Integer> borderVertices ;
        for (Edge e : mesh.edges) {
        	int countMembership = 0 ;
        	for (Face face : mesh.faces) {
        		if (face.containsEdge(e)) countMembership++ ;
        		if (countMembership >= 2) {
        			// ok so the edge e is shared, and hence not a border edge
        			break ;
        		}
        	}
        	if (countMembership == 1) {
        		// e is a border edge:
        		verticesType.set(e.i, VertexType.BORDER) ;
        		verticesType.set(e.j, VertexType.BORDER) ;	
        	}
        }
        
        // Add the center points of the Faces to the nav-Graph, and connect
        // their corners to the corresponding center-point:
        faceToCenterIdMap = new HashMap<>() ;
        for (Face face : faces) {
        	Vec3 center = face.center(mesh.vertices) ;
        	int center_id = vertices.size() ;
        	// add the center to the set of vertices:
        	vertices.add(center) ;
        	// mark its type:
        	verticesType.add(VertexType.CENTRE) ;
        	// remember what the id of this center:
        	faceToCenterIdMap.put(face,center_id) ;
        	// add edges that connect the corners of the Face to this center
        	for (Integer corner : face) {
        		edges.put(new Edge(corner,center_id));
        	}	
        }
        
        // We will also connect the centers of connected Faces:
        for (int i = 0; i < faces.size(); i++) {
            for (int j = 0; j < i; j++) {
            	var face1 = faces.get(i) ;
            	var face2 = faces.get(j) ;
                if (Face.isConnected(face1,face2)) {
                	var center1 = faceToCenterIdMap.get(face1) ;
                	var center2 = faceToCenterIdMap.get(face2) ;
                    edges.put(new Edge(center1,center2));
                }
            }
        }
        
        // By default we will mark all vertices as "unseen"
        seenVertices = new ArrayList<>() ;
        wipeOutMemory() ;
        
        // setting A* as the default pathfinder:
        pathfinder = new AStar() ;
    }
    
    /**
     * Mark all vertices as "unseen".
     */
    public void wipeOutMemory() {
    	seenVertices.clear();
    	int N = vertices.size() ;
    	for(int k = 0; k<N; k++) {
        	seenVertices.add(false) ;
        }
    }
    
    /**
     * Cheat. Mark all vertices as "seen". In other words, from this point on
     * memory-based navigation is disabled (until we do wipeOutMemory()).
     */
    public void disableMemoryBasedNavigation() {
    	int N = seenVertices.size() ;
        for(int k = 0; k<N; k++) {
        	seenVertices.add(true) ;
        }
    }
    
    public void setPathFinder(Pathfinder pf) {
    	pathfinder = pf ;
    }
    
    /**
     * Mark the given vertices as "seen" for the purpose of memory-based navigation.
     * Since Faces' center-points were added artificially (they were not explicitly
     * present in the mesh-data used to build this nav-graph), the agent that calls
     * this method may not check if it also saw center-points. This method will
     * therefore take the heuristic to mark a center-point as seen if one of its
     * non-center neighbor is marked as seen.
     */
    public void markAsSeen(List<Integer> seen) {
    	for (Integer v : seen) {
    		// mark v as seen:
    		seenVertices.set(v,true) ;
    		// additionally, if v is not a center, then mark the center connected to
    		// it as "seen".
    		if (verticesType.get(v) != VertexType.CENTRE) {
    			var neighbors = edges.neighbours(v) ;
    			for (Integer z : neighbors) {
    				if (verticesType.get(z) == VertexType.CENTRE)
    					seenVertices.set(z,true) ;	
    			}
    		}
    	}
    }
    
    /**
	 * Return the id/index of a vertex, which is nearest to the given location, and
	 * moreover the line between the location and this vertex is not blocked by any
	 * of the blocking obstacles.
	 * 
	 * The method returns null if no such vertex can be found.
	 * 
	 * Note that this method does not take into account whether this nearest vertex
	 * has been seen or not. If it is not, navigation to there will not be possible.
	 * Note: this choice is intentional.
	 * 
	 */
    public Integer getNearestUnblockedVertex(Vec3 location) {
    	float dist = Float.MAX_VALUE ;
    	Integer nearest = null ;
    	int id = 0 ;
    	for (Vec3 v : vertices) {
    		boolean direct_line_is_blocked = false ;
    		for (var obs : obstacles) {
    			Line line = new Line(location,v) ;
    			if (obs.isBlocking && obs.obstacle.intersects(line))  {
    				direct_line_is_blocked = true ;
    				break ;
    			}
    		}
    		if (direct_line_is_blocked) break ;
    		if (Vec3.dist(location, v) < dist) nearest = id ;
    		id++ ;
    	}
    	return nearest ;
    }
    
    
    /**
     * Return the neighboring vertices of id. Only neighbors marked as "seen"
     * will be returned (so as to support the memory-based navigation).
     */
    @Override
    public Iterable<Integer> neighbours(int id) {
    	// get the neighbors of id, and CLONE it to avoid destructive side effect
    	// when we later filter it:
    	HashSet<Integer> neighbors = (HashSet<Integer>) edges.neighbours(id).clone();
    	// only let "seen" neighbors through:
    	neighbors.removeIf(v -> ! seenVertices.get(v));
        return neighbors ;
    }
    
    
    
    @Override
    public float distance(int from, int to) {
    	float distance = super.distance(from,to) ;
    	if (distance == Float.POSITIVE_INFINITY) return distance ;
        // apply penalty to the distanced according to the travel preference:
        switch(travelPreferrence) {
           case PREFER_CENTER : 
        	   if (verticesType.get(to) != VertexType.CENTRE)
        		   distance = 1.2f * distance ;
        	   break ; 
           case PREFER_BORDER : 
        	   if (verticesType.get(to) != VertexType.BORDER)
            	   distance = 1.1f * distance ;
               break ; 
           default :
        }
        return distance ;
    }
    
    /**
     * Return a path from the given start-vertex to the goal-vertex. If the goal-vertex
     * is reachable, a path will be returned. A* is used as the default path-finder,
     * which should return the optimal path to get to the goal.
     * 
     * The path-finding will be memory-based. That is, only navigation over vertices
     * marked as seen will be possible.
     */
    public ArrayList<Integer> findPath(int start, int goal) {
    	return pathfinder.findPath(this, start, goal) ;
    }
    
    /**
     * The same as the other findPath. This will return a path from a vertex closest
     * to the given start position, to a vertex closest to the given goal location.
     * It is up to the agent to figure out how to get from its own physical start
     * location to the starting vertex, and to get from the goal vertex to its
     * actual goal location.
     * 
     * The method calculates the start and goal-nodes such that the straight line
     * between them and the corresponding start and goal locations are not blocked
     * by any of the blocking obstacles.
     */
    public ArrayList<Integer> findPath(Vec3 start, Vec3 goal) {
    	Integer startNode = getNearestUnblockedVertex(start) ;
    	if (startNode == null) return null ;
    	Integer goalNode = getNearestUnblockedVertex(goal) ;
    	return findPath(startNode,goalNode) ;
    }
    
    public Integer explore() {
    	// bla bla
    }

}
