package nl.uu.cs.aplib;

import java.util.function.Function;
import java.util.function.Predicate;

import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.*;
import nl.uu.cs.aplib.mainConcepts.Tactic.*;
import nl.uu.cs.aplib.utils.Pair;

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
     * Let H be the goal structure created by REPEAT(G). Executing H will 
     * repeatedly try G until it succeeds. Then H succeeds. H fails if it
     * runs out of budget.
     */
    public static GoalStructure REPEAT(GoalStructure subgoal) {
        return new GoalStructure(GoalsCombinator.REPEAT, subgoal);
    }
    
    /**
     * REPEAT(a,G,p) will repeatedly try G, until p is true. The guard
     * p is checked "after" G: this means after G is concluded with either
     * success of failed.
     * Unlike the standard REPEAT, we do not stops when G succeeds. The iteration
     * steps when at the end of G, g holds on the resulting state.
     */
    public static <State> GoalStructure REPEAT(
    		BasicAgent agent,
    		GoalStructure subgoal, 
    		Predicate<State> g) {
        return REPEAT(
        		  SEQ(FIRSTof(subgoal,SUCCESS()),
        		      IF(agent,g,SUCCESS(),FAIL()))        		  
        	   ) ;
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
     * Turn an action into a goal. The goal itself will always succeeds. If the action
     * is enabled, the action will be  executed (once), that would then automatically 
     * solves this goal. If the action is not enabled, then nothing happens (the goal
     * is kept, and it is not solved).
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
     * A goal structure that does nothing but printing a message for debugging purpose.
     */
    public static <State> GoalStructure DEBUG(String debugstring) {
    	return goal("debug-goal").toSolve((State belief) -> true)
    			.withTactic(action("").do1((State state) -> { System.out.println(debugstring) ; return state ; } ).lift()).lift();
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
    
    
    @Deprecated
    /**
     * Deprecated. Use {@link AplibEDSL#WHILE(BasicAgent, Predicate, GoalStructure)}
     * instead.
     * 
     * <p> Repeatedly trying to solve a goal g, while the given predicate p is true. More
     * precisely, the agent first checks the given guard predicate p. If it does not
     * hold, the loop ends. Else, it makes the subgoal current and tries to solve it.
     * If this subgoal is solved, the loop ends. Else we repeat the above steps.
     * 
     * <p>If the agent runs out of the budget to do the loop, it also leaves the loop.
     * 
     * <p> Note that WHILEDO(p,g) does not behave as the usual while-loop, as WHILEDO
     * also breaks the loop when the body g succeeds.
     */
    public static <State> GoalStructure WHILEDO(Predicate<State> p, GoalStructure subgoal) {
        GoalStructure not_g = lift((State state) -> !p.test(state));
        return REPEAT(FIRSTof(not_g, subgoal));
    }
    
    /**
     * WHILE(a,p,G) will repeatedly try G, while p is true. The guard
     * p is checked at the start of every iteration, and "after" G: 
     * this means after G is concluded with either success of failed.
     * Unlike the standard REPEAT, we do not stops when G succeeds. The iteration
     * steps when at the end of G, g holds on the resulting state.
     */
    public static <State> GoalStructure WHILE(
    						BasicAgent agent,
    						Predicate<State> g, 
    						GoalStructure subgoal) {
        return IF(agent,
        		  g,
        		  REPEAT(agent,subgoal,g), 
        		  SUCCESS()) ;
    }
    
    /**
     * When this goal structure is executed, it first apply the query function q
     * on the current agent state S to obtain a value a. This value can be
     * null to indicate that the query is not successful.
     * 
     * <p> If a is not null, g1(a) will be deployed as the next goal to solve.
     * Else g2 is deployed.
     */
    public static <State,QueryResult> GoalStructure IF(
    		BasicAgent agent,
    		Function<State,QueryResult> q, 
    		Function<QueryResult,GoalStructure> g1, 
    		GoalStructure g2) {
    	
    	return DEPLOY(agent, (State S) ->  {
    		QueryResult a = q.apply(S) ;
    		if (a!=null) 
    			 return g1.apply(a) ; 
    		else return g2 ;
    		}) ;    	
    }
    
    /**
     * When this goal structure is executed, it evaluates p on the current state.
     * If it is true, g1 will be deployed as the next goal. Else g2 is deployed
     * as the next goal.
     */
    public static <State> GoalStructure IF(
    		BasicAgent agent,
    		Predicate<State> p, 
    		GoalStructure g1, 
    		GoalStructure g2) {
    	return DEPLOY(agent, (State S) ->  p.test(S) ? g1 : g2) ;    	
    }
    

    @Deprecated
    /**
     * Deprecated. Use {@link AplibEDSL#TRYIF(Predicate, GoalStructure, GoalStructure)}
     * instead.
     * 
     * <p>If this goal becomes current, it will evaluate the current state. If p holds,
     * it will continue with the goal g1 as the goal to solve. if p does not hold,
     * of SEQ(p,g1) failed, g2 is tried.
     * 
     * <p>Note that IFELSE does not behave as the usual if-then-else as IFELSE can
     * still do the else-part g2 even if p is true, namely if the then-branch g1
     * subsequently fails.
     */
    public static <State> GoalStructure IFELSE(Predicate<State> p, GoalStructure g1, GoalStructure g2) {
        GoalStructure not_g = lift((State state) -> p.test(state));
        return FIRSTof(SEQ(lift(p), g1), g2);
    }
    
    
    /**
     * If this goal becomes current, it will evaluate the current state. 
     * 
     * <ol>
     * <li> If p holds on the current state, the agent will then try the goal g1. 
     * If g1 is solved we are done. If g1 fails, g2 is tried.
     * 
     * <li> If p does not hold, we do g2.
     * </ol>
     */
    public static <State> GoalStructure TRYIF(Predicate<State> p, GoalStructure g1, GoalStructure g2) {
        GoalStructure not_g = lift((State state) -> p.test(state));
        return FIRSTof(SEQ(lift(p), g1), g2);
    }
    
    /**
     * The combinator will "dynamically" deploy a goal to be executed/adopted after executing this
     * combinator. The parameter dynamic goal takes the agent current state and constructs a goal H
     * based on it, and this H is the one that is deployed. Notice that the kind of H produced can thus
     * be made dependent on the current agent state.
     * 
     * <p>The new goal H is only deployed once; so if this structure is iterated, it will not deploy
     * another new goal.
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
     * The combinator will "dynamically" deploy a goal to be executed/adopted after executing this
     * combinator. The paramerter dynamic goal takes the agent current state and constructs a goal H
     * based on it, and this H is the one that is deployed. Notice that the kind of H produced can thus
     * be made dependent on the current agent state.
     * 
     * After H is executed, it is removed from the goal-tree.
     */
    public static <AgentState> GoalStructure  DEPLOY(BasicAgent agent, Function<AgentState,GoalStructure> dynamicgoal) {
        GoalStructure[] newGoal = { null } ; 
        GoalStructure G = goal("deploy a goal")
                .toSolve((AgentState state) -> true)
                .withTactic(
                        action("deploying a goal")
                            .do1((AgentState state) -> {
                            	newGoal[0] = dynamicgoal.apply(state) ;
                                agent.addAfter(newGoal[0]);
                                return state ;
                                })
                            .lift())
                .lift();
        
        Function<Void,GoalStructure> remove = dummy -> 
        	  goal("removing the new goal after completion")
        		.toSolve((AgentState state) -> true)
        		.withTactic(action("removing a goal")
        				.do1((AgentState state) -> {
        					if (newGoal[0] != null) 
        					    agent.remove(newGoal[0]) ;
        					return state ;
        				})
        				.lift()
        				)
        		.lift()  ;
        	  
        return FIRSTof(
        		   SEQ(G,remove.apply(null)), 
        		   // always remove the new goal again, but if the new goal failed we will also
        		   // make the whole construct fail:
        		   SEQ(remove.apply(null),FAIL())) ;  
    }
    
    /**
     * Construct a goal that will be interrupted on a certain conditions. The goal is
     * aborted, and a new goal is launched. When the new goal is done (success or fail),
     * the execution continues with the original goal.
     * 
     * <p>For example: 
     * 
     *      <p>INTERRUPTIBLE(g, HANDLER(c1,H1), HANDLER(c2,H2))
     *      
     * <p>Will abort the execution of g when the condition c1 or c2 becomes true. If c1 is true,
     * we then proceed with the goal H1. When H1 is done, either in success or failure, the
     * execution of g is resumed.  
     * 
     * <p>The goal structure constructed with INTERRUPTIBLE, if it terminates within budget,
     * always terminate successfully, even if g itself fails. To check if g was actually
     * successful, you can check its predicate again after the INTERRUPTIBLE. 
     */
    public static <State> GoalStructure INTERRUPTIBLE(Goal g, 
    		Pair<Predicate<State>,GoalStructure>... handlers) {
    	
    	Tactic originalTactic = g.getTactic() ;
    	
    	Predicate<State> guard = S -> {
    		for(var h : handlers) {
    			if (h.fst.test(S)) return true ;
    		}
    		return false ;
    	} ;
    	
    	Tactic exceptional = Abort().on_(guard).lift() ;
    			
    	Goal g2 = g.withTactic(FIRSTof(exceptional,originalTactic)) ;
    	
    	GoalStructure H = FAIL() ;
    	for(int k = handlers.length-1 ; 0<=k; k--) {
    		var H_k = SEQ(handlers[k].snd)  ;
    		var condition = handlers[k].fst ;
    		H = TRYIF(condition,H_k,H) ;
    	}
    	H = SEQ(H,FAIL()) ;
    	
    	return REPEAT(
    			   FIRSTof(
    			      g2.lift(),
    			      SEQ(lift((State S) -> ! guard.test(S)), SUCCESS()),
    			      H)) ;
    }
    
    /**
     * Just for representing a pair (p,G) of state-predicate and a goal structure.
     */
    public static <State> Pair<Predicate<State>,GoalStructure> HANDLE(Predicate<State> p ,GoalStructure handler) {
    	return new Pair<>(p,handler) ;
    }

    /**
     * Create a blank instance of {@link nl.uu.cs.aplib.mainConcepts.Goal} with the
     * given name.
     */
    public static Goal goal(String name) {
        return new Goal(name);
    }

    /**
     * Create a blank {@link nl.uu.cs.aplib.mainConcepts.Action} with the given
     * name.
     */
    public static Action action(String name) {
        return new Action(name);
    }
    
    /**
     * Create a blank {@link nl.uu.cs.aplib.mainConcepts.Action}.
     */
    public static Action action() {
        return new Action("");
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
        return Abort().lift() ; 
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
    

}
