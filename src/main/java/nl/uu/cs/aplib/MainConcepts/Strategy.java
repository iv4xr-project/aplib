package nl.uu.cs.aplib.MainConcepts;

import java.util.LinkedList;
import java.util.List;

import nl.uu.cs.aplib.MainConcepts.GoalTree.GoalsCombinator;

public class Strategy {
	
	static public enum StrategyType { FIRSTOF, ANYOF, SEQ, PRIMITIVE } 
	
	Strategy parent = null ;
	List<Strategy> subplans ;
	StrategyType nodety ;
	
	public Strategy(StrategyType type, Strategy ... plans) {
		if (plans.length == 0) throw new IllegalArgumentException() ;
		nodety = type ;
		subplans = new LinkedList<Strategy>() ;
		for (Strategy p : plans) {
			this.subplans.add(p) ;
			p.parent = this ;
		}
	}
	
	public List<PrimitiveStrategy> getEnabledPrimitivePlans(AbstractState agentstate) {
		
		List<PrimitiveStrategy> primplans = new LinkedList<PrimitiveStrategy>() ;
		
		if (nodety == StrategyType.FIRSTOF) {
			for (Strategy PT : subplans) {
				primplans = PT.getEnabledPrimitivePlans(agentstate) ;
				if (! primplans.isEmpty()) return primplans ;
			}
			return primplans ;
		}
		
		if (nodety == StrategyType.ANYOF) {
			for (Strategy PT : subplans) {
				primplans.addAll(PT.getEnabledPrimitivePlans(agentstate)) ;
			}
			return primplans ;
		}
		
		if (nodety == StrategyType.SEQ) {
			return subplans.get(0).getEnabledPrimitivePlans(agentstate) ;
		}
		
		return null ;
	}
	
	
	static public class PrimitiveStrategy extends Strategy {
		Action plan ;
		public PrimitiveStrategy(Action P) { 
			super(StrategyType.PRIMITIVE) ;
			plan = P ; 
		}
		
		@Override
		public List<PrimitiveStrategy> getEnabledPrimitivePlans(AbstractState agentstate) {
			var primplans = new LinkedList<PrimitiveStrategy>() ;
			if (plan.isEnabled(agentstate)) primplans.add(this) ;
			return primplans ;
		}

	}
	

}
