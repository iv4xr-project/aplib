package nl.uu.cs.aplib.MainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

public class Test_Strategy {
	
	static class IntState extends SimpleState {
		int i ;
		IntState(int i) { this.i = i ; }
	}
	
	static IntState Int(int x) { return new IntState(x) ; }
	
	@Test
	public void test_getFirstEnabledActions() {
		var a0 = lift(action("a0").on__(s -> ((IntState) s).i == 0)) ;
		var a1 = lift(action("a1").on__(s -> ((IntState) s).i == 1)) ;
		var a2 = lift(action("a2").on__(s -> ((IntState) s).i == 1)) ;
		var a3 = lift(action("a3").on__(s -> ((IntState) s).i == 3)) ;
		var a4 = lift(action("a4").on__(s -> ((IntState) s).i == 4)) ;
		
		assertFalse(FIRSTof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a0)) ;
		assertTrue(FIRSTof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a1)) ;
		assertFalse(FIRSTof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a2)) ;
		
		assertFalse(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a0)) ;
		assertTrue(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a1)) ;
		assertTrue(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a2)) ;
		
		assertFalse(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a0)) ;
		assertTrue(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a1)) ;
		assertTrue(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a2)) ;
	}

}
