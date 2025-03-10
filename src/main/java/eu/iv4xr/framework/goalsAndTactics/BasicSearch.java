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
 * an entity to go to, and then interacting with that entity. This random algorithm
 * assumes e.g. "exploring" and "traveling to an entity" to be functionalities
 * provided through goal structures, to be supplied in e.g. {@link #exploredG}
 * and {@link #reachedG}.
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
	public TestAgent agent;

	/**
	 * Available total search-budget in ms. The default is 3-min.
	 */
	public int totalSearchBudget = 180000;

	/**
	 * Remaining total-search budget in ms.
	 */
	protected int remainingSearchBudget;

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
	static public boolean DEBUG = false;

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

	Random rnd = new Random();

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
	 * If set to true, the algorithm will stop if an error in the game-under test is found.
	 * "Error" is checked via LTL-violation (so, you need LTL specifications to detect it).
	 * <br>Default: false.
	 */
	public boolean stopWhenErrorIsFound = false ;
	
	/**
	 * This will be set to true when {@link #topGoalPredicate} becomes
	 * true; and will remain true to indicate that the goal has ever been achieved.
	 */
	boolean goalHasBeenAchieved = false ;
	
	/**
	 * Sequence of interactions that was found to lead to a state satisfying {@link #topGoalPredicate}.
	 */
	public List<String> winningplay = null ;
	
	/**
	 * If the algorithm detect an error in the SUT, this will be set to false. Currently this is
	 * checked via LTL (so, you need to specify some LTL properties that specify correct agent
	 * execution).
	 */
	public boolean foundError = false ;
	
	public boolean goalHasBeenAchieved() {
		return goalHasBeenAchieved ;
	}
	
	protected void markThatGoalIsAchieved(List<String> trace) {
		goalHasBeenAchieved = true ;
		if (winningplay == null || trace.size() < winningplay.size()) {
			winningplay = trace ;
		}		
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
	 * The value of a state where the agent is dead. Default is -100.
	 */
	public float agentDeadValue = -100 ;

	/**
	 * If specified, this calculates the value of a given state. The maximum possible value should be 
	 * less or equal to {@link #maxReward}, and greater or equal to {@link #agentDeadValue}.
	 */
	@SuppressWarnings("rawtypes")
	public Function<Iv4xrAgentState, Float> stateValueFunction ;
	
	/**
	 * If specified, this wipes the agent memory on visited places. Only the navigation nodes need to be wiped;
	 * seen entities can be kept.
	 */
	public Function<TestAgent,Void> wipeoutMemory ;

	/**
	 * Contain the locations visited by the agent during the search.
	 */
	public List<Vec3> visitedLocations = new LinkedList<>();

	public BasicSearch() {
		//remainingSearchBudget = totalSearchBudget;
	}

	public void setRndSeed(int seed) {
		rnd = new Random(seed);
	}
	
	public TestAgent getAgent() {
		return agent ;
	}

	/**
	 * Return the agent's state. NOTE that this does NOT apply cloning. Instead, the
	 * method simply returns a reference/pointer to the agent's state. So, if the agent
	 * does an update and chenges its state, the state returned by this method changes
	 * as well. 	 
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
	
	/**
	 * The "clamped" value of the current game state. This mainly calculated by {@link #stateValueFunction}. 
	 * However, there are a number of additional logic imposed. First the return value will be
	 * clamped between {@link #agentDeadValue} and {@link #maxReward} (inclusive). Second, the value is equal 
	 * to {@link #maxReward} if {@link #topGoalPredicate} holds on that state. Third, if {@link #agentIsDead} is defined, 
	 * and it says the agent is dead, the returned value is as specified in {@link #agentDeadValue}.
	 */
	float clampedValueOfCurrentGameState() {
		var state = agentState() ;
		if (topGoalPredicate.test(state)) {
			return maxReward ;
		}
		if (agentIsDead())
			return agentDeadValue ;
		if (stateValueFunction != null) {
			float rw = Math.min(maxReward, stateValueFunction.apply(state))   ;
			return Math.max(agentDeadValue,rw) ;
		}
		// if stateValFunction is undefined, the agent is not dead etc, just return 0:
		return 0 ;
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
		
		// we should probably apply this first to clean the goal-stack:
		agent.dropAll();
		
		agent.setGoal(G);
		agent.update();
		Thread.sleep(delayBetweenAgentUpateCycles);
		int i = 1;
		// take budget-1 to account for the extra update that is always inserted at the end
		int budget_ = budget - 1 ;
		// WorldEntity lastInteractedButton = null ;
		while (G.getStatus().inProgress() 
				&& 
				! topGoalPredicate.test(agentState())
				&& 
				(agentIsDead == null || ! agentIsDead.test(agentState()))) {
			if (budget > 0 && i >= budget_) {
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
		// adding a single extra update to force the agent state to be updated to the SUT
		// state right after G completion.
		agent.setGoal(SUCCESS()) ;
		agent.update() ;
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
	 * It returns a value/reward obtained by the episode.
	 */
	float runAlgorithmForOneEpisode() throws Exception {

		initializeEpisode();
		List<String> trace = new LinkedList<>() ;

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
				markThatGoalIsAchieved(trace) ;
				log("*** Goal is ACHIEVED");
				break ;
			}
			
			WorldEntity e = entities.get(rnd.nextInt(entities.size()));
			var G = SEQ(reachedG.apply(e.id), interactedG.apply(e.id));
			trace.add(e.id) ;
			solveGoal("Reached and interacted " + e.id, G, budget_per_task);

			depth++;
			
			if (topGoalPredicate.test(agentState())) {
				markThatGoalIsAchieved(trace) ;
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
		foundError = foundError || ! agent.evaluateLTLs() ;
		
		closeEnv_();
		return clampedValueOfCurrentGameState() ;
	}
	
	/**
	 * The termination condition for {@link #runAlgorithm()}. 
	 */
	boolean terminationCondition() {
		return (this.stopAfterGoalIsAchieved && goalHasBeenAchieved) 
				|| remainingSearchBudget <= 0
				|| (maxNumberOfEpisodes != null && totNumberOfEpisodes >= maxNumberOfEpisodes
				|| (stopWhenErrorIsFound && foundError)) ;
	}
	
	
	/**
	 * Execute the given trace of interactions. The method will report whether the trace
	 * manages to solve {@link #topGoalPredicate}. It also reports if the re-execution found 
	 * any violation. Currently only LTL violation is checked. 
	 * 
	 * <p>Each string e in the trace is interpreted as specifying an interaction on
	 * a game-object identified by e. "Executing" e is done by instructing the test agent
	 * to first get close to it, and then to interact with it. This is done by executing
	 * {@link #reachedG} and {@link #interactedG}.
	 * 
	 * <p>No full exploration will be forced after every step in the trace (the 
	 * goal-structure reachedG is assumed to invoke exploration if the target e
	 * is not known in the agent's belief).
	 */
	public RerunWinningPlayResult runTrace(List<String> trace) throws Exception {
		
		initializeEpisode();
		
		log(">>> executing a trace: " + trace);
		
		var result = new RerunWinningPlayResult() ;
		result.indexOfLastExecutedStep = 0 ;
		result.traceLength = trace.size() ;
		for (var e : trace) {
			var G = SEQ(reachedG.apply(e), interactedG.apply(e));
			var status = solveGoal("Reached and interacted " + e, G, budget_per_task);
			
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
			// also break if interacting with e failed:
			if (status.failed()) {
				result.aborted = true ;
				break ;
			}
			result.indexOfLastExecutedStep++ ;
		}
		closeEnv_() ;

		result.violationDetected = ! agent.evaluateLTLs() ;
		
		return result ;
	}
	
	/**
	 * Execute the trace of interactions stored in {@link #winningplay}. If not empty, this 
	 * field should content the traces of interactions that leads to solving {@link #topGoalPredicate}. 
	 * The method re-executes the trace, and at the end will report whether this indeed solves
	 * {@link #topGoalPredicate}. Note that this is not guaranteed if the SUT is non-deterministic.
	 * 
	 * <p> The method {@link #runTrace(List)} is used to execute the play. Each string e in 
	 * {@link #winningplay} is interpreted as specifying an interaction on
	 * a game-object identified by e. "Executing" e is done by instructing the test agent
	 * to first get close to it, and then to interact with it. This is done by executing
	 * {@link #reachedG} and {@link #interactedG}.
	 * 
	 * <p>The method also reports if the re-execution found any violation. Currently only LTL violation
	 * is checked. 
	 */
	public RerunWinningPlayResult runWinningPlay() throws Exception {
		return runTrace(winningplay) ;
	}
	
	
	
	
	public static class AlgorithmResult {
		public String algName ;
		public boolean goalAchieved ;
		public int usedBudget ;
		public int usedTurns  ;
		public int totEpisodes ;
		public List<String> winningplay ;
		public List<Float> episodesValues ;
		public boolean foundError ;
		
		public String showShort() {
			String z = "" + algName 
			    + ", goal:" + (goalAchieved ? "ACHIEVED" : "X")
				+ ", #episodes:" + totEpisodes 
				+ ", used-budget: " + usedBudget 
				+ ", used-turns:" + usedTurns ;
			
			if (foundError) 
				z += ". Found an ERROR" ;
				
			if (goalAchieved)
				z += ". #winningplay:" + winningplay.size() ;
			else {
				var maxVal = episodesValues.stream().max((v1,v2) -> Float.compare(v1,v2)) ;				
				z += ". Episode-val:" + maxVal.get() ;
			}
			return z ;		
		}
	}
	
	public static class RerunWinningPlayResult {
		public boolean topPredicateSolved = false ;
		public boolean agentIsDead = false ;
		public boolean aborted = false ;
		public int indexOfLastExecutedStep = -1 ;
		public int traceLength = -1 ;
		public boolean violationDetected = false ;
		
		@Override
		public String toString() {
			String z = "" ;
			z += "Top-goal " + (topPredicateSolved ? "is SOLVED" : "is NOT solved.") ;
			if (agentIsDead)
			   z += "\nExecution aborted because the agent is DEAD." ;
			if (aborted)
				z += "\nExecution ABORTED." ;
			z += "\nLast executed step-index: " + indexOfLastExecutedStep 
					+ ", trace-length=" + traceLength ;
			z += "\n" + (violationDetected ? "VIOLATED detected" : "NO violation was detected") ;
			return z ;
		}
		
	}

	/**
	 * Perform the multi-episodic search to get to an SUT state where {@link #topGoalPredicate} 
	 * is true. If {@link #stopAfterGoalIsAchieved} is set to true, the search stops as soon as
	 * the predicate becomes true. Else, the search continues until the budget is exhausted, or
	 * the agent is dead, or when the maximum number of episodes is reached.
	 */
	public AlgorithmResult runAlgorithm() throws Exception {
		var tStart = System.currentTimeMillis() ;
		remainingSearchBudget = totalSearchBudget ;
		totNumberOfEpisodes = 0;
		List<Float> episodesValues = new LinkedList<>() ;
		log("*** START " + algName);
		do {
			totNumberOfEpisodes++;
			long t0 = System.currentTimeMillis();
			log("*** === starting episode " + totNumberOfEpisodes);
			System.out.println(">>> " + algName + " starting episode " + totNumberOfEpisodes);		
			var value = runAlgorithmForOneEpisode();
			remainingSearchBudget = remainingSearchBudget - (int) (System.currentTimeMillis() - t0);
			episodesValues.add(value) ;
		} 
		while (! terminationCondition());
		
		var R = new AlgorithmResult() ;
		R.algName = this.algName ;
		R.goalAchieved = goalHasBeenAchieved() ;
		R.totEpisodes = totNumberOfEpisodes ;
		R.usedBudget = totalSearchBudget - remainingSearchBudget ;
		R.usedTurns = turn ;
		R.winningplay = this.winningplay ;
		R.episodesValues = episodesValues ;
		R.foundError = this.foundError ;
		log("*** END " + R.showShort());
			
		log(">>> remaining budget:" + remainingSearchBudget) ;
		log(">>> exec-time:" + (System.currentTimeMillis() - tStart)) ;
		
		return R ;
		
	}

}
