package nl.uu.cs.aplib.MainConcepts;

import java.util.* ;

/**
 * A GoalTree represents a series of goals for an agent to solve. It can take
 * one of the following forms:
 * 
 *    (1) a primitive goal
 *    (2) SEQ g1,g2,... representing a series of goals that all have to be solved,
 *       and solved in the order as they are listed.
 *    (3) ALT g1,g2,... representing a series of alternative goals. They will be
 *        tried one at a time, starting from g1. If one is solved, the entire ALT
 *        is solved. If all subgoals fail, the ALT fails.
 *        
 * @author wish
 *
 */
public class GoalTree {
	
	static public enum GoalsCombinator { SEQ, ALT, PRIMITIVE }
	
	GoalTree parent = null ;
	List<GoalTree> subgoals ;
	GoalsCombinator combinator ;
	PlanScheme plan ;
	
	public GoalTree(GoalsCombinator type, GoalTree ... subgoals) {
		combinator = type ;
		this.subgoals = new LinkedList<GoalTree>() ;
		for (GoalTree g : subgoals) {
			this.subgoals.add(g) ;
			g.parent = this ;
		}
	}
	
	public GoalsCombinator getCombinatorType() { return combinator ; }
	public List<GoalTree> getSubgoals() { return subgoals ; }
	public GoalTree getParent() { return parent ; }
	
	public PlanScheme getPlanScheme() { return plan ; }
	public GoalTree withPlan(PlanScheme plan) { this.plan = plan ; return this ;}
	
	public ProgressStatus getStatus() {
		if (combinator == GoalsCombinator.SEQ) {
			for (GoalTree g : subgoals) {
				ProgressStatus status = g.getStatus() ;
				if (status != ProgressStatus.SUCCESS) return status ;
			}
			return ProgressStatus.SUCCESS ;
		}
		if (combinator == GoalsCombinator.ALT) {
			for (GoalTree g : subgoals) {
				ProgressStatus status = g.getStatus() ;
				if (status != ProgressStatus.FAILED) return status ;
			}
			return ProgressStatus.FAILED ;
		}
		// should not reach this point!
		return null ;
	}
	
	/**
	 * Assuming this goal is solved, the method will return the next primitive goal
	 * to solve. The method will traverse up through the parent of this goal tree
	 * to look for this next goal. If none is found, null is returned.
	 */
	public PrimitiveGoal getNextPrimitiveGoalWorker(ProgressStatus thisGoalStatus) {
		if (parent==null) return null ;
		if (parent.combinator == GoalsCombinator.SEQ) {
			if(thisGoalStatus == ProgressStatus.FAILED)
				return parent.getNextPrimitiveGoalWorker(ProgressStatus.FAILED) ;
			// else: so, this goal is solved:
			int k = parent.subgoals.indexOf(this) ;
			if (k == parent.subgoals.size() - 1 ) 
				return parent.getNextPrimitiveGoalWorker(ProgressStatus.SUCCESS) ;
			else
				return parent.subgoals.get(k+1).getDeepestFirstPrimGoal() ;
		}
		
		if (parent.combinator == GoalsCombinator.ALT) {
			if(thisGoalStatus == ProgressStatus.SUCCESS)
				return parent.getNextPrimitiveGoalWorker(ProgressStatus.SUCCESS) ;
			// else: so, this goal failed:
			int k = parent.subgoals.indexOf(this) ;
			if (k == parent.subgoals.size() - 1 ) 
				return parent.getNextPrimitiveGoalWorker(ProgressStatus.FAILED) ;
			else
				return parent.subgoals.get(k+1).getDeepestFirstPrimGoal() ;
		}
		// this case should not happen
		return null ;
	}
	
	
	PrimitiveGoal getDeepestFirstPrimGoal() {
		return subgoals.get(0).getDeepestFirstPrimGoal() ;
	}
	
	static public class PrimitiveGoal extends GoalTree {
		Goal goal ;
		public PrimitiveGoal(Goal g) { 
			super(GoalsCombinator.PRIMITIVE) ;
			goal = g ; 
		}
		
		@Override
		public ProgressStatus getStatus() {
			return goal.getStatus() ;
		}
		
		PrimitiveGoal getDeepestFirstPrimGoal() {
			return this ;
		}

	}

}
