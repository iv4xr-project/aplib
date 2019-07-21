package nl.uu.cs.aplib;

import nl.uu.cs.aplib.MainConcepts.*;
import nl.uu.cs.aplib.MainConcepts.GoalTree.*;
import nl.uu.cs.aplib.MainConcepts.Strategy.*;

public class AplibEDSL {
	
	static public GoalTree SEQ(GoalTree ... subgoals) {
		return new GoalTree(GoalsCombinator.SEQ, subgoals) ;
	}
	
	static public GoalTree FIRSTof(GoalTree ... subgoals) {
		return new GoalTree(GoalsCombinator.FIRSTOF, subgoals) ;
	}
	
	static public Goal goal(String name) { 
		return new Goal(name) ;
	}
	
	static public GoalTree lift(Goal g) {
		return new PrimitiveGoal(g) ;
	}
	
	static public Strategy FIRSTof(Strategy ... strategies) {
		return new Strategy(StrategyType.FIRSTOF, strategies) ;
	}
	
	static public Strategy SEQ(Strategy ... strategies) {
		return new Strategy(StrategyType.SEQ, strategies) ;
	}
	
	static public Strategy ANYof(Strategy ... strategies) {
		return new Strategy(StrategyType.ANYOF, strategies) ;
	}

	static public Strategy lift(Action a) {
		return new PrimitiveStrategy(a) ;
	}
}
