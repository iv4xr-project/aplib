package nl.uu.cs.aplib.MainConcepts;

import java.util.function.*;

public class Goal {
	
	String name ;
	public String desc ;
	public double budget = Integer.MAX_VALUE ;
	public ProgressStatus status = new ProgressStatus() ;
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
	public Goal toSolve_(Predicate predicateToSolve) {
		checkPredicate = predicateToSolve ; return this ;
	}
	
	public <Proposal> Goal toSolve(Predicate<Proposal> predicateToSolve) {
		return toSolve_(p -> predicateToSolve.test((Proposal) p)) ;
	}
	
	/**
	 * Set the used distance function.
	 */
	public Goal withDistF_(ToDoubleFunction f) {
		distFunction = f ; return this ;
	}

	public <Proposal> Goal withDistF(ToDoubleFunction<Proposal> f) {
		return withDistF_(p -> f.applyAsDouble((Proposal) p)) ;
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
		if (status.sucess()) return proposal ; else return null ;
	}
	
	Goal setStatusToFail(String reason) { status.setToFail(reason) ; return this ; }
	Goal setStatusToSuccess(String reason) { status.setToSuccess(reason); ; return this ; }
	
	public void propose(Object proposal) {
		if (proposal == null) return ;
		if(checkPredicate.test(proposal)) status.setToSuccess(); ;
		if (distFunction != null) distance = distFunction.applyAsDouble(proposal) ;
	}

	public ProgressStatus getStatus() { return status ; }
	public Double distance() { return distance ; }
	

}
