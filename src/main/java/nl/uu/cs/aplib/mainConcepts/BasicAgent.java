package nl.uu.cs.aplib.mainConcepts;

import java.util.*;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exception.AplibError;
import nl.uu.cs.aplib.mainConcepts.Action.Abort;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.GoalsCombinator;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.mainConcepts.Tactic.PrimitiveTactic;
import nl.uu.cs.aplib.multiAgentSupport.ComNode;
import nl.uu.cs.aplib.multiAgentSupport.Message;
import nl.uu.cs.aplib.multiAgentSupport.Messenger;
import nl.uu.cs.aplib.utils.Time;

/**
 * This is the root class of all agents in aplib. As the name suggests, this
 * class provides you with basic but working agents. An instance of BasicAgent
 * is an agent. You can give it a goal (to be more precise, an instance of
 * {@link GoalStructure}. Assuming a strategy has been supplied along with the goal,
 * you can then execute the agent, which in turn will execute the strategy
 * towards solving the goal. A BasicAgent uses a very simplistic
 * <i>deliberation</i> scheme. When in the current state (of the agent) the
 * given strategy leads to multiple possible actions ({@link Action}) to choose,
 * the agent's internal process to decide which one to choose is called
 * <b>deliberation</b>. A BasicAgent simply chooses randomly. A BasicAgent is
 * suitable to be used as a so-called <b>subservient</b> agent. In particular,
 * it lacks the ability to run as an autonomous agent. It also lacks the ability
 * to exchange messages with other agents. If you need autonomous and
 * communicating agents, you can use a subclass called
 * {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent}.
 * 
 * <p>After creating a blank agent using the constructor, you will need to attach
 * a state to it. This may sound contradictory to you. Since we are in Java,
 * the just created agent would be an object, and it would have some methods.
 * This object already has a state, composed of the values of its fields; so the statement of
 * 'attaching' a state would be wierd. It is true that any instance of BasicAgent
 * will come with its own fields etc that form its state. However this state would
 * have no information about the domain of the goal that you try to solve with
 * the agent. For example if you want the agent to play the Monopoly game, its
 * built-in state would have no knowledge of how a the Monopoly board looks like.
 * Without this knowledge we can't expect it to be able to play Monopoly. You would
 * first need to provide a representation of this board as a subclass of
 * {@link SimpleState} and then attach an instance of this board to your agent so
 * that it can use it to at least track the state of the game. Use the method
 * {@link #attachState(SimpleState)} to attach a state. After this, the agent
 * is ready to do work for you.
 * 
 * <p>
 * Use the method {@link #setGoal(GoalStructure)} to give a goal to a BasicAgent. It
 * is assumed, that when you provide a goal, you also attach a {@link Tactic}
 * to it that the agent can use to solve the goal. To execute this strategy you
 * can invoke the method {@link #update()}, which will execute the strategy for
 * a single tick. Call the method repeatedly until the goal is solved, or until
 * the agent exhaust the computation budget that you may have specified for the
 * goal:
 * 
 * <pre>
 *   while (topgoal.getStatus().inProgress()) {
 *      agent.update();
 *      ... // do whatever else between agent's updates
 *   }
 * </pre>
 * 
 * <p>
 * Between calls to {@code update()} the agent will not do anything (though the
 * {@link Environment} on which it operates might do a lot of things). Each call
 * to {@code update()} is also called a <b>tick</b>. The above way of running an
 * agent puts you in full control of when to give a tick to the agent, hence
 * invoking its behavior. An agent that is used in this way is called
 * <i>subservient</i>, as opposed to autonomous agents that control the ticking
 * themselves. Autonomous agents would run on their own threads, whereas a
 * subservient agent can stay in the same thread is your main thread. See
 * {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent} for a base class to create
 * autonomous agents.
 * 
 * 
 * @author Wish
 *
 */
public class BasicAgent {
	
	protected String id ;
	protected String role ;
	
	protected SimpleState state ;
	
	/**
	 * The current topgoal the agent has.
	 */
	protected GoalStructure goal ;
	
	/**
	 * The last goal handled by this agent before it was detached.
	 */
	protected GoalStructure lastHandledGoal ;
	
	/**
	 * A topgoal may consists of multiple subgoals, which the agent will work on
	 * one at a time. This field will point to the current subgoal the agent
	 * is working on.
	 */
	protected PrimitiveGoal currentGoal ;
	
	/**
	 * The tactic the agent is currently using to solve its currentGoal.
	 */
	protected Tactic currentTactic ;
	
	
	protected Logger logger = Logging.getAPLIBlogger() ;
	
	/**
	 * A time tracker used to calculated the agent's actions' execution time for the purpose
	 * of budget calculation. This is declared as an explicit field so that it can be
	 * conveniently mocked during testing (you have to test from the same package).
	 */
	protected Time mytime = new Time() ;
	
	/**
	 * An instance of Deliberation is responsible for, as the name says, executing
	 * a deliberation process for this agent. {@see Deliberation}.
	 */
	protected Deliberation deliberation = new Deliberation() ;
	
	/**
	 * Specify how to calculate the cost of a single invocation of an action. The default
	 * is that each action invocation costs 1.0.
	 */
	protected CostFunction costFunction = new CostFunction() ;
	
	/**
	 * Create a blank agent. You will need to at least attach a {@link SimpleState} and 
	 * a {@link GoalStructure} to it before it can be used to do something.
	 */
	public BasicAgent() { 
		// Setting up this default logging configuration
		//Logging.addSystemErrAsLogHandler(); ... no need? it seems to have already been added by default
		Logging.setLoggingLevel(Level.INFO);
	}
	
	/**
	 * Create a blank agent with the given id and role. You will need to at least
	 * attach a {@link SimpleState} and a {@link GoalStructure} to it before it can be
	 * used to do something.
	 * 
	 * The id should be unique. Multiple agents can have the same role. For
	 * BasicAgent, Id and role do not have any influence. For
	 * {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent} they are important to
	 * identify where messages should be sent to.
	 */
	public BasicAgent(String id, String role) {
		this() ;
		this.id = id ; this.role = role ;
	}
	

	/**
	 * Set a goal for this agent. The method returns the agent itself so that this
	 * method can be used in the Fluent Interface style.
	 */
	public BasicAgent setGoal(GoalStructure g) {
		goal = g ;
		if (! allGoalsHaveTactic(g)) 
			throw new IllegalArgumentException("Agent " + id + ": some goal has no tactic.") ;
		currentGoal = goal.getDeepestFirstPrimGoal_andAllocateBudget() ;
		if (currentGoal == null) throw new IllegalArgumentException("Agent " + id + ": is gievn a goal structure with NO goal.") ;
		currentTactic = currentGoal.goal.getTactic() ; 
		if (currentTactic == null) 
			throw new IllegalArgumentException("Agent "  + id 
					+ ", goal " + currentGoal.goal.name + ": has NO tactic.") ;
		return this ; 
	}
	
	private static boolean allGoalsHaveTactic(GoalStructure g) {
		if (g instanceof PrimitiveGoal) {
			var g_ = (PrimitiveGoal) g ;
			return g_.goal.getTactic() != null ;
		}
		for (GoalStructure h : g.subgoals) {
			if (! allGoalsHaveTactic(h)) return false ;
		}
		return true ;
	}

	/**
	 * Set a goal for this agent, with the specified initial budget. 
	 * The method returns the agent itself so that this method can be used in the Fluent Interface style.
	 */
	public BasicAgent setGoal(double budget, GoalStructure g) {
		g.budget = budget ;
		return setGoal(g) ;
	}
	
	/**
	 * Set initial computation budget for this agent. The agent must have a goal set.
	 * This method should not be called when the agent is already working on its 
	 * goal.
	 */
	public BasicAgent budget(double b) {
		if (goal == null) throw new IllegalArgumentException("Agent " + id + ": allocating budget to an agent requires it to have a goal.") ;
		if (b <= 0 || ! Double.isFinite(b)) throw new IllegalArgumentException() ;
		goal.budget = b ;
		setGoal(goal) ;
		return this ;
	}
	
	/**
	 * Set f as this agent cost-function. Return the agent itself so that this
	 * method can be used in the Fluent Interface style.
	 */
	public BasicAgent withCostFunction(CostFunction f) {
		costFunction = f ; return this ;
	}
	
	/**
	 * Return the agent's id, if it was set.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Return the agent's role, if it was set.
	 */
	public String getRole() {
		return role;
	}
	
	/**
	 * Attach a state structure to this agent. The method returns the agent itself
	 * so that this method can be used in the Fluent Interface style.
	 */
	public BasicAgent attachState(SimpleState state) {
		this.state = state ; 
		state.logger = this.logger ;
		return this ;
	}
	
	/**
	 * Attach an Environment to this agent. To be more precise, to attach the
	 * Environment to the state structure of this agent. The method returns the
	 * agent itself so that this method can be used in the Fluent Interface style.
	 */
	public BasicAgent attachEnvironment(Environment env) {
		if (state == null) throw new IllegalArgumentException("The agent needs to have a state to attach an environment to it.") ;
		state.setEnvironment(env) ;
		return this  ;
	}
	
	/**
	 * Replace the agent's deliberation module with the one given to this method.
	 * The method returns the agent itself so that this method can be used in the
	 * Fluent Interface style.
	 */
	public BasicAgent useDeliberation(Deliberation delib) {
		this.deliberation = delib ;
		return this ;
	}

	
	/**
	 * As the name says, this will detach the current topgoal and subgoal from the agent. So,
	 * setting the {@code goal} and {@code currentGoal} fields to null.
	 */
	protected void detachgoal() {
		lastHandledGoal = goal ;
		goal = null ;
		currentGoal = null ;
	}
	
	/**
	 * Return the goal-structure that was last detached by this agent. A goal is detached
	 * when it is declared successful or failed. 
	 */
	public GoalStructure getLastHandledGoal() { return lastHandledGoal ; }
	
	/**
	 * Currently unimplemented.
	 */
	public void restart() { 
		throw new UnsupportedOperationException() ;
	}
	
	/**
	 * Write the string to this agent logger, with the specified logging level.
	 */
	protected void log(Level level, String s) {
		//if (logger == null) return ;
		logger.log(level, s);
	}
	
	/**
	 * For marking the agent's topgoal as succees, and adding the given info string to it.
	 */
	protected void setTopGoalToSuccess(String info) {
		goal.setStatusToSuccess(info);
	}
	
	/** 
	 * Set topgoal to fail, with the given reason. Then the goal is detached.
	 */
	protected void setTopGoalToFail(String reason) {
		goal.setStatusToFail(reason); detachgoal() ;
	}
	
	/**
	 * Insert the goal-structure G as the <b>next</b> direct sibling of the current goal.
	 * However, if the parent of this goal-structure is REPEAT (which can only have one
	 * child), a SEQ node will first be inserted in-between, and the G is added as the
	 * next sibling of this goal-structure.
	 * 
	 * <p>Fail if the current goal is null or if it is the top-goal.
	 */
	public void addAfter(GoalStructure G) {
		if (currentGoal == null || currentGoal.isTopGoal()) 
			throw new IllegalArgumentException() ;
		
		var parent = currentGoal.parent ;
		if (parent.combinator == GoalsCombinator.REPEAT) {
			// if the parent is a REPEAT-node, it can only have one child. So, we insert
			// an additional SEQ node.
			var H = SEQ(currentGoal,G) ;
			H.budget = parent.budget ;
			parent.subgoals.clear(); 
			parent.subgoals.add(H) ;
			return ;
		}
		else {
			int k = currentGoal.parent.subgoals.indexOf(currentGoal) ;
			int N = currentGoal.parent.subgoals.size() ;
			if (k==N-1) {
				currentGoal.parent.subgoals.add(G) ;
			}
			else {
				currentGoal.parent.subgoals.add(k+1,G);
			}
		}
	}
	
	/**
	 * Insert the goal-structure G as the <b>before</b> direct sibling of the current goal.
	 * More precisely, the current goal will be replaced by REPEAT(SEQ(G,current-goal)).
	 * This is only carried out if G does not already appear as a previous sibling
	 * of the current goal under a SEQ node.
	 * 
	 * <p>If added, the REPEAT node will get the same budget and max-budget as whatever
	 * the current budget of the current-goal's parent.
	 * 
	 * <p>Note that inserting a new goal in this way has the following effect. Suppose G0 is the current
	 * goal. If it fails, the inserted REPEAT node will cause the agent to retry, but this time by
	 * trying the newly inserted G first. If it is solved, the agent will continue with re-trying
	 * G0. If this still fails, G will be tried again, and so on. It will not be added twice.
	 * 
	 * <p>Fail if the current goal is null or if it is the top-goal. The latter case is forbidden
	 * because otherwise we would have to introduce a new top-goal, which might confuse the user
	 * of this agent.
	 */
	public void addBefore(GoalStructure G) {
		if (currentGoal == null || currentGoal.isTopGoal()) throw new IllegalArgumentException() ;
		
		// currentGoal must therefore have a parent:
		var parent = currentGoal.parent ;
		int k = parent.subgoals.indexOf(currentGoal) ;
		
		// case (1), G was already added. This is the case if G occurs as a previous
		// sibling under a SEQ parent.
		if (parent.combinator == GoalsCombinator.SEQ) {
			if (k>0 && G.isomorphic(parent.subgoals.get(k-1))) {
				// G already occurs as the previous sibling!
				return ;
			}
		}
		// else:
		// case (2), the parent is NOT a SEQ node. We insert REPEAT(SEQ(G,currenrgoal))
		var g1 = SEQ(G,currentGoal) ;
		var repeatNode = REPEAT(g1) ;
		g1.budget = parent.budget ;
		repeatNode.budget = parent.budget ;
		if (Double.isFinite(parent.budget)) repeatNode.maxbudget(parent.budget) ;
		parent.subgoals.remove(k) ;
		parent.subgoals.add(k,repeatNode);
		repeatNode.parent = parent ;
	    // case-2 done
	}
	
	/**
	 * Remove the goal-structure G from this agent root goal-structure.
	 * Fail if the agent has no goal or if G is an ancestor of the current goal.
	 */
	public void remove(GoalStructure G) {
		if (goal==null || currentGoal.isDescendantOf(G)) throw new IllegalArgumentException() ;
		removeGoalWorker(goal,G) ;
	}
	
	private boolean removeGoalWorker(GoalStructure H, GoalStructure tobeRemoved) {
		if (H.subgoals.contains(tobeRemoved)) {
			H.subgoals.remove(tobeRemoved) ;
			if (H.subgoals.isEmpty()) {
				if (H.isTopGoal()) {
					throw new AplibError("Removal of a goal structure causes the topgoal to become childless.") ;
				}
				else {
					removeGoalWorker(goal,H) ;
 				}
			}
			return true ;
		}
		else {
			for (GoalStructure H2 : H.subgoals) {
				var r = removeGoalWorker(H2,tobeRemoved) ;
				if (r) return true ;
			}
			return false ;
		}
	}
	
	
	/**
	 * You should not need to use this, unless you want to implement your own Agent
	 * by overriding this class, and you need your own custom way to lock and unlock
	 * access to the Environment.
	 */
	protected void lockEnvironment() {
		state.env.lock.lock(); 
	}
	
	/**
	 * You should not need to use this, unless you want to implement your own Agent
	 * by overriding this class, and you need your own custom way to lock and unlock
	 * access to the Environment.
	 */
	protected void unlockEnvironment() {
		state.env.lock.unlock(); 
	}

	
	/**
	 * Look for enabled actions within the current strategy that would be enabled on
	 * the current agent state. If there are none, the method returns. If there are
	 * multiple, the method {@link #deliberate(List)} is used to choose one. The
	 * chosen {@link Action} will then be executed for a single tick. If the Action
	 * returns a non-null proposal, this method will check is this proposal solves
	 * the current subgoal, it will be marked as such, and the next subgoal will be
	 * search. If there is none, then the topgoal is solved.
	 * 
	 * <p>
	 * This method also keeps track of the computation time so far used to work on
	 * the topgoal as well as the current subgoal. If this exceeds the allocated
	 * time, the corresponding topgoal/subgoal will be marked as failed.
	 */
	public void update() {
		if (goal == null) {
			//System.err.print("x") ;
			return ;
		}
		// Note on BUDGET:
		// We will not check the budget at the start of the update. The first current
		// goal is guaranteed to have >0 budget. This implies it is safe to check
		// the budget at the end of every update instead.
			
		mytime.sample(); 
		// We need to lock the environment since there may be multiple agents
		// sharing the same environment:
		lockEnvironment() ;
		//goal.redistributeRemainingBudget();
		try { updateWorker() ; } 
		finally {
			unlockEnvironment() ;
		}
	}
	
	private void updateWorker() {
		
		// update the agent's state:
		state.updateState() ;
		
		var candidates = currentTactic.getFirstEnabledActions(state) ;
		if (candidates.isEmpty()) {
			// if no action is enabled, we wait until the next update, to see
			// if the environment changes its state.
				
				// we keep the goal.
				return ;
		}
		// we have at least one enabled actions to choose from; use deliberation
		// to decide:
		var chosenAction = deliberation.deliberate(state,candidates) ;
		
		
		if (chosenAction.action instanceof Abort) {
			// if the action is ABORT:
			currentGoal.setStatusToFail("Abort was invoked.");
		}
		else {
			// else execute the action:
			Object proposal = costFunction.executeAction_andInstrumentCost(state,chosenAction.action) ;
			currentGoal.goal.propose_(proposal);	
			if (currentGoal.goal.getStatus().success()) {
				currentGoal.setStatusToSuccess("Solved by " + chosenAction.action.name);
			}
			currentGoal.registerConsumedBudget(costFunction.getCost());
		}
		
		// registering some statistics:
		chosenAction.action.invocationCount++ ;
		var elapsed = mytime.elapsedTimeSinceLastSample() ;
		//System.out.println("### elapsed: " + elapsed) ;
		chosenAction.action.totalRuntime += elapsed ;   
		currentGoal.registerUsedTime(elapsed);

		
		// if the current goal is not decided (still in progres), check if its budget is
		// not exhausted:
		if (currentGoal.getStatus().inProgress() && currentGoal.budget <= 0d) {
			currentGoal.setStatusToFailBecauseBudgetExhausted();
		}
		
		// check the status of top-level goal; if it is resolved, the agent is done:
		if (goal.getStatus().success() || goal.getStatus().failed())  {
		   detachgoal() ;
		   return ;
		}
		// otherwise the top goal is still in-progress...
		
		if (currentGoal.getStatus().success() || currentGoal.getStatus().failed()) {
			// so... if the current goal is closed (but the topgoal is not closed yet), we need
			// to find another goal to solve:
			currentGoal = currentGoal.getNextPrimitiveGoal_andAllocateBudget() ;
			if (currentGoal != null) {
				currentTactic = currentGoal.goal.getTactic() ;
				if (currentTactic == null) 
					// should not happen...
					throw new AplibError("Goal " + currentGoal.goal.name + " has no tactic.") ;
			}
			else {
				// there is no more goal left! 
				detachgoal() ;
				return ;
			}
			
		}
		else {
			// else the currentgoal is still in-progress
			if(chosenAction.action.isCompleted()) {
				currentTactic = chosenAction.calcNextTactic() ;
				// if no tactic can be found, reset it to the root tactic of the goal:
				if (currentTactic == null) 
					currentTactic = currentGoal.goal.getTactic() ;
			}
			else {
				currentTactic = chosenAction ;
			}
		}
	}


}
