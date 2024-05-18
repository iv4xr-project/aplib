package eu.iv4xr.framework.extensions.ltl;

import java.util.* ;

/**
 * A data structure to keep track of visited states in bounded model checking.
 * It allows a use-hash-only mode, where only the hashes of the state will be
 * stored. This is unsound for checking membership, but it takes less memory.
 */
public class StateSet<State> {

	public boolean useHashInsteadOfExplicitState = false ;
	
	/**
	 * When a state s is visited at depth k, we store this information in this map
	 * as the mapping (s,k). If we don't want to keep track of the depth,
	 * just give some depth, e.g. 0.
	 */
	Map<State,Integer> states = new HashMap<>() ;
	
	/**
	 * When a state s is visited at depth k, we store this information in this map
	 * as the mapping (hash(s),k). Notice that only the hash of s will be stored.
	 * If we don't want to keep track of the depth,
	 * just give some depth, e.g. 0.
	 */
	Map<Integer,Integer> hashes = new HashMap<>() ;
	

	
	public StateSet() { } 
	
	public int size() {
		if (useHashInsteadOfExplicitState) {
			return hashes.size() ;
		}
		else {
			return states.size() ;
		}
	}
	
	public void put(State s, int depth) {
		if (useHashInsteadOfExplicitState) {
			hashes.put(s.hashCode(),depth) ;
		}
		else {
			states.put(s,depth) ;
		}
	}
	
	public int getDepth(State s) {
		if (useHashInsteadOfExplicitState) {
			return hashes.get(s.hashCode()) ;
		}
		else {
			return states.get(s) ;
		}
	}
	
	public boolean contains(State s) {
		if (useHashInsteadOfExplicitState) {
			return hashes.get(s.hashCode()) != null ;
		}
		else {
			return states.get(s) != null ;
		}
	}
	
	public void remove(State s) {
		if (useHashInsteadOfExplicitState) {
			hashes.remove(s.hashCode()) ;
		}
		else {
			states.remove(s) ;
		}
	}
	
	public void clear() {
		hashes.clear();
		states.clear();
	}
}
