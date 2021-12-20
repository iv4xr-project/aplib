package eu.iv4xr.framework.extensions.ltl;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static eu.iv4xr.framework.extensions.ltl.LTL.* ;
import static eu.iv4xr.framework.extensions.ltl.LTL2Buchi.* ;
import static eu.iv4xr.framework.extensions.ltl.Test_LTL.* ;
import static org.junit.jupiter.api.Assertions.*;

public class Test_LTL2Buchi {
	
	void test_pushNeg(LTL<Integer> phi, List<Integer> ... seqs) {
        System.out.println("------") ;
		System.out.println(">>  phi: " + phi) ;
		var phi__ = pushNegations(phi) ;
		System.out.println(">>  pushneq(phi): " + phi__) ;
		for(int k=0; k<seqs.length; k++) {
			var verdictPhi = phi.sat(seqs[k]) ;
			var verdictPhi__ = phi__.sat(seqs[k]) ;
			System.out.println(">>  seq-" + k + " passed:" + verdictPhi 
					+ " vs " + verdictPhi + "; aggree:" + (verdictPhi == verdictPhi__)) ;
			assertEquals(verdictPhi,verdictPhi__) ;
		}
	}
	
	void evalAndPrint(String name, LTL<Integer> phi, List<Integer> seq) {
		System.out.println(">>> " + name + ": " + phi.sat(seq)) ;
		for(int k=0; k<phi.evals.size(); k++) {
		    System.out.println("   " + k + " : " + phi.evals.get(k).verdict);	
		}
	}
	
	@Test
	public void test_pushNeg() {
		
		Now<Integer> x0 = now("x=0", x -> x==0) ;
		Now<Integer> x3 = now("x=3", x -> x==3) ;
		Now<Integer> x4 = now("x=4", x -> x==4) ;
		Now<Integer> x5 = now("x=5", x -> x==5) ;
		Now<Integer> x_1_3 = now("1<=x<=3", x -> 1<=x && x<=3) ;

		var seq1 = sequence(0,1,2,3,4) ;
		var seq2 = sequence(1,2,3,4) ;
		var seq3 = sequence(3,4) ;
		var seq4 = sequence(1,2,3,5) ;
		var seq5 = sequence(0,1,2,3,4,4,4,5) ;
		var seq6 = sequence(0,1,2,3,4,4,4,6) ;
		
		LTL<Integer> phi = ltlNot(x0) ;
		test_pushNeg(phi,seq1,seq2) ;
		phi = ltlNot(ltlOr(x0,x4)) ;
		test_pushNeg(phi,seq1,seq2) ;
			
		phi = ltlNot(ltlAnd(x3, ltlNot(ltlOr(x0,x4)))) ;
		test_pushNeg(phi,seq1,seq2,seq3) ;
		
		phi = ltlNot(ltlAnd(x3, ltlOr(x0,x4))) ;
		test_pushNeg(phi,seq1,seq2,seq3) ;
		
		phi = ltlNot(ltlOr(x3, ltlNot(ltlAnd(x0,x4)))) ;
		test_pushNeg(phi,seq1,seq2,seq3) ;

		phi = ltlNot(ltlOr(x3, ltlAnd(x0,x4))) ;
		test_pushNeg(phi,seq1,seq2,seq3) ;
		
		phi = ltlNot(x_1_3.until(x4)) ;
		test_pushNeg(phi,seq1,seq2,seq3,seq4) ;
		
		phi = ltlNot(x_1_3.weakUntil(x4)) ;
		test_pushNeg(phi,seq1,seq2,seq3,seq4) ;
		
		phi = ltlNot(x_1_3.until(x4.until(x5))) ;
		test_pushNeg(phi,seq1,seq2,seq3,seq4,seq4,seq5) ; 
		
		phi = ltlNot(x_1_3.until(x4.weakUntil(x5))) ;
		test_pushNeg(phi,seq1,seq2,seq3,seq4,seq4,seq5) ; 
		
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
	
	@Test
	public void test_insert_and_renameBuchiStates() {
		Buchi B = new Buchi() ;
		B.withStates("S","A") 
		.withInitialState("S")
		.withNonOmegaAcceptance("A")
		.withOmegaAcceptance("S") ;
		B.withTransition("S", "S", "p", S -> true) ;
		B.withTransition("S", "A", "q", S -> true) ;
		
		System.out.println("B:" + B) ;
		
		B = B.appendStateNames("xxx") ;
		
		System.out.println("Bxxx:" + B) ;
		
		B = B.insertNewState("init") ;
		
		System.out.println("Bxxx+:" + B) ;
		
		assertTrue(B.states.size() == 3) ;
		assertTrue(B.initialState == 1) ;
		assertTrue(B.decoder[0].equals("init")) ;
		assertTrue(B.decoder[1].equals("Sxxx")) ;
		assertTrue(B.decoder[2].equals("Axxx")) ;	
		
	}
	


}
