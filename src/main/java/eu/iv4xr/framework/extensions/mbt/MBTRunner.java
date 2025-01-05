package eu.iv4xr.framework.extensions.mbt;

import java.util.* ;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.agents.State;

public class MBTRunner<S extends State> {
	
	public MBTModel<S> model ;
	
	public boolean inferTransitions = false ;
	
	public Random rnd = new Random(3731) ;
	
	public boolean DEBUG = true ;
	
	public enum ACTION_SELECTION { RANDOM , VALUE }
	
	public ACTION_SELECTION actionSelectionPolicy = ACTION_SELECTION.RANDOM ;
	
	/**
	 * If true, suite/sequence generation will stop at the first failed
	 * action, or first violation of post-conditions.
	 * Default: true.
	 */
	public boolean stopOnFailedOrViolation = true ;
	
	// We won't do this. It makes the runner quite complicated
	// e.g. for coverage counting. Instead, you should have a
	// model where a transition ~g -> t is added for each g -> t
	//
	// public boolean alsoTryUnenabledActions = false ;
	
	public Function<Void,TestAgent> resetSUT ;
	
	public Map<String,Integer> coveredStates = new HashMap<>() ;
	public Map<MBTTransition,Integer> coveredTransitions = new HashMap<>() ;
	public TransitionValueTable vtable = new TransitionValueTable() ;
	
	
	public void DEBUGPrint(String str) {
		if (! DEBUG) return ;
		System.out.print(str);
	}
	
	public void DEBUGPrintln(String str) {
		if (! DEBUG) return ;
		System.out.println(str);
	}
	
	public void clearCoverageData() {
		coveredStates.clear(); 
		coveredTransitions.clear(); 
	}
	
	public MBTStateConfiguration  agentState2Configuration(S currentState) {
		List<String> activeModelStates = new LinkedList<>() ;
		for (var modelState : model.states.values()) {
			if (modelState.inState(currentState)) {
				activeModelStates.add(modelState.id) ;
			}
		}
		return new MBTStateConfiguration(activeModelStates) ;
	}
	
	@SuppressWarnings("unchecked")
	void registerStateCover(TestAgent agent) {
		var currentAgentState = (S) agent.state() ;
		registerStateCover(agentState2Configuration(currentAgentState)) ;
	}
	
	void registerStateCover(MBTStateConfiguration stz) {
		for (var st : stz.states) {
			Integer count = coveredStates.get(st) ;
			if (count == null) {
				count = 0 ;
			}
			count++ ;
			coveredStates.put(st, count) ;
		}
	}
	
	void registerTransitionCover(MBTTransition tr) {
		Integer count = coveredTransitions.get(tr) ;
		if (count == null)
			count = 0 ;
		count++ ;
		coveredTransitions.put(tr, count) ;
	}
	
	
	public static class ActionExecutionResult {
		public String executedAction ;
		public boolean executionSuccessful ;
		public boolean postConditionViolationFound ;
		public List<String> violatedPostConditions = new LinkedList<>() ;
		public MBTStateConfiguration statesBefore ;
		public MBTStateConfiguration statesAfter ;
		
		public MBTTransition getTransition() {
			return new MBTTransition(statesBefore,executedAction,statesAfter) ;
		}
	}
	
	@SuppressWarnings("unchecked")
	public ActionExecutionResult executeNext(MBTTransition previousTrasition, TestAgent agent) {
		
		S stateBeforeAction = (S) agent.state() ;
		
		List<MBTAction<S>> actions = model.enabledActions(stateBeforeAction) ;
		//List<MBTAction<S>> actions = null ;
		//if (alsoTryUnenabledActions) {
		//	actions = model.actions.values().stream().collect(Collectors.toList()) ;
		//}
		//else {
		//	actions = enableds ;
		//}
		if (actions.isEmpty()) {
			// update the value of the previous transition to -inf:
			if (previousTrasition != null)
				vtable.updateTransitionValue(previousTrasition, Float.NEGATIVE_INFINITY);
			return null ;	
		}
		
		var configBeforeAction = agentState2Configuration(stateBeforeAction) ;
		
		// for now we will just choose actions randomly:
		var selected = actions.get(rnd.nextInt(actions.size())) ;
		
		
		
		
		
		DEBUGPrint(">> active states: ") ;
		var activeModelStates = agentState2Configuration(stateBeforeAction) ;
		int k = 0 ;
		for (var st : activeModelStates.states) {
			if (k>0) DEBUGPrint(", ") ;
			DEBUGPrint(st) ;
			k++ ;
		}
		DEBUGPrintln("") ;
		DEBUGPrint("-- executing " + selected.name) ;
		
		var R = new ActionExecutionResult() ;
		R.statesBefore = configBeforeAction ;
		R.executedAction = selected.name ;
		
		R.executionSuccessful = selected.theAction.apply(agent) ;
		
		if (R.executionSuccessful) {
			
			var stateAfterAction = (S) agent.state() ;
			R.statesAfter = agentState2Configuration(stateAfterAction) ;
			var tr = new MBTTransition(R.statesBefore,R.executedAction,R.statesAfter) ;
			
			// register coverage:
			registerStateCover(agent) ;
			registerTransitionCover(tr) ;
			
			// if transition-inference is turned on, add tr if it is not in the
			// model:
			if (inferTransitions) {
				List<MBTTransition> outgoing = model.transitions.get(R.statesBefore ) ;
				if (outgoing == null) {
					outgoing = new LinkedList<>() ;
					model.transitions.put(R.statesBefore,outgoing) ;
					
				}
				if (! outgoing.contains(tr)) {
					outgoing.add(tr) ;
				}
			}
			
			// check post-conds:
			for (var pcond : selected.postConditions) {
				var pcOK = pcond.P.test(stateAfterAction) ;
				if (! pcOK) {
					R.violatedPostConditions.add(pcond.name) ;
				}
			}
			if (R.violatedPostConditions.size() > 0) {
				R.postConditionViolationFound = true ;
				DEBUGPrintln("-- some post-conds are VIOLATED: " + R.violatedPostConditions) ;
			}
			
			// update vtable:
			Float oldValue = vtable.getTransitionValue(tr) ;
			if (oldValue == null)
				oldValue = 1f ;
			
			Float directReward =  1f / (float) coveredTransitions.get(tr) ;
			
			float alpha = 0.5f ;
			float gamma = 0.55f ;
			var nextOutgoingsV = vtable.transValues.get(tr.dest) ;
			float bestNextNextValue = 0 ;
			if (nextOutgoingsV != null) {
				for (var nextnextVal : nextOutgoingsV.values()) {
					if (nextnextVal > bestNextNextValue)
						bestNextNextValue = nextnextVal ;
				}
			}
			float newValue = (1 - alpha) * oldValue 
					+ alpha * (directReward + gamma*bestNextNextValue) ;
			
			vtable.updateTransitionValue(tr, newValue) ;
			
		}
		else {
			// hhm will not update the vtable in the case of fail...
			DEBUGPrintln("-- execution of the action FAILED.") ;
		}
		
		return R ;
		
	}
	
	/**
	 * Use the model to generate and execute a test-sequence. It returns
	 * action-execution-results that failed or violated the actions'
	 * post-conditions.
	 * 
	 * <p>The method returns a sequence of ActionExecutionResults obtained
	 * from every executed action in the sequence. The transitions can
	 * be reconstructed from this sequence of results. The results also
	 * include information on failed action/s and violated post-conditions.
	 */
	@SuppressWarnings("unchecked")
	public List<ActionExecutionResult> generateTestSequence(TestAgent agent, int maxDepth) {
		// register the coverage on the initial-state:
		registerStateCover(agent) ;
        
		// now we generate the test
		List<ActionExecutionResult> sequence = new LinkedList<>() ;
		MBTTransition previousTransition = null ;
		for (int k=0; k<maxDepth; k++) {
			ActionExecutionResult R = executeNext(previousTransition,agent) ;
			if (R == null) {
				// there are no next-action possible
				break ;
			}
			previousTransition = R.getTransition() ;
			sequence.add(R) ;
			if (stopOnFailedOrViolation && (! R.executionSuccessful || R.postConditionViolationFound)) {
				break ;
			}
		}
		return sequence ;	
	}
	
	/**
	 * Use the model to generate and execute a test-sequence. It returns
	 * action-execution-results that failed or violated the actions'
	 * post-conditions.
	 * 
	 * <p>The method returns a sequence of ActionExecutionResults obtained
	 * from every executed action in the sequence. The transitions can
	 * be reconstructed from this sequence of results. The results also
	 * include information on failed action/s and violated post-conditions.
	 */
	public List<List<ActionExecutionResult>> generate(Function<Void,TestAgent> initializer, 
			int numOfTestSequences,
			int maxDepth) {
		
		List<List<ActionExecutionResult>> suite = new LinkedList<>() ;
		for (int n=0; n<numOfTestSequences; n++) {
			// run the initializer to set the SUT state to a state suitable
			// for running the model. This can potentially re-launch the
			// SUT, or simply doing some execution on it to move it to a
			// different state:
			var agent = initializer.apply(null) ;
			if (agent == null)
				break ;
			var seq = generateTestSequence(agent,maxDepth) ;
			if (!seq.isEmpty())
				suite.add(seq) ;
			
			boolean seqOk = seq.stream()
							.allMatch(R -> R.executionSuccessful && ! R.postConditionViolationFound) ;
			
			if (stopOnFailedOrViolation && !seqOk) 
				break ;
		}
		return suite ;
	}
	
	

}
