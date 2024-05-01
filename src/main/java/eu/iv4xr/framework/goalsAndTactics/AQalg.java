package eu.iv4xr.framework.goalsAndTactics;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;

import java.util.*;
import java.util.stream.Collectors;

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
		float episodeReward = 0 ;
		
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
				episodeReward = this.maxReward ;
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
		}
		closeEnv_() ;
		return episodeReward ;
	}

}
