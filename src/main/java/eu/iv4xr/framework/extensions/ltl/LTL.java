package eu.iv4xr.framework.extensions.ltl;

import static eu.iv4xr.framework.extensions.ltl.LTL.ltlNot;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Provides a representation LTL formulas and a DSL for constructing them. An
 * LTL formula is a predicate over sequences of states.
 * 
 * 
 * <p>
 * Additionally methods are provided to evaluate an LTL formula over a finite
 * sequence of states (evaluating over an infinite sequence is obviously
 * impossible). Evaluating an LTL formula on a sequence sigma means checking
 * whether the formula holds on sigma (whether sigma satisfies the formula). The
 * evaluation results in either SAT, UNSAT, or UNKNOWN if neither SAT nor UNSAT
 * can be decided.
 * 
 * <p>
 * The checking is done recursively over the structure of the LTL, essentially
 * following the recursive 'semantics' of LTL formulas. We start by evaluating
 * the atomic propositions in the formula over the given sequence, and then
 * bottom-up towards the root LTL. The semantics used is mostly as usual, see
 * e.g. Baier's Principles of Model Checking. However, we define the meaning of
 * Xp on [s] (a sequence that contains of only one state) is always UNSAT
 * (even for Xtrue), because the sequence has no next-state. A subtle consequence
 * of this is the property not(X phi), when interpreted over finite sequences,
 * is not equivalent to X(not phi) (which they are, when interpreted on infinite 
 * sequences).
 * 
 * <p>For steps needed to evaluate an LTL formula on a sequence, see the documentation
 * of {@link SequencePredicate}. This class LTL extends SequencePredicate.
 * 
 * @author Wish
 */
public abstract class LTL<State> extends SequencePredicate<State> {

    /**
     * If sigma is the sequence of states that is under evaluation. Calling sat()
     * will check/evaluates. This evaluation is recursive over the structure of
     * the LTL formula. To facilitate this, it is necessary to also store intermediate
     * results. 
     * This field evals is a sequence, such that evals(i) is the value of this LTL
     * (satisfied or not satisfied) formula, evaluated on the suffix of sigma, starting at 
     * sigma(i).
     * 
     * (this implies that validity of this LTL on the whole sigma can be obtained
     * by looking at the value of evals(0)) 
     */
    LinkedList<LTLVerdictInfo> evals = new LinkedList<>();
    
    // Just a wrapper over vedict, to allow the value inside to be conveniently
    // updated while being in the evals-list above:
    static class LTLVerdictInfo {
	    public SATVerdict verdict;
	
	    LTLVerdictInfo(SATVerdict v) {
	        verdict = v;
	    }
	}
    
    /**
     * When the true then this formula is considered to have been fully evaluated. 
     * Invoking sat() will not trigger new-re-evaluation.
     */
    public boolean fullyEvaluated = false ; 

    LTL() {
    }
    

    /**
     * Evaluate the atoms of this LTL formula on the given current state, and
     * add the verdict at the end of {@link #evals} of those atoms.
     */
    abstract void evalAtomSat(State state);
    
    /**
     * Invoke this first before start checking this LTL formula on an execution; this
     * will rest the internal state of this formula, making it ready to check a new 
     * sequence. 
     */
    @Override
    public void startChecking() {
    	evals.clear();
    	fullyEvaluated = false ;
    }
    
	/**
     * Use this to check this LTL formula on a sequence of states by feeding the 
     * states one state at a time. The typical setup is if the execution under evaluation 
     * does not allow states to be serialized or cloned. This means that we cannot collect
     * those states in some collection and therefore the execution has to be checked
     * incrementally by feeding this predicate one state (the current state) at a time.
     */
    @Override
    public void checkNext(State state) {
    	evalAtomSat(state) ;
    }
    
    /**
     * Call this to mark that the last state of the execution under evaluation has
     * been fed to this predicate. So, we can now call sat() to inspect whether this
     * predicate holds on the execution or not.
     */
    @Override
    public void endChecking() {
    	sat() ;
    	fullyEvaluated = true ;
    }
    
    /**
     * Make a clone of this LTL formula. The structure will be cloned, but the under
     * lying state predicates are not cloned.
     */
    abstract public LTL<State> treeClone() ;
         

    /**
     * phi.Until(psi) constructs the LTL formula "phi U psi".
     */
    public Until<State> until(LTL<State> psi) {
        var ltl = new Until<State>();
        ltl.phi1 = this.treeClone();
        ltl.phi2 = psi.treeClone();
        return ltl;
    }

    /**
     * phi.weakUntil(psi) constructs the LTL formula "phi W psi".
     */
    public WeakUntil<State> weakUntil(LTL<State> psi) {
        var ltl = new WeakUntil<State>();
        ltl.phi1 = this.treeClone() ;
        ltl.phi2 = psi.treeClone() ;
        return ltl ;
    }

    public LTL<State> until(Predicate<State> psi) {
    	return until(now(psi)) ;
    }
    
    /**
     * phi.implies(psi) constructs the LTL formula "phi --> psi".
     */
    public LTL<State> implies(LTL<State> psi) {
    	return ltlNot(ltlAnd(this, ltlNot(psi))) ;
    }
    
    
    public static class Now<State> extends LTL<State> {
    	
        public Predicate<State> p;
        public String name = null ;
        
        Now() { super() ; }

        @Override
        public SATVerdict sat() {
            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            if (p.test(state))
                evals.add(new LTL.LTLVerdictInfo(SATVerdict.SAT));
            else
                evals.add(new LTL.LTLVerdictInfo(SATVerdict.UNSAT));
        }
        
        @Override 
        public String toString() { 
        	if (name==null) return "p" ; 
        	return name ;
        }
        
        @Override
        public Now<State> treeClone() {
        	var clone = new Now<State>() ;
        	clone.p = this.p ;
        	if(this.name != null) clone.name = "" + this.name ;
        	return clone ;        	
        }
    }

    public static class Not<State> extends LTL<State> {
    	public LTL<State> phi;
        
        Not() { super() ; }

        @Override
        public void startChecking() {
            super.startChecking();
            phi.startChecking();
        }

        @Override
        public SATVerdict sat() {
        	
        	if(fullyEvaluated) 
        		return evals.getFirst().verdict;
        	
            phi.sat();
            var iterator = evals.descendingIterator();
            var iteratorPhi = phi.evals.descendingIterator();

            while (iterator.hasNext()) {
                var psi = iterator.next();
                var p = iteratorPhi.next().verdict;
                switch (p) {
                case SAT:
                    psi.verdict = SATVerdict.UNSAT;
                    break;
                case UNSAT:
                    psi.verdict = SATVerdict.SAT;
                    break;
                }
            }

            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SATVerdict.UNKNOWN));
            phi.evalAtomSat(state);
        }
        
        @Override
        public Not<State> treeClone() {
        	var clone = new Not<State>() ;
        	clone.phi = this.phi.treeClone() ;
        	return clone ;        	
        }
        
        @Override 
        public String toString() { return "~(" + phi + ")" ; }
    }

    public static class And<State> extends LTL<State> {
        
    	public LTL<State>[] conjuncts;
        
        And() { super() ; }

        @Override
        public void startChecking() {
        	super.startChecking() ;
            for (LTL<State> phi : conjuncts)
                phi.startChecking();
        }

        @Override
        public SATVerdict sat() {
        	
        	if(fullyEvaluated) 
        		return evals.getFirst().verdict;
        	
            for (LTL<State> phi : conjuncts)
                phi.sat();
            var iterator = evals.descendingIterator();
            var N = conjuncts.length;
            Iterator<LTL.LTLVerdictInfo>[] conjuntIterators = new Iterator[N];
            for (int k = 0; k < N; k++)
                conjuntIterators[k] = conjuncts[k].evals.descendingIterator();

            while (iterator.hasNext()) {
                var psi = iterator.next();
                boolean allSat = true;
                boolean someUnsat = false ;
                for (int k = 0; k < N; k++) {
                    var p = conjuntIterators[k].next().verdict;
                    allSat = allSat && (p == SATVerdict.SAT);
                    someUnsat = someUnsat || (p == SATVerdict.UNSAT) ;
                }
                if (allSat)
                     psi.verdict = SATVerdict.SAT;
                else if (someUnsat)
                	 psi.verdict = SATVerdict.UNSAT ;
                else psi.verdict = SATVerdict.UNKNOWN ;
            }
            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SATVerdict.UNKNOWN));
            for (LTL<State> phi : conjuncts)
                phi.evalAtomSat(state);
        }
        
        @Override
        public And<State> treeClone() {
        	var clone = new And<State>() ;
        			
            LTL<State>[] dummy = (LTL<State>[]) new LTL[0] ;
			
            clone.conjuncts = Arrays.asList(this.conjuncts)
			   .stream()
			   .map((LTL<State> g) -> g.treeClone())
			   .collect(Collectors.toList())
			   .toArray(dummy) 
			;		
        	return clone ;        	
        }
        
        @Override 
        public String toString() { 
        	String z = "" ;
        	int k=0 ;
        	for(var f : conjuncts) {
        		if(k>0) {
        			z += " && " ;
        		}
        		z += "(" + f + ")" ;
        		k++ ;
        	}
        	return z ; 
        }
    }
    
    /**
     * For representing "phi || psi". We could define this as
     * a derived operator "not(not phi && not psi)", but for
     * e.g. translation to Buchi we need to be able to structurally
     * identify the "phi || psi" pattern. So we add this explicit
     * representation.
     */
    public static class Or<State> extends LTL<State> {
    	
        public LTL<State>[] disjuncts;
        
        Or() { super() ; }

        @Override
        public void startChecking() {
        	super.startChecking() ;
            for (LTL<State> phi : disjuncts)
                phi.startChecking();
        }

        @Override
        public SATVerdict sat() {
        	
        	if(fullyEvaluated) 
        		return evals.getFirst().verdict;
        	
            for (LTL<State> phi : disjuncts)
                phi.sat();
            var iterator = evals.descendingIterator();
            var N = disjuncts.length;
            Iterator<LTL.LTLVerdictInfo>[] disjunctIterators = new Iterator[N];
            for (int k = 0; k < N; k++)
            	disjunctIterators[k] = disjuncts[k].evals.descendingIterator();

            while (iterator.hasNext()) {
                var psi = iterator.next();
                boolean someSat = false;
                boolean allUnsat = true ;
                for (int k = 0; k < N; k++) {
                    var p = disjunctIterators[k].next().verdict;
                    someSat = someSat || (p == SATVerdict.SAT) ;
                    allUnsat = allUnsat && (p == SATVerdict.UNSAT) ;
                }
                if (someSat)
                    psi.verdict = SATVerdict.SAT;
                else if (allUnsat)
                	psi.verdict = SATVerdict.UNSAT;
                else 
                    psi.verdict = SATVerdict.UNKNOWN ;
            }
            return evals.getFirst().verdict;
        }

		@Override
		void evalAtomSat(State state) {
			evals.add(new LTL.LTLVerdictInfo(SATVerdict.UNKNOWN));
            for (LTL<State> phi : disjuncts)
                phi.evalAtomSat(state);
		}
		
        @Override
        public Or<State> treeClone() {
        	var clone = new Or<State>() ;
        			
            LTL<State>[] dummy = (LTL<State>[]) new LTL[0] ;
			
            clone.disjuncts = Arrays.asList(this.disjuncts)
			   .stream()
			   .map((LTL<State> g) -> g.treeClone())
			   .collect(Collectors.toList())
			   .toArray(dummy) 
			;		
        	return clone ;        	
        }
		
		@Override 
        public String toString() { 
        	String z = "" ;
        	int k=0 ;
        	for(var f : disjuncts) {
        		if(k>0) {
        			z += " || " ;
        		}
        		z += "(" + f + ")" ;
        		k++ ;
        	}
        	return z ; 
        }
    }

    public static class Until<State> extends LTL<State> {
    	public LTL<State> phi1;
    	public LTL<State> phi2;
        
        Until() { super() ; }

        @Override
        public void startChecking() {
        	super.startChecking();
            phi1.startChecking();
            phi2.startChecking();
        }

        @Override
        public SATVerdict sat() {
        	
        	if(fullyEvaluated) 
        		return evals.getFirst().verdict;
        
            phi1.sat();
            phi2.sat();
            var iterator = evals.descendingIterator();
            var iteratorPhi1 = phi1.evals.descendingIterator();
            var iteratorPhi2 = phi2.evals.descendingIterator();

            // keep track if phi1 until phi2 holds at sigma(k+1)
            boolean nextSat = false;

            // calculate phi1 until phi2 holds on every sigma(k); we calculate this
            // backwards for every state in the interval:
            while (iterator.hasNext()) {
                var psi = iterator.next();
                var p = iteratorPhi1.next().verdict;
                var q = iteratorPhi2.next().verdict;
                if (q == SATVerdict.SAT) {
                    psi.verdict = SATVerdict.SAT;
                    nextSat = true;
                } else {
                    if (nextSat && p == SATVerdict.SAT)
                        psi.verdict = SATVerdict.SAT;
                    else {
                        psi.verdict = SATVerdict.UNSAT;
                        nextSat = false;
                    }
                }
            }
            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SATVerdict.UNKNOWN));
            phi1.evalAtomSat(state);
            phi2.evalAtomSat(state);
        }
        
        @Override
        public Until<State> treeClone() {
        	var clone = new Until<State>() ;
        			
           clone.phi1 = this.phi1.treeClone() ;
           clone.phi2 = this.phi2.treeClone() ;
        	return clone ;        	
        }
        
        @Override 
        public String toString() { 
        	return "(" + phi1 + ") U (" + phi2 + ")" ; 
        }
    }
    
    /**
     * Weak-until is not primitive in LTL-sense, but we add it as a separate class
     * so that we can structurally recognize that a formula is a weak-until formula.
     */
    public static class WeakUntil<State> extends LTL<State> {
        
    	public LTL<State> phi1 ;
    	public LTL<State> phi2 ;
        
        WeakUntil() { super() ; }

        @Override
        public void startChecking() {
        	super.startChecking();
        	phi1.startChecking();
        	phi2.startChecking();
        }

        @Override
        public SATVerdict sat() {
        	
        	if(fullyEvaluated) 
        		return evals.getFirst().verdict;
        	
        	/*
        	 does not work :|  want to know why...
        	 
        	LTL<State> encoding = ltlOr(always(phi1), phi1.until(phi2)) ;
      
            encoding.sat();
            var iterator = evals.descendingIterator();
            var iteratorEncoding = encoding.evals.descendingIterator();

            // just copy the evaluation of the encoding:
            while (iterator.hasNext()) {
                var f = iterator.next();
                f.verdict = iteratorEncoding.next().verdict;
            }
            */
        	phi1.sat();
            phi2.sat();
        	var iterator = evals.descendingIterator();
            var iteratorPhi1 = phi1.evals.descendingIterator();
            var iteratorPhi2 = phi2.evals.descendingIterator();

            // keep track if phi1 until phi2 holds at sigma(k+1)
            boolean nextSat = false;

            // (1) calculate phi1 until phi2 holds on every sigma(k); we calculate this
            // backwards for every state in the interval:
            while (iterator.hasNext()) {
                var psi = iterator.next();
                var p = iteratorPhi1.next().verdict;
                var q = iteratorPhi2.next().verdict;
                if (q == SATVerdict.SAT) {
                    psi.verdict = SATVerdict.SAT;
                    nextSat = true;
                } else {
                    if (nextSat && p == SATVerdict.SAT)
                        psi.verdict = SATVerdict.SAT;
                    else {
                        psi.verdict = SATVerdict.UNSAT;
                        nextSat = false;
                    }
                }
            }
            
            // (2) combine with always(phi1):   
            iterator = evals.descendingIterator();
            iteratorPhi1 = phi1.evals.descendingIterator();
            while (iterator.hasNext()) {
            	var psi = iterator.next();
            	var p = iteratorPhi1.next().verdict; 
            	if (p == SATVerdict.SAT) {
            		psi.verdict = SATVerdict.SAT ;
            	}
            	else {
            		// else this i and its predecesors won't satisfy always(phi1)
            		// we break:
            		break ;
            	}
            }  

            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SATVerdict.UNKNOWN));
            phi1.evalAtomSat(state);
            phi2.evalAtomSat(state);
        }
        
        @Override
        public WeakUntil<State> treeClone() {
        	var clone = new WeakUntil<State>() ;
        			
           clone.phi1 = this.phi1.treeClone() ;
           clone.phi2 = this.phi2.treeClone() ;
        	return clone ;        	
        }
        
        @Override 
        public String toString() { 
        	return "(" + phi1 + ") W (" + phi2 + ")" ; 
        }
    }

    public static class Next<State> extends LTL<State> {

    	public LTL<State> phi;
        
        Next() { super() ; }

        @Override
        public void startChecking() {
            super.startChecking();
            phi.startChecking();
        }

        @Override
        public SATVerdict sat() {
        	
        	if(fullyEvaluated) 
        		return evals.getFirst().verdict;
        	
            phi.sat();
            var iterator = evals.descendingIterator();
            var iteratorPhi = phi.evals.descendingIterator();

            var psi = iterator.next();
            psi.verdict = SATVerdict.UNSAT; // always unsat at the last state

            // calculate phi1 until phi2 holds on every sigma(k); we calculate this
            // backwards for every state in the interval:
            while (iterator.hasNext()) {
                psi = iterator.next();
                var q = iteratorPhi.next().verdict;
                switch (q) {
                case SAT:
                    psi.verdict = SATVerdict.SAT;
                    break;
                case UNSAT:
                    psi.verdict = SATVerdict.UNSAT;
                }
            }

            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SATVerdict.UNKNOWN));
            phi.evalAtomSat(state);
        }
        
        @Override
        public Next<State> treeClone() {
           var clone = new Next<State>() ;       			
           clone.phi = this.phi.treeClone() ;
           return clone ;        	
        }
        
        @Override 
        public String toString() { 
        	return "X(" + phi + ")" ; 
        }
    }

   
    /**
     * If p is a state-predicate, this construct the LTL formula "now(p)".
     */
	public static <State>  Now<State> now(Predicate<State> p) {
        var a = new Now<State>();
        a.p = p ;
        return a;
    }
	
	public static <State>  Now<State> now(String name, Predicate<State> p) {
        var a = now(p) ;
        a.name = name ;
        return a ;
    }

	/**
	 * If p is a state-predicate, next(p) constructs the LTL formula "X now(p)".
	 */
    public static <State>  Next<State> next(LTL<State> phi) {
        var ltl = new Next<State>();
        ltl.phi = phi.treeClone();
        return ltl;
    }
    
    /**
     * next(phi) constructs the LTL formula "X phi".
     */
    public static <State>  Next<State> next(Predicate<State> phi) {
        return next(now(phi)) ;
    }

    /**
     * ltlNot(phi) constructs the LTL formula "not phi".
     */
    public static <State>  Not<State> ltlNot(LTL<State> phi) {
        var ltl = new Not<State>();
        ltl.phi = phi.treeClone();
        return ltl;
    }

    /**
     * ltlAnd(phi1, ..., phin ) constructs the LTL formula "phi1 && phi2 ... && phin".
     */
    public static <State>  And<State> ltlAnd(LTL<State>... phis) {
        if (phis == null)
            throw new IllegalArgumentException();
        if (phis.length < 2)
            throw new IllegalArgumentException();
        var ltl = new And<State>();
        
        LTL<State>[] dummy = (LTL<State>[]) new LTL[0] ;
		
		ltl.conjuncts = Arrays.asList(phis)
		   .stream()
		   .map((LTL<State> g) -> g.treeClone())
		   .collect(Collectors.toList())
		   .toArray(dummy) 
		;
        return ltl;
    }
    
    /**
     * ltlOr(phi1, ..., phin ) constructs the LTL formula "phi1 || phi2 ... || phin".
     */
    public static <State>  Or<State> ltlOr(LTL<State>... phis) {
    	if (phis == null)
            throw new IllegalArgumentException();
    	if (phis.length < 2)
            throw new IllegalArgumentException();
    	var ltl = new Or<State>();
    	
        LTL<State>[] dummy = (LTL<State>[]) new LTL[0] ;
		
		ltl.disjuncts = Arrays.asList(phis)
		   .stream()
		   .map((LTL<State> g) -> g.treeClone())
		   .collect(Collectors.toList())
		   .toArray(dummy) 
		;
        return ltl ;   	
    }

    /**
     * evenatually(phi) constructs the LTL formula "F phi" (also written "diamond phi").
     */
    public static <State>  Until<State> eventually(LTL<State> phi) {
        return now("true",(State state) -> true).until(phi.treeClone());
    }
    
    /**
     * if p is a state-predicate, evenatually(p) constructs the LTL formula 
     * "F noq(p)" (also written "diamond now(p)").
     */
    public static <State>  Until<State> eventually(Predicate<State> phi) {
        return eventually(now(phi)) ;
    }

    /**
     * always(phi) constructs the LTL formula "G phi" (also written "[] phi").
     */
    public static <State> LTL<State> always(LTL<State> phi) {
        return ltlNot(eventually(ltlNot(phi)));
    }
    
    /**
     * If p is a state-predicate, always(phi) constructs the LTL formula 
     * "G now(p)" (also written "[] now(p)").
     */
    public static <State> LTL<State> always(Predicate<State> phi) {
        return always(now(phi)) ;
    }

}