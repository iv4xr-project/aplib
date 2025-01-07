package eu.iv4xr.framework.extensions.mbt;

import java.util.*;
import java.util.function.Predicate;

import nl.uu.cs.aplib.agents.State;

public class MBTState<S extends State> {
	
	public String id ;
	
	/**
	 * A set of predicates representing SUT's concrete states that
	 * belong to this MBTState. More precisely, the set Z of concrete
	 * states that is represented consists of those concrete states on
	 * which every predicate in preds below is true.
	 */
	public List<Predicate<S>> preds = new LinkedList<>() ;
		
	public MBTState(String id) {
		this.id = id ;
	}
	
	@SuppressWarnings("unchecked")
	public MBTState<S> addPredicates(Predicate<S> ... ps) {
		for (Predicate<S>  g : ps) {
			preds.add(g) ;
		}
		return this ;
	}
	
	public boolean inState(S state) {
		return preds.stream().allMatch(P -> P.test(state)) ;
	}

}
