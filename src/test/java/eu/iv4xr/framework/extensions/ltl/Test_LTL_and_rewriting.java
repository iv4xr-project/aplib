package eu.iv4xr.framework.extensions.ltl;

import static eu.iv4xr.framework.extensions.ltl.LTL.* ;
import static eu.iv4xr.framework.extensions.ltl.LTL2Buchi.*;
import static org.junit.jupiter.api.Assertions.*;
import static eu.iv4xr.framework.extensions.ltl.Test_LTL.sequence ; 

import java.util.List;

import org.junit.jupiter.api.Test;

public class Test_LTL_and_rewriting {
	
	void test_andRewrite(LTL<Integer> phi, List<Integer> ... seqs) {
        System.out.println("------") ;
		System.out.println(">>  phi: " + phi) ;
		var phi__ = andRewrite(phi) ;
		System.out.println(">>  andrw(phi): " + phi__) ;
		for(int k=0; k<seqs.length; k++) {
			var verdictPhi = phi.sat(seqs[k]) ;
			var verdictPhi__ = phi__.sat(seqs[k]) ;
			System.out.println(">>  seq-" + k + " passed:" + verdictPhi 
					+ " vs " + verdictPhi__ + "; aggre:" + (verdictPhi == verdictPhi__)) ;
			assertEquals(verdictPhi,verdictPhi__) ;
		}
	}
	
	// check that all conjunctions in f is irreducible
	void check_irreducibility(LTL<Integer> f) {
		if (f instanceof Now) 
			return ;
		if (f instanceof Next) {
			check_irreducibility(((Next<Integer>) f).phi) ;
			return ;
		}
		if (f instanceof Or) {
			Or<Integer> f_ = (Or<Integer>) f ;
			for (int k=0; k<f_.disjuncts.length; k++) {
				check_irreducibility(f_.disjuncts[k]) ;				
			}
			return ;
		}
		if (f instanceof Not) {
			// should not happen!
			assertTrue(false) ;
			return ;
		}
		if (f instanceof Until) {
			Until<Integer> f_ = (Until<Integer>) f ;
			check_irreducibility(f_.phi1) ;
			check_irreducibility(f_.phi2) ;
			return ;
		}
		if (f instanceof WeakUntil) {
			WeakUntil<Integer> f_ = (WeakUntil<Integer>) f ;
			check_irreducibility(f_.phi1) ;
			check_irreducibility(f_.phi2) ;
			return ;
		}
		if (f instanceof And) {
			assertTrue(isIrreducibleConj((And<Integer>) f)) ;
			return ;
		}
		assertTrue(false) ;
	}
	
	@Test
	public void test_irreducible_cases() {
		var seq0 = sequence(0,1,2,3,4) ;
		var seq1 = sequence(2,3,4) ;
		var seq2 = sequence(3) ;
		
		test_andRewrite(now("i=0",i -> i==0),seq0,seq1,seq2) ;
		test_andRewrite(next(now("i=0",i -> i==0)),seq0,seq1,seq2) ;
		test_andRewrite(now("i=0",(Integer i) -> i<2).until(now("i=2",i -> i==2)),seq0,seq1,seq2) ;
		test_andRewrite(now("i=0",(Integer i) -> i<2).weakUntil(now("i=3",i -> i==3)),seq0,seq1,seq2) ;
		
		test_andRewrite(ltlOr(now("i=0",i -> i==0), now("i=3",i -> i==3)),seq0,seq1,seq2) ;
		test_andRewrite(ltlOr(now("i=0",i -> i==0), next(now("i=3",i -> i==3))),seq0,seq1,seq2) ;
		
		LTL<Integer> f1 = now("i=0",(Integer i) -> i<2).until(now("i=2",i -> i==2)) ;
		LTL<Integer> f2 = now("i=0",(Integer i) -> i<2).weakUntil(now("i=2",i -> i==2)) ;
		
		test_andRewrite(ltlOr(f1, next(now("i=3",i -> i==3))),seq0,seq1,seq2) ;
		test_andRewrite(ltlOr(f1,f2),seq0,seq1,seq2) ;
		
		// conjunctive irreducible:
		
		test_andRewrite(ltlAnd(now("i=0",i -> i==0), next(now("i<=2",i -> i<=2))),seq0,seq1,seq2) ;
		test_andRewrite(ltlAnd(next(now("i<=2",i -> i<=2)), now("i=0",i -> i==0)),seq0,seq1,seq2) ;

	}
	
	@Test
	public void test_now_cases() {
		var seq0 = sequence(0,1,2,3,4) ;
		var seq1 = sequence(2,3,4) ;
		var seq2 = sequence(3) ;
		var seq2b = sequence(3,3) ;
		
		LTL<Integer> nowLTE2 = now("i<=2",i -> i<=2) ;
		LTL<Integer> phi = ltlAnd(nowLTE2, now("i=0", i-> i==0)) ;
		test_andRewrite(phi,seq0,seq1,seq2) ;
		assertTrue(andRewrite(phi) instanceof Now) ;
		
		// now && or
		test_andRewrite(ltlAnd(nowLTE2, 
				               ltlOr(now("i=0", i-> i==0),
				            		 now("i=2", i-> i==2))),seq0,seq1,seq2) ;
		
		// now && and
		phi = ltlAnd(nowLTE2, 
                     ltlAnd(now("i=0", i-> i==0),
  		                    now("i=2", i-> i==2))) ;
		test_andRewrite(phi,seq0,seq1,seq2) ;
		assertTrue(andRewrite(phi) instanceof Now) ;
		
		// now && until
		test_andRewrite(
		   ltlAnd(nowLTE2, 
                  now("i<3", (Integer i)-> i<3)
                      .until
  		              (now("i==3", i-> i==3))), 
		    seq0, 
		    seq1, 
		    seq2) ;
		
		test_andRewrite(
				   ltlAnd(now("i<3", (Integer i)-> i<3)
		                      .until
		  		              (now("i==3", i-> i==3)),
		  		          nowLTE2), 
				    seq0, 
				    seq1, 
				    seq2) ;
		
		// now && weakuntil
		test_andRewrite(
			 ltlAnd(nowLTE2, 
					now("i<3", (Integer i)-> i<3)
						.weakUntil
						(now("i==3", i-> i==3))), 
				seq0, 
				seq1, 
				seq2) ;
		
		test_andRewrite(
				 ltlAnd(now("i<3", (Integer i)-> i<3)
							.weakUntil
							(now("i==3", i-> i==3)),
						nowLTE2), 
					seq0, 
					seq1, 
					seq2) ;
		
		test_andRewrite(
				 ltlAnd(now("i>0", i -> i>0), 
						now("i>=2", (Integer i)-> i >= 2)
							.weakUntil
							(now("false", i-> false))), 
					seq0, 
					seq1, 
					// seq2,  the rewrite is NOT equivalent on seq2 because it is too short.
					// check the doc of andRewrite.
					seq2b) ;
	}
	
	@Test
	public void test_next_cases() {
		var seq0 = sequence(0,1,2,3,4) ;
		var seq1 = sequence(2,2,3,4) ;
		var seq2 = sequence(3) ;
		var seq2b = sequence(3,3,3) ;
		var seq3 = sequence(3,2,5) ;
		
		LTL<Integer> nextLTE2 = next(now("i<=2",i -> i<=2)) ;
		
		// X && X
		test_andRewrite(
		    ltlAnd(nextLTE2, 
				   next(now("i=1", (Integer i)-> i==1))), 
			seq0, 
			seq1, 
			seq2, seq2b) ;
		
		// X && U
		test_andRewrite(
		    ltlAnd(nextLTE2, 
		           now("i<3", (Integer i)-> i<3)
		               .until
		  		       (now("i==3", i-> i==3))), 
	        seq0, 
			seq1, 
			seq2b,
			seq3) ;
				
		test_andRewrite(
			ltlAnd(now("i<3", (Integer i)-> i<3)
				       .until
				  	   (now("i==3", i-> i==3)),
				   nextLTE2), 
			seq0, 
			seq1, 
			seq2b,
			seq3) ;
		
		// X && W
		test_andRewrite(
				ltlAnd(now("i<3", (Integer i)-> i<3)
					       .weakUntil
					  	   (now("i==3", i-> i==3)),
					   nextLTE2), 
				seq0, 
				seq1, 
				seq2b,
				seq3) ;
		
		test_andRewrite(
				 ltlAnd(next(now("i=3", i -> i==3)), 
						now("i>=2", (Integer i)-> i >= 2)
							.weakUntil
							(now("false", i-> false))), 
					seq0, 
					seq1, 
					seq2b,
					seq3) ;		
	}
	
	@Test
	public void test_until_and_weakuntil_cases() {
		
		var seq0 = sequence(0,1,2,3,4) ;
		var seq1 = sequence(2,2,3,4) ;
		var seq2 = sequence(3) ;
		var seq2b = sequence(3,3) ;
		var seq3 = sequence(3,2,5) ;
		
		LTL<Integer> until1 = now("i<=2",(Integer i) -> i<=2).until(now("i=3", i-> i==3)) ;
		LTL<Integer> weakUntil1 = now("i<=2",(Integer i) -> i<=2).weakUntil(now("i=3", i-> i==3)) ;
		
		// U && U
		LTL<Integer> phi = ltlAnd(until1, 
				now("i>=2", (Integer i)-> i >= 2)
				.until
				(now("i=4", i-> i==4))) ;
		test_andRewrite(phi, seq0, seq1, seq2b, seq3) ;	
		check_irreducibility(andRewrite(phi)) ;
		
		phi = ltlAnd(until1, 
				now("i>=5", (Integer i)-> i >= 5)
				.until
				(now("i=3", i-> i==3))) ;
		test_andRewrite(phi, seq0, seq1, seq2b, seq3);
		check_irreducibility(andRewrite(phi)) ;
		
		
		// U & W
		phi = ltlAnd(until1, 
				now("i>=2", (Integer i)-> i >= 2)
				.weakUntil
				(now("i=4", i-> i==4))) ;
		test_andRewrite(phi, seq0, seq1, seq2b, seq3);
		check_irreducibility(andRewrite(phi)) ;
		
		// W & W
		phi = ltlAnd(weakUntil1, 
				now("i>=2", (Integer i)-> i >= 2)
				.weakUntil
				(now("i=4", i-> i==4))) ;
		test_andRewrite(phi, seq0, seq1, seq2b, seq3);
		check_irreducibility(andRewrite(phi)) ;

		phi = ltlAnd(now("i>=2", (Integer i)-> i >= 2)
				   .weakUntil
				   (now("i=7", i-> i==7)), 
			     now("i<=3", (Integer i)-> i <= 3)
				   .weakUntil
				   (now("false", i-> false))) ;
		test_andRewrite(phi, seq0, seq1, seq2b, seq3);
		check_irreducibility(andRewrite(phi)) ;	
	}
	
	@Test
	public void test_recursion() {
		var seq0 = sequence(0,1,2,3,4) ;
		var seq1 = sequence(2,2,3,4) ;
		var seq2 = sequence(3) ;
		var seq2b = sequence(3,3) ;
		var seq3 = sequence(3,2,5) ;
		
		LTL<Integer> phi =ltlAnd(
				now("i<=2", i -> i<=2),
				ltlAnd(now("i>=0", i -> i>=0),
					   next(now("i==1", i -> i==1)),
					   now("i<3", (Integer i) -> i<3).until(now("i=3", i -> i == 3))
						),
				now("i>=0", i -> i>=0),
				ltlOr(now("true", (Integer i) -> true).until(now("i=7",i -> i==7)),
					  now("i<=4", (Integer i) -> i<=4).weakUntil(now("false",i -> false))
					 )
				
				) ;
		test_andRewrite(phi, seq0, seq1, seq2b, seq3);
		check_irreducibility(andRewrite(phi)) ;	
		
	}
	
}
