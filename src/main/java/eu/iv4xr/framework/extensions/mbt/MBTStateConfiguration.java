package eu.iv4xr.framework.extensions.mbt;

import java.util.* ;

public class MBTStateConfiguration {
	
	/**
	 * The set of states that belong to this configuration. It should be
	 * kept sorted too.
	 */
	public List<String> states ;
	
	public MBTStateConfiguration(List<String> states) {
		this.states = new LinkedList<>() ;
		this.states.addAll(states) ;
		this.states.sort((s1,s2) -> s1.compareTo(s2));
	}
	
	@Override
	public int hashCode() {
		return states.hashCode() ; 
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MBTStateConfiguration) {
			var v = (MBTStateConfiguration) o ;
			return this.states.equals(v.states) ;
		}
		return false ;
	}

}
