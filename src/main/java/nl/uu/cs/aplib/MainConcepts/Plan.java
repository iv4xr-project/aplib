package nl.uu.cs.aplib.MainConcepts;

import java.util.function.*;

public class Plan {
	
	String name ;
	public String desc ;
	public double budget ; 
	boolean completed = false ;
	ProgressStatus status = ProgressStatus.INPROGRESS ;
	
	Predicate<AbstractState> guard ;
	Function<AbstractState,Function<Plan,Object>> action ;
	
	public Plan(String name, double budget, 
			  Predicate<AbstractState> guard,  
			  Function<AbstractState,Function<Plan,Object>> action) {
		this.name = name ;
		this.budget = budget ;
		this.guard = guard ;
		this.action = action ;
	}
	
	public boolean isCompleted() { return completed ; }
	public void markAsCompleted() { completed = true ; }
	
	public boolean isEnabled(AbstractState agentstate) { 
		return guard.test(agentstate)  ; 
	}
	
	public Object exec1(AbstractState agentstate) {
		Object proposal = action.apply(agentstate).apply(this) ;
		return proposal ;
	}


}
