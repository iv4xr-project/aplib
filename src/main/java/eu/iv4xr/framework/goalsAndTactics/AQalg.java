package eu.iv4xr.framework.goalsAndTactics;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;

import java.util.*;
import java.util.stream.Collectors;

import eu.iv4xr.framework.goalsAndTactics.BasicSearch.RerunWinningPlayResult;
import eu.iv4xr.framework.goalsAndTactics.XQalg.ActionInfo;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.utils.Pair;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.Iv4xrEnvironment ;

/**
 * An implementation of the Q-learning algorithm. This is implemented as a subclass of
 * {@link XQalg}, but is meant to work with low-level actions.
 */

public class AQalg<QState> extends XQalg<QState> {
	
	public Map<String,Action>availableActions = new HashMap<>() ;
	
	public AQalg() {
		super() ;
		algName = "Q-basic" ;
	}
	
	/**
	 * Get the set of actions and their values of a given state from the Q-table.
	 * If the state is not in the table yet, a new entry for that state will be
	 * created. All actions from {@link #availableActions} will be added, and the
	 * values are all set to 0.
	 */
	@Override
	Map<String,ActionInfo> getActionsInfoOnState(QState qstate) {
		var q_entry = qtable.get(qstate) ;
		if (q_entry == null) {
			// the state has not been registered
			q_entry = new HashMap<String,ActionInfo>() ;
			for (var actionName : availableActions.keySet()) {
				var info = new ActionInfo() ;
				info.maxReward = 0 ;
				q_entry.put(actionName, info) ;
			}
			qtable.put(qstate, q_entry) ;
		}
		return q_entry ;
	}
	
	/**
	 * Run a single episode of Q-learning.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	float runAlgorithmForOneEpisode() throws Exception {
		initializeEpisode() ;
		// sequence of interactions so-far
		List<String> trace = new LinkedList<>() ;
		List<Pair<QState,Pair<String,Float>>> stateActionRewardTrace = new LinkedList<>() ;
		
		var currenState = agentState() ;
		QState qstate =  getQstate.apply(trace,currenState) ;
		var q_entry = getActionsInfoOnState(qstate) ;
		registerQstate(qstate) ;
		
		float episodeReward = clampedValueOfCurrentGameState() ;
		
		while (trace.size() < maxDepth) {
				
			// decide the next actions to do:
			var currenState_ = currenState ; // copy ref to a new var, else lambda complains
			List<String> enabledActions = availableActions.entrySet().stream()
				.filter(a -> a.getValue().isEnabled(currenState_))
				.map(a -> a.getKey())
				.collect(Collectors.toList()) ;
	
			if (enabledActions.isEmpty()) 
				// no further actions is possible, so we stop the episode
				break ;
			
			//q_entry = getActionsInfoOnState(qstate) ;
			String chosenAction = null ;
			
			if (rnd.nextFloat() <= exploreProbability) {
				// explore:
				chosenAction = enabledActions.get(rnd.nextInt(enabledActions.size())) ;
			}
			else {
				float bestVal = Float.NEGATIVE_INFINITY ;
				//System.out.println(">>> cadidates : " + candidateActions.size()) ;
				
				for (var a : enabledActions) {
					//System.out.println(">>> " + a.getKey()  + ", " + a.getValue().maxReward) ;
					if (q_entry.get(a).maxReward > bestVal) {
						bestVal = q_entry.get(a).maxReward ;
					}
				}
				// get the actions with the best value (we could have multiple)
				final float bestVal_ = bestVal ;
				var q_entry_ = q_entry ;
				var bestCandidates = enabledActions.stream()
						.filter(a -> q_entry_.get(a).maxReward >= bestVal_)
						.collect(Collectors.toList()) ;
				
				chosenAction = bestCandidates.get(rnd.nextInt(bestCandidates.size())) ;
			}
		    // System.out.println(">>> chosen-action : " + chosenAction + ", info:" + info.maxReward) ;
		    
			// now, execute the action:
		    var value0 = clampedValueOfCurrentGameState() ;
		    Action action = availableActions.get(chosenAction) ;
		    action.exec1(currenState) ;
		    var newObs = ((Iv4xrEnvironment) agent.env()).observe(agent.getId()) ;
		    ((Iv4xrAgentState) agent.state()).worldmodel = newObs ;
		    trace.add(chosenAction) ;
		    var newState = agentState() ;
			var newQstate =  getQstate.apply(trace,newState) ;
			
			// current entry of Q(currentState,chosenAction):
			var info = q_entry.get(chosenAction) ;
			
			if (topGoalPredicate.test(newState)) {
				markThatGoalIsAchieved(trace) ;
				info.maxReward = this.maxReward ;
				episodeReward = info.maxReward;
				log("*** Goal is ACHIEVED");
				backPropagation(newQstate,chosenAction,info.maxReward,stateActionRewardTrace) ;
				break ;
			}
			else if (agentIsDead()) {
				info.maxReward = clampedValueOfCurrentGameState()  ; ;
				episodeReward = info.maxReward ;
				log("*** The agent is DEAD.");
				backPropagation(newQstate,chosenAction,info.maxReward,stateActionRewardTrace) ;
				break;
			}
			// else then the top-goal has not been achieved, and the agent is alive. 
			// The newQstate is thus non-terminal.

			// create entry for newQstate in the Q-table if it is not there yet:
			var q_entry_of_newQstate = getActionsInfoOnState(newQstate) ;
			
			var value1 = clampedValueOfCurrentGameState() ;
			
			// obtain the direct reward of performing the chosen action. This is calculated either through
			// actionDirectRewardFunction, if it is defined. And else the direct reward is defined to be
			// the difference in the value of state and newState.
			float reward = 0 ;
			if (actionDirectRewardFunction != null) {
				reward = actionDirectRewardFunction.apply(new Pair<>(currenState,chosenAction), newState) ;
			}
			else 
				reward = value1 - value0 ;

			// update the Qtable
			updateQ(qstate,chosenAction, newQstate, reward) ;
			backPropagation(newQstate,chosenAction,reward,stateActionRewardTrace) ;
			currenState = newState ;
			qstate = newQstate ;
			q_entry = q_entry_of_newQstate ;
			// the value of the episode so far is defined simply as the value of the new current state:
			episodeReward = value1 ;
			
			Thread.sleep(delayBetweenAgentUpateCycles);
		}
		
		foundError = foundError || ! agent.evaluateLTLs() ;
		closeEnv_() ;
		return episodeReward ;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public RerunWinningPlayResult runTrace(List<String> trace) throws Exception {
		initializeEpisode();
		
		log(">>> executing a trace: " + trace);
		
		var result = new RerunWinningPlayResult() ;
		result.indexOfLastExecutedStep = 0 ;
		result.traceLength = trace.size() ;
		for (var a : trace) {
			// execute the action:
			Action action = availableActions.get(a) ;
		    action.exec1(agentState()) ;
		    var newObs = ((Iv4xrEnvironment) agent.env()).observe(agent.getId()) ;
		    // set the observation as the new agent-state:
		    ((Iv4xrAgentState) agent.state()).worldmodel = newObs ;
			// the state after the interaction:

			if (topGoalPredicate.test(agentState())) {
				result.topPredicateSolved = true ;
				log("*** Goal is ACHIEVED");
				break ;
			}
			if (agentIsDead()) {
				result.agentIsDead = true ;
				result.aborted = true ;
				log("*** The agent is DEAD.");
				break;
			}
			result.indexOfLastExecutedStep++ ;
		}
		closeEnv_() ;

		// cannot check LTL with this mode:
		// result.violationDetected = ! agent.evaluateLTLs() ;
		
		return result ;
	}

	
	/**
	 * Use the model to play the target game. This will always choose the next action which
	 * gives the best future reward, according to the model. The method returns the sequence
	 * af action
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Pair<List<String>,Float> play(int maxPlayLength) throws Exception {
		
		initializeEpisode() ;
		List<String> bestSequece = new LinkedList<>() ;
		List<QState> sequenceSoFar = new LinkedList<>() ;
		var state = agentState() ;
		QState qstate =  getQstate.apply(bestSequece,state) ;
		sequenceSoFar.add(qstate) ; 

		float totalReward = clampedValueOfCurrentGameState() ;
		
		while (bestSequece.size() < maxPlayLength) {
			
			var possibleActions = qtable.get(qstate) ;
			if (possibleActions==null || possibleActions.isEmpty()) {
				break ;
			}
			// NOTE: the lowest min-valid is not Float.MIN_VALUE (which is in fact a positive value). We use
			// negative infinity:
			float bestValue = Float.NEGATIVE_INFINITY ;
			//System.out.println(">>> step " + bestSequece.size() + ", #actions-possible:" + possibleActions.entrySet().size()) ;
			for (var option : possibleActions.entrySet()) {
				float val = option.getValue().maxReward ;
				//System.out.println("    action: " + a + ", reward: " + val) ; 
				if (val > bestValue) {
					bestValue = val ;
				}
			}
			
			float bestValue_ = bestValue ;
			var candidates = possibleActions.entrySet().stream().filter(z -> z.getValue().maxReward >= bestValue_ )
				.map(z -> z.getKey())
				.collect(Collectors.toList()) ;
			
			String bestAction = candidates.get(rnd.nextInt(candidates.size())) ;
			
			
			
			System.out.println(">>> bestAction: " + bestAction + ", bestValue: " + bestValue) ; 
			
			var value0 = clampedValueOfCurrentGameState() ;
			bestSequece.add(bestAction) ;
			
			// execute the action:
		    Action action = availableActions.get(bestAction) ;
		    action.exec1(state) ;
		    var newObs = ((Iv4xrEnvironment) agent.env()).observe(agent.getId()) ;
		    // set the observation as the new agent-state:
		    ((Iv4xrAgentState) agent.state()).worldmodel = newObs ;
			// the state after the interaction:

		    var newState = agentState() ;
			var value1 = clampedValueOfCurrentGameState() ;

			// calculate direct-reward of executing bestAction:
			float reward = 0 ;
			if (actionDirectRewardFunction != null) {
				reward = actionDirectRewardFunction.apply(new Pair<>(state,bestAction), newState) ;
			}
			else 
				reward = value1 - value0 ;
						
			totalReward += reward ;
			System.out.println(">>> reward:" + reward + ", tot:"+ totalReward) ;
			
			if (topGoalPredicate.test(newState)) {
				System.out.println(">>> Goal is ACHIEVED") ;
				log("*** Goal is ACHIEVED");
				break ;
			}
			else if (agentIsDead()) {
				System.out.println(">>> DEAD") ;
				log("*** The agent is DEAD.");
				break;
			}
			
			//System.out.println(">>> NEXT ITER") ;
			
			// advance state and qstate to newState, and then we iterate:
		    state = newState ;
			qstate =  getQstate.apply(bestSequece,newState) ;
		}
		
		return new Pair<>(bestSequece,totalReward) ;
		
	}

}
