package eu.iv4xr.framework.extensions.ltl;

import static eu.iv4xr.framework.extensions.ltl.LTL.* ;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * For translating an LTL formula to a Buchi automaton. For now, we
 * exclude the following patterns:
 * 
 *    <ol>
 *    <li> The translator does not handle conjunctions "phi && psi";
 *    <li> also does not handle left-recursive until and weak-until.
 *         E.g. "(p U q) U r" cannot be handled.
 *    </ol>     
 * 
 * Before translating, the input LTL formula is first normalized using
 * the following rewrites to push negation inside:
 * 
 *    <ol>
 *    <li> "not X phi" to "X not phi"
 *    <li> "not(phi && psi)" to "(not phi) || (not psi)"  
 *    <li> "not(phi || psi)" to "(not phi) && (not psi)"  
 *    <li> "not(phi U psi) to "(phi && not psi) W (not phi && not psi)"
 *    <li> "not(phi W psi) to "(phi && not psi) U (not phi && not psi)"  
 *    </ol>
 * 
 * After the normalization, a Buchi automaton is constructed recursively.
 * The key recursions are these:
 * 
 *    <ol>
 *    <li> X phi where phi is non-atom LTL formula. Let B is the Buchi of
 *    phi, with S1 as the initial state. Create a new initial state S0,
 *    and add (S0,true,S1) as a new transition.
 *    
 *    <li> p U psi, where p is a state-predicate (atom). Let B is the Buchi of 
 *    psi. Let S1 be the initial state of B, Create a new initial state 
 *    S0, with (S0,p,S0) as a transition. For each out-going transitions
 *    of S1: (S1,q,T), add a new transition (S0,q,T).
 *     
 *    <li> phi U psi, where phi is not a state-predicate: unimplemented.
 *    
 *    <li> p W psi, where p is a state-predicate (atom). Let B is the Buchi of 
 *    psi. Let S1 be the initial state of B, Create a new initial state 
 *    S0, with (S0,p,S0) as a transition. 
 *    We also add S0 as an omega-accepting state.
 *    For each out-going transition of S1: (S1,q,T), add a new transition (S0,q,T). 
 *    
 *    <li> phi W psi, where phi is not a state-predicate: unimplemented.
 *    
 *    <li> phi || psi. Let B and C be the Buchis of phi and psi, respectively,
 *    and S1 and S2 their initial states. We make S1 as the initial state of
 *    the combined Buchi. We remove S2, and change every transition that goes
 *    from or to S2 to go from/to S1.
 *    
 *    <li> phi && psi. Not implemented. TODO.
 *    
 *    </ol>
 * 
 * 
 * @author Wish
 *
 */
public class LTL2Buchi {
	
	// ======
	// Bunch of functions to recognize patterns and deconstruct them
	// ======
	
	public static <State> boolean isAtom(LTL<State> phi) {
		return phi instanceof Now ;
	}
	
	public static <State> boolean isNotAtom(LTL<State> phi) {
		return !(phi instanceof Now) ;
	}
	
	/**
	 * To recognize and destruct "now p".
	 */
	public static <State> Predicate<State> isNow(LTL<State> phi) {
		if(isNotAtom(phi)) return null ;
		return ((Now<State>) phi).p ;
	}

	/**
	 * To recognize and destruct "not(now p)".
	 */
	public static <State> Now<State> isNotNow(LTL<State> phi) {
		if(! (phi instanceof Not)) return null ;
		var f = (Not<State>) phi ;
		if(isNotAtom(f.phi)) return null ;
		return (Now<State>) f.phi ;
	}
	
	/**
	 * To recognize and destruct "not(phi)" pattern, where phi
	 * is non-atomic.
	 */
	public static <State> LTL<State> isNotPhi(LTL<State> phi) {
		if(! (phi instanceof Not)) return null ;
		var f = (Not<State>) phi ;
		if(isAtom(f.phi)) return null ;
		return f.phi ;
	}
	
	public static <State> Next<State> isNotNext(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Next)) return null ;
		return (Next<State>) f ;
	}
	
	public static <State> Not<State> isNotNot(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Not)) return null ;
		return (Not<State>) f ;
	}
	
	public static <State> And<State> isNotAnd(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof And)) return null ;
		return (And<State>) f ;
	}
	
	public static <State> Or<State> isNotOr(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Or)) return null ;
		return (Or<State>) f ;
	}
	
	public static <State> Until<State> isNotUntil(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Until)) return null ;
		return (Until<State>) f ;
	}
	
	public static <State> WeakUntil<State> isNotWeakUntil(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Until)) return null ;
		return (WeakUntil<State>) f ;
	}
	
	/**
	 * Normalize an LTL formula by rewriting it, so that negations
	 * are recursively pushed as far as possible inside.
	 */
	public static <State> LTL<State> pushNegations(LTL<State> f) {
		
		// case atom
		if(isNow(f) != null) return f ;
		
		// case not p:
		Now<State> phi_case1 = isNotNow(f) ;
		if(phi_case1 != null) {
			Now<State> f2 = new Now<>() ;
			f2.p = S -> ! phi_case1.p.test(S) ;
			return f2 ;
		}
		
		// case not X phi
		Next<State> phi_case2 = isNotNext(f) ;
		if(phi_case2 != null) {
			Next<State> f2 = new Next<>() ;
			f2.phi = pushNegations(ltlNot(phi_case2.phi)) ;
			return f2 ;
		}
		
		// case not not phi:
		Not<State> phi_case3 = isNotNot(f) ; // this gives not phi
		if(phi_case3 != null) {
			return pushNegations(phi_case3.phi) ; // this returns the phi
		}
		
		// case not(phi && psi)
		And<State> phi_case4 = isNotAnd(f) ;
		if(phi_case4 != null) {
			
			var inners = Arrays.asList(phi_case4.conjuncts)
			   .stream()
			   .map((LTL<State> g) -> pushNegations(ltlNot(g)))
			   .collect(Collectors.toList())
			   .toArray() 
			;
			
			return ltlOr((LTL<State>[]) inners) ;
		}
		
		// case not(phi || psi)
		Or<State> phi_case5 = isNotOr(f) ;
		if(phi_case5 != null) {
					
			var inners = Arrays.asList(phi_case5.disjuncts)
				.stream()
				.map((LTL<State> g) -> pushNegations(ltlNot(g)))
				.collect(Collectors.toList())
				.toArray() 
				;
					
			return ltlAnd((LTL<State>[]) inners) ;
		}
		
		// case not(phi U psi)
		Until<State> phi_case6 = isNotUntil(f) ;
		if(phi_case6 != null) {
					
			var psi1 = pushNegations(ltlAnd(phi_case6.phi1, 
					                    ltlNot(phi_case6.phi2))) ;
			
			var psi2 = pushNegations(ltlAnd(ltlNot(phi_case6.phi1), 
                                        ltlNot(phi_case6.phi2))) ;
					
			return psi1.weakUntil(psi2) ;
		}
		// case not(phi W psi)
		WeakUntil<State> phi_case7 = isNotWeakUntil(f) ;
		if(phi_case7 != null) {
					
			var psi1 = pushNegations(ltlAnd(phi_case7.phi1, 
					                    ltlNot(phi_case7.phi2))) ;
			
			var psi2 = pushNegations(ltlAnd(ltlNot(phi_case7.phi1), 
                                        ltlNot(phi_case7.phi2))) ;
					
			return psi1.until(psi2) ;
		}
		
		// if come here then the normalizer cannot handle the input
		// LTL formula
		throw new IllegalArgumentException() ;
	}
	
	/**
	 * Translate the given LTL formula to a Buchi automaton.
	 */
	public static <State> Buchi getBuchi(LTL<State> phi) {
		throw new UnsupportedOperationException();
	}
	
	


}
