package nl.uu.cs.aplib.mainConcepts;

import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.uu.cs.aplib.AplibEDSL.*;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exception.AplibError;
import nl.uu.cs.aplib.mainConcepts.Action.Abort;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.GoalsCombinator;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.utils.Time;
import static nl.uu.cs.aplib.mainConcepts.GoalStructureStack.StackItem ;

/**
 * This is the root class of all agents in aplib. As the name suggests, this
 * class provides you with basic but working agents. An instance of BasicAgent
 * is an agent. You can give it a goal (to be more precise, an instance of
 * {@link GoalStructure}. Assuming a strategy has been supplied along with the
 * goal, you can then execute the agent, which in turn will execute the strategy
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
 * <p>
 * After creating a blank agent using the constructor, you will need to attach a
 * state to it. This may sound contradictory to you. Since we are in Java, the
 * just created agent would be an object, and it would have some methods. This
 * object already has a state, composed of the values of its fields; so the
 * statement of 'attaching' a state would be wierd. It is true that any instance
 * of BasicAgent will come with its own fields etc that form its state. However
 * this state would have no information about the domain of the goal that you
 * try to solve with the agent. For example if you want the agent to play the
 * Monopoly game, its built-in state would have no knowledge of how a the
 * Monopoly board looks like. Without this knowledge we can't expect it to be
 * able to play Monopoly. You would first need to provide a representation of
 * this board as a subclass of {@link SimpleState} and then attach an instance
 * of this board to your agent so that it can use it to at least track the state
 * of the game. Use the method {@link #attachState(SimpleState)} to attach a
 * state. After this, the agent is ready to do work for you.
 * 
 * <p>
 * Use the method {@link #setGoal(GoalStructure)} to give a goal to a
 * BasicAgent. It is assumed, that when you provide a goal, you also attach a
 * {@link Tactic} to it that the agent can use to solve the goal. To execute
 * this strategy you can invoke the method {@link #update()}, which will execute
 * the strategy for a single tick. Call the method repeatedly until the goal is
 * solved, or until the agent exhaust the computation budget that you may have
 * specified for the goal:
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

    protected String id;
    protected String role;

    protected SimpleState state;
    
	/**
	 * The agent maintains a stack of goal-structures. Typically there is only one
	 * goal-structure there, which is the goal-structure that the agent will be
	 * working on. Having a stack allows the agent to push a new goal-structure H to
	 * the stack (or rather, allowing an action to push a new goal-structure). The
	 * agent will then switch to this new goal-structure and work on it until it
	 * either succeeds or fails, after which the agent will pop the H from the stack
	 * and returns to the goal-structure that it was working before H was pushed.
	 * 
	 * <p>
	 * Suppose the agent was working on a root goal structure G when it pushes H.
	 * And suppose at that time the current primitive goal that the agent is working
	 * is g. The pushing of H to the stack will happen inside the agent's update
	 * cycle. H will not be immediately pushed; the mechanic is as follows:
	 * 
	 * <ul>
	 * <li>The agent will first finish the current update cycle on G.
	 * <li>If that cycle actually concludes G (with either success or fail), H will
	 * be discarded (and G is popped out of the stack).
	 * 
	 * <li>If the cycle does not conclude G (so still in progress), H is pushed to
	 * the stack, which means the agent will switch to H at the next cycle. It will
	 * get the budget as much as what g has.
	 * 
	 * <li>When H is concluded (with either success or fail) the agent pops it from
	 * the stack and "returns" to G. In the stack we also memorize what the current
	 * primitive goal (g) within G was, and the current tactic was before we pushed
	 * H. So, when the agent returns to G to it would continue with the same
	 * prim-goal and current-tactic as they were at the moment H was pushed.
	 * 
	 * <li>As the agent returns to G, its remaining budget will be adjusted with what 
	 * H consumed.
	 * 
	 * </ul>
	 */
    protected GoalStructureStack goalstack = new GoalStructureStack() ;

    /**
     * The last root goal-structure handled by this agent before it was detached.
     */
    protected GoalStructure lastHandledRootGoalStructure;
    

    protected Logger logger = Logging.getAPLIBlogger();

    /**
     * A time tracker used to calculated the agent's actions' execution time for the
     * purpose of budget calculation. This is declared as an explicit field so that
     * it can be conveniently mocked during testing (you have to test from the same
     * package).
     */
    protected Time mytime = new Time();

    /**
     * An instance of Deliberation is responsible for, as the name says, executing a
     * deliberation process for this agent. {@see Deliberation}.
     */
    protected Deliberation deliberation = new Deliberation();

    /**
     * Specify how to calculate the cost of a single invocation of an action. The
     * default is that each action invocation costs 1.0.
     */
    protected CostFunction costFunction = new CostFunction();

    /**
     * Create a blank agent. You will need to at least attach a {@link SimpleState}
     * and a {@link GoalStructure} to it before it can be used to do something.
     */
    public BasicAgent() {
        // Setting up this default logging configuration
        // Logging.addSystemErrAsLogHandler(); ... no need? it seems to have already
        // been added by default
        Logging.setLoggingLevel(Level.INFO);
    }

    /**
     * Create a blank agent with the given id and role. You will need to at least
     * attach a {@link SimpleState} and a {@link GoalStructure} to it before it can
     * be used to do something.
     * 
     * The id should be unique. Multiple agents can have the same role. For
     * BasicAgent, Id and role do not have any influence. For
     * {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent} they are important to
     * identify where messages should be sent to.
     */
    public BasicAgent(String id, String role) {
        this();
        this.id = id;
        this.role = role;
    }

    /**
     * Set a goal/goal-structure for this agent. More precisely, the agent maintains a stack of 
     * goal-structures. The given goal-structure g will then be pushed onto the stack to become
     * the current-goal structure that the agent will work on.
     * 
     * <p>The method returns the agent itself so that this method can be used in the Fluent Interface style.
     */
    public BasicAgent setGoal(GoalStructure G) {
        if (!allGoalsHaveTactic(G))
            throw new IllegalArgumentException("Agent " + id + ": some goal has no tactic.");
        if (!G.checkIfWellformed())
        	throw new IllegalArgumentException("Agent " + id + ": is given a goal-structure that is not well-formed.");
        
        var GI = new StackItem(G) ;
        goalstack.stack.add(GI) ;
        prepareGoalStructureAtTheTopOfStack() ;
        logger.info("Agent " + id + " is given a new goal structure " + showGoalStructShortDesc(G));
        return this;
    }
    
    private void prepareGoalStructureAtTheTopOfStack() {
    	var g = goalstack.currentRootGoal().getDeepestFirstPrimGoal_andAllocateBudget();
    	goalstack.setCurrentPrimitiveGoal(g);
        if (g == null)
            throw new IllegalArgumentException("Agent " + id + " is given a goal structure with NO goal.");
        var tac = g.goal.getTactic() ;
        goalstack.setCurrentTactic(tac);
        if (tac == null)
            throw new IllegalArgumentException("Agent " + id + ", goal " + g.goal.name + ": has NO tactic.");
    }

    private static boolean allGoalsHaveTactic(GoalStructure g) {
        if (g instanceof PrimitiveGoal) {
            var g_ = (PrimitiveGoal) g;
            return g_.goal.getTactic() != null;
        }
        for (GoalStructure h : g.subgoals) {
            if (!allGoalsHaveTactic(h))
                return false;
        }
        return true;
    }
    
    private static String showGoalStructShortDesc(GoalStructure G) {
    	if (G instanceof PrimitiveGoal) return G.getName() ;
    	if (G.shortdesc == null) return "" ;
    	return G.getName() ;
    }

	/**
	 * Similar to {@link #setGoal(GoalStructure)}, but will also assign the
	 * specified initial budget for the given goal/goal-structure. The method
	 * returns the agent itself so that this method can be used in the Fluent
	 * Interface style.
	 */
    public BasicAgent setGoal(double budget, GoalStructure G) {
        G.budget = budget;
        return setGoal(G);
    }

    /**
     * Set initial computation budget the current/top goal-structure in the agent's
     * goal-stack. 
     * NOTE: This method should not be called when the agent is already working on
     * that top goal.
     */
    public BasicAgent budget(double b) {
    	var goal = goalstack.currentRootGoal() ;
        if (b <= 0 || !Double.isFinite(b))
            throw new IllegalArgumentException();
        goal.budget = b;
        return this;
    }
    
    /**
     * Panic button, to drop all goal-structures currently in the goal-stack.
     */
    public void dropAll() {
    	goalstack.commitPendingPush() ;
    	goalstack.stack.clear();
    }

    /**
     * Set f as this agent cost-function. Return the agent itself so that this
     * method can be used in the Fluent Interface style.
     */
    public BasicAgent withCostFunction(CostFunction f) {
        costFunction = f;
        return this;
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
        this.state = state;
        state.owner = this ;
        state.logger = this.logger;
        return this;
    }
    
    /**
     * Return the agent's state.
     */
    public SimpleState state() {
    	return state ;
    }

    /**
     * Attach an Environment to this agent. To be more precise, to attach the
     * Environment to the state structure of this agent. The method returns the
     * agent itself so that this method can be used in the Fluent Interface style.
     */
    public BasicAgent attachEnvironment(Environment env) {
        if (state == null)
            throw new IllegalArgumentException("The agent needs to have a state to attach an environment to it.");
        state.setEnvironment(env);
        return this;
    }
    
    /**
     * Return the environment attached to this agent.
     */
    public Environment env() {
    	return state.env() ;
    }

    /**
     * Replace the agent's deliberation module with the one given to this method.
     * The method returns the agent itself so that this method can be used in the
     * Fluent Interface style.
     */
    public BasicAgent useDeliberation(Deliberation delib) {
        this.deliberation = delib;
        return this;
    }

    /**
     * As the name says, this will detach/pop the entire goal-structure that is currently
     * at the top of the agent's goal-stack (this is the goal-structure that the
     * agent is currently working on). If that was the only goal-structure in the stack,
     * the stack will then become empty and the agent has no further goal to work on.
     * Else the next goal-structure in the stack becomes the top goal-structure.
     * 
     * <p>Popping a goal from the goalstack also drops a currently uncommited push 
     * to the goalstack. The rationale is that the goal that pushed the uncommited
     * goal has concluded, so the uncommitted goal should be retracted as well.
     */
    protected void detachgoal() {
        lastHandledRootGoalStructure = goalstack.currentRootGoal() ;
        // pop the goal:
        goalstack.pop() ;
        int N = goalstack.stack.size() ;
        if (N > 0) {
        	// deduct the budget that the popped-goal used from the remaining budget
        	// of the new root goal-structure, and add statistics about time
        	// consumed by that popped-goal:
        	goalstack.currentPrimitiveGoal().registerConsumedBudget(lastHandledRootGoalStructure.consumedBudget);
        	goalstack.currentPrimitiveGoal().registerUsedTime(lastHandledRootGoalStructure.consumedTime);
        }
        // popping a goal also cancel uncommitted push to the goal-stack:
        if (goalstack.pendingPush != null) {
        	var H = goalstack.pendingPush ;
        	goalstack.pendingPush = null ;
        	logger.info("Agent " + id + " RETRACTs a tentatively pushed goal " 
        			+ showGoalStructShortDesc(H.rootGoal)
        			+ ", because the root goal-structure that pushed it has concluded.") ;
        }
        
        
        String status = "" ;
        if (lastHandledRootGoalStructure.getStatus().success()) status = "(success)" ;
        else if(lastHandledRootGoalStructure.getStatus().failed()) status = "(fail)" ;
        if (N==0)
        	logger.info("Agent " + id + " detaches the last goal-structure from the stack "
        			+ showGoalStructShortDesc(lastHandledRootGoalStructure)
        			+ "; status: " + status + ".") ;
        else
        	logger.info("Agent " + id + " pops a goal-structure from the stack " 
        			+ showGoalStructShortDesc(lastHandledRootGoalStructure)
        			+ "(status:" + status 
        			+ "), and switches to the next root goal-structure in the stack "
        			+ showGoalStructShortDesc(goalstack.currentRootGoal())
        			+ ".") ;
    }

    /**
     * Return the goal-structure that was last detached by this agent. A goal is
     * detached when it is declared successful or failed.
     */
    public GoalStructure getLastHandledGoal() {
        return lastHandledRootGoalStructure;
    }

    /**
     * Currently unimplemented.
     */
    public void restart() {
        throw new UnsupportedOperationException();
    }

    /**
     * Write the string to this agent logger, with the specified logging level.
     */
    protected void log(Level level, String s) {
        // if (logger == null) return ;
        logger.log(level, s);
    }


    /**
     * Insert the goal-structure G as the <b>next</b> direct sibling of the current
     * primitive goal (the primitive goal that the agent is currently working on).
     * However, if the parent of this primitive goal is REPEAT (which can only
     * have one child), a SEQ node will first be inserted in-between, and the G is
     * added as the next sibling of this goal-structure.
     * 
     * <p>
     * Fail if the current primitive-goal is null or if it is a root-goal.
     */
    public void addAfter(GoalStructure G) {
    	var currentGoal = goalstack.currentPrimitiveGoal() ;
        if (currentGoal == null || currentGoal.isRootGoal())
            throw new IllegalArgumentException();
        
        G.makeInProgressAgain();

        var parent = currentGoal.parent;
        if (parent.combinator == GoalsCombinator.REPEAT) {
            // if the parent is a REPEAT-node, it can only have one child. So, we insert
            // an additional SEQ node.
            var H = SEQ(currentGoal, G);
            H.budget = parent.budget ;
            parent.subgoals.clear();
            parent.subgoals.add(H);
            H.parent = parent;
        } else {
            int k = currentGoal.parent.subgoals.indexOf(currentGoal);
            int N = currentGoal.parent.subgoals.size();
            if (k == N - 1) {
                currentGoal.parent.subgoals.add(G);
            } else {
                currentGoal.parent.subgoals.add(k + 1, G);
            }
            G.parent = currentGoal.parent;
        }
        G.budget = Math.min(G.bmax, G.parent.budget) ;
        String Gname = "(" + G.getName() + ")" ;
        logger.info("Agent " + id + " inserts a new goal "
        		             + Gname + " after goal " + currentGoal.goal.name 
        		             + "; autoremove=" + G.autoRemove);
    }
    
    /** 
     * As {@link #addAfter(GoalStructure)}, but the inserted goal is set with 
     * the auto-remove flag turned on. This means that after G is achieved or
     * failed, it will also be removed from the goal-structure that it is
     * part of.
     */
    public void addAfterWithAutoRemove(GoalStructure G) {
    	G.autoRemove = true ;
    	addAfter(G) ;
    }

    
    @Deprecated
	/**
	 * Deprecated. Use the non-repeating {@link #simpleAddBefore(GoalStructure)}
	 * instead.
	 * 
	 * <p>
	 * Insert the goal-structure G as the <b>before</b> direct sibling of the
	 * current primitive-goal (the primitive goal that the agent is currently
	 * working on). More precisely, this current primitive-goal will be replaced by
	 * REPEAT(SEQ(G,current-goal)). This is only carried out if G does not already
	 * appear as a previous sibling of the current goal under a SEQ node.
	 * 
	 * <p>
	 * If added, the REPEAT node will get the same budget and max-budget as whatever
	 * the current budget of the current-goal's parent.
	 * 
	 * <p>
	 * Note that inserting a new goal in this way has the following effect. Suppose
	 * G0 is the current goal. If it fails, the inserted REPEAT node will cause the
	 * agent to retry, but this time by trying the newly inserted G first. If it is
	 * solved, the agent will continue with re-trying G0. If this still fails, G
	 * will be tried again, and so on. It will not be added twice.
	 * 
	 * <p>
	 * Fail if the current primitive-goal is null or if it is a root-goal.
	 */
    public void addBefore(GoalStructure G) {
    	var currentGoal = goalstack.currentPrimitiveGoal() ;
        if (currentGoal == null || currentGoal.isRootGoal())
            throw new IllegalArgumentException();
        
        G.makeInProgressAgain() ;

        // currentGoal must therefore have a parent:
        var parent = currentGoal.parent;
        int k = parent.subgoals.indexOf(currentGoal);

        // case (1), G was already added. This is the case if G occurs as a previous
        // sibling under a SEQ parent.
        if (parent.combinator == GoalsCombinator.SEQ) {
            if (k > 0 && G.isomorphic(parent.subgoals.get(k - 1))) {
                // G already occurs as the previous sibling!
                return;
            }
        }
        // else:
        // case (2), the parent is NOT a SEQ node. We insert REPEAT(SEQ(G,currenrgoal))
        var g1 = SEQ(G, currentGoal);
        var repeatNode = REPEAT(g1);
        g1.budget = parent.budget;
        repeatNode.budget = parent.budget;
        if (Double.isFinite(parent.budget))
            repeatNode.maxbudget(parent.budget);
        g1.budget = parent.budget;
        G.budget = Math.min(G.parent.budget, G.bmax) ;
        parent.subgoals.remove(k);
        parent.subgoals.add(k, repeatNode);
        repeatNode.parent = parent;
        logger.info("Agent " + id + " inserts a new goal structure before goal " + currentGoal.goal.name + ".");
        // case-2 done
    }
    
	/**
	 * Insert the goal-structure G as a sibling <b>before</b> the current
	 * primitive-goal (the primitive goal that the agent is currently working on).
	 * However, if the parent of this primitive-goal is REPEAT (which can only have
	 * one child), a SEQ node will first be inserted in-between, and the G is added
	 * as the pre-sibling of this primitive-goal.
	 * 
	 * <p>
	 * Fail if the current primitive-goal is null or if it is the top-goal.
	 */
    public void simpleAddBefore(GoalStructure G) {
    	var currentGoal = goalstack.currentPrimitiveGoal() ;
        if (currentGoal == null || currentGoal.isRootGoal())
            throw new IllegalArgumentException();

        var parent = currentGoal.parent;
        if (parent.combinator == GoalsCombinator.REPEAT) {
            // if the parent is a REPEAT-node, it can only have one child. So, we insert
            // an additional SEQ node.
            var H = SEQ(G, currentGoal);
            H.budget = parent.budget;
            parent.subgoals.clear();
            parent.subgoals.add(H);
            H.parent = parent;
        } else {
            int k = currentGoal.parent.subgoals.indexOf(currentGoal);
            currentGoal.parent.subgoals.add(k, G);
            G.parent = currentGoal.parent;
        }
        G.budget = Math.min(G.parent.budget, G.bmax) ;
        logger.info("Agent " + id + " inserts a new goal structure after goal " + currentGoal.goal.name + ".");
    }
    
    /** 
     * As {@link #simpleAddBefore(GoalStructure)}, but the inserted goal is set with 
     * the auto-remove flag turned on. This means that after G is achieved or
     * failed, it will also be removed from the goal-structure that it is
     * part of.
     */
    public void addBeforeWithAutoRemove(GoalStructure G) {
    	G.autoRemove = true ;
    	simpleAddBefore(G) ;
    }
    
    /**
     * Push the given goal-structure G to the goalstack. The goal is not immediately pushed.
     * The agent will wait until the end of the current update cycle, and then commit
     * the push to the stack. Note that if the current root goal-structure is closed/concluded,
     * either in success or fail, G will be retracted (it will not be pushed onto the stack).
     * 
     * <p>See also {@link #goalstack}.
     */
    public void pushGoal(GoalStructure G) {
    	G.makeInProgressAgain();
    	goalstack.pendingPush(new StackItem(G)) ;
    }

	/**
	 * Remove the goal-structure G from this agent top goal-structure (the
	 * goal-structure at the top of the agent's goal stack). Fail if the agent has
	 * no root goal or if G is an ancestor of the current goal.
	 */
    public void remove(GoalStructure G) {
    	var goal = goalstack.currentRootGoal() ;
        if (goalstack.currentPrimitiveGoal().isDescendantOf(G))
            throw new IllegalArgumentException("Trying to remove a goal-structure that contains the current primitive-goal.");
        removeGoalWorker(goal, G);
        logger.info("Agent " + id + " removes a sub-goal-structure.");
    }

    private boolean removeGoalWorker(GoalStructure H, GoalStructure tobeRemoved) {
        if (H.subgoals.contains(tobeRemoved)) {
            H.subgoals.remove(tobeRemoved);
            if (H.subgoals.isEmpty()) {
                if (H.isRootGoal()) {
                    throw new AplibError("Removal of a goal structure causes the topgoal to become childless.");
                } else {
                    removeGoalWorker(goalstack.currentRootGoal(), H);
                }
            }
            return true;
        } else {
            for (GoalStructure H2 : H.subgoals) {
                var r = removeGoalWorker(H2, tobeRemoved);
                if (r)
                    return true;
            }
            return false;
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
     * multiple, the deliberation policy set by the method
     * {@link #useDeliberation(Deliberation)} is used to choose one (the default is
     * to just choose randomly). The chosen {@link Action} will then be executed for
     * a single tick. If the Action returns a non-null proposal, this method will
     * check is this proposal solves the current subgoal, it will be marked as such,
     * and the next subgoal (within the current root goal structure) will be search. 
     * If there is none, then the current root-goal is solved.
     * 
     * <p>
     * This method also keeps track of the computation time so far used to work on
     * the current root-goal as well as the current subgoal. If this exceeds the allocated
     * time, the corresponding root-goal/subgoal will be marked as failed.
     */
    public void update() {
        try {
        	// We need to lock the environment since there may be multiple agents
            // sharing the same environment:
            lockEnvironment();
            
        	if (goalstack.stack.size() == 0) {
                // System.err.print("x") ;
                return;
            }
            // Note on BUDGET:
            // We will not check the budget at the start of the update. The first current
            // goal is guaranteed to have >0 budget. This implies it is safe to check
            // the budget at the end of every update instead.

            mytime.sample();
            
            // goal.redistributeRemainingBudget();
            updateWorker();
        } finally {
            // In case there is a pending push to the goal-stack, commit this goal.
        	// This would mean that the agent would then (in the next update) switch to the pushed goal.
            var pushed = goalstack.commitPendingPush() ;
        	if (pushed) {
        		prepareGoalStructureAtTheTopOfStack() ;
        		// a goal-structure H is pushed; this does imply that we must have
        		// one root goal structure still in the stack.
        		// We will use H's currentPrim-goal budget as H's budget as the max of H's budget:
        		var H = goalstack.currentRootGoal() ;
        		int N = goalstack.stack.size() ;
            	var inheritedBudget = goalstack.stack.get(N-2).currentPrimitiveGoal.budget ;
            	if (H.budget > inheritedBudget) 
            		H.budget = inheritedBudget ;

        		logger.info("Agent " + id + " switches to a newly pushed goal " + showGoalStructShortDesc(H) + ".");
        	}
        	// unlock the env:	
        	unlockEnvironment();
        }
    }

    private void updateWorker() {

        // update the agent's state:
        state.updateState(id);
        
        var currentRootGoalStructure = goalstack.currentRootGoal() ;
        var currentPrimitiveGoal   = goalstack.currentPrimitiveGoal() ;
        
        var candidates = goalstack.currentTactic().getFirstEnabledActions(state);
        if (candidates.isEmpty()) {
            // if no action is enabled, we wait until the next update, to see
            // if the environment changes its state.

            // we keep the goal.
            return;
        }
        // we have at least one enabled actions to choose from; use deliberation
        // to decide:
        var chosenAction = deliberation.deliberate(state, candidates);

        if (chosenAction.action instanceof Abort) {
            // if the action is ABORT:
            logger.info("Agent " + id + " ABORTs the goal " + currentPrimitiveGoal.goal.name + ".");
            currentPrimitiveGoal.setStatusToFail("Abort was invoked.");
        } else {
            // else execute the action:
            Object proposal = costFunction.executeAction_andInstrumentCost(state, chosenAction.action);
            currentPrimitiveGoal.goal.propose_(proposal);
            if (currentPrimitiveGoal.goal.getStatus().success()) {
                logger.info("Agent " + id + " SOLVEs the goal " + currentPrimitiveGoal.goal.name + ".");
                currentPrimitiveGoal.setStatusToSuccess("Solved by " + chosenAction.action.name);
            }
            currentPrimitiveGoal.registerConsumedBudget(costFunction.getCost());
        }

        // registering some statistics:
        chosenAction.action.invocationCount++;
        var elapsed = mytime.elapsedTimeSinceLastSample();
        // System.out.println("### elapsed: " + elapsed) ;
        chosenAction.action.totalRuntime += elapsed;
        currentPrimitiveGoal.registerUsedTime(elapsed);

        // if the current goal is not decided (still in progress), check if its budget is
        // not exhausted:
        if (currentPrimitiveGoal.getStatus().inProgress() && currentPrimitiveGoal.budget <= 0d) {
            logger.info("Agent " + id + " FAILs the goal " + currentPrimitiveGoal.goal.name + "; its budget is exhausted.");
            currentPrimitiveGoal.setStatusToFailBecauseBudgetExhausted();
        }

        // check the status of root goal; if it is resolved, the agent is done:
        if (currentRootGoalStructure.getStatus().success() || currentRootGoalStructure.getStatus().failed()) {
            detachgoal();
            // we don't bother to clean up auto-remove goals since the top-goal
            // in concluded anyway:
            return;
        }
        // otherwise the root goal is still in-progress...

        if (currentPrimitiveGoal.getStatus().success() || currentPrimitiveGoal.getStatus().failed()) {
            // so... if the current goal is closed (but the root goal is not closed yet),
        	// check first if we have an auto-remove goal that needs to be removed:
        	GoalStructure autoRemovedGoal_tobeRemoved = currentRootGoalStructure.get_Concluded_AutoRemove_Subgoal() ;
        	
        	// Next, we need to find another goal to solve:
        	
        	var nextGoalToDo = currentPrimitiveGoal.getNextPrimitiveGoal_andAllocateBudget();
        	goalstack.setCurrentPrimitiveGoal(nextGoalToDo);
            
            if (nextGoalToDo != null) {
                logger.info("Agent " + id + " switches to goal " + nextGoalToDo.goal.name + ".");
                goalstack.setCurrentTactic(nextGoalToDo.goal.getTactic());
                if (goalstack.currentTactic() == null)
                    // should not happen...
                    throw new AplibError("Goal " + nextGoalToDo.goal.name + " has no tactic.");
              
                // Apply removal of auto-remove subgoals that become achieved or failed.
                // Actually, we will only remove at most one such subgoal G because there 
                // can only be at most one such G that is closest to the root. There may
                // be other concluded auto-remove subgoals, but these should all be subgoals
                // of G.
                // 
                // Note that the new currentGoal g cannot be part of the removed G. This would
                // otherwise imply that both the previous goal g0 and g are
                // decendants of G. But since G is concluded, g cannot possibly be a new 
                // current goal.
                if (autoRemovedGoal_tobeRemoved != null) {
                	if (autoRemovedGoal_tobeRemoved == goalstack.currentPrimitiveGoal()) {
                		throw new AplibError("Something is wrong: agent " 
                				+ this.id
                				+ " tries to auto-remove the current goal: " + goalstack.currentPrimitiveGoal().getName()) ;
                	}
                	this.remove(autoRemovedGoal_tobeRemoved);
                	logger.info("Agent " + id + " AUTO-remove a goal: "
                			+ showGoalStructShortDesc(autoRemovedGoal_tobeRemoved)) ;
                }
                
                
            } else {
                // there is no more goal left!
                detachgoal();
                return;
            }

        } else {
            // else the current primitive goal is still in-progress; 
        	// We determine the next tactic to try; this depends on the tactic-structure of
        	// the current prim-goal.
            if (chosenAction.action.isCompleted()) {
                var nextTactic = chosenAction.calcNextTactic() ;
                if (nextTactic == null)
                	// if no tactic can be found, reset it to the root tactic of the current goal:
                	nextTactic = currentPrimitiveGoal.goal.getTactic();
            	goalstack.setCurrentTactic(nextTactic);
            } else {
            	goalstack.setCurrentTactic(chosenAction) ;
            }
        }
    }

}
