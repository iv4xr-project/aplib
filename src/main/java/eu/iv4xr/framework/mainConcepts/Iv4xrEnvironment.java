package eu.iv4xr.framework.mainConcepts;

import eu.iv4xr.framework.spatial.meshes.Mesh;
import nl.uu.cs.aplib.mainConcepts.Environment;

/**
 * An extension of the standard {@link nl.uu.cs.aplib.mainConcepts.Environment}.
 * The extra feature is that the method {@link #observe(String)} now returns 
 * the observation as an instance of {@link WorldModel}.
 * 
 * <p>Note that this class is not use-ready. You have to implement several
 * methods
 * 
 * <ul>
 * <li> {@link #sendCommand_(EnvOperation)}
 * <li> {@link #observe(String)}
 * <li> {@link #worldNavigableMesh()}
 * </ul>
 * 
 * @author Wish
 *
 */
public class Iv4xrEnvironment extends Environment  {
	
	
	/**
     * You should implement this method.
     * 
     * <p>Return a polygon-mesh describing the navigable surface of the 3D-world of the
     * real environment/system that is represented/controlled
     * by this environment, <b>if</b> this information is provided by the
     * real environment. Else the method returns null.
     * 
     * <p>The obtained Mesh can subsequently be converted into a navigation graph
     * that is attached to an agent-state, in particular a state of type
     * {@link Iv4xrAgentState}. To do the conversion, see for example
     * the methods {@link Iv4xrAgentState#loadSimpleNavGraph(Iv4xrAgentState)}
     * and {@link Iv4xrAgentState#loadSurfaceNavGraph(Iv4xrAgentState, float)}.
     */
    public Mesh worldNavigableMesh() {
    	throw new UnsupportedOperationException();
    }
	
	/**
	 * You should implement this method.
	 * 
	 * <p>
	 * This method should send a command to the real environment that will cause it
	 * to send back what the agent of the given id observes in the real environment.
	 * This method should translate the obtained observation to an a
	 * {@link eu.iv4xr.framework.mainConcepts.WorldModel} and return this
	 * world-model.
	 * 
	 * @param agentId The id of the agent whose observation is requested.
	 * @return An instance of WorldModel representing what the specified agent
	 *         observes.
	 */
    @Override
    public WorldModel observe(String agentId) {
    	throw new UnsupportedOperationException();
    }

}
