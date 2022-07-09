package eu.iv4xr.framework.extensions.pathfinding;

import java.util.*;
import java.util.stream.Collectors;

import eu.iv4xr.framework.spatial.Vec3;

/**
 * Representing a navigation graph over a 2D tiled-world. The world is assumed to
 * be made of tiles/squares, arranged from tile (0,0) to tile (maxX-1,maxY-1) to
 * form a rectangle world.
 * 
 * <p>The tiles are not explicitly stored. Rather, we only store non-navigable tiles.
 * These are tiles that block movement through them. There are two types: Wall and
 * Door. A wall is always non-navigable. A door can be made blocking/unblocking.
 * 
 * <p>The class also implements {@link Xnavigatable}, so it offers methods to do
 * pathfinding and exploration over the world.
 * 
 * @author Wish
 *
 */
public class Sparse2DTiledSurface_NavGraph 
		implements 
		Navigatable<Sparse2DTiledSurface_NavGraph.Tile> ,
		XPathfinder<Sparse2DTiledSurface_NavGraph.Tile> ,
		CanDealWithDynamicObstacle<Sparse2DTiledSurface_NavGraph.Tile> 
		{
	
	public static class Tile {
		public int x ;
		public int y ;
		public Tile() { }
		public Tile(int x, int y) {
			this.x = x ;
			this.y = y ;
		}
		
		public Tile(float x, float y) {
			this.x = Math.round(x) ;
			this.y = Math.round(y) ;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Tile) {
				Tile t = (Tile) o ;
				return this.x == t.x && this.y == t.y ;
			}
			return false ;
		}
		
		@Override
	    public int hashCode() {
	        return Objects.hash(x, y);
	    }
		
		@Override
		public String toString() {
			return "(" + x + "," + y + ")" ;
		}
	}
	
	public static class NonNavigableTile extends Tile { 
		public NonNavigableTile(int x, int y) {
			super(x,y) ;
		}
	}
	
	public static class Wall extends NonNavigableTile {  
		public Wall(int x, int y) {
			super(x,y) ;
		}
	}
	
	public static class Door extends NonNavigableTile {
		boolean isOpen = false ;
		public Door(int x, int y) {
			super(x,y) ;
		}
		public Door(int x, int y, boolean isOpen) {
			super(x,y) ;
			this.isOpen = isOpen ;
		}
	}
	
	/**
	 * The x coordinates of the this tiled-surface starts from 0 until sizeX-1
	 */
	public int sizeX ;
	
	/**
	 * The y coordinates of the this tiled-surface starts from 0 until sizeY-1
	 */
	public int sizeY ;
	
	public boolean diagonalMovementPossible = false ;
	
    /**
     * If true, the pathfinder will assume that the whole navgraph has been "seen",
     * so no vertex would count as unreacahble because it is still unseen. This
     * essentially turns off memory-based path finding. The default of this flag is
     * false.
     */
    boolean perfect_memory_pathfinding = false;
	
    public Map<Integer,Map<Integer,NonNavigableTile>> obstacles = new HashMap<>() ;
    public Map<Integer,Set<Integer>> seen = new HashMap<>() ;
    
    Set<Tile> frontierCandidates = new HashSet<>() ;
	
	
	public Pathfinder<Tile> pathfinder = new AStar<>() ;
	
	/**
	 * Add a non-navigable tile (obstacle).
	 */
	public void addObstacle(Tile o) {
		if (!(o instanceof NonNavigableTile)) throw new IllegalArgumentException() ;
		
		Map<Integer,NonNavigableTile> xMap = obstacles.get(o.x) ;
		if (xMap == null) {
			xMap = new HashMap<>();
			obstacles.put(o.x,xMap) ;
		}
		xMap.put(o.y, (NonNavigableTile) o) ;
	}
	
	/**
	 * Remove a non-navigable tile (obstacle).
	 */
	public void removeObstacle(Tile o) {
		Map<Integer,NonNavigableTile> xMap = obstacles.get(o.x) ;
		if (xMap != null) {
			xMap.remove(o.y) ;
		}
	}
	
	public void markAsSeen(Tile p) {
		Set<Integer> ys = seen.get(p.x) ;
		if (ys == null) {
			ys = new HashSet<>() ;
			seen.put(p.x, ys) ;
		}
		ys.add(p.y) ;
		frontierCandidates.add(p) ;
	}
	
	public void markAsSeen(List<Tile> newlyseen) {
		for(Tile p : newlyseen) {
			markAsSeen(p) ;
		}
	}
	
	public boolean hasbeenSeen(int x, int y) {
		var ys = seen.get(x) ;
		return ys != null && ys.contains(y) ;
	}
	
	public boolean hasbeenSeen(Tile tile) {
		return hasbeenSeen(tile.x, tile.y) ;
	}
	
	/**
	 * The tile is blocking (true) if it is a wall or a closed door. Else it is non-blocking (false).
	 */
	public boolean isBlocking(Tile tile) {
		if (isWall(tile.x,tile.y)) return true ;
		if (isDoor(tile.x,tile.y)) {
			var o = obstacles.get(tile.x).get(tile.y) ;
			Door door = (Door) o ;
			return ! door.isOpen  ;
		}
		return false ;
	}
	
	
	@Override
	/**
	 * Set the blocking state of this tile, if it is a Door, to the given
	 * blocking-state (true means blocking, false non-blocking).
	 * 
	 * If the tile is not a Door, this method has no effect.
	 */
	public void setBlockingState(Tile tile, boolean isBlocking) {
		int x = tile.x ;
		int y = tile.y ;
		if (isDoor(x,y)) {
			var o = obstacles.get(x).get(y) ;
			Door door = (Door) o ;
			door.isOpen = ! isBlocking ;
		}
	}
	
	public boolean isDoor(int x, int y) {
		var xmap = obstacles.get(x) ;
		if (xmap == null) return false ;
		var o = xmap.get(y) ;
		if (o == null) return false ;
		return o instanceof Door ;
	}
	
	public boolean isWall(int x, int y) {
		var xmap = obstacles.get(x) ;
		if (xmap == null) return false ;
		var o = xmap.get(y) ;
		if (o == null) return false ;
		return o instanceof Wall ;
	}
	

	
    /**
	 * When true then the pathfinder will consider all nodes in the graph to have been seen.
	 */
	public boolean usingPerfectMemoryPathfinding() {
		return perfect_memory_pathfinding ;
	}
	
	/**
	 * When true then the pathfinder will consider all nodes in the graph to have been seen.
	 */
	public void setPerfectMemoryPathfinding(Boolean flag) {
		perfect_memory_pathfinding = flag ;
	}
	
	 /**
     * Mark all vertices as "unseen".
     */
    public void wipeOutMemory() {
        seen.clear();
        frontierCandidates.clear();
    }


    /**
     * Return the neighbors of a tile. A tile u is a neighbor of a tile t if u is
     * adjacent to t.
     * This method does not consider whether u has been seen or not, nor whether
     * u is navigable.
     */
	public List<Tile> physicalNeighbours(int x, int y) {
		
		int left = x-1 ;
		int right = x+1 ;
		int below = y-1 ;
		int above = y+1 ;
		
		List<Tile> candidates = new LinkedList<>() ;
		if(left >= 0)    candidates.add(new Tile(left, y)) ;
		if(right < sizeX) candidates.add(new Tile(right, y)) ;
		if(below >= 0)   candidates.add(new Tile(x,below)) ;
		if(above < sizeY) candidates.add(new Tile(x,above)) ;
		if(diagonalMovementPossible) {
			if(left >= 0 && below >= 0)      candidates.add(new Tile(left,below)) ;
			if(left >= 0 && above < sizeY)    candidates.add(new Tile(left,above)) ;
			if(right < sizeX && above < sizeY) candidates.add(new Tile(right,above)) ;
			if(right < sizeX && below >= 0)   candidates.add(new Tile(right,below)) ;		
		}
		
		candidates = candidates.stream()
			//.filter(c -> ! isBlocked(c.x,c.y))
			.collect(Collectors.toList());
		//System.out.println("&&&& " + candidates.size()) ;
		
		return candidates ;
	}
	
	/**
	 * Return the neighbors of a tile. A tile u is a neighbor of a tile t if u is
     * adjacent to t, and moreover u is navigable (e.g. it is not a wall or a
     * closed door). If the flag diagonalMovementPossible is true, then tiles
     * that are diagonally touching t are also considered neighbors.
     * 
     * <p>Only neighbors that have been seen before will be included.
	 */
	public List<Tile> neighbours_(int x, int y) {
		var candidates = physicalNeighbours(x,y) ;
		//System.out.println("=== (" + x + "," + y + ") -> " + candidates.size()) ;
		
		candidates = candidates.stream()
				.filter(c -> ! isBlocking(c))
				.collect(Collectors.toList());
		
		if (! perfect_memory_pathfinding) {
			candidates = candidates.stream().filter(c -> hasbeenSeen(c.x,c.y)).collect(Collectors.toList()) ;
		}
		//System.out.println("=== " + candidates.size() + ":" + candidates) ;
		return candidates ;
	}
	
	/**
	 * Return the neighbors of a tile. A tile u is a neighbor of a tile t if u is
     * adjacent to t, and moreover u is navigable (e.g. it is not a wall or a
     * closed door). If the flag diagonalMovementPossible is true, then tiles
     * that are diagonally touching t are also considered neighbors.
     * 
     * <p>Only neighbors that have been seen before will be included.
	 */
	@Override
	public Iterable<Tile> neighbours(Tile t) {
		return neighbours_(t.x,t.y) ;
	}

    /**
     * The estimated distance between two arbitrary vertices.
     */ 
	public float heuristic(Tile from, Tile to) {
		if(diagonalMovementPossible)
			// straight-line distance:
			return (float) Math.sqrt(distSq(from.x,from.y,to.x,to.y)) ;
		else
			// Manhattan distance:
			return Math.abs(from.x - to.x) + Math.abs(from.y - to.y) ;
	}

    /**
     * The distance between two neighboring tiles.
     */ 
	public float distance(Tile from, Tile to) {
		if (from.x == to.x || from.y == to.y) return 1 ;
		return 1.4142f ;
	}
	
	public List<Tile> findPath(Tile from, Tile to) {
		return pathfinder.findPath(this, from, to) ;
	}
	
	public List<Tile> findPath(int fromX, int fromY, int toX, int toY) {
		return findPath(new Tile(fromX,fromY), new Tile(toX,toY)) ;
	}
	
    /**
     * This returns the set of frontier-tiles. A tile is a frontier tile if
     * it is a seen/explored tile and it has at least one unexplored and unblocked
     * neighbor. Note that under this definition a frontier does not have to be
     * reachable. You can use {@link findPath} to check which frontiers are reachable.
     */
	public List<Tile> getFrontier() {
		List<Tile> frontiers = new LinkedList<>() ;
		List<Tile> cannotBeFrontier = new LinkedList<>() ;
		for(var t : frontierCandidates) {
			var pneighbors = physicalNeighbours(t.x,t.y) ;
			boolean isFrontier = false ;
			for (var n : pneighbors) {
				if (! hasbeenSeen(n.x,n.y)) {
					frontiers.add(t) ;
					isFrontier = true ;
					break ;
				}
			}
			if (!isFrontier) {
				cannotBeFrontier.add(t) ;
			}
		}
		// remove tiles that are obviously not frontiers:
		frontierCandidates.removeAll(cannotBeFrontier) ;
		//System.out.println(">>> Sparse2D.getFrontier() is called") ;
		return frontiers ;
	}
	
	float distSq(int x1, int y1, int x2, int y2) {
		float dx = x2 - x1 ;
		float dy = y2 - y1 ;
		return dx*dx + dy*dy ;
	}
	
	public List<Tile> explore(Tile startingLocation, Tile heuristicLocation) {
		return explore(startingLocation.x, startingLocation.y, heuristicLocation.x, heuristicLocation.y) ;
	}
	
	public List<Tile> explore(int x, int y, int heuristicX, int heuristicY) {

		var frontiers = getFrontier();
        
        if (frontiers.isEmpty())
            return null;
        // sort the frontiers ascendingly, by their geometric distance to (x,y):
        frontiers.sort((p1, p2) -> Float.compare(distSq(p1.x,p1.y,x,y), distSq(p2.x,p2.y,heuristicX,heuristicY)));

        //System.out.println(">>> #frontiers:" + frontiers.size()) ;
        
        for (var front : frontiers) {
        	//System.out.println(">>> (" + x + "," + y + ")  --> (" + front.x + "," + front.y + ")" ) ;
            var path = findPath(x,y,front.x, front.y);
            //System.out.println("==== path " + path) ;
            // System.out.println("frontier path " + path +" frontier vertices: "+ front.fst);
            if (path != null) {
                return path;
            }
        }
        return null;
	}
	
	@Override
	public String toString() {
		StringBuffer z = new StringBuffer() ;
		for (int y = sizeY-1 ; 0<=y ; y--) {
			for (int x=0; x<sizeX; x++) {
				Tile o = null ;
				var mapx = obstacles.get(x) ;
				if (mapx != null) o = mapx.get(y) ;
				boolean wasSeen = hasbeenSeen(x,y) ;
				char c = ' ' ;
				if(o == null) {
					if (wasSeen) c = '.' ;
 				}
				else {
					if (o instanceof Wall) {
						c = wasSeen ? 'W' : 'w' ;
					}
					if (o instanceof Door) {
						var d = (Door) o ;
						if (d.isOpen) {
							c = wasSeen ? 'O' : 'o' ;
						}
						else {
							c = wasSeen ? 'X' : 'x' ;
						}
					}
				}
				z.append(c) ;
			}
			if (y>0) z.append("\n") ;
		}
		return z.toString() ;
	}
		

}
