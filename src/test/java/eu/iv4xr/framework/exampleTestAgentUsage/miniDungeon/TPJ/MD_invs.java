package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import java.util.function.Predicate;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

public class MD_invs {
	
	boolean hpInv(MyAgentState S) {
		int hp = (Integer) S.val("hp") ;
		int hpmax = (Integer) S.val("hpmax") ;
		return hp>=0 && hp <= hpmax && hpmax>0 ;
	}
	
	boolean scoreInv(MyAgentState S) {
		int score = (Integer) S.val("score") ;
		Integer prev = (Integer) S.before("score") ;
		return prev!=null ? score >= prev : true ;		
	}
	
	Predicate<SimpleState>[] wrap_(Predicate<SimpleState> ... predicates) {
		return predicates ;
	}

	@SuppressWarnings("unchecked")
	Predicate<SimpleState>[] allInvs = wrap_(
			S -> hpInv((MyAgentState) S),
			S -> scoreInv((MyAgentState) S)
	) ;
	
	Predicate<SimpleState>[] selectedInvs(int ... selections) {
		Predicate<SimpleState>[] chosen = new Predicate[selections.length] ;
		for (int k=0; k<chosen.length; k++)
			chosen[k] = allInvs[selections[k]] ;
		return chosen ;
 	}

}
