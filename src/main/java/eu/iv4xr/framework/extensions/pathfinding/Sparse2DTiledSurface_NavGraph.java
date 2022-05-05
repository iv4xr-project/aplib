package eu.iv4xr.framework.extensions.pathfinding;

import java.util.*;
import java.util.stream.Collectors;

import eu.iv4xr.framework.spatial.Vec3;

public class Sparse2DTiledSurface_NavGraph implements Navigatable<Sparse2DTiledSurface_NavGraph.Tile> {
	
	public static class Tile {
		public int x ;
		public int y ;
		public Tile() { }
		public Tile(int x, int y) {
			this.x = x ;
			this.y = y ;
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
	
	public int maxX ;
	public int maxY ;
	
	public boolean diagonalMovementPossible = false ;
	
    /**
     * If true, the pathfinder will assume that the whole navgraph has been "seen",
     * so no vertex would count as unreacahble because it is still unseen. This
     * essentially turns off memory-based path finding. The default of this flag is
     * false.
     */
    public boolean perfect_memory_pathfinding = false;
	
    public Map<Integer,Map<Integer,NonNavigableTile>> obstacles = new HashMap<>() ;
    public Map<Integer,Set<Integer>> seen = new HashMap<>() ;
    
    Set<Tile> frontierCandidates = new HashSet<>() ;
	
	
	public Pathfinder<Tile> pathfinder = new AStar<>() ;
	
	public void addNonNavigable(NonNavigableTile o) {
		Map<Integer,NonNavigableTile> xMap = obstacles.get(o.x) ;
		if (xMap == null) {
			xMap = new HashMap<>();
			obstacles.put(o.x,xMap) ;
		}
		xMap.put(o.y, o) ;
	}
	
	public void removeNonNavigable(int x, int y) {
		Map<Integer,NonNavigableTile> xMap = obstacles.get(x) ;
		if (xMap != null) {
			xMap.remove(y) ;
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
	
	public boolean hasBeenSeen(int x, int y) {
		var ys = seen.get(x) ;
		return ys != null && ys.contains(y) ;
	}
	
	public void openDoor(int x, int y) {
		if (isDoor(x,y)) {
			var o = obstacles.get(x).get(y) ;
			Door door = (Door) o ;
			door.isOpen = true ;
		}
	}
	
	public void closeDoor(int x, int y) {
		if (isDoor(x,y)) {
			var o = obstacles.get(x).get(y) ;
			Door door = (Door) o ;
			door.isOpen = false ;
		}
	}
	
	public boolean isDoor(int x, int y) {
		var xmap = obstacles.get(x) ;
		if (xmap == null) return false ;
		var o = xmap.get(y) ;
		if (o == null) return false ;
		return o instanceof Door ;
	}
	
	public boolean isBlocked(int x, int y) {
		var xmap = obstacles.get(x) ;
		if (xmap == null) return false ;
		var o = xmap.get(y) ;
		if (o == null) return false ;
		if (o instanceof Door) {
			return ! ((Door) o).isOpen ;
		}
		else return true ;
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
     * adjacent to t, and moreover u is navigable (e.g. it is not a wall or a
     * closed door). If the flag diagonalMovementPossible is true, then tiles
     * that are diagonally touching t are also considered neighbors.
     * 
     * This method does not consider whether u has been seen or not.
     */
	public List<Tile> physicalNeighbours(int x, int y) {
		
		int left = x-1 ;
		int right = x+1 ;
		int below = y-1 ;
		int above = y+1 ;
		
		List<Tile> candidates = new LinkedList<>() ;
		if(left >= 0)    candidates.add(new Tile(left, y)) ;
		if(right < maxX) candidates.add(new Tile(right, y)) ;
		if(below >= 0)   candidates.add(new Tile(x,below)) ;
		if(above < maxY) candidates.add(new Tile(x,above)) ;
		if(diagonalMovementPossible) {
			if(left >= 0 && below >= 0)      candidates.add(new Tile(left,below)) ;
			if(left >= 0 && above < maxY)    candidates.add(new Tile(left,above)) ;
			if(right < maxX && above < maxY) candidates.add(new Tile(right,above)) ;
			if(right < maxX && below >= 0)   candidates.add(new Tile(right,below)) ;		
		}
		
		candidates = candidates.stream()
			.filter(c -> ! isBlocked(c.x,c.y))
			.collect(Collectors.toList());
		
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
		
		if (! perfect_memory_pathfinding) {
			candidates = candidates.stream().filter(c -> hasBeenSeen(c.x,c.y)).collect(Collectors.toList()) ;
		}
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
		return (float) Math.sqrt(distSq(from.x,from.y,to.x,to.y)) ;
	}

    /**
     * The distance between two neighboring tiles.
     */ 
	public float distance(Tile from, Tile to) {
		 if (from.x == to.x || from.y == to.y) return 1 ;
		 return 1.4142f ;
	}
	
	
	public List<Tile> findPath(int fromX, int fromY, int toX, int toY) {
		return pathfinder.findPath(this, new Tile(fromX,fromY), new Tile(toX,toY)) ;
	}
	
    /**
     * This returns the set of frontier-tiles. A tile is a frontier tile if
     * it is a seen/explored tile and it has at least one unexplored and unblocked
     * neighbor.
     */
	public List<Tile> getFrontierTiles() {
		List<Tile> frontiers = new LinkedList<>() ;
		List<Tile> cannotBeFrontier = new LinkedList<>() ;
		for(var t : frontierCandidates) {
			int maxNumberOfNeighbors = physicalNeighbours(t.x,t.y).size() ;
			int N = neighbours_(t.x,t.y).size() ;
			if (N == maxNumberOfNeighbors) {
				cannotBeFrontier.add(t) ;
				continue ;
			}
			frontiers.add(t) ;
		}
		// remove tiles that are obviously not frontiers:
		frontierCandidates.removeAll(cannotBeFrontier) ;
		return frontiers ;
	}
	
	float distSq(int x1, int y1, int x2, int y2) {
		float dx = x2 - x1 ;
		float dy = y2 - y1 ;
		return dx*dx + dy*dy ;
	}
	
	
	public List<Tile> explore(int x, int y) {

		var frontiers = getFrontierTiles();
        
        if (frontiers.isEmpty())
            return null;
        // sort the frontiers ascendingly, by their geometric distance to (x,y):
        frontiers.sort((p1, p2) -> Float.compare(distSq(p1.x,p1.y,x,y), distSq(p2.x,p2.y,x,y)));

        for (var front : frontiers) {
            var path = findPath(x,y,front.x, front.y);
            // System.out.println("frontier path " + path +" frontier vertices: "+ front.fst);
            if (path != null) {
                return path;
            }
        }
        return null;
	}
	
	

}
