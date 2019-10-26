package nl.uu.cs.aplib.Agents;

import java.util.LinkedList;
import java.util.List;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;


public class StateWithProlog extends StateWithMessenger {
	
	Prolog prolog = new Prolog() ;
	
	public StateWithProlog() { super() ; }
	
	
	public static class AplibPrologClause {
		String head ;
		List<String> body = new LinkedList<String>() ;
		AplibPrologClause(String head) { this.head = head ; }
		
		public AplibPrologClause IMPby(String term) {
			body.add(term) ; return this ;
		}
		
		public AplibPrologClause and(String term) {
			body.add(term) ; return this ;
		}
		
		@Override
		public String toString() {
			String s = head  + " :- " ;
			int i = 0 ;
			for (String t : body) {
				if (i>0) s += " , " ;
				s += t ;
				i++ ;
			}
			return s ;
		}
	}
	
	static public AplibPrologClause clause(String head) {
		return new AplibPrologClause(head) ;
	}
	
	
	static public String not(String t) { return "not(" + t + ")" ; }
	static public String or(String ... args) {
		String s = "(" ;
		for (int i=0; i<args.length; i++) {
			if (i>0) s += " ; " ;
			s += args[i] ;
		}
		s += ")" ;
		return s ;
	}
	
	static public String and(String ... args) {
		String s = "(" ;
		for (int i=0; i<args.length; i++) {
			if (i>0) s += " , " ;
			s += args[i] ;
		}
		s += ")" ;
		return s ;
	}
	
	static public String mkPredString(String name, String ... args) {
		name += "(" ;
		for (int i=0; i<args.length; i++) {
		    if (i>0) name += "," ;
		    name += args[i] ;
		}
		name += ")" ; return name ;
	}
	
	static Term term(String s) {
		return Term.createTerm(s) ;
	}
	
	static Struct fact(String s) {
		return (Struct) Term.createTerm(s + " :- true" ) ;
	}
	
	static Struct rule(String s) {
		return (Struct) Term.createTerm(s) ;
	}
	
	static Theory theory(Struct ... structs) throws InvalidTheoryException {
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
		for (int k=0; k<rules.length; k++) rules_[k] = rule(rules[k]) ;
		prolog.addTheory(theory(rules_));
		return this ;	
	}
	
	public StateWithProlog addRules(AplibPrologClause ... rules) throws InvalidTheoryException {
		String[] rules_ = new String[rules.length] ;
		for (int k=0; k<rules.length; k++) rules_[k] = rules[k].toString() ;
		addRules(rules_) ;
		return this ;
	}
	
	public static class QueryResult {
		SolveInfo info ;
		public QueryResult(SolveInfo info) { this.info = info ; }
		
		public Integer int_(String varname) {
			try {
				Term t = info.getVarValue(varname) ;
				return ((alice.tuprolog.Int) t).intValue() ; 
			}
			catch(NoSolutionException e) {
				return null ;
			}			
		}
		
		public String str_(String varname) {
			try {
				Term t = info.getVarValue(varname) ;
				if (!t.isAtom()) throw new IllegalArgumentException() ;
				return ((Struct) t).getName() ;		
			}
			catch(NoSolutionException e) {
				return null ;
			}	
		}
		
	}
	
	public QueryResult query(String queryterm) {
		SolveInfo info = prolog.solve(term(queryterm));
		//System.out.println("## " + q + " --> " + info) ;
		if (! info.isSuccess()) return null ;
		return new QueryResult(info) ;
	}
	
	public boolean test(String queryterm) {
		try {
			SolveInfo info = prolog.solve(term(queryterm));
			return info.isSuccess() ;
		}
		catch(Exception e) { return false ; }
	}
	
	public String showTheory() {
		return prolog.getTheory().toString() ;
	}
	
	static public int intval(Term t) {
		return ((alice.tuprolog.Int) t).intValue() ;
	}
	
	static public String stringval(Term t) {
		if (!t.isAtom()) throw new IllegalArgumentException() ;
		return ((Struct) t).getName() ;
	}

}
