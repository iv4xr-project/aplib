package nl.uu.cs.aplib.MainConcepts;

import java.util.LinkedList;
import java.util.List;

import nl.uu.cs.aplib.MainConcepts.GoalTree.GoalsCombinator;

public class PlanScheme {
	
	static enum PlanNodeType { FIRSTOF, ANYOF, SEQ, PRIMITIVE } 
	
	PlanScheme parent = null ;
	List<PlanScheme> subplans ;
	PlanNodeType nodety ;
	
	public PlanScheme(PlanNodeType type, PlanScheme ... plans) {
		if (plans.length == 0) throw new IllegalArgumentException() ;
		nodety = type ;
		subplans = new LinkedList<PlanScheme>() ;
		for (PlanScheme p : plans) {
			this.subplans.add(p) ;
			p.parent = this ;
		}
	}
	
	public List<PrimitivePlan> getEnabledPrimitivePlans(AbstractState agentstate) {
		
		List<PrimitivePlan> primplans = new LinkedList<PrimitivePlan>() ;
		
		if (nodety == PlanNodeType.FIRSTOF) {
			for (PlanScheme PT : subplans) {
				primplans = PT.getEnabledPrimitivePlans(agentstate) ;
				if (! primplans.isEmpty()) return primplans ;
			}
			return primplans ;
		}
		
		if (nodety == PlanNodeType.ANYOF) {
			for (PlanScheme PT : subplans) {
				primplans.addAll(PT.getEnabledPrimitivePlans(agentstate)) ;
			}
			return primplans ;
		}
		
		if (nodety == PlanNodeType.SEQ) {
			return subplans.get(0).getEnabledPrimitivePlans(agentstate) ;
		}
		
		return null ;
	}
	
	
	static public class PrimitivePlan extends PlanScheme {
		Plan plan ;
		public PrimitivePlan(Plan P) { 
			super(PlanNodeType.PRIMITIVE) ;
			plan = P ; 
		}
		
		@Override
		public List<PrimitivePlan> getEnabledPrimitivePlans(AbstractState agentstate) {
			var primplans = new LinkedList<PrimitivePlan>() ;
			if (plan.isEnabled(agentstate)) primplans.add(this) ;
			return primplans ;
		}

	}
	

}
