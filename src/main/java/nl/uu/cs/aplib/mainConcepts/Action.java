package nl.uu.cs.aplib.mainConcepts;

import java.util.function.*;

import nl.uu.cs.aplib.mainConcepts.Tactic.PrimitiveTactic;

/**
 * Actions are the building blocks for to build a {@link Tactic}. To solve a
 * {@link Goal} an agent would need a {@link Tactic}. An Action is the simplest
 * form of {@link Tactic}. Multiple Actions can be combined to form a more
 * complex strategy. When a Strategy is given to an agent to solve a Goal, we
 * say that the strategy is bound to the agent. Likewise, all Actions in the
 * Strategy are then bound to this agent. Each Action is essentially a stateful
 * program that may produce a proposal to be proposed to the Goal. Conceptually,
 * an Action can be seen as a pair (g,e) of guard and effect. The guard is a
 * predicate ove the agent's state, and the effect part e is a program that
 * reads the agent's state and the Action's own state to produce a proposal
 * (which may be null). In doing so, e may also change both the agent's and the
 * Action's states. Producing a proposal is not mandatory, e.g. if the Action
 * thinks it is not ready to produce one, or if its role is dedicated on
 * inferring more information to be added to the agent's state.
 * 
 * <p>
 * The agent invokes the Action's behavior by invoking the method
 * {@link #exec1(SimpleState)}, passing to it its (the Agent's) state. This
 * method should only be invoked if the Action's guard evaluates to true on the
 * current agent's state. This requirement is not imposed by this implementation
 * of Action. However the implementation of {@link BasicAgent} does impose
 * this.
 * 
 * @author wish
 *
 */
public class Action {

    String name;
    public String desc;
    boolean completed = false;
    long totalRuntime = 0;
    int invocationCount = 0;

    /**
     * This Action's guard, which is a query over the agent's state. This action is
     * considered as executable if the query results in a non-null value. Else the
     * action is not executable.
     * 
     * Note: by 'the agent' we mean the agent to which this Action becomes bound to.
     */
    Function<SimpleState, Object> guard = s -> this.true_;

    private final Boolean true_ = true;

    /**
     * Store the result of guard evaluation, until it is retrieved.
     */
    private Object queryResult = null;

    Object retrieveQueryResult() {
        Object o = queryResult;
        queryResult = null;
        return o;
    }

    /**
     * The effect part of this Action. It is a function that takes: (1) the agent's
     * state as parameter, and (2) the query-result of this action's guard (assumed
     * to be non-null). The function then produces a proposal. Returning a null is
     * interpreted as producing no proposal. By 'the agent' we mean the agent to
     * which this Action becomes bound to.
     */
    Function<SimpleState, Function<Object, Object>> effect;

    Action() {
    }

    /**
     * Create a blank action with the given name. The effect part of this agent is
     * set to null (so it will crash if you try to call its
     * {@link #exec1(SimpleState)}).
     */
    public Action(String name) {
        this.name = name;
    }

    /**
     * Add a description string to this Action. It returns the Action itself so that
     * it can be used in the Fluent Interface style.
     */
    public Action desc(String desc) {
        this.desc = desc;
        return this;
    }

    /**
     * Set the given predicate as the guard of this Action. The method returns the
     * Action itself so that it can be used in the Fluent Interface style.
     */
    public <AgentSt> Action on_(Predicate<AgentSt> guard) {
        this.guard = st -> {
            if (guard.test((AgentSt) st))
                return true_;
            else
                return null;
        };
        return this;
    }

    /**
     * Set the given query function as the guard of this Action. The method returns
     * the Action itself so that it can be used in the Fluent Interface style.
     */
    public <AgentSt, QueryResult> Action on(Function<AgentSt, QueryResult> myguard) {
        this.guard = st -> myguard.apply((AgentSt) st);
        return this;
    }

    /**
     * Set the given function as the effect-part of this Action. The method returns
     * the Action itself so that it can be used in the Fluent Interface style.
     */
    Action do__(Function<SimpleState, Function<Object, Object>> action) {
        this.effect = s -> y -> {
            try {
                return action.apply(s).apply(y);
            } finally {
                completed = true;
            }
        };
        return this;
    }

    /**
     * Set the given function as the effect-part of this Action. This effect
     * function takes two arguments: the state of the agent, and the value which was
     * the result of this action's guard execution/query.
     * 
     * <p>
     * The method returns the Action itself so that it can be used in the Fluent
     * Interface style.
     */
    public <AgentSt, QueryResult, T> Action do2(Function<AgentSt, Function<QueryResult, T>> action) {
        return do__(s -> y -> action.apply((AgentSt) s).apply((QueryResult) y));
    }

    /**
     * Set the given function as the effect-part of this Action. This effect
     * function takes one argument: the state of the agent.
     * 
     * <p>
     * The method returns the Action itself so that it can be used in the Fluent
     * Interface style.
     */
    public <AgentSt, T> Action do1(Function<AgentSt, T> action) {
        return do__(s -> y -> action.apply((AgentSt) s));
    }

    /**
     * Normally, when invoked (by {@link Abort#exec1(SimpleState)}), at the end of
     * the invocation an Action is marked as completed. So, the next turn the Agent
     * can choose to execute another Action. With this method we force the Agent to
     * keep executing this Action over multiple turns, <b>until</b> the given
     * predicate becomes true. The Action is also considered to be always enabled
     * (it can be executed in any agent's state).
     * 
     * <p>
     * The method returns the Action itself so that it can be used in the Fluent
     * Interface style.
     */
    Action until__(Predicate<SimpleState> myguard) {
        if (this.effect == null)
            throw new IllegalArgumentException("the action is null");
        var action_ = this.effect;
        Function<SimpleState, Function<Object, Object>> a = s -> y -> {
            var o = action_.apply(s).apply(y);
            if (myguard.test(s)) {
                completed = true;
            } else
                completed = false;
            return o;
        };
        this.effect = a;
        this.guard = o -> true_;
        return this;
    }

    /**
     * Normally, when invoked (by {@link Abort#exec1(SimpleState)}), at the end of
     * the invocation an Action is marked as completed. So, the next turn the Agent
     * can choose to execute another Action. With this method we force the Agent to
     * keep executing this Action over multiple turns, <b>until</b> the given
     * predicate becomes true. The Action is also considered to be always enabled
     * (it can be executed in any agent's state).
     * 
     * <p>
     * The method returns the Action itself so that it can be used in the Fluent
     * Interface style.
     */
    public <AgentSt> Action until(Predicate<AgentSt> myguard) {
        return until__(s -> myguard.test((AgentSt) s));
    }

    /**
     * Wrap this Action to become a {@link PrimitiveTactic}.
     */
    public PrimitiveTactic lift() {
        return new PrimitiveTactic(this);
    }

    // === fluent interface end

    /**
     * True if the Action has been marked as completed.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * True if the guard of this Action evaluates to true on the given agent state.
     */
    public boolean isEnabled(SimpleState agentstate) {
        queryResult = guard.apply(agentstate);
        return queryResult != null;
    }

    /**
     * Execute the effect part of this Action on the given agent state. The method
     * will also retrieve the stored result of the guard evaluation, and pass it to
     * the effect-function.
     * 
     * This method does not check whether the guard is true on that state. The agent
     * that calls this method is responsible for guaranteeing this.
     * 
     * <p>
     * Note: calling this method will clear the stored guard's query result.
     */
    public Object exec1(SimpleState agentstate) {
        Object o = retrieveQueryResult();
        Object proposal = effect.apply(agentstate).apply(o);
        return proposal;
    }

    /**
     * A special Action. When an agent executes this Action from within a
     * {@link Tactic}, it will cause the current goal to be considered as being
     * failed. Note that this only fails the current goal. This does not necessarily
     * mean that the top-goal will fail as well. E.g if g = FIRSTof(g1,g2); if g1
     * fails, g2 might still succeed, hence solving g.
     */
    static public class Abort extends Action {
        public Abort() {
        }
    }

}
