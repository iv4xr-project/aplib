package nl.uu.cs.aplib.MainConcepts;

import java.util.*;

import nl.uu.cs.aplib.MainConcepts.Action.Abort;
import nl.uu.cs.aplib.MainConcepts.GoalTree.PrimitiveGoal;
import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;

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
	
	/**
	 * When a new current goal is set, its current action is still set to null.
     * The agent will need to fetch  one. This method will find all actions which
	 * structurally eligible as the first action of the current goal's strategy,
	 * and whose guards are also enabled on the current agent state. 
	 * 
	 * If no candidates can be found, then the current goal is not solvable. It
	 * is marked as failed, and the method fetches the next eligible goal, and repeat
	 * the search.
	 * 
	 * If finally this method returns empty, then we have exhausted all goals
	 * (there are no next-goal left for which there are actions possible to
	 * solve them).
	 */
	private List<PrimitiveStrategy> getFirstActionCandidates() {
		List<PrimitiveStrategy> actions = new LinkedList<PrimitiveStrategy>() ;
		while (goal.getStatus() != ProgressStatus.FAILED && actions.isEmpty()) {
			// get the enabled actions at current goal's strategy's root
			actions = currentGoal.goal.strategy.getEnabledActions(state) ;
			if (actions.isEmpty()) {
				currentGoal.setStatusToFail(); 
				currentGoal = currentGoal.getNextPrimitiveGoal() ;
				if (currentGoal == null) {
					// we have exhausted all goals... return empty:
					return actions ;
				}
			}
		}
		return actions ;
	}
	
	public void update() {
		if (currentGoal == null) return ;

		// update the agent's state:
		state.upateState() ;
		
		// to collect the set of possible actions to choose from in this
		// state:
			
		if (currentAction == null) {
			// no action is decided yet for the current goal (because this was the first
			// time the current goal was handled). Calculate candidates for its first action:
			List<PrimitiveStrategy> actions = getFirstActionCandidates() ;
			if (actions.isEmpty()) {
				// there are no enabled first actions for the current goal, not even when we
				// check all eligible next-goal (this also implies that the root goal is
				// no longer solvable)
				//goal.setStatusToFail();  ... no need
				//currentGoal = null ;
				currentAction = null ;
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
				// if we cannot get candidate action, we cycle back to the root strategy:
				actions = getFirstActionCandidates() ;
				// there are no enabled first actions for the current goal, not even when we
				// check all eligible next-goal (this also implies that the root goal is
				// no longer solvable)
				if (actions.isEmpty()) {
					currentAction = null ;
					return ;
				}
			}
			// we have at least one enabled actions to choose from; use deliberation
			// to decide:
			currentAction = deliberate(actions) ;
		}
		
		// at this point we have a currentAction which is not completed yet:
		
		// if the action is ABORT:
		if (currentAction.action instanceof Abort) {
			goal.setStatusToFail();
			currentGoal = null ;
			return ;
		}
		
		// else execute the action:
		Object proposal = currentAction.action.exec1(state) ;
		currentGoal.goal.propose(proposal);
		if (currentGoal.goal.getStatus() == ProgressStatus.SUCCESS) {
			currentGoal.setStatusToSuccess();
			if (goal.getStatus() == ProgressStatus.SUCCESS)  {
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
