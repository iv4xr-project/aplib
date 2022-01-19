package eu.iv4xr.framework.extensions.ltl;

import static eu.iv4xr.framework.extensions.ltl.LTL.*;
import static org.junit.jupiter.api.Assertions.* ;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;


public class Test_LTL {
	
	
	public static List<Integer> sequence(int... s) {
		LinkedList<Integer> seq = new LinkedList<>() ;
		for(int i=0; i<s.length; i++) {
			seq.add(s[i]) ;
		}
		return seq ;
	}
	
	

	@Test
    public void test_ltl_now() {
        // Test an ltl of the form now(p)
		
		var seq1 = sequence(0, 1, 2, 3) ;
		var seq2 = sequence(2, 3) ;
		
		LTL<Integer> phi = now(i -> i == 0) ;
		
		try {
			phi.sat() ;
			assertTrue(false) ;
		}
		catch(Exception e) { }
				
		phi.startChecking();
		for(var i : seq1) {
			phi.checkNext(i);
		}
		phi.endChecking();
		assertEquals(SATVerdict.SAT, phi.sat()) ;
		assertEquals(SATVerdict.SAT, phi.sat()) ;
		phi.startChecking();
		assertEquals(SATVerdict.SAT, phi.sat(seq1)) ;
		assertEquals(SATVerdict.UNSAT, phi.sat(seq2)) ;
    }
	
	@Test
    public void test_ltl_next() {
        
		var seq1 = sequence(0, 1, 2, 3, 4, 5) ;
		
		LTL<Integer> phi = next(next(now(i -> i == 2))) ;
		
		try {
			phi.sat() ;
			assertTrue(false) ;
		}
		catch(Exception e) { }
				
		phi.startChecking();
		for(var i : seq1) {
			phi.checkNext(i);
		}
		phi.endChecking();
		assertEquals(SATVerdict.SAT, phi.sat()) ;
		assertEquals(SATVerdict.SAT, phi.sat()) ;
		phi.startChecking();
		assertEquals(SATVerdict.SAT, phi.sat(seq1)) ;
		
		assertEquals(SATVerdict.UNSAT, next(now((Integer i) -> i == 2)).sat(seq1)) ;
    }
	
	@Test
    public void test_ltlUntil() {
        
		var seq1 = sequence(0, 1, 2, 3, 4, 5) ;
		var seq2 = sequence(0, 1, 2, 2, 2, 2) ;
		
		LTL<Integer> phi = now((Integer i) -> i < 3).until(now((Integer i) -> i==3)) ;
		
		try {
			phi.sat() ;
			assertTrue(false) ;
		}
		catch(Exception e) { }
				
		phi.startChecking();
		for(var i : seq1) {
			phi.checkNext(i);
		}
		phi.endChecking();
		assertEquals(SATVerdict.SAT, phi.sat()) ;
		assertEquals(SATVerdict.SAT, phi.sat()) ;
		phi.startChecking();
		assertEquals(SATVerdict.SAT, phi.sat(seq1)) ;
		
		assertEquals(SATVerdict.UNSAT, phi.sat(seq2)) ;
		
		LTL<Integer> phi2b = now((Integer i) -> i>=3 && i < 5).until(now((Integer i) -> i==5)) ;
		LTL<Integer> phi2 = now((Integer i) -> i<3).until(phi2b) ;
		assertEquals(SATVerdict.SAT, phi2.sat(seq1)) ;
		
		LTL<Integer> phi3b = now((Integer i) -> false).until(now((Integer i) -> i==3)) ;
		LTL<Integer> phi3 = now((Integer i) -> i<3).until(phi3b) ;
		assertEquals(SATVerdict.SAT, phi3.sat(seq1)) ;
		
		assertEquals(SATVerdict.SAT, eventually((Integer i) -> i>=4).sat(seq1)) ;
		assertEquals(SATVerdict.SAT, always((Integer i) -> i<=5).sat(seq1)) ;
		assertEquals(SATVerdict.UNSAT, eventually((Integer i) -> i>5).sat(seq1)) ;
		assertEquals(SATVerdict.UNSAT, always((Integer i) -> i<5).sat(seq1)) ;
    }
	
	@Test
    public void test_ltlWeakUntil() {
        
		var seq1 = sequence(0, 1, 2, 3, 4, 5) ;
		var seq2 = sequence(0, 1, 2, 2, 2, 2) ;
		var seq3 = sequence(0, 1, 2, 4, 5) ;
		var seq4 = sequence(4,5,3) ;
		
		LTL<Integer> phi = now((Integer i) -> i < 3).weakUntil(now((Integer i) -> i==3)) ;
		
		assertEquals(SATVerdict.SAT, phi.sat(seq1)) ;
		assertEquals(SATVerdict.SAT, phi.sat(seq2)) ;
		assertEquals(SATVerdict.UNSAT, phi.sat(seq3)) ;
		assertEquals(SATVerdict.UNSAT, phi.sat(seq4)) ;
	}
	
	@Test
    public void test_ltlNot() {
		var seq1 = sequence(0, 1, 2, 3, 4, 5) ;
		var seq2 = sequence(0, 1, 2, 2, 2, 2) ;
		
		LTL<Integer> phi = now((Integer i) -> i < 6).until((Integer i) -> i==6) ;
		LTL<Integer> psi = ltlNot(phi) ;
		
		try {
			psi.sat() ;
			assertTrue(false) ;
		}
		catch(Exception e) { }
				
		psi.startChecking();
		for(var i : seq1) {
			psi.checkNext(i);
		}
		psi.endChecking();
		assertEquals(SATVerdict.SAT, psi.sat()) ;
		assertEquals(SATVerdict.SAT, psi.sat()) ;
		assertEquals(SATVerdict.UNSAT, phi.sat(seq1)) ;
		psi.startChecking();
		assertEquals(SATVerdict.SAT, psi.sat(seq1)) ;
		assertEquals(SATVerdict.SAT, psi.sat(seq2)) ;
		
		assertEquals(SATVerdict.UNSAT, ltlNot(psi).sat(seq1)) ;
		
	}
	
	@Test
	public void test_ltlAnd() {
		var seq1 = sequence(0, 1, 2, 3, 4, 5) ;
		var seq2 = sequence(0, 1, 2, 2, 2, 2) ;
		
		LTL<Integer> phi1 = now((Integer i) -> i < 5).until((Integer i) -> i==4) ;
		LTL<Integer> phi2 = always((Integer i) -> i <= 5) ;
		LTL<Integer> psi = ltlAnd(phi1,phi2) ;
		
		try {
			psi.sat() ;
			assertTrue(false) ;
		}
		catch(Exception e) { }
				
		psi.startChecking();
		for(var i : seq1) {
			psi.checkNext(i);
		}
		psi.endChecking();
		assertEquals(SATVerdict.SAT, psi.sat()) ;
		assertEquals(SATVerdict.SAT, psi.sat()) ;
		assertEquals(SATVerdict.SAT, phi1.sat(seq1)) ;
		assertEquals(SATVerdict.SAT, phi2.sat(seq1)) ;
		psi.startChecking();
		assertEquals(SATVerdict.SAT, psi.sat(seq1)) ;

		assertEquals(SATVerdict.UNSAT, psi.sat(seq2)) ;

		assertEquals(SATVerdict.UNSAT, ltlNot(psi).sat(seq1)) ;
	}
	
	@Test
	public void test_ltlOr_Implies() {
		
		var seq1 = sequence(0, 1, 2, 3, 4, 5) ;
		
		assertEquals(SATVerdict.UNSAT, eventually((Integer i) -> i>5).sat(seq1)) ;
		assertEquals(SATVerdict.SAT, ltlOr(eventually((Integer i) -> i>5),
				                           eventually((Integer i) -> i>4))
				                       .sat(seq1)) ;
		assertEquals(SATVerdict.UNSAT, ltlOr(eventually((Integer i) -> i>5),
                always((Integer i) -> i<5))
            .sat(seq1)) ;
		
		assertEquals(SATVerdict.SAT, eventually((Integer i) -> i>5)
				.implies(eventually((Integer i) -> false))
            .sat(seq1)) ;
		assertEquals(SATVerdict.SAT, eventually((Integer i) -> i>3)
				.implies(eventually((Integer i) -> i>=5))
            .sat(seq1)) ;
		assertEquals(SATVerdict.UNSAT, eventually((Integer i) -> i>3)
				.implies(eventually((Integer i) -> i>5))
            .sat(seq1)) ;
		
	}
	
	
}
