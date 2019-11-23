package eu.iv4xr.framework;

import eu.iv4xr.framework.mainConcepts.TestGoal;
import nl.uu.cs.aplib.mainConcepts.Goal;

public class Iv4xrEDSL {
	
	/**
	 * Create a blank instance of {@link eu.iv4xr.framework.mainConcepts.TestGoal} with the given name.
	 */
    public static TestGoal testgoal(String name) { 
		return new TestGoal(name) ;
	}

}
