package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.Environment.EnvironmentInstrumenter;

/**
 * A Bouded LTL property F is a tuple (p,q,ltl,n). It is interpreted over a finite sequence of
 * states representing execution. Let sigma be such a sequence. p and q are state predicates.
 * They define a sequence of segments in sigma, as follows. A segment z starts on a state where
 * p holds for the first time since the previous segment. If there is no previous segment, z
 * starts at the first state where p holds. The segment z ends when q holds the first time at
 * or after the start of z. Defined in this way, note that the segments do not overlap.
 * 
 * <p>ltl is an LTL formula interpreted over a finite sequence of states. sigma satisfies F
 * is there exists a [p,q] segment in sigma, whose length is less than n, and such that ltl
 * holds on this segment. Notice the existential-interpretation of F. So, if we instead
 * want to verify that some LTL property G would hold on ALL [p,q] segments of length less than n
 * in sigma, we should check the satisfaction of not G instead.
 * 
 * @author wish
 *
 */
public class BoundedLTL implements EnvironmentInstrumenter {
	
	LTL ltl ;
	Predicate<Environment> startf ;
	Predicate<Environment> endf ;
	Integer maxlength = null ;
	Function<Environment,String> stateShowFunction = null ;
	Function<Environment,String> transitionShowFunction = env -> { 
		var tr = env.getLastOperation() ;
		return "agent=" + tr.invokerId + "; command=" + tr.command + "; target=" + tr.targetId + "; result=" + tr.result ;
	} ;
	
	
	enum BLTLstate { NOTSTARTED, STARTED , SATFOUND  }
	
	BLTLstate bltlState  = BLTLstate.NOTSTARTED ;
	ExecutionTrace trace = new ExecutionTrace() ;
	
	public BoundedLTL() { }
	public BoundedLTL thereIs(LTL F) { ltl = F ; return this ;}
	public BoundedLTL when_(Predicate<Environment> p) { startf = p ; return this ; }
	public <E> BoundedLTL when(Predicate<E> p) { return when_(env -> p.test((E) env)) ; }
	public BoundedLTL until_(Predicate<Environment> q) { endf = q ; return this ; }
	public <E> BoundedLTL until(Predicate<E> q) { return until_(env -> q.test((E) env)); }
	public BoundedLTL withMaxLength(int n) { 
		if (n<1) throw new IllegalArgumentException() ;
		maxlength = n ; return this ; 
	}
	public BoundedLTL withStateShowFunction_(Function<Environment,String> f) { stateShowFunction = f ; return this ; }
	public <E> BoundedLTL withStateShowFunction(Function<E,String> f) { 
		return withStateShowFunction_(env -> f.apply((E) env)) ;
	}
	public BoundedLTL attachToEnv(Environment env) {
		env.registerInstrumenter(this) ;
		return this ;
	}
	
	/**
	 * This will reset the state of this bounded LTL checker to its initial state, but only
	 * if no SAT has been found. Else if will leave the state of the checker unchanged.
	 */
	public void reset() {
		if (bltlState != BLTLstate.SATFOUND) {
			bltlState = BLTLstate.NOTSTARTED ;
			trace.reset(); 
		}
	}
	
	/**
	 * This defines when the environment will be sampled for sat checking by this
	 * bounded LTL checker. This will happen whenever the environment invokes
	 * an "operation". Currently the policy is that the checker will only sample the environment
	 * state whenever the refresh() operation is invoked. 
	 */
	public void update(Environment env) {
		if (env.lastOperationWasRefresh()) {
			//System.out.println(">>> invoking sat(env)") ;
			sat(env) ;
		}
	}
	
	public static class ExecutionTrace {
		/**
		 * A list of "transition". Every transition is represented by an array of two elements, with
		 * a[0] is a string representing the label of the transition, and a[1] representing the
		 * resulting state of the transition.
		 */
		List<String[]> trace = new LinkedList<>() ;
		
		public void register(String transitionLabel, String destinationState) {
			String[] tr = {transitionLabel, destinationState } ;
			trace.add(tr) ;
		}
		
		public void reset() { trace.clear(); }
		
		public String getState(int k) {
			if (k<0 || k >= trace.size()) throw new IllegalArgumentException() ;
			if (k==0) return null ;
			return trace.get(k)[1] ;
		}
		
		public String getTransitionLabel(int k) {
			if (k<0 || k >= trace.size()) throw new IllegalArgumentException() ;
			return trace.get(k)[0] ;
		}
		
		public String getTransition(int k) {
			if (k<0 || k >= trace.size()) throw new IllegalArgumentException() ;
			var tr = trace.get(k) ;
			return "" + getState(k) + ";" + tr[0] + ";" + tr[1] ;
		}
		
		public List<String[]> getTrace() { return trace ; }
		
		public String toString() {
			int k = 0 ;
			String s = "" ;
			for (String[] tr : trace) {
				if (k>0) s += "\n" ;
				s += k + ";<" + tr[0] + ">;<" + tr[1] + ">" ;
				k++ ;
			}
			return s ;
		}

	}
	
	private void registerToTrace(Environment env) {
		String oprStr = "?" ;
		if (env.getLastOperation() != null) oprStr = transitionShowFunction.apply(env) ;
		String stateStr = "?" ;
		if (stateShowFunction != null) stateStr = stateShowFunction.apply(env) ;
		trace.register(oprStr, stateStr);
	}
	
	/**
	 * Search for an interval on which the LTL antecedent of this bounded-LTL is satisfied.
	 * If this is found, the LTL antecedent should hold the abstract trace of the satisfying
	 * interval. 
	 * @param env
	 * @return
	 */
	LTLVerdict sat(Environment env) {
		switch(bltlState) {
			case SATFOUND : return LTLVerdict.SAT ;
			case NOTSTARTED :
				if (startf.test(env)) {
					registerToTrace(env) ;
					ltl.resettracking();
					ltl.evalAtomSat(env);
					bltlState = BLTLstate.STARTED ;
					if (endf.test(env)) {
						// the interval ends immediately
						var verdict = ltl.sat() ;
						if (verdict == LTLVerdict.SAT) {
							bltlState = BLTLstate.SATFOUND ;
						}
						else {
							trace.reset(); bltlState = BLTLstate.NOTSTARTED ;
						}
						return verdict ;
					}
				}
				return LTLVerdict.UNKNOWN ;
			case STARTED :
				// pass the env to the ltl to have its atoms evaluated:
				registerToTrace(env) ;
				ltl.evalAtomSat(env) ;
				
				if (endf.test(env)) {
					// end marker holds; then force full evaluation of the ltl
					var verdict = ltl.sat() ;
					if (verdict == LTLVerdict.SAT) {
						bltlState = BLTLstate.SATFOUND ;
					}
					else {
						trace.reset(); bltlState = BLTLstate.NOTSTARTED ;
					}
					return verdict ;
				}
				else {
					if (maxlength != null && ltl.absexecution.size()>=maxlength) {
						// maximum interval length is reached, since the end-marker
						// is not seen yet, we stop the evaluation:
						bltlState = BLTLstate.NOTSTARTED ;
						trace.reset();
					}
					return LTLVerdict.UNKNOWN ;
				}
		}
		// should not reach this point
		return null ;
	}
	
	public LTLVerdict getVerdict() {
		if (bltlState == BLTLstate.SATFOUND) return LTLVerdict.SAT ;
		return LTLVerdict.UNSAT ;
	}
	
	public ExecutionTrace getWitness() {
		if (bltlState == BLTLstate.SATFOUND) return trace ;
		return null ;
	}

	public enum LTLVerdict { SAT, UNSAT, UNKNOWN }
	public static class LTLVerdictInfo { 
		public LTLVerdict verdict ;
		LTLVerdictInfo(LTLVerdict v) { verdict = v ; }
	}
	
}
