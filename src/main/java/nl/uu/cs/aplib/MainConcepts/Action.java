package nl.uu.cs.aplib.MainConcepts;

import java.util.function.*;

public class Action {
	
	String name ;
	public String desc ;
	public double budget = Integer.MAX_VALUE ;
	boolean completed = false ;
	ProgressStatus status = ProgressStatus.INPROGRESS ;
	
	Predicate<SimpleState> guard ;
	Function<SimpleState,Function<Action,Object>> action ;
	
	Action(){}
	public Action(String name) {this.name = name ; }
	
	public Action desc(String desc) { this.desc = desc ; return this ; }
	public Action on_(Predicate<SimpleState> guard) { this.guard = guard ; return this ; }
	public Action do_(Function<SimpleState,Function<Action,Object>> action) {
		this.action = action ; return this ;
	}
	public Action withBudget(Double budget) { this.budget = budget ; return this ; }
	
	
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
