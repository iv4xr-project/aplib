package nl.uu.cs.aplib;

import java.util.function.Function;
import java.util.function.Predicate;

import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.*;
import nl.uu.cs.aplib.mainConcepts.Tactic.*;

/**
 * Provide a set of convenience static methods to be used as
 * operators/combinators for constructing
 * {@link nl.uu.cs.aplib.mainConcepts.GoalStructure} and
 * {@link nl.uu.cs.aplib.mainConcepts.Tactic}.
 * 
 * @author wish
 *
 */
public class AplibEDSL {

    AplibEDSL() {
    }

    /**
     * Create a SEQ type {@link nl.uu.cs.aplib.mainConcepts.GoalStructure}.
     */
    public static GoalStructure SEQ(GoalStructure... subgoals) {
        return new GoalStructure(GoalsCombinator.SEQ, subgoals);
    }

    /**
     * Create a FIRSTof type {@link nl.uu.cs.aplib.mainConcepts.GoalStructure}.
     */
    public static GoalStructure FIRSTof(GoalStructure... subgoals) {
        return new GoalStructure(GoalsCombinator.FIRSTOF, subgoals);
    }

    /**
     * Create a REPEAT type {@link nl.uu.cs.aplib.mainConcepts.GoalStructure}.
     */
    public static GoalStructure REPEAT(GoalStructure subgoal) {
        return new GoalStructure(GoalsCombinator.REPEAT, subgoal);
    }

    /**
     * Turn a predicate over state to become a goal. When this goal becomes current,
     * the agent will test the predicate on its state; if the predicate holds on the
     * state, the goal is solved, and else the goal is declared as failed.
     */
    public static <State> GoalStructure lift(String goalname, Predicate<State> p) {
        return goal(goalname).toSolve((Boolean b) -> b).withTactic(FIRSTof(
                action("lifting a predicate").do1((State belief) -> true).on_((State belief) -> p.test(belief)).lift(),
                ABORT())).lift();
    }
    
    public static <State> GoalStructure lift(Predicate<State> p) {
        return lift("some predicate must hold", p) ;
    }
    
    /**
     * Turn an action into a goal. The goal itself wil always succeeds. The action will be
     * executed (once), that would then automatically solves this goal.
     */
    public static <AgentState,Proposal> GoalStructure lift(String goalname, Action a) {
        return goal(goalname)
                .toSolve( (Proposal p) -> true)
                .withTactic(a.lift())
                .lift() ;
    }

    /**
     * This goal will always succeeds.
     */
    public static <State> GoalStructure SUCCESS() {
        return goal("unit-goal that always succeeds").toSolve((State belief) -> true)
                .withTactic(action("").do1((State state) -> state).lift()).lift();

    }

    /**
     * This goal will always succeeds. The goal name can be configured as given.
     */
    public static <State> GoalStructure SUCCESS(String goalname) {
        return goal(goalname).toSolve((State belief) -> true)
                .withTactic(action("").do1((State state) -> state).lift()).lift();

    }
    
    /**
     * This goal will always fail.
     */
    public static <State> GoalStructure FAIL() {
        return goal("a goal that always fail").toSolve((State belief) -> false).withTactic(ABORT()).lift();
    }
    
    /**
     * This goal will always fail. The goal name can be configured as given.
     */    
    public static <State> GoalStructure FAIL(String goalname) {
        return goal(goalname).toSolve((State belief) -> false).withTactic(ABORT()).lift();
    }
    
    
    /**
     * Repeatedly trying to solve a goal, while the given predicate is true. More
     * precisely, the agent first checks the given guard predicate g. If it does not
     * hold, the loop ends. Else, it makes the sugoal current and tries to solve it.
     * If this subgoal is solved, the loop ends. Else we repeat the above steps.
     * 
     * If the agent runs out of the budget to do the loop, it also leaves the loop.
     */
    public static <State> GoalStructure WHILEDO(Predicate<State> p, GoalStructure subgoal) {
        GoalStructure not_g = lift((State state) -> !p.test(state));
        return REPEAT(FIRSTof(not_g, subgoal));
    }

    /**
     * If this goal becomes current, it will evaluate the current state. If p holds,
     * it will continue with the goal g1 as the goal to solve, and else g2 has to be
     * solved.
     */
    public static <State> GoalStructure IFELSE(Predicate<State> p, GoalStructure g1, GoalStructure g2) {
        GoalStructure not_g = lift((State state) -> p.test(state));
        return FIRSTof(SEQ(lift(p), g1), g2);
    }
    
    /**
     * The combinator will "dynamically" deploy a goal to be executed/adopted after executing this
     * combinator. The paramerter dynamic goal takes the agent current state and constructs a goal G
     * based on it, and this G is the one that is deployed. Notice that the kind of G produced can thus
     * be made dependent on the current agent state.
     */
    public static <AgentState> GoalStructure  DEPLOYonce(BasicAgent agent, Function<AgentState,GoalStructure> dynamicgoal) {
        Boolean[] deployed = {false} ;
        GoalStructure G = goal("deploy once")
                .toSolve((AgentState state) -> false )
                .withTactic(FIRSTof(
                        action("deploying a goal")
                            .do1((AgentState state) -> {
                                agent.addAfter(dynamicgoal.apply(state));
                                deployed[0] = true ;
                                //System.out.println(">>> action: deployed[0] = " + deployed[0]) ;
                                return state ;
                                })
                            .on_(state -> ! deployed[0] )
                            .lift(),
                        ABORT())
                ).lift();
        return FIRSTof(G) ;
    }

    /**
     * Create a blank instance of {@link nl.uu.cs.aplib.mainConcepts.Goal} with the
     * given name.
     */
    public static Goal goal(String name) {
        return new Goal(name);
    }

    /**
     * Lift a Goal to become a {@link nl.uu.cs.aplib.mainConcepts.GoalStructure}.
     */
    public static PrimitiveGoal lift(Goal g) {
        return g.lift();
    }

    /**
     * Create a blank {@link nl.uu.cs.aplib.mainConcepts.Action} with the given
     * name.
     */
    public static Action action(String name) {
        return new Action(name);
    }

    /**
     * To construct a FIRSTof {@link nl.uu.cs.aplib.mainConcepts.Tactic}.
     */
    public static Tactic FIRSTof(Tactic... strategies) {
        return new Tactic(TacticType.FIRSTOF, strategies);
    }

    /**
     * Creating an Abort action ({@see nl.uu.cs.aplib.MainConcepts.Action.Abort})
     */
    public static Action Abort() {
        return new Action.Abort();
    }

    /**
     * Creating a {@link nl.uu.cs.aplib.mainConcepts.Tactic.PrimitiveTactic} that wraps
     * over an Abort action.
     */
    public static PrimitiveTactic ABORT() {
        return lift(new Action.Abort());
    }

    /**
     * To construct a SEQ {@link nl.uu.cs.aplib.mainConcepts.Tactic}.
     */
    public static Tactic SEQ(Tactic... strategies) {
        return new Tactic(TacticType.SEQ, strategies);
    }

    /**
     * To construct a ANYof {@link nl.uu.cs.aplib.mainConcepts.Tactic}.
     */
    public static Tactic ANYof(Tactic... strategies) {
        return new Tactic(TacticType.ANYOF, strategies);
    }

    /**
     * Lift an {@link nl.uu.cs.aplib.mainConcepts.Action} to become a
     * {@link nl.uu.cs.aplib.mainConcepts.Tactic.PrimitiveTactic}.
     */
    public static PrimitiveTactic lift(Action a) {
        return new PrimitiveTactic(a);
    }
    
//    /**
//     * If this goal becomes current, it will evaluate the current state. If p holds,
//     * it will continue with the goal g1 as the goal to solve, and else g2 has to be
//     * solved.
//     */
//    public static <State> Tactic IFELSE(Predicate<State> p, Tactic g1, Tactic g2) {
//        GoalStructure not_g = lift((State state) -> p.test(state));
//        
//        return FIRSTof(SEQ(lift(p), g1), g2);
//    }
    
    
    //----------------------//
    /**
     * If this goal becomes current, it will evaluate the current state. If p holds,
     * it will continue with the goal g1 as the goal to solve, and else g2 has to be
     * solved.
     */
    public static <State> GoalStructure IFELSE2(GoalStructure p, GoalStructure g1, GoalStructure g2) {
       // GoalStructure not_g = lift((State state) -> p.test(state));
        return FIRSTof(SEQ(p, g1), g2);
    }
}
