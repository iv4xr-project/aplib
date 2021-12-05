package nl.uu.cs.aplib.mainConcepts;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * 
 * A Tactic is needed to solve a {@link Goal}. There are the following types of
 * tactics:
 * 
 * <ol>
 * <li>A PRIMITIVE tactic, consist of just a single {@link Action}. If invoked,
 * this tactic will execute the Action, if the latter is enabled in the current
 * agent's state.
 * 
 * <li>A tactic T of the type FIRSTOF(T1,T2,...) where T1,T2,.. are tactics.
 * Executing T will execute <b>the first</b> subtactic (so, T1 or T2 or ...) in
 * the given order that has an enabled action to be executed in the current
 * state.
 * 
 * <li>A tactic T of the type ANYOF(T1,T2,...) where T1,T2,.. are tactics.
 * Executing T will execute one of the subtactic that has an enabled action to
 * be executed in the current state.
 * 
 * <li>A tactic T of the type SEQ(T1,T2,T3,...) where T1,T2,.. are tactics.
 * Executing T will execute the subtactics in sequence. Note however that an
 * agent (instance of {@link BasicAgent}) always executes a tactic one action
 * per tick. For example, if T1 is a SEQ of three actions, and T2 is an ANYOF
 * two actions, and T3 is a single action, then T2 will execute at the 4th tick,
 * and T3 at the 5th tick.
 * 
 * </ol>
 * 
 * @author Wish
 *
 */

public class Tactic {

    /**
     * Four types of {@link Tactic}. {@see Tactic}.
     */
    static public enum TacticType {
        FIRSTOF, ANYOF, SEQ, PRIMITIVE
    }

    Tactic parent = null;
    List<Tactic> subtactics;
    TacticType strTy;

    /**
     * Construct a new Tactic of the given type, with the given subtactics.
     */
    public Tactic(TacticType type, Tactic... subtactics) {
        strTy = type;
        this.subtactics = new LinkedList<Tactic>();
        for (Tactic p : subtactics) {
            this.subtactics.add(p);
            p.parent = this;
        }
    }

    /**
     * Given a state, this method returns the set of actions in this tactic which
     * are both eligible for executions and whose guard are true on the state.
     */
    List<PrimitiveTactic> getFirstEnabledActions(SimpleState agentstate) {

        List<PrimitiveTactic> actions = new LinkedList<PrimitiveTactic>();
        switch (strTy) {
        case FIRSTOF:
            for (Tactic PT : subtactics) {
                actions = PT.getFirstEnabledActions(agentstate);
                if (!actions.isEmpty())
                    return actions;
            }
            return actions;
        case ANYOF:
            for (Tactic PT : subtactics) {
                actions.addAll(PT.getFirstEnabledActions(agentstate));
            }
            return actions;
        case SEQ:
            return subtactics.get(0).getFirstEnabledActions(agentstate);
        case PRIMITIVE:
            var this_ = (PrimitiveTactic) this;
            if (this_.action.isEnabled(agentstate))
                actions.add(this_);
            return actions;
        }
        // should not happen:
        return null;
    }

    /**
     * Suppose this tactic is done/completed. This method calculates the next tactic
     * to execute.
     * 
     * <p>
     * Note: if the top tactic consists of only FIRSTof and ANYof nodes, and no
     * actions are persistent, this method should return null. Else the next tactic
     * is determined by the presence of SEQ and uncompleted persistent actions.
     */
    Tactic calcNextTactic() {

        if (parent == null)
            // the root tactic itself cannot have any next-tactic:
            return null;

        if (strTy == TacticType.PRIMITIVE) {
            var this_ = (PrimitiveTactic) this;
            // well, if the current action is not completed yet, stay on it:
            if (!this_.action.isCompleted())
                return this;
        }

        switch (parent.strTy) {
        case FIRSTOF:
            return parent.calcNextTactic();
        case ANYOF:
            return parent.calcNextTactic();
        case SEQ:
            int k = parent.subtactics.indexOf(this);
            if (k == parent.subtactics.size() - 1)
                return parent.calcNextTactic();
            else
                return parent.subtactics.get(k + 1);
        }
        // should not arrive here:
        return null;
    }

    /**
     * Write some basic statistics of this Tactic (e.g. the number of times each
     * Action in this Tactic has been invoked, and its total running time) to a
     * String.
     */
    public String showStatistics() {
        String s = "";
        if (parent == null) {
            // root-tactic
            s += "\n   tot. #invoked: " + totInvocation();
            s += "\n   tot. #used time: " + totRuntime() + " (ms)";
        }
        if (this instanceof PrimitiveTactic) {
            var action = ((PrimitiveTactic) this).action;
            s += "\n   action: " + action.name + "\n     #invoked: " + action.invocationCount + "\n     used time: "
                    + action.totalRuntime + " (ms)";
            return s;
        }
        for (Tactic S : subtactics) {
            s += S.showStatistics();
        }
        return s;
    }

    int totInvocation() {
        if (this instanceof PrimitiveTactic) {
            return ((PrimitiveTactic) this).action.invocationCount;
        }
        return subtactics.stream().mapToInt(T -> T.totInvocation()).sum();
    }

    long totRuntime() {
        if (this instanceof PrimitiveTactic) {
            return ((PrimitiveTactic) this).action.totalRuntime;
        }
        return subtactics.stream().mapToLong(T -> T.totRuntime()).sum();
    }

    public void resetStatistics() {
        if (this instanceof PrimitiveTactic) {
            var action = ((PrimitiveTactic) this).action;
            action.totalRuntime = 0;
            action.invocationCount = 0;
        }
    }

    /**
     * Print some basic statistics of this tactic (e.g. the number of times each
     * Action in this tactic has been invoked, and its total running time) to the
     * console.
     */
    public void printStatistics() {
        System.out.println("** Actions statistics:");
        System.out.println(showStatistics());
    }

    /**
     * A subclass of {@link Tactic} representing a single {@link Action}. It
     * bassically just wraps the Action.
     */
    static public class PrimitiveTactic extends Tactic {
        Action action;

        /**
         * Construct a PrimitiveTactic by wrapping around the given {@link Action}.
         */
        public PrimitiveTactic(Action a) {
            super(TacticType.PRIMITIVE);
            action = a;
        }

        /**
         * Set the given predicate as the guard of the Action that underlies this
         * PrimitiveTactic. The method returns this instance of PrimitiveTactic so that
         * it can be used in the Fluent Interface style.
         */
        public <AgentSt> PrimitiveTactic on_(Predicate<AgentSt> guard) {
            action.on_(guard);
            return this;
        }

        public <AgentSt, QueryResult> PrimitiveTactic on(Function<AgentSt, QueryResult> guard) {
            action.on(guard);
            return this;
        }
    }

}
