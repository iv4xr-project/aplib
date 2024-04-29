package eu.iv4xr.framework.goalsAndTactics;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.utils.Pair;

/**
 * An implementation of the Q-learning algorithm.
 * 
 * <p>The algorithm is meant to be used with high-level actions. Such an action represents
 * navigating to some object e, and interacting with it. It is assumed that the implementation
 * of the action takes care of e.g. how to steer the player agent to reach e's location.
 * The underlying navigation and exploration capabilities need to be provided. See e.g. 
 * {@link BasicSearch#exploredG} and {@link BasicSearch#reachedG}.
 * 
 *
 * @param <QState>
 */
public class XQalg<QState> extends BasicSearch {

	
	public static class ActionInfo {
		// public float avrgReward ;
		// let's only use max reward:
		public float maxReward ; 
	}
	
	
	public Map<QState,Map<String,ActionInfo>> qtable = new HashMap<>() ;
		
	public float exploreProbability = 0.2f ;
	
	public float maxReward = 10000 ;
	
	/**
	 * A function that construct a state-representation of a given agent/SUT state. This state representation is
	 * the one that is used in the Q-table {@link #qtable}. More precisely, the function gets a pair (tr,s)
	 * as input, where s is the agent/SUT state, and tr is the trace of the actions to get to that state. Using
	 * the latter information would make the Q-table essentially become non-Markovian. But it is a simpler representation
	 * of different sequence of actions are less likely to lead to the same state.
	 */
	@SuppressWarnings("rawtypes")
	public BiFunction<List<String>,Iv4xrAgentState,QState> getQstate ;
		
	/**
	 * Learning rate
	 */
	public float alpha = 0.8f ;
	
	/**
	 * Discount factor
	 */
	public float gamma = 0.99f ;
	
	/**
	 * If the value is bigger than 1, then the reward update on Q(current-state,action) with the reward
	 * will be propagated further to previous states. To this end, the trace of previous states and the
	 * action taken on each of those states is kept. The back-propagation will be done up to K states
	 * in the past, including the current state. This variable enableBackPropagationOfReward specifies
	 * this K. 
	 * 
	 * <p>Keep in mind that making this K too long (e.g. to force the reward to be propagated all the
	 * way to the initial state) may not be a good choice as it may cause sub-optimal plays to stick
	 * in the table and the algorithm may find it more difficult to escape these sub-optimal plays.
	 * There is interplay with {@link #exploreProbability}, as the latter allows the algorithm to 
	 * escape sub-optimals. However, unlike in MTCS, {@link #exploreProbability} remains constant through
	 * the entire Q-runs, regardless how many times a certain pair (state,action) has been revisited. 
	 *
	 */
	public int enableBackPropagationOfReward = 0 ;
	
	public XQalg() {
		super() ;
		algName = "Q" ;
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
		
		QState qstate =  getQstate.apply(trace,agentState()) ;
		
		wipeoutMemory.apply(agent) ;
		solveGoal("Exploration", exploredG.apply(null), explorationBudget);
		var startingEntities = wom().elements.values().stream()
				.filter(e -> isInteractable.test(e))
				.collect(Collectors.toList());
		Map<String,ActionInfo> firstActions = new HashMap<>() ;
		for (var e : startingEntities) {
			 var info = new ActionInfo() ;
			 info.maxReward = 0 ;
			 firstActions.put(e.id, info) ;
		}
		qtable.put(qstate,firstActions) ;
		
		float totalEpisodeReward = 0 ;
		
		while (trace.size() < maxDepth) {
			
			//System.out.println(">>> TRACE: " + trace) ;
			var candidateActions = qtable.get(qstate) ;
	
			if (candidateActions.isEmpty()) 
				// no further actions is possible, so we stop the episode
				break ;
			
			String chosenAction = null ;
			if (rnd.nextFloat() <= exploreProbability) {
				// explore:
				var actions = candidateActions.keySet().stream().collect(Collectors.toList()) ;
				chosenAction = actions.get(rnd.nextInt(actions.size())) ;
			}
			else {
				float bestVal = Float.NEGATIVE_INFINITY ;
				//System.out.println(">>> cadidates : " + candidateActions.size()) ;
				
				for (var a : candidateActions.entrySet()) {
					//System.out.println(">>> " + a.getKey()  + ", " + a.getValue().maxReward) ;
					if (a.getValue().maxReward > bestVal) {
						bestVal = a.getValue().maxReward ;
					}
				}
				// get the actions with the best value (we could have multiple)
				final float bestVal_ = bestVal ;
				var bestCandidates = candidateActions.entrySet()
						.stream()
						.filter(e -> e.getValue().maxReward >= bestVal_)
						.collect(Collectors.toList()) ;
				
				chosenAction = bestCandidates.get(rnd.nextInt(bestCandidates.size())).getKey() ;
			}
			var entityToInteract = chosenAction ;
			var info = candidateActions.get(entityToInteract) ;
		    System.out.println(">>> chosen-action : " + chosenAction + ", info:" + info.maxReward) ;
		    // now, execute the action:
		    var value0 = valueOfCurrentGameState() ;
		    var G = SEQ(reachedG.apply(entityToInteract), interactedG.apply(entityToInteract));
			trace.add(entityToInteract) ;
			var status = solveGoal("Reached and interacted " + entityToInteract, G, budget_per_task);
			// the state after the interaction:
			var newQstate =  getQstate.apply(trace,agentState()) ;

			 // break the episode if the interaction failed:
			if (status.failed()) {
				log("*** There is no entity left that the agent can intertact. Terminating current search episode.");
				break;
			}
			
			// the case when newQstate is a terminal state:
			
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
			
			// We need to explore to assess the value of the state after the interaction:
			wipeoutMemory.apply(agent) ;
			solveGoal("Exploration", exploredG.apply(null), explorationBudget);	
			var value1 = valueOfCurrentGameState() ;
			totalEpisodeReward = value1 ;
			
			// the case when the state after exploration is terminal:
			if (topGoalPredicate.test(agentState())) {
				markThatGoalIsAchieved(trace) ;
				info.maxReward = this.maxReward ;
				totalEpisodeReward = this.maxReward ;
				log("*** Goal is ACHIEVED");
				backPropagation(newQstate,chosenAction,info.maxReward,stateActionRewardTrace) ;
				break ;
			}
			else if (agentIsDead()) {
				info.maxReward = value1 ; ;
				log("*** The agent is DEAD.");
				backPropagation(newQstate,chosenAction,info.maxReward,stateActionRewardTrace) ;
				break;
			}
			
			// else the state after exploration is non-terminal.
						 
		    // define obtained reward as the diff between the value of the new and previous states:
			var reward = value1 - value0 ;
			

			// calculate the maximum rewards if we continue from that next state T:
			// note that the trace is already extended with the last action taken
			var nextnextActions = qtable.get(newQstate) ;
			// first check if newQstate is already in the qtable. If not, add an entry to it.
			if (nextnextActions == null) {
				 var entities = wom().elements.values().stream()
							.filter(e -> isInteractable.test(e))
							.collect(Collectors.toList());
				 nextnextActions = new HashMap<>() ;
				 for (var e : entities) {
					 var info2 = new ActionInfo() ;
					 info2.maxReward = 0 ;
					 nextnextActions.put(e.id, info2) ;
				 }
				 qtable.put(newQstate, nextnextActions) ;
			}
			
			// update the Qtable
			updateQ(qstate,chosenAction, newQstate,reward) ;
			backPropagation(newQstate,chosenAction,reward,stateActionRewardTrace) ;
			qstate = newQstate ;
			
		}
		closeEnv_() ;
		return totalEpisodeReward ;
	}
	
	void backPropagation(QState newState, 
			String chosenAction, 
			float directReward,
			List<Pair<QState,Pair<String,Float>>> stateActionRewardTrace) {
		if (enableBackPropagationOfReward <= 1) 
			return ;
		var state2 = newState ;
		for(int k = stateActionRewardTrace.size()-1 ; k>=0; k--) {
			var h = stateActionRewardTrace.get(k) ;
			var state1 = h.fst ;
			var action = h.snd.fst ;
			var directReward_of_state1_action = h.snd.snd ;
			var current_value_of_state1_action = qtable.get(state1).get(directReward_of_state1_action).maxReward ;
			var new_value = updateQ(state1,action,state2,directReward_of_state1_action) ;
			// if the new value is less than the current value, restore the current value,
			// and stop the back-propagation as it won't change the values further down
			// the propagation:
			if (new_value < current_value_of_state1_action) {
				 qtable.get(state1).get(action).maxReward = current_value_of_state1_action ;
				 break ;
			}
			state2 = state1 ;
		}
		stateActionRewardTrace.add(new Pair<>(newState, new Pair<>(chosenAction,directReward))) ;
		if (stateActionRewardTrace.size() > enableBackPropagationOfReward - 1)
			stateActionRewardTrace.remove(0) ;
		
	}
	
	float updateQ(QState qstate, String action, QState nextQstate, float directReward) {
		
		var nextnextActions = qtable.get(nextQstate) ;
		float S_maxNextReward = 0 ;
		if (nextnextActions.isEmpty()) {
			// then nextQstate is a terminal and not a winning state, we'll set its reward to be 0
		}
		else {
			S_maxNextReward = Float.MIN_VALUE ;
			for (var v : nextnextActions.values()) {
				if (v.maxReward > S_maxNextReward) {
					S_maxNextReward = v.maxReward ;
				}
			}
		}
		
		// update the value of Q(qstate,action):
		var info = qtable.get(qstate).get(action) ;
		info.maxReward = (1 - alpha) * info.maxReward
							         + alpha * (directReward + gamma * S_maxNextReward) ;
		return info.maxReward ;
	}
	
}
