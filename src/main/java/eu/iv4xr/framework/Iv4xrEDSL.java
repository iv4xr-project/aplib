package eu.iv4xr.framework;

import eu.iv4xr.framework.MainConcepts.TestGoal;
import nl.uu.cs.aplib.MainConcepts.Goal;

public class Iv4xrEDSL {
	
	/**
	 * Create a blank instance of {@link eu.iv4xr.framework.MainConcepts.TestGoal} with the given name.
	 */
    public static TestGoal testgoal(String name) { 
		return new TestGoal(name) ;
	}

}
