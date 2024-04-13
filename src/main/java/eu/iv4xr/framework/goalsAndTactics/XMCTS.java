package eu.iv4xr.framework.goalsAndTactics;


import static nl.uu.cs.aplib.AplibEDSL.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;

/**
 * Implementation of Monte Carlo Search Tree (MCTS). In this implementation we
 * assume a deterministic target game. That is, playing the same sequence of
 * actions always give the same reward/value.
 * 
 * <p>The algorithm constructs a tree, encoding a winning strategy to play a game.
 * The game is assumed to be adversarial (e.g. like chess). 
 * 
 * However, rather than playing moves in alteration (player move, then opponent move),
 * a step in the tree represents the player's move, and the corresponding game's
 * respond to that (e.g. a monster hits the player). Since the game is assumed to
 * be deterministic, the game's respond is deterministic as well.
 * 
 * <p>A winning state is a state satisfying the goal set in BaseSearch's topGoalPredicate.
 * The first time a rollout finds a winning state, the sequences of steps that reach it
 * is remembered in {@link #winningplay}. The search can stop, or continued depending on
 * the value of the field stopAfterGoalsAchieved.
 * MCTS is actually a learning algorithm. The resulting Monte Carlo tree represents a
 * strategy to reach a winning state. If the algorithm is set to stop as soon as a winning
 * state is found, then we use it as a search algorithm. If we let it continue then it behaves
 * as a learning algorithm.
 * 
 * <p>Use the field maxDepth to control the depth of the search.  Also, giving value/reward to a rollout
 * where the agent survives, but not necessarily reaches a winning state helps in directing
 * the search towards closing to a winning state.
 */
public class XMCTS extends BasicSearch{
	
	static class Node {
		float totalReward ;
		float averageReward ;
		int numberOfPlays = 0 ;
		int depth ;
		
		/**
		 * The action that leads to this node. Null if this is the root node.
		 */
		String action = null ;
		
		/**
		 * A node is fully explored if it is a terminal node, or or all its children are
		 * fully explored.
		 */
		boolean fullyExplored = false ;
		
		/**
		 * A node is a terminal node if no further action is possible, or if max-depth is reached.
		 */
		boolean terminal = false ;
		
		Node parent ;
		
		List<Node> children ;
		
		/** 
		 * Get the UCB-value of this node.
		 */
		float ucbValue() {
			if (numberOfPlays == 0) return Float.POSITIVE_INFINITY ;
			
			return averageReward 
					+ 2f * (float) Math.sqrt(Math.log((float) parent.numberOfPlays)
					                         / (float) numberOfPlays) ;	
		}
		
		/**
		 * Recursively back-propagate a reward. Also takes care of marking if
		 * a node is fully explored.
		 */
		void backPropagate(float newReward) {
			numberOfPlays++ ;
			totalReward += newReward ;
			averageReward = totalReward / (float) numberOfPlays ;
			//System.out.println(">>> update avrg rew: " + averageReward) ;
			if (parent != null)
				parent.backPropagate(newReward) ;
		}
		
		/**
		 * Mark this node as fully explored, and propagate the information towards the 
		 * root.
		 */
		void propagateFullyExploredStatus() {
			if (children != null && children.stream().allMatch(ch -> ch.fullyExplored)) {
				fullyExplored = true ;
				//System.out.println("====>> setting this node as fully explored: " + this.action) ;
				//System.out.println(">>>> hit RET") ;
				//Scanner scanner = new Scanner(System.in);
				//scanner.nextLine() ;
				if (parent != null)
					parent.propagateFullyExploredStatus() ;
			}
		}
		
		List<Node> getPathLeadingToThisNode() {
			List<Node> path = null ;
			if (parent != null)
				path = parent.getPathLeadingToThisNode() ;
			else
				path = new LinkedList<>() ;
			path.add(this) ;
			return path ;
		}
		
		List<String> getTraceLeadingToThisNode() {
			List<String> tr =  getPathLeadingToThisNode()
					  .stream()
					  .map(nd -> nd.action)
					  .collect(Collectors.toList()) ;
			// first element is null, remove it:
			tr.remove(0) ;
			return tr ;
		}	
		
		@Override
		public String toString() {
			return toStringWorker("") ;
		}
		
		public String toStringWorker(String indent) {
			String z = "" + indent + this.action 
					+ ", avrgReward=" +  this.averageReward 
					+ ", fully explored:" + this.fullyExplored ;
			if (children == null) {
				z += " X" ;
				return z ;
			}
			for (var ch : children) {
				var u = ch.toStringWorker("  " + indent) ;
				z += "\n" ;
				z += u ;
			}
			return z ;
		}
	}
	
	static class PlayResult {
		/**
		 * The list of actions that were played.
		 */
		List<String> trace ;
		
		/**
		 * The reward obtained at the end of the play.
		 */
		float reward ;	
	}
	
	/**
	 * The Monte Carlo Tree.
	 */
	public Node mctree ;
	
	
	/**
	 * Wipe the agent memory on visited places. Only the navigation nodes need to be wiped;
	 * seen entities can be kept.
	 */
	public Function<TestAgent,Void> wipeoutMemory ;
	
	
	public XMCTS() { 
		super() ;
		algName = "MCTS" ;
		mctree = new Node() ;
		mctree.depth = 0 ;
	}
	
	/**
	 * Execute all the actions in the path towards and until the given node. The method
	 * returns true if the whole sequence can be executed, and else false.
	 */
	boolean runPath(Node node, boolean closeEnvAtTheEnd) throws Exception {
		
		var trace = node.getTraceLeadingToThisNode() ;

		log(">>> executing prefix " + trace);
		
		boolean success = true ;
		
		if (trace.isEmpty()) {
			// special case when the trace is still empty:
			solveGoal("Exploration", exploredG.apply(null), explorationBudget) ;
			if (agentIsDead()) {
				 success = false ;
			}
			return success ;
		}
		
		for (var entityToInteract : trace) {
			 var G = SEQ(reachedG.apply(entityToInteract), interactedG.apply(entityToInteract)) ;
			 var status = solveGoal("Reached and interacted " + entityToInteract, G, budget_per_task) ;
			 // if the agent is dead, break:
			 if (agentIsDead()) {
				 success = false ;
				 break ;
			 }
			 // also break the execution if G fails:
			 if (!status.success()) {
				 success = false ;
				 break ;
			 }
			 // reset exploration, then do full explore:
			 wipeoutMemory.apply(agent) ;
			 solveGoal("Exploration", exploredG.apply(null), explorationBudget) ; 
		}
		return success ;
	}
	
	/**
	 * Play all the actions leading to the given node, then continue to play
	 * the game from that point either until a terminal state is reached, or
	 * a maximum depth is reached.
	 * <p> Return a play-result, which contains the full sequence of actions
	 * of the play, and the reward obtained by the play.
	 * @throws Exception 
	 */
	PlayResult rollout(Node node) throws Exception {
		
		initializeEpisode();
		
		List<String> trace = node.getTraceLeadingToThisNode() ;
		
		var success = runPath(node,false) ;

		if (!success) {
			// if the trace replay is not successful, we don't continue:
			PlayResult R = new PlayResult() ;
			R.trace = trace ;
			R.reward = valueOfCurrentGameState() ;
			closeEnv_() ;
			return R ;
		}
		
		int depth = trace.size() ;

		while (depth < maxDepth) {
			
			var interactables = wom().elements.values().stream()
									.filter(e -> isInteractable.test(e))
									.collect(Collectors.toList()) ;
			
			if (interactables.isEmpty()) break ;
			var chosen = interactables.get(rnd.nextInt(interactables.size())) ;
			trace.add(chosen.id) ;
			// ask the agent to interact with the chosen entity:
			var G = SEQ(reachedG.apply(chosen.id), interactedG.apply(chosen.id)) ;
			var status = solveGoal("Reached and interacted " + chosen.id, G, budget_per_task) ;
			depth++ ;
			// if the agent is dead, break:
			if (agentIsDead()) {
				 break ;
			}
			// also break the execution if G fails:
			if (! status.success()) {
				 break ;
			}
			// reset exploration, then do full explore:
			wipeoutMemory.apply(agent) ;
			solveGoal("Exploration", exploredG.apply(null), explorationBudget);

			if (topGoalPredicate.test(agentState())) {
				markThatGoalIsAchieved(trace);
				break ;
			}
			if (agentIsDead()) {
				 break ;
			}
		}
		
		PlayResult R = new PlayResult() ;
		R.trace = trace ;
		R.reward = valueOfCurrentGameState() ;
		closeEnv_();
		return R ;	
	}
	
	List<Node> generateChildren(Node node) throws Exception {
		List<Node> children = new LinkedList<>() ;
		initializeEpisode();
		var success = runPath(node,true) ;
		closeEnv_();
		if (success) {
			var interactables = wom().elements.values().stream()
					.filter(e -> isInteractable.test(e))
					.collect(Collectors.toList()) ;
			for (var e : interactables) {
				Node child = new Node() ;
				child.action = e.id ;
				children.add(child) ;
			}
		}
		return children ;
	}
	
	Node chooseLeaf(Node nd) {
		if (nd.children == null) return nd ;
		if (nd.children.isEmpty())
			throw new IllegalArgumentException() ;
		
		float bestUCB = Float.NEGATIVE_INFINITY ;
		var explorableChildren = nd.children.stream().filter(ch -> ! ch.fullyExplored).collect(Collectors.toList()) ;
		for (var ch : explorableChildren) {
			float U = ch.ucbValue() ;
			if (U > bestUCB) {
				bestUCB = U ;
			}
		}
		final float bestUCB_ = bestUCB ;
		// we might have multiple maxes. If so, we choose randomly among the maxes:
		var maxes = explorableChildren.stream().filter(ch -> ch.ucbValue() >= bestUCB_)
				.collect(Collectors.toList()) ;
		Node bestChild = maxes.get(rnd.nextInt(maxes.size())) ;
		return  chooseLeaf(bestChild) ;
	}
	
	
	float evaluateLeaf(Node leaf) throws Exception {
		
		log(">>> EVAL-leaf " + leaf.action) ;
		
		if (leaf.terminal || leaf.fullyExplored) 
			throw new IllegalArgumentException() ;
		
		// the leaf is at the max-depth:
		if (leaf.depth >= maxDepth) {
			leaf.terminal = true ;
			leaf.fullyExplored = true ;
			initializeEpisode();
			runPath(leaf,true) ;
			closeEnv_() ;
			var R = valueOfCurrentGameState() ;
			leaf.backPropagate(R);
			if (leaf.parent != null) 
				leaf.parent.propagateFullyExploredStatus();
			// the case when the state after this node is a winning state:
			if (R >= maxReward) {
				markThatGoalIsAchieved(leaf.getTraceLeadingToThisNode());
			}
			return R ;			
		}
		
		// leaf is not at max-depth and has not been sampled/played before:
		if (leaf.numberOfPlays == 0) {
			System.out.println(">>> ROLLOUT") ;
			var R = rollout(leaf) ;
			leaf.backPropagate(R.reward) ;
			if (R.reward >= maxReward) {
				// should not be needed, but just to make sure:
				markThatGoalIsAchieved(leaf.getTraceLeadingToThisNode());
			}
			return R.reward ;
		}
		
		// last case is that the leaf has been sampled. In this case we expand:
		leaf.children = generateChildren(leaf) ;
		if (leaf.children.isEmpty()) {
			// no further actions from the leaf is possible, mark it as terminal:
			leaf.terminal = true ;
			leaf.fullyExplored = true ;
			//System.out.println("====+++>> setting this node as fully explored: " + leaf.action) ;
			//System.out.println(">>>> hit RET") ;
			//Scanner scanner = new Scanner(System.in);
			//scanner.nextLine() ;
			if (leaf.parent != null) 
				leaf.parent.propagateFullyExploredStatus();
			return leaf.averageReward ;
		}
		
		log(">>> EXPAND") ;

		// else, go to a random child, and evaluate it:
		for (var ch : leaf.children) {
			ch.parent = leaf ;
			ch.depth = leaf.depth+1 ;
		}
		return evaluateLeaf(leaf.children.get(rnd.nextInt(leaf.children.size()))) ;
	}
	
	/**
	 * This run a single MCTS iteration. It will choose a leaf, and evaluate the leaf.
	 * This will execute all the steps/moves until the leaf, followed by either a
	 * rollout from the leaf (a play from the leaf until we reach a SUT terminal state,
	 * or until a certain maximum depth is reached) or an expand. 
	 * <p>We rollout if the leaf has not been sampled/visited before. Else we expand the leaf. 
	 *  Expanding means that we figure out successors of the leaf, and then we choose one
	 *  of these successors to rollout.
	 *  
	 *  <p>The method {@link #runAlgorithm()} invokes this method, to run a multi-episodic
	 *  search. Effectively, {@link #runAlgorithm()} implements the MCTS algorithm.
	 */
	@Override
	float runAlgorithmForOneEpisode() throws Exception {
		Node leaf = chooseLeaf(mctree) ;
		return evaluateLeaf(leaf) ;
	}
	
	/**
	 * The termination condition for {@link #runAlgorithm()}. 
	 */
	boolean terminationCondition() {
		return (this.stopAfterGoalIsAchieved && goalHasBeenAchieved) 
				|| remainingSearchBudget <= 0
				|| (maxNumberOfEpisodes != null && totNumberOfEpisodes > maxNumberOfEpisodes) 
				|| mctree.fullyExplored ;
	}

}
