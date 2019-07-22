package nl.uu.cs.aplib.MainConcepts;

import java.util.function.*;

public class Goal {
	
	String name ;
	public String desc ;
	public double budget = Integer.MAX_VALUE ;
	public ProgressStatus status = ProgressStatus.INPROGRESS ;
	Strategy strategy ;
	Double distance = null ;
	Object proposal ;
	
	Predicate checkPredicate ;
	ToDoubleFunction distFunction ;
	
	public Goal(String name) {
		this.name = name ;
	}
	
	public Goal withBudget(Double budget) {
		if (budget<0) throw new IllegalArgumentException() ;
		this.budget = budget ;
		return this ;
	}
	
	/**
	 * Set the predicate which would serve as the predicate to solve.
	 */
	public Goal toSolve(Predicate predicateToSolve) {
		checkPredicate = predicateToSolve ; return this ;
	}
	
	/**
	 * Set the used distance function.
	 */
	public Goal withDistF(ToDoubleFunction f) {
		distFunction = f ; return this ;
	}
	
	/**
	 * Set the strategy to use to solve this goal.
	 */
	public Goal withStrategy(Strategy S) {
		strategy = S ; return this ;
	}
	
	public String getName() { return name ; }
	public Object getProposal() { return proposal ; }
	public Object getSolution() {
		if (status == ProgressStatus.SUCCESS) return proposal ; else return null ;
	}
	
	public Goal setStatusToFail() { status = ProgressStatus.FAILED ; return this ; }
	Goal setStatusToSuccess() { status = ProgressStatus.SUCCESS ; return this ; }
	
	public void propose(Object proposal) {
		if (proposal == null) return ;
		if(checkPredicate.test(proposal)) status = ProgressStatus.SUCCESS ;
		if (distFunction != null) distance = distFunction.applyAsDouble(proposal) ;
	}

	public ProgressStatus getStatus() { return status ; }
	public Double distance() { return distance ; }
	

}
