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
			Nav extends XPathfinder<NodeId> & CanDealWithDynamicObstacle<NodeId>> 
		implements 
		XPathfinder<Pair<Integer,NodeId>>,
		CanDealWithDynamicObstacle<Pair<Integer,NodeId>> {
	
	public List<Nav> areas = new LinkedList<>() ;
		
	/**
	 * Portals between areas. If P is the i-th Portal in the list, this 
	 * P connects area i to i+1. P.lowPortal is a node in area i, and
	 * P.highPortal is a node in area i+1. From P.lowPortal we can travel
	 * to P.highPortal (which is in area i+1).
	 * 
	 * <p>Note that if there are n areas, we will only have n-1 portals, as
	 * the last area have no next-area to connect to.
	 */ 
	public List<Portal<NodeId>> portals = new LinkedList<>() ;
	
	boolean perfect_memory_pathfinding = false;

	
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
	
	/**
	 * Add a new area with index a, where a is the current number of areas
	 * we have. The new area is connected to the area a-1 through the given
	 * pair of portals, where portalLow is in the area a-1 and portalHigh
	 * is in the area a. 
	 * 
	 * <p>Note that for the first area added, the specified portals are ignored (so they
	 * can just be null) because we don't have a previous portal yet.
	 *
	 *  @param isOpen indicate whether the added portal is set to be open or close.
	 */
	public void addNextArea(Nav area, NodeId portalLow, NodeId portalHigh, boolean isOpen) {
		System.out.println("area size before; " + areas.size()  );
		areas.add(area) ;
		System.out.println("area size after; " + areas.size() );
		if (areas.size() == 1) {
			// this is the first area, so we don't do portals yet:
			return ;
		}
		area.setPerfectMemoryPathfinding(perfect_memory_pathfinding);
		var portal = new Portal<NodeId>(portalLow,portalHigh) ;
		portal.isOpen = isOpen ;
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
		if (A == 0) return null ;
		return portals.get(A-1).lowPortal ;
	}
	
	/**
	 * Set the portal between area a1 and a2 to open or close.
	 * 
	 * @param isOpen If true will open the portal, else close it.
	 */
	public void setPortal(int areaFrom, int areaTo, boolean isOpen) {
		if(areaTo == areaFrom+1) {
			portals.get(areaFrom).isOpen = isOpen ;
			return ;
		}
		if (areaTo == areaFrom-1) {
			portals.get(areaTo).isOpen = isOpen ;
			return ;
		}
		throw new IllegalArgumentException() ;
	}


	@Override
	public List<Pair<Integer, NodeId>> findPath(Pair<Integer, NodeId> from, Pair<Integer, NodeId> to) {
		int area1_id = from.fst;
		int areaN_id = to.fst;
		if (areaN_id == area1_id) {
			var path = areas.get(area1_id).findPath(from.snd, to.snd);
			if (path == null)
				return null;
			return path.stream().map(nd -> new Pair<>(area1_id, nd)).collect(Collectors.toList());
		}

		List<Pair<Integer, NodeId>> path = new LinkedList<>();

		NodeId nd0 = from.snd;
		NodeId ndNext = null;

		boolean we_go_up = areaN_id > area1_id;
		int increment = we_go_up ? 1 : -1;

		for (var L = area1_id; L != areaN_id; L = L + increment) {
			Portal po = null ;
			if (we_go_up) {
				po = portals.get(L) ;
				ndNext = lowPortal(L);

			}
			else {
				po = portals.get(L-1) ;
				ndNext = highPortal(L);
			}
			if (! po.isOpen) {
				// the portal is closed!
				return null ;
			}
			var pathNext = areas.get(L).findPath(nd0, ndNext);
			if (pathNext == null)
				return null;
			int areaId = L;
			List<Pair<Integer, NodeId>> pathNext_ = pathNext.stream()
					. map(nd -> new Pair<>(areaId, nd))
					. collect(Collectors.toList());
			path.addAll(pathNext_);
			if (we_go_up) {
				NodeId connectedPortal = connectedHighPortal(L);
				nd0 = connectedPortal;				
			}
			else {
				NodeId connectedPortal = connectedLowPortal(L);
				nd0 = connectedPortal;	
			}
		}
		// still have to do the last area:
		NodeId ndFinal = to.snd;
		var pathNext = areas.get(areaN_id).findPath(nd0, ndFinal);
		if (pathNext == null)
			return null;
		List<Pair<Integer, NodeId>> pathNext_ = pathNext.stream().map(nd -> new Pair<>(areaN_id, nd))
				.collect(Collectors.toList());
		path.addAll(pathNext_);
		return path;
	}


	@Override
	public void addObstacle(Pair<Integer, NodeId> o) {
		areas.get(o.fst).addObstacle(o.snd);	
	}


	@Override
	public void removeObstacle(Pair<Integer, NodeId> o) {
		areas.get(o.fst).removeObstacle(o.snd);	
	}

	@Override
	public void setBlockingState(Pair<Integer, NodeId> o, boolean isBlocking) {
		//System.out.println(">>>> trying to switch blocking state of: " + o) ;
		int area = o.fst ;
		//System.out.println(">>>> low portal of: " + area + ": " + lowPortal(area)) ;
		//System.out.println(">>>> high portal of: " + area + ": " + highPortal(area)) ;
		var nd = o.snd ;
		areas.get(area).setBlockingState(nd,isBlocking);	
		/*
		 should not do this here --> responsibility of the user class.
		 
		if (nd.equals(lowPortal(area))) {
			//System.out.println(">>>> swicthing low-portal " + area + " to " + !isBlocking) ;
			setPortal(area,area+1, ! isBlocking) ;
			return ;
		}
		if (nd.equals(highPortal(area))) {
			//System.out.println(">>>> swicthing high-portal " + area + " to " + !isBlocking) ;
			setPortal(area,area-1, ! isBlocking) ;
			return ;
		}
		*/
	}
	
	@Override
	public void markAsSeen(Pair<Integer, NodeId> ndx) {
	//	System.out.println(">>> area size " + areas.size() + "ndx.fst" + ndx.fst);
		areas.get(ndx.fst).markAsSeen(ndx.snd);
	//	System.out.println(">>> registering maze " + ndx.fst + ", tile " + ndx.snd) ;
		
		var areaId = ndx.fst ;
		var nd = ndx.snd ;
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
		//System.out.println(">>> registering maze end" ) ;
	}
	
	@Override
	public boolean hasbeenSeen(Pair<Integer, NodeId> ndx) {
		return areas.get(ndx.fst).hasbeenSeen(ndx.snd) ;
	}


	@Override
	public List<Pair<Integer,NodeId>> getFrontier() {
		List<Pair<Integer,NodeId>> frontiers = new LinkedList<>() ;
		for (int a=0; a<areas.size(); a++) {
			var nav = areas.get(a) ;
			Integer a_ = a ;
			var fr = nav.getFrontier().stream().map(nd -> new Pair<>(a_,nd)).collect(Collectors.toList()) ;
			//System.out.println("xxxx calling getFrontier of map " + a_) ;
			frontiers.addAll(fr) ;
		}
		return frontiers ;
	}


	@Override
	public List<Pair<Integer, NodeId>> explore(Pair<Integer, NodeId> startNode, Pair<Integer, NodeId> heuristicNode) {
		
		int startArea = startNode.fst ;
		int heuristicArea = heuristicNode.fst ;
		
		if (startArea == heuristicArea) {
			var nav = areas.get(startArea) ;
			List<NodeId> path = nav.explore(startNode.snd, heuristicNode.snd) ;
			if (path != null && !path.isEmpty()) 
				return path.stream().map(nd -> new Pair<Integer, NodeId>(startArea,nd)).collect(Collectors.toList()) ;
		}
		
		var candidates = getFrontier() ;
		if (candidates.size() == 0) 
			return null ;
		
		var candidates2 = candidates.stream()
			.map(c -> new Pair<>(c, findPath(startNode,c)))
			.filter(d -> d.snd != null)
			.collect(Collectors.toList()) ;
		
		if (candidates2.size() ==0)
			return null ;
			
		candidates2.sort((d1,d2) -> Integer.compare(
				distanceCandidate(d1,heuristicNode), 
				distanceCandidate(d2,heuristicNode)))  ;
		
		//System.out.println(">>> LayeredAread.exaplore(), candidates: " + candidates2) ;
		
		return candidates2.get(0).snd ;
	}
	
	private int distanceCandidate(
			Pair<Pair<Integer,NodeId>, List<Pair<Integer,NodeId>>> candidate,
			Pair<Integer,NodeId> heuristicNode) {
		
		int c = candidate.fst.fst ;
		
		if (c == heuristicNode.fst) {
			// candidates from the same area;
			return candidate.snd.size() ;
		}
		return Math.abs(c - heuristicNode.fst)*20000 + candidate.snd.size() ;
	}


	@Override
	public void wipeOutMemory() {
		for(var nav : areas) {
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
		for(var nav : areas) {
			nav.setPerfectMemoryPathfinding(flag);
		}	
	}


	@Override
	public boolean isBlocking(Pair<Integer, NodeId> o) {
		var nav = areas.get(o.fst) ;
		return nav.isBlocking(o.snd) ;
	}
	
	@Override
	public String toString() {
		StringBuffer z = new StringBuffer() ;
		for (int a=0; a<areas.size(); a++) {
			if (a>0) z.append("\n") ;
			var nav = areas.get(a) ;
			z.append("=== Area " + a) ;
			if (a < areas.size() -1) {
				var po = portals.get(a) ;
				char open = po.isOpen?'o' : 'x' ;
				if (nav.hasbeenSeen(po.lowPortal))
					open = po.isOpen?'O' : 'X' ;
				
				z.append(", portal: " + po.lowPortal + " --" +  open + "-> " + po.highPortal) ;				
			}
			
			z.append("\n" + nav) ;	
		}
		return z.toString() ;
	}


}
