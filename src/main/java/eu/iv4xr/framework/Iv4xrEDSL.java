package eu.iv4xr.framework;

import eu.iv4xr.framework.mainConcepts.*;
import nl.uu.cs.aplib.mainConcepts.*;
import static nl.uu.cs.aplib.AplibEDSL.* ;

import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;

import java.util.function.Predicate;

public class Iv4xrEDSL {

    /**
     * Create a blank instance of {@link eu.iv4xr.framework.mainConcepts.TestGoal}
     * with the given name.
     */
    public static TestGoal testgoal(String name) {
        return new TestGoal(name);
    }

    /**
     * Create a blank instance of {@link eu.iv4xr.framework.mainConcepts.TestGoal}
     * with the given name, and linking it to the given test-agent.
     */
    public static TestGoal testgoal(String name, TestAgent ta) {
        return new TestGoal(name, ta);
    }

    /**
     * Check a boolean expression, if it is true then produce a positive
     * VerdictEvent, and else a negative VerdictEvent.
     * 
     * @param assertionFamilyName a text to classify the asserting as belonging to
     *                            some category, if any.
     * @param assertionInfo       some info text on what the property is being
     *                            checked.
     * @param assertion           the boolean expression to check.
     */
    public static VerdictEvent assertTrue_(String assertionFamilyName, String assertionInfo, boolean assertion) {
        if (assertion)
            return new VerdictEvent(assertionFamilyName, assertionInfo, true);
        return new VerdictEvent(assertionFamilyName, assertionInfo, false);
    }
    
    /**
     * A Goal that when selected, will cause the agent to check the given predicate.
     */
    public static <State> GoalStructure assertTrue_(
    		TestAgent ta,
    		String assertionFamilyName, String assertionInfo,
    		Predicate<State> predicate) {
    	
    	TestGoal g = testgoal("Assertion is checked: " + assertionFamilyName + ", " + assertionInfo)
    			.toSolve((State S) -> true)
    			.invariant(ta, (State S) -> assertTrue_(assertionFamilyName,assertionInfo, predicate.test(S)))
    			.withTactic(
    				action("skip")
    				.do1((State S) -> S)
    				.lift())
    		;
    		 	
    	return g.lift() ;
    	
    }
}
