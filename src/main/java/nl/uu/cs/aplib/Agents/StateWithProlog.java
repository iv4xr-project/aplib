package nl.uu.cs.aplib.Agents;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;

public class StateWithProlog extends StateWithMessanger {
	
	Prolog prolog = new Prolog() ;
	
	public StateWithProlog() { super() ; }
	
	static public Term term(String s) {
		return Term.createTerm(s) ;
	}
	
	static public Struct fact(String s) {
		return (Struct) Term.createTerm(s + " :- true" ) ;
	}
	
	static public Struct rule(String s) {
		return (Struct) Term.createTerm(s) ;
	}
	
	static public Theory theory(Struct ... structs) throws InvalidTheoryException {
		Struct r = new Struct() ;
		int k = structs.length ;
		while (k>0) {
			k-- ;
			r = new Struct(structs[k],r) ;
		}
		return new Theory(r) ;
	}
	
	public StateWithProlog addFacts(String ... facts) throws InvalidTheoryException {
		Struct[] facts_ = new Struct[facts.length] ;
		for (int k=0; k<facts.length; k++) facts_[k] = fact(facts[k]) ;
		prolog.addTheory(theory(facts_));
		return this ;	
	}
	
	public StateWithProlog addRules(String ... rules) throws InvalidTheoryException {
		Struct[] rules_ = new Struct[rules.length] ;
		for (int k=0; k<rules.length; k++) rules_[k] = fact(rules[k]) ;
		prolog.addTheory(theory(rules_));
		return this ;	
	}
	
	public Term[] query(String queryterm, String ... vars) throws NoSolutionException {
		SolveInfo info = prolog.solve(term(queryterm));
		if (! info.isSuccess()) return null ;
		Term[] solutions = new Term[vars.length] ;
		for (int k=0; k<vars.length; k++) {
			solutions[k] = info.getVarValue(vars[k]) ;
		}
		return solutions ;
	}
	
	static public int intval(Term t) {
		return ((alice.tuprolog.Int) t).intValue() ;
	}
	
	static public String stringval(Term t) {
		if (!t.isAtom()) throw new IllegalArgumentException() ;
		return ((Struct) t).getName() ;
	}

}
