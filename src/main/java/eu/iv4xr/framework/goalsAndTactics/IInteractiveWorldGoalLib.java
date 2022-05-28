package eu.iv4xr.framework.goalsAndTactics;

import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.*;

/**
 * 
 * This interface lists/suggests some commonly useful goal-structures for an
 * agent to go about in an interactive world.
 * 
 * @author Wish
 *
 */
public interface IInteractiveWorldGoalLib<Location> {

	/**
	 * The method constructs a goal structure G that will guide an agent towards a
	 * given location p. The goal is solved when the agent is in a position close
	 * enough to the given location p.
	 * 
	 * What "close" means is left unspecified.
	 * 
	 * The executing agent can fail/abort the goal if it no longer believe it is
	 * possible to reach p.
	 */
	public GoalStructure positionInCloseRange(Location p);

	/**
	 * The method constructs a goal structure G that will guide an agent towards a
	 * world entity with the given id. The goal is solved when the agent is in a
	 * position close enough to the entity.
	 * 
	 * What "close" means is left unspecified.
	 * 
	 * The executing agent can fail/abort the goal if it no longer believe it is
	 * possible to reach the entity.
	 */
	public GoalStructure entityInCloseRange(String entityId);

	/**
	 * The method constructs a goal structure G that will guide an agent towards a
	 * world entity with the given id, until the entity becomes visible to the agent
	 * so that its knowledge on the entity state is refreshed.
	 * The goal is solved when the agent can see the entity.
	 * 
	 * The executing agent can fail/abort the goal if it no longer believe it is
	 * possible to achieve the goal.
	 */
	public GoalStructure entityStateRefreshed(String entityId);

	/**
	 * The method constructs a goal structure G, to make an agent interacts with the
	 * given entity. It is solved when the interaction is done. The executing agent
	 * can fail/abort the goal if the interaction is not possible (e.g. if it is 
	 * not close enough to the entity to be able to interact with it).
	 */
	public GoalStructure entityInteracted(String entityId);
	
	
	/**
	 * Construct a goal structure G that will make the agent explore the world.
	 * The exploration stops when either no further progress can be made, or
	 * when the given budget is exhausted. "Exploring" can be seen as a persistent
	 * goal (something that the agent should always strives to do). So, when
	 * it is terminated, it always terminate in a fail (this is intentional).
	 * 
	 * <p>If a heuristic location is given, the agent will try to explore towards
	 * this location.
	 */
	public GoalStructure exploring(Location heuristicLocation, int budget);

}
