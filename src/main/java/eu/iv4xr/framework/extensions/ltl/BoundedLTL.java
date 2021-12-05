package eu.iv4xr.framework.extensions.ltl;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import static eu.iv4xr.framework.extensions.ltl.LTL.ltlNot; 

import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.Environment.EnvironmentInstrumenter;
import nl.uu.cs.aplib.utils.Pair;

/**
 * Bounded LTL is a variation of LTL. So, a BLTL formula is also a sequence predicate.
 * In this particular implementation, the sequence consists of pairs (tr,s) where
 * s represents a state, and tr represents the transition that was done out in
 * the system under evaluation, that results in the state s.
 *  
 * A BLTL formula F can be thought a tuple (phi,p,q,n), where phi is an LTL formula,
 * p,q are state-predicates, and n (if provided) is a natural number. To construct
 * F we do:
 * 
 *    var F = new BoundedLTL()
 *      .thereIs(phi)
 *      .when(p)
 *      .until(q)
 *      .withMaxLength(n) ; 
 * 
 * F is interpreted over a (finite) sequence. 
 * Let sigma be a finite sequence. The predicates p and q induce a set of disjoint
 * segments of sigma, such every segment Z: (1) starts with a state satisfying p,
 * (2) Z ends with a state satisfying q (this can coincide with the starting state),
 * (3) the end-state of Z is the only state satisfying q,
 * (3b) if n is given, then additionally Z length should be at most n.
 * (4) Z is maximal in length while maintaining properties 1..3 above.
 * 
 * Note that conditions above imply that the segments cannot overlap. Also note
 * that p can holds multiple times within Z, but Z ends at with a q. If p occurs
 * multiple times, Z is maximal in the sense that it starts with the first p before
 * q holds.
 * 
 * F holds on the sequence (F is satisfied by the sequence) if the sequence contains 
 * such a pq-segment Z such that this Z satisfies ltl. The segment Z can be seen 
 * as the witness of this satisfaction.
 * 
 * Note that the interpretation of F is indeed existensial (it is satisfied if 
 * there is a pq-segment in the sequence that satisfies F's ltl). But we can
 * also check a universal interpretation. To checks whether ALL pq-segment satisfies 
 * an ltl-formula phi, we instead check the BLTL (p,q, ltlNot(phi),n)).
 * If this results in UNSAT, the all pq-segments in the target sequence satisfies
 * phi (and else, there is one segment that does not satisfy phi).
 * 
 * @author Wish
 */
public class BoundedLTL extends SequencePredicate<Pair<ITransition,IState>> {

    LTL<IState> ltl;
    Predicate<IState> startf;
    Predicate<IState> endf;
    Integer maxlength = null;

    /**
     * Internal state for tracking the formula-state.
     */
    enum BLTLstate {
        NOTSTARTED, STARTED, SATFOUND
    }

    BLTLstate bltlState = BLTLstate.NOTSTARTED;
    WitnessTrace trace = new WitnessTrace();

    public <State> BoundedLTL() {
    }

    public BoundedLTL thereIs(LTL<IState> F) {
        ltl = F;
        return this;
    }

    public BoundedLTL when(Predicate<IState> p) {
        startf = p;
        return this;
    }

    public BoundedLTL until(Predicate<IState> q) {
        endf = q;
        return this;
    }

    
    public BoundedLTL withMaxLength(int n) {
        if (n < 1)
            throw new IllegalArgumentException();
        maxlength = n;
        return this;
    }


    /**
     * Represent a 'trace' of an execution. An execution is a sequence of steps and the
     * states that result from each state. A 'trace' is not literally a sequence of these
     * states, as it might be expensive or not possible to 'remember' states passed during 
     * the execution. But rather it only keeps some string 'description' or 'summary' of
     * the states.
     * 
     * To be more precise, a witness-trace is a sequence of pairs (tr,s) where tr is the
     * id of a step/transition, and s is a string description of the state after tr.
     */
    public static class WitnessTrace {
        /**
         * A list of "transition". Every transition is represented by a pair (tr,s) where
         * tr is the id of a transition, and s is a string-dec of the resulting state.
         * 
         * The transition tr might be null, if it is not known, or if s represents an 
         * initial state (so, it has no transition that precedes it).
         */
        List<Pair<String,String>> trace = new LinkedList<>();

        public void register(ITransition transitionLabel, IState resultingState) {
            trace.add(new Pair<String,String>(transitionLabel.getId(),resultingState.showState())) ;
        }

        public void reset() {
            trace.clear();
        }

        public String getState(int k) {
            if (k < 0 || k >= trace.size())
                throw new IllegalArgumentException();
            return trace.get(k).snd ;
        }

        public String getTransitionLabel(int k) {
            if (k < 0 || k >= trace.size())
                throw new IllegalArgumentException();
            return trace.get(k).snd ;
        }

        @Override
        public String toString() {
            int k = 0;
            String s = "";
            for (var tr : trace) {
                if (k > 0)
                    s += "\n";
                s += k + ": action:" + tr.fst + " --> state:" + tr.snd + ">";
                k++;
            }
            return s;
        }

    }
    
    /**
     * Invoke this first before start checking this predicate on a sequence; this
     * will reset the state of this bounded LTL checker to its initial internal state.
     */
    @Override
    public void startChecking() {
        bltlState = BLTLstate.NOTSTARTED;
        trace.reset();
        ltl.startChecking();
    }


	/**
     * Use this to check this BLTL predicate on a sequence of pairs transition-state by 
     * feeding it one item at a time. The typical setup is if the execution under evaluation 
     * does not allow states to be serialized or cloned. This means that we cannot collect
     * those states in some collection and therefore the execution has to be checked
     * incrementally by feeding this predicate one item (the current transition-state pair) 
     * at a time.
     */
    @Override
    public void checkNext(Pair<ITransition,IState> step) {
    	ITransition tr = step.fst ;
    	IState state = step.snd ;
        switch (bltlState) {
        case SATFOUND:
            return ;
        case NOTSTARTED:
            if (startf.test(state)) {
                trace.register(tr,state);
                ltl.startChecking();
                ltl.checkNext(state);
                bltlState = BLTLstate.STARTED;
                if (endf.test(state)) {
                    // the interval ends immediately
                	ltl.endChecking();
                    var verdict = ltl.sat();
                    if (verdict == SATVerdict.SAT) {
                        bltlState = BLTLstate.SATFOUND;
                    } else {
                        trace.reset();
                        bltlState = BLTLstate.NOTSTARTED;
                    }
                    return ;
                }
            }
            return ;
        case STARTED:
            // pass the env to the ltl to have its atoms evaluated:
            trace.register(tr, state);
            ltl.checkNext(state);

            if (endf.test(state)) {
                // end marker holds; then force full evaluation of the ltl
            	ltl.endChecking();
                var verdict = ltl.sat();
                if (verdict == SATVerdict.SAT) {
                    bltlState = BLTLstate.SATFOUND;
                } else {
                    trace.reset();
                    bltlState = BLTLstate.NOTSTARTED;
                }
                return ;
            } else {
                if (maxlength != null && ltl.evals.size() >= maxlength) {
                    // maximum interval length is reached, since the end-marker
                    // is not seen yet, we stop the evaluation:
                    bltlState = BLTLstate.NOTSTARTED;
                    trace.reset();
                }
            }
        }
    }
    
    /**
     * Call this to mark that the last state of the execution under evaluation has
     * been fed to this predicate. So, we can now call sat() to inspect whether this
     * predicate holds on the execution or not.
     */
    @Override
    public void endChecking() {
    	// nothing special need to be done
    }
    
    /**
     * This checks this BLTL formula on the given execution (list of pairs transition-state).
     * The execution is assumed to be complete (that is, we don't wait for more states
     * to extend it). 
     * 
     * If the execution satisfy this BLTL, this method will return SAT, and the 
     * witnessing trace can be obtained. Otherwise it returns UNSAT.
     * 
     * The witness is a trace that satisfies this BLTL's interval/segment specification,
     * and on which (on this interval/segment) the LTL part of this BLTL holds.
     * The witness can be obtained through the method getWitness().
     */
    @Override
    public SATVerdict sat(List<Pair<ITransition,IState>> sequence) {
    	this.startChecking();
    	for(var step : sequence) {
    		checkNext(step) ;
    		if(bltlState == BLTLstate.SATFOUND) break ;
    	}	
        return sat() ;
    }
    
    /**
     * Return the verdict of the last sat-checking. If it was SAT, then this method returns
     * SAT as well. Else, it returns UNSAT (even if the previous SAT-check returned UNKNOWN;
     * so essentially this method assumes that the last state of the execution under evaluation
     * has been seen).
     */
    @Override
    public SATVerdict sat() {
        if (bltlState == BLTLstate.SATFOUND)
            return SATVerdict.SAT;
        return SATVerdict.UNSAT;
    }

    /**
     * Return the witnessing trace, if the last sat-checking results in a SAT. Else it returns
     * null.
     */
    public WitnessTrace getWitness() {
        if (bltlState == BLTLstate.SATFOUND)
            return trace;
        return null;
    }

}
