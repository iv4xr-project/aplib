package nl.uu.cs.aplib;

import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.*;
import nl.uu.cs.aplib.mainConcepts.Tactic.*;

/**
 * Provide a set of convenience static methods to be used as operators/combinators for constructing
 * {@link nl.uu.cs.aplib.mainConcepts.GoalStructure} and§  {@link nl.uu.cs.aplib.mainConcepts.Tactic}. 
 * 
 * @author wish
 *
 */
public class AplibEDSL {
	
	AplibEDSL() {}
	
	/**
	 * Create a SEQ type {@link nl.uu.cs.aplib.mainConcepts.GoalStructure}.
	 */
    public static GoalStructure SEQ(GoalStructure ... subgoals) {
		return new GoalStructure(GoalsCombinator.SEQ, subgoals) ;
	}

	/**
	 * Create a FIRSTof type {@link nl.uu.cs.aplib.mainConcepts.GoalStructure}.
	 */
    public static GoalStructure FIRSTof(GoalStructure ... subgoals) {
		return new GoalStructure(GoalsCombinator.FIRSTOF, subgoals) ;
	}
	
	/**
	 * Create a REPEAT type {@link nl.uu.cs.aplib.mainConcepts.GoalStructure}.
	 */
    public static GoalStructure REPEAT(GoalStructure subgoal) {
		return new GoalStructure(GoalsCombinator.REPEAT, subgoal) ;
	}
	
	/**
	 * Create a blank instance of {@link nl.uu.cs.aplib.mainConcepts.Goal} with the given name.
	 */
    public static Goal goal(String name) { 
		return new Goal(name) ;
	}
	
	/**
	 * Lift a Goal to become a {@link nl.uu.cs.aplib.mainConcepts.GoalStructure}.
	 */
    public static PrimitiveGoal lift(Goal g) {
		return g.lift() ;
	}
	
	/**
	 * Create a blank {@link nl.uu.cs.aplib.mainConcepts.Action} with the given name.
	 */
    public static Action action(String name) {
		return new Action(name) ;
	}
	
	/**
	 * To construct a FIRSTof {@link nl.uu.cs.aplib.mainConcepts.Tactic}.
	 */
    public static Tactic FIRSTof(Tactic ... strategies) {
		return new Tactic(TacticType.FIRSTOF, strategies) ;
	}
	
	/**
	 * Creating an Abort action ({@see nl.uu.cs.aplib.MainConcepts.Action.Abort})
	 */
    public static Action Abort() { return new Action.Abort() ; }
	
	/**
	 * Creating a {@link nl.uu.cs.aplib.MainConcepts.PrimitiveTactic} that
	 * wraps over an Abort action.
	 */
    public static PrimitiveTactic ABORT() { return lift(new Action.Abort()) ; }
	
	/**
	 * To construct a SEQ {@link nl.uu.cs.aplib.mainConcepts.Tactic}.
	 */
	public static Tactic SEQ(Tactic ... strategies) {
		return new Tactic(TacticType.SEQ, strategies) ;
	}
	
	/**
	 * To construct a ANYof {@link nl.uu.cs.aplib.mainConcepts.Tactic}.
	 */
	public static Tactic ANYof(Tactic ... strategies) {
		return new Tactic(TacticType.ANYOF, strategies) ;
	}

	/**
	 * Lift an {@link nl.uu.cs.aplib.mainConcepts.Action} to become a
	 * {@link nl.uu.cs.aplib.MainConcepts.PrimitiveTactic}.
	 */
	public static PrimitiveTactic lift(Action a) {
		return new PrimitiveTactic(a) ;
	}
}
