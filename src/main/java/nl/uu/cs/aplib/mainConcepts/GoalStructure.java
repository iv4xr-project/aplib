package nl.uu.cs.aplib.mainConcepts;

import java.util.*;
import java.util.stream.Collectors;

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
    public boolean isTopGoal() {
        return parent == null;
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
    void setStatusToSuccess(String info) {
        status.setToSuccess(info);
        if (!isTopGoal()) {
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
    void setStatusToFail(String reason) {
        status.setToFail(reason);
        if (!isTopGoal()) {
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
    PrimitiveGoal getNextPrimitiveGoal_andAllocateBudget() {

        if (status.inProgress())
            // this method should not be called on a goal-structure that is still in
            // progress
            throw new IllegalArgumentException();

        if (isTopGoal())
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
        if (isTopGoal()) {
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
        if (!isTopGoal())
            parent.registerConsumedBudget(delta);
    }

    void registerUsedTime(long duration) {
        consumedTime += duration;
        if (!isTopGoal())
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
        String indent = space(3 * (level + 1));
        String s = "";
        if (this instanceof PrimitiveGoal) {
            s += indent + "Goal " + ((PrimitiveGoal) this).goal.getName() + ": " + status;
        } else
            s += indent + combinator + ": " + status;
        if (bmax < Double.POSITIVE_INFINITY)
            s += "\n" + indent + "Max. budget:" + bmax;
        s += "\n" + indent + "Budget: " + budget;
        s += "\n" + indent + "Consumed budget:" + consumedBudget + "\n";
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

    }

}
