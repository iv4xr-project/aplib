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
 * <p>Additional functionalities are provided e.g. to find a minimum length witness
 * and to produce a test-suite that covers all states of the model.
 * 
 * 
 * 
 * 
 * @author Wish
 *
 */
public class BasicModelChecker {
	
	/**
	 * The 'program' or 'model of a program' that we want to target in model-checking.
	 */
	public ITargetModel model ;
	
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
	
	public BasicModelChecker(ITargetModel model) {
		this.model = model ;
	}
	
	public static class Path<State> {
		
		public List<Pair<ITransition,State>> path = new LinkedList<>() ;
		
		void addInitialState(State state) {
		    path.add(new Pair(null,state)) ;	
		}
		
		void addTransition(ITransition tr, State state) {
			path.add(new Pair(tr,state)) ;
		}
		
		void removeLastTransition() {
			path.remove(path.size() - 1) ;
		}
		
		Path copy() {
			Path z = new Path() ;
			for(var step : this.path) {
				z.addTransition(step.fst, step.snd);
			}
			return z ;
		}
		
		public List<State> getStateSequence() {
			return path.stream().map(step -> step.snd).collect(Collectors.toList()) ;
		}
		
		@Override
		public String toString() {
			int k = 0;
            String s = "";
            for (var tr : path) {
                if (k > 0)
                    s += "\n";
                s += k + ": action:" + tr.fst + " --> state:" + tr.snd + ">";
                k++;
            }
            return s;
		}
	}
	
	public SATVerdict sat(Predicate<IExplorableState> q) {
		var path = find(q,Integer.MAX_VALUE) ;
		if(path == null) return SATVerdict.UNSAT ;
		return SATVerdict.SAT ;
	}
	
	public Path<IExplorableState> find(Predicate<IExplorableState> q, int maxDepth) {
		model.reset();
		stats.clear(); 
		Path<IExplorableState> path = new Path<>() ;
		Collection<IExplorableState> visitedStates = new HashSet<>() ;
		IExplorableState state = model.getCurrentState() ;
		path.addInitialState(state);
		
		return dfs(q,path,visitedStates,state,maxDepth+1) ;
		
	}
	
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
			var nextState = model.getCurrentState() ;
			pathSoFar.addTransition(tr, nextState);
			// recurse to the next state:
			Path result = dfs(whatToFind,pathSoFar,visitedStates,nextState,remainingDepth-1) ;
			if(result != null) {
				// a solving path is found! Return the path:
				return result ;
			}	
			pathSoFar.removeLastTransition();
			model.backTrackToPreviousState() ;
		}
		return null ;		
	}
	
	public static class TestSuite<CoverageItem> {
		public List<CoverageItem> targets = new LinkedList<>() ;
		public List<Pair<Path,CoverageItem>> tests = new LinkedList<>() ;
		public List<CoverageItem> covered = new LinkedList<>() ;
		public List<CoverageItem> notCovered() {
			return targets.stream().filter(st -> ! covered.contains(st)).collect(Collectors.toList()) ;
		}
		public float coverage() {
			return ((float) covered.size() ) / (float) targets.size() ;
		}
	}
	
	public <CoverageItem> TestSuite testSuite(
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
			suite.tests.add(new Pair(path,target)) ;
			
			covered.addAll(path.path.stream()
					.map(step -> coverageFunction.apply(step.snd))
					.collect(Collectors.toList())) ;				
			
		}
		
		suite.covered.addAll(covered) ;
		
		return suite ;
	}
	

}
