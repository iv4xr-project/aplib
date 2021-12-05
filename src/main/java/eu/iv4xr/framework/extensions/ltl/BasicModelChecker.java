package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.utils.Pair;

/**
 * Provide an explicit-state bounded and lazy model checker that can be used
 * to check if a model contains a reachable state (within a given maximum depth)
 * satisfying
 * some predicate q. If one can be found, a witness in the form of an execution/path
 * in the model that reaches such a state can be obtained. Note that failing to find
 * such a witness means that ~q holds globally on all states within the maximum
 * depth from the model's initial state.
 * 
 * <p>The model checker can be used to target any 'program' or 'model of program'
 * that implements the interface {@link ITargetModel}.
 * 
 * <p>Additional functionalities are provided e.g. to find a minimum length witness
 * and to produce a test-suite that covers all states of the model.
 * 
 * @author Wish
 *
 */
public class BasicModelChecker {
	
	/**
	 * The 'program' or 'model of a program' that we want to target in model-checking.
	 */
	public ITargetModel model ;
	
	/**
	 * Hold some basic statistics over the last model-checking run.
	 */
    public MCStatistics stats = new MCStatistics() ;
	
	public static class MCStatistics {
		public int numberOfStatesExplored = 0 ;
		public int numberOfTransitionsExplored = 0 ;
		
		public void clear() {
			numberOfStatesExplored = 0 ;
			numberOfTransitionsExplored = 0 ;
		}
		
		@Override
		public String toString() {
			return "Number of states explored: " + numberOfStatesExplored
					+ "\nNumber of transitions explored: " + numberOfTransitionsExplored ;
		}

	}
	
	/**
	 * Create an instance of a model checker.
	 * 
	 * @param model The 'program' to be model-checked.
	 */
	public BasicModelChecker(ITargetModel model) {
		this.model = model ;
	}
	
	/**
	 * Representing a witness execution. It is represented as a sequence of pairs
	 * (tr,s) where s is a state passed by the execution, and tr is a representation
	 * of the transition that results in that state s. This tr might be null if
	 * s is the starting state.
	 */
	public static class Path<State> {
		
		public List<Pair<ITransition,State>> path = new LinkedList<>() ;
		
		void addInitialState(State state) {
		    path.add(new Pair<ITransition,State>(null,state)) ;	
		}
		
		void addTransition(ITransition tr, State state) {
			path.add(new Pair<ITransition,State>(tr,state)) ;
		}
		
		void removeLastTransition() {
			path.remove(path.size() - 1) ;
		}
		
		Path<State> copy() {
			Path<State> z = new Path<>() ;
			for(var step : this.path) {
				z.addTransition(step.fst, step.snd);
			}
			return z ;
		}
		
		public List<State> getStateSequence() {
			return path.stream().map(step -> step.snd).collect(Collectors.toList()) ;
		}
		
		public State getLastState() {
			var seq = getStateSequence() ;
			return seq.get(seq.size() - 1) ;
		}
		
		@Override
		public String toString() {
			int k = 0;
            String s = "";
            for (var tr : path) {
                if (k > 0)
                    s += "\n";
                s += k + ": action:" + tr.fst + " --> state:" + tr.snd ;
                k++;
            }
            return s;
		}
	}
	
	/**
	 * Check if the target program has an finite execution that ends in a state
	 * satisfying the predicate q. If so, it returns SAT, and else UNSAT. <b>Be
	 * careful</b> that this method may not terminate if the target program has an
	 * infinite state space. Use {@link #sat(Predicate, int)} instead.
	 */
	public SATVerdict sat(Predicate<IExplorableState> q) {
		var path = find(q,Integer.MAX_VALUE) ;
		if(path == null) return SATVerdict.UNSAT ;
		return SATVerdict.SAT ;
	}
	
	/**
	 * Check if the target program has an finite execution of the specified maximum
	 * length, that ends in a state satisfying the predicate q. If so, it returns
	 * SAT, and else UNSAT.
	 */
	public SATVerdict sat(Predicate<IExplorableState> q, int maxDepth) {
		var path = find(q,maxDepth) ;
		if(path == null) return SATVerdict.UNSAT ;
		return SATVerdict.SAT ;
	}
	
	/**
	 * Do model-checking to find a finite execution (of the given max-length) of the
	 * target program that ends in a state satisfying the predicate q. If so, it
	 * returns this execution, and else null.
	 */
	public Path<IExplorableState> find(Predicate<IExplorableState> q, int maxDepth) {
		model.reset();
		stats.clear(); 
		Path<IExplorableState> path = new Path<>() ;
		Collection<IExplorableState> visitedStates = new HashSet<>() ;
		IExplorableState state = model.getCurrentState().clone() ;
		path.addInitialState(state);	
		return dfs(q,path,visitedStates,state,maxDepth+1) ;	
	}
	
	/**
	 * Similar to {@link #find(Predicate, int)}, but it will return the shortest execution
	 * (of the specified max-length) that ends in q.
	 */
	public Path<IExplorableState> findShortest(Predicate<IExplorableState> q, int maxDepth) {
		if (maxDepth < 0) 
			throw new IllegalArgumentException() ;
		int lowbound = 0 ;
		int upbound = maxDepth+1 ;
		
		Path<IExplorableState> bestpath = null ;
		while (upbound > lowbound) {
			int mid = lowbound + (upbound - lowbound)/2 ;
			Path<IExplorableState> path = find(q,mid) ;
			if (path != null) {
				upbound = mid ;
				bestpath = path ;
			}
			else {
				if(mid==lowbound) {
				   upbound = mid ;
				}
				else {
					lowbound = mid ;
				}
			}
		}
		return bestpath ;	
	}
	
	/**
	 * Implement the 'lazy model checking' algorithm similar to what is used by the
	 * SPIN model checker. It is actually a Depth First Search (DFS) algorithm. We
	 * don't do double DSF because we only want to check state reachability.
	 * 
	 * @param whatToFind     predicate characterizing the state whose reachability
	 *                       is to be checked.
	 * @param pathSoFar      the path that leads to the parameter state given below.
	 * @param visitedStates  the set of all states visited so far by this DFS.
	 * @param state          the next state to explore.
	 * @param remainingDepth speaks for itself :)
	 * @return
	 */
	Path<IExplorableState> dfs(Predicate<IExplorableState> whatToFind, 
			 Path<IExplorableState> pathSoFar, 
			Collection<IExplorableState> visitedStates,
			IExplorableState state,
			int remainingDepth			
			) {
		
		
		if(remainingDepth==0) return null ;

		stats.numberOfTransitionsExplored++ ;
		
		//System.out.println(">>> #visited states: " + visitedStates.size()) ;
		//for(var st : visitedStates) {
		//	System.out.println("     " + st) ;
		//}
		
		if(visitedStates.contains(state)) {
			return null ;
		}
		// else the state is new
		stats.numberOfStatesExplored++ ;
		visitedStates.add(state) ;
		
		if(whatToFind.test(state)) {
			// we find a state satisfying the search criterion!
			return pathSoFar.copy() ;	
		}
		
		var nextTransitions = model.availableTransitions() ;
		for(var tr: nextTransitions) {
			model.execute(tr);
			var nextState = (IExplorableState) model.getCurrentState().clone() ;
			pathSoFar.addTransition(tr, nextState);
			// recurse to the next state:
			Path<IExplorableState> result = dfs(whatToFind,pathSoFar,visitedStates,nextState,remainingDepth-1) ;
			if(result != null) {
				// a solving path is found! Return the path:
				return result ;
			}	
			pathSoFar.removeLastTransition();
			model.backTrackToPreviousState() ;
		}
		return null ;		
	}
	
	/**
	 * Representing a test suite produced by
	 * {@link BasicModelChecker#testSuite(List, Function, int, boolean)}.
	 * 
	 * @param <CoverageItem> The type of 'items' that we want to cover.
	 */
	public static class TestSuite<CoverageItem> {
		
		/**
		 * All the 'targets' that are to be covered.
		 */
		public List<CoverageItem> targets = new LinkedList<>() ;
		
		/**
		 * The test-cases that constitute this test suite. It is a list of pairs (tc,o) where
		 * tc is a test-case and o that coverage-target that the last state of tc covers.
		 * Note that a test-case would typically covers more targets than just this o;
		 * we just mention this o for convenience. 
		 */
		public List<Pair<Path,CoverageItem>> tests = new LinkedList<>() ;
		
		/**
		 * All coverage 'targets' that are covered by this test-suite. It will also include
		 * targets that were not specified in {@link #targets} but happen to be covered by
		 * the test-suite.
		 */
		public List<CoverageItem> covered = new LinkedList<>() ;
		
		/**
		 * Give all targets specified by {@link #targets} that are still left uncovered.
		 */
		public List<CoverageItem> notCovered() {
			return targets.stream().filter(st -> ! covered.contains(st)).collect(Collectors.toList()) ;
		}
		
		/**
		 * Return the coverage degree, which is a number in [0..1]; it is 1 if all
		 * targets from {@link #targets} are covered.
		 */
		public float coverage() {
			
			// the number of actual targets that are covered:
			long net_covered = this.targets.stream().filter(t -> covered.contains(t)).count() ;
					
			return ((float) net_covered ) / (float) targets.size() ;
		}
	}
	
	/**
	 * Return a test suite that tries to cover the specified coverage targets.
	 * Although the model-checking procedure is exhaustive up to the given maximum
	 * depth/length, note that it may not be possible to cover all the given
	 * targets. The returned test suite give a suite that would cover as much as
	 * possible.
	 * 
	 * @param <CoverageItem>   The type of coverage-targets.
	 * @param itemsToCover     A list of targets to cover.
	 * @param coverageFunction A function that maps the state of the target
	 *                         'program' to a coverage target. Given a state s, this
	 *                         function maps s to a target. E.g. is s can be though
	 *                         to have some int variable x, this function can map s
	 *                         to 0 if s.x is 0 or less, and to 1 if s.x is 1, and
	 *                         to 2 if s.x is 2 or larger. So, implicitly this means
	 *                         that we would then have three possible coverage
	 *                         targets, namely 0,1,2.
	 * @param maxLength        The maximum length of the test-cases. The model
	 *                         checker will not try to find test-cases that are
	 *                         longer than this specified length.
	 * @param minimizeLength   If true, the model checker will also to minimize the
	 *                         length of each test-case. Note that the minimization
	 *                         is performed per test-case. This does not guarantee
	 *                         that the resulting test-suite would be minimal in the
	 *                         total number of steps. Minimizing the whole
	 *                         test-suite would be very expensive.
	 * 
	 * @return The resulting test-suite.
	 */
	public <CoverageItem> TestSuite<CoverageItem> testSuite(
			List<CoverageItem> itemsToCover, 
			Function<IExplorableState,CoverageItem> coverageFunction,
			int maxLength, 
			boolean minimizeLength) {
		
		
		Set<CoverageItem> covered = new HashSet<>() ;
		
		TestSuite<CoverageItem> suite = new TestSuite<>() ;
		suite.targets.addAll(itemsToCover) ;
	
		List<CoverageItem> worklist = new LinkedList<>() ;
		worklist.addAll(itemsToCover) ;
		
		while (! worklist.isEmpty()) {
			var target = worklist.remove(0) ;
			
			if(covered.contains(target)) {
				// the state is already covered
				continue ;
			}
			
			// else it is still uncovered
			
			System.out.print("=== targeting: " + target) ;
			Path<IExplorableState> path ;
			if(minimizeLength) {
				path = findShortest(st -> coverageFunction.apply(st).equals(target), maxLength) ;
			}
			else {
				path = find(st -> coverageFunction.apply(st).equals(target), maxLength) ;
			}
			if (path == null) {
				System.out.println(" NO") ;
				continue ;
			}
			else {
				System.out.println(" YES") ;
			}
			
			suite.tests.add(new Pair<Path,CoverageItem>(path,target)) ;
			
			covered.addAll(path.path.stream()
					.map(step -> coverageFunction.apply(step.snd))
					.collect(Collectors.toList())) ;				
			
		}
				
		suite.covered.addAll(covered) ;
		
		//System.out.println(">>> #covered =" + covered.size()) ;
		//System.out.println(">>> #covered2 =" + suite.covered.size()) ;
		
		return suite ;
	}
	

}
