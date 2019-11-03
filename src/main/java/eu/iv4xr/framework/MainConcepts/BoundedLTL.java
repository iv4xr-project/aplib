package eu.iv4xr.framework.MainConcepts;

import java.util.*;
import java.util.function.Predicate;

import nl.uu.cs.aplib.MainConcepts.Environment;

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
public class BoundedLTL {
	
	LTL ltl ;
	Predicate<Environment> startf ;
	Predicate<Environment> endf ;
	Integer maxlength ;
	
	enum BLTLstate { NOTSTARTED, STARTED , SATFOUND  }
	
	BLTLstate state  = BLTLstate.NOTSTARTED ;
	List<String> trace = new LinkedList<>() ;
	
	boolean keepFullTrace = false ;
	
	/**
	 * Search for an interval on which the LTL antecedent of this bounded-LTL is satisfied.
	 * If this is found, the LTL antecedent should hold the abstract trace of the satisfying
	 * interval. 
	 * @param env
	 * @return
	 */
	public VERDICT sat(Environment env) {
		switch(state) {
			case SATFOUND : return VERDICT.SAT ;
			case NOTSTARTED :
				if (startf.test(env)) {
					ltl.resettracking();
					ltl.evalAtomSat(env);
					state = BLTLstate.STARTED ;
					if (endf.test(env)) {
						// the interval ends immediately
						var verdict = ltl.sat() ;
						switch(verdict) {
							case SAT : state = BLTLstate.SATFOUND ; break ;
							default  : state = BLTLstate.NOTSTARTED ;
						}
						return verdict ;
					}
				}
				return VERDICT.UNKNOWN ;
			case STARTED :
				if (maxlength != null && ltl.absexecution.size()>=maxlength) {
					// maximum interval length is reached exceeded, since the end-marker
					// is not seen yet, the next evaluation will exceed the max-length anyway,
					// so we abort the evaluation:
					state = BLTLstate.NOTSTARTED ;
					return VERDICT.UNKNOWN ;
				}
				
				// pass the env to the ltl to have its atoms evaluated:
				ltl.evalAtomSat(env) ;
				
				if (endf.test(env)) {
					// end marker holds; then force full evaluation of the ltl
					var verdict = ltl.sat() ;
					switch(verdict) {
						case SAT  : state = BLTLstate.SATFOUND ; break ;
						default   : state = BLTLstate.NOTSTARTED ;
					}
					return verdict ;
				}
				else {
					return VERDICT.UNKNOWN ;
				}
		}
		// should not reach this point
		return null ;
	}
	

	public static abstract class LTL {
		LinkedList<VerdictInfo> absexecution = new LinkedList<VerdictInfo>() ;
		LTL() { }
		abstract void resettracking() ;
		abstract VERDICT sat() ;
		abstract void evalAtomSat(Environment env) ;	
	}
	
	static public enum VERDICT { SAT, UNSAT, UNKNOWN }
	static public class VerdictInfo { 
		public VERDICT verdict ;
		VerdictInfo(VERDICT v) { verdict = v ; }
	}
	
	
	static public class Atom extends LTL {
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
	
	static public class Not extends LTL {
		LTL phi ;
		
		@Override
		void resettracking() {
			absexecution.clear(); phi.resettracking();	
		}

		@Override
		VERDICT sat() {
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
	
	static public class Until extends LTL {
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
				var q = iteratorPhi1.next().verdict ;
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
	

}
