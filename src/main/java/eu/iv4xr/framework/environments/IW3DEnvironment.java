package eu.iv4xr.framework.environments;

import eu.iv4xr.framework.mainConcepts.Iv4xrEnvironment;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;

/**
 * An additional interface that can be added when sub-classing {@link Iv4xrEnvironment}.
 * The interface adds several methods that are typical for controlling a 3D
 * virtual world (e.g. a 3D game), such as moveToward() and interact().
 * 
 * An example of a partial implementation is provided in the class {@link W3DEnvironment}.
 *
 * @author Wish
 *
 */
public interface IW3DEnvironment {

	/**
	 * Send a command to the real environment that should cause it to send over the
	 * navigation-mesh of its 3D world, which this method should then attach in some
	 * field/variable for future use. This mesh is assumed to be static (does not
	 * change through out the agents' runs).
	 */
	public void loadWorld();

	/**
	 * Execute an interaction of the specified type on the given target entity in
	 * the real environment.
	 * 
	 * The parameter interactionType is a string specifying the name of the
	 * interaction to do, e.g. "Open" or "TurnOn".
	 * 
	 * The method should return a new observation from the perspective of the
	 * specified agent, sampled after the execution of the command by the real
	 * environment. It is up to the implementation to decide the exact sampling
	 * moment (e.g. whether or not to wait until the full effect of the interaction
	 * has taken place).
	 * 
	 * @param agentId  The id of the agent that does the interaction.
	 * @param targetId The id of the entity that is the target of the interaction.
	 */
	public WorldModel interact(String agentId, String targetId, String interactionType);

	/**
	 * A command to instruct an agent to move a small distance towards the given
	 * target location. How far the agent actually moves depends on the real
	 * environment. Typically, the calling agent will execute interactions/commands
	 * in update cycles. Then it depends on how fast time proceeds in the real
	 * environment as we advance from one agent's update-cycle to the next. A
	 * possible setup is to make the real environment to run in sync with the
	 * agent's cycles and to fix the simulated time between cycles, e.g. 1/30-th
	 * second. In this case, the agent will then move to some distance of the
	 * specified velocity/30.
	 * 
	 * <p>
	 * The method should return an observation, sampled after the execution the
	 * command by the real environment. It is up to the implementation to decide the
	 * exact sampling moment (e.g. whether this mean to immediately return an
	 * observation, or to wait until actual environment reaches the end of its
	 * current update cycle).
	 */
	public WorldModel moveToward(String agentId, Vec3 agentLocation, Vec3 targetLocation);

}
