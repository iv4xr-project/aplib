package eu.iv4xr.framework.mainConcepts;

import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import eu.iv4xr.framework.extensions.ltl.LTL;
import eu.iv4xr.framework.extensions.ltl.SATVerdict;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import nl.uu.cs.aplib.agents.AutonomousBasicAgent;
import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.mainConcepts.CostFunction;
import nl.uu.cs.aplib.mainConcepts.Deliberation;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.utils.Pair;

/**
 * This class implements a test-agent. It extents
 * {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent}. All the functionalities
 * of a test-agent fot being an agent are inherited for either
 * {@link nl.uu.cs.aplib.mainConcepts.BasicAgent} or
 * {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent}. This class TestAgent adds
 * some functionalities related to testing.
 * 
 * <p>
 * A test-agent is used to test some target system, also called
 * System-Under-Test or SUT. From the test-agent's perspective, this SUT is seen
 * as an environment the agent want to interact with. This implies that proper
 * instance of the class {@link nl.uu.cs.aplib.mainConcepts.Environment}, or of
 * one of its subclasses. must be placed between the agent and the SUT to
 * facilitate the interactions between them.
 * 
 * <p>
 * The idea is that a test-agent interacts with the SUT in order to observe it.
 * It is up to the test-agent to decide what are the circumstances worth
 * observing. When it observes something notable, it can register this as a
 * so-called observation event. There are several types of observations a
 * test-agent can collect:
 * 
 * <ul>
 * <li><b>Verdicts</b>. A verdict is an instance of
 * {@link ObservationEvent.VerdictEvent}.
 * </ul>
 * 
 * 
 * @author Wish
 *
 */

public class TestAgent extends AutonomousBasicAgent {

    protected String testDesc;
    protected TestDataCollector testDataCollector;
    protected SyntheticEventsProducer syntheticEventsProducer ;
    public List<LTL<SimpleState>> ltls = new LinkedList<>() ;
    
    //protected GoalStructure goal;  <-- agent already has this field!
    
    /**
     * If defined, this function can be used to evaluate an agent's state, to produce a list
     * of name-value properties of the state. The value is required to be numeric.
     */
    protected Function<SimpleState,Pair<String,Number>[]> scalarInstrumenter = null ;

    /**
     * Create a blank instance of TestAgent. To be useful you will need to add few
     * other things to it, e.g. a state and a goal. You also need to link it to a
     * {@link TestDataCollector}.
     */
    public TestAgent() {
        super();
    }

    /**
     * Create a plain instance of TestAgent with the given id and role. To be useful
     * you will need to add few other things to it, e.g. a state and a goal. You
     * also need to link it to a {@link TestDataCollector}.
     */
    public TestAgent(String id, String role) {
        super(id, role);
    }

    /**
     * To add some textual description of the test carried out by this test agent.
     */
    public TestAgent setTestDesc(String desc) {
        testDesc = desc;
        return this;
    }

    public TestAgent setTestDataCollector(TestDataCollector dc) {
        if (dc == null)
            throw new IllegalArgumentException();
        testDataCollector = dc;
        dc.registerTestAgent(this.id);
        return this;
    }

    public TestDataCollector getTestDataCollector() {
        return testDataCollector;
    }
    
    public TestAgent attachSyntheticEventsProducer(SyntheticEventsProducer syntheticEventsProducer) {
    	this.syntheticEventsProducer = syntheticEventsProducer ;
    	syntheticEventsProducer.agent = this ;
    	return this ;
    }
    
    public SyntheticEventsProducer getSyntheticEventsProducer() {
    	return this.syntheticEventsProducer ;
    }

    /**
     * Register a visit to e for the purpose of test-coverage tracking.
     * 
     * @param e Representing something of interest that we want to cover during
     *          testing.
     */
    public void registerVisit(CoveragePointEvent e) {
        testDataCollector.registerVisit(id, e);
    }

    /**
     * Register this event to be appended to a historical trace that this test agent
     * keeps track.
     * 
     * @param event Some event whose occurrence we want to keep track in a trace.
     *              The event will be time-stamped.
     */
    public void registerEvent(TimeStampedObservationEvent event) {
        testDataCollector.registerEvent(id, event);
    }

    /**
     * Register a verdict, which can be either a success or fail, or undecided.
     * Verdicts will also be put in this agent trace.
     * 
     * @param verdict Representing the verdict.
     */
    public void registerVerdict(VerdictEvent verdict) {
        testDataCollector.registerEvent(id, verdict);
    }

    /**
     * Attach a state structure to this agent. The method returns the agent itself
     * so that this method can be used in the Fluent Interface style.
     */
    @Override
    public TestAgent attachState(SimpleState state) {
        return (TestAgent) super.attachState(state);
    }

    /**
     * Attach a state structure to this agent. The method returns the agent itself
     * so that this method can be used in the Fluent Interface style.
     */
    @Override
    public TestAgent attachState(State state) {
        super.attachState(state);
        return this;
    }
    
	/**
	 * Attach behavior model of the game-under-test. Such a model is an instance of
	 * {@link GameWorldModel}. As a model, it may describe the system under test at
	 * a certain level of abstraction. If such a model is given, the agent (that
	 * owns this state) can then exploit it, e.g. to help it solves goals. The model
	 * can for example implements an extended Finite State Machine (EFSM).
	 * Importantly, note that the model, as an instance of {@link GameWorldModel},
	 * implements the interface
	 * {@link eu.iv4xr.framework.extensions.ltl.ITargetModel} and hence it can be
	 * queried using e.g. {@link eu.iv4xr.framework.extensions.BasicModelChecker}.
	 * 
	 * <p>
	 * Alternatively, the model that is given may also be empty initially, and the
	 * agent may be equipped with instrumentation to incrementally build the model.
	 * If not null, the parameter gwmodelLearner specifies this instrumentation
	 * function mentioned above. This function will be called by
	 * {@link #updateState(String)}. The function inspects this state, and can be
	 * used to register new model elements discovered in the current state.
	 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public TestAgent attachBehaviorModel(
    		GameWorldModel model,
			BiFunction<Iv4xrAgentState,GameWorldModel,Void> gwmodelLearner) {
    	if (this.state == null)
    		throw new IllegalArgumentException("Cannot attach a model. Attach a state first.") ;
    	if (! (this.state instanceof Iv4xrAgentState)) {
    		throw new IllegalArgumentException("Cannot attach a model on a state which is not an instance of Iv4xrAgentState.") ;
    	}
    	((Iv4xrAgentState) this.state).attachBehaviorModel(model, gwmodelLearner) ;
    	return this ;
    }
    
    /**
     * Attach an Environment to this agent. To be more precise, to attach the
     * Environment to the state structure of this agent. The method returns the
     * agent itself so that this method can be used in the Fluent Interface style.
     */
    @Override
    public TestAgent attachEnvironment(Environment env) {
        super.attachEnvironment(env);
        return this;
    }
    
    /**
     * Add one or more LTL-properties that would be checked as the agent runs.
     */
    public TestAgent addLTL(@SuppressWarnings("unchecked") LTL<SimpleState> ... ltlsToAdd) {
    	for (var phi : ltlsToAdd) {
    		ltls.add(phi) ;
    		phi.startChecking();
    	}
    	return this ;
    }
    
    /**
     * Add one or more invariants that would be checked as the agent runs. An "invariant"
     * is a state predicate I that is required to hold at every state along an execution.
     * Each given I will be translated to the LTL formula always(I).
     */
    public TestAgent addInv(Predicate<SimpleState> ... invs) {
    	for (var I : invs) {
    		LTL<SimpleState> I_ = LTL.always(I) ;
    		ltls.add(I_) ;
    		I_.startChecking();
    	}
    	return this ;
    }
    
    /**
     * Reset the state of the LTL-formulas attached/registered to this agent.
     */
    public TestAgent resetLTLs() {
    	for (var phi : ltls) phi.startChecking();
    	return this ;
    }
    
    /**
     * Call this at the end of agent's run to evaluate all LTL properties
     * registered to the agent (see [{@link #addLTL(LTL...)}). If one gives UNSAT, this method returns false (
     * one of the LTL is violated), else it returns true (no LTL is violated).
     */
    public boolean evaluateLTLs() {
    	boolean hasUnSat = false ;
    	// evaluate all ltls, if they were not evaluated yet:
    	int k = 0 ;
    	System.out.println("## Checking " + ltls.size() + " LTL properties...") ;
    	for (var phi : ltls)  {
    		if (!phi.fullyEvaluated) phi.endChecking();
    		//if (!hasUnSat  &&  phi.sat() == SATVerdict.UNSAT) {
        	if (phi.sat() == SATVerdict.UNSAT) {
    			hasUnSat = true ;
    			System.out.println("   " + k + "-th LTL-property is violated!") ;
    		}
    		k++ ;
    	}
    	if (!hasUnSat) 
    		System.out.println("   No LTL violation found.") ;
    	return !hasUnSat ;
    }
    
    /**
     * Call this at the end of agent's run to evaluate all invariants
     * registered to the agent (see {@link #addInv(Predicate...)} )If one gives UNSAT, this method returns false (
     * one of the invariant is violated), else it returns true (no invariant is violated).
     * The implementation of this method simply calls the more generic {@link #evaluateLTLs()}.
     */
    public boolean evaluateInvs() {
    	return evaluateLTLs() ;
    }
    
    

    /**
     * Set initial computation budget for this agent. The agent must have a goal
     * set. This method should not be called when the agent is already working on
     * its goal.
     */
    @Override
    public TestAgent budget(double b) {
        return (TestAgent) super.budget(b);
    }

    /**
     * Set a goal for this agent. The method returns the agent itself so that this
     * method can be used in the Fluent Interface style.
     */
    @Override
    public TestAgent setGoal(GoalStructure g) {
        return (TestAgent) super.setGoal(g);
    }

    /**
     * Set a goal for this agent, with the specified initial budget. The method
     * returns the agent itself so that this method can be used in the Fluent
     * Interface style.
     */
    @Override
    public TestAgent setGoal(double budget, GoalStructure g) {
        return (TestAgent) super.setGoal(budget, g);
    }

    /**
     * Replace the agent's deliberation module with the one given to this method.
     * The method returns the agent itself so that this method can be used in the
     * Fluent Interface style.
     */
    @Override
    public TestAgent useDeliberation(Deliberation delib) {
        return (TestAgent) super.useDeliberation(delib);
    }

    /**
     * Set f as this agent cost-function. Return the agent itself so that this
     * method can be used in the Fluent Interface style.
     */
    @Override
    public TestAgent withCostFunction(CostFunction f) {
        return (TestAgent) super.withCostFunction(f);
    }
    
	/**
	 * Attach an instrumentation function. This is a function can be used to
	 * evaluate an agent's state, to produce a list of name-value properties of the
	 * state. The value is required to be numeric.
	 */
    public TestAgent withScalarInstrumenter(Function<SimpleState,Pair<String,Number>[]> scalarInstrumenter) {
        this.scalarInstrumenter =  scalarInstrumenter ;
        return this ;
    }
    
	/**
	 * This will invoke the stadard update() function of the agent. This is defined
	 * by {@link nl.uu.cs.aplib.mainConcepts.BasicAgent#update()}; e.g. this decides
	 * which action is to take next and execute that action. If the action solves
	 * the current goal, the next goal will be decided.
	 * 
	 * <p>
	 * Additionally, in this update() the following updates are also performed:
	 * 
	 * <ul>
	 * <li>If there is a data-collector {@link #testDataCollector} and a
	 * scalar-instrumenter {@link #scalarInstrumenter} attached to this state, the
	 * instrumenter will be invoked, and the resulting data are registered into the
	 * data-collector.
	 * <li>If there is a synthetic event-producer {@link #syntheticEventsProducer}
	 * attached to this state, it will be invoked.
	 * </ul>
	 */
    @Override
    public void update() {
        super.update();
        if(testDataCollector != null && scalarInstrumenter != null) {
            Pair<String,Number>[] properties = scalarInstrumenter.apply(state) ;
            registerEvent(new ScalarTracingEvent(properties)) ;
        }
        if(this.syntheticEventsProducer != null) {
        	syntheticEventsProducer.generateCurrentEvents();
        }
        for (var phi : ltls) {
        	phi.checkNext(state) ;
        }
    }

}
