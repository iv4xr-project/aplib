package nl.uu.cs.aplib.MainConcepts;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

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
	List<PrimitiveStrategy> getFirstEnabledActions(SimpleState agentstate) {
		
		List<PrimitiveStrategy> actions = new LinkedList<PrimitiveStrategy>() ;
		switch(strTy) {
		   case FIRSTOF : for (Strategy PT : substrategies) {
				             actions = PT.getFirstEnabledActions(agentstate) ;
				             if (! actions.isEmpty()) return actions ;
			              } 
			              return actions ;
		   case ANYOF   : for (Strategy PT : substrategies) {
				             actions.addAll(PT.getFirstEnabledActions(agentstate)) ;
			              }
			              return actions ;
		   case SEQ : return substrategies.get(0).getFirstEnabledActions(agentstate) ;
		   case PRIMITIVE : var this_ = (PrimitiveStrategy) this ;
		                    if (this_.action.isEnabled(agentstate)) actions.add(this_) ;
			                return actions ;
		}
		// should not happen:
		return null ;
	}
	
	/**
	 * Suppose this strategy is done/completed. This method calculates the next strategy 
	 * to execute.
	 */
	Strategy calcNextStrategy(SimpleState agentstate) {
		
		if (parent == null)
			// the root strategy itself cannot have any next-strategy:
			return null ; 
		
	    if (strTy == StrategyType.PRIMITIVE) {
	    	var this_ = (PrimitiveStrategy) this ;
	    	// well, if the current action is not completed yet, stay on it:
	    	if (! this_.action.isCompleted()) return this ; 
	    }
		
	    switch(parent.strTy) {
	       case FIRSTOF : return parent.calcNextStrategy(agentstate) ;
	       case ANYOF   : return parent.calcNextStrategy(agentstate) ;
	       case SEQ     : int k = parent.substrategies.indexOf(this) ;
			              if (k == parent.substrategies.size() - 1) 
				              return parent.calcNextStrategy(agentstate) ;
			              else
				              return parent.substrategies.get(k+1) ;
	    }
        // should not arrive here:
		return null ;
	}
	
	public String showActionsStatistics() {
		String s = "" ;
		if (this instanceof PrimitiveStrategy) {
			var action = ((PrimitiveStrategy) this).action ;
			s += "   " + action.name 
				 + "\n     #invoked : " + action.invocationCount 
				 + "\n     used time: " + action.totalRuntime + " (ms)" ;
			return s ;
		}
		for (Strategy S : substrategies) {
			s += S.showActionsStatistics() + "\n" ;
		}
		return s ;
	}
	
	public void printActionsStatistics() {
		System.out.println("** Actions statistics:") ;
		System.out.println(showActionsStatistics()) ;
	}
	
	static public class PrimitiveStrategy extends Strategy {
		Action action ;
		public PrimitiveStrategy(Action a) { 
			super(StrategyType.PRIMITIVE) ;
			action = a ; 
		}
		
		// for fluent interface
		public <AgentSt> PrimitiveStrategy on_(Predicate<AgentSt> guard) { 
			action.on_(guard) ;
			return this ;
		}
		
		
	}
	

}
