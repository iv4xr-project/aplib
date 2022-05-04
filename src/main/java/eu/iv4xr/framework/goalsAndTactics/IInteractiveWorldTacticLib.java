package eu.iv4xr.framework.goalsAndTactics;

import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.Tactic;

/**
 * 
 * Some common tactics to for a 3D interactive world.
 * 
 * @author Wish
 *
 */
public interface IInteractiveWorldTacticLib {
	
	/**
	 * A tactic that if executed repeatedly will drive the agent to explore the 
	 * world. It is enabled when there is a place to explore, and else it is 
	 * disabled.
	 */
	public Tactic explore() ;
	
	/**
	 * A variation of {@link explore}  that gives preference to a certain direction to
	 * explore first.
	 */
	public Tactic explore(Vec3 direction) ;
	

}
