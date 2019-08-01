package nl.uu.cs.aplib;

import nl.uu.cs.aplib.MainConcepts.*;
import nl.uu.cs.aplib.MainConcepts.GoalTree.*;
import nl.uu.cs.aplib.MainConcepts.Strategy.*;

/**
 * Provide a set of convenience static methods to be used as operators/combinators for constructing
 * {@link nl.uu.cs.aplib.MainConcepts.GoalTree} and§  {@link nl.uu.cs.aplib.MainConcepts.Strategy}. 
 * 
 * @author wish
 *
 */
public class AplibEDSL {
	
	
	/**
	 * Create a SEQ type {@link nl.uu.cs.aplib.MainConcepts.GoalTree}.
	 */
	static public GoalTree SEQ(GoalTree ... subgoals) {
		return new GoalTree(GoalsCombinator.SEQ, subgoals) ;
	}

	/**
	 * Create a FIRSTof type {@link nl.uu.cs.aplib.MainConcepts.GoalTree}.
	 */
	static public GoalTree FIRSTof(GoalTree ... subgoals) {
		return new GoalTree(GoalsCombinator.FIRSTOF, subgoals) ;
	}
	
	/**
	 * Create a blank instance of {@link nl.uu.cs.aplib.MainConcepts.Goal} with the given name.
	 */
	static public Goal goal(String name) { 
		return new Goal(name) ;
	}
	
	/**
	 * Lift a Goal to become a {@link nl.uu.cs.aplib.MainConcepts.GoalTree}.
	 */
	static public GoalTree lift(Goal g) {
		return new PrimitiveGoal(g) ;
	}
	
	/**
	 * Create a blank {@link nl.uu.cs.aplib.MainConcepts.Action} with the given name.
	 */
	static public Action action(String name) {
		return new Action(name) ;
	}
	
	/**
	 * To construct a FIRSTof {@link nl.uu.cs.aplib.MainConcepts.Strategy}.
	 */
	static public Strategy FIRSTof(Strategy ... strategies) {
		return new Strategy(StrategyType.FIRSTOF, strategies) ;
	}
	
	/**
	 * Creating an Abort action ({@see nl.uu.cs.aplib.MainConcepts.Action.Abort})
	 */
	static public Action ABORT() { return new Action.Abort() ; }
	
	/**
	 * To construct a SEQ {@link nl.uu.cs.aplib.MainConcepts.Strategy}.
	 */
	static public Strategy SEQ(Strategy ... strategies) {
		return new Strategy(StrategyType.SEQ, strategies) ;
	}
	
	/**
	 * To construct a ANYof {@link nl.uu.cs.aplib.MainConcepts.Strategy}.
	 */
	static public Strategy ANYof(Strategy ... strategies) {
		return new Strategy(StrategyType.ANYOF, strategies) ;
	}

	/**
	 * Lift an {@link nl.uu.cs.aplib.MainConcepts.Action} to become a
	 * {@link nl.uu.cs.aplib.MainConcepts.Strategy}.
	 */
	static public Strategy lift(Action a) {
		return new PrimitiveStrategy(a) ;
	}
}
