package nl.uu.cs.aplib.MainConcepts;

import java.util.function.*;

import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;

public class Action {
	
	String name ;
	public String desc ;
	public double budget = Integer.MAX_VALUE ;
	boolean completed = false ;
	
	Predicate<SimpleState> guard = s -> true;
	Function<SimpleState,Function<Action,Object>> action ;
	
	Action(){ }
	public Action(String name) {this.name = name ; }
	
	// === fluent interface start
	public Action desc(String desc) { this.desc = desc ; return this ; }
	
	public Action on__(Predicate<SimpleState> guard) { this.guard = guard ; return this ; }
	public <AgentSt> Action on_(Predicate<AgentSt> guard) { 
		return on__(st -> guard.test((AgentSt) st)) ;
	}
	
	public Action do__(Function<SimpleState,Function<Action,Object>> action) {
		this.action = action ; return this ;
	}
	public <AgentSt,T> Action do_(Function<AgentSt,Function<Action,T>> action) {
		return do__(s -> y -> action.apply((AgentSt) s).apply(y)) ;
	}
	
	public Action do1__(Function<SimpleState,Function<Action,Object>> action) {
		this.action = s -> y -> { 
			try { return action.apply(s).apply(y) ; }
			finally { y.completed = true  ;}
		} ;
		return this ;
	}
	
	public <AgentSt,T> Action do1_(Function<AgentSt,Function<Action,T>> action) {
		return do1__(s -> y -> action.apply((AgentSt) s).apply(y)) ;
	}
	
	public Action withBudget(Double budget) { this.budget = budget ; return this ; }
	
	public PrimitiveStrategy lift() {
		return new PrimitiveStrategy(this) ;
	}
	
	// === fluent interface end
	
	public boolean isCompleted() { return completed ; }
	public void markAsCompleted() { completed = true ; }
	
	public boolean isEnabled(SimpleState agentstate) { 
		return guard.test(agentstate)  ; 
	}
	
	public Object exec1(SimpleState agentstate) {
		Object proposal = action.apply(agentstate).apply(this) ;
		return proposal ;
	}
	
	static public class Abort extends Action {
		public Abort() { }
	}


}
