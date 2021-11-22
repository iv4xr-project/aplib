package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Predicate;

public class Buchi {
	
	public Set<String> states = new HashSet<>() ;

	Map<String,Integer> encoder ;
	String[] decoder ;
	
	public Set<Integer> initialStates = new HashSet<>() ;
	public Set<Integer> omegaAcceptingStates = new HashSet<>() ;
	
	public static class BuchiTransition implements ITransition {
		
		public String id ;
		public Predicate<IExplorableState> condition ;
		
		@Override
		public String getId() {
			return id;
		}
		
	}
	

}
