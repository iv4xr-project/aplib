package eu.iv4xr.framework.extensions.ltl;

import eu.iv4xr.framework.extensions.ltl.Test_SimpleModelChecker.MyProgram;
import static eu.iv4xr.framework.extensions.ltl.LTL.*;
import static eu.iv4xr.framework.extensions.ltl.LTL2Buchi.* ;
import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class Test_LTL2BuchiTranslator {
	
	MyProgram cast(IExplorableState S) { return (MyProgram) S ; }
	
	String indent(int indent, String s) {
		
		String sp = "" ;
		for (int k=0; k<indent; k++) {
			sp += " " ;
		}	
		String sp_ = "" + sp ;
		String z = s.lines().map(r -> sp_ + r).collect(Collectors.joining("\n")) ;
		return z ;
	}
	
	void testBuchi(LTL<IExplorableState> f, SATVerdict expectedVerdict) {
		Buchi B = getBuchi(f) ;
		System.out.println("=================") ;
		System.out.println("** LTL: " + f) ;
		System.out.println("** Expected: " + expectedVerdict) ;
		System.out.println("** Buchi: ") ;
		System.out.println(indent(3,B.toString())) ;
		var BM = new BuchiModelChecker(new MyProgram()) ;
		assertEquals(expectedVerdict,BM.sat(B)) ;
	}
	
	@Test
	public void test_now_and_next() {
		
		LTL<IExplorableState> f = now(S -> cast(S).x==0)  ; 	
		testBuchi(f,SATVerdict.SAT) ;
		f = now(S -> cast(S).x==1)  ; 	
		testBuchi(f,SATVerdict.UNSAT) ;	
		
		f = next(S -> cast(S).x==1)  ; 	
		testBuchi(f,SATVerdict.SAT) ;
		
		f = next(next(S -> cast(S).x==1 && cast(S).y==1 ))  ; 	
		testBuchi(f,SATVerdict.SAT) ;

		f = next(next(S -> cast(S).x==3))  ; 	
		testBuchi(f,SATVerdict.UNSAT) ;
	}
	
	
	@Test
	public void test_until() {
		LTL<IExplorableState> f = now("x<8 && y=0", (IExplorableState S) -> cast(S).x < 8 && cast(S).y == 0)
				.until(now("x=8", S -> cast(S).x == 8));
		testBuchi(f, SATVerdict.SAT);

		f = eventually(now("x=9", S -> cast(S).x == 9));
		testBuchi(f, SATVerdict.UNSAT);

		LTL<IExplorableState> g = now("x<8 && y=4", (IExplorableState S) -> cast(S).x < 8 && cast(S).y == 4)
				.until(now("y>=4 && x=8", S -> cast(S).y >= 4 && cast(S).x == 8));
		f = now("x<=4", (IExplorableState S) -> cast(S).x <= 4).until(g);
		testBuchi(f, SATVerdict.SAT);

		g = now("x<8 && y=4", (IExplorableState S) -> cast(S).x < 8 && cast(S).y == 4)
				.until(now("y>x", S -> cast(S).y > cast(S).x));
		f = now("x<=4", (IExplorableState S) -> cast(S).x <= 4).until(g);
		testBuchi(f, SATVerdict.UNSAT);
	}
	
	@Test
	public void test_weakuntil() {
		LTL<IExplorableState> f = now("x<8 && y=0", (IExplorableState S) -> cast(S).x < 8 && cast(S).y == 0)
				.weakUntil(now("x=8", S -> cast(S).x == 8));
		testBuchi(f, SATVerdict.SAT);
		
		f = now("y<=x", (IExplorableState S) -> cast(S).y <= cast(S).x)
				.weakUntil(now("x==9", S -> cast(S).x == 9));
		testBuchi(f, SATVerdict.SAT);
		
		f = now("x<4", (IExplorableState S) -> cast(S).x < 4)
				.weakUntil(now("y==4", S -> cast(S).y == 4));
		testBuchi(f, SATVerdict.UNSAT);
		
		LTL<IExplorableState> g = now("x<8 && y=4", (IExplorableState S) -> cast(S).x < 8 && cast(S).y == 4)
				.weakUntil(now("y>=4 && x=8", S -> cast(S).y >= 4 && cast(S).x == 8));
		f = now("x<=4", (IExplorableState S) -> cast(S).x <= 4).weakUntil(g);
		testBuchi(f, SATVerdict.SAT);
		
		g = now("x<=8 && y=4", (IExplorableState S) -> cast(S).x <= 8 && cast(S).y == 4)
				.weakUntil(now("x=9", S -> cast(S).x == 9));
		f = now("x<=4", (IExplorableState S) -> cast(S).x <= 4).weakUntil(g);
		testBuchi(f, SATVerdict.SAT);
		
		g = now("x<7 && y=4", (IExplorableState S) -> cast(S).x < 7 && cast(S).y == 4)
				.weakUntil(now("x=9", S -> cast(S).x == 9));
		f = now("x<=4", (IExplorableState S) -> cast(S).x <= 4).weakUntil(g);
		testBuchi(f, SATVerdict.UNSAT);
		
	}
	
	@Test
	public void test_or() {
		LTL<IExplorableState> f = ltlOr(
				now("x=1",S -> cast(S).x==1),
				now("x=0",S -> cast(S).x==0))  ; 
		testBuchi(f, SATVerdict.SAT);
		
		f = ltlOr(
				now("x=1",S -> cast(S).x==1),
				now("x=2",S -> cast(S).x==2),
				now("x=0",S -> cast(S).x==0))  ; 
		testBuchi(f, SATVerdict.SAT);
		
		f = ltlOr(
				now("x=1",S -> cast(S).x==1),
				now("x=2",S -> cast(S).x==2),
				now("x=3",S -> cast(S).x==3))  ; 
		testBuchi(f, SATVerdict.UNSAT);
		
		f = ltlOr(
				now("x=1",S -> cast(S).x==1),
				next(now("x=1",S -> cast(S).x==1)))  ; 
		
		testBuchi(f, SATVerdict.SAT);
		
		f = ltlOr(
				next(now("x=2",S -> cast(S).x==2)),
				now("x<4",(IExplorableState S) -> cast(S).x < 4).until(now("x=4", S -> cast(S).x == 4)),
				now("x=1",S -> cast(S).x==1)
				)  ; 
		testBuchi(f, SATVerdict.SAT);
		
		
		f = ltlOr(
				next(now("x=2",S -> cast(S).x==2)),
				now("x<4",(IExplorableState S) -> cast(S).x < 4).until(now("x=5", S -> cast(S).x == 5)),
				now("x=1",S -> cast(S).x==1)
				)  ; 
		testBuchi(f, SATVerdict.UNSAT);
	}

}
