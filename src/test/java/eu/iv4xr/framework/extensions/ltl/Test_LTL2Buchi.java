package eu.iv4xr.framework.extensions.ltl;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static eu.iv4xr.framework.extensions.ltl.LTL.* ;
import static eu.iv4xr.framework.extensions.ltl.LTL2Buchi.* ;
import static eu.iv4xr.framework.extensions.ltl.Test_LTL.* ;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Test_LTL2Buchi {
	
	void test_pushNeg(LTL<Integer> phi, List<Integer> ... seqs) {
        System.out.println("------") ;
		System.out.println(">>  phi: " + phi) ;
		var phi__ = pushNegations(phi) ;
		System.out.println(">>  pushneq(phi): " + phi__) ;
		for(int k=0; k<seqs.length; k++) {
			boolean pass = phi.sat(seqs[k]) == phi__.sat(seqs[k]) ;
			System.out.println(">>  seq-" + k + " passed:" + pass) ;
			assertTrue(pass) ;
		}
	}
	
	@Test
	public void test_pushNeg() {
		
		Predicate<Integer> p0 = x -> x==0 ;
		Predicate<Integer> p3 = x -> x==3 ;
		Predicate<Integer> p4 = x -> x==4 ;

		Predicate<Integer> p_1_3 = x -> 1<=x && x<=3 ;
		
		var seq1 = sequence(0,1,2,3,4) ;
		var seq2 = sequence(1,2,3,4) ;
		var seq3 = sequence(3,4) ;
		var seq4 = sequence(1,2,3,5) ;
		
		var phi = ltlNot(now(p0)) ;
		test_pushNeg(phi,seq1,seq2) ;
		phi = ltlNot(ltlOr(now(p0),now(p4))) ;
		test_pushNeg(phi,seq1,seq2) ;
			
		phi = ltlNot(ltlAnd(now(p3), ltlNot(ltlOr(now(p0),now(p4))))) ;
		test_pushNeg(phi,seq1,seq2,seq3) ;
		
		phi = ltlNot(ltlAnd(now(p3), ltlOr(now(p0),now(p4)))) ;
		test_pushNeg(phi,seq1,seq2,seq3) ;
		
		phi = ltlNot(ltlOr(now(p3), ltlNot(ltlAnd(now(p0),now(p4))))) ;
		test_pushNeg(phi,seq1,seq2,seq3) ;

		phi = ltlNot(ltlOr(now(p3), ltlAnd(now(p0),now(p4)))) ;
		test_pushNeg(phi,seq1,seq2,seq3) ;
		
		phi = ltlNot(now(p_1_3).until(now(p4))) ;
		test_pushNeg(phi,seq1,seq2,seq3,seq4) ;
		
		phi = ltlNot(now(p_1_3).weakUntil(now(p4))) ;
		test_pushNeg(phi,seq1,seq2,seq3,seq4) ;
		
		/*
		var phi1 = pushNegations(phi);
		
		var phi2 = ltlAnd(now(p_1_3),ltlNot(now(p4)))
				.weakUntil(ltlAnd(ltlNot(now(p_1_3)), ltlNot(now(p4)))) ;
		
		var phi2a = ltlAnd(now(p_1_3),ltlNot(now(p4)))
				.until(ltlAnd(ltlNot(now(p_1_3)), ltlNot(now(p4)))) ;
		
		var phi2b = always(ltlAnd(now(p_1_3),ltlNot(now(p4)))) ;
		
		
		var phi3a = ltlAnd(now(p_1_3), ltlNot(now(p4))) ;
		var phi3b = ltlAnd(ltlNot(now(p_1_3)), ltlNot(now(p4))) ;
		
		evalAndPrint("phi",phi,seq1) ;
		evalAndPrint("phi1",phi1,seq1) ;
		evalAndPrint("phi2",phi2,seq1) ;
		//evalAndPrint("phi2a",phi2a,seq4) ;
		 */

		
			
	}
	
	void evalAndPrint(String name, LTL<Integer> phi, List<Integer> seq) {
		System.out.println(">>> " + name + ": " + phi.sat(seq)) ;
		for(int k=0; k<phi.evals.size(); k++) {
		    System.out.println("   " + k + " : " + phi.evals.get(k).verdict);	
		}
	}

}
