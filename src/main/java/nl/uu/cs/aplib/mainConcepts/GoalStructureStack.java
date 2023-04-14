package nl.uu.cs.aplib.mainConcepts;

import java.util.*;

import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;

public class GoalStructureStack {
	
	protected List<StackItem> stack = new LinkedList<>() ;
	protected StackItem pendingPush = null ;
	
	protected static class StackItem {
		protected GoalStructure rootGoal ;
		protected PrimitiveGoal currentPrimitiveGoal;
		protected Tactic currentTactic;
		protected StackItem(GoalStructure G) {
			rootGoal = G ;
		}
	}
	
	public void pendingPush(StackItem G) {
		if (pendingPush != null) 
			throw new IllegalStateException("Trying to push onto the goal-stack that still has a pending push.") ;
		pendingPush = G ;
	}
	
	public boolean commitPendingPush() {
		if (pendingPush != null) {
			stack.add(pendingPush) ;
			pendingPush = null ;
			return true ;
		}
		return false ;
	}
	
	public boolean isEmpty() {
		return stack.size() == 0 ;
	}
	
	public StackItem pop() {
		if (stack.size() == 0) 
			throw new IllegalStateException("Trying to pop from an empty goal-stack.")	;
		return stack.remove(stack.size()-1) ;
	}
	
	public StackItem top() {
		if (stack.size() == 0) 
			throw new IllegalStateException("The-goal stack has no goal.") ;
		return stack.get(stack.size()-1) ;
	}
	
	public GoalStructure currentRootGoal() {
		return top().rootGoal ;
	}
	
	public PrimitiveGoal currentPrimitiveGoal() {
		return top().currentPrimitiveGoal ;
	}
	
	public Tactic currentTactic() {
		return top().currentTactic ;
	}
	
	public void setCurrentPrimitiveGoal(PrimitiveGoal g) {
		top().currentPrimitiveGoal = g ;
	}
	
	public void setCurrentTactic(Tactic T) {
		top().currentTactic = T ;
	}

}
