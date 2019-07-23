package nl.uu.cs.aplib.MainConcepts;

import java.util.LinkedList;
import java.util.List;

import nl.uu.cs.aplib.MainConcepts.GoalTree.GoalsCombinator;


/**
 * 
 *    (1) s = FIRSTOF(s1,s2) is enabled in the current state of either s1 or s2 is
 *        enabled. If s1 is enabled, the set of actions of s is that of s1, else
 *        that of s2.
 *        
 *    (2) s = ANYOF(s1,s2) is enabled if either s1 or s2 is enabled.  The set 
 *        of actions of s is the union of that of s1 and s2.
 *        
 *    (3) s = SEQ(s1,s2) is enabled is s1 is enabled.   
 * 
 * @author iswbprasetya
 *
 */

public class Strategy {
	
	static public enum StrategyType { FIRSTOF, ANYOF, SEQ, PRIMITIVE } 
	
	Strategy parent = null ;
	List<Strategy> substrategies ;
	StrategyType strTy ;
	
	public Strategy(StrategyType type, Strategy ... substrategies) {
		strTy = type ;
		this.substrategies = new LinkedList<Strategy>() ;
		for (Strategy p : substrategies) {
			this.substrategies.add(p) ;
			p.parent = this ;
		}
	}
	
	/**
	 * Given a state, this method returns the set of actions in this strategy
	 * which are both eligible for executions and whose guard are true on the
	 * state.
	 */
	List<PrimitiveStrategy> getEnabledActions(SimpleState agentstate) {
		
		List<PrimitiveStrategy> actions = new LinkedList<PrimitiveStrategy>() ;
		
		if (strTy == StrategyType.FIRSTOF) {
			for (Strategy PT : substrategies) {
				actions = PT.getEnabledActions(agentstate) ;
				if (! actions.isEmpty()) return actions ;
			}
			return actions ;
		}
		
		if (strTy == StrategyType.ANYOF) {
			for (Strategy PT : substrategies) {
				actions.addAll(PT.getEnabledActions(agentstate)) ;
			}
			return actions ;
		}
		
		if (strTy == StrategyType.SEQ) {
			return substrategies.get(0).getEnabledActions(agentstate) ;
		}
		// should not happen:
		return null ;
	}
	
	/**
	 * Suppose this strategy is done/completed. This method calculates the next
	 * set of enabled actions to choose.
	 */
	List<PrimitiveStrategy> getNextSetOfEnabledActions(SimpleState agentstate) {
		List<PrimitiveStrategy> actions = new LinkedList<PrimitiveStrategy>() ;
		if (parent == null) return actions ;
		if (parent.strTy == StrategyType.FIRSTOF) return parent.getEnabledActions(agentstate) ;
		if (parent.strTy == StrategyType.ANYOF) return parent.getEnabledActions(agentstate) ;
		if (parent.strTy == StrategyType.SEQ) {
			int k = parent.substrategies.indexOf(this) ;
			if (k == parent.substrategies.size() - 1) 
				return parent.getEnabledActions(agentstate) ;
			else
				return parent.substrategies.get(k+1).getEnabledActions(agentstate) ;
		}
		return null ;
	}
	
	
	
	static public class PrimitiveStrategy extends Strategy {
		Action action ;
		public PrimitiveStrategy(Action a) { 
			super(StrategyType.PRIMITIVE) ;
			action = a ; 
		}
		
		@Override
		List<PrimitiveStrategy> getEnabledActions(SimpleState agentstate) {
			var actions = new LinkedList<PrimitiveStrategy>() ;
			if (action.isEnabled(agentstate)) actions.add(this) ;
			return actions ;
		}
		
	}
	

}
