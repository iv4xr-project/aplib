package eu.iv4xr.framework.extensions.mbt;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.*;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.agents.State;

/** 
 * Representing actual execution of a transitions.
 */
public class MBTAction<S extends State> {
	
	public String name ;
	
	/**
	 * The function that implements this action. It should returns true if
	 * the execution was successful, and else false.
	 */
	public Function<TestAgent,Boolean> theAction ;
	
	public List<Predicate<S>> guards = new LinkedList<>() ;
	
	public List<MBTPostCondition<S>> postConditions = new LinkedList<>() ;
	
	public MBTAction(String name) { this.name = name ; }
	
	@SuppressWarnings("unchecked")
	public MBTAction<S> addGuards(Predicate<S> ... ps) {
		for (Predicate<S>  g : ps) {
			guards.add(g) ;
		}
		return this ;
	}
	
	@SuppressWarnings("unchecked")
	public MBTAction<S> addPostConds(MBTPostCondition<S> ... ps) {
		for (MBTPostCondition<S>  g : ps) {
			postConditions.add(g) ;
		}
		return this ;
	}
	
	public MBTAction<S>  withAction(Function<TestAgent,Boolean> a) {
		this.theAction = a ;
		return this ;
	}
	
	public boolean enabled(S state) {
		return guards.stream().allMatch(g -> g.test(state)) ;
	}
	

}
