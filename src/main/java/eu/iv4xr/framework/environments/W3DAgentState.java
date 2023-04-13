package eu.iv4xr.framework.environments;

import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import nl.uu.cs.aplib.mainConcepts.Environment;

/**
 * Provide a subclass of {@link Iv4xrAgentState} that uses
 * {@link eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph} as
 * navigation graph. SurfaceNavGraph extends
 * {@link eu.iv4xr.framework.extensions.pathfinding.SimpleNavGraph}. So, it can
 * also represent dynamic obstacles in the graph. Additionally, SurfaceNavGraph
 * also includes path finding, and importanty it can keep track of navigation
 * nodes that are seen so far, and use this to provide methods to do exploration
 * over the graph.
 * 
 * <p>
 * The class requires {@link eu.iv4xr.framework.environments.W3DEnvironment} as
 * an environment.
 * 
 * @author Wish
 *
 */
public class W3DAgentState extends Iv4xrAgentState<Integer> {

	// public WorldModel wom;

	// public SurfaceNavGraph worldNavigation;

	@Override
	public SurfaceNavGraph worldNavigation() {
		return (SurfaceNavGraph) super.worldNavigation();
	}

	/*
	 * The timestamp of when a vertex becomes known to the agent. So, if vertex i
	 * becomes known to the agent at time t, then knownVerticesTimestamps[i]= t.
	 */
	// long[] knownVerticesTimestamps ; // move this to the NavGraph? TODO

	/**
	 * Link the given environment to this State. It will also import the mesh
	 * describing the navigable surface of the 3D world (this is supposed to have
	 * been loaded into env) and convert it into an instance of SurfaceNavGraph to
	 * facilitate navigation over this surface.
	 * 
	 * For each face in the mesh, this constructor will also add the center-point of
	 * the face in the created navigation graph. This is done if the face's area is
	 * large enough; that is, if it exceeds the threshold
	 * faceAreaThresholdToAddCenterNode.
	 */
	public W3DAgentState setEnvironment(W3DEnvironment env, float faceAreaThresholdToAddCenterNode) {
		if(env == null) {
			throw new IllegalArgumentException("Cannot attach a null environment to a state.") ;
		}
		if (!(env instanceof W3DEnvironment)) {
			throw new IllegalArgumentException("This class requires an W3DEnvironment as its environment.");
		}
		super.setEnvironment(env);
		setWorldNavigation(new SurfaceNavGraph(env.worldNavigableMesh(), faceAreaThresholdToAddCenterNode)) ;
		return this;
	}

	/**
	 * Link an environment to this state. An instance of
	 * {@link eu.iv4xr.framework.environments.W3DEnvironment} is needed. The method
	 * will forward the call to
	 * {@link W3DAgentState#setEnvironment(W3DEnvironment, float)}, set with
	 * faceAreaThresholdToAddCenterNode set to 1.0.
	 */
	@Override
	public W3DAgentState setEnvironment(Environment env) {	
		return setEnvironment((W3DEnvironment) env, 1.0f);
	}

	@Override
	public W3DEnvironment env() {
		return (W3DEnvironment) super.env();
	}

}
