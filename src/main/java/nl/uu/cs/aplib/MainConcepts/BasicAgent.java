package nl.uu.cs.aplib.MainConcepts;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.MainConcepts.Action.Abort;
import nl.uu.cs.aplib.MainConcepts.GoalTree.PrimitiveGoal;
import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;
import nl.uu.cs.aplib.MultiAgentSupport.ComNode;
import nl.uu.cs.aplib.MultiAgentSupport.Message;
import nl.uu.cs.aplib.MultiAgentSupport.Messenger;
import nl.uu.cs.aplib.Utils.Time;

/**
 * This is the root class of all agents in aplib. As the name suggests, this
 * class provides you with basic but working agents. An instance of BasicAgent
 * is an agent. You can give it a goal (to be more precise, an instance of
 * {@link GoalTree}. Assuming a strategy has been supplied along with the goal,
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
 * {@link nl.uu.cs.aplib.Agents.AutonomousBasicAgent}.
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
 * Use the method {@link #setGoal(GoalTree)} to give a goal to a BasicAgent. It
 * is assumed, that when you provide a goal, you also attach a {@link Strategy}
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
 * {@link nl.uu.cs.aplib.Agents.AutonomousBasicAgent} for a base class to create
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
	protected GoalTree goal ;
	
	/**
	 * A topgoal may consists of multiple subgoals, which the agent will work on
	 * one at a time. This field will point to the current subgoal the agent
	 * is working on.
	 */
	protected PrimitiveGoal currentGoal ;
	
	/**
	 * The strategy the agent is currently using to solve its currentGoal.
	 */
	protected Strategy currentStrategy ;
	
	protected long rndseed = 1287821 ; // a prime and a palindrome :D
	protected Random rnd = new Random(rndseed) ;
	
	protected Logger logger = Logging.getAPLIBlogger() ;
	
	
	/**
	 * Create a blank agent. You will need to at least attach a {@link SimpleState} and 
	 * a {@link GoalTree} to it before it can be used to do something.
	 */
	public BasicAgent() { }
	
	/**
	 * Create a blank agent with the given id and role. You will need to at least
	 * attach a {@link SimpleState} and a {@link GoalTree} to it before it can be
	 * used to do something.
	 * 
	 * The id should be unique. Multiple agents can have the same role. For
	 * BasicAgent, Id and role do not have any influence. For
	 * {@link nl.uu.cs.aplib.Agents.AutonomousBasicAgent} they are important to
	 * identify where messages should be sent to.
	 */
	public BasicAgent(String id, String role) {
		this() ;
		this.id = id ; this.role = role ;
	}
	
	/**
	 * When the agent has multiple candidate plans which are enabled on its current
	 * state, this method decides which one to take. In this default implementation,
	 * it will just choose randomly.
	 * 
	 * Override this method if you want to implement more intelligent deliberation.
	 */
	protected PrimitiveStrategy deliberate(List<PrimitiveStrategy> candidates) {
		return candidates.get(rnd.nextInt(candidates.size())) ;
	}
	
   
	/**
	 * Set a goal for this agent. The method returns the agent itself so that this
	 * method can be used in the Fluent Interface style.
	 */
	public BasicAgent setGoal(GoalTree g) {
		goal = g ;
		currentGoal = goal.getDeepestFirstPrimGoal() ;
		currentStrategy = currentGoal.goal.getStrategy() ;
		return this ; 
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
	 * As the name says, this will detach the current topgoal and subgoal from the agent. So,
	 * setting the {@code goal} and {@code currentGoal} fields to null.
	 */
	protected void detachgoal() {
		goal = null ;
		currentGoal = null ;
	}
	
	/**
	 * Currently unimplemented.
	 */
	public void restart() { }
	
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
		// if goal is not null, currentGoal should not be null either
		Time time0 = new Time() ;
		time0.sample(); 
		goal.redistributeRemainingBudget();
		var goalWhoseBudget_tobeTracked =  currentGoal ;
		try { updateWorker(time0) ; } 
		finally {
			goalWhoseBudget_tobeTracked.addConsumedBudget(time0.elapsedTimeSinceLastSample());
		}
	}
	
	private void updateWorker(Time time) {
		
		if (currentGoal.remainingBudget <= 0d) {
			currentGoal.setStatusToFail("Running out of budget.");
			//System.err.println(">> Running out of budget.") ;
			currentGoal = currentGoal.getNextPrimitiveGoal() ;
			if (currentGoal == null) {
				setTopGoalToFail("Some subgoal has run out of budget and there is no alternative subgoal towards solving the topgoal.");
				//System.err.println(">> setting topgoal to fail...") ;
				//System.err.println(">> status topgoal:"  + goal.status) ;
				return ;
			}
		 	else {
				currentStrategy = currentGoal.goal.getStrategy() ;
			}
		}
		
		// update the agent's state:
		state.upateState() ;
		
		var candidates = currentStrategy.getFirstEnabledActions(state) ;
		if (candidates.isEmpty()) {
			// if no action is enabled, we wait until the next update, to see
			// if the environment changes its state.
				
				// we keep the goal.
				return ;
		}
		// we have at least one enabled actions to choose from; use deliberation
		// to decide:
		var chosenAction = deliberate(candidates) ;
		
		
		// if the action is ABORT:
		if (chosenAction.action instanceof Abort) {
			setTopGoalToFail("abort() were invoked.");
			return ;
		}
		
		// else execute the action:
		Object proposal = chosenAction.action.exec1(state) ;
		currentGoal.goal.propose(proposal);
		chosenAction.action.invocationCount++ ;
		chosenAction.action.totalRuntime += time.elapsedTimeSinceLastSample() ;
		
		if (currentGoal.goal.getStatus().sucess()) {
			currentGoal.setStatusToSuccess("Solved by " + chosenAction.action.name);
			if (goal.getStatus().sucess())  {
				// the top level goal is solved! Then the agent is done:
				detachgoal() ;
				return ;
			}
			currentGoal = currentGoal.getNextPrimitiveGoal() ;
			if (currentGoal != null) currentStrategy = currentGoal.goal.getStrategy() ;
			return ;
		}
		
		// goal is not completed; update the currentStrategy for the next tick:
		else {
			if(chosenAction.action.isCompleted()) {
				currentStrategy = chosenAction.calcNextStrategy() ;
				// if no strategy can be found, reset it to the root strategty of the goal:
				if (currentStrategy == null) currentStrategy = currentGoal.goal.getStrategy() ;
			}
			else {
				currentStrategy = chosenAction ;
			}
		}
	}


}
