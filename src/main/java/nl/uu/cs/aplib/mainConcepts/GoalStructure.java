package nl.uu.cs.aplib.mainConcepts;

import java.util.*;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exception.AplibError;

/**
 * A GoalStructure is a generalization of a {@link Goal}. It is a tree-shaped
 * structure that conceptually represents a more complex goal. The simplest
 * GoalStructure is an object of type {@link PrimitiveGoal}. A
 * {@link PrimitiveGoal} itself is a subclass of GoalTree. Such a GoalStructure
 * represents a single leaf, containing a single instance of {@link Goal}, which
 * is the concrete goal represented by this leaf.
 * 
 * <p>
 * More complex GoalStructure can be constructed by combining subgoals. There
 * are two types of nodes available to combine sub-GoalStructure: the <b>SEQ</b>
 * and <b>FIRSTOF</b> nodes:
 * 
 * <ol>
 * <li>SEQ g1,g2,... represents a series of goals that all have to be solved,
 * and solved in the order as they are listed.
 * <li>FIRSTof g1,g2,... represents a series of alternative goals. They will be
 * tried one at a time, starting from g1. If one is solved, the entire ALT is
 * solved. If all subgoals fail, the ALT fails.
 * </ol>
 * 
 * @author wish
 *
 */
public class GoalStructure {
	
	/**
	 * Sometimes useful to attach a short-desc, e.g. for debugging.
	 */
	String shortdesc = null ;

    /**
     * Represent the available types of {@link GoalStructure}. There are three
     * types: SEQ, FIRSTOF, and PRIMITIVE. If a GoalStructure is marked as
     * PRIMITIVE, then it is a leaf (in other words, it is a {@link PrimitiveGoal}).
     * If a GoalStructure h is marked as SEQ, it represents a tree of the form SEQ
     * g1,g2,... where g1,g2,... are h' subgoals. If h is marked as FIRSTOF, it
     * represents a tree of the form FIRSTof g1,g2,....
     */
    static public enum GoalsCombinator {
        SEQ, FIRSTOF, REPEAT, PRIMITIVE
    }

    GoalStructure parent = null;
    List<GoalStructure> subgoals = new LinkedList<GoalStructure>();
    GoalsCombinator combinator;
    ProgressStatus status = new ProgressStatus();
    
    /**
     * A sub-goal-structure that is flagged as auto-remove will be automatically
     * removed when it is achieved or failed. This flag is by default set to
     * false (so, NO auto-removal). If set to true, then auto-remove applies.
     * 
     * <p>In principle, only goals dynamically added through agent.addBefore or
     * agent.addAfter could be auto-remove.
     */
    boolean autoRemove = false ;

    /**
     * Maximum limit on the budget that can be allocated to this goal structure.
     */
    double bmax = Double.POSITIVE_INFINITY;

    /**
     * Total budget that is spent so far on this goal structure.
     */
    double consumedBudget = 0;

    long consumedTime = 0;

    /**
     * The budget that remains for this goal structure.
     */
    double budget = Double.POSITIVE_INFINITY;

    GoalStructure() {
    }

    /**
     * Construct a new GoalStructure with the specified type of node (SEQ, FIRSTOFF,
     * or PRIMITIVE) and the given subgoals.
     */
    public GoalStructure(GoalsCombinator type, GoalStructure... subgoals) {
        combinator = type;
        // this.subgoals = new LinkedList<GoalStructure>() ;
        if (subgoals != null) {
            for (int k = 0; k < subgoals.length; k++) {
                var g = subgoals[k];
                this.subgoals.add(g);
                g.parent = this;
            }
        }
    }

    /**
     * Return the type of this GoalStructure (SEQ, FIRSTOF, or PRIMITIVE).
     */
    public GoalsCombinator getCombinatorType() {
        return combinator;
    }

    public List<GoalStructure> getSubgoals() {
        return subgoals;
    }

    /**
     * Return the parent of this GoalStructure. It returns null if it has no parent.
     */
    public GoalStructure getParent() {
        return parent;
    }

    /**
     * True is this goal has no parent.
     */
    public boolean isRootGoal() {
        return parent == null;
    }
    public boolean checkIfWellformed() {
    	return checkIfWellformedWorker(new LinkedList<GoalStructure>(), new LinkedList<GoalStructure>()) ;
    }
    
    private boolean checkIfWellformedWorker(List<GoalStructure>  ancestors, List<GoalStructure> seen) {
    	if(subgoals==null) {
    		Logging.getAPLIBlogger().info("A goal has null subgoals-field (use empty instead).");
    		return  false ;
    	}
    	if (ancestors.contains(this)) {
			Logging.getAPLIBlogger().info("The goal-structure has a cycle. This is not allowed).");
			return false ;
		}
    	if (seen.contains(this)) {
			Logging.getAPLIBlogger().info("The goal-structure has a goal shared by multiple parents. This is not allowed).");
			return false ;
		}
    	ancestors.add(this) ;
    	seen.add(this) ;
    	for(var G : subgoals) {
    		if(! G.checkIfWellformedWorker(ancestors,seen)) {
    			return false ;
    		}
    	}
		ancestors.remove(this) ;
    	return true ;
    }

    /**
     * Check if this goal structure is isomorphic with H. This is the case if they
     * have primitive goals with the same names, and they have the same tree-shapes.
     */
    public boolean isomorphic(GoalStructure H) {
        if (this instanceof PrimitiveGoal) {
            var this_ = (PrimitiveGoal) this;
            if (H instanceof PrimitiveGoal) {
                var H_ = (PrimitiveGoal) H;
                return this_.goal.name.equals(H_.goal.name);
            }
            return false;
        }
        if (H instanceof PrimitiveGoal)
            return false;
        // case when both are not prim.goals
        if (this.combinator != H.combinator)
            return false;
        if (this.subgoals.size() != H.subgoals.size())
            return false;
        int N = this.subgoals.size();
        for (int k = 0; k < N; k++) {
            if (!this.subgoals.get(k).isomorphic(H.subgoals.get(k)))
                return false;
        }
        return true;
    }

    /**
     * Set the status of this goal to success, and propagating this accordingly to
     * its ancestors.
     */
    @SuppressWarnings("incomplete-switch")
	void setStatusToSuccess(String info) {
        status.setToSuccess(info);
        if (!isRootGoal()) {
            switch (parent.combinator) {
            case FIRSTOF:
                parent.setStatusToSuccess(info);
                break;
            case SEQ:
                int i = parent.subgoals.indexOf(this);
                if (i == parent.subgoals.size() - 1)
                    parent.setStatusToSuccess(info);
                break;
            case REPEAT:
                parent.setStatusToSuccess(info);
                break;
            }
        }
    }

    /**
     * Set the status of this goal to fail, and propagating this accordingly to its
     * ancestors.
     */
    @SuppressWarnings("incomplete-switch")
	void setStatusToFail(String reason) {
        status.setToFail(reason);
        if (!isRootGoal()) {
            if (parent.budget <= 0d) {
                parent.setStatusToFailBecauseBudgetExhausted();
                return;
            }
            switch (parent.combinator) {
            case SEQ:
                parent.setStatusToFail(reason);
                break;
            case FIRSTOF:
                int i = parent.subgoals.indexOf(this);
                if (i == parent.subgoals.size() - 1)
                    parent.setStatusToFail(reason);
                break;
            case REPEAT:
                break;
            }
        }
    }

    void setStatusToFailBecauseBudgetExhausted() {
        setStatusToFail("The budget is exhausted");
    }

    /**
     * Get the status of this GoalStructure. The status is INPROGRESS if the
     * GoalStructure is not solved or failed yet. It is SUCCESS if the GoalStructure
     * was solved, and FAILED if the GoalStructure has been marked as such.
     */
    public ProgressStatus getStatus() {
        return status;
    }
    
    /**
     * If this is a primitive-goal, returns its name. Else return the name of
     * the combinator of this goal structure and its shortdesc, if the latter
     * is provided.
     */
    public String getName() {
    	if (this instanceof PrimitiveGoal) {
    		return ((PrimitiveGoal) this).goal.name ;
    	}
    	if (shortdesc == null)
    		return "" + combinator ;
    	else return "" + combinator + " (" + shortdesc + ")" ;
    }

    /**
     * Assuming this goal is closed (that is, it has been solved or failed), this
     * method will return the next {@link PrimitiveGoal} to solve. The method will
     * traverse up through the parent of this GoalStructure to look for this next
     * goal. If none is found, null is returned.
     * 
     * <p>
     * If a new {@link PrimitiveGoal} can be found, it will be adopted. So, budget
     * will also be allocated for it. Recursively, all its ancestors that just
     * become current will also get freshly allocated budget.
     */
    @SuppressWarnings("incomplete-switch")
	PrimitiveGoal getNextPrimitiveGoal_andAllocateBudget() {

        if (status.inProgress())
            // this method should not be called on a goal-structure that is still in
            // progress
            throw new IllegalArgumentException();

        if (isRootGoal())
            return null;

        // So... this goal structure is either solved or failed, and is not the top-goal

        if (parent.status.success() || parent.status.failed())
            return parent.getNextPrimitiveGoal_andAllocateBudget();

        // this case implies: (1) the parent goal is still open/in-progress, and
        // (2) the parent must have some budget left!

        switch (parent.combinator) {
        case SEQ:
            // Since the parent is still open, it follows that this goal cannot be failed.
            // In other words, this goal must be successful. Furthermore, it cannot be the
            // last goal of the SEQ, because then the whole SEQ would be successful as well.
            //
            // So.. we can simplify this case:

            // if(status.failed())
            // this case should have caught by the if-parent above; as it implies that the
            // parent also failed
            // return parent.getNextPrimitiveGoal_andAllocateBudget() ;
            // else: so, this goal is solved:
            int k = parent.subgoals.indexOf(this);
            // if (k == parent.subgoals.size() - 1 )
            // this case should have been caught by the if-parent case above; as it implies
            // that the parent succeeded
            // return parent.getNextPrimitiveGoal_andAllocateBudget() ;
            // else
            return parent.subgoals.get(k + 1).getDeepestFirstPrimGoal_andAllocateBudget();

        case FIRSTOF:
            // Since the parent is still open, it follows that this goal cannot be
            // successful.
            // In other words, this goal must be failed. Furthermore, it cannot be the
            // last goal of the FIRSTOF, because then the whole FIRSTOF would be failed as
            // well.
            //
            // So.. we can simplify this case:

            // if(status.success())
            // this case should have been caught by the if-parent case above; as it implies
            // that the parent succeeded
            // return parent.getNextPrimitiveGoal_andAllocateBudget() ;
            // else: so, this goal failed:
            k = parent.subgoals.indexOf(this);
            // if (k == parent.subgoals.size() - 1 )
            // this case should have caught by the if-parent above; as it implies that the
            // patent failed
            // return parent.getNextPrimitiveGoal_andAllocateBudget() ;
            // else
            return parent.subgoals.get(k + 1).getDeepestFirstPrimGoal_andAllocateBudget();
        case REPEAT:
            // Since the parent is still open, it follows that this goal cannot be
            // successful.
            // In other words, this goal must be failed, and furthermore its REPEAT parent
            // still have some budget (otherwise the parent would be failed).
            //
            // so we can simplify this to the following:

            // first reset the status of this goal-structure and its descendants to
            // in-progress:
            this.makeInProgressAgain();
            // then get the first primitive goal:
            return this.getDeepestFirstPrimGoal_andAllocateBudget();
        }
        // this case should not happen
        return null;
    }

    PrimitiveGoal getDeepestFirstPrimGoal_andAllocateBudget() {
        // allocate budget:
        if (isRootGoal()) {
            budget = Math.min(bmax, budget);
        } else {
            budget = Math.min(bmax, parent.budget);
        }
        // find the first deepest primitive subgoal:
        if (this instanceof PrimitiveGoal) {
            return (PrimitiveGoal) this;
        } else {
            return subgoals.get(0).getDeepestFirstPrimGoal_andAllocateBudget();
        }
    }
    
    /**
     * Apply a depth-first=-search, to find the first descedant (child or lower)
     * subgoal G that is concluded and has its auto-remove flag turned on. There
     * should be at most just one such G.
     * Note that this method won't mark  "this" goal itself as G (G has to be a
     * child or lower).
     * 
     * <p>It is an error if auto-removal would cause a goal to have an empty
     * list of subgoals.
     */
    GoalStructure get_Concluded_AutoRemove_Subgoal() {
    	for(var G : subgoals) {
    		if(!G.status.inProgress() && G.autoRemove) {
    			// find a subgoal that should be removed
    			return G ;
    		}
    	}
    	// else none of the subgoals are themselves removable;
    	// we search deeper:
    	for(var G : subgoals) {
    		GoalStructure GoalStructure = G.get_Concluded_AutoRemove_Subgoal() ;
    		if (GoalStructure != null) {
    			return GoalStructure ;
    		}
    	}
    	return null ;
    }

    void makeInProgressAgain() {
        status.resetToInProgress();
        for (GoalStructure G : subgoals)
            G.makeInProgressAgain();
    }

    /**
     * Check if this goal structure is a descendant of G. It is it is G itself, or
     * if its parent is a descendant of G.
     */
    boolean isDescendantOf(GoalStructure G) {
        if (this == G)
            return true;
        if (parent == null)
            return false;
        return parent.isDescendantOf(G);
    }

    public GoalStructure maxbudget(double b) {
        if (b <= 0 || !Double.isFinite(b))
            throw new IllegalArgumentException();
        bmax = b;
        return this;
    }

    /**
     * Register that the agent has consumed the given amount of budget. Delta will
     * be subtracted from this goal-structure's budget, as well as that of its
     * ancestors.
     */
    void registerConsumedBudget(double delta) {
        consumedBudget += delta;
        budget -= delta;
        if (!isRootGoal())
            parent.registerConsumedBudget(delta);
    }

    void registerUsedTime(long duration) {
        consumedTime += duration;
        if (!isRootGoal())
            parent.registerUsedTime(duration);
    }

    /**
     * Return the remaining budget for this goal structure.
     */
    public double getBudget() {
        return budget;
    }

    /**
     * Return the agent's maximum allowed budget each time the goal is adopted.
     */
    public double getMaxBudgetAllowed() {
        return bmax;
    }

    private String space(int k) {
        String s = "";
        for (int i = 0; i < k; i++)
            s += " ";
        return s;
    }

    String showGoalStructureStatusWorker(int level) {
        String indent = space(4 * (level + 1));
        String s = "";
        if (this instanceof PrimitiveGoal) {
            s += indent + "(" + status + ") Goal " + ((PrimitiveGoal) this).goal.getName() ;
        } else {
            s += indent + this.getName() + ": " + status + ", #children=" + subgoals.size();
        }
        s += "\n" + indent + "  Budget=" + budget;
        if (bmax < Double.POSITIVE_INFINITY)
            s += "(max=" + bmax + ")";
        s += ", consumed=" + consumedBudget + "\n";
        for (GoalStructure gt : subgoals)
            s += gt.showGoalStructureStatusWorker(level + 1);
        return s;
    }

    private String indent(int indentation, String s) {
        String[] lines = s.split("\n");
        String z = "";
        for (int k = 0; k < lines.length; k++) {
            z += space(k);
            z += lines[k];
            if (k > 0)
                z += "\n";
        }
        return z;
    }

    /**
     * Format a summary of the state of this GoalStructure to a readable string.
     */
    public String showGoalStructureStatus() {
        return showGoalStructureStatusWorker(0);
    }

    /**
     * Print a summary of the state of this GoalStructure.
     */
    public void printGoalStructureStatus() {
        System.out.println("\n** Goal status:");
        System.out.println(showGoalStructureStatus());
    }

    /**
     * A special subclass of {@link GoalStructure} to represent a leaf, wrapping
     * around an instance of {@link Goal}.
     */
    static public class PrimitiveGoal extends GoalStructure {
        
    	Goal goal;

        /**
         * Create an instance of PrimitiveGoal, wrapping around the given {@link Goal}.
         */
        public PrimitiveGoal(Goal g) {
            super();
            combinator = GoalsCombinator.PRIMITIVE;
            goal = g;
        }

        // need to override these three methods to set the goal status... because we
        // maintain it
        // using two fields!! ANTI PATERN :( Should fix this...

        @Override
        void setStatusToFail(String reason) {
            goal.status.setToFail(reason);
            super.setStatusToFail(reason);
        }

        @Override
        void setStatusToSuccess(String info) {
            goal.status.setToSuccess(info);
            super.setStatusToSuccess(info);
        }

        @Override
        void makeInProgressAgain() {
            goal.status.resetToInProgress();
            super.makeInProgressAgain();
        }
        
        public Goal getGoal() {
        	return goal ;
        }

    }

}
