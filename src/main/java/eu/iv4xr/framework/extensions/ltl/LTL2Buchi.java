package eu.iv4xr.framework.extensions.ltl;

import static eu.iv4xr.framework.extensions.ltl.LTL.* ;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.utils.Pair;

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
	
	
	public static <State> LTL<State> arg(LTL<State> phi) {
		if (phi instanceof Not) {
			return ((Not<State>) phi).phi ;
		}
		if (phi instanceof Next) {
			return ((Next<State>) phi).phi ;
		}
		throw new IllegalArgumentException("Formula " + phi + " has no LTL-arg") ;
	}
	
	public static <State> LTL<State> argL(LTL<State> phi) {
		if (phi instanceof Until) {
			return ((Until<State>) phi).phi1 ;
		}
		if (phi instanceof WeakUntil) {
			return ((Until<State>) phi).phi1 ;
		}
		throw new IllegalArgumentException("Formula " + phi + " has no LTL-argL") ;
	}
	
	public static <State> LTL<State> argR(LTL<State> phi) {
		if (phi instanceof Until) {
			return ((Until<State>) phi).phi2 ;
		}
		if (phi instanceof WeakUntil) {
			return ((Until<State>) phi).phi2 ;
		}
		throw new IllegalArgumentException("Formula " + phi + " has no LTL-argR") ;
	}
	
	
	// ======
	// Bunch of functions to recognize patterns and de-construct them
	// ======
	
	public static <State> boolean isAtom(LTL<State> phi) {
		return phi instanceof Now ;
	}
	
	public static <State> boolean isNotAtom(LTL<State> phi) {
		return !(phi instanceof Now) ;
	}
	
	/**
	 * To recognize and destruct "now p". Returns p.
	 */
	public static <State> Predicate<State> isNow(LTL<State> phi) {
		if(isNotAtom(phi)) return null ;
		return ((Now<State>) phi).p ;
	}

	/**
	 * To recognize and destruct "not(now p)". Returns now p.
	 */
	public static <State> Now<State> isNotNow(LTL<State> phi) {
		if (!(phi instanceof Not))
			return null;
		var f = (Not<State>) phi;
		if (isNotAtom(f.phi))
			return null;
		return (Now<State>) arg(phi);
	}
	
	/**
	 * To recognize and destruct "not(phi)" pattern, where phi
	 * is non-atomic. This returns phi.
	 */
	public static <State> LTL<State> isNotPhi(LTL<State> phi) {
		if(! (phi instanceof Not)) return null ;
		var f = (Not<State>) phi ;
		if(isAtom(f.phi)) return null ;
		return arg(phi) ;
	}
	
	/**
	 * Recognize "not(next(psi))". Return "next psi".
	 */
	public static <State> Next<State> isNotNext(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Next)) return null ;
		return (Next<State>) f ;
	}
	
	/**
	 * Recognize "not(not(phi))". Return "not phi".
	 */
	public static <State> Not<State> isNotNot(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Not)) return null ;
		return (Not<State>) f ;
	}
	
	/**
	 * Recognize "not(p && ... && q)" . Returns "p && ... && q".
	 */
	public static <State> And<State> isNotAnd(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof And)) return null ;
		return (And<State>) f ;
	}
	
	/**
	 * Recognize "not(p || ... || q)" . Returns "p || ... || q".
	 */
	public static <State> Or<State> isNotOr(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Or)) return null ;
		return (Or<State>) f ;
	}
	
	/**
	 * Recognize "not(p U q)" . Returns "p U q".
	 */
	public static <State> Until<State> isNotUntil(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof Until)) return null ;
		return (Until<State>) f ;
	}
	
	/**
	 * Recognize "not(p W q)" . Returns "p W q".
	 */
	public static <State> WeakUntil<State> isNotWeakUntil(LTL<State> psi) {
		var f = isNotPhi(psi) ;
		if(f==null || ! (f instanceof WeakUntil)) return null ;
		return (WeakUntil<State>) f ;
	}
	
	/**
	 * Normalize an LTL formula by rewriting it, so that negations
	 * are recursively pushed as far as possible inside.
	 */
	public static <State> LTL<State> pushNegations(LTL<State> f) {
		
		// (1) case atom
		if(isNow(f) != null) return f ;
		
		// (2) cases when we have no outer not, we recurse:
		if (f instanceof Next) {
			var f_ = (Next<State>) f ;
			f_.phi = pushNegations(f_.phi) ;
			return f_ ;
		}
		if (f instanceof And) {
			var f_ = (And<State>) f ;
			for(int k=0; k<f_.conjuncts.length; k++) {
				f_.conjuncts[k] = pushNegations(f_.conjuncts[k]) ;
			}
			return f_ ;
		}
		if (f instanceof Or) {
			var f_ = (Or<State>) f ;
			for(int k=0; k<f_.disjuncts.length; k++) {
				f_.disjuncts[k] = pushNegations(f_.disjuncts[k]) ;
			}
			return f_ ;
		}
		if (f instanceof Until) {
			var f_ = (Until<State>) f ;
			f_.phi1 = pushNegations(f_.phi1) ;
			f_.phi2 = pushNegations(f_.phi2) ;
			return f_ ;
			
		}
		if (f instanceof WeakUntil) {
			var f_ = (WeakUntil<State>) f ;
			f_.phi1 = pushNegations(f_.phi1) ;
			f_.phi2 = pushNegations(f_.phi2) ;
			return f_ ;
		}
		
		// (3) cases when f starts wit a negation
		
		// case not p:
		Now<State> phi_case1 = isNotNow(f) ;
		if(phi_case1 != null) {
			Now<State> f2 = new Now<>() ;
			f2.p = S -> ! phi_case1.p.test(S) ;
			if(phi_case1.name != null) {
				f2.name = "~" + phi_case1.name ;
			}
			else {
				f2.name = "~p" ;
			}
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
			//System.out.println(">>> f:" + f) ;
			//System.out.println(">>> phi_case3:" + phi_case3) ;
			return pushNegations(phi_case3.phi) ; // this returns the phi
		}
		
		// case not(phi && psi)
		And<State> phi_case4 = isNotAnd(f) ;
		if(phi_case4 != null) {
			
			LTL<State>[] dummy = (LTL<State>[]) new LTL[0] ;
			
			var inners = Arrays.asList(phi_case4.conjuncts)
			   .stream()
			   .map((LTL<State> g) -> pushNegations(ltlNot(g)))
			   .collect(Collectors.toList())
			   .toArray(dummy) 
			;
			
			return ltlOr((LTL<State>[]) inners) ;
		}
		
		// case not(phi || psi)
		Or<State> phi_case5 = isNotOr(f) ;
		if(phi_case5 != null) {
					
			LTL<State>[] dummy = (LTL<State>[]) new LTL[0] ;
			
			var inners = Arrays.asList(phi_case5.disjuncts)
				.stream()
				.map((LTL<State> g) -> pushNegations(ltlNot(g)))
				.collect(Collectors.toList())
				.toArray(dummy)
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
	static Pair<Buchi,Integer> getBuchiWorker(int buchiNr, LTL<IExplorableState> phi) {
		throw new UnsupportedOperationException();
	}
	
	static String atomName(LTL ltl) {
		if (!(ltl instanceof Now)) {
			throw new IllegalArgumentException() ;
		}
		var ltl_ = (Now) ltl ;
		return ltl_.toString() ;
	}
	
	static Predicate<IExplorableState> atomPred(LTL<IExplorableState> ltl) {
		if (!(ltl instanceof Now)) {
			throw new IllegalArgumentException() ;
		}
		var ltl_ = (Now<IExplorableState>) ltl ;
		return ltl_.p ;
	}
	
	
	/**
	 * Construct the Buchi of "now p". 
	 */
	static Pair<Buchi,Integer> getBuchiFromNow(int buchiNr, Now<IExplorableState> phi) {
		Buchi B = new Buchi() ;
		String S = "S" + buchiNr ;
		String A = "A" + buchiNr ;
		B.withStates(S,A) 
		.withInitialState(S)
		.withNonOmegaAcceptance(A) ;
		B.withTransition(S,A, atomName(phi), phi.p) ;
		return new Pair<Buchi,Integer> (B, buchiNr+1) ;
	}
	
	/**
	 * Construct the Buchi of "p U psi" where p is a state predicates. 
	 */
	static Pair<Buchi,Integer> getBuchiFromUntil(int buchiNr, Until<IExplorableState> phi) {
		if (!isAtom(phi.phi1)) {
			throw new IllegalArgumentException("Expecting p in p U psi to be an atom") ;
		}
		var rec = getBuchiWorker(buchiNr+1, phi.phi2) ;
		Buchi BF = rec.fst.treeClone() ;
		int nextbuchiNr = rec.snd ;
		
		
		String S = "S" + buchiNr ;
		String A = "A" + buchiNr ;
		BF.insertNewState(S) ;
		BF.insertNewState(A) ;
		
		int oldInitialState = BF.initialState ;
		BF.initialState = BF.states.get(S) ;
		BF.withTransition(S,S,atomName(phi.phi1),atomPred(phi.phi1)) ;
		
		// transitions that branch out from BF's original init:
		var initArrowsOut = BF.transitions.get(oldInitialState) ;
		for(var tr : initArrowsOut) {
			var tr_ = tr.fst ;
			String target = BF.decoder[tr.snd] ;
			BF.withTransition(S, target, tr_.id, tr_.condition) ;
		}
							
		return new Pair<Buchi,Integer>(BF,nextbuchiNr) ;
	}
	
	
	/**
	 * Construct the Buchi of "p U (phi " where p and q are state predicates.
	 */
	static Buchi getBuchiFromSimpleUntil(Until<IExplorableState> phi) {
		if (!isAtom(phi.phi1) || !isAtom(phi.phi2)) {
			throw new IllegalArgumentException("Expecting atoms in p U q") ;
		}
		Buchi B = new Buchi() ;
		B.withStates("S","A") 
		.withInitialState("S")
		.withNonOmegaAcceptance("A") ;
		B.withTransition("S","S",atomName(phi.phi1),atomPred(phi.phi1)) ;
		B.withTransition("S","A",atomName(phi.phi2),atomPred(phi.phi2)) ;
		return B ;
	}
	
	/**
	 * Construct the Buchi of "p W q" where p and q are state predicates.
	 */
	static Buchi getBuchiFromSimpleWeakUntil(WeakUntil<IExplorableState> phi) {
		if (!isAtom(phi.phi1) || !isAtom(phi.phi2)) {
			throw new IllegalArgumentException("Expecting atoms in p W q") ;
		}
		Buchi B = new Buchi() ;
		B.withStates("S","A") 
		.withInitialState("S")
		.withNonOmegaAcceptance("A")
		.withOmegaAcceptance("S") ;
		B.withTransition("S","S",atomName(phi.phi1),atomPred(phi.phi1)) ;
		B.withTransition("S","A",atomName(phi.phi2),atomPred(phi.phi2)) ;
		return B ;
	}

	
	/**
	 * Construct the Buchi of "Xp" where p is a state predicate.
	 */
	static Buchi getBuchiFromSimpleNext(Next<IExplorableState> phi) {
		if (!isAtom(phi.phi)) {
			throw new IllegalArgumentException("Expecting atoms in p W q") ;
		}
		Buchi B = new Buchi() ;
		B.withStates("S","T","A") 
		.withInitialState("S")
		.withNonOmegaAcceptance("A") ;
		B.withTransition("S","T","*",S -> true) 
		 .withTransition("T","A",atomName(phi.phi),atomPred(phi.phi)) ;
		return B ;
	}

}
