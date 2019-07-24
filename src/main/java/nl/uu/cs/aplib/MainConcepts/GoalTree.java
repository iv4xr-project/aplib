package nl.uu.cs.aplib.MainConcepts;

import java.util.* ;
import java.util.stream.Collectors;

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
	ProgressStatus status = new ProgressStatus() ;
	Budget allocatedBudget = new Budget() ;
	Budget consumedBudget = new Budget(0d) ;
	Budget remainingBudget = new Budget() ;
	
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
	
	public boolean isTopGoal() { return parent == null ; }
	
	/**
	 * Set the status of this goal to success, and propagating this accordingly
	 * to its ancestors.
	 */
	void setStatusToSuccess(String info) {
		status.setToSuccess(info) ;
		if (! isTopGoal()) {
			switch(parent.combinator) {
			   case FIRSTOF : parent.setStatusToSuccess(info); break ;
			   case SEQ : 
				    int i = parent.subgoals.indexOf(this) ;
				    if (i == parent.subgoals.size()-1)
					  	 parent.setStatusToSuccess(info); 
				    break ;
			}
		}
	}
	
	/** 
	 * Set the status of this goal to fail, and propagating this accordingly
	 * to its ancestors.
	 */
	void setStatusToFail(String reason) {
		status.setToFail(reason);
		if (! isTopGoal()) {
			switch(parent.combinator) {
			   case SEQ : parent.setStatusToFail(reason); break;
			   case FIRSTOF :
				    int i = parent.subgoals.indexOf(this) ;
					if (i == parent.subgoals.size()-1)
						parent.setStatusToFail(reason);
					break;
			}
		}
	}
	
	/**
	 * To abort the entire goal tree; this is done by marking this goal, all
	 * the way to the root, as fail.
	 */
	void abort() {
		status.setToFail("abort() is invoked.") ; 
		if (! isTopGoal()) parent.abort() ;
 	}
	
	public ProgressStatus getStatus() { return status ; }
	
	/**
	 * Assuming this goal is solved or failed, the method will return the next primitive goal
	 * to solve. The method will traverse up through the parent of this goal tree
	 * to look for this next goal. If none is found, null is returned.
	 */
	public PrimitiveGoal getNextPrimitiveGoal() {
		if (status.inProgress() || isTopGoal()) return null ;
		
		switch(parent.combinator) {
		  case SEQ :
			   if(status.failed())
				  return parent.getNextPrimitiveGoal() ;
			   // else: so, this goal is solved:
			   int k = parent.subgoals.indexOf(this) ;
			   if (k == parent.subgoals.size() - 1 ) 
				  return parent.getNextPrimitiveGoal() ;
			   else
				  return parent.subgoals.get(k+1).getDeepestFirstPrimGoal() ;
		  case FIRSTOF :
			   if(status.sucess())
				  return parent.getNextPrimitiveGoal() ;
			   // else: so, this goal failed:
			   k = parent.subgoals.indexOf(this) ;
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
	
	
	/**
	 * Calculate the demanded minimum budget for this goal, if any specified.
	 */
	double demandedMinimumBudget() {
		if (this instanceof PrimitiveGoal) {
			var b = ((PrimitiveGoal) this).goal.demandedMinimumBudget ;
			if (b == null) return 0d ;
			else return b.amount() ;
		}
		// for FIRSTOF and SEQ we define the minimum demanded budget to be the sum
		// of that of the subgoals. For FIRSTOF this is indeed a worst case assumption.
		double sum = 0 ;
		for (GoalTree gt : subgoals) {
			sum += gt.demandedMinimumBudget() ;
		}
		return sum ;
	}
	
	/**
	 * Allocate budget to this goal. This can only be invoked on the top goal.
	 */
	public GoalTree withBudget(Budget budget) {
		if (! isTopGoal()) throw new IllegalArgumentException("Can only be called on a top goal") ;
		if (budget.isUnlimited() || budget.amount() <= 0) throw new IllegalArgumentException() ;
		if (budget.amount() < demandedMinimumBudget())
			throw new IllegalArgumentException("The allocated budget is below the demanded minimum.") ;
		allocatedBudget = budget ;
		redistributeRemainingBudget() ;
		return this ;
	}
	
	/**
	 * Add delta to the tracked amount of consumed budget.
	 */
	void addConsumedBudget(float delta) {
		consumedBudget.add(delta); 
		if (! isTopGoal()) parent.addConsumedBudget(delta);
	}
	
	/**
	 * Call this at the top goal to reset tracked consumed budget in this goal and its subgoals to
	 * 0, and then to reset the distribution of the budget accordingly.
	 */
	void resetBudget() {
		if (! isTopGoal()) throw new IllegalArgumentException() ;
		resetConsumedBudget() ;
		redistributeRemainingBudget() ;
	}
	
	private void resetConsumedBudget() {
		consumedBudget.amount = 0d ;
		for (GoalTree gt : subgoals) gt.resetConsumedBudget(); 
	}
	
	/**
	 * Call this at the top goal, to recalculate remaining budget and re-distribute it to
	 * the subgoals.
	 */
	void redistributeRemainingBudget() {
		if (! isTopGoal()) throw new IllegalArgumentException() ;
		if (allocatedBudget.isUnlimited()) return ;
		distributeRemainingBudgetWorker(allocatedBudget.amount - consumedBudget.amount) ;
	}
	
	private void distributeRemainingBudgetWorker(double budget) {
		remainingBudget.amount = budget ;
		remainingBudget.consume(consumedBudget);
		double available = Math.max(remainingBudget.amount(),0) ;
		
		var subgoalsWithBudgetDemand = subgoals.stream()
				                       .filter(g -> g.status.inProgress() &&  g.demandedMinimumBudget()>0d)
				                       .collect(Collectors.toList()) ;
		var subgoalsWithNOBudgetDemand = subgoals.stream()
				                       .filter(g -> g.status.inProgress() && ! (g.demandedMinimumBudget()>0d))
				                       .collect(Collectors.toList()) ;
		
		int K = subgoalsWithBudgetDemand.size() + subgoalsWithNOBudgetDemand.size() ;
		
		if (K == 0) return ;
		
		for (GoalTree gt : subgoalsWithBudgetDemand) {
			if (available <= 0) {
				gt.distributeRemainingBudgetWorker(0); 
			}
			else {
				double demanded = gt.demandedMinimumBudget() ;
				double toAllocate = available/K ;
				if (toAllocate < demanded) toAllocate = Math.min(demanded,available) ;
				gt.distributeRemainingBudgetWorker(toAllocate);
				available = available - toAllocate ;
			}
			K-- ;
		}
		if (K==0) return ;
		double avrg = available/K ;
		for (GoalTree gt : subgoalsWithNOBudgetDemand) {
			if (available <= 0) {
				gt.distributeRemainingBudgetWorker(0); 
			}
			else {
				gt.distributeRemainingBudgetWorker(avrg);
				available = available - avrg ;
			}
		}
	}
	
	private String space(int k) { String s = "" ; for(int i=0; i<k; i++) s += " " ; return s ; }
	String showTreeStatusWorker(int level) {
		String indent =  space(3*(level+1)) ;
		String s = "" ;
		if (this instanceof PrimitiveGoal) {
			s += indent + ((PrimitiveGoal) this).goal.getName() + ": " + status ;
			if (isTopGoal()) s += "\n" + indent + "Budget:" + allocatedBudget ;
			s += "\n" + indent + "Consumed budget:" + consumedBudget ;
			
			return s ;
		}
		s += indent + combinator + ": " + status ; 
		if (isTopGoal()) s += "\n" + indent + "Budget:" + allocatedBudget ;
		s += "\n" + indent + "Consumed budget:" + consumedBudget + "\n" ;
		for (GoalTree gt : subgoals) s += gt.showTreeStatusWorker(level+1) + "\n" ;
		return s ;
	}
	
	public String showTreeStatus() { return showTreeStatusWorker(0) ; }
	public void printTreeStatus() { 
		System.out.println("\n** Goal status:") ;
		System.out.println(showTreeStatus()) ; 
	}
	
	
	static public class PrimitiveGoal extends GoalTree {
		Goal goal ;
		public PrimitiveGoal(Goal g) { 
			super(GoalsCombinator.PRIMITIVE) ;
			goal = g ; 
		}
		
		PrimitiveGoal getDeepestFirstPrimGoal() {
			return this ;
		}

	}

}
