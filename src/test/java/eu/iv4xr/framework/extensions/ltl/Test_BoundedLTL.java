package eu.iv4xr.framework.extensions.ltl;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.ltl.BoundedLTL;
import nl.uu.cs.aplib.mainConcepts.*;

import static eu.iv4xr.framework.extensions.ltl.BoundedLTL.*;
import static eu.iv4xr.framework.extensions.ltl.LTL.*;
import static org.junit.jupiter.api.Assertions.* ;

import java.util.function.Predicate;

public class Test_BoundedLTL {
	
	static class MyEnv extends Environment {
		
		int[] statetrace ;
		int k ;
		boolean init  ;
		Integer val ;
		
		MyEnv(int... x) {
			statetrace = x ; 
			resetWorker() ;
		}
		
		@Override
		public void refreshWorker() { 
			if (init) init = false ;
			else {
			   k++ ; val = statetrace[k] ; 
			}
		} 
		
		@Override
		public void resetWorker() {
			k = 0 ; 
			init = true ; 
			assertTrue(statetrace != null) ;
			val = statetrace[0] ;
		}
	}
	
	@Test
	public void test_ltl_now() {
		// Test an ltl of the form now(p)
		
		// Scenario 1: where the result should be SAT
		var bltl = new BoundedLTL() 
				   .thereIs(now((MyEnv env) -> env.val == 1)) 
				   .when((MyEnv env) ->  env.val % 3 == 1)
				   .until((MyEnv env) -> env.val % 3 == 0)
				   .withStateShowFunction((MyEnv env) -> "" + env.val)
				   ;
		var env_ = new MyEnv(0,1,2,3,4,5,6,7,8,9) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		// 3x refresh
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		
		// 4th time:
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		//System.out.println("" + bltl.trace) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		
		// 3x refresh .. this should not change the SAT result nor that witness found
		env_.refresh() ;
		env_.refresh() ;
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		assertTrue(bltl.trace.trace.size() == 3) ;
		assertTrue(bltl.trace.trace.get(0)[1].equals("1") ) ;
		assertTrue(bltl.trace.trace.get(2)[1].equals("3") ) ;
		
		// Scenario 2: where the result should be UNSAT
		bltl.bltlState = BLTLstate.NOTSTARTED ; // hard reseting bltl so that we can reuse it
		bltl.thereIs(now((MyEnv env) -> env.val == 2)) ;
		env_.resetAndInstrument();
		for (int k=0; k<9; k++) {
			env_.refresh() ;
			assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		}		
	}
	
	@Test
	public void test_ltl_next() {
		// Test an ltl of the form now(p)
		
		// Scenario 1: where the result should be SAT
		var bltl = new BoundedLTL() 
				   .thereIs(next(now((MyEnv env) -> env.val == 2))) 
				   .when((MyEnv env) ->  env.val % 3 == 1)
				   .until((MyEnv env) -> env.val % 3 == 0)
				   .withStateShowFunction((MyEnv env) -> "" + env.val)
				   ;
		var env_ = new MyEnv(0,1,2,3,4,5,6,7,8,9) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		// 3x refresh
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		
		// 4th time:
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		//System.out.println("" + bltl.trace) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		
		// 3x refresh .. this should not change the SAT result nor that witness found
		env_.refresh() ;
		env_.refresh() ;
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		assertTrue(bltl.trace.trace.size() == 3) ;
		assertTrue(bltl.trace.trace.get(0)[1].equals("1") ) ;
		assertTrue(bltl.trace.trace.get(2)[1].equals("3") ) ;
		
		// Scenario 2: where the result should be UNSAT
		bltl.bltlState = BLTLstate.NOTSTARTED ; // hard reseting bltl so that we can reuse it
		bltl.thereIs(next(now((MyEnv env) -> env.val == 3))) ;
		env_.resetAndInstrument();
		for (int k=0; k<9; k++) {
			env_.refresh() ;
			assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		}		
	}
	
	@Test
	public void test_ltl_not() {
		// Test an ltl of the form now(p)
		
		// Scenario 1: where the result should be SAT
		var bltl = new BoundedLTL() 
				   .thereIs(ltlNot(now((MyEnv env) -> env.val != 1))) 
				   .when((MyEnv env) ->  env.val % 3 == 1)
				   .until((MyEnv env) -> env.val % 3 == 0)
				   .withStateShowFunction((MyEnv env) -> "" + env.val)
				   ;
		var env_ = new MyEnv(0,1,2,3,4,5,6,7,8,9) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		// 3x refresh
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		
		// 4th time:
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		//System.out.println("" + bltl.trace) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		
		// 3x refresh .. this should not change the SAT result nor that witness found
		env_.refresh() ;
		env_.refresh() ;
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		assertTrue(bltl.trace.trace.size() == 3) ;
		assertTrue(bltl.trace.trace.get(0)[1].equals("1") ) ;
		assertTrue(bltl.trace.trace.get(2)[1].equals("3") ) ;
		
		// Scenario 2: where the result should be UNSAT
		bltl.bltlState = BLTLstate.NOTSTARTED ; // hard reseting bltl so that we can reuse it
		bltl.thereIs(ltlNot(now((MyEnv env) -> env.val >= 0))) ;
		env_.resetAndInstrument();
		for (int k=0; k<9; k++) {
			env_.refresh() ;
			assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		}		
	}
	
	@Test
	public void test_ltl_until() {
		// Test an ltl of the form now(p)
		
		// Scenario 1: where the result should be SAT
		var bltl = new BoundedLTL() 
				   .thereIs(now((MyEnv env) -> env.val <=6).ltlUntil(now((MyEnv env)-> env.val==3))) 
				   .when((MyEnv env) ->  env.val % 3 == 1)
				   .until((MyEnv env) -> env.val % 3 == 0)
				   .withStateShowFunction((MyEnv env) -> "" + env.val)
				   ;
		var env_ = new MyEnv(0,1,2,3,4,5,6,7,8,9) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		// 3x refresh
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		
		// 4th time:
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		//System.out.println("" + bltl.trace) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		
		// 3x refresh .. this should not change the SAT result nor that witness found
		env_.refresh() ;
		env_.refresh() ;
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		assertTrue(bltl.trace.trace.size() == 3) ;
		assertTrue(bltl.trace.trace.get(0)[1].equals("1") ) ;
		assertTrue(bltl.trace.trace.get(2)[1].equals("3") ) ;
		
		// Scenario 2: where the result should be UNSAT
		bltl.bltlState = BLTLstate.NOTSTARTED ; // hard reseting bltl so that we can reuse it
		bltl.thereIs(now((MyEnv env) -> env.val <=6).ltlUntil(now((MyEnv env)-> env.val==100)))  ;
		env_.resetAndInstrument();
		for (int k=0; k<9; k++) {
			env_.refresh() ;
			assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		}		
	}
	
	@Test
	public void test_ltl_and() {
		// Test an ltl of the form now(p)
		
		// Scenario 1: where the result should be SAT
		var bltl = new BoundedLTL() 
				   .thereIs(ltlAnd(now((MyEnv env) -> env.val <=6), next(now((MyEnv env)-> env.val==2)))) 
				   .when((MyEnv env) ->  env.val % 3 == 1)
				   .until((MyEnv env) -> env.val % 3 == 0)
				   .withStateShowFunction((MyEnv env) -> "" + env.val)
				   ;
		var env_ = new MyEnv(0,1,2,3,4,5,6,7,8,9) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		// 3x refresh
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		
		// 4th time:
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		System.out.println("" + bltl.trace) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		
		// 3x refresh .. this should not change the SAT result nor that witness found
		env_.refresh() ;
		env_.refresh() ;
		env_.refresh() ;
		//System.out.println(">>> " + env_.val) ;
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		assertTrue(bltl.trace.trace.size() == 3) ;
		assertTrue(bltl.trace.trace.get(0)[1].equals("1") ) ;
		assertTrue(bltl.trace.trace.get(2)[1].equals("3") ) ;
		
		// Scenario 2: where the result should be UNSAT
		bltl.bltlState = BLTLstate.NOTSTARTED ; // hard reseting bltl so that we can reuse it
		bltl.thereIs(ltlAnd(now((MyEnv env) -> env.val <=6), next(now((MyEnv env)-> env.val==3)))) ;
		env_.resetAndInstrument();
		for (int k=0; k<9; k++) {
			env_.refresh() ;
			assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		}		
	}
	
	@Test
	public void test_ltl_withZeroLengthInterval() {
		// Scenario 1: SAT
		var bltl = new BoundedLTL() 
				   .thereIs(now((MyEnv env) -> env.val == 1)) 
				   .when( (MyEnv env) ->  true)
				   .until((MyEnv env) -> true)
				   .withStateShowFunction((MyEnv env) -> "" + env.val)
				   ;
		var env_ = new MyEnv(0,1,2,3,4) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		env_.refresh(); 
		assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		
		// Scenario 2: UNSAT
		bltl.bltlState = BLTLstate.NOTSTARTED ; // hard reseting bltl so that we can reuse it
		bltl.thereIs(now((MyEnv env) -> env.val == 100)) ; 
		env_.resetAndInstrument();
		for (int k=0; k<4; k++) {
			env_.refresh(); 
			assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		}
	}
	
	@Test
	public void test_ltl_whenIntervalLengthIsExceeded() {
		
		var bltl = new BoundedLTL() 
					   .thereIs(now((MyEnv env) -> env.val == 1)) 
					   .when( (MyEnv env) -> env.val == 1)
					   .until((MyEnv env) -> env.val == 4)
					   .withMaxLength(2)
					   .withStateShowFunction((MyEnv env) -> "" + env.val)
						;
		var env_ = new MyEnv(0,1,2,3,4,5) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		for (int k=0; k<5; k++) {
			env_.refresh(); 
			assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		}
	}
	
	@Test
	public void test_ltl_nested_formula() {
		// well... deeper nested
		
		Predicate<MyEnv> p1 = env -> 2 <= env.val ;
		Predicate<MyEnv> p2 = env -> env.val <= 3 ;
		
		Predicate<MyEnv> q = env -> 4 <= env.val ;
		Predicate<MyEnv> r = env -> env.val != 7 ;
		// X(p1 && p2 U (q U ~r)) :
		var phi = next(ltlAnd(now(p1),now(p2)).ltlUntil(now(q).ltlUntil(ltlNot(now(r))))) ;
		
		var bltl = new BoundedLTL() 
				   .thereIs(phi) 
				   .when( (MyEnv env) -> env.val == 1)
				   .until((MyEnv env) -> env.val == 8)
				   .withStateShowFunction((MyEnv env) -> "" + env.val)
					;
		var env_ = new MyEnv(0,1,2,3,4,5,6,7,8,9,10) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		
		for(int k=0; k<8; k++) {
			env_.refresh();
			System.out.println(">>> val=" + env_.val) ;
			assertTrue(bltl.getVerdict() == LTLVerdict.UNSAT) ;
		}
		
		for (int k=8; k<11; k++) {
			env_.refresh();
			System.out.println(">>> *val=" + env_.val) ;
			assertTrue(bltl.getVerdict() == LTLVerdict.SAT) ;
		}
	}

}
