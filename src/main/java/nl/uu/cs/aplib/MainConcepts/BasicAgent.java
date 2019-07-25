package nl.uu.cs.aplib.MainConcepts;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.uu.cs.aplib.MainConcepts.Action.Abort;
import nl.uu.cs.aplib.MainConcepts.GoalTree.PrimitiveGoal;
import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;
import nl.uu.cs.aplib.Utils.Time;

public class BasicAgent {
	
	String id ;
	String role ;
	
	SimpleState state ;
	GoalTree goal ;
	PrimitiveGoal currentGoal ;
	Strategy currentStrategy ;
	long rndseed = 1287821 ; // a prime and a palindrome :D
	Random rnd = new Random(rndseed) ;
	Logger logger = null ;
	
	
	public BasicAgent() { }
	
	
	/**
	 * Currently not used.
	 */
	PriorityQueue<APEvent> q = new PriorityQueue<APEvent>(10,new APEvent.APEventComparator()) ;
	
	
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
	
	public BasicAgent attachState(SimpleState state) {
		this.state = state ; return this ;
	}
	
	public void restart() { }
	
	protected void log(Level level, String s) {
		if (logger == null) return ;
		logger.log(level, s);
	}
	
	public void update() {
		if (currentGoal == null) {
			//System.err.print("x") ;
			return ;
		}
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
		
		if (! currentGoal.remainingBudget.isUnlimited() && currentGoal.remainingBudget.amount<= 0d) {
			currentGoal.setStatusToFail("Running out of budget.");
			//System.err.println(">> Running out of budget.") ;
			currentGoal = currentGoal.getNextPrimitiveGoal() ;
			if (currentGoal == null) {
				goal.setStatusToFail("Some subgoal has run out of budget and there is no alternative subgoal towards solving the topgoal.");
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
			goal.setStatusToFail("abort() were invoked.");
			currentGoal = null ;
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
				currentGoal = null ;
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
	
	
	
	
	
	// abstract xxx ;

}
