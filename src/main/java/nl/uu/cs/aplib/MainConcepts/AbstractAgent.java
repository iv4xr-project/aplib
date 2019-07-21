package nl.uu.cs.aplib.MainConcepts;

import java.util.*;

import nl.uu.cs.aplib.MainConcepts.GoalTree.PrimitiveGoal;
import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;

public abstract class AbstractAgent {
	
	AbstractState state ;
	GoalTree goal ;
	PrimitiveGoal currentGoal ;
	Strategy planscheme ;
	PrimitiveStrategy currentPlan ;
	long rndseed = 1287821 ; // a prime and a palindrome :D
	Random rnd = new Random(rndseed) ;
	
	
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
	
   
	public void restart() {
		
	}
	
	public void update() {
		if (currentGoal != null) return ;
		// request the Environment to update its state
		state.upateState() ;
		
		if (currentPlan.plan.isCompleted()) {
			// the current plan is marked as completed, we calculate what's
			// the most appropriate next plan:
			currentPlan = getNextPlan() ;
			if (currentPlan == null) {
				// if no next plan can be calculated, the goal is marked as fail
				
			}
		}
		
		
		
		
		if (! currentPlan.plan.isCompleted()) {
			currentPlan.plan.exec1(state) ;
		}
		
		
		
		
	}
	
	
	
	
	
	// abstract xxx ;

}
