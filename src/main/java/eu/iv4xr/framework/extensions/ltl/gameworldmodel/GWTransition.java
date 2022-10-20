package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import eu.iv4xr.framework.extensions.ltl.ITransition;

/**
 * Representing a transition in {@link GameWorldModel}. The class only
 * hold the transition-type and id. It does not implement the actual
 * semantic of the transition. This is implemented in the {@link GameWorldModel}
 * side.
 */
public class GWTransition implements ITransition {
	
	public enum GWTransitionType { TRAVEL, INTERACT }
	
	public GWTransitionType type ;		
	public String target ;

	public GWTransition(GWTransitionType type, String target) {
		this.type = type ;
		this.target = target ;
		
	}
	
	@Override
	public String getId() {
		switch(type) {
		   case TRAVEL :   return "TVL:" + target ;
		   case INTERACT : return "INT:" + target ;
		}
		throw new IllegalArgumentException() ;
	}
}