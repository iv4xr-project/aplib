package nl.uu.cs.aplib.MainConcepts;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import nl.uu.cs.aplib.MainConcepts.GoalTree.GoalsCombinator;


/**
 * 
 * A Strategy is needed to solve a {@link Goal}. There are the following
 * types of strategies:
 * 
 * <ol>
 *    <li> A PRIMITIVE strategy, consist of just a single {@link Action}.
 *    If invoked, this strategy will execute the Action, if the latter is
 *    enabled in the current agent's state.
 * 
 *    <li> A strategy s of the type FIRSTOF(s1,s2,...) where s1,s2,.. are
 *    strategies. Executing s will execute <b>the first</b> sub-strategy (so, s1 or
 *    s2 or ...) in the given order that has an enabled action to be executed
 *    in the current state.
 *         
 *    <li> A strategy s of the type ANYOF(s1,s2,...) where s1,s2,.. are
 *    strategies. Executing s will execute one of the sub-strategy
 *    that has an enabled action to be executed
 *    in the current state.
 *        
 *    <li>  A strategy s of the type SEQ(s1,s2,s3,...) where s1,s2,.. are
 *    strategies. Executing s will execute the sub-strategies in sequence.
 *    Note however that an agent (instance of {@link SimpleAgent}) execute
 *    a strategy always one action per tick. For example, if s1 is a SEQ and three
 *    Actions, and s2 is an ANYOF two actions, and s3 is a single action,
 *    s2 will execute at the 4th tick, and s3 at the 5th tick.
 *    
 * </ol>   
 * 
 * @author Wish
 *
 */

public class Strategy {
	
	/**
	 * Four types of {@link Strategy}. {@see Strategy}.
	 */
	static public enum StrategyType { FIRSTOF, ANYOF, SEQ, PRIMITIVE } 
	
	Strategy parent = null ;
	List<Strategy> substrategies ;
	StrategyType strTy ;
	
	/**
	 * Construct a new Strategy of the given type, with the given sub-strategies.
	 */
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
	 * Suppose this strategy is done/completed. This method calculates the next
	 * strategy to execute. If the top strategy consists of only FIRSTof and ANYof
	 * nodes, and no actions are persistent, this should return null. Else the next
	 * Strategy is determined by the presence of SEQ and uncompleted persistent
	 * actions.
	 */
	Strategy calcNextStrategy() {
		
		if (parent == null)
			// the root strategy itself cannot have any next-strategy:
			return null ; 
		
	    if (strTy == StrategyType.PRIMITIVE) {
	    	var this_ = (PrimitiveStrategy) this ;
	    	// well, if the current action is not completed yet, stay on it:
	    	if (! this_.action.isCompleted()) return this ; 
	    }
		
	    switch(parent.strTy) {
	       case FIRSTOF : return parent.calcNextStrategy() ;
	       case ANYOF   : return parent.calcNextStrategy() ;
	       case SEQ     : int k = parent.substrategies.indexOf(this) ;
			              if (k == parent.substrategies.size() - 1) 
				              return parent.calcNextStrategy() ;
			              else
				              return parent.substrategies.get(k+1) ;
	    }
        // should not arrive here:
		return null ;
	}
	
	/**
	 * Write some basic statistics of this Strategy (e.g. the number of times
	 * each Action in this Strategy has been invoked, and its total running time)
	 * to a String.
	 */
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
	
	/**
	 * Print some basic statistics of this Strategy (e.g. the number of times
	 * each Action in this Strategy has been invoked, and its total running time)
	 * to the console.
	 */
	public void printActionsStatistics() {
		System.out.println("** Actions statistics:") ;
		System.out.println(showActionsStatistics()) ;
	}
	
	/**
	 * A subclass of {@link Strategy} representing a single {@link Action}. It bassically
	 * just wraps the Action.
	 */
	static public class PrimitiveStrategy extends Strategy {
		Action action ;
		
		/**
		 * Construct a PrimitiveStrategy by wrapping around the given {@link Action}.
		 */
		public PrimitiveStrategy(Action a) { 
			super(StrategyType.PRIMITIVE) ;
			action = a ; 
		}
		
		/**
		 * Set the given predicate as the guard of the Action that underlies this
		 * PrimitiveStrategy. The method returns this instance of PrimitiveStrategy
		 * so that it can be used in the Fluent Interface style.
		 */
		public <AgentSt> PrimitiveStrategy on_(Predicate<AgentSt> guard) { 
			action.on_(guard) ;
			return this ;
		}
		
		
	}
	

}
