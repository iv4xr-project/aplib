package eu.iv4xr.framework.extensions.ltl;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.ltl.Test_SimpleModelChecker.MyProgram;
import static eu.iv4xr.framework.extensions.ltl.Test_SimpleModelChecker.cast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Test_BuchiModelChecker {
	
	Buchi eventually(Predicate<MyProgram> q) {
		Buchi buchi = new Buchi() ;
		buchi.withStates("S0","accept") 
		.withInitialState("S0")
		.withNonOmegaAcceptance("accept")
		.withTransition("S0", "S0", "~q", S -> ! q.test(cast(S)))
		.withTransition("S0", "accept", "q", S -> q.test(cast(S)))  ;
		return buchi ;
	}
	
	Buchi eventuallyAlways(Predicate<MyProgram> q) {
		Buchi buchi = new Buchi() ;
		buchi.withStates("S0","accept") 
		.withInitialState("S0")
		.withOmegaAcceptance("accept")
		.withTransition("S0", "S0", "~q", S -> ! q.test(cast(S)))  
		.withTransition("S0", "accept", "q", S -> q.test(cast(S)))  
		.withTransition("accept", "accept", "q", S -> q.test(cast(S)))  
		;
		return buchi ;
	}
	
	Buchi until(Predicate<MyProgram> p, Predicate<MyProgram> q) {
		Buchi buchi = new Buchi() ;
		buchi.withStates("S0","accept") 
		.withInitialState("S0")
		.withNonOmegaAcceptance("accept")
		.withTransition("S0", "S0", "p && ~q", S -> p.test(cast(S)) && ! q.test(cast(S)))
		.withTransition("S0", "accept", "q", S -> q.test(cast(S)))  ;
		return buchi ;
	}
	
	Buchi weakUntil(Predicate<MyProgram> p, Predicate<MyProgram> q) {
		Buchi buchi = new Buchi() ;
		buchi.withStates("S0","accept") 
		.withInitialState("S0")
		.withOmegaAcceptance("S0")
		.withNonOmegaAcceptance("accept")
		.withTransition("S0", "S0", "p && ~q", S -> p.test(cast(S)) && ! q.test(cast(S)))
		.withTransition("S0", "accept", "q", S -> q.test(cast(S)))  ;
		return buchi ;
	}
	
	@Test
	public void test_eventually() {
		
		BuchiModelChecker bmc = new BuchiModelChecker(new MyProgram()) ;
		
		Predicate<MyProgram> q = S -> S.x == 5 ;
		var path = bmc.find(eventually(q), 12) ;
		assertTrue(path != null) ;
		assertEquals(SATVerdict.SAT, bmc.sat(eventually(q),12)) ;
		
		q = S -> S.x > 3 && S.x == S.y ;
		path = bmc.find(eventually(q), 12) ;
		assertTrue(path != null) ;
		assertEquals(SATVerdict.SAT, bmc.sat(eventually(q),12)) ;
		
		q = S -> S.x == 6 ;
		path = bmc.find(eventually(q), 5) ;
		assertTrue(path == null) ;
		assertEquals(SATVerdict.UNSAT, bmc.sat(eventually(q),5)) ;
		
		q = S -> S.x < 0 ;
		path = bmc.find(eventually(q), 5) ;
		assertTrue(path == null) ;
		assertEquals(SATVerdict.UNSAT, bmc.sat(eventually(q),5)) ;
		
	}
	
	@Test
	public void test_eventuallyAlways() {
		
    BuchiModelChecker bmc = new BuchiModelChecker(new MyProgram()) ;
		
		Predicate<MyProgram> q = S -> S.x == 5 ;
		var path = bmc.find(eventuallyAlways(q), 12) ;
		assertTrue(path == null) ;
	
		q = S -> S.x == 9 ;
		path = bmc.find(eventuallyAlways(q), 12) ;
		assertTrue(path == null) ;
	
		
		q = S -> S.x >= 5 ;
		path = bmc.find(eventuallyAlways(q),5) ;
		assertTrue(path == null) ;
		
		path = bmc.find(eventuallyAlways(q),12) ;
		assertTrue(path != null) ;
		assertTrue(q.test(cast(path.getLastState().fst))) ;
	
	}
	
	@Test
	public void test_until() {
		
        BuchiModelChecker bmc = new BuchiModelChecker(new MyProgram()) ;
		
        Predicate<MyProgram> p = S -> S.x < 3 ;
		Predicate<MyProgram> q = S -> S.x >= 3 ;
		var path = bmc.find(until(p,q), 2) ;
		assertTrue(path == null) ;
		
		path = bmc.find(until(p,q), 4) ;
		assertTrue(path != null) ;
		assertTrue(q.test(cast(path.getLastState().fst))) ;
		
		p = S -> S.x <= 8 ;
		q = S -> S.x > 8 ;
		path = bmc.find(until(p,q), 12) ;
		assertTrue(path == null) ;
		
		p = S -> S.x < 0 ;
		q = S -> S.x >= 0 ;
		path = bmc.find(until(p,q), 12) ;
		assertTrue(path != null) ;
		assertTrue(q.test(cast(path.getLastState().fst))) ;		
	}
	
	@Test
	public void test_weakUntil() {
		
        BuchiModelChecker bmc = new BuchiModelChecker(new MyProgram()) ;
		
        Predicate<MyProgram> p = S -> S.x < 3 ;
		Predicate<MyProgram> q = S -> S.x >= 3 ;
		var path = bmc.find(weakUntil(p,q), 2) ;
		assertTrue(path == null) ;
		
		p = S -> S.x < 3 ;
		q = S -> S.x >= 3 ;
		path = bmc.find(weakUntil(p,q), 4) ;
		assertTrue(path != null) ;
		assertTrue(q.test(cast(path.getLastState().fst))) ;		
		
		p = S -> S.x <= 8 ;
		q = S -> S.x > 8 ;
		path = bmc.find(weakUntil(p,q), 10) ;
		assertTrue(path != null) ;
		assertTrue(p.test(cast(path.getLastState().fst))) ;		
			
	}

}
