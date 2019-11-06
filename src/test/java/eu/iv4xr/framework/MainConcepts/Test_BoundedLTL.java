package eu.iv4xr.framework.MainConcepts;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import nl.uu.cs.aplib.MainConcepts.*;
import static eu.iv4xr.framework.MainConcepts.BoundedLTL.* ;

public class Test_BoundedLTL {
	
	static class MyEnv extends Environment {
		
		int[] statetrace ;
		int k = 0 ;
		boolean init = true ;
		Integer val ;
		
		MyEnv(int... x) {
			statetrace = x ;
			val = x[0] ;
		}
		
		@Override
		public void refresh() { 
			if (init) init = false ;
			else {
			   k++ ; val = statetrace[k] ; 
			}
		} 
	}
	
	@Test
	public void test_ltl_now() {
		var bltl = new BoundedLTL() 
				   .thereIs(now((MyEnv env) -> env.val == 2)) 
				   .when((MyEnv env) ->  env.val == 1)
				   .until((MyEnv env) -> env.val == 3)
				   .withStateShowFunction((MyEnv env) -> "" + env.val)
				   ;
		var env_ = new MyEnv(0,1,2,3,4,5,6,7,8,9) ;
		env_.turnOnDebugInstrumentation() ;
		bltl.attachToEnv(env_) ;
		// 3x refresh
		env_.refreshAndInstrument(); env_.refreshAndInstrument(); env_.refreshAndInstrument(); 
		System.out.println(">>> " + env_.val) ;
		assertTrue(bltl.getVerdict() == VERDICT.UNSAT) ;
		env_.refreshAndInstrument() ;
		System.out.println(">>> " + env_.val) ;
		System.out.println("" + bltl.trace) ;
		//assertTrue(bltl.getVerdict() == VERDICT.SAT) ;
		env_.refreshAndInstrument() ;
		System.out.println(">>> " + env_.val) ;
		System.out.println("" + bltl.trace) ;
		//assertTrue(bltl.getVerdict() == VERDICT.SAT) ;
		
	}

}
