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

import eu.iv4xr.framework.goalsAndTactics.XQalg.ActionInfo;
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
 * <p>The algorithm needs a function to calculate the direct reward of executing an action a on
 * a state S1, and transitioning to state S2. This can be specified by {@link #actionDirectRewardFunction}.
 * If the function is left unspecified the algorithm will use the function {@link BasicSearch#stateValueFunction}
 * to calculate the value v1 and v2 of states S1 and S2, and uses v2-v1 as the reward of doing 
 * the action a on the state S1.
 * 
 * <p>To determine actions that are possible at every state, the algorithm looks in the
 * state of the agent, to infer the set of game interactables that the agent knows at
 * that moment. These are considered as the actions possible on the state.
 * Use the function {@link BasicSearch#isInteractable} to specify what game objects are
 * considered as interactables; useful for limiting the learning space.
 * 
 * @param <QState>
 */
public class XQalg<QState> extends BasicSearch {

	
	public static class ActionInfo {
		// public float avrgReward ;
		// let's only use max reward:
		public float maxReward = 0 ; 
	}
		
	public Map<QState,Map<String,ActionInfo>> qtable = new HashMap<>() ;
		
	public float exploreProbability = 0.2f ;
	
	/**
	 * If specified, this calculates the direct reward of executing an action a on a state S1,
	 * and transitioning to a state S2. If the function is unspecified, the difference of the
	 * values of S2 and S1 is used, where the values are calculated through {@link BasicSearch#stateValueFunction}.
	 */
	public BiFunction<Pair<Iv4xrAgentState,String>,Iv4xrAgentState,Float> actionDirectRewardFunction ;
	
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
	 * Get the set of actions and their values of a given state s from the Q-table.
	 * If the state is not in the table yet, a new entry for that state will be
	 * created. This is done by inspecting the agent-state, and obtaining all
	 * game interactables that the agent knows, according to its state. All these
	 * interactables a will be considered as possible action for the state s and
	 * added as as entry in the Q-table. For each new (s,a) added to the Q-table,
	 * its Q-value is set to 0.
	 */
	Map<String,ActionInfo> getActionsInfoOnState(QState qstate) {
		var q_entry = qtable.get(qstate) ;
		if (q_entry == null) {
			// the state has not been registered
			q_entry = new HashMap<String,ActionInfo>() ;
			var availableEntities = wom().elements.values().stream()
					.filter(e -> isInteractable.test(e))
					.collect(Collectors.toList());
			for (var e : availableEntities) {
				 var info = new ActionInfo() ;
				 info.maxReward = 0 ;
				 q_entry.put(e.id, info) ;
			}
			qtable.put(qstate,q_entry) ;
		}
		return q_entry ;
	}
	
	/**
	 * Add a qstate into the Q-table. If it is already in the table nothing happens.
	 * If the qstate is not in the table yet, the it as added. Furthermore, the
	 * set of possible actions on that state is calculated and added into the table.
	 * For each new pair (s,a) that is added, its Q-value it set to 0. Adding the
	 * actions uses the function {@link #getActionsInfoOnState(Object)}
	 */
	void registerQstate(QState qstate) {
		getActionsInfoOnState(qstate) ;
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
		var state = agentState() ;
		QState qstate =  getQstate.apply(trace,state) ;

		if (exploredG != null) {
			wipeoutMemory.apply(agent) ;
			solveGoal("Exploration", exploredG.apply(null), explorationBudget);			
		}
		
		registerQstate(qstate) ;
		
		float episodeReward = clampedValueOfCurrentGameState() ;
		
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
				//System.out.println(">>> candidates : " + candidateActions.size()) ;
				
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
		    var value0 = clampedValueOfCurrentGameState() ;
		    var G = SEQ(reachedG.apply(entityToInteract), interactedG.apply(entityToInteract));
			trace.add(entityToInteract) ;
			var status = solveGoal("Reached and interacted " + entityToInteract, G, budget_per_task);
			// the state after the interaction:
			var newState = agentState() ;
			var newQstate =  getQstate.apply(trace,newState) ;
			// calculate the value of the state after the interaction:
			var value1 = clampedValueOfCurrentGameState() ;

			 // break the episode if the interaction failed:
			if (status.failed()) {
				log("*** Trying to reach and interact with " + entityToInteract + ", but FAILED. Terminating current search episode.");
				break;
			}
			
			// the case when newQstate is a terminal state:
			
			if (topGoalPredicate.test(newState)) {
				markThatGoalIsAchieved(trace) ;
				info.maxReward = this.maxReward ;
				episodeReward = info.maxReward ;
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
			
			// If exploreG is defined, we use it to explore before we assess the value of the state 
			// after the interaction:
			if (exploredG != null) {
				wipeoutMemory.apply(agent) ;
				System.out.println(">>> exploring...") ;
				solveGoal("Exploration", exploredG.apply(null), explorationBudget);					
				// sample the state again:
				newState = agentState() ;
				newQstate =  getQstate.apply(trace,newState) ;	
				value1 = clampedValueOfCurrentGameState() ;
				
				// the case when the state after exploration is terminal:
				if (topGoalPredicate.test(agentState())) {
					markThatGoalIsAchieved(trace) ;
					info.maxReward = this.maxReward ;
					episodeReward = info.maxReward ;
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

			}
			
			// at this point we know that the new state is NOT terminal
			registerQstate(newQstate) ;
						 
		    // obtain the direct reward of performing the chosen action. This is calculated either through
			// actionDirectRewardFunction, if it is defined. And else the direct reward is defined to be
			// the difference in the value of state and newState.
			float reward = 0 ;
			if (actionDirectRewardFunction != null) {
				reward = actionDirectRewardFunction.apply(new Pair<>(state,chosenAction), newState) ;
			}
			else 
				reward = value1 - value0 ;
									
			// update the Qtable
			var oldVal = info.maxReward ;
			
			updateQ(qstate,chosenAction, newQstate,reward) ;
			
			System.out.println(">> chosen action: " + chosenAction
					+ ", v0=" + value0 + ", v1=" + value1 
					+ (reward > 0 ? ", DIRECT-rw=" : ", direct-rw=") + reward
					+ ", oldval=" + oldVal 
					+ ", val=" + info.maxReward 
					)  ;

			backPropagation(newQstate,chosenAction,reward,stateActionRewardTrace) ;
			// move the current state to the new state:
			state = newState ;
			qstate = newQstate ;
			// the value of the episode so far is defined simply as the value of the new current state:
			episodeReward = value1 ;
		}
		foundError = foundError || ! agent.evaluateLTLs() ;
		closeEnv_() ;
		return episodeReward ;
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
			var info = qtable.get(state1).get(action) ;
			if (info == null)
				break ;
			var current_value_of_state1_action = info.maxReward ;
			var new_value = updateQ(state1,action,state2,directReward_of_state1_action) ;
			// if the new value is less than the current value, restore the current value,
			// and stop the back-propagation as it won't change the values further down
			// the propagation:
			if (new_value < current_value_of_state1_action) {
				 info.maxReward = current_value_of_state1_action ;
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
		if (nextnextActions == null) {
			// then qstate is a terminal state, then we take 0 as the value of doing an action
			// on qstate; since no action is possible
		}
		else if (nextnextActions.isEmpty()) {
			// then nextQstate is a terminal and not a winning state, also consider the value
			// of doing an action there to be 0
		}
		else {
			S_maxNextReward = Float.NEGATIVE_INFINITY ;
			for (var v : nextnextActions.values()) {
				if (v.maxReward > S_maxNextReward) {
					S_maxNextReward = v.maxReward ;
				}
			}
		}
		//System.out.println(">>> S_maxNextReward = " + S_maxNextReward) ;
		// update the value of Q(qstate,action):
		var info = qtable.get(qstate).get(action) ;
		info.maxReward = (1 - alpha) * info.maxReward
							         + alpha * (directReward + gamma * S_maxNextReward) ;
		return info.maxReward ;
	}
	

	/**
	 * Use the model to play the target game. This will always choose the next action which
	 * gives the best future reward, according to the model. The method returns the sequence
	 * af action
	 */
	public Pair<List<String>,Float> play(int maxPlayLength) throws Exception {
		
		initializeEpisode() ;
		List<String> bestSequece = new LinkedList<>() ;
		var state = agentState() ;
		QState qstate =  getQstate.apply(bestSequece,state) ;

		if (exploredG != null) {
			wipeoutMemory.apply(agent) ;
			solveGoal("Exploration", exploredG.apply(null), explorationBudget);			
		}
		
		float totalReward = clampedValueOfCurrentGameState() ;
		
		while (bestSequece.size() < maxPlayLength) {
			
			var possibleActions = qtable.get(qstate) ;
			if (possibleActions==null || possibleActions.isEmpty()) {
				break ;
			}
			// NOTE: the lowest min-valid is not Float.MIN_VALUE (which is in fact a positive value). We use
			// negative infinity:
			float bestValue = Float.NEGATIVE_INFINITY ;
			String bestAction = null ;
			//System.out.println(">>> step " + bestSequece.size() + ", #actions-possible:" + possibleActions.entrySet().size()) ;
			for (var option : possibleActions.entrySet()) {
				String a = option.getKey() ;
				float val = option.getValue().maxReward ;
				//System.out.println("    action: " + a + ", reward: " + val) ; 
				if (val > bestValue) {
					bestValue = val ;
					bestAction = a ;
				}
			}
			//System.out.println(">>> bestAction: " + bestAction + ", bestValue: " + bestValue) ; 
			
			var value0 = clampedValueOfCurrentGameState() ;
			bestSequece.add(bestAction) ;
			String entityToInteract = bestAction ;
		    var G = SEQ(reachedG.apply(entityToInteract), interactedG.apply(entityToInteract));
			var status = solveGoal("Reached and interacted " + entityToInteract, G, budget_per_task);
			if (status.failed()) {
				//System.out.println(">>> Interaction " + bestAction + " FAILED") ;
				log("*** Trying to reach and interact with " + entityToInteract + ", but FAILED. Terminating the sequence.");
				break;
			}
			if (exploredG != null) {
				wipeoutMemory.apply(agent) ;
				solveGoal("Exploration", exploredG.apply(null), explorationBudget);			
			}
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
			
			if (topGoalPredicate.test(newState)) {
				//System.out.println(">>> Goal is ACHIEVED") ;
				log("*** Goal is ACHIEVED");
				break ;
			}
			else if (agentIsDead()) {
				//System.out.println(">>> DEAD") ;
				log("*** The agent is DEAD.");
				break;
			}
			
			//System.out.println(">>> NEXT ITER") ;
			
			// advance qstate to newState, and then we iterate:
			qstate =  getQstate.apply(bestSequece,newState) ;
		}
		
		return new Pair<>(bestSequece,totalReward) ;
		
	}
	
}
