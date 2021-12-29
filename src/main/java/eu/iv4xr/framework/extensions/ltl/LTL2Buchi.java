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
	public static Buchi getBuchi(LTL<IExplorableState> phi) {
		return getBuchiWorker(0,phi).fst ;
	}
	
	/**
	 * The worker function to translate the given LTL formula to a Buchi automaton.
	 * The translation builds the Buchi recursively. Since we will then need to
	 * generate unique states, the variable buchiNr is used to keep track the number
	 * of the next sub-Buchi to generate. This number is unique, and is used as part
	 * of the ids of the states and transitions inside the sub-Buchi.
	 * 
	 * The worker returns a pair (B,i) where B is the resulting Buchi, and i is the
	 * next unique number of buchiNr.
	 */
	static Pair<Buchi,Integer> getBuchiWorker(int buchiNr, LTL<IExplorableState> phi) {
		if (phi instanceof Now) {
			return getBuchiFromNow(buchiNr, (Now<IExplorableState>) phi) ;
		}
		if (phi instanceof Next) {
			return getBuchiFromNext(buchiNr, (Next<IExplorableState>) phi) ;
		}
		if (phi instanceof Until) {
			return getBuchiFromUntil(buchiNr, (Until<IExplorableState>) phi) ;
		}
		if (phi instanceof WeakUntil) {
			return getBuchiFromWeakUntil(buchiNr, (WeakUntil<IExplorableState>) phi) ;
		}
		if (phi instanceof Or) {
			return getBuchiFromOr(buchiNr, (Or<IExplorableState>) phi) ;
		}
		if (phi instanceof And) {
			throw new IllegalArgumentException("Coverting f && g to Buchi is not supported yet.") ;
		}
		if (phi instanceof Not) {
			throw new IllegalArgumentException("Cannot convert ~f to Buchi. Remove negations by rewriting the formula first.") ;
		}
		// should not reach this point
		throw new IllegalArgumentException() ;
	}
	
	static String atomName(LTL ltl) {
		if (!(ltl instanceof Now)) {
			throw new IllegalArgumentException() ;
		}
		var ltl_ = (Now) ltl ;
		return ltl_.toString() ;
	}
	
	static <State> Predicate<State> atomPred(LTL<State> ltl) {
		if (!(ltl instanceof Now)) {
			throw new IllegalArgumentException() ;
		}
		var ltl_ = (Now<State>) ltl ;
		return ltl_.p ;
	}
	
	
	/**
	 * Construct the Buchi of "now p". 
	 */
	static <State> Pair<Buchi,Integer> getBuchiFromNow(int buchiNr, Now<IExplorableState> phi) {
		Buchi B = new Buchi() ;
		String S = "S" + buchiNr ;
		String A = "A" + buchiNr ;
		B.withStates(S,A) 
		.withInitialState(S)
		.withNonOmegaAcceptance(A) ;
		String tid = "t_" + S + "_" + A ;
		B.withTransition(S,A, tid, atomName(phi), phi.p) ;
		return new Pair<> (B, buchiNr+1) ;
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
		var t0Id = "t_" + S + "_" + S ;
		BF.withTransition(S,S,t0Id,atomName(phi.phi1),atomPred(phi.phi1)) ;
		
		// transitions that branch out from BF's original init:
		var initArrowsOut = BF.transitions.get(oldInitialState) ;
		int tid = 1 ;
		for(var tr : initArrowsOut) {
			var tr_ = tr.fst ;
			String target = BF.decoder[tr.snd] ;
			String tid_ = "t" + buchiNr + "_" + tid ;
			BF.withTransition(S, target, tid_, tr_.name , tr_.condition) ;
			tid++ ;
		}
							
		return new Pair<>(BF,nextbuchiNr) ;
	}
	
	/**
	 * Construct the Buchi of "p W psi" where p is a state predicates. 
	 */
	static Pair<Buchi,Integer> getBuchiFromWeakUntil(int buchiNr, WeakUntil<IExplorableState> phi) {
		if (!isAtom(phi.phi1)) {
			throw new IllegalArgumentException("Expecting p in p W psi to be an atom") ;
		}
		
		// We will first construct a Buchi of p U psi, then add its initial state as an 
		// omega-accepting state.
		
		Until<IExplorableState> untilVariant = (Until<IExplorableState>) phi.phi1.until(phi.phi2) ;
		
		Pair<Buchi,Integer> rec = getBuchiFromUntil(buchiNr, untilVariant) ;
		
		Buchi BF = rec.fst ;
		
		int S0 = BF.initialState ;
		// turn BF to a Buchi of weak-until by making S0 omega-accepting:
		BF.omegaAcceptingStates.add(S0) ;
		
		return new Pair<>(BF,rec.snd) ;	
	}
		
	/**
	 * Construct the Buchi of "X phi".
	 */
	static Pair<Buchi,Integer> getBuchiFromNext(int buchiNr, Next<IExplorableState> phi) {
		
		var rec = getBuchiWorker(buchiNr+1, phi.phi) ;
		Buchi BF = rec.fst.treeClone() ;
		int nextbuchiNr = rec.snd ;
		
		String S = "S" + buchiNr ;
		BF.insertNewState(S) ;
		
		int oldInitialState = BF.initialState ;
		String oldInitialStateName = BF.decoder[oldInitialState] ;
		BF.initialState = BF.states.get(S) ;
		var tId = "t_" + S + "_" +  oldInitialStateName ;
		BF.withTransition(S,oldInitialStateName,tId,"true", state -> true) ;

		return new Pair<>(BF,nextbuchiNr) ;
	}
	
	/**
	 * Construct a Buchi B which represents B1 || B2 (disjunction). It should accept
	 * any execution that can be accepted by either B1 or B2. States and transitions'
	 * ids of B1 and B2 are assumed to be disjoint. 
	 */
	private static Buchi union(Buchi B1, Buchi B2) {
		
		Buchi B2_ = B2.treeClone() ;
		
		for(int k=B1.decoder.length-1 ; 0<=k; k--) {
			String stateName = B1.decoder[k] ;
			B2_.insertNewState(stateName) ;
		}
				
		String B1initState = B1.decoder[B1.initialState] ;	
		B2_.initialState = B2_.states.get(B1initState) ;
		
		for(var f : B1.traditionalAcceptingStates) {
			String fname = B1.decoder[f] ;
			int f_newindex = B2_.states.get(fname) ;
			B2_.traditionalAcceptingStates.add(f_newindex) ;
		}
		
		for(var f : B1.omegaAcceptingStates) {
			String fname = B1.decoder[f] ;
			int f_newindex = B2_.states.get(fname) ;
			B2_.omegaAcceptingStates.add(f_newindex) ;
		}
		
		var B1_ = B1.treeClone() ;
		for (var tr : B1_.transitions.entrySet()) {
			B2_.transitions.put(tr.getKey(), tr.getValue()) ;
		}
		
		String B2initState = B2.decoder[B2.initialState] ;
		int B2initState_newIndex = B2_.states.get(B2initState) ;
		
		var B2initArrowsOut = B2_.transitions.get(B2initState_newIndex) ;
		// need some number to assign to the ids of the new transitions...
		int tid = B1.transitions.get(B1.initialState).size() ;
		for(var tr : B2initArrowsOut) {
			var tr_ = tr.fst ;
			String target = B2_.decoder[tr.snd] ;
			String tid_ = "t_" + B1initState + "_" + tid ;
			B2_.withTransition(B1initState, target, tid_, tr_.name , tr_.condition) ;
			tid++ ;
		}	
		return B2_ ;
	}
	
	static Pair<Buchi,Integer> getBuchiFromOr(int buchiNr, Or<IExplorableState> phi) {
		if (phi.disjuncts == null || phi.disjuncts.length == 0) {
			throw new IllegalArgumentException("Or(..) but it contains no disjunct.") ;
		}
		if (phi.disjuncts.length == 1) {
			return getBuchiWorker(buchiNr,phi.disjuncts[0]) ;
		}
		// case where we have 2 or more disjuncts:
		
		var rec = getBuchiWorker(buchiNr, phi.disjuncts[0]) ;
		Buchi BF0 = rec.fst.treeClone() ;
		int nextbuchiNr = rec.snd ;		
		for (int k=1; k < phi.disjuncts.length; k++) {
			rec = getBuchiWorker(nextbuchiNr, phi.disjuncts[k]) ;
			Buchi BF1 = rec.fst.treeClone() ;
			nextbuchiNr = rec.snd ;
			BF0 = union(BF0,BF1) ;
		}
		
		return new Pair<>(BF0,nextbuchiNr) ;
	}
	
	private static <State> LTL<State> andRewrite(LTL<State> f) {
		throw new UnsupportedOperationException() ;
	}
	
	private static <State> LTL<State> andRewrite(Now<State> f1, Now<State> f2) {
		String name = "(" + f1.name + ") && (" + f2.name + ")" ;
		return now(name, S -> f1.p.test(S) && f2.p.test(S)) ;
	}
	
	private static <State> LTL<State> andRewrite(LTL<State> f1, Or<State> f2) {
		Or<State> g = new Or<>() ;
		g.disjuncts = new LTL[f2.disjuncts.length] ;
		for(int k=0; k < f2.disjuncts.length; k++) {
			g.disjuncts[k] = andRewrite(ltlAnd(f1,f2.disjuncts[k])) ;
		}
		return g ;
	}
	
	private static <State> LTL<State> andRewrite(Now<State> f1, Until<State> f2) {
		LTL<State> g1 = andRewrite(ltlAnd(f1,f2.phi2)) ;
		LTL<State> g2 = andRewrite(ltlAnd(f1,f2.phi1,next(f2))) ;		
		return ltlOr(g1,g2) ;
	}
	
	private static <State> LTL<State> andRewrite(Now<State> f1, WeakUntil<State> f2) {
		LTL<State> g1 = andRewrite(ltlAnd(f1,f2.phi2)) ;
		LTL<State> g2 = andRewrite(ltlAnd(f1,f2.phi1,next(f2))) ;		
		return ltlOr(g1,g2) ;
	}
	
	private static <State> LTL<State> andRewrite(Next<State> f1, Next<State> f2) {
		LTL<State> g = andRewrite(ltlAnd(f1.phi,f2.phi)) ;
		return next(g) ;
	}
	
	private static <State> LTL<State> andRewrite(Next<State> f1, Until<State> f2) {
		LTL<State> g1 = andRewrite(ltlAnd(f1,f2.phi2)) ;
		LTL<State> g2 = andRewrite(ltlAnd(f2.phi1, next(ltlAnd(f1.phi, f2)))) ;		
		return ltlOr(g1,g2) ;
	}
	
	private static <State> LTL<State> andRewrite(Next<State> f1, WeakUntil<State> f2) {
		LTL<State> g1 = andRewrite(ltlAnd(f1,f2.phi2)) ;
		LTL<State> g2 = andRewrite(ltlAnd(f2.phi1, next(ltlAnd(f1.phi, f2)))) ;		
		return ltlOr(g1,g2) ;
	}
	
	private static <State> LTL<State> andRewrite(Until<State> f1, Until<State> f2) {
		LTL<State> a = f1.phi1 ;
		LTL<State> b = f1.phi2 ;
		LTL<State> p = f2.phi1 ;
		LTL<State> q = f2.phi2 ;
		LTL<State> g1 = andRewrite(ltlAnd(a,p).until(ltlAnd(b,f2))) ;
		LTL<State> g2 = andRewrite(ltlAnd(a,p).until(ltlAnd(q,f1))) ;
		return ltlOr(g1,g2) ;
	}
	
	private static <State> LTL<State> andRewrite(Until<State> f1, WeakUntil<State> f2) {
		LTL<State> a = f1.phi1 ;
		LTL<State> b = f1.phi2 ;
		LTL<State> p = f2.phi1 ;
		LTL<State> q = f2.phi2 ;
		// note that both middle operators should be UNTIL:
		LTL<State> g1 = andRewrite(ltlAnd(a,p).until(ltlAnd(b,f2))) ;
		LTL<State> g2 = andRewrite(ltlAnd(a,p).until(ltlAnd(q,f1))) ;
		return ltlOr(g1,g2) ;
	}
	
	private static <State> LTL<State> andRewrite(WeakUntil<State> f1, WeakUntil<State> f2) {
		LTL<State> a = f1.phi1 ;
		LTL<State> b = f1.phi2 ;
		LTL<State> p = f2.phi1 ;
		LTL<State> q = f2.phi2 ;
		LTL<State> g1 = andRewrite(ltlAnd(a,p).weakUntil(ltlAnd(b,f2))) ;
		LTL<State> g2 = andRewrite(ltlAnd(a,p).weakUntil(ltlAnd(q,f1))) ;
		return ltlOr(g1,g2) ;
	}

}
