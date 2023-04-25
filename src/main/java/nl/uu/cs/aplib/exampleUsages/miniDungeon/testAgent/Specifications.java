package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import eu.iv4xr.framework.extensions.ltl.LTL;
import static eu.iv4xr.framework.extensions.ltl.LTL.* ;

import java.util.function.Predicate;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

public class Specifications {
	MyAgentState cast(SimpleState S) {
		return (MyAgentState) S;
	}
	
	/*
	 * Scenario based specifications
	 * */
	
	/*
	 * There is a heal pot which is used 
	 * */
	public LTL<SimpleState> scenarioSpec1() {
		Predicate<SimpleState> p1 = (S -> (int) ((MyAgentState) S).worldmodel().val("healpotsInBag") < (int) ((MyAgentState) S).worldmodel().before("healpotsInBag") ) ;
		var ps1  = eventually(p1);
		Predicate<SimpleState> p2 = (S -> (int) ((MyAgentState) S).worldmodel().val("healpotsInBag") < (int) ((MyAgentState) S).worldmodel().before("healpotsInBag") ) ;
		var ps2  = eventually(p2);
		Predicate<SimpleState> p3  =  (S -> (int) ((MyAgentState) S).worldmodel().val("hp") > (int) ((MyAgentState) S).worldmodel().before("hp") ) ;
		var ps3  = eventually(p3);
		var ps4 = ltlAnd(ps2,ps3);
		return  ps1.implies(ps4);
	}
	
	
	
	/*
	 * There is a rage pot which is used ???
	 */
	public LTL<SimpleState> scenarioSpec2() {
		Predicate<SimpleState> p1 = (S -> (int) ((MyAgentState) S).worldmodel().val("ragepotsInBag") > 0 ) ;
		var ps1  = eventually(p1);
		Predicate<SimpleState> p2 = (S -> (int) ((MyAgentState) S).worldmodel().val("ragepotsInBag") < (int) ((MyAgentState) S).worldmodel().before("ragepotsInBag") ) ;
		var ps2  = eventually(p2);
	    ///return ps1;
		return  ps1.implies(ps2);
	}
	
	
	/*
	 * There is a scroll which is used 
	 */
	public LTL<SimpleState> scenarioSpec3() {
		Predicate<SimpleState> p1 = (S -> (int) ((MyAgentState) S).worldmodel().val("scrollsInBag") > 0 ) ;
		var ps1  = eventually(p1);
		Predicate<SimpleState> p2 = (S -> (int) ((MyAgentState) S).worldmodel().val("scrollsInBag") < (int) ((MyAgentState) S).worldmodel().before("scrollsInBag") ) ;
		var ps2  = eventually(p2);
	    //return ps1;
		return  ps1.implies(ps2);
	}
	
	
	
	
	
	
	
	/*
	 * General Specifications
	 * */
	
	/*hp bigger than zero*/
	public LTL<SimpleState> spec1() {
		Predicate<SimpleState> p1 = (S -> (int) ((MyAgentState) S).worldmodel().val("hp") >= 0 ) ;
		return always(p1) ;
	}
	
	
	public LTL<SimpleState> spec2() {
		Predicate<SimpleState> p2 = (S -> (int) cast(S).worldmodel().val("Frodo","hp") < 20 ) ;
		return eventually(p2) ;
	}
	
	/*hp value not bigger than hpmax*/	
	public LTL<SimpleState> spec3() {
		Predicate<SimpleState> p3 = (S -> (int) cast(S).worldmodel().val("Frodo","hp") < (int) cast(S).worldmodel().val("Frodo","hpmax") ) ;
		return always(p3) ;
	}

	/*bag size*/
	public LTL<SimpleState> spec4() {
		Predicate<SimpleState> p4 = (S -> (int) cast(S).worldmodel().val("Frodo","bagUsed") <= (int) cast(S).worldmodel().val("Frodo","maxBagSize") ) ;
		return always(p4) ;
	}
	
	public LTL<SimpleState> spec5() {
		Predicate<SimpleState> p1 = (S -> (int) ((MyAgentState) S).worldmodel().val("healpotsInBag") < (int) ((MyAgentState) S).worldmodel().before("healpotsInBag") ) ;
		var ps1  = eventually(p1) ;
		Predicate<SimpleState> p2 = (S -> (int) ((MyAgentState) S).worldmodel().val("hp") ==  (int) ((MyAgentState) S).worldmodel().before("hp")) ;
		var ps2 = eventually(p2);
		return ps1.implies(ps2);
	}
	
	
	
	//Second Agent
	public LTL<SimpleState> spec40() {
		Predicate<SimpleState> p40 = (S -> (int) cast(S).worldmodel().val("Smeagol","hp") <  30) ;
		return eventually(p40) ;
	}
}
