package eu.iv4xr.framework.extensions.pathfinding;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.utils.Pair;

/**
 * Support navigation over multiple areas, linearly stacked in layers. The areas are 
 * numbered starting from 0. Each area is connected to the area before it through a
 * portal, and to the area next to it with another portal. The first and last areas
 * only have one portal.
 * 
 * @author Wish
 *
 * @param <NodeId>
 * @param <Nav>
 */
public class LayeredAreasNavigation<
			NodeId, 
			Nav extends Xnavigatable<NodeId> & CanDealWithDynamicObstacle<NodeId>> 
		implements 
		Xnavigatable<Pair<Integer,NodeId>>,
		CanDealWithDynamicObstacle<Pair<Integer,NodeId>> {
	
	public List<Nav> areas = new LinkedList<>() ;
		
    boolean perfect_memory_pathfinding = false;

	
	/**
	 * Specify the heuristic distance between two adjacent area. For faster
	 * search, this should be larger than the maximum "diameter" of the areas.
	 */
	public float heuristicDistanceBetweenAdjacentAreas = 1000 ;
	
	/**
	 * Specify the cost/distance of going through a portal to go to an
	 * adjacent area.
	 */
	public float distanceBetweenTwoSidesOfPortal = 1f ;

	/**
	 * Portals between areas. If P is the i-th Portal in the list, this 
	 * P connects area i to i+1. P.lowPortal is a node in area  i, and
	 * P.highPortal is a node in area i+1. From P.lowPortal we can travel
	 * to P.highPortal (which is in area i+1).
	 * 
	 * <p>Note that if there are n areas, we will only have n-1 portals, as
	 * the last area have no next-area to connect to.
	 */ 
	public List<Portal<NodeId>> portals ;
	
	/**
	 * Representing a bi-directional "portal" between areas.
	 */
	public class Portal<Node> {
		/**
		 * The portal from area n to go up to area n+1.
		 */
		public Node lowPortal ; 
		
		/**
		 * The portal from area n+1 to go down to area n.
		 */
		public Node highPortal ;
		
		/**
		 * When true, the portal can be navigated through. Else it is blocked.
		 */
		public boolean isOpen = false ;
		
		public Portal(Node n1, Node n2) {
			lowPortal= n1 ;
			highPortal = n2 ;
		}
				
	}
	
	public void addArea(Nav area) {
		areas.add(area) ;
		area.setPerfectMemoryPathfinding(perfect_memory_pathfinding);
	}
	
	/**
	 * Add a portal from area a to a+1, where a is the current number
	 * of portals.
	 */
	public void addPortal(NodeId portalLow, NodeId portalHigh) {
		var portal = new Portal<NodeId>(portalLow,portalHigh) ;
		portals.add(portal) ;
	}
	
	/**
	 * Return the low-portal of the given area A.
	 */
	public NodeId lowPortal(int A) {
		if (A == areas.size() - 1) return null ;
		return portals.get(A).lowPortal ;
	}
	
	/**
	 * Return the high-portal of the next area, to which we will travel to
	 * if we take the A's low-portal.
	 */
	public NodeId connectedHighPortal(int A) {
		if (A == areas.size() - 1) return null ;
		return portals.get(A).highPortal ;
	}
	
	/**
	 * Return the high-portal of the given area A (this is the portal we travel to
	 * from the low-portal of the area previous to A).
	 */
	public NodeId highPortal(int A) {
		if (A == 0) return null ;
		return portals.get(A-1).highPortal ;
	}
	
	/**
	 * Return the low-portal of the previous area, to which we will travel from
	 * to teleport to A.
	 */
	public NodeId connectedLowPortal(int A) {
		if (A == areas.size() - 1) return null ;
		return portals.get(A).lowPortal ;
	}



	@Override
	public Iterable<Pair<Integer, NodeId>> neighbours(Pair<Integer, NodeId> id) {
		int areaId = id.fst ;
		NodeId nodeSrc = id.snd ;
		var area = areas.get(areaId) ;
		List<Pair<Integer,NodeId>> NS = new LinkedList<>() ;
		for(var n : area.neighbours(nodeSrc)) {
			NS.add(new Pair<>(areaId,n)) ;
		}
		// the case when id is a portal, add the opposite portal as neighbor,
		// if the portals are not blocked:
		Integer otherArea = null ;
		NodeId otherPortal = null ;
		if (nodeSrc.equals(lowPortal(areaId))) {
			otherArea = areaId+1 ;
			otherPortal = connectedHighPortal(areaId) ;		
		}
		else if (nodeSrc.equals(highPortal(areaId))){
			otherArea = areaId-1 ;
			otherPortal = connectedLowPortal(areaId) ;		
		}
		if (otherPortal != null) {
			Nav area2 = areas.get(otherArea) ;
			if(!area.isBlocking(nodeSrc) && !area2.isBlocking(otherPortal)) {
				if (perfect_memory_pathfinding 
					|| area2.hasbeenSeen(otherPortal)
						) {
					NS.add(new Pair<>(otherArea,otherPortal)) ;
				}
			}
		}
		return NS;
	}
	

	@Override
	public float heuristic(Pair<Integer, NodeId> from, Pair<Integer, NodeId> to) {
		int area1Id = from.fst ;
		int area2Id = to.fst ;
		Nav area1 = areas.get(area1Id) ;
		NodeId nd1 = from.snd ;
		NodeId nd2 = to.snd ;
		
		if (area1Id == area2Id) {
			return area1.heuristic(nd1, nd2) ;
		}
		else if (area1Id < area2Id) {
			NodeId area1Portal = highPortal(area1Id) ;
			NodeId area2Portal = lowPortal(area2Id) ;
			Nav area2 = areas.get(area2Id) ;
			float dist = 0 ;
			dist += area1.distance(nd1,area1Portal) ;
			dist += distanceBetweenTwoSidesOfPortal ;
			dist += heuristicDistanceBetweenAdjacentAreas*(area2Id - area1Id - 1) ;
			dist += area2.distance(area2Portal, nd2) ;
			return dist ;	
		}
		else { // area1Id > area2Id 
			NodeId area1Portal = lowPortal(area1Id) ;
			NodeId area2Portal = highPortal(area2Id) ;
			Nav area2 = areas.get(area2Id) ;
			float dist = 0 ;
			dist += area1.distance(nd1,area1Portal) ;
			dist += distanceBetweenTwoSidesOfPortal ;
			dist += heuristicDistanceBetweenAdjacentAreas*(area1Id - area2Id - 1) ;
			dist += area2.distance(area2Portal, nd2) ;
			return dist ;
		}
	}

	@Override
	public float distance(Pair<Integer, NodeId> from, Pair<Integer, NodeId> to) {
		var area1 = areas.get(from.fst) ;
		if (from.fst.equals(to.fst)) {
			return area1.distance(from.snd, to.snd) ;
		}
		// else "from" and "to" must be a pair of portals:
		return distanceBetweenTwoSidesOfPortal ;
	}

	@Override
	public List<Pair<Integer, NodeId>> findPath(Pair<Integer, NodeId> from, Pair<Integer, NodeId> to) {
		int area1_id = from.fst ;
		int areaN_id = to.fst ;
		if (areaN_id == area1_id) {
			var path = areas.get(area1_id).findPath(from.snd, to.snd) ;
			if (path == null) return null ;
			return path.stream().map(nd -> new Pair<>(area1_id,nd)).collect(Collectors.toList()) ;
		}

		List<Pair<Integer, NodeId>>  path = new LinkedList<>() ;
		
		NodeId nd0 = from.snd ;
		NodeId ndNext = null ;

		boolean we_go_up = areaN_id > area1_id ;
		int increment = we_go_up ? 1 : -1 ;
		
		if (areaN_id > area1_id) {
			// going up:
			for (var L = area1_id; L != areaN_id; L = L+increment) 
			{
				ndNext = lowPortal(L) ;
				var pathNext = areas.get(L).findPath(nd0, ndNext) ;
				if (pathNext == null) return null ;
				int areaId = L ;
				List<Pair<Integer, NodeId>> pathNext_ = pathNext.stream()
						.map(nd -> new Pair<>(areaId,nd))
						.collect(Collectors.toList()) ;
				path.addAll(pathNext_) ;
				NodeId connectedPortal = connectedHighPortal(L) ;
				nd0 = connectedPortal ;
			}
			// still have to do the last area:
			NodeId ndFinal = to.snd ;
			var pathNext = areas.get(areaN_id).findPath(nd0, ndFinal) ;
			if (pathNext == null)
				return null ;
			List<Pair<Integer, NodeId>> pathNext_ = pathNext.stream()
					.map(nd -> new Pair<>(areaN_id,nd))
					.collect(Collectors.toList()) ;
			path.addAll(pathNext_) ;
			return path ;
		}
		// else we go down:
		
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void addObstacle(Pair<AreaId, NodeId> o) {
		areas.get(o.fst).addObstacle(o.snd);
	}


	@Override
	public void removeObstacle(Pair<AreaId, NodeId> o) {
		areas.get(o.fst).removeObstacle(o.snd);	
	}


	@Override
	public void toggleBlockingOn(Pair<AreaId, NodeId> o) {
		areas.get(o.fst).toggleBlockingOn(o.snd);		
	}


	@Override
	public void toggleBlockingOff(Pair<AreaId, NodeId> o) {
		areas.get(o.fst).toggleBlockingOff(o.snd);		
	}

	@Override
	public boolean hasbeenSeen(Pair<AreaId, NodeId> nd) {
		return areas.get(nd.fst).hasbeenSeen(nd.snd);		
	}


	@Override
	public void markAsSeen(Pair<Integer, NodeId> id) {
		areas.get(id.fst).hasbeenSeen(id.snd);
		var areaId = id.fst ;
		var nd = id.snd ;
		// if id is a portal, we also mark the otherside as seen:	
		if (nd.equals(lowPortal(areaId))) {
			int otherArea = areaId+1 ;
			var area2 = areas.get(otherArea) ;
			NodeId otherPortal = connectedHighPortal(areaId) ;	
			area2.markAsSeen(otherPortal);
		}
		else if (nd.equals(highPortal(areaId))){
			int otherArea = areaId-1 ;
			var area2 = areas.get(otherArea) ;
			NodeId otherPortal = connectedLowPortal(areaId) ;		
			area2.markAsSeen(otherPortal);
		}
		throw new IllegalArgumentException() ;
	}


	@Override
	public List<Pair<AreaId, NodeId>> getFrontier() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<Pair<AreaId, NodeId>> explore(Pair<AreaId, NodeId> startNode) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void wipeOutMemory() {
		for(var nav : areas.values()) {
			nav.wipeOutMemory();
		}
	}


	@Override
	public boolean usingPerfectMemoryPathfinding() {
		return perfect_memory_pathfinding ;
	}


	@Override
	public void setPerfectMemoryPathfinding(Boolean flag) {
		perfect_memory_pathfinding = flag ;
		for(var nav : areas.values()) {
			nav.setPerfectMemoryPathfinding(flag);
		}	
	}
	


}
