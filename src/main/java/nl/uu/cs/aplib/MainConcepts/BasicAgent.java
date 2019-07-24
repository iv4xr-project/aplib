package nl.uu.cs.aplib.MainConcepts;

import java.util.*;

import nl.uu.cs.aplib.MainConcepts.Action.Abort;
import nl.uu.cs.aplib.MainConcepts.GoalTree.PrimitiveGoal;
import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;
import nl.uu.cs.aplib.Utils.Time;

public class BasicAgent {
	
	SimpleState state ;
	GoalTree goal ;
	PrimitiveGoal currentGoal ;
	PrimitiveStrategy currentAction ;
	long rndseed = 1287821 ; // a prime and a palindrome :D
	Random rnd = new Random(rndseed) ;
	
	
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
		return this ; 
	}
	
	public BasicAgent attachState(SimpleState state) {
		this.state = state ; return this ;
	}
	
	public void restart() { }
	

	
	public void update() {
		if (currentGoal == null) {
			//System.err.print("x") ;
			return ;
		}

		Time time0 = new Time() ;
		time0.sample(); 

		goal.redistributeRemainingBudget();
		
		
		if (! currentGoal.remainingBudget.isUnlimited() && currentGoal.remainingBudget.amount<0) {
			currentGoal.setStatusToFail("Running out of budget.");
			System.err.println(">> Running out of budget.") ;
			currentAction = null ;
			currentGoal = currentGoal.getNextPrimitiveGoal() ;
			if (currentGoal == null) {
				goal.setStatusToFail("Some subgoal has run out of budget and there is no alternative subgoal towards solving the topgoal.");
				System.err.println(">> setting topgoal to fail...") ;
				System.err.println(">> status topgoal:"  + goal.status) ;
				
				return ;
			}
		}
		
		
		// at this point, the current goal must still be in-progress
		
		
		// update the agent's state:
		state.upateState() ;
		
		// to collect the set of possible actions to choose from in this
		// state:
		if (currentAction == null) {
			// no action is decided yet for the current goal, because this was the first
			// time the current goal was handled. Get the enabled actions at current goal's 
			// strategy's root
			var actions = currentGoal.goal.strategy.getEnabledActions(state) ;
			
			if (actions.isEmpty()) {
				// if no action is enabled, we wait until the next update, to see
				// if the environment changes its state.
				
				// we keep the goal.
				currentGoal.addConsumedBudget(time0.elapsedTimeSinceLastSample());
				return ;
			}
			// we have at least one enabled actions to choose from; use deliberation
			// to decide:
			currentAction = deliberate(actions) ;
		}
		else if(currentAction.action.isCompleted()) {
			// the current action was marked as completed; we need to get the next action
			// to execute.
			// We first calculate which actions are eligible for execution, and moreover 
			// their guards are true on the current agent state:
			var actions = currentAction.getNextSetOfEnabledActions(state) ;
			
			if (actions.isEmpty()) {
				// if no action is enabled, we wait until the next update, to see
				// if the environment changes its state.
				
				// we keep the goal. We also keep the current-action so that next time
				// around we know what the candidate next actions should be.
				currentGoal.addConsumedBudget(time0.elapsedTimeSinceLastSample());
				return ;
			}
			// we have at least one enabled actions to choose from; use deliberation
			// to decide:
			currentAction = deliberate(actions) ;
		}
		
		// at this point we have a currentAction which is not completed yet:
		
		// if the action is ABORT:
		if (currentAction.action instanceof Abort) {
			goal.setStatusToFail("abort() were invoked.");
			currentGoal.addConsumedBudget(time0.elapsedTimeSinceLastSample());
			currentGoal = null ;
			return ;
		}
		
		// else execute the action:
		Time time1 = new Time() ;
		time1.sample(); 
		Object proposal = currentAction.action.exec1(state) ;
		currentGoal.goal.propose(proposal);
		currentAction.action.totalRuntime += time1.elapsedTimeSinceLastSample() ;
		currentAction.action.invocationCount++ ;
		currentGoal.addConsumedBudget(time0.elapsedTimeSinceLastSample()) ;
		
		if (currentGoal.goal.getStatus().sucess()) {
			currentGoal.setStatusToSuccess("Solved by " + currentAction.action.name);
			if (goal.getStatus().sucess())  {
				// the top level goal is solved! Then the agent is done:
				currentGoal = null ;
				return ;
			}
			currentGoal = currentGoal.getNextPrimitiveGoal() ;
			currentAction = null ;
		}
		
	}
	
	
	
	
	
	// abstract xxx ;

}
