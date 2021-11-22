package eu.iv4xr.framework.extensions.ltl;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.ltl.BoundedLTL;
import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.utils.Pair;
import static eu.iv4xr.framework.extensions.ltl.BoundedLTL.*;
import static eu.iv4xr.framework.extensions.ltl.LTL.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Test_BoundedLTL {

    static class MyAction implements ITransition{
    	String id ;
    	
    	MyAction(String id) { this.id = id ; }

		@Override
		public String getId() {
			return id ;
		}
    }
    
    static MyAction init = new MyAction("INIT") ;
    static MyAction inc = new MyAction("inc") ;
    static MyAction dec = new MyAction("dec") ;
    static MyAction skip = new MyAction("skip") ;
    static MyAction jump = new MyAction("jump") ;
    
    static class MyState implements IState {
    	
    	int val ;

		@Override
		public String showState() {
			return "val=" + val ;
		}
    }
    
    static MyState state(int i) {
    	MyState S = new MyState() ;
    	S.val = i ;
    	return S ;
    }
    
    static Pair<ITransition,IState> cast(Pair<MyAction,MyState> step) {
    	return new Pair(step.fst,step.snd) ;
    }
    
    static Pair<MyAction,MyState> step(MyAction tr, MyState s) {
    	return new Pair(tr,s) ;
    }
      
    static List<Pair<MyAction,MyState>> sequence(Pair<MyAction,MyState> ...steps) {
    	return Arrays.asList(steps) ;
    }
    
    static List<Pair<ITransition,IState>> sequence_(Pair<MyAction,MyState> ...steps) {
    	return sequence(steps).stream().map(step -> cast(step)).collect(Collectors.toList()) ;
    }
    
    static void checkWitness(BoundedLTL phi, int[] expectedWitness) {
    	var witness = phi.getWitness() ;
		assertEquals(expectedWitness.length, witness.trace.size());
		for (int i=0; i < expectedWitness.length; i++) {
			assertEquals("val=" + expectedWitness[i], witness.trace.get(i).snd) ;
		}
    }
    
    @Test
    public void test1() {
    	
    	var seq1 = sequence_(
    			step(init,state(0)),
    			step(inc,state(1)),
    			step(inc,state(2)),
    			step(inc,state(3)),
    			step(jump,state(8)),
       			step(dec,state(7)),       		     			
       			step(skip,state(7))       		     			
    			) ;
    	
    	var seq2 = sequence_(
    			step(init,state(0)),
    			step(jump,state(5)),
    			step(inc,state(6)),
    			step(inc,state(7)),
       			step(skip,state(7))       		     			
    			) ;
    	
    	BoundedLTL phi = new BoundedLTL()
    			.thereIs(eventually(S -> ((MyState) S).val == 8)) 
    			.when(S -> ((MyState) S).val==2)
    			.until(S -> ((MyState) S).val==7) ;
    	
    	assertEquals(SATVerdict.UNSAT,phi.sat()) ;

    	phi.startChecking();
		for(var i : seq1) {
			phi.checkNext(i);
		}
		phi.endChecking();
		assertEquals(SATVerdict.SAT, phi.sat()) ;
		assertEquals(SATVerdict.SAT, phi.sat()) ;
		assertTrue(phi.getWitness() != null) ;

		System.out.println(phi.getWitness().toString()) ;

		int[] expectedWitness = {2,3,8,7} ;
		checkWitness(phi,expectedWitness) ;
		
		assertEquals(SATVerdict.UNSAT, phi.sat(seq2)) ;
		assertTrue(phi.getWitness() == null) ;
		assertEquals(SATVerdict.SAT, phi.sat(seq1)) ;
		assertTrue(phi.getWitness() != null) ;
    	
    }
    
    @Test
    public void test_when_thereare_multiple_sat_segments() {
    	
    	BoundedLTL phi = new BoundedLTL()
    			.thereIs(eventually(S -> ((MyState) S).val == 3)) 
    			.when(S -> ((MyState) S).val==2)
    			.until(S -> ((MyState) S).val>2 && ((MyState) S).val<5) ;
    	
    	var seq = sequence_(
    			step(init,state(0)),
    			step(inc,state(1)),
    			step(inc,state(2)),   // Satisfying segment 1
    			step(skip,state(2)),  // Satisfying segment 1
    			step(inc,state(3)),   // Satisfying segment 1
       			step(dec,state(2)),   // Satisfying segment 2   	
       			step(dec,state(1)),   // Satisfying segment 2	
       			step(inc,state(3))    // Satisfying segment 2	
    			) ;
    	
    	assertEquals(SATVerdict.SAT, phi.sat(seq)) ;
    	// check that it is the first segment that is returned as the witness;
    	// also checking that the witness allow multiple instances of p in
    	// the segment, but the maximal one will be returned
    	int[] expectedWitness = {2,2,3} ;
    	checkWitness(phi,expectedWitness) ;
    }
    
    @Test
    public void test_when_p_or_q_holds_immediately() {
    	
    	BoundedLTL phi1 = new BoundedLTL()
    			.thereIs(always(S -> ((MyState) S).val >= 2)) 
    			.when(S -> ((MyState) S).val==2)
    			.until(S -> ((MyState) S).val==3) ;
    	
    	var seq = sequence_(
    			step(init,state(2)),
    			step(inc,state(3)),
    			step(dec,state(0)),   
    			step(skip,state(0))
    			) ;
    	
    	// we have a satisfying segment that starts immediately:
    	assertEquals(SATVerdict.SAT, phi1.sat(seq)) ;
    	int[] expectedWitness1 = {2,3} ;
    	checkWitness(phi1,expectedWitness1) ;
    	
    	BoundedLTL phi2a = new BoundedLTL()
    			.thereIs(always(S -> true)) 
    			.when(S -> ((MyState) S).val==3)
    			.until(S -> ((MyState) S).val<5) ;
    	
    	BoundedLTL phi2b = new BoundedLTL()
    			.thereIs(always(S -> false)) 
    			.when(S -> ((MyState) S).val==3)
    			.until(S -> ((MyState) S).val<5) ;
    	
    	// we have a q that is implied by p; so the segment is just of size 1:
    	assertEquals(SATVerdict.SAT, phi2a.sat(seq)) ;
    	int[] expectedWitness2 = {3} ;
    	checkWitness(phi2a,expectedWitness2) ;
    	
    	assertEquals(SATVerdict.UNSAT, phi2b.sat(seq)) ;
		
    }
    
    /**
     * Testing different cases of unsat.
     */
    @Test
    public void test_UNSAT() {
    	
    	// the case when there are pq-segments, but none satisfy phi
    	BoundedLTL phi1 = new BoundedLTL()
    			.thereIs(always(S -> false)) 
    			.when(S -> ((MyState) S).val==2)
    			.until(S -> ((MyState) S).val==3) ;
    	
    	var seq1 = sequence_(
    			step(init,state(2)),
    			step(inc,state(3)),
    			step(dec,state(2)),   
    			step(skip,state(2)),
    			step(inc,state(3))
    			) ;
    	assertEquals(SATVerdict.UNSAT, phi1.sat(seq1)) ;
    	
    	BoundedLTL phi2 = new BoundedLTL()
    			.thereIs(always(S -> true)) 
    			.when(S -> ((MyState) S).val==2)
    			.until(S -> ((MyState) S).val==3) ;

    	// the case when there is no pq-segment
    	var seq2 = sequence_(
    			step(init,state(2)),
    			step(inc,state(5)),
    			step(dec,state(2)),   
    			step(skip,state(2)),
    			step(inc,state(2))
    			) ;
    	assertEquals(SATVerdict.UNSAT, phi2.sat(seq2)) ;
    }

    @Test
    public void test_upperbound_on_segment_length() {
    	
    	BoundedLTL phi1 = new BoundedLTL()
    			.thereIs(always(S -> true)) 
    			.when(S -> ((MyState) S).val==2)
    			.until(S -> ((MyState) S).val==3)
    			.withMaxLength(2) ;
    	
    	var seq1 = sequence_(
    			step(init,state(2)),
    			step(skip,state(2)),
    			step(inc,state(3)),
    			step(dec,state(0)),   
    			step(inc,state(2)),
    			step(inc,state(3)),
    			step(dec,state(0))   
    			) ;
       assertEquals(SATVerdict.SAT, phi1.sat(seq1)) ;
       assertEquals(2,phi1.getWitness().trace.size()) ;
      
       int[] expectedWitness = {2,3} ;
  	   checkWitness(phi1,expectedWitness) ;
  	   
  	 var seq2 = sequence_(
 			step(init,state(2)),
 			step(skip,state(2)),
 			step(inc,state(3)),
 			step(dec,state(0)),   
 			step(inc,state(2)),
 			step(skip,state(2)),
 			step(inc,state(3)),
 			step(dec,state(0))   
 			) ;
  	 
  	assertEquals(SATVerdict.UNSAT, phi1.sat(seq2)) ;
  	  
    }
    

}
