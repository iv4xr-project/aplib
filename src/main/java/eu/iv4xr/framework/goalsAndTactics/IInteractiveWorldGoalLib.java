package eu.iv4xr.framework.goalsAndTactics;

import java.util.function.Predicate;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

/**
 * 
 * Some common goals to for a 3D interactive world.
 * 
 * @author Wish
 *
 */
public interface IInteractiveWorldGoalLib {

	/**
	 * The method constructs a goal structure G, which is solved when the agent is in a
	 * position close enough to the given 3D position p.
	 * 
	 * What "close" means is left unspecified.
	 */
	public GoalStructure positionInCloseRange(Vec3 p);

	/**
	 * The method constructs a goal structure G, which is solved when the agent is in a
	 * position close enough to the position of an in-world entity e with the given
	 * id.
	 * 
	 * What "close" means is left unspecified.
	 */
	public GoalStructure entityInCloseRange(String entityId);

	/**
	 * The method constructs a goal structure G, which is solved when the agent is in a
	 * position where it can see the in-world entity with the given id. Being able
	 * to "see" is assumed to mean that it allows the agent to refresh its knowledge
	 * about e.
	 */
	public GoalStructure entityStateRefreshed(String entityId);

	/**
	 * The method constructs a goal structure G, which is solved when the entity e with
	 * the given id is interacted by the agent. Note that this may require the agent
	 * to first move to a position close enough to e so that interaction is
	 * possible.
	 */
	public GoalStructure entityInteracted(String entityId);
	

}
