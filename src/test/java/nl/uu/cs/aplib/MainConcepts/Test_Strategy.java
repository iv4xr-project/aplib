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
		assertTrue(FIRSTof(a0,a1,a2).getFirstEnabledActions(Int(99)).isEmpty()) ;
		
		assertFalse(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a0)) ;
		assertTrue(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a1)) ;
		assertTrue(ANYof(a0,a1,a2).getFirstEnabledActions(Int(1)).contains(a2)) ;
		assertTrue(ANYof(a0,a1,a2).getFirstEnabledActions(Int(99)).isEmpty()) ;
			
		assertTrue(SEQ(a0,a1,a2).getFirstEnabledActions(Int(0)).contains(a0)) ;
		assertFalse(SEQ(a0,a1,a2).getFirstEnabledActions(Int(0)).contains(a1)) ;
		assertFalse(SEQ(a0,a1,a2).getFirstEnabledActions(Int(0)).contains(a2)) ;	
		assertTrue(SEQ(a0,a1,a2).getFirstEnabledActions(Int(1)).isEmpty()) ;
		assertTrue(SEQ(a0,a1,a2).getFirstEnabledActions(Int(99)).isEmpty()) ;
	}

	@Test
	public void test_calcNextStrategy() {
		var a0 = lift(action("a0")) ;
		var a1 = lift(action("a1")) ;
		var a2 = lift(action("a2")) ;
		var a3 = lift(action("a3")) ;
		var a4 = lift(action("a4")) ;
		var a5 = lift(action("a5")) ;

        var s1 = FIRSTof(a0,a1) ;
        var s2 = ANYof(s1,a2) ;
        
        a0.action.completed = false ;
        assertTrue(a0.calcNextStrategy() == a0) ;

        a0.action.completed = true ;
        assertTrue(a0.calcNextStrategy() == null) ;
        a1.action.completed = true ;
        assertTrue(a1.calcNextStrategy() == null) ;
        a2.action.completed = true ;
        assertTrue(a2.calcNextStrategy() == null) ;
        
        var s3 = ANYof(a3,a4) ;
        var s4 = SEQ(s2,s3,a5) ;
        assertTrue(a2.calcNextStrategy() == s3) ;
        assertTrue(s2.calcNextStrategy() == s3) ;
        
        a3.action.completed = true ;
        assertTrue(a3.calcNextStrategy() == a5) ;
        assertTrue(s3.calcNextStrategy() == a5) ;
        
        var s5 = SEQ(a0,a1) ;
        var s6 = SEQ(s5,a2) ;
        a1.action.completed = true ;
        assertTrue(a1.calcNextStrategy() == a2) ;
        a2.action.completed = true ;
        assertTrue(a2.calcNextStrategy() == null) ;
        
		
		
	}
	
}
