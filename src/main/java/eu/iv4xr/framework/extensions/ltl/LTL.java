package eu.iv4xr.framework.extensions.ltl;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import eu.iv4xr.framework.extensions.ltl.LTL.LTLVerdictInfo;
import eu.iv4xr.framework.extensions.ltl.SequencePredicate.SATVerdict;
import nl.uu.cs.aplib.mainConcepts.Environment;

/**
 * Representing an LTL formula. An LTL formula is a predicate over
 * sequences of states. Such a formula can be checked/evaluated whether
 * it holds on a given sequence (whether it is satisfied by the sequence).
 * The evaluation results in either SAT, UNSAT, or UNKNOWN if neither
 * SAT nor UNSAT can be decided.
 * 
 * In this implementation, only checking over finite sequence is implemented.
 * 
 * The checking is done recursively over the structure of the LTL, essentially
 * following the recursive semantics of LTL formulas. We start by evaluating
 * the atomic propositions in the formula over the given sequence, and then 
 * bottom-up towards the root LTL.
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
	    public SequencePredicate.SATVerdict verdict;
	
	    LTLVerdictInfo(SequencePredicate.SATVerdict v) {
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
     * Construct the LTL formula "phi Until psi", where phi is this LTL formula.
     */
    public LTL<State> ltlUntil(LTL<State> psi) {
        var ltl = new Until<State>();
        ltl.phi1 = this;
        ltl.phi2 = psi;
        return ltl;
    }

    public LTL<State> ltlUntil(Predicate<State> psi) {
    	return ltlUntil(now(psi)) ;
    }
    
    public LTL<State> ltlImplies(LTL<State> psi) {
    	return ltlNot(ltlAnd(this, ltlNot(psi))) ;
    }
    
    public static class Now<State> extends LTL<State> {
    	
        Predicate<State> p;
        
        Now() { super() ; }

        @Override
        public void startChecking() {
        	super.startChecking() ;
        }

        @Override
        public SequencePredicate.SATVerdict sat() {
            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            if (p.test(state))
                evals.add(new LTL.LTLVerdictInfo(SequencePredicate.SATVerdict.SAT));
            else
                evals.add(new LTL.LTLVerdictInfo(SequencePredicate.SATVerdict.UNSAT));
        }
    }

    public static class Not<State> extends LTL<State> {
        LTL<State> phi;
        
        Not() { super() ; }

        @Override
        public void startChecking() {
            super.startChecking();
            phi.startChecking();
        }

        @Override
        public SequencePredicate.SATVerdict sat() {
        	
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
                    psi.verdict = SequencePredicate.SATVerdict.UNSAT;
                    break;
                case UNSAT:
                    psi.verdict = SequencePredicate.SATVerdict.SAT;
                    break;
                }
            }

            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SequencePredicate.SATVerdict.UNKNOWN));
            phi.evalAtomSat(state);
        }
    }

    public static class And<State> extends LTL<State> {
        LTL<State>[] conjuncts;
        
        And() { super() ; }

        @Override
        public void startChecking() {
        	super.startChecking() ;
            for (LTL<State> phi : conjuncts)
                phi.startChecking();
        }

        @Override
        public SequencePredicate.SATVerdict sat() {
        	
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
                for (int k = 0; k < N; k++) {
                    var p = conjuntIterators[k].next().verdict;
                    allSat = allSat && (p == SequencePredicate.SATVerdict.SAT);
                }
                if (allSat)
                    psi.verdict = SequencePredicate.SATVerdict.SAT;
                else
                    psi.verdict = SequencePredicate.SATVerdict.UNSAT;
            }
            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SequencePredicate.SATVerdict.UNKNOWN));
            for (LTL<State> phi : conjuncts)
                phi.evalAtomSat(state);
        }
    }

    public static class Until<State> extends LTL<State> {
        LTL<State> phi1;
        LTL<State> phi2;
        
        Until() { super() ; }

        @Override
        public void startChecking() {
        	super.startChecking();
            phi1.startChecking();
            phi2.startChecking();
        }

        @Override
        public SequencePredicate.SATVerdict sat() {
        	
        	if(fullyEvaluated) 
        		return evals.getFirst().verdict;
        
            phi1.sat();
            phi2.sat();
            var iterator = evals.descendingIterator();
            var iteratorPhi1 = phi1.evals.descendingIterator();
            var iteratorPhi2 = phi2.evals.descendingIterator();

            // keep track if phi1 untill phi2 holds at sigma(k+1)
            boolean nextSat = false;

            // calculate phi1 until phi2 holds on every sigma(k); we calculate this
            // backwards for every state in the interval:
            while (iterator.hasNext()) {
                var psi = iterator.next();
                var p = iteratorPhi1.next().verdict;
                var q = iteratorPhi2.next().verdict;
                if (q == SequencePredicate.SATVerdict.SAT) {
                    psi.verdict = SequencePredicate.SATVerdict.SAT;
                    nextSat = true;
                } else {
                    if (nextSat && p == SequencePredicate.SATVerdict.SAT)
                        psi.verdict = SequencePredicate.SATVerdict.SAT;
                    else {
                        psi.verdict = SequencePredicate.SATVerdict.UNSAT;
                        nextSat = false;
                    }
                }
            }
            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SequencePredicate.SATVerdict.UNKNOWN));
            phi1.evalAtomSat(state);
            phi2.evalAtomSat(state);
        }
    }

    public static class Next<State> extends LTL<State> {

        LTL<State> phi;
        
        Next() { super() ; }

        @Override
        public void startChecking() {
            super.startChecking();
            phi.startChecking();
        }

        @Override
        public SequencePredicate.SATVerdict sat() {
        	
        	if(fullyEvaluated) 
        		return evals.getFirst().verdict;
        	
            phi.sat();
            var iterator = evals.descendingIterator();
            var iteratorPhi = phi.evals.descendingIterator();

            var psi = iterator.next();
            psi.verdict = SequencePredicate.SATVerdict.UNSAT; // always unsat at the last state

            // calculate phi1 until phi2 holds on every sigma(k); we calculate this
            // backwards for every state in the interval:
            while (iterator.hasNext()) {
                psi = iterator.next();
                var q = iteratorPhi.next().verdict;
                switch (q) {
                case SAT:
                    psi.verdict = SequencePredicate.SATVerdict.SAT;
                    break;
                case UNSAT:
                    psi.verdict = SequencePredicate.SATVerdict.UNSAT;
                }
            }

            return evals.getFirst().verdict;
        }

        @Override
        void evalAtomSat(State state) {
            evals.add(new LTL.LTLVerdictInfo(SequencePredicate.SATVerdict.UNKNOWN));
            phi.evalAtomSat(state);
        }

    }

   

	public static <State>  LTL<State> now(Predicate<State> p) {
        var a = new Now<State>();
        a.p = p ;
        return a;
    }

    public static <State>  LTL<State> next(LTL<State> phi) {
        var ltl = new Next<State>();
        ltl.phi = phi;
        return ltl;
    }
    
    public static <State>  LTL<State> next(Predicate<State> phi) {
        return next(now(phi)) ;
    }

    public static <State>  LTL<State> ltlNot(LTL<State> phi) {
        var ltl = new Not<State>();
        ltl.phi = phi;
        return ltl;
    }

    public static <State>  LTL<State> ltlAnd(LTL<State>... phis) {
        if (phis == null)
            throw new IllegalArgumentException();
        if (phis.length < 2)
            throw new IllegalArgumentException();
        var ltl = new And<State>();
        ltl.conjuncts = phis;
        return ltl;
    }
    
    public static <State>  LTL<State> ltlOr(LTL<State>... phis) {
    	if (phis == null)
            throw new IllegalArgumentException();
        for(int k=0; k<phis.length; k++) {
        	phis[k] = ltlNot(phis[k]) ;
        }
        return ltlNot(ltlAnd(phis)) ;    	
    }

    public static <State>  LTL<State> eventually(LTL<State> phi) {
        return now((State state) -> true).ltlUntil(phi);
    }
    
    public static <State>  LTL<State> eventually(Predicate<State> phi) {
        return eventually(now(phi)) ;
    }

    public static <State> LTL<State> always(LTL<State> phi) {
        return ltlNot(eventually(ltlNot(phi)));
    }
    
    public static <State> LTL<State> always(Predicate<State> phi) {
        return always(now(phi)) ;
    }

}