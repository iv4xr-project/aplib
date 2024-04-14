package eu.iv4xr.framework.goalsAndTactics;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.utils.Pair;

public class XQalg extends BasicSearch {

	
	public static class ActionInfo {
		// public float avrgReward ;
		// let's only use max reward:
		public float maxReward ; 
	}
	
	/** 
	 * A map from states to their visit-counts. 
	 */
	public Map<String,Integer> visitCount = new HashMap<>() ;
	
	public Map<String,Map<String,ActionInfo>> qtable = new HashMap<>() ;
		
	public float exploreProbability = 0.2f ;
	
	public float maxReward = 10000 ;
		
	/**
	 * Learning rate
	 */
	public float alpha = 0.8f ;
	
	/**
	 * Discount factor
	 */
	public float gamma = 0.99f ;
	
	/**
	 * Wipe the agent memory on visited places. Only the navigation nodes need to be wiped;
	 * seen entities can be kept.
	 */
	public Function<TestAgent,Void> wipeoutMemory ;
	
	
	public List<String> winningplay = null ;
	
	
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
		// flattened-version of the trace, which we will take as a representation of
		// the current q-state:
		String qstate = "" ;
		float totalEpisodeReward = 0 ;
		
		while (trace.size() < maxDepth) {
			
			//System.out.println(">>> TRACE: " + trace) ;
			Integer visited = visitCount.get(qstate) ;
			if (visited == null) {
				// we have not seen this state before. Register it in the qtable,
				// and also calculate possible actions on this state
				
				// System.out.println(">>> state not yet visited: " + qstate) ;
				// reset exploration, then do full explore:
				wipeoutMemory.apply(agent) ;
				solveGoal("Exploration", exploredG.apply(null), explorationBudget);
				var entities = wom().elements.values().stream()
						.filter(e -> isInteractable.test(e))
						.collect(Collectors.toList());
				 Map<String,ActionInfo> actions = new HashMap<>() ;
				 for (var e : entities) {
					 var info = new ActionInfo() ;
					 info.maxReward = 0 ;
					 actions.put(e.id, info) ;
				 }
				 qtable.put(qstate, actions) ;
				 visited = 1 ;
			}
			else visited++ ;
			visitCount.put(qstate, visited) ;
			
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
			qstate += "," + entityToInteract ;
			var status = solveGoal("Reached and interacted " + entityToInteract, G, budget_per_task);

			 // break the episode if the interaction failed:
			if (status.failed()) {
				log("*** There is no entity left that the agent can intertact. Terminating current search episode.");
				break;
			}
			
			if (! topGoalPredicate.test(agentState()) && ! agentIsDead() ) {
				wipeoutMemory.apply(agent) ;
				solveGoal("Exploration", exploredG.apply(null), explorationBudget);				
			}
			
			var value1 = valueOfCurrentGameState() ;
			
			if (topGoalPredicate.test(agentState())) {
				markThatGoalIsAchieved(trace) ;
				info.maxReward = value1 ;
				totalEpisodeReward = info.maxReward ;
				log("*** Goal is ACHIEVED");
				break ;
			}
			if (agentIsDead()) {
				info.maxReward = value1 ; ;
				totalEpisodeReward = info.maxReward ;
				log("*** The agent is DEAD.");
				break;
			}
			
			// The case when the interaction brings us to a non-terminal state.
		    // We are now at the "next state" T reached after executing the interaction,
			// and exploration has been done to evaluate the reward of that state.
			
		    // define obtained reward as the diff between the value of the new and previous states:
			var reward = value1 - value0 ;
			totalEpisodeReward += reward ;

			// calculate the maximum rewards if we continue from that next state T:
			// note that the trace is already extended with the last action taken
			 var nextnextActions = qtable.get(qstate) ;
			 float S_maxNextReward = -100 ;
			 if (nextnextActions == null) {
				 var entities = wom().elements.values().stream()
							.filter(e -> isInteractable.test(e))
							.collect(Collectors.toList());
				 Map<String,ActionInfo> actions = new HashMap<>() ;
				 for (var e : entities) {
					 var info2 = new ActionInfo() ;
					 info2.maxReward = 0 ;
					 actions.put(e.id, info2) ;
				 }
				 qtable.put(qstate, actions) ;
				 nextnextActions = actions ;
				 visitCount.put(qstate, 1) ;
				 S_maxNextReward = 0 ;
			 }
			 else {
				 for (var v : nextnextActions.values()) {
						if (v.maxReward > S_maxNextReward) {
							S_maxNextReward = v.maxReward ;
						}
				 }
			 }
			 // calculate the new reward (prevstate,a):
			 info.maxReward = (1 - alpha) * info.maxReward
					           + alpha * (reward + gamma * S_maxNextReward) ;
			
		}
		closeEnv_() ;
		return totalEpisodeReward ;
	}
	
}
