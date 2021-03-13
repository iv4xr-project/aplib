package eu.iv4xr.framework.extensions.ltl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Predicate;

import eu.iv4xr.framework.extensions.ltl.BoundedLTL.LTLVerdict;
import eu.iv4xr.framework.extensions.ltl.BoundedLTL.LTLVerdictInfo;
import nl.uu.cs.aplib.mainConcepts.Environment;

/**
 * Representing an LTL formula.
 */
public abstract class LTL {

    /**
     * Abstractly representing the finite execution on which this LTL is
     * interpreted.
     */
    LinkedList<LTLVerdictInfo> absexecution = new LinkedList<>();

    LTL() {
    }

    abstract void resettracking();

    /**
     * Check if {@link #absexecution} satisfies this LTL formula.
     */
    abstract LTLVerdict sat();

    /**
     * Evaluate the atoms of this LTL formula on the given env (concrete state), and
     * add the verdict at the end of {@link #absexecution}.
     */
    abstract void evalAtomSat(Environment env);

    /**
     * Construct the LTL formula "phi Until psi", where phi is this LTL formula.
     */
    public LTL ltlUntil(LTL psi) {
        var ltl = new Until();
        ltl.phi1 = this;
        ltl.phi2 = psi;
        return ltl;
    }

    public static class Atom extends LTL {
        Predicate<Environment> p;

        void check(Environment env) {

        }

        @Override
        void resettracking() {
            absexecution.clear();
        }

        @Override
        LTLVerdict sat() {
            return absexecution.getFirst().verdict;
        }

        @Override
        void evalAtomSat(Environment env) {
            if (p.test(env))
                absexecution.add(new LTLVerdictInfo(LTLVerdict.SAT));
            else
                absexecution.add(new LTLVerdictInfo(LTLVerdict.UNSAT));
        }
    }

    public static class Not extends LTL {
        LTL phi;

        @Override
        void resettracking() {
            absexecution.clear();
            phi.resettracking();
        }

        @Override
        LTLVerdict sat() {
            phi.sat();
            var iterator = absexecution.descendingIterator();
            var iteratorPhi = phi.absexecution.descendingIterator();

            while (iterator.hasNext()) {
                var psi = iterator.next();
                var p = iteratorPhi.next().verdict;
                switch (p) {
                case SAT:
                    psi.verdict = LTLVerdict.UNSAT;
                    break;
                case UNSAT:
                    psi.verdict = LTLVerdict.SAT;
                    break;
                }
            }

            return absexecution.getFirst().verdict;
        }

        @Override
        void evalAtomSat(Environment env) {
            absexecution.add(new LTLVerdictInfo(LTLVerdict.UNKNOWN));
            phi.evalAtomSat(env);
        }
    }

    public static class And extends LTL {
        LTL[] conjuncts;

        @Override
        void resettracking() {
            absexecution.clear();
            for (LTL phi : conjuncts)
                phi.resettracking();
        }

        @Override
        LTLVerdict sat() {
            for (LTL phi : conjuncts)
                phi.sat();
            var iterator = absexecution.descendingIterator();
            var N = conjuncts.length;
            Iterator<LTLVerdictInfo>[] conjuntIterators = new Iterator[N];
            for (int k = 0; k < N; k++)
                conjuntIterators[k] = conjuncts[k].absexecution.descendingIterator();

            while (iterator.hasNext()) {
                var psi = iterator.next();
                boolean allSat = true;
                for (int k = 0; k < N; k++) {
                    var p = conjuntIterators[k].next().verdict;
                    allSat = allSat && (p == LTLVerdict.SAT);
                }
                if (allSat)
                    psi.verdict = LTLVerdict.SAT;
                else
                    psi.verdict = LTLVerdict.UNSAT;
            }
            return absexecution.getFirst().verdict;
        }

        @Override
        void evalAtomSat(Environment env) {
            absexecution.add(new LTLVerdictInfo(LTLVerdict.UNKNOWN));
            for (LTL phi : conjuncts)
                phi.evalAtomSat(env);
        }
    }

    public static class Until extends LTL {
        LTL phi1;
        LTL phi2;

        @Override
        void resettracking() {
            absexecution.clear();
            phi1.resettracking();
            phi2.resettracking();
        }

        @Override
        LTLVerdict sat() {
            phi1.sat();
            phi2.sat();
            var iterator = absexecution.descendingIterator();
            var iteratorPhi1 = phi1.absexecution.descendingIterator();
            var iteratorPhi2 = phi2.absexecution.descendingIterator();

            // keep track if phi1 untill phi2 holds at sigma(k+1)
            boolean nextSat = false;

            // calculate phi1 until phi2 holds on every sigma(k); we calculate this
            // backwards for every state in the interval:
            while (iterator.hasNext()) {
                var psi = iterator.next();
                var p = iteratorPhi1.next().verdict;
                var q = iteratorPhi2.next().verdict;
                if (q == LTLVerdict.SAT) {
                    psi.verdict = LTLVerdict.SAT;
                    nextSat = true;
                } else {
                    if (nextSat && p == LTLVerdict.SAT)
                        psi.verdict = LTLVerdict.SAT;
                    else {
                        psi.verdict = LTLVerdict.UNSAT;
                        nextSat = false;
                    }
                }
            }
            return absexecution.getFirst().verdict;
        }

        @Override
        void evalAtomSat(Environment env) {
            absexecution.add(new LTLVerdictInfo(LTLVerdict.UNKNOWN));
            phi1.evalAtomSat(env);
            phi2.evalAtomSat(env);
        }
    }

    public static class Next extends LTL {

        LTL phi;

        @Override
        void resettracking() {
            absexecution.clear();
            phi.resettracking();
        }

        @Override
        LTLVerdict sat() {
            phi.sat();
            var iterator = absexecution.descendingIterator();
            var iteratorPhi = phi.absexecution.descendingIterator();

            var psi = iterator.next();
            psi.verdict = LTLVerdict.UNSAT; // always unsat at the last state

            // calculate phi1 until phi2 holds on every sigma(k); we calculate this
            // backwards for every state in the interval:
            while (iterator.hasNext()) {
                psi = iterator.next();
                var q = iteratorPhi.next().verdict;
                switch (q) {
                case SAT:
                    psi.verdict = LTLVerdict.SAT;
                    break;
                case UNSAT:
                    psi.verdict = LTLVerdict.UNSAT;
                }
            }

            return absexecution.getFirst().verdict;
        }

        @Override
        void evalAtomSat(Environment env) {
            absexecution.add(new LTLVerdictInfo(LTLVerdict.UNKNOWN));
            phi.evalAtomSat(env);
        }

    }

    public static <E> LTL now(Predicate<E> p) {
        var a = new Atom();
        a.p = env -> p.test((E) env);
        return a;
    }

    public static LTL next(LTL phi) {
        var ltl = new Next();
        ltl.phi = phi;
        return ltl;
    }

    public static LTL ltlNot(LTL phi) {
        var ltl = new Not();
        ltl.phi = phi;
        return ltl;
    }

    public static LTL ltlAnd(LTL... phis) {
        if (phis == null)
            throw new IllegalArgumentException();
        if (phis.length < 2)
            throw new IllegalArgumentException();
        var ltl = new And();
        ltl.conjuncts = phis;
        return ltl;
    }

    public static LTL eventually(LTL phi) {
        return now((Environment env) -> true).ltlUntil(phi);
    }

    public static LTL always(LTL phi) {
        return ltlNot(eventually(ltlNot(phi)));
    }

}