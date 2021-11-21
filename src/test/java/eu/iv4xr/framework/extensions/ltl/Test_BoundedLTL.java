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
    			.thereIs(eventually((MyState S) -> S.val == 8)) 
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
		
		assertEquals(SATVerdict.UNSAT, phi.sat(seq2)) ;
		assertTrue(phi.getWitness() == null) ;
		assertEquals(SATVerdict.SAT, phi.sat(seq1)) ;
		assertTrue(phi.getWitness() != null) ;
    	
    }

    

}
