package eu.iv4xr.framework.extensions.mbt;

import java.util.* ;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.utils.Pair;

public class MBTRunner<S extends State> {
	
	public MBTModel<S> model ;
	
	/**
	 * If true, then executed transitions will be added to the model, if
	 * it is not already there. Default: false.
	 */
	public boolean inferTransitions = false ;
	
	public Random rnd = new Random(3731) ;
	
	public boolean DEBUG = true ;
	
	/**
	 * If true, sequence generation will stop at the first failed
	 * action, or first violation of post-conditions.
	 * Default: true.
	 */
	public boolean stopSeqGenerationOnFailedOrViolation = true ;
	
	/**
	 * If true, suite generation will stop after a sequence detects
	 * a failed action, or violation of post-conditions.
	 * Default: true.
	 */
	public boolean stopSuiteGenerationOnFailedOrViolation = true ;
	
	/**
	 * If non-null, sequence generation will stops right after, or few steps
	 * after, this predicates becomes true.
	 * The field {@link #additionalStepsAfterGameOver} specifies how many steps
	 * after.
	 */
	public Predicate<S> isGameOver = null ;
	
	/**
	 * See {@link #isGameOver}. Default is 0.
	 */
	public int additionalStepsAfterGameOver = 0 ;
	
	
	// We won't do this. It makes the runner quite complicated
	// e.g. for coverage counting. Instead, you should have a
	// model where a transition ~g -> t is added for each g -> t
	//
	// public boolean alsoTryUnenabledActions = false ;
	
	public enum ACTION_SELECTION { RANDOM , Q }
	
	public ACTION_SELECTION actionSelectionPolicy = ACTION_SELECTION.RANDOM ;
	
	// Qalg related hyper params:
	public float QexploreProbability = 0.5f ;
	public float Qalpha = 0.5f ;
	public float Qgamma = 0.6f ;
		
	public Map<String,Integer> coveredStates = new HashMap<>() ;
	public Map<MBTTransition,Integer> coveredTransitions = new HashMap<>() ;
	public TransitionValueTable vtable = new TransitionValueTable() ;
	
	public MBTRunner() { }
	public MBTRunner(MBTModel<S> model) {
		this.model = model ;
	}
	
	public void DEBUGPrint(String str) {
		if (! DEBUG) return ;
		System.out.print(str);
	}
	
	public void DEBUGPrintln(String str) {
		if (! DEBUG) return ;
		System.out.println(str);
	}
	
	public void log(String str) {
		System.out.println(str) ;
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
	
	
	public static Set<String> getFailedActionsFromSeqResult(List<ActionExecutionResult> Rs) {
		return Rs.stream()
			.filter(R -> ! R.executionSuccessful)
			.map(R -> R.executedAction)
			.collect(Collectors.toSet()) ;
	}
	
	public static Set<String> getFailedActionsFromSuiteResults(List<List<ActionExecutionResult>> RS) {
		Set<String> failed = new HashSet<>() ;
		for (var Z : RS) failed.addAll(getFailedActionsFromSeqResult(Z)) ;
		return failed ;
	}
	
	public static Set<Pair<String,List<String>>> getViolatedPostCondsFromSeqResult(List<ActionExecutionResult> Rs) {
		return Rs.stream()
			.filter(R -> R.postConditionViolationFound)
			.map(R -> new Pair<>(R.executedAction, R.violatedPostConditions))
			.collect(Collectors.toSet()) ;
	}
	
	public static Set<Pair<String,List<String>>> getViolatedPostCondsFromSuiteResults(List<List<ActionExecutionResult>> RS) {
		Set<Pair<String,List<String>>> violations = new HashSet<>() ;
		for (var Z : RS)
			violations.addAll(getViolatedPostCondsFromSeqResult(Z)) ;
		return violations ;
	}
	
	
	/**
	 * Implement action selection heuristics. The method select one of the
	 * given set of actions. The set is assumed to be non-empty, and consists
	 * of actions that are enabled in the current SUT state.
	 */
	public MBTAction<S> selectAction(
			MBTStateConfiguration st,
			List<MBTAction<S>> enabledActions) {
		
		int N = enabledActions.size() ;
		
		switch(actionSelectionPolicy) {
			
		case RANDOM: return enabledActions.get(rnd.nextInt(N)) ;
			
		case Q : 
			if (rnd.nextFloat() <= QexploreProbability) {
				DEBUGPrintln(">>> Q-explore!") ;
				return enabledActions.get(rnd.nextInt(N)) ;
			}
			DEBUGPrintln(">>> Q-exploit") ;
			// else choose the action with the highest q-value:
			var actions_ = enabledActions.stream().map(a -> a.name).collect(Collectors.toList()) ;
			var candidates1 = vtable.getActionsWithMaxAverageValue(st, actions_) ;
			if (candidates1 == null) {
				// fall back to random:
				 return enabledActions.get(rnd.nextInt(N)) ;
			}
			
			// if there are enabled actions that are untried, we will
			// assume a value of 1 for them.
			// We then get actions with the max average according to the
			// vtable. If they have better value than 1, we choose one of
			// them. And else we choose one of the untried actions.
			//
			var knownOutGoingActions = vtable.getOutgoingActions(st) ;
			var untriedActions = actions_.stream()
						.filter(a -> !knownOutGoingActions.contains(a))
						.collect(Collectors.toList()) ;
			
			String selected = null ;
			
			if (candidates1.snd > 1 || untriedActions.isEmpty()) {
				var candidates1x = candidates1.fst ;
				selected = candidates1x.get(rnd.nextInt(candidates1x.size())) ;
			}
			else {
				selected = untriedActions.get(rnd.nextInt(untriedActions.size())) ;
			}
			
			final String selected_ = selected ;
			
			var A = enabledActions.stream()
					.filter(a -> a.name.equals(selected_))
					.collect(Collectors.toList())
					.get(0) ;
			return A ;
		}
		// should not come here
		return null ;
	}
	
	/**
	 * Generate the next test-step (transition).
	 * 
	 * 
	 * @param previousTrasition null, if there is no previous transition (or if we don't care).
	 * @param agent
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ActionExecutionResult executeNext(MBTTransition previousTrasition, TestAgent agent) {
		
		S stateBeforeAction = (S) agent.state() ;
		var configBeforeAction = agentState2Configuration(stateBeforeAction) ;
		DEBUGPrintln(">> active states: " + configBeforeAction.states) ;
		
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
			DEBUGPrintln("-- NO enabled action is available.") ;
			return null ;	
		}
		
		// select an action to do:
		var selected = selectAction(configBeforeAction,actions) ;
		
		DEBUGPrintln("-- executing " + selected.name) ;
		
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
			
			var nextOutgoingsV = vtable.transValues.get(tr.dest) ;
			float bestNextNextValue = 0 ;
			if (nextOutgoingsV != null) {
				for (var nextnextVal : nextOutgoingsV.values()) {
					if (nextnextVal > bestNextNextValue)
						bestNextNextValue = nextnextVal ;
				}
			}
			float newValue = 
					  (1 - Qalpha) * oldValue 
					+ Qalpha * (directReward + Qgamma * bestNextNextValue) ;
			
			DEBUGPrintln(">>> tr " + tr) ;
			DEBUGPrintln(">>> old-qval " + oldValue) ;
			DEBUGPrintln(">>> new-qval " + newValue) ;
			
			vtable.updateTransitionValue(tr, newValue) ;
			
		}
		else {
			// hhm will not update the vtable in the case of fail...
			DEBUGPrintln("-- execution of the action FAILED.") ;
		}
		
		return R ;
		
	}
	
	/**
	 * The same as {@link #generateTestSequence(TestAgent, int, Integer)},
	 * but with null/infinite time budget.
	 */
	public List<ActionExecutionResult> generateTestSequence(TestAgent agent, int maxDepth) {
		return generateTestSequence(agent,maxDepth,null) ;
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
	 * 
	 * <p>Time-budget can be specified (in ms). If null, then this is ignored (so,
	 * infinite budget).
	 */
	@SuppressWarnings("unchecked")
	public List<ActionExecutionResult> generateTestSequence(TestAgent agent, int maxDepth, Integer timeBudget) {
		// register the coverage on the initial-state:
		registerStateCover(agent) ;
        
		// now we generate the test
		List<ActionExecutionResult> sequence = new LinkedList<>() ;
		MBTTransition previousTransition = null ;
		S state = (S) agent.state() ;
		// clone budget:
		Integer budget_ = null ;
		if (timeBudget != null) {
			budget_ = timeBudget.intValue() ;
		}
		int afterDeathStepCount = 0 ;
		
		for (int k=0; k<maxDepth && budget_ != null && budget_ > 0 ; k++) {
			var t0 = System.currentTimeMillis() ;
			ActionExecutionResult R = executeNext(previousTransition,agent) ;
			if (R == null) {
				// there are no next-action possible
				break ;
			}
			previousTransition = R.getTransition() ;
			sequence.add(R) ;
			if (stopSeqGenerationOnFailedOrViolation && (! R.executionSuccessful || R.postConditionViolationFound)) {
				break ;
			}
			// check if gameover is true, and if the seq needs to be terminated::
			if (isGameOver != null && isGameOver.test(state)) {
				if (afterDeathStepCount >= additionalStepsAfterGameOver) break ;
				afterDeathStepCount++ ;
			}
			if (budget_ != null)
				budget_ = budget_ - (int)((System.currentTimeMillis() - t0)) ;			
		}
		return sequence ;	
	}
	
	
	/**
	 * The same as {@link #generate(Function, int, int, Integer), but with null 
	 * time-budget (so, infinite time budget).
	 */
	public List<List<ActionExecutionResult>> generate(Function<Void,TestAgent> initializer, 
			int numOfTestSequences,
			int maxDepth) {
		return generate(initializer,numOfTestSequences,maxDepth,null) ;
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
	 * 
	 * <p>Time-budget can be specified (in ms). If null, then this is ignored (so,
	 * infinite budget).
	 */
	public List<List<ActionExecutionResult>> generate(Function<Void,TestAgent> initializer, 
			int numOfTestSequences,
			int maxDepth,
			Integer timeBudget) {
		
		// load transitions in the model, if any is kept there, into the vtable
		vtable.initializeFromModel(model,1);
		
		// clone budget:
		Integer budget_ = null ;
		if (timeBudget != null) {
			budget_ = timeBudget.intValue() ;
		}
		
		List<List<ActionExecutionResult>> suite = new LinkedList<>() ;
		for (int n=0; n<numOfTestSequences && budget_ != null && budget_>0 ; n++) {
			
			var t0 = System.currentTimeMillis() ;
			
			// run the initializer to set the SUT state to a state suitable
			// for running the model. This can potentially re-launch the
			// SUT, or simply doing some execution on it to move it to a
			// different state:
			var agent = initializer.apply(null) ;
			if (agent == null)
				break ;
			var seq = generateTestSequence(agent,maxDepth,budget_) ;
			if (!seq.isEmpty())
				suite.add(seq) ;
			
			boolean seqOk = seq.stream()
							.allMatch(R -> R.executionSuccessful && ! R.postConditionViolationFound) ;
			
			int numOfCoveredStates = (int) coveredStates.values().stream()
							.filter(cnt -> cnt > 0)
							.count() ;
			int numOfCoveredTransitions = (int) coveredTransitions.values().stream()
					.filter(cnt -> cnt > 0)
					.count() ;
			
			log("** #suite " + suite.size() + ", #covered-states="
					+ numOfCoveredStates
					+ ", #covered-transitions:" + numOfCoveredTransitions) ;
			if (!seqOk) {
				log("   seq-" + n + " failed or violating its post-cond") ;
			}
			
			
			if (stopSuiteGenerationOnFailedOrViolation && !seqOk) 
				break ;
			
			if (budget_ != null)
				budget_ = budget_ - (int)((System.currentTimeMillis() - t0)) ;	
		}
		log("** suite generated. #suite:" + suite.size()) ;
		return suite ;
	}
	
	
	public String showCoverage(boolean showTransitions) {
		var z = new StringBuffer() ;
		z.append("** State coverage:\n") ;
		var Z = coveredStates.entrySet() ;
		for (var SC : Z) {
			z.append("   " + SC.getKey() + " (" + SC.getValue() + ")\n") ;
		}
		z.append("   #states covered: " + Z.size() + "\n") ;
		var TRS = coveredTransitions.entrySet() ;
		Set<MBTStateConfiguration> coveredConfigurations = new HashSet<>() ;
		for (var TC : TRS) {
			coveredConfigurations.add(TC.getKey().src) ;
			coveredConfigurations.add(TC.getKey().dest) ;
		}
		if (showTransitions) {
			z.append("** Transition coverage:\n") ;
			for (var TC : TRS) {
				z.append("   " + TC.getKey() + " (" + TC.getValue() + ")\n") ;
			}
		}
		z.append("   #transitions covered: " + TRS.size() + "\n") ;
		z.append("   #configurations covered: " + coveredConfigurations.size() + "\n") ;
		return z.toString() ;
	}
	
	/**
	 * As {@link #showCoverage()}, with the covered transitions always shown.
	 */
	public String showCoverage() {
		return showCoverage(true) ;
	}
	

}
