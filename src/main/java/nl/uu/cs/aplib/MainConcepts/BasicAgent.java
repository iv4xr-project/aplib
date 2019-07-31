package nl.uu.cs.aplib.MainConcepts;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import nl.uu.cs.aplib.MainConcepts.Action.Abort;
import nl.uu.cs.aplib.MainConcepts.GoalTree.PrimitiveGoal;
import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;
import nl.uu.cs.aplib.MultiAgentSupport.Message;
import nl.uu.cs.aplib.Utils.Time;

public class BasicAgent {
	
	protected String id ;
	protected String role ;
	
	protected SimpleState state ;
	protected GoalTree goal ;
	protected PrimitiveGoal currentGoal ;
	protected Strategy currentStrategy ;
	protected long rndseed = 1287821 ; // a prime and a palindrome :D
	protected Random rnd = new Random(rndseed) ;
	protected Logger logger = null ;
	
	
	public BasicAgent() { }
	
	
	/**
	 * When the agent has multiple candidate plans which are enabled on its current state,
	 * this method decides which one to take. In this default implementation, it will
	 * just choose randomly. 
	 * 
	 * Override this method to implement more intelligent deliberation.
	 */
	protected PrimitiveStrategy deliberate(List<PrimitiveStrategy> candidates) {
		return candidates.get(rnd.nextInt(candidates.size())) ;
	}
	
   
	public BasicAgent setGoal(GoalTree g) {
		goal = g ;
		currentGoal = goal.getDeepestFirstPrimGoal() ;
		currentStrategy = currentGoal.goal.getStrategy() ;
		return this ; 
	}
	
	public String getId() {
		return id;
	}


	public String getRole() {
		return role;
	}
	
	public BasicAgent attachState(SimpleState state) {
		this.state = state ; return this ;
	}
	
	public BasicAgent addSystemErrAsLogger() {
		if (logger == null) logger = Logger.getLogger("APLIBlogger") ;
		logger.addHandler(new ConsoleHandler());
		return this ;
	}
	
	/**
	 * Attach a file specified by the filename to this agent to be used to save its logging
	 * messages. The filename can include a path to the file.
	 */
	public BasicAgent attachLogFile(String filename) {
		if (logger == null) logger = Logger.getLogger("APLIBlogger") ;
		try {
			var fh = new FileHandler(filename);  
	        logger.addHandler(fh);
	        var formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);  
		}
		catch(Exception e) { 
			// swallow exception....
		}
		return this ;
	}
	
	public BasicAgent setLoggingLevel(Level level) {
		logger.setLevel(level);
		return this ;
	}
	
	protected void detachgoal() {
		goal = null ;
		currentGoal = null ;
	}
	
	protected List<Message> getIncomingMsgQueue() {
		return state.incomingMsgs ;
	}
	
	public void restart() { }
	
	protected void log(Level level, String s) {
		if (logger == null) return ;
		logger.log(level, s);
	}
	
	protected void setTopGoalToSuccess(String info) {
		goal.setStatusToSuccess(info);
	}
	
	/** 
	 * Set top-goal to fail, then detach it.
	 */
	protected void setTopGoalToFail(String reason) {
		goal.setStatusToFail(reason); detachgoal() ;
	}
	
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
				currentStrategy = chosenAction.calcNextStrategy(state) ;
				// if no strategy can be found, reset it to the root strategty of the goal:
				if (currentStrategy == null) currentStrategy = currentGoal.goal.getStrategy() ;
			}
			else {
				currentStrategy = chosenAction ;
			}
		}
	}


}
