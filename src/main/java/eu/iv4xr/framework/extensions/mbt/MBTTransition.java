package eu.iv4xr.framework.extensions.mbt;

import java.util.*;

/**
 * Representing transition. Transitions in our setup are non-parameterized.
 * A transition goes from a configuration to another configuration. 
 * Non-determinism can be represented by multiple transitions with that 
 * same action, and from the same source-config, but goes to multiple
 * different dest-configs.
 */
public class MBTTransition {
	
	public MBTStateConfiguration src ;
	public MBTStateConfiguration dest ;
	public String action ;
	
	public MBTTransition(MBTStateConfiguration src, String action, MBTStateConfiguration dest) {
		this.src = src ;
		this.action = action ;
		this.dest = dest ;
	}
		
	@Override
	public int hashCode() {
		var z = new StringBuffer() ;
		z.append(action) ;
		for (var s : src.states) z.append(s) ;
		for (var t : dest.states) z.append(t) ;
		return z.toString().hashCode() ;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MBTTransition) {
			var t = (MBTTransition) o ;
			return src.equals(t.src) && dest.equals(t.dest)
					&& action.equals(t.action) ;
		}
		return false ;
	}
	
}
