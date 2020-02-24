package eu.iv4xr.framework;

import eu.iv4xr.framework.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.Goal;
import static eu.iv4xr.framework.mainConcepts.ObservationEvent.* ;

public class Iv4xrEDSL {
	
	/**
	 * Create a blank instance of {@link eu.iv4xr.framework.mainConcepts.TestGoal} with the given name.
	 */
    public static TestGoal testgoal(String name) { 
		return new TestGoal(name) ;
	}

	/**
	 * Create a blank instance of {@link eu.iv4xr.framework.mainConcepts.TestGoal} with the given name,
	 * and linking it to the given test-agent.
	 */
    public static TestGoal testgoal(String name, TestAgent ta) { 
		return new TestGoal(name,ta) ;
	}   
    
    /**
     * Check a boolean expression, if it is true then produce a positive VerdictEvent, and else a negative
     * VerdictEvent.
     * 
     * @param assertionFamilyName  a text to classify the asserting as belonging to some category, if any.
     * @param assertionInfo some info text on what the property is being checked.
     * @param assertion the boolean expression to check.
     */
    public static VerdictEvent assertTrue_(String assertionFamilyName, String assertionInfo, boolean assertion) {
    	if (assertion)
    		return new VerdictEvent(assertionFamilyName,assertionInfo,true) ;
		return new VerdictEvent(assertionFamilyName,assertionInfo,false) ;
    }
}
