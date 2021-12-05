package eu.iv4xr.framework.extensions.pathfinding;

import java.util.ArrayList;

import eu.iv4xr.framework.spatial.*;
import eu.iv4xr.framework.spatial.meshes.*;

/**
 * This class is provides an implementation of a navigation graph (an instance
 * of {@link Navigatable}) that facilitates pathfinding over a 3D-surface
 * defined by a surface-mesh with obstacles. By 3D surface we mean that it is a
 * surface in a 3D space. An <i>obstacle</i> is an object that can block travel
 * along one or more edges in a navigation graph. It moreover may have a dynamic
 * state (can change at the runtime), which is either blocking, or non-blocking.
 * If we don't have obstacles, that is also fine; we simply don't add any to the
 * graph. For the actual pathfinding over the graph, you can e.g. use
 * {@link AStar} that can take an instance of {@link Navigatable} to find a path
 * between two nodes in a navigation graph.
 * 
 * <p>
 * This class itself only keeps a navigation graph. In particular, it does not
 * keep the original surface-mesh. Instead, we can convert a given surface-mesh
 * to instance of this navigation graph using the method:
 * 
 * <blockquote> {@link #fromMeshFaceAverage(Mesh)} </blockquote>
 * 
 * The center of the faces in the surface-mesh will be converted the nodes of
 * this navigation-graph. Two nodes are connected by an edge if the original
 * faces they belong to are adjacent. Travel is assumed to be possible by
 * following such edges.
 * 
 * @author Naraenda
 */
public class SimpleNavGraph implements Navigatable<Integer> {

	/**
	 * The set of vertices that form the navigation graph. The index of the vertex
	 * is its id. So if v = vertices(i), then i is its id, and vertices(i) actually
	 * returns the 3D position of the vertex.
	 */
	public ArrayList<Vec3> vertices;

	/**
	 * Describe the connectivity (edges) between the vertices.
	 */
	public EdgeMap edges;

	// TODO: Use a more optimal datastructure for better performance
	public ArrayList<Obstacle<LineIntersectable>> obstacles;

	/**
	 * Add the obstacle to this navgraph. The obstacle is set as non-blocking.
	 */
	public void addObstacle(LineIntersectable obstacle) {
		obstacles.add(new Obstacle<LineIntersectable>(obstacle));
	}

	/**
	 * Add the obstacle to this navgraph. The obstacle is set as blocking.
	 */
	public void addObstacleInBlockingState(LineIntersectable obstacle) {
		var o = new Obstacle<LineIntersectable>(obstacle);
		o.isBlocking = true;
		obstacles.add(o);
	}

	public void removeObstacle(LineIntersectable tobeRemoved) {
		Obstacle<LineIntersectable> q = null;
		for (var o : obstacles) {
			if (o.obstacle == tobeRemoved) {
				q = o;
				break;
			}
		}
		if (q != null)
			obstacles.remove(q);
	}

	public void toggleBlockingOn(LineIntersectable obstacle) {
		for (var o : obstacles) {
			if (o.obstacle == obstacle) {
				o.isBlocking = true;
				break;
			}
		}
	}

	public void toggleBlockingOff(LineIntersectable obstacle) {
		for (var o : obstacles) {
			if (o.obstacle == obstacle) {
				o.isBlocking = false;
				break;
			}
		}
	}

	/**
	 * Return the status of the given obstacle, if it is marked as blocking (true)
	 * or non-blocking (false). Return null of the said obstacle is not known.
	 */
	public Boolean getBlockingStatus(LineIntersectable obstacle) {
		for (var o : obstacles) {
			if (o.obstacle == obstacle) {
				return o.isBlocking;
			}
		}
		return null;
	}

	public SimpleNavGraph(EdgeMap edges, ArrayList<Vec3> vertices, ArrayList<Obstacle<LineIntersectable>> obstacles) {
		this.edges = edges;
		this.vertices = vertices;
		this.obstacles = obstacles;
	}

	public SimpleNavGraph() {
		this.edges = new EdgeMap();
		this.vertices = new ArrayList<Vec3>();
		this.obstacles = new ArrayList<Obstacle<LineIntersectable>>();
	}

	@Override
	public Iterable<Integer> neighbours(Integer id) {
		return edges.neighbours(id);
	}

	public Vec3 position(int i) {
		return vertices.get(i);
	}

	/**
	 * Heuristic distance between any two vertices. Here it is chosen to be the
	 * geometric distance between them.
	 */
	@Override
	public float heuristic(Integer from, Integer to) {
		return Vec3.dist(vertices.get(from), vertices.get(to));
	}

	/**
	 * The distance between two NEIGHBORING vertices. If the connection is not
	 * blocked, it is deined to be their geometric distance, and else +inf.
	 */
	@Override
	public float distance(Integer from, Integer to) {
		Vec3 a = vertices.get(from);
		Vec3 b = vertices.get(to);

		// Check if there is a collision on path
		Line l = new Line(a, b);

		if (obstacles != null && obstacles.stream().filter(o -> o.isBlocking).anyMatch(o -> o.obstacle.intersects(l))) {
			return Float.POSITIVE_INFINITY;
		}

		return Vec3.dist(a, b);
	}

	/**
	 * Generates a SimpleNavGraph from a mesh where a vertex in the navigation graph
	 * corresponds to the average center of a face in the mesh. This works with any
	 * mesh, but only meshes that consist out of convex faces will guarantee to
	 * produce a useful result.
	 * 
	 * @param mesh a convex mesh.
	 * @return a SimpleNavGraph constructed from the given mesh.
	 */
	public static SimpleNavGraph fromMeshFaceAverage(Mesh mesh) {
		SimpleNavGraph g = new SimpleNavGraph();
		EdgeMap edgeMap = g.edges;

		ArrayList<Face> faces = mesh.faces;

		// Get center of each face as vertex
		for (int i = 0; i < faces.size(); i++) {
			Face f = faces.get(i);
			g.vertices.add(f.center(mesh.vertices));
		}

		// Connect common edges in faces to create dual map
		for (int i = 0; i < faces.size(); i++) {
			for (int j = 0; j < i; j++) {
				if (Face.isConnected(faces.get(i), faces.get(j))) {
					edgeMap.put(new Edge(i, j));
				}
			}
		}
		return g;
	}
}