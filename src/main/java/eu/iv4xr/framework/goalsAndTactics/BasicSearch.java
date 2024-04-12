package eu.iv4xr.framework.goalsAndTactics;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;

import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.ProgressStatus;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import static nl.uu.cs.aplib.AplibEDSL.*;

/**
 * A search algorithm. It drives the agent, which in turns drives the SUT, to
 * get the SUT to a state satisfying {@link #topGoalPredicate}. The search is
 * meant to be multi-episodic. E.g. an episode can perform a search in a certain
 * direction, up to a certain depth. The overall search will then try multiple
 * episode until {@link #topGoalPredicate} becomes true, or until {@link #totalSearchBudget}
 * is exhausted.
 * 
 * <p> The method {@link #runAlgorithm()} executes this multi-episodic search. The
 * method {@link #runAlgorithmForOneEpisode()} implements a single episode search.
 * 
 * <p>
 * This class is meant to be a template to be subclassed, but it implements a
 * form of random-search. It alternates between exploring and randomly choosing
 * an entity to go to, and then interacting with that entity.
 * 
 */
public class BasicSearch {

	/**
	 * To keep track the number of agent.updates() done so far.
	 */
	public int turn = 0;

	public String algName = "RND";

	/**
	 * A function that constructs a test agent. This may potentially also
	 * re-launch/re-deploy the SUT (your choice).
	 */
	public Function<Void, TestAgent> agentConstructor;

	/**
	 * The test-agent. Don't set this manually. This class will use
	 * {@link #agentConstructor} for creating an agent.
	 */
	TestAgent agent;

	/**
	 * Available total search-budget in ms. The default is 3-min.
	 */
	public int totalSearchBudget = 180000;

	/**
	 * Remaining total-search budget in ms.
	 */
	int remainingSearchBudget;

	/**
	 * If the search has a concept of "depth", this variable specifies a maximum
	 * depth. Default is 10.
	 */
	public int maxDepth = 10;

	/**
	 * If the search has a concept of reward or fitness, this specifies the maximum
	 * reward that can be obtained. Default is 10000.
	 */
	public float maxReward = 10000;

	/**
	 * If true, then the agent's location will be recorded in
	 * {@link #visitedLocations}. The default is false.
	 */
	public boolean traceLocation = false;

	/**
	 * If true, the algorithm will print debug-messages to the console.
	 */
	static boolean DEBUG = false;

	/**
	 * The delay (in ms) added between the agent's update cycles when executing a
	 * goal, in {@link #solveGoal(String, GoalStructure, int)}. The default is 50ms.
	 */
	public int delayBetweenAgentUpateCycles = 50;

	public int getTotalSearchBudget() {
		return totalSearchBudget;
	}

	public int getRemainingSearchBudget() {
		return remainingSearchBudget;
	}

	public void setTotalSearchBudget(int budget) {
		totalSearchBudget = budget;
		remainingSearchBudget = totalSearchBudget;
	}

	public Random rnd = new Random();

	/**
	 * The max. number of turns that each goal-based task will be allowed. If this
	 * is exceeded the task will be dropped.
	 */
	public int budget_per_task = 150;

	/**
	 * The max. number of turns spent on exploration.
	 */
	public int explorationBudget = 150;

	/**
	 * For collecting statistics. It represents the total number of "episodes" the
	 * algorithm does. With "episodes" being loosely defined.
	 */
	public int totNumberOfEpisodes = 0;

	/**
	 * In defined, this specifies the maximum number of episodes for this algorithm.
	 * With "episodes" being loosely defined. E.g. at each episode/run the algorithm
	 * performs a search, and different episodes may repeat the search but with
	 * different random choices.
	 * <br>
	 * Default: null.
	 */
	public Integer maxNumberOfEpisodes;
	
	/**
	 * If this is set to true, the search stops as soon as the {@link #topGoalPredicate} becomes
	 * true. Else the search will continue (and then it will continue until we exhaust the
	 * budget or the maximum number of episodes is reached).
	 * <br>Default: true.
	 */
	public boolean stopAfterGoalIsAchieved = true ;
	
	/**
	 * This will be set to true when {@link #topGoalPredicate} becomes
	 * true; and will remain true to indicate that the goal has ever been achieved.
	 */
	boolean goalHasBeenAchieved = false ;
	
	public boolean goalHasBeenAchieved() {
		return goalHasBeenAchieved ;
	}

	/**
	 * A predicate specifying when the search is considered completed. The predicate
	 * is evaluated on the agent's state.
	 */
	@SuppressWarnings("rawtypes")
	public Predicate<Iv4xrAgentState> topGoalPredicate;

	/**
	 * If specified, this identifies when the agent is dead (so, it can't do
	 * anything anymore).
	 */
	@SuppressWarnings("rawtypes")
	public Predicate<Iv4xrAgentState> agentIsDead;

	/**
	 * A predicate specifying which type of world-entities the algorithm considers
	 * to be interactable.
	 */
	public Predicate<WorldEntity> isInteractable;

	/**
	 * Construct a goal structure that will guide the agent to the location of the
	 * given world-entity.
	 */
	public Function<String, GoalStructure> reachedG;

	/**
	 * Construct a goal structure that will cause the given world-entity to be
	 * interacted by the agent.
	 */
	public Function<String, GoalStructure> interactedG;

	/**
	 * Construct a goal structure that will cause the agent to explore the game
	 * world. It is left open, how far the exploration should go. But in any case,
	 * you can control the exploration budget through the variable
	 * {@link #explorationBudget}.
	 * 
	 * <p>
	 * The Vec3 location, if given, can be used to give a direction to explore to.
	 */
	public Function<Vec3, GoalStructure> exploredG;

	/**
	 * If non-null, this constructs a goal structure that will do some
	 * initialisation work before we start the search.
	 */
	public Function<Void, GoalStructure> initializedG;

	/**
	 * If specified, will close the agent's environment. This may also
	 * close/un-deloy the SUT. If defined, the function is called at the end of each
	 * single-episode run.
	 */
	public Function<Void, Void> closeEnv;

	/**
	 * Wrapper over {@link #closeEnv}. If non-null, it will be called to close the
	 * agent's environment.
	 */
	void closeEnv_() {
		if (closeEnv != null) {
			var t0 = System.currentTimeMillis();
			closeEnv.apply(null);
			var duration = System.currentTimeMillis() - t0;
			// we don't count the time for closing the SUT. Give back the time:
			remainingSearchBudget += (int) duration;
		}
	}

	/**
	 * If defined, this calculate the reward of being in a given state. Keep in mind
	 * that the maximum reward value should be consistent with {@link #maxReward}.
	 */
	public Function<Iv4xrAgentState, Float> rewardFunction;

	/**
	 * Contain the locations visited by the agent during the search.
	 */
	public List<Vec3> visitedLocations = new LinkedList<>();

	public BasicSearch() {
		remainingSearchBudget = totalSearchBudget;
	}

	public void setRndSeed(int seed) {
		rnd = new Random(seed);
	}

	/**
	 * Return the agent's state.
	 */
	@SuppressWarnings("rawtypes")
	public Iv4xrAgentState agentState() {
		// System.out.println(">>> agent wom:"
		// + ((Iv4xrAgentState) agent.state()).worldmodel()) ;
		return (Iv4xrAgentState) agent.state();
	}

	public WorldModel wom() {
		return agentState().worldmodel();
	}

	void log(String msg) {
		if (DEBUG) {
			System.out.println(msg);
		}
	}

	/**
	 * If {@link #agentIsDead} is defined, this checks if the agent is dead in the
	 * current agent/SUT state.
	 */
	boolean agentIsDead() {
		if (agentIsDead != null && agentIsDead.test(agentState())) {
			return true;
		}
		return false;
	}
	
	/**
	 * Assign the given goal-structure to the test-agent and runs the agent to solve
	 * this goal. The agent stops when the given goal is reached, or when the
	 * general {{@link #terminationConditionIsReached()} becomes true.
	 * 
	 * <p>
	 * The budget-parameter, if specified, specifies the maximum number of turns
	 * available to solve the goal. When this maximum is reached, the agnet will
	 * stop pursuing the goal. There is an overall computation-budget (in
	 * millisecond). If this is exhausted, the agent will stop as well. If the
	 * goal-level budget is 0 or negative, then it is ignored. Only the total budget
	 * matters then.
	 * 
	 * <p>
	 * The method returns the status of the given goal at the end of the method
	 * (success/fail or in-progress).
	 * 
	 */
	ProgressStatus solveGoal(String goalDesc, GoalStructure G, int budget) throws Exception {
		log("*** Deploying a goal: " + goalDesc);
		agent.setGoal(G);
		agent.update();
		Thread.sleep(delayBetweenAgentUpateCycles);
		int i = 1;
		// WorldEntity lastInteractedButton = null ;
		while (G.getStatus().inProgress() 
				&& 
				! topGoalPredicate.test(agentState())
				&& 
				(agentIsDead == null || ! agentIsDead.test(agentState()))) {
			if (budget > 0 && i >= budget) {
				log("*** Goal-level budget (" + budget + " turns) is EXHAUSTED.");
				break;
			}
			var pos = wom().position;
			log("*** " + turn + ", " + agent.getId() + " @" + pos);
			// track locations visited by the agent:
			if (traceLocation)
				visitedLocations.add(pos);
			Thread.sleep(delayBetweenAgentUpateCycles);
			i++;
			turn++;
			agent.update();
		}
		// agent.printStatus();
		log("*** Goal " + goalDesc + " terminated. Consumed turns: " + i + ". Status: " + G.getStatus());

		return G.getStatus();
	}

	void initializeEpisode() throws Exception {
		// create and initialize the agent:
		var t0 = System.currentTimeMillis();
		agent = agentConstructor.apply(null);
		var duration = System.currentTimeMillis() - t0;
		// we don't count the time for creating the agent and re-launching the
		// SUT. Give back the time:
		remainingSearchBudget += (int) duration;

		var state = agent.state();
		if (state == null)
			throw new IllegalArgumentException("Expecting an agent that already has a state.");
		if (!(state instanceof Iv4xrAgentState))
			throw new IllegalArgumentException("The agent's state should be an instance of Iv4xrAgentState.");

		if (initializedG != null) {
			var G = initializedG.apply(null);
			solveGoal("SUT initialized", G, budget_per_task);
		}
		// single initial agent-update to initialize wom:
		agent.update();
	}
	
	/**
	 * Implements one-episode search to move the SUT to a state satisfying {@link #topGoalPredicate}.
	 */
	void runAlgorithmForOneEpisode() throws Exception {

		initializeEpisode();

		int depth = 0;
		while(true) {
			solveGoal("Exploration", exploredG.apply(null), explorationBudget);
			var entities = wom().elements.values().stream().filter(e -> isInteractable.test(e))
					.collect(Collectors.toList());
			if (entities.isEmpty()) {
				// no entities to interact ... we terminate
				log("*** There is no entity left that the agent can intertact. Terminating current search episode.");
				break;
			}
			if (agentIsDead()) {
				log("*** The agent is DEAD.");
				break;
			}
			if (topGoalPredicate.test(agentState())) {
				goalHasBeenAchieved = true ;
				log("*** Goal is ACHIEVED");
				break ;
			}
			
			WorldEntity e = entities.get(rnd.nextInt(entities.size()));
			var G = SEQ(reachedG.apply(e.id), interactedG.apply(e.id));
			solveGoal("Reached and interacted " + e.id, G, budget_per_task);

			depth++;
			
			if (topGoalPredicate.test(agentState())) {
				goalHasBeenAchieved = true ;
				log("*** Goal is ACHIEVED");
				break ;
			}

			if (agentIsDead()) {
				log("*** The agent is DEAD.");
				break;
			}

			if (depth > this.maxDepth) {
				log("*** Max-depth is reached. Terminating the current search episode.");
				break;
			}

		}
		
		
		closeEnv_();
	}
	
	/**
	 * The termination condition for {@link #runAlgorithm()}. 
	 */
	boolean terminationCondition() {
		return (this.stopAfterGoalIsAchieved && goalHasBeenAchieved) 
				|| remainingSearchBudget <= 0
				|| (maxNumberOfEpisodes != null && totNumberOfEpisodes > maxNumberOfEpisodes) ;
	}

	/**
	 * Perform the multi-episodic search to get to an SUT state where {@link #topGoalPredicate} 
	 * is true. If {@link #stopAfterGoalIsAchieved} is set to true, the search stops as soon as
	 * the predicate becomes true. Else, the search continues until the budget is exhausted, or
	 * the agent is dead, or when the maximum number of episodes is reached.
	 */
	public void runAlgorithm() throws Exception {
		var tStart = System.currentTimeMillis() ;
		totNumberOfEpisodes = 0;
		log("*** START " + algName);
		do {
			totNumberOfEpisodes++;
			long t0 = System.currentTimeMillis();
			log("*** === starting episode " + totNumberOfEpisodes);
			runAlgorithmForOneEpisode();
			remainingSearchBudget = remainingSearchBudget - (int) (System.currentTimeMillis() - t0);
		} 
		while (! terminationCondition());
		log("*** END " + algName 
				+ ", goal:" + (goalHasBeenAchieved ? "ACHIEVED" : "X")
				+ ", #episodes:" + totNumberOfEpisodes 
				+ ", used-budget: " + (totalSearchBudget - remainingSearchBudget));
		
		
		log(">>> remaining budget:" + remainingSearchBudget) ;
		log(">>> exec-time:" + (System.currentTimeMillis() - tStart)) ;
		
	}

}
