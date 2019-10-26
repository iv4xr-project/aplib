package nl.uu.cs.aplib;

import nl.uu.cs.aplib.MainConcepts.*;
import nl.uu.cs.aplib.MainConcepts.GoalStructure.*;
import nl.uu.cs.aplib.MainConcepts.Tactic.*;

/**
 * Provide a set of convenience static methods to be used as operators/combinators for constructing
 * {@link nl.uu.cs.aplib.MainConcepts.GoalStructure} and§  {@link nl.uu.cs.aplib.MainConcepts.Tactic}. 
 * 
 * @author wish
 *
 */
public class AplibEDSL {
	
	
	/**
	 * Create a SEQ type {@link nl.uu.cs.aplib.MainConcepts.GoalStructure}.
	 */
	static public GoalStructure SEQ(GoalStructure ... subgoals) {
		return new GoalStructure(GoalsCombinator.SEQ, subgoals) ;
	}

	/**
	 * Create a FIRSTof type {@link nl.uu.cs.aplib.MainConcepts.GoalStructure}.
	 */
	static public GoalStructure FIRSTof(GoalStructure ... subgoals) {
		return new GoalStructure(GoalsCombinator.FIRSTOF, subgoals) ;
	}
	
	/**
	 * Create a blank instance of {@link nl.uu.cs.aplib.MainConcepts.Goal} with the given name.
	 */
	static public Goal goal(String name) { 
		return new Goal(name) ;
	}
	
	/**
	 * Lift a Goal to become a {@link nl.uu.cs.aplib.MainConcepts.GoalStructure}.
	 */
	static public PrimitiveGoal lift(Goal g) {
		return g.lift() ;
	}
	
	/**
	 * Create a blank {@link nl.uu.cs.aplib.MainConcepts.Action} with the given name.
	 */
	static public Action action(String name) {
		return new Action(name) ;
	}
	
	/**
	 * To construct a FIRSTof {@link nl.uu.cs.aplib.MainConcepts.Tactic}.
	 */
	static public Tactic FIRSTof(Tactic ... strategies) {
		return new Tactic(TacticType.FIRSTOF, strategies) ;
	}
	
	/**
	 * Creating an Abort action ({@see nl.uu.cs.aplib.MainConcepts.Action.Abort})
	 */
	static public Action Abort() { return new Action.Abort() ; }
	
	/**
	 * Creating a {@link nl.uu.cs.aplib.MainConcepts.PrimitiveTactic} that
	 * wraps over an Abort action.
	 */
	static public PrimitiveTactic ABORT() { return lift(new Action.Abort()) ; }
	
	/**
	 * To construct a SEQ {@link nl.uu.cs.aplib.MainConcepts.Tactic}.
	 */
	static public Tactic SEQ(Tactic ... strategies) {
		return new Tactic(TacticType.SEQ, strategies) ;
	}
	
	/**
	 * To construct a ANYof {@link nl.uu.cs.aplib.MainConcepts.Tactic}.
	 */
	static public Tactic ANYof(Tactic ... strategies) {
		return new Tactic(TacticType.ANYOF, strategies) ;
	}

	/**
	 * Lift an {@link nl.uu.cs.aplib.MainConcepts.Action} to become a
	 * {@link nl.uu.cs.aplib.MainConcepts.PrimitiveTactic}.
	 */
	static public PrimitiveTactic lift(Action a) {
		return new PrimitiveTactic(a) ;
	}
}
