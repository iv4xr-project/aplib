package eu.iv4xr.framework.mainConcepts;

import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;
import static eu.iv4xr.framework.mainConcepts.TestDataCollector.*;

import java.util.*;

import nl.uu.cs.aplib.agents.AutonomousBasicAgent;
import nl.uu.cs.aplib.agents.StateWithMessenger;
import nl.uu.cs.aplib.mainConcepts.BasicAgent;
import nl.uu.cs.aplib.mainConcepts.CostFunction;
import nl.uu.cs.aplib.mainConcepts.Deliberation;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

/**
 * This class implements a test-agent. It extents {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent}.
 * All the functionalities of a test-agent fot being an agent are inherited
 * for either {@link nl.uu.cs.aplib.mainConcepts.BasicAgent} 
 * or {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent}. This class TestAgent adds some functionalities
 * related to testing.
 * 
 * <p>A test-agent is used to test some target system, also called System-Under-Test or SUT.
 * From the test-agent's perspective, this SUT is seen as an environment the agent want to 
 * interact with.
 * This implies that  proper instance of the class {@link nl.uu.cs.aplib.mainConcepts.Environment},
 * or of one of its subclasses. must be placed between the agent and the SUT to facilitate
 * the interactions between them.
 * 
 * <p>The idea is that a test-agent interacts with the SUT in order to observe it. It is
 * up to the test-agent to decide what are the circumstances worth observing. When it
 * observes something notable, it can register this as a so-called observation event.  
 * There are several types of observations a test-agent can collect:
 * 
 * <ul>
 *    <li> <b>Verdicts</b>. A verdict is an instance of {@link TestDataCollector.VerdictEvent}.
 * </ul>
 * 
 * 
 * @author Wish
 *
 */

public class TestAgent extends AutonomousBasicAgent {
	
	protected String testDesc ;
	protected TestDataCollector testDataCollector ;
	protected GoalStructure goal;
	
	/**
	 * Create a blank instance of TestAgent. To be useful you will need 
	 * to add few other things to it, e.g. a state and a goal. You also need to link it
	 * to a {@link TestDataCollector}.
	 */
	public TestAgent() { super() ; }
	
	/**
	 * Create a plain instance of TestAgent with the given id and role.
	 * To be useful you will need to add few other things to it, e.g. a state and a
	 * goal. You also need to link it to a {@link TestDataCollector}.
	 */
	public TestAgent(String id, String role) {
		super(id,role) ;
	}
    		
	/**
	 * To add some textual description of the test carried out by this test agent.
	 */
	public TestAgent setTestDesc(String desc) {
		testDesc = desc ; return this ;
	}

	public TestAgent setTestDataCollector(TestDataCollector dc) {
		if (dc==null) throw new IllegalArgumentException() ;
		testDataCollector = dc ;
		dc.registerTestAgent(this.id);
		return this ;
	}
	
	public TestDataCollector getTestDataCollector() { return testDataCollector ; }
	
	/**
	 * Register a visit to e for the purpose of test-coverage tracking.
	 * 
	 * @param e Representing something of interest that we want to cover during testing.
	 */
	public void registerVisit(CoveragePointEvent e) {
		testDataCollector.registerVisit(id,e);
	}
	
	/**
	 * Register this event to be appended to a historical trace that this test agent keeps track.
	 * 
	 * @param event Some event whose occurrence we want to keep track in a trace. The event will be time-stamped.
	 */
	public void registerEvent(TimeStampedObservationEvent event) {
		testDataCollector.registerEvent(id,event);
	}

	/**
	 * Register a verdict, which can be either a success or fail, or undecided. Verdicts will
	 * also be put in this agent trace.
	 * 
	 * @param verdict Representing the verdict.
	 */
	public void registerVerdict(VerdictEvent verdict) {
		testDataCollector.registerEvent(id,verdict);
	}
	
	/**
	 * Attach a state structure to this agent. The method returns the agent itself
	 * so that this method can be used in the Fluent Interface style.
	 */
	@Override
	public TestAgent attachState(SimpleState state) {
		return (TestAgent) super.attachState(state) ;
	}
	
	/**
	 * Attach a state structure to this agent. The method returns the agent itself
	 * so that this method can be used in the Fluent Interface style.
	 */
	@Override
	public TestAgent attachState(StateWithMessenger state) {
		super.attachState(state) ;
		return this ;
	}
	
	/**
	 * Attach an Environment to this agent. To be more precise, to attach the
	 * Environment to the state structure of this agent. The method returns the
	 * agent itself so that this method can be used in the Fluent Interface style.
	 */
	@Override
	public TestAgent attachEnvironment(Environment env) {
		super.attachEnvironment(env) ;
		return this  ;
	}

	/**
	 * Set initial computation budget for this agent. The agent must have a goal set.
	 * This method should not be called when the agent is already working on its 
	 * goal.
	 */
	@Override
	public TestAgent budget(double b) {
		return (TestAgent) super.budget(b) ;
	}
	
	/**
	 * Set a goal for this agent. The method returns the agent itself so that this
	 * method can be used in the Fluent Interface style.
	 */
	@Override
	public TestAgent setGoal(GoalStructure g) {
		return (TestAgent) super.setGoal(g) ;
	}
	
	/**
	 * Set a goal for this agent, with the specified initial budget. 
	 * The method returns the agent itself so that this method can be used in the Fluent Interface style.
	 */
	@Override
	public TestAgent setGoal(double budget, GoalStructure g) {
		return (TestAgent) super.setGoal(budget, g) ;
	}
	
	
	/**
	 * Replace the agent's deliberation module with the one given to this method.
	 * The method returns the agent itself so that this method can be used in the
	 * Fluent Interface style.
	 */
	@Override
	public TestAgent useDeliberation(Deliberation delib) {
		return (TestAgent) super.useDeliberation(delib) ;
	}
	
	/**
	 * Set f as this agent cost-function. Return the agent itself so that this
	 * method can be used in the Fluent Interface style.
	 */
	@Override
	public TestAgent withCostFunction(CostFunction f) {
		return (TestAgent) super.withCostFunction(f) ;
	}

}
