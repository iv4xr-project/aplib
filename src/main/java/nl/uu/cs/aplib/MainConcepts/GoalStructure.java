package nl.uu.cs.aplib.MainConcepts;

import java.util.* ;
import java.util.stream.Collectors;

/**
 * A GoalStructure is a generalization of a {@link Goal}. It is a tree-shaped structure
 * that conceptually represents a more complex goal. The simplest GoalStructure is an object
 * of type {@link PrimitiveGoal}. A {@link PrimitiveGoal} itself is a subclass
 * of GoalTree. Such a GoalStructure represents a single leaf, containing a single instance of {@link Goal},
 * which is the concrete goal represented by this leaf.
 * 
 * <p>
 * More complex GoalStructure can be constructed by combining subgoals. There are two types
 * of nodes available to combine sub-GoalStructure: the <b>SEQ</b> and <b>FIRSTOF</b> nodes:
 * 
 * <ol>
 *    <li> SEQ g1,g2,... represents a series of goals that all have to be solved,
 *       and solved in the order as they are listed.
 *    <li> FIRSTof g1,g2,... represents a series of alternative goals. They will be
 *        tried one at a time, starting from g1. If one is solved, the entire ALT
 *        is solved. If all subgoals fail, the ALT fails.
 * </ol>       
 *        
 * @author wish
 *
 */
public class GoalStructure {
	
	/**
	 * Represent the available types of {@link GoalStructure}. There are three types: SEQ, FIRSTOF, and
	 * PRIMITIVE. If a GoalStructure is marked as PRIMITIVE, then it is a leaf (in other words, it is
	 * a {@link PrimitiveGoal}). If a GoalStructure h is marked as SEQ, it represents a tree of the form
	 * SEQ g1,g2,... where g1,g2,... are h' subgoals. If h is marked as FIRSTOF, it represents a
	 * tree of the form FIRSTof g1,g2,....
	 */
	static public enum GoalsCombinator { SEQ, FIRSTOF, PRIMITIVE }
	
	GoalStructure parent = null ;
	List<GoalStructure> subgoals ;
	GoalsCombinator combinator ;
	ProgressStatus status = new ProgressStatus() ;
	double allocatedBudget = Double.POSITIVE_INFINITY ;
	double consumedBudget = 0 ;
	double remainingBudget = Double.POSITIVE_INFINITY ;
	
	/**
	 * Construct a new GoalStructure with the specified type of node (SEQ, FIRSTOFF, or PRIMITIVE)
	 * and the given subgoals.
	 */
	public GoalStructure(GoalsCombinator type, GoalStructure ... subgoals) {
		combinator = type ;
		this.subgoals = new LinkedList<GoalStructure>() ;
		for (GoalStructure g : subgoals) {
			this.subgoals.add(g) ;
			g.parent = this ;
		}
	}
	
	/**
	 * Return the type of this GoalStructure (SEQ, FIRSTOF, or PRIMITIVE).
	 */
	public GoalsCombinator getCombinatorType() { return combinator ; }
	
	public List<GoalStructure> getSubgoals() { return subgoals ; }
	
	/**
	 * Return the parent of this GoalStructure. It returns null if it has no parent.
	 */
	public GoalStructure getParent() { return parent ; }
	
	/**
	 * True is this goal has no parent.
	 */
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
	
	/**
	 * Get the status of this GoalStructure. The status is INPROGRESS if the GoalStructure is
	 * not solved or failed yet. It is SUCCESS if the GoalStructure was solved, and
	 * FAILED if the GoalStructure has been marked as such.
	 */
	public ProgressStatus getStatus() { return status ; }
	
	/**
	 * Assuming this goal is closed (that is, it has been solved or failed), this
	 * method will return the next {@link PrimitiveGoal} to solve. The method will traverse
	 * up through the parent of this GoalStructure to look for this next goal. If none
	 * is found, null is returned.
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
			   if(status.success())
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
	 * Calculate the estimated demanded minimum budget for this goal, if any specified.
	 */
	double demandedMinimumBudget() {
		if (this instanceof PrimitiveGoal) {
			var b = ((PrimitiveGoal) this).goal.demandedMinimumBudget ;
			if (b <=0) return 0 ;
			else return b ;
		}
		// for FIRSTOF and SEQ we define the minimum demanded budget to be the sum
		// of that of the subgoals. For FIRSTOF this is indeed a worst case assumption.
		double sum = 0 ;
		for (GoalStructure gt : subgoals) {
			sum += gt.demandedMinimumBudget() ;
		}
		return sum ;
	}
	
	/**
	 * Allocate budget to this goal. This can only be invoked on the top goal.
	 * The allocated budget should finite and positive.
	 */
	public GoalStructure withBudget(double budget) {
		if (! isTopGoal()) throw new IllegalArgumentException("Can only be called on a top goal") ;
		if (budget <= 0 || ! Double.isFinite(budget)) throw new IllegalArgumentException() ;
		if (budget < demandedMinimumBudget())
			throw new IllegalArgumentException("The allocated budget is below the demanded minimum.") ;
		allocatedBudget = budget ;
		redistributeRemainingBudget() ;
		return this ;
	}
	
	/**
	 * Register that the agent has consumed the given amount of budget.
	 */
	void addConsumedBudget(float delta) {
		consumedBudget += delta ;
		remainingBudget -= delta ;
		if (! isTopGoal()) parent.addConsumedBudget(delta);
	}
	
	
	/**
	 * Return the budget allocated for this GoalTree.
	 */
	public double getBudget() { return allocatedBudget ; }
	
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
		consumedBudget = 0 ;
		for (GoalStructure gt : subgoals) gt.resetConsumedBudget(); 
	}
	
	/**
	 * Call this at the top goal, to recalculate remaining budget and re-distribute it to
	 * the subgoals.
	 */
	void redistributeRemainingBudget() {
		if (! isTopGoal()) throw new IllegalArgumentException() ;
		if (allocatedBudget == Double.POSITIVE_INFINITY) return ;
		// if the topgoal is already closed:
		if (!status.inProgress()) {
			this.remainingBudget = 0 ;
			return ;
		}
		distributeRemainingBudgetWorker(allocatedBudget - consumedBudget) ;
	}
	
	private int numberOfOpenPrimitiveGoals() {
		if (! status.inProgress()) return 0 ;
		if (combinator == GoalsCombinator.PRIMITIVE) return 1 ;
		return subgoals.stream().collect(Collectors.summingInt(G -> G.numberOfOpenPrimitiveGoals())) ;
	}
	
	private void giveZeroBudget() {
		this.remainingBudget = 0 ;
		for (GoalStructure gt : subgoals) gt.giveZeroBudget();
	}
	
	private void distributeRemainingBudgetWorker(double budget) {
		
		// NOT needed; this is handled in below
		//if (! status.inProgress()) {
		//	System.out.println(">>> zeroing") ;
		//	giveZeroBudget() ; return ;
		//}
		
		double available = Math.max(budget,0) ;
		this.remainingBudget = available ;
		
		if (combinator == GoalsCombinator.PRIMITIVE) return ;
		
		//System.err.println(">> remaining budget " + remainingBudget.amount) ;
		var subgoalsWithBudgetDemand = subgoals.stream()
				                       .filter(g -> g.status.inProgress() &&  g.demandedMinimumBudget()>0d)
				                       .collect(Collectors.toList()) ;
		var subgoalsWithNOBudgetDemand = subgoals.stream()
				                       .filter(g -> g.status.inProgress() && ! (g.demandedMinimumBudget()>0d))
				                       .collect(Collectors.toList()) ;
		
		var closedSubgoals = subgoals.stream()
                .filter(g -> ! g.status.inProgress())
                .collect(Collectors.toList()) ;
		
		// set budget for closed subgoals to zero first:
		for (GoalStructure gt : closedSubgoals) gt.giveZeroBudget();
		
		//int K = subgoalsWithBudgetDemand.size() + subgoalsWithNOBudgetDemand.size() ;
		int K = numberOfOpenPrimitiveGoals() ;
		if (K == 0) return ;
		double avrg_perPrimGoal = available / K ;
		
		for (GoalStructure gt : subgoalsWithBudgetDemand) {
			if (available <= 0) {
				gt.distributeRemainingBudgetWorker(0); 
			}
			else {
				double demanded = gt.demandedMinimumBudget() ;
				double toAllocate = avrg_perPrimGoal * gt.numberOfOpenPrimitiveGoals() ;
				if (toAllocate < demanded) toAllocate = Math.min(demanded,available) ;
				gt.distributeRemainingBudgetWorker(toAllocate);
				available = available - toAllocate ;
				//System.out.println("## allocating = " + toAllocate + ", available = " + available) ;
			}
		}
		
		K = subgoalsWithNOBudgetDemand.stream().collect(Collectors.summingInt(G -> G.numberOfOpenPrimitiveGoals())) ;
		
		if (K==0) return ;
		//System.out.println("## available = " + available + ", K = " + K) ;
		avrg_perPrimGoal = available/K ;
		for (GoalStructure gt : subgoalsWithNOBudgetDemand) {
			if (available <= 0) {
				gt.distributeRemainingBudgetWorker(0); 
			}
			else {
				double toAllocate = avrg_perPrimGoal * gt.numberOfOpenPrimitiveGoals() ;
				gt.distributeRemainingBudgetWorker(toAllocate);
				available = available - toAllocate ;
			}
		}
	}
	
	private String space(int k) { String s = "" ; for(int i=0; i<k; i++) s += " " ; return s ; }
	
	String showGoalStructureStatusWorker(int level) {
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
		for (GoalStructure gt : subgoals) s += gt.showGoalStructureStatusWorker(level+1) + "\n" ;
		return s ;
	}
	
	/**
	 * Format a summary of the state of this GoalStructure to a readable string.
	 */
	public String showGoalStructureStatus() { return showGoalStructureStatusWorker(0) ; }
	
	/**
	 * Print a summary of the state of this GoalStructure.
	 */
	public void printGoalStructureStatus() { 
		System.out.println("\n** Goal status:") ;
		System.out.println(showGoalStructureStatus()) ; 
	}
	
	/**
	 * A special subclass of {@link GoalStructure} to represent a leaf, wrapping around
	 * an instance of {@link Goal}.
	 */
	static public class PrimitiveGoal extends GoalStructure {
		Goal goal ;
		
		/**
		 * Create an instance of PrimitiveGoal, wrapping around the given {@link Goal}.
		 */
		public PrimitiveGoal(Goal g) { 
			super(GoalsCombinator.PRIMITIVE) ;
			goal = g ; 
		}
		
		PrimitiveGoal getDeepestFirstPrimGoal() {
			return this ;
		}

	}

}
