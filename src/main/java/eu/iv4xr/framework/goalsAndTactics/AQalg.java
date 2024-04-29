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
	@Override
	float runAlgorithmForOneEpisode() throws Exception {
		initializeEpisode() ;
		// sequence of interactions so-far
		List<String> trace = new LinkedList<>() ;
		List<Pair<QState,Pair<String,Float>>> stateActionRewardTrace = new LinkedList<>() ;
		
		
		float totalEpisodeReward = 0 ;
		
		while (trace.size() < maxDepth) {
			
			//System.out.println(">>> TRACE: " + trace) ;
			var currenState = agentState() ;
			QState qstate =  getQstate.apply(trace,currenState) ;
			var q_entry = getActionsInfoOnState(qstate) ;
			
			List<String> enabledActions = availableActions.entrySet().stream()
				.filter(a -> a.getValue().isEnabled(currenState))
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
				var bestCandidates = enabledActions.stream()
						.filter(a -> q_entry.get(a).maxReward >= bestVal_)
						.collect(Collectors.toList()) ;
				
				chosenAction = bestCandidates.get(rnd.nextInt(bestCandidates.size())) ;
			}
		    // System.out.println(">>> chosen-action : " + chosenAction + ", info:" + info.maxReward) ;
		    
			// now, execute the action:
		    var value0 = valueOfCurrentGameState() ;
		    Action action = availableActions.get(chosenAction) ;
		    action.exec1(currenState) ;
		    var newObs = ((Iv4xrEnvironment) agent.env()).observe(agent.getId()) ;
		    ((Iv4xrAgentState) agent.state()).worldmodel = newObs ;
		    trace.add(chosenAction) ;
		    var newState = agentState() ;
			var newQstate =  getQstate.apply(trace,newState) ;
			
			// current entry of Q(currentState,chosenAction):
			var info = q_entry.get(chosenAction) ;
			
			if (topGoalPredicate.test(agentState())) {
				markThatGoalIsAchieved(trace) ;
				info.maxReward = this.maxReward ;
				totalEpisodeReward = this.maxReward ;
				log("*** Goal is ACHIEVED");
				backPropagation(newQstate,chosenAction,info.maxReward,stateActionRewardTrace) ;
				break ;
			}
			else if (agentIsDead()) {
				info.maxReward = valueOfCurrentGameState()  ; ;
				totalEpisodeReward = info.maxReward ;
				log("*** The agent is DEAD.");
				backPropagation(newQstate,chosenAction,info.maxReward,stateActionRewardTrace) ;
				break;
			}
			// else then the top-goal has not been achieved, and the agent is alive. 
			// The newQstate is thus non-terminal.

			// create entry for newQstate in the Q-table if it is not there yet:
			getActionsInfoOnState(newQstate) ;
			
			var value1 = valueOfCurrentGameState() ;
			totalEpisodeReward = value1 ;
			
			// define obtained reward as the diff between the value of the new and previous states:
			var reward = value1 - value0 ;

			// update the Qtable
			updateQ(qstate,chosenAction, newQstate,reward) ;
			backPropagation(newQstate,chosenAction,reward,stateActionRewardTrace) ;
			qstate = newQstate ;
		}
		closeEnv_() ;
		return totalEpisodeReward ;
	}

}
