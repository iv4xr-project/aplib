package eu.iv4xr.framework.goalsAndTactics;

import java.util.function.Predicate;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

public interface IInteractiveWorldGoalLib {
	
	public GoalStructure positionInCloseRange(Vec3 p) ;
	public GoalStructure entityInCloseRange(String entityId) ;
	public GoalStructure entityStateRefreshed(String entityId) ;
	public GoalStructure entityInteracted(String entityId) ;
	public GoalStructure invariantChecked(TestAgent agent, String entityId, String info, Predicate<SimpleState> predicate) ;	

}
