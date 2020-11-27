package eu.iv4xr.framework.mainConcepts;

import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph;
import nl.uu.cs.aplib.agents.State ; 

public class W3DAgentState extends State {
	
	WorldModel wom ;
	
	SurfaceNavGraph worldNavigation ;
	
	
	/**
	 * The timestamp of when a vertex becomes known to the agent. So, if vertex
	 * i becomes known to the agent at time t, then knownVerticesTimestamps[i]= t.
	 */
    long[] knownVerticesTimestamps ; // move this to the NavGraph? TODO
	
    /**
     * Link the given environment to this State. It will also import the 
     * mesh describing the navigable surface of the 3D world (this is
     * supposed to have been loaded into env) and convert it into an instance
     * of SurfaceNavGraph to facilitate navigation over this surface.
     * 
     * For each face in the mesh, this constructor will also add the center-point of the face
     * in the created navigation graph. This is done if the face's area is large enough;
     * that is, if it exceeds the threshold faceAreaThresholdToAddCenterNode.
     */
	public W3DAgentState setEnvironment(W3DEnvironment env, float faceAreaThresholdToAddCenterNode) {
		super.setEnvironment(env) ;
		worldNavigation = new SurfaceNavGraph(env.worldNavigableMesh, faceAreaThresholdToAddCenterNode) ;
		return this ;
	}
	
	/**
	 * With faceAreaThresholdToAddCenterNode set to 1.0.
	 */
	public W3DAgentState setEnvironment(W3DEnvironment env) {
		return setEnvironment(env,1.0f) ;
	}
	
	public W3DEnvironment env() {
		return(W3DEnvironment) super.env() ;
	}

}
