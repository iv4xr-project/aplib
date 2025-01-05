package eu.iv4xr.framework.extensions.mbt;

import java.util.function.Predicate;

import nl.uu.cs.aplib.agents.State;

public class MBTPostCondition<S extends State> {
	
	public String name ;
	public String desc ;
	public Predicate<S> P ;
	
	public MBTPostCondition(String name, Predicate<S> P) {
		this.name = name ;
		this.P = P ;
	}
	
	public MBTPostCondition<S> withName(String name) {
		this.name = name ;
		return this ;
	}
	
	public static <X extends State> MBTPostCondition<X> postConditionPredicate(Predicate<X> P) {
		return new MBTPostCondition<>(null,P) ;
 	}

}
