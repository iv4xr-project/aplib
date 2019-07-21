package nl.uu.cs.aplib.MainConcepts;

import java.util.function.*;

public class Goal {
	
	String name ;
	public String desc ;
	public double budget ;
	public ProgressStatus status = ProgressStatus.INPROGRESS ;
	Double distance = null ;
	Object proposal ;
	
	Predicate checkPredicate ;
	ToDoubleFunction distFunction ;
	
	public Goal(String name, double budget, 
			    Predicate checkP,  
			    ToDoubleFunction distF) {
		this.name = name ;
		this.budget = budget ;
		this.checkPredicate = checkP ;
		this.distFunction = distF ;
	}
	
	public String getName() { return name ; }
	public Object getProposal() { return proposal ; }
	public Object getSolution() {
		if (status == ProgressStatus.SUCCESS) return proposal ; else return null ;
	}
	
	public void setStatusToFail() { status = ProgressStatus.FAILED ; }
	
	public void submitProposal(Object proposal) {
		if (proposal == null) return ;
		if(checkPredicate.test(proposal)) status = ProgressStatus.SUCCESS ;
		if (distFunction != null) distance = distFunction.applyAsDouble(proposal) ;
	}

	public ProgressStatus getStatus() { return status ; }
	public Double distance() { return distance ; }
	

}
