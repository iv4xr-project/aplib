package eu.iv4xr.framework.extensions.mbt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.mbt.MBTRunner.ACTION_SELECTION;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.spatial.IntVec2D;
import nl.uu.cs.aplib.agents.State;

public class Test_MBT {
	
	public static class MyAgentState extends State {	
		SimpleGame G ;
		public MyAgentState(SimpleGame G) {
			this.G = G ;
		}
	}
	
	@SuppressWarnings("unchecked")
	MBTAction<MyAgentState> turnR_Action = (new MBTAction<MyAgentState>("turn-right")) 
		.withAction(A -> { 
			var S = (MyAgentState) A.state() ;
			S.G.turnR() ; 
			return true ; 
			
		}) 
		// constrain it:
		.addGuards(S -> S.G.pos.y == S.G.goal.y && S.G.orientation != SimpleGame.EAST) ;
		
	
	@SuppressWarnings("unchecked")
	MBTAction<MyAgentState> turnL_Action = (new MBTAction<MyAgentState>("turn-left"))
		.withAction(A -> { 
			var S = (MyAgentState) A.state() ;
			S.G.turnL() ; 
			return true ; 
			}) 
    	// constrain it:
		.addGuards(S ->S. G.pos.x == S.G.goal.x && S.G.orientation != SimpleGame.NORTH) ;
	
	
	MBTAction<MyAgentState> moveF_Action = (new MBTAction<MyAgentState>("move-forward"))
		.withAction(A -> {
			try {
				var S = (MyAgentState) A.state() ;
				S.G.moveForward() ;
				return true ;
			}
			catch(Exception e) {
				return false ;
			}
		}) ;

	
	@SuppressWarnings("unchecked")
	MBTState<MyAgentState> stateNotWinYet = (new MBTState<MyAgentState>("in-progress")) 
		.addPredicates(S -> ! S.G.win) ;
	
	
	@SuppressWarnings("unchecked") 
	MBTState<MyAgentState> stateWin= (new MBTState<MyAgentState>("win"))
		.addPredicates(S -> S.G.win) ;

	@SuppressWarnings("unchecked")
	MBTModel<MyAgentState> mkModel0() {
		MBTModel<MyAgentState> mymodel = new MBTModel<>("model0") ;
		mymodel.addStates(stateNotWinYet,stateWin) ;
		mymodel.addActions(moveF_Action) ;
		return mymodel ;
	}
	
	public MBTModel<MyAgentState> mkModel1() {
		MBTModel<MyAgentState> mymodel = mkModel0() ;
		mymodel.name = "model1" ;
		mymodel.addActions(turnL_Action, turnR_Action) ;
		return mymodel ;
	}
	
	
	//@Test
	public void test_execNext() {
		var mymodel = mkModel1() ;
		System.out.println("" + mymodel) ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		
		runner.executeNext(null, agent) ;
		assertTrue(mystate.G.pos.equals(new IntVec2D(0,1)));
		runner.executeNext(null, agent) ;
		assertTrue(mystate.G.pos.equals(new IntVec2D(0,2)));
	}

	//@Test
	public void test_genSeq_0() {
		var mymodel = mkModel1() ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		
		var results = runner.generateTestSequence(agent,4) ;
		assertTrue(mystate.G.pos.equals(new IntVec2D(0,4)));
	}
	
	//@Test
	public void test_genSeq_1() {
		var mymodel = mkModel0() ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		
		var results = runner.generateTestSequence(agent,5) ;
		assertTrue(mystate.G.pos.equals(new IntVec2D(0,4)));
		assertFalse(results.get(4).executionSuccessful) ;
	}
	
	//@Test
	public void test_genSeq_2() {
		var mymodel = mkModel1() ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		runner.rnd = new Random() ;
		var results = runner.generateTestSequence(agent,15) ;
		
		var last = results.get(results.size() - 1) ;
		
		if (results.size() < 15) {
			assertFalse(last.executionSuccessful) ;
			assertTrue(last.executedAction.equals("move-forward")) ;
			assertFalse(mystate.G.win) ;
		}
		else {
			assertTrue(mystate.G.pos.equals(mystate.G.goal)) ;
			assertTrue(mystate.G.win) ;
			assertTrue(last.executionSuccessful) ;
		}
		System.out.println(runner.showCoverage()) ;
	}
	
	//@Test
	public void test_genSuite_0() {
		var mymodel = mkModel1() ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		runner.rnd = new Random() ;
		runner.stopSuiteGenerationOnFailedOrViolation = false ;
		var results = runner.generate(
				dummy -> { mystate.G = new SimpleGame(); return agent ;}, 
				30, 
				10) ;
		assertTrue(results.size() == 30) ;
		assertTrue(results.stream().allMatch(seq -> seq.size() <= 10)) ;
		assertTrue(runner.coveredStates.values().stream().filter(i -> i>0).count() ==2 ) ;
		assertTrue(runner.coveredTransitions.values().stream().filter(i -> i>0).count() >= 5 ) ;
		System.out.println(runner.showCoverage()) ;
	}
	
	//@Test
	public void test_inferTransitions() {
		var mymodel = mkModel1() ;
		assertTrue(mymodel.getAllTransitions().size() == 0) ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		runner.rnd = new Random() ;
		runner.inferTransitions = true ;
		runner.stopSuiteGenerationOnFailedOrViolation = false ;
		var results = runner.generate(
				dummy -> { mystate.G = new SimpleGame(); return agent ;}, 
				20, 
				10) ;
		assertTrue(mymodel.getAllTransitions().size() >= 2) ;
		System.out.println(mymodel) ;
	}
	
	//@Test
	public void test_genQ_1() {
		var mymodel = mkModel1() ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		runner.rnd = new Random() ;
		runner.QexploreProbability = 0 ;
		runner.actionSelectionPolicy = ACTION_SELECTION.Q ;
		
		var results = runner.generateTestSequence(agent,10) ;
		
		assertTrue(mystate.G.win) ;
		
		List<String> s1 = new LinkedList<>() ; s1.add("in-progress") ;
		List<String> s2 = new LinkedList<>() ; s2.add("win") ;
		var C1 = new MBTStateConfiguration(s1) ;
		var C2 = new MBTStateConfiguration(s2) ;
		var tr1 = new MBTTransition(C1,"move-forward",C1) ;
		var tr2 = new MBTTransition(C1,"move-forward",C2) ;
		var tr3 = new MBTTransition(C1,"turn-right",C1) ;
		
		assertTrue(runner.vtable.getAllTransitionsValues().get(tr1) < 1) ; 
		assertTrue(runner.vtable.getAllTransitionsValues().get(tr2) >= 1) ; 
		assertTrue(runner.vtable.getAllTransitionsValues().get(tr3) > 1) ; 
		
	}
	
	@Test
	public void test_genQ_2() {
		var mymodel = mkModel1() ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		runner.rnd = new Random() ;
		runner.actionSelectionPolicy = ACTION_SELECTION.Q ;
		runner.stopSuiteGenerationOnFailedOrViolation = false ;
		var results = runner.generate(
				dummy -> { mystate.G = new SimpleGame(); return agent ;}, 
				10, 
				10) ;
		//assertTrue(results.size() == 10) ;
		assertTrue(results.stream().allMatch(seq -> seq.size() <= 10)) ;
		assertTrue(runner.coveredStates.values().stream().filter(i -> i>0).count() ==2 ) ;
		assertTrue(runner.coveredTransitions.values().stream().filter(i -> i>0).count() >= 5 ) ;
		System.out.println(runner.showCoverage()) ;
	}

}
