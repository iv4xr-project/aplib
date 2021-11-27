package eu.iv4xr.framework.extensions.ltl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.ltl.BasicModelChecker.Path;

public class Test_SimpleModelChecker {
	
	static class Transition implements ITransition{
		
		public String name ;
		
		public Transition(String name) { this.name = name ; }
		
		@Override
		public String getId() {
			return name;
		}
		
		@Override
		public String toString() { return name ; } 
	}
	
	/**
	 * Some program we use as target for testing model-checker. The program has two
	 * "transitions", one (incx) increases the value of x by 1, and the other (incy)
	 * is enabled when x<y, and would increase the value of y by 1.
	 * They wont increase x and y beyond 8.
	 */
	public static class MyProgram implements ITargetModel, IExplorableState {
		
		public int x = 0 ;
		public int y = 0 ;
		
		List<MyProgram> history = new LinkedList<>() ;
		
		public void incx() { 
			if (x<8) x++ ; 
			//System.out.println(">> incx:" + this) ;
		}
		public void incy() { 
			if(y<x) y++ ; 
			//System.out.println(">> incy:" + this) ;
		}
		
		static Transition incx = new Transition("incx") ;
		static Transition incy = new Transition("incy") ;
		
		@Override
		public void reset() {
			x = 0 ; y = 0 ;
			history.clear();
			history.add(this.clone()) ;
			//System.out.println(">> reset:" + this) ;
		}

		@Override
		public IExplorableState getCurrentState() {
			return this ;
		}

		@Override
		public boolean backTrackToPreviousState() {
			if (history.size() <= 1) {
				throw new IllegalArgumentException() ;
			}
			history.remove(history.size() - 1) ;
			var st =  history.get(history.size() - 1) ;
			this.x = st.x ;
			this.y = st.y ;
			return true ;
		}

		@Override
		public List<ITransition> availableTransitions() {
			List<ITransition> candidates = new LinkedList<>() ;
			candidates.add(incx) ;
			if (y<x) {
				candidates.add(incy) ;
			}
			//System.out.println("== #availableTransitions " + candidates.size()) ;
			return candidates ;
		}

		@Override
		public void execute(ITransition tr) {
			if(tr == incx) {
				incx()  ;
			}
			else if (tr == incy) {
				incy() ; 
			}
			history.add(this.clone()) ;
		}

		@Override
		public String showState() {
			return "<x=" + x + ", y=" + y + ">" ;
		}
		
		@Override
		public String toString() {
			return showState() ;
		}

		@Override
		public MyProgram clone() {
			MyProgram o = new MyProgram() ;
			o.x = this.x ;
			o.y = this.y ;
			return o ;
		}
		
		@Override
		public int hashCode() {
			return 1000*y + x ;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof MyProgram) {
				var o_ = (MyProgram) o ;
				return this.x == o_.x && this.y == o_.y ;
			}
			return false ;
		}
		
	}
	
	public static MyProgram cast(IExplorableState S) { return (MyProgram) S ; }
	
	boolean isWellFormed(Path<IExplorableState> path) {
		//System.out.println(">>>> path: " + path) ;
		var path_ = path.path ;
		if (path_.isEmpty()) return false ;
		IExplorableState previousState = null ;
		for(var step : path_) {
			if(previousState != null) {
				MyProgram previousState_ = (MyProgram) previousState ;
				MyProgram pr = new MyProgram() ;
				pr.x = previousState_.x ;
				pr.y = previousState_.y ;
				pr.execute(step.fst);
				//System.out.println(">>>> step: " + step + " vs pr: " + pr) ;
				if(! step.snd.equals(pr)) return false ;
			}
			previousState = step.snd ;
		}
		return true ;
	}
	
	@Test
	public void test_SAT_find_findShortest() {
		var mc = new BasicModelChecker(new MyProgram()) ;
		Predicate<IExplorableState> q = S -> cast(S).x == 0 && cast(S).y == 0 ;
		assertEquals(SATVerdict.SAT,mc.sat(q)) ;
		assertTrue(mc.find(q,0) != null) ;
		assertTrue(mc.find(q,1) != null) ;
		
		q = S -> cast(S).x == 2  ;
		assertEquals(SATVerdict.SAT,mc.sat(q)) ;
		assertTrue(mc.find(q,1) == null) ;
		assertTrue(mc.find(q,2) != null) ;	
		
		Path<IExplorableState> path = mc.find(q,2) ;
		assertTrue(q.test(path.getLastState())) ;
		assertTrue(isWellFormed(path)) ;
		
		q = S -> cast(S).x == 5 && cast(S).y == 5 ;
		assertEquals(SATVerdict.SAT,mc.sat(q,15)) ;
		assertTrue(mc.find(q,9) == null) ;
		assertTrue(mc.find(q,10) != null) ;	
		
		path = mc.find(q,10) ;
		assertTrue(q.test(path.getLastState())) ;
		assertTrue(isWellFormed(path)) ;
		
		
		q = S -> cast(S).x == 3 && cast(S).y > 3 ;
		assertEquals(SATVerdict.UNSAT,mc.sat(q,10)) ;
		
		q = S -> cast(S).x == 5 && cast(S).y == 5 ;
		assertTrue(mc.findShortest(q,9) == null) ;
		assertTrue(mc.findShortest(q,12) != null) ;
		assertTrue(mc.findShortest(q,12).path.size() == 11) ;
		path = mc.findShortest(q,12) ;
		//System.out.println(">>> solution:\n" + path) ;
		assertTrue(q.test(path.getLastState())) ;
		assertTrue(isWellFormed(path)) ;
	}
	
	@Test
	public void test_testsuiteGenerator() {

		var mc = new BasicModelChecker(new MyProgram());

		Function<IExplorableState, Integer> coverageFunction = S -> cast(S).x;

		Integer[] targets0 = { 5 };

		var tsuite = mc.testSuite(Arrays.asList(targets0), coverageFunction, 9, false);

		assertTrue(tsuite.targets.size() == 1);
		assertTrue(tsuite.tests.size() == 1);
		assertTrue(tsuite.covered.size() >= 1);
		assertTrue(tsuite.notCovered().size() == 0);
		assertTrue(tsuite.coverage() == 1f);
		
		Integer[] targets1 = { -1, 5 };

		tsuite = mc.testSuite(Arrays.asList(targets1), coverageFunction, 9, false);

		assertTrue(tsuite.targets.size() == 2);
		assertTrue(tsuite.tests.size() == 1);
		assertTrue(tsuite.covered.size() >= 1);
		assertTrue(tsuite.notCovered().size() == 1);
		assertTrue(tsuite.coverage() == 0.5f);

		Integer[] targets2 = { 5, 0, 1, 2, 3, 4 };

		tsuite = mc.testSuite(Arrays.asList(targets2), coverageFunction, 9, false);

		assertTrue(tsuite.targets.size() == 6);
		assertTrue(tsuite.tests.size() == 1);
		assertTrue(tsuite.covered.size() >= 6);
		assertTrue(tsuite.notCovered().size() == 0);
		assertTrue(tsuite.coverage() == 1f);

		mc.testSuite(Arrays.asList(targets2), coverageFunction, 9, true);

		assertTrue(tsuite.targets.size() == 6);
		assertTrue(tsuite.tests.size() == 1);
		assertTrue(tsuite.covered.size() >= 6);
		assertTrue(tsuite.notCovered().size() == 0);
		assertTrue(tsuite.coverage() == 1f);
	}

}
