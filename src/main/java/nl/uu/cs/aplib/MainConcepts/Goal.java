package nl.uu.cs.aplib.MainConcepts;

import java.util.function.*;

import nl.uu.cs.aplib.MainConcepts.GoalTree.PrimitiveGoal;

/**
 * An instance of this class represents a goal that can be given to an agent (an
 * instance of {@link SimpleAgent} or its subclasses). To be more precise, an
 * agent expects a {@link GoalTree}, and a {@code Goal} is the
 * simplest/primitive form a {@link GoalTree}. Note that when a {@code Goal} is
 * given to an agent, the agent will expect the the goal to be accompanied with
 * a {@link Strategy} which the agent will then use to solve the goal. Use the
 * method {@code withStrategy} to attach a {@link Strategy} to a {@code Goal}.
 * 
 * <p>
 * A {@code Goal} is essentially a predicate over some domain. You can imagine
 * it as a domain of 'proposals' to be proposed to the goal. Proposals on which
 * the predicate returns {@code true} are 'solutions' of the goal. When a goal
 * is given to an agent, the task of the agent is to find a solution for the
 * goal.
 * 
 * <p>
 * A goal can specify its estimated minimum computation time budget. The agent
 * solving the goal will <b>try</b> (but not committed to) to allocate at least
 * this amount of total computation time for working on the goal. The
 * computation time of an agent with respect to a goal is defined as the total
 * time the agent spends per invocation of its {@code update()} method while
 * solving the goal. The time that elapses between two invocations of
 * {@code update()} is not counted. If no time budget is specified, then the
 * budget is assumed as infinite.
 * 
 * <p>
 * Optionally, we can attach a distance function to a goal. A distance function
 * is a function from the goal domain of proposals to {@code Double}. It is a
 * measure of how 'far' a given proposal is from being a solution. This
 * information might be exploitable by a strategy when it searches for a
 * solution, e.g. if the strategy implements a search algorithm (hill climbing,
 * evolutionary, etc).
 * 
 * @author wish
 *
 */
public class Goal {
	
	String name ;
	public String desc ;
	double demandedMinimumBudget = 0 ;
	ProgressStatus status = new ProgressStatus() ;
	Strategy strategy ;
	Double distance = null ;
	Object proposal ;
	
	Predicate checkPredicate ;
	ToDoubleFunction distFunction ;
	
	/**
	 * Create a blank instance of a Goal. It will have no goal and no distance function.
	 * 
	 * @param name A name to be associated with the goal.
	 */
	public Goal(String name) {
		this.name = name ;
	}
	
	/**
	 * Set an estimated minimum computation budget that this goal requires. Note that an
	 * agent can try to allocate this budget, but it does not have to.
	 * 
	 * The method returns this Goal itself so that it can be used in the Fluent Interface style.
	 */
	public Goal demandMinimumBudget(double budget) {
		if (budget<=0 || ! Double.isFinite(budget)) throw new IllegalArgumentException() ;
		demandedMinimumBudget = budget ;
		return this ;
	}
	
	/**
	 * Return the agent's estimate required minimum computation budget. The default is
	 * just 0.
	 */
	public double getDemandedMinimumBudget() { return demandedMinimumBudget ; }
	
	/**
	 * Set the predicate which would serve as the predicate to solve.
	 * The method returns this Goal itself so that it can be used in the Fluent Interface style.
	 */
	public Goal toSolve_(Predicate predicateToSolve) {
		checkPredicate = predicateToSolve ; return this ;
	}
	
	/**
	 * Set the predicate which would serve as the predicate to solve. The more general
	 * typing of the method's signature is for convenience, to allow you to explicitly
	 * specify the type of the goal's proposals domain at the point where this method is
	 * called, e.g. as in:
	 * 
	 * <pre>
	 *   Goal g = new Goal() . toSolve((Integer x) -> x==9999) ;
	 * </pre>
	 * 
	 * The method returns this Goal itself so that it can be used in the Fluent Interface style.
	 */
	public <Proposal> Goal toSolve(Predicate<Proposal> predicateToSolve) {
		return toSolve_(p -> predicateToSolve.test((Proposal) p)) ;
	}
	
	/**
	 * Set a distance function to be associated to this goal.
	 * The method returns this Goal itself so that it can be used in the Fluent Interface style.
	 */
	public Goal withDistF_(ToDoubleFunction f) {
		distFunction = f ; return this ;
	}

	/**
	 * Set a distance function to be associated to this goal. The more general
	 * typing of the method's signature is for convenience, to allow you to explicitly
	 * specify the type of the goal's proposals domain at the point where this method is
	 * called, e.g. as in:
	 * 
	 * <pre>
	 *   Goal g = new Goal() . withDistF((Integer x) -> Math.abs(9999 - x)) ;
	 * </pre>
	 * The method returns this Goal itself so that it can be used in the Fluent Interface style.
	 */
	public <Proposal> Goal withDistF(ToDoubleFunction<Proposal> f) {
		return withDistF_(p -> f.applyAsDouble((Proposal) p)) ;
	}

	
	/**
	 * Set the strategy to that a solving agent can use to solve this goal.
	 * The method returns this Goal itself so that it can be used in the Fluent Interface style.
	 */
	public Goal withStrategy(Strategy S) {
		strategy = S ; return this ;
	}

	public Strategy getStrategy() { return strategy  ; }

	//public Goal withAction(Action a) {
	//	return withStrategy(a.lift()) ;
	//}
	
	/**
	 * Check if a given proposal satisfied this goal (so, if the proposal is a solution).
	 * If the proposal is indeed a solution the status of this goal will be set to
	 * SUCCESS.
	 * 
	 * <p>A null proposal is always rejected.
	 */
	public void propose(Object proposal) {
		if (proposal == null) return ;
		if(checkPredicate.test(proposal)) status.setToSuccess(); ;
		if (distFunction != null) distance = distFunction.applyAsDouble(proposal) ;
	}
	
	public String getName() { return name ; }
	
	/**
	 * Return the last non-null proposal that was tested by this goal (see the method {@link Goal#propose(Object)}).
	 */
	public Object getProposal() { return proposal ; }

	/**
	 * Return the last proposal that was tested by this goal (see the method {@link Goal#propose(Object)})
	 * that turned out to be a solution. If there was none, null is returned.
	 */
	public Object getSolution() {
		if (status.sucess()) return proposal ; else return null ;
	}
	
	Goal setStatusToFail(String reason) { status.setToFail(reason) ; return this ; }
	Goal setStatusToSuccess(String reason) { status.setToSuccess(reason); ; return this ; }
	
	/**
	 * To lift this goal to become an instance of {@link Solution}. Conceptually, a Goal is a simplest
	 * type of {@link Solution}. Technically, to be more precise, Goal is not a subclass of
	 * {@link Solution}. The corresponding representation of Goal as a Solution is called
	 * {@link PrimitiveGoal}, which is a subclass of {@link Solution}. So, this method will lift
	 * this goal to a {@link PrimitiveGoal}.
	 * 
	 * @return A new instance of {@link PrimitiveGoal} that wrap around this goal.
	 */
	public PrimitiveGoal lift() {
		return new PrimitiveGoal(this) ;
	}
	
    /**
     * Return the progress status of this goal (INPROGRESS, SUCCESS, or FAILED).
     */
	public ProgressStatus getStatus() { return status ; }
	
	/**
	 * If a distance function has been specified for this goal, this will return
	 * the distance between the last proposed and non-null proposal to being
	 * a solution.
	 */
	public Double distance() { return distance ; }
	

}
