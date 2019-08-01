package nl.uu.cs.aplib.MainConcepts;

import java.util.function.*;

import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;

/**
 * Actions are the building blocks for to build a {@link Strategy}. To solve a
 * {@link Goal} an agent would need a {@link Strategy}. An Action is the
 * simplest form of {@link Strategy}. Multiple Actions can be combined to form a
 * more complex strategy. When a Strategy is given to an agent to solve a Goal,
 * we say that the strategy is bound to the agent. Likewise, all Actions in the
 * Strategy are then bound to this agent. Each Action is essentially a stateful
 * program that may produce a proposal to be proposed to the Goal. Conceptually,
 * an Action can be seen as a pair (g,e) of guard and effect. The guard is a
 * predicate ove the agent's state, and the effect part e is a program that
 * reads the agent's state and the Action's own state to produce a proposal
 * (which may be null). In doing so, e may also change both the agent's and the
 * Action's states. Producing a proposal is not
 * mandatory, e.g. if the Action thinks it is not ready to produce one, or if
 * its role is dedicated on inferring more information to be added to the
 * agent's state.
 * 
 * <p>
 * The agent invokes the Action's behavior by invoking the method {@link #exec1(SimpleState)},
 * passing to it its (the Agent's) state. This method should only be invoked if the
 * Action's guard evaluates to true on the current agent's state. This requirement is
 * not imposed by this implementation of Action. However the implementation of {@link SimpleAgent}
 * does impose this.
 * 
 * @author wish
 *
 */
public class Action {
	
	String name ;
	public String desc ;
	boolean completed = false ;
	long totalRuntime = 0 ;
	int invocationCount = 0 ;
	
	/**
	 * This Action's guard, which is a predicate over the agent's state. By 'the agent'
	 * we mean the agent to which this Action becomes bound to.
	 */
	Predicate<SimpleState> guard = s -> true;
	
	/**
	 * The effect part of this Action. It is a function that takes the agent's state
	 * as parameter, this Action itself (to get access to its state), and produces a
	 * proposal. Returning a null is interpreted as producing no proposal. By 'the
	 * agent' we mean the agent to which this Action becomes bound to.
	 */
	Function<SimpleState,Function<Action,Object>> action ;
	
	
	Action(){ }
	
	/**
	 * Create a blank action with the given name. The effect part of this agent is set to null
	 * (so it will crash if you try to call its {@link #exec1(SimpleState)}).
	 */
	public Action(String name) {this.name = name ; }
	
	/**
	 * Add a description string to this Action. It returns the Action itself so that
	 * it can be used in the Fluent Interface style.
	 */
	public Action desc(String desc) { this.desc = desc ; return this ; }
	
	/**
	 * Set the given predicate as the guard of this Action. The method returns the Action itself so that
	 * it can be used in the Fluent Interface style.
	 */
	public Action on__(Predicate<SimpleState> guard) { this.guard = guard ; return this ; }
	
	/**
	 * Set the given predicate as the guard of this Action. The method returns the
	 * Action itself so that it can be used in the Fluent Interface style.
	 */
	public <AgentSt> Action on_(Predicate<AgentSt> guard) { 
		return on__(st -> guard.test((AgentSt) st)) ;
	}
	
	/**
	 * Set the given function as the effect-part of this Action. The method returns
	 * the Action itself so that it can be used in the Fluent Interface style.
	 */
	public Action do__(Function<SimpleState,Function<Action,Object>> action) {
		this.action = s -> y -> { 
			try { return action.apply(s).apply(y) ; }
			finally { y.completed = true  ;}
		} ;
		return this ;
	}
	
	/**
	 * Set the given function as the effect-part of this Action. The method returns
	 * the Action itself so that it can be used in the Fluent Interface style.
	 */
	public <AgentSt,T> Action do_(Function<AgentSt,Function<Action,T>> action) {
		return do__(s -> y -> action.apply((AgentSt) s).apply(y)) ;
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
	public Action until__(Function<SimpleState,Predicate<Action>> guard) {
		if (action == null) throw new IllegalArgumentException("the action is null") ;
		Function<SimpleState,Function<Action,Object>> a = s -> y -> {
			var o = action.apply(s).apply(y) ;
			if (guard.apply(s).test(y)) {
				y.completed = true ;
			}
			else y.completed = false ;
			return o ;
		} ;
		this.action = a ;
		this.guard = o -> true ;
		return this ;
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
	public <AgentSt> Action until_(Function<AgentSt,Predicate<Action>> guard) {
		return until__(s -> y -> guard.apply((AgentSt) s).test(y)) ;
	}
	
	
	/**
	 * Wrap this Action to become a {@link PrimitiveStrategy}.
	 */
	public PrimitiveStrategy lift() {
		return new PrimitiveStrategy(this) ;
	}
	
	// === fluent interface end
	
	/**
	 * True if the Action has been marked as completed.
	 */
	public boolean isCompleted() { return completed ; }
	
	/**
	 * Mark this action as completed.
	 */
	void markAsCompleted() { completed = true ; }
	
	/**
	 * True if the guard of this Action evaluates to true on the given agent state.
	 */
	public boolean isEnabled(SimpleState agentstate) { 
		return guard.test(agentstate)  ; 
	}
	
	/**
	 * Execute the effect part of this Action on the given agent state. This method
	 * does not check whether the guard is true on that state. The agent that calls
	 * this method is responsible for guaranteeing this.
	 */
	public Object exec1(SimpleState agentstate) {
		Object proposal = action.apply(agentstate).apply(this) ;
		return proposal ;
	}
	
	/**
	 * A special Action. When an agent executes this Action from within a
	 * {@link Strategy}, it will cause the entire Strategy (all the way to the root
	 * Strategy) to be aborted, and whatever the top-level Goal the agent is
	 * currently work on is marked as failed.
	 */
	static public class Abort extends Action {
		public Abort() { }
	}


}
