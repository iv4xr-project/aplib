package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Predicate;

import nl.uu.cs.aplib.utils.Pair;

/**
 * 
 * @author Wish
 *
 */
public class ModelChecker {
	
	public ITargetModel model ;
	
	
	public ModelChecker(ITargetModel model) {
		this.model = model ;
	}
	
	static class Path {
		List<Pair<ITransition,IExplorableState>> path = new LinkedList<>() ;
		
		void addInitialState(IExplorableState state) {
		    path.add(new Pair(null,state)) ;	
		}
		
		void addTransition(ITransition tr, IExplorableState state) {
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
	
	public Path find(Predicate<IExplorableState> q, int maxDepth) {
		model.reset();
		Path path = new Path() ;
		Collection<IExplorableState> visitedStates = new HashSet<>() ;
		IExplorableState state = model.getCurrentState() ;
		path.addInitialState(state);
		
		return dfs(q,path,visitedStates,state,maxDepth+1) ;
		
	}
	
	public Path findShortest(Predicate<IExplorableState> q, int maxDepth) {
		for(int depth=0; depth<=maxDepth; depth++) {
			Path path = find(q,depth) ;
			if(path != null) {
				return path ;
			}
		}
		return null ;		
	}
	
	
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
		
	Path dfs(Predicate<IExplorableState> whatToFind, 
			 Path pathSoFar, 
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
	

}
