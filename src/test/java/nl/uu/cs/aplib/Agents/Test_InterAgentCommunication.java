package nl.uu.cs.aplib.Agents;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import nl.uu.cs.aplib.Environments.ConsoleEnvironment;
import nl.uu.cs.aplib.MainConcepts.*;
import nl.uu.cs.aplib.MultiAgentSupport.ComNode;
import nl.uu.cs.aplib.MultiAgentSupport.Message.MsgCastType;


public class Test_InterAgentCommunication {
	
	static class MyState extends StateWithMessanger {
		int counter = 0 ;
		@Override
		public MyState setEnvironment(Environment env) {
			super.setEnvironment(env) ;
			return this ;
		}
	}
	
	@Test
	public void test1() {
		var env = new ConsoleEnvironment() ;
		var comNode = new ComNode() ;
		
		var state1 = new MyState().setEnvironment(env) ;
		var agent1 = new AutonomousBasicAgent("D1","teacher")
				    . attachState(state1)
				    . registerTo(comNode) ;
		var a0 = action("a0")
				 . do_((MyState S)->actionState-> { 
					 S.messenger.send("D1",0, MsgCastType.SINGLECAST, "P1","SC") ;
					 return 0 ;
				 })
				 . lift() ;
		
		var a1 = action("a1")
				 . do_((MyState S)->actionState-> { 
					 S.messenger.send("D1",0, MsgCastType.BROADCAST, "P1","BC") ;
					 return 0 ;
				 })
				 . lift() ;
		
		var a2 = action("a2")
				 . do_((MyState S)->actionState-> { 
					 S.messenger.send("D1",0, MsgCastType.ROLECAST, "student","RC") ;
					 return 0 ;
				 })
				 . lift() ;
		
		
		var g1 = goal("g1").toSolve((Integer x) -> false).withStrategy(SEQ(a0,a1,a2)) . lift() ;
		agent1.setGoal(g1) ;
		
		var state2 = new MyState().setEnvironment(env) ;	
		var agent2 = new AutonomousBasicAgent("P1","student")
				     . attachState(state2)
				     . registerTo(comNode) ;
		
		var state3 = new MyState().setEnvironment(env) ;	
		var agent3 = new AutonomousBasicAgent("P2","unknown")
				     . attachState(state3)
				     . registerTo(comNode) ;
		
		agent1.update() ;
		assertTrue(state2.messenger.size() == 1) ;
		assertTrue(state3.messenger.size() == 0) ;
		assertTrue(state2.messenger.test(M -> M.getMsgName().equals("SC"))) ;
		assertFalse(state3.messenger.test(M -> M.getMsgName().equals("SC"))) ;
		
		agent1.update();
		assertTrue(state2.messenger.size() == 2) ;
		assertTrue(state3.messenger.size() == 1) ;
		assertTrue(state2.messenger.test(M -> M.getMsgName().equals("BC"))) ;
		assertTrue(state3.messenger.test(M -> M.getMsgName().equals("BC"))) ;

		agent1.update();
		assertTrue(state2.messenger.size() == 3) ;
		assertTrue(state3.messenger.size() == 1) ;
		assertTrue(state2.messenger.test(M -> M.getMsgName().equals("RC"))) ;
		assertFalse(state3.messenger.test(M -> M.getMsgName().equals("RC"))) ;

	}

}
