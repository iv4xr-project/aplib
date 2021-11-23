package nl.uu.cs.aplib.mainConcepts;

import java.util.function.*;

import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;

/**
 * An instance of this class represents a goal that can be given to an agent (an
 * instance of {@link BasicAgent} or its subclasses). To be more precise, an
 * agent expects a {@link GoalStructure}, and a {@code Goal} is the
 * simplest/primitive form a {@link GoalStructure}. Note that when a
 * {@code Goal} is given to an agent, the agent will expect the the goal to be
 * accompanied with a {@link Tactic} which the agent will then use to solve the
 * goal. Use the method {@code withTactic} to attach a {@link Tactic} to a
 * {@code Goal}.
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

    String name;
    public String desc;

    ProgressStatus status = new ProgressStatus();
    Tactic tactic;

    /**
     * Minimum abs-distance for a proposal to be accepted as a solution. Currently
     * set to be 0.005.
     */
    Double epsilon = 0.005;

    Double distance = null;

    Object proposal;

    Function<Object, Double> checkPredicate;

    /**
     * Create a blank instance of a Goal. It will have no actual predicate as the
     * actual goal.
     * 
     * @param name A name to be associated with the goal.
     */
    public Goal(String name) {
        this.name = name;
    }

    /**
     * Set the value of eplison. The method returns this Goal itself so that it can
     * be used in the Fluent Interface style.
     */
    public Goal withEpsilon(Double e) {
        if (epsilon < 0)
            throw new IllegalArgumentException();
        epsilon = e;
        return this;
    }

    /**
     * Set the predicate which would serve as the predicate to solve. The method
     * returns this Goal itself so that it can be used in the Fluent Interface
     * style.
     */
    public Goal toSolve_(Predicate predicateToSolve) {
        checkPredicate = o -> predicateToSolve.test(o) ? 0.0 : 1;
        return this;
    }

    /**
     * Set the given function as a goal function. A proposal o is a solution if
     * abs(goalfunction(o)) is a value less than epsilon (default is 0.005).
     * 
     * @param goalfunction the goal function to solve.
     * @return The method returns this Goal itself so that it can be used in the
     *         Fluent Interface style.
     */
    public Goal ftoSolve_(Function<Object, Double> goalfunction) {
        checkPredicate = goalfunction;
        return this;
    }

    /**
     * Set the predicate which would serve as the predicate to solve. The more
     * general typing of the method's signature is for convenience, to allow you to
     * explicitly specify the type of the goal's proposals domain at the point where
     * this method is called, e.g. as in:
     * 
     * <pre>
     * Goal g = new Goal().toSolve((Integer x) -> x == 9999);
     * </pre>
     * 
     * The method returns this Goal itself so that it can be used in the Fluent
     * Interface style.
     */
    public <Proposal> Goal toSolve(Predicate<Proposal> predicateToSolve) {
        return toSolve_(p -> predicateToSolve.test((Proposal) p));
    }

    /**
     * Set the given function as a goal function. A proposal o is a solution if
     * abs(goalfunction(o)) is a value less than epsilon (default is 0.005). The
     * more general typing of the method's signature is for convenience, to allow
     * you to explicitly specify the type of the goal's proposals domain at the
     * point where this method is called, e.g. as in:
     * 
     * <pre>
     * Goal g = new Goal().ftoSolve((Integer x) -> x - 9999);
     * </pre>
     * 
     * The method returns this Goal itself so that it can be used in the Fluent
     * Interface style.
     */
    public <Proposal> Goal ftoSolve(Function<Proposal, Double> predicateToSolve) {
        return ftoSolve_(p -> predicateToSolve.apply((Proposal) p));
    }

    /**
     * Set the strategy to that a solving agent can use to solve this goal. The
     * method returns this Goal itself so that it can be used in the Fluent
     * Interface style.
     */
    public Goal withTactic(Tactic S) {
        tactic = S;
        return this;
    }

    public Tactic getTactic() {
        return tactic;
    }

    // public Goal withAction(Action a) {
    // return withStrategy(a.lift()) ;
    // }

    /**
     * Check if a given proposal satisfied this goal (so, if the proposal is a
     * solution). If the proposal is indeed a solution the status of this goal will
     * be set to SUCCESS.
     * 
     * <p>
     * A null proposal is always rejected.
     */
    final public void propose(Object proposal) {
        if (proposal == null)
            return;
        this.proposal = proposal;
        distance = checkPredicate.apply(proposal);
        if (Math.abs(distance) <= epsilon)
            status.setToSuccess();
    }

    /**
     * This is the version of {@link #propose(Object)} that will be called
     * internally by {@link BasicAgent#updateWorker()}. Override this if its
     * behavior need to be extended.
     */
    protected void propose_(Object proposal) {
        propose(proposal);
    }

    public String getName() {
        return name;
    }

    /**
     * Return the last non-null proposal that was tested by this goal (see the
     * method {@link Goal#propose_(Object)}).
     */
    public Object getProposal() {
        return proposal;
    }

    /**
     * Return the last proposal that was tested by this goal (see the method
     * {@link Goal#propose_(Object)}) that turned out to be a solution. If there was
     * none, null is returned.
     */
    public Object getSolution() {
        if (status.success())
            return proposal;
        else
            return null;
    }

    /**
     * To lift this goal to become an instance of {@link GoalStructure}. Conceptually, a
     * Goal is a simplest type of {@link GoalStructure}. Technically, to be more precise,
     * Goal is not a subclass of {@link GoalStructure}. The corresponding representation
     * of Goal as a GoalStructure is called {@link PrimitiveGoal}, which is a subclass of
     * {@link GoalStructure}. So, this method will lift this goal to a
     * {@link GoalStructure.PrimitiveGoal}.
     * 
     * @return A new instance of {@link PrimitiveGoal} that wrap around this goal.
     */
    public PrimitiveGoal lift() {
        return new PrimitiveGoal(this);
    }

    /**
     * Return the progress status of this goal (INPROGRESS, SUCCESS, or FAILED).
     */
    public ProgressStatus getStatus() {
        return status;
    }

    /**
     * If a distance function has been specified for this goal, this will return the
     * absolute distance between the last proposed and non-null proposal to being a
     * solution.
     */
    public Double distance() {
        return distance;
    }

}
