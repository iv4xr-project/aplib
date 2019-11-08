package eu.iv4xr.framework.MainConcepts;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import nl.uu.cs.aplib.MainConcepts.Environment;
import nl.uu.cs.aplib.MainConcepts.Environment.EnvironmentInstrumenter;

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
	VERDICT sat(Environment env) {
		switch(bltlState) {
			case SATFOUND : return VERDICT.SAT ;
			case NOTSTARTED :
				if (startf.test(env)) {
					registerToTrace(env) ;
					ltl.resettracking();
					ltl.evalAtomSat(env);
					bltlState = BLTLstate.STARTED ;
					if (endf.test(env)) {
						// the interval ends immediately
						var verdict = ltl.sat() ;
						if (verdict == VERDICT.SAT) {
							bltlState = BLTLstate.SATFOUND ;
						}
						else {
							trace.reset(); bltlState = BLTLstate.NOTSTARTED ;
						}
						return verdict ;
					}
				}
				return VERDICT.UNKNOWN ;
			case STARTED :
				// pass the env to the ltl to have its atoms evaluated:
				registerToTrace(env) ;
				ltl.evalAtomSat(env) ;
				
				if (endf.test(env)) {
					// end marker holds; then force full evaluation of the ltl
					var verdict = ltl.sat() ;
					if (verdict == VERDICT.SAT) {
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
					return VERDICT.UNKNOWN ;
				}
		}
		// should not reach this point
		return null ;
	}
	
	public VERDICT getVerdict() {
		if (bltlState == BLTLstate.SATFOUND) return VERDICT.SAT ;
		return VERDICT.UNSAT ;
	}
	
	public ExecutionTrace getWitness() {
		if (bltlState == BLTLstate.SATFOUND) return trace ;
		return null ;
	}

	/**
	 * Representing an LTL formula.
	 */
	public abstract static class LTL {
		
		/**
		 * Abstractlty representing the finite execution on which this LTL is interpreted.
		 */
		LinkedList<VerdictInfo> absexecution = new LinkedList<VerdictInfo>() ;
		
		LTL() { }
		
		
		abstract void resettracking() ;
		
		/**
		 * Check if {@link #absexecution} satisfies this LTL formula.
		 */
		abstract VERDICT sat() ;
		
		/**
		 * Evaluate the atoms of this LTL formula on the given env (concrete state),
		 * and add the verdict at the end of {@link #absexecution}.
		 */
		abstract void evalAtomSat(Environment env) ;	

		/**
		 * Construct the LTL formula "phi Until psi", where phi is this LTL formula.
		 */
		public LTL ltlUntil(LTL psi) {
			var ltl = new Until() ;
			ltl.phi1 = this ;
			ltl.phi2 = psi ;
			return ltl ;
		}
	}
	
	public enum VERDICT { SAT, UNSAT, UNKNOWN }
	public static class VerdictInfo { 
		public VERDICT verdict ;
		VerdictInfo(VERDICT v) { verdict = v ; }
	}
	
	
	public static class Atom extends LTL {
		Predicate<Environment> p ;
		void check(Environment env) {
					
 		}
		@Override
		void resettracking() { absexecution.clear(); }
		
		@Override
		VERDICT sat() {
			return absexecution.getFirst().verdict ;
		}
		
		@Override
		void evalAtomSat(Environment env) {
			if (p.test(env)) 
				absexecution.add(new VerdictInfo(VERDICT.SAT)) ;
			else
				absexecution.add(new VerdictInfo(VERDICT.UNSAT)) ;		
		}
	}
	
	public static class Not extends LTL {
		LTL phi ;
		
		@Override
		void resettracking() {
			absexecution.clear(); phi.resettracking();	
		}

		@Override
		VERDICT sat() {
			phi.sat() ;
			var iterator = absexecution.descendingIterator() ;
			var iteratorPhi = phi.absexecution.descendingIterator() ;
			
			while (iterator.hasNext()) {
				var psi = iterator.next() ;
				var p = iteratorPhi.next().verdict ;
				switch (p) {
				case SAT : psi.verdict = VERDICT.UNSAT ; break ;
				case UNSAT : psi.verdict = VERDICT.SAT ; break ;
				}
			}
			
			return absexecution.getFirst().verdict ;
		}

		@Override
		void evalAtomSat(Environment env) {
			absexecution.add(new VerdictInfo(VERDICT.UNKNOWN)) ;
			phi.evalAtomSat(env);		
		}
	}
	
	public static class And extends LTL {
		LTL[] conjuncts ;
		
		@Override
		void resettracking() {
			absexecution.clear(); 
			for (LTL phi : conjuncts) phi.resettracking();	
		}
		
		@Override
		VERDICT sat() {
			for (LTL phi : conjuncts) phi.sat() ;
			var iterator = absexecution.descendingIterator() ;
			var N = conjuncts.length ;
			Iterator<VerdictInfo>[] conjuntIterators = new Iterator[N] ;
			for (int k=0; k<N; k++)
				conjuntIterators[k] = conjuncts[k].absexecution.descendingIterator() ;
		
			while (iterator.hasNext()) {
				var psi = iterator.next() ;
				boolean allSat = true ;
				for (int k=0; k<N; k++) {
					var p = conjuntIterators[k].next().verdict ;
					allSat = allSat && (p == VERDICT.SAT) ;
				}
				if (allSat) psi.verdict = VERDICT.SAT ;
				else psi.verdict = VERDICT.UNSAT ;
			}
			return absexecution.getFirst().verdict ;
		}
		
		@Override
		void evalAtomSat(Environment env) {
			absexecution.add(new VerdictInfo(VERDICT.UNKNOWN)) ;
			for (LTL phi : conjuncts) phi.evalAtomSat(env);		
		}
	}
	
	
	public static class Until extends LTL {
		LTL phi1 ;
		LTL phi2 ;
		
		@Override
		void resettracking() {
			absexecution.clear(); 
			phi1.resettracking();	
			phi2.resettracking();	
		}
		
		@Override
		VERDICT sat() {
			phi1.sat() ;
			phi2.sat() ;
			var iterator = absexecution.descendingIterator() ;
			var iteratorPhi1 = phi1.absexecution.descendingIterator() ;
			var iteratorPhi2 = phi2.absexecution.descendingIterator() ;
			
			// keep track if phi1 untill phi2 holds at sigma(k+1)
			boolean nextSat = false ;
			
			// calculate phi1 until phi2 holds on every sigma(k); we calculate this
			// backwards for every state in the interval:
			while (iterator.hasNext()) {
				var psi = iterator.next() ;
				var p = iteratorPhi1.next().verdict ;
				var q = iteratorPhi2.next().verdict ;
				if (q == VERDICT.SAT) {
					psi.verdict = VERDICT.SAT ;
					nextSat = true ;
				}
				else {
					if (nextSat && p == VERDICT.SAT) psi.verdict = VERDICT.SAT ;
					else {
						psi.verdict = VERDICT.UNSAT ;
						nextSat = false ;	
					}
				}
			}
			return absexecution.getFirst().verdict ;
		}
		
		@Override
		void evalAtomSat(Environment env) {
			absexecution.add(new VerdictInfo(VERDICT.UNKNOWN)) ;
			phi1.evalAtomSat(env);	
			phi2.evalAtomSat(env);			
		}						
	}
	
	static public class Next extends LTL {
		
		LTL phi ;

		@Override
		void resettracking() {
			absexecution.clear(); 
			phi.resettracking();		
		}

		@Override
		VERDICT sat() {
			phi.sat() ;
			var iterator = absexecution.descendingIterator() ;
			var iteratorPhi = phi.absexecution.descendingIterator() ;
			
			var psi = iterator.next() ;
			psi.verdict = VERDICT.UNSAT ; // always unsat at the last state

			// calculate phi1 until phi2 holds on every sigma(k); we calculate this
			// backwards for every state in the interval:
			while (iterator.hasNext()) {
				psi = iterator.next() ;
				var q = iteratorPhi.next().verdict ;
				switch(q) {
				  case SAT   : psi.verdict = VERDICT.SAT ; break ;
				  case UNSAT : psi.verdict = VERDICT.UNSAT ;
				}
			}
			
			return absexecution.getFirst().verdict ;
		}

		@Override
		void evalAtomSat(Environment env) {
			absexecution.add(new VerdictInfo(VERDICT.UNKNOWN)) ;
			phi.evalAtomSat(env);			
		}
		
	}
	
	public static <E> LTL now(Predicate<E> p) {
		var a = new Atom() ;
		a.p = env -> p.test((E) env) ;
		return a ;
	}
	
	public static LTL next(LTL phi) { 
		var ltl = new Next() ;
		ltl.phi = phi ;
		return ltl ;
	}
	
	public static LTL ltlNot(LTL phi) {
		var ltl = new Not() ;
		ltl.phi = phi ; return ltl ;
	}
	
	public static LTL ltlAnd(LTL... phis) {
		if (phis == null) throw new IllegalArgumentException() ;
		if (phis.length <2) throw new IllegalArgumentException() ;
		var ltl = new And() ;
		ltl.conjuncts = phis ;
		return ltl ;
	}
	
	public static LTL eventually(LTL phi) {
		return now((Environment env) -> true).ltlUntil(phi) ;
	}
	
	public static LTL always(LTL phi) {
		return ltlNot(eventually(ltlNot(phi))) ;
	}

}
