package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import eu.iv4xr.framework.extensions.ltl.LTL;
import static eu.iv4xr.framework.extensions.ltl.LTL.* ;

import java.util.function.Predicate;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

public class SomeLTLSpecifications {
	
	MyAgentState cast(SimpleState S) {
		return (MyAgentState) S;
	}
	
	LTL<SimpleState> spec1() {
		Predicate<SimpleState> p1 = (S -> (int) ((MyAgentState) S).worldmodel().val("hp") >= 0 ) ;
		return always(p1) ;
	}
	
	LTL<SimpleState> spec2() {
		Predicate<SimpleState> p2 = (S -> (int) cast(S).worldmodel().val("Frodo","hp") < 20 ) ;
		return eventually(p2) ;
	}
	
	LTL<SimpleState> spec3() {
		Predicate<SimpleState> p3 = (S -> (int) cast(S).worldmodel().val("Smeagol","hp") < 30 ) ;
		return eventually(p3) ;
	}

}
