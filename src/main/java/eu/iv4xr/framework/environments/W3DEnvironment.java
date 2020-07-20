package eu.iv4xr.framework.environments;

import eu.iv4xr.framework.extensions.pathfinding.NavGraph;
import eu.iv4xr.framework.extensions.spatial.Mesh;
import eu.iv4xr.framework.extensions.spatial.Vec3;
import eu.iv4xr.framework.world.WorldEntity;
import eu.iv4xr.framework.world.WorldModel;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.Environment.EnvOperation;

/**
 * An extension of {@link nl.uu.cs.aplib.mainConcepts.Environment}. It
 * adds methods typical for interacting with a 3D-virtual-world 
 * environment. Examples of such an environment are 3D games or simulators.
 * 
 * @author Wish
 *
 */
public class W3DEnvironment extends Environment {
	
	
	static public String OBSERVE_CMDNAME = "Observe" ;
	static public String MOVETOWARD_CMDNAME = "Move" ;
	
	/**
	 * A polygon-mesh describing the navigable surface of the 3D-world represented by
	 * this environment.
	 */
	Mesh worldNavigableMesh ;
	
	/**
	 * A graph of vertices/nodes to navigate the D-world represented by this
	 * environment. The vertices will be as such that each polygon in the
	 * worldNavigableMesh is represented by at least one vertex, and that if two
	 * polygons are neighboring then there are at least two vertices in those polygons
	 * which are directly connected.
	 */
	NavGraph worldNavigationGraph ;
	
	/**
	 * Execute an interaction of the specified type on the given target entity in
	 * the real environment.
	 * 
	 * The parameter interactionType is a string specifying the name of the
	 * interaction to do, e.g. "Open" or "TurnOn". This string should not be the
	 * same command names that are already pre-defined by this class, such as
	 * OBSERVE_CMDNAME.
	 * 
	 * This method has no further argument to send to the real environment.
	 * 
	 * The method should return a new observation by the specified agent, sampled
	 * after the interaction.
	 * 
	 * @param agentId  The id of the agent that does the interaction.
	 * @param targetId The id of the entity that is the target of the interaction.
	 */
	public WorldModel interact(String agentId, String targetId, String interactionType) {
		return (WorldModel) sendCommand(agentId, targetId, interactionType, null, WorldModel.class);
	}
	
	/**
	 * Send a command to the real environment that will cause it to send back what the 
	 * agent of the given id observes in the real environment. The observation will be
	 * parsed into an instance of {@link eu.iv4xr.framework.world.WorldModel}.

	 * @param agentId The id of the agent whose observation is requested.
	 * @return An instance of WorldModel representing what the specified agent observes.
	 */
	public WorldModel observe(String agentId) {
		return (WorldModel) sendCommand(agentId,null,OBSERVE_CMDNAME,null,WorldModel.class) ;
	}	
	
	/**
	 * A command to instruct an agent to move a small distance in the given
	 * direction. How far the agent actually moves depends on the real environment.
	 * Typically, the calling agent will execute interactions/commands in update
	 * cycles. Then it depends on how fast time proceeds in the real environment as
	 * we advance from one agent's update-cycle to the next. A possible setup is to
	 * make the real environment to run in sync with the agent's cycles and to fix
	 * the simulated time between cycles, e.g. 1/30-th second. In this case, the
	 * agent will then move to some distance of the specified velocity/30.
	 * 
	 * The method should return a new observation, sampled at the end of its
	 * movement.
	 * 
	 * @param direction A vector specifying the direction for the agent to move to.
	 *                  The length of the vector specifies the movement velocity, as
	 *                  far as the real environment allow the velocity to be
	 *                  specified. Else the velocity will be ignored, and the real
	 *                  environment decides the velocity.
	 */
	public WorldModel moveToward(String agentId, Vec3 direction) {
		return (WorldModel) sendCommand(agentId,null,MOVETOWARD_CMDNAME,direction,WorldModel.class) ;
	}
	
	/**
	 * You need to implement this method. There are a number of pre-defined command
	 * names, namely OBSERVE_CMDNAME and MOVETOWARD_CMDNAME, that you need to
	 * implement. OBSERVE_CMDNAME has no argument, and MOVETOWARD_CMDNAME has one
	 * Vec3 argument specifying the direction (and velocity) of the move.
	 * 
	 * Other command names are interpreted as interaction commands by the executing
	 * agent on a specified target entity.
	 */
	protected Object sendCommand_(EnvOperation cmd) {
		 throw new UnsupportedOperationException() ;
	}
}
