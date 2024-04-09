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
import static nl.uu.cs.aplib.AplibEDSL.* ;

public class BasicSearch {
	
	/**
	 * To keep track the number of agent.updates() done so far.
	 */
	public int turn = 0 ;
	
	public String algName = "RND" ;
	
	/**
	 * A function that constructs a test agent.
	 */
	public Function<Void,TestAgent> agentConstructor ;
	
	/**
	 * The test-agent. Don't set this manually. This class will use {@link #agentConstructor}
	 * for creating an agent.
	 */
	TestAgent agent ;
	
	/**
	 * Available total search-budget in ms. The default is 3-min.
	 */
	public int totalSearchBudget = 180000 ;
	
	/**
	 * Remaining total-search budget in ms.
	 */
	int remainingSearchBudget ;
	
	/**
	 * If the search has a concept of "depth", this variable specifies a maximum
	 * depth.
	 */
	public int maxDepth = 10 ;
	
	
	/**
	 * If true, then the agent's location will be recorded in {@link #visitedLocations}.
	 * The default is false.
	 */
	public boolean traceLocation = false ;
	
	
	
	/**
	 * If true, the algorithm will print debug-messages to the console.
	 */
	static boolean DEBUG = false ;
	
	
	
	/**
	 * The delay (in ms) added between the agent's update cycles when executing a goal,
	 * in {@link #solveGoal(String, GoalStructure, int)}. The default is 50ms.
	 */
	public int delayBetweenAgentUpateCycles = 50 ;
	
	
	public int getTotalSearchBudget() { 
		return totalSearchBudget ;
	}
	
	public int getRemainingSearchBudget() {
		return remainingSearchBudget ;
	}
	
	public void setTotalSearchBudget(int budget) {
		totalSearchBudget = budget ;
		remainingSearchBudget = totalSearchBudget ;
	}
		
	public Random rnd = new Random() ;

	/**
	 * The max. number of turns that each goal-based task will be allowed. If this is
	 * exceeded the task will be dropped.
	 */
	public int budget_per_task = 150 ;
	
	/**
	 * The max. number of turns spent on exploration. 
	 */
	public int explorationBudget = 150 ;
	
	/**
	 * For collecting statistics. It represents the total number of "runs" the algorithm
	 * does. With "runs" being loosely defined. 
	 */
	public int totNumberOfRuns = 0 ;
	
	/**
	 * The maximum number of runs for this algorithm. With "runs" being loosely defined.
	 * E.g. it could mean episodes, where at each episode/run the algorithm performs a
	 * Search, and different episodes may repeat the search but with different random
	 * choices.
	 */
	public int maxNumberOfRuns = 10 ;
	
	/**
	 * A predicate specifying when the search is considered completed.
	 * The predicate is evaluated on the agent's state. 
	 */
	@SuppressWarnings("rawtypes")
	public Predicate<Iv4xrAgentState> topGoalPredicate ;
	
	/**
	 * If specified, this identifies when the agent is dead (so, it can't do
	 * anything anymore).
	 */
	@SuppressWarnings("rawtypes")
	public Predicate<Iv4xrAgentState> agentIsDead ;
	
	/**
	 * A predicate specifying which type of world-entities the algorithm considers
	 * to be interactable.
	 */
	public Predicate<WorldEntity> isInteractable ;
	
	/**
	 * Construct a goal structure that will guide the agent to the location of
	 * the given world-entity.
	 */
	public Function<WorldEntity,GoalStructure> reachedG ;
	
	/**
	 * Construct a goal structure that will cause the given world-entity to be
	 * interacted by the agent.
	 */
	public Function<WorldEntity,GoalStructure> interactedG ;
	
	/**
	 * Construct a goal structure that will cause the agent to explore the game
	 * world. It is left open, how far the exploration should go. But in any case,
	 * you can control the exploration budget through the variable 
	 * {@link #explorationBudget}.
	 * 
	 * <p>The Vec3 location, if given, can be used to give a direction to explore to.
	 */
	public Function<Vec3,GoalStructure> exploredG ;
	
	/**
	 * If non-null, this constructs a goal structure that will do some 
	 * initialisation work before we start the search.
	 */
	public Function<Void,GoalStructure> initializedG ;
	
	/**
	 * Contain the locations visited by the agent during the search.
	 */
	public List<Vec3> visitedLocations = new LinkedList<>() ;
	

	public BasicSearch() {
		remainingSearchBudget = totalSearchBudget;
	}
	
	public void setRndSeed(int seed) {
		rnd = new Random(seed) ;
	}
	
	/**
	 * Return the agent's state.
	 */
	@SuppressWarnings("rawtypes")
	public Iv4xrAgentState agentState() {
		//System.out.println(">>> agent wom:" 
		//		+ ((Iv4xrAgentState) agent.state()).worldmodel()) ;
		return (Iv4xrAgentState) agent.state() ;
	}
	
	public WorldModel wom() {
		return agentState().worldmodel() ;
	}
	
	void log(String msg) {
		if (DEBUG) {
			System.out.println(msg) ;
		}
	}
	
	/**
	 * Define the termination condition of this algorithm. The one implemented by
	 * this method is that TC is true if either the algorithm's total budget is
	 * exhausted, or the top-goal is reached, or if the agent is dead.
	 */
	boolean terminationConditionIsReached() {
		if (remainingSearchBudget <= 0) {
			log("*** TOTAL BUDGET IS EXHAUSTED.") ;
			return true ;
		}
		if (topGoalPredicate.test(agentState())) {
			log("*** The search FOUND its global-goal. YAY!") ;
			return true ;
		}
		if (agentIsDead != null && agentIsDead.test(agentState())) {
			log("*** The agent is DEAD.") ;
			return true ;
		}
		return false ;
	}
	
	/**
	 * Assign the given goal-structure to the test-agent and runs the agent to solve this goal.
	 * The agent stops when the given goal is reached, or when the general {{@link #terminationConditionIsReached()}
	 * becomes true.
	 * 
	 * <p>The budget-parameter, if specified, specifies the maximum number of turns available 
	 * to solve the goal. When this maximum is reached, the agnet will stop pursuing the goal.
	 * There is an overall computation-budget (in millisecond). If this is exhausted, the agent
	 * will stop as well. If the goal-level budget is 0 or negative, then it is ignored. Only
	 * the total budget matters then.
	 * 
	 * <p>The method returns the status of the given goal at the end of the method (success/fail
	 * or in-progress).
	 * 
	 */
	ProgressStatus solveGoal(String goalDesc, GoalStructure G, int budget) throws Exception {
		log("*** Deploying a goal: " + goalDesc) ;
		agent.setGoal(G) ;
		agent.update() ;
		Thread.sleep(delayBetweenAgentUpateCycles);
		int i=1 ;
		//WorldEntity lastInteractedButton = null ;
		while (G.getStatus().inProgress() && !terminationConditionIsReached()) {
			if (budget>0 && i >= budget) {
				log("*** Goal-level budget (" + budget + " turns) is EXHAUSTED.") ;
				break ;
			}
			var pos = wom().position ;
			log("*** " + turn + ", " + agent.getId() + " @" + pos);
			// track locations visited by the agent:
			if (traceLocation)
				visitedLocations.add(pos) ;
			Thread.sleep(delayBetweenAgentUpateCycles);
			i++; turn++ ;
			agent.update();
		}
		// agent.printStatus();	
		log("*** Goal " + goalDesc + " terminated. Consumed turns: " + i + ". Status: " + G.getStatus()) ;
		
		return G.getStatus() ;
	}
	
	void runAlgorithmForOneEpisode() throws Exception {
		
		// create and initialize the agent:
		agent = agentConstructor.apply(null) ;
		
		var state = agent.state() ;
    	if (state == null) 
    		throw new IllegalArgumentException("Expecting an agent that already has a state.") ;
    	if (! (state instanceof Iv4xrAgentState))
    		throw new IllegalArgumentException("The agent's state should be an instance of Iv4xrAgentState.") ;
    		
		
		if (initializedG != null) {
			var G = initializedG.apply(null) ;
			solveGoal("SUT initialized", G, budget_per_task) ;		
		}
		// single initial agent-update to initialize wom:
		agent.update();
		int depth = 0 ;
		while (! terminationConditionIsReached()) {
			solveGoal("Exploration", exploredG.apply(null), explorationBudget) ;
			var entities = wom().elements.values().stream()
					.filter(e -> isInteractable.test(e))
					.collect(Collectors.toList()) ;
			if (entities.isEmpty()) {
				// no entities to interact ... we terminate
				log("*** There is no entity left that the agent can intertact. Terminating current search episode.") ;
				break ;
			}
			WorldEntity e = entities.get(rnd.nextInt(entities.size())) ;
			var G = SEQ(reachedG.apply(e), interactedG.apply(e)) ;
			solveGoal("Interacted " + e.id, G, budget_per_task) ;
			
			depth++ ;
			
			if (depth > this.maxDepth) {
				log("*** Max-depth is reached. Terminating the current search episode.") ;
				break ;
			}
			
		}
	}
	
	public void runAlgorithm() throws Exception {

		remainingSearchBudget = totalSearchBudget ;

		long t0 = System.currentTimeMillis() ;
		log("*** " + algName + " starts") ;
		runAlgorithmForOneEpisode() ;
		remainingSearchBudget = remainingSearchBudget - (int) (System.currentTimeMillis() - t0) ;
		totNumberOfRuns++ ;
		while (! terminationConditionIsReached() && totNumberOfRuns < maxNumberOfRuns) {
			t0 = System.currentTimeMillis() ;
			log("*** === starting episode " + totNumberOfRuns) ;
			runAlgorithmForOneEpisode() ;
			remainingSearchBudget = remainingSearchBudget - (int) (System.currentTimeMillis() - t0) ;
			totNumberOfRuns++ ;
		}
		log("*** " + algName + " ends. #runs:" + totNumberOfRuns) ;
	}
	

}
