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
	
	static public enum GoalsCombinator { SEQ, FIRSTOF, PRIMITIVE }
	
	GoalTree parent = null ;
	List<GoalTree> subgoals ;
	GoalsCombinator combinator ;
	ProgressStatus status = ProgressStatus.INPROGRESS ;
	
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
	
	/**
	 * Set the status of this goal to success, and propagating this accordingly
	 * to its ancestors.
	 */
	void setStatusToSuccess() {
		status = ProgressStatus.SUCCESS ;
		if (parent != null) {
			if (parent.combinator == GoalsCombinator.FIRSTOF)
				parent.setStatusToSuccess();
			else if (parent.combinator == GoalsCombinator.SEQ) {
				int i = parent.subgoals.indexOf(this) ;
				if (i == parent.subgoals.size()-1)
					parent.setStatusToSuccess(); 
			}
		}
	}
	
	/** 
	 * Set the status of this goal to fail, and propagating this accordingly
	 * to its ancestors.
	 */
	void setStatusToFail() {
		status = ProgressStatus.FAILED ;
		if (parent != null) {
			if (parent.combinator == GoalsCombinator.SEQ)
				parent.setStatusToFail();
			else if (parent.combinator == GoalsCombinator.FIRSTOF) {
				int i = parent.subgoals.indexOf(this) ;
				if (i == parent.subgoals.size()-1)
					parent.setStatusToFail(); 
			}
		}
	}
	
	/**
	 * To abort the entire goal tree; this is done by marking this goal, all
	 * the way to the root, as fail.
	 */
	void abort() {
		status = ProgressStatus.FAILED ;
		if (parent != null) parent.abort() ;
 	}
	
	public ProgressStatus getStatus() { return status ; }
	
	/**
	 * Assuming this goal is solved or failed, the method will return the next primitive goal
	 * to solve. The method will traverse up through the parent of this goal tree
	 * to look for this next goal. If none is found, null is returned.
	 */
	public PrimitiveGoal getNextPrimitiveGoal() {
		if (status == ProgressStatus.INPROGRESS || parent==null) return null ;
		if (parent.combinator == GoalsCombinator.SEQ) {
			if(status == ProgressStatus.FAILED)
				return parent.getNextPrimitiveGoal() ;
			// else: so, this goal is solved:
			int k = parent.subgoals.indexOf(this) ;
			if (k == parent.subgoals.size() - 1 ) 
				return parent.getNextPrimitiveGoal() ;
			else
				return parent.subgoals.get(k+1).getDeepestFirstPrimGoal() ;
		}
		
		if (parent.combinator == GoalsCombinator.FIRSTOF) {
			if(status == ProgressStatus.SUCCESS)
				return parent.getNextPrimitiveGoal() ;
			// else: so, this goal failed:
			int k = parent.subgoals.indexOf(this) ;
			if (k == parent.subgoals.size() - 1 ) 
				return parent.getNextPrimitiveGoal() ;
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
