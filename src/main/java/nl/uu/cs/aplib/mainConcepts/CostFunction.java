package nl.uu.cs.aplib.mainConcepts;

/**
 * This class provides a method to calculate the budget-cost of executing an
 * action. This class will define this cost to be 1.0 for each invocation of an
 * action. You can override this class if you want to have a more complicated
 * cost function, e.g. if you want to charge the action's computation time as
 * the cost.
 * 
 * @author wish
 *
 */
public class CostFunction {

    double cost;

    /**
     * Execute an action on the given agent state, and calculate the cost of this
     * execution. The cost will be stored in the field {@link cost}. This method
     * simply defines the cost to be always 1. Override this method to define
     * different costing.
     */
    public Object executeAction_andInstrumentCost(SimpleState state, Action a) {
        cost = 0.0;
        Object proposal = a.exec1(state);
        cost = 1.0;
        return proposal;
    }

    public double getCost() {
        return cost;
    }

}
