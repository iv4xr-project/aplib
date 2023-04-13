package nl.uu.cs.aplib.agents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;

/**
 * Provide a Prolog-engine where you can add facts and rules, and perform
 * queries/inference over those facts.
 * 
 * @author Wish.
 *
 */
public class PrologReasoner {

    public Prolog prolog = new Prolog();

    /**
     * A representation of a predicate-name.
     */
    public static class PredicateName {
        String name;

        PredicateName(String name) {
            this.name = name;
        }

        public String on(Object... args) {
            String s = name + "(";
            for (int k = 0; k < args.length; k++) {
                if (k > 0)
                    s += ",";
                s += args[k].toString();
            }
            s += ")";
            return s;
        }
    }

    /**
     * A represenration of a Prolog-rule. A rule has the form of "h :- t1,t2..."
     * where "h" and "tk" are terms. The term h is called the rule-head and
     * "t1,t2..." is the rule-body.
     * 
     * A rule is interpreted as: h follows from the conjunction of t1,t2...
     */
    public static class Rule {
        String head;
        List<String> body = new LinkedList<String>();

        /**
         * Construct a rule with the specified head and empty body.
         */
        Rule(String head) {
            this.head = head;
        }

        /**
         * Extend this rule h :- body by adding the given term to the body.
         */
        public Rule impBy(String term) {
            body.add(term);
            return this;
        }

        /**
         * Extend this rule h :- body by adding the given term to the body.
         */
        public Rule and(String term) {
            body.add(term);
            return this;
        }

        @Override
        public String toString() {
            String s = head + " :- ";
            int i = 0;
            if (body.isEmpty())
                return s + "true";
            for (String t : body) {
                if (i > 0)
                    s += " , ";
                s += t;
                i++;
            }
            return s;
        }

        Struct toStruct() {
            return (Struct) Term.createTerm(toString());
        }
    }

    /**
     * Construct a representation of a prolog-rule consisting of just the head, and
     * empty body. So, it has the form "head :- ".
     */
    public static Rule rule(String head) {
        return new Rule(head);
    }

    /**
     * Convert a string representing a prolog-term to actually become a Prolog term.
     */
    private static Term term(String s) {
        return Term.createTerm(s);
    }

    private static Theory theory(Struct... structs) throws InvalidTheoryException {
        Struct r = new Struct();
        int k = structs.length;
        while (k > 0) {
            k--;
            r = new Struct(structs[k], r);
        }
        return new Theory(r);
    }

    /**
     * Construct a predicate name, e.g. "P". This can be combined with arguments to
     * construct a string representing a predicate, e.g. as in:
     * 
     * predicate("P").on("X","Y")
     */
    public static PredicateName predicate(String name) {
        return new PredicateName(name);
    }

    /**
     * Construct a string representing the prolog-term "not(t)".
     */
    public static String not(String t) {
        return "not(" + t + ")";
    }

    /**
     * Construct a string representing the prolog-term "(t1; t2; ...)", which
     * denotes the disjunction of the composing terms.
     */
    public static String or(String... args) {
        String s = "(";
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                s += " ; ";
            s += args[i];
        }
        s += ")";
        return s;
    }

    /**
     * Construct a string representing the prolog-term "(t1,t2,...)"; it denotes the
     * conjunction of the composing terms.
     */
    public static String and(String... args) {
        String s = "(";
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                s += " , ";
            s += args[i];
        }
        s += ")";
        return s;
    }

    /**
     * Add new facts to the Prolog engine.
     */
    public PrologReasoner facts(String... facts) throws InvalidTheoryException {
        for (int k = 0; k < facts.length; k++) {
            Struct f = rule(facts[k]).toStruct();
            prolog.getTheoryManager().assertA(f, true, "", true);
        }
        return this;
    }

    /**
     * Remove facts from the Prolog engine.
     */
    public PrologReasoner removeFacts(String... facts) throws InvalidTheoryException {
        for (int k = 0; k < facts.length; k++) {
            Struct f = rule(facts[k]).toStruct();
            prolog.getTheoryManager().retract(f);
        }
        return this;
    }

    /**
     * Add new rules to the Prolog engine.
     */
    public PrologReasoner add(Rule... rules) throws InvalidTheoryException {
        Struct[] rules_ = new Struct[rules.length];
        for (int k = 0; k < rules.length; k++) {
            Struct f = rules[k].toStruct();
            prolog.getTheoryManager().assertA(f, true, "", true);
        }
        // prolog.addTheory(theory(rules_));
        return this;
    }

    /**
     * Representing the result of a Prolog-query.
     */
    public static class QueryResult {
        public SolveInfo info;

        public QueryResult(SolveInfo info) {
            this.info = info;
        }

        /**
         * Extract the integer-value of the given variable from this query-result.
         */
        public Integer int_(String varname) {
            try {
                Term t = info.getVarValue(varname);
                return ((alice.tuprolog.Int) t).intValue();
            } catch (NoSolutionException e) {
                return null;
            }
        }

        /**
         * Extract the string-value of the given variable from this query-result.
         */
        public String str_(String varname) {
            try {
                Term t = info.getVarValue(varname);
                if (t.isAtom()) {
                	return ((Struct) t).getName();
                }
                else return t.toString() ;
                //if (!t.isAtom())
                //    throw new IllegalArgumentException();
                
            } catch (NoSolutionException e) {
                return null;
            }
        }

    }

    /**
     * Pose a query to the prolog-engine.
     */
    public QueryResult query(String queryterm) {
        SolveInfo info = prolog.solve(term(queryterm));
        // System.out.println("## " + q + " --> " + info) ;
        if (!info.isSuccess())
            return null;
        return new QueryResult(info);
    }
    
    public List<QueryResult> queryAll(String queryterm) {
    	List<QueryResult> results = new LinkedList<>() ;
    	QueryResult r = query(queryterm) ;
    	if (r==null) return results ;
    	results.add(r) ;
    	while(true) {
    		try {
    			SolveInfo info = prolog.solveNext() ;
    			if(info.isSuccess()) {
        			results.add(new QueryResult(info)) ;
        		}
        		else {
        			break ;
        		}
    		}
    		catch(Exception e) {
    			break ;
    		}
    	}
    	return results ;    	
    }

    /**
     * Check if the given query would return a result. If it does, this method
     * returns true, and else false.
     */
    public boolean test(String queryterm) {
        try {
            SolveInfo info = prolog.solve(term(queryterm));
            return info.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public static int intval(Term t) {
        return ((alice.tuprolog.Int) t).intValue();
    }

    public static String stringval(Term t) {
        if (!t.isAtom())
            throw new IllegalArgumentException();
        return ((Struct) t).getName();
    }

    public String showTheory() {
        return prolog.getTheory().toString();
    }

    /**
     * Save the current set of facts and rules to a file, which can be re-loaded
     * later.
     */
    public void saveTheory(String filename) throws IOException {
        Path path = Paths.get(filename);
        byte[] strToBytes = showTheory().getBytes();
        Files.write(path, strToBytes);
    }

    /**
     * Clear the current theory, and replace it with a theory loaded form the
     * specified file.
     */
    public void loadTheory(String filename) throws IOException, InvalidTheoryException {
        Path path = Paths.get(filename);
        String s = Files.readString(path);
        prolog.getTheoryManager().clear();
        prolog.addTheory(new Theory(s));
    }

    // just for testing
    public static void main(String[] args) throws InvalidTheoryException, IOException {
        PrologReasoner prolog = new PrologReasoner();

        var pirate = predicate("pirate");
        var sailor = predicate("sailor");

        // adding facts and a rule:
        prolog.facts(pirate.on("jack"), pirate.on("davy"));
        prolog.add(rule(sailor.on("X")).impBy(pirate.on("X")));
        System.out.println(prolog.showTheory());
        // test some queries:
        System.out.println("## testing some queries...");
        System.out.println("** pirate : " + prolog.query(pirate.on("X")).str_("X"));
        System.out.println("** sailor : " + prolog.query(sailor.on("X")).str_("X"));
        // test removing facts:
        System.out.println("## testing removing fact...");
        prolog.removeFacts(pirate.on("davy"));
        System.out.println(prolog.showTheory());
        System.out.println("** sailor = " + prolog.query(sailor.on("X")).str_("X"));

        System.out.println("## testing saving and reloading facts and rules ...");
        prolog.facts(pirate.on("jack"), pirate.on("davy"));
        prolog.saveTheory("blatheory.txt");

        prolog.prolog.getTheoryManager().clear();

        prolog.loadTheory("blatheory.txt");
        System.out.println(prolog.showTheory());
        prolog.removeFacts(pirate.on("davy"));
        System.out.println(prolog.showTheory());

    }

}
