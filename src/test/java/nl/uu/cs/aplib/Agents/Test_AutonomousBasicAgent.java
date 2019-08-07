package nl.uu.cs.aplib.Agents;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static nl.uu.cs.aplib.AplibEDSL.* ;

import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.Agents.Test_InterAgentCommunication.MyState;
import nl.uu.cs.aplib.Environments.ConsoleEnvironment;
import nl.uu.cs.aplib.MainConcepts.*;
import nl.uu.cs.aplib.MultiAgentSupport.ComNode;
import nl.uu.cs.aplib.MultiAgentSupport.Message;
import nl.uu.cs.aplib.MultiAgentSupport.Message.MsgCastType;

public class Test_AutonomousBasicAgent {
	
	static class MyState extends StateWithMessenger {
		int counter = 0 ;
	}
	
	void sleepx(long t) {
		try { Thread.sleep(t) ; } catch(Exception e) { }
	}
	
	static String readTxtFile(String fname) {
		try {
			return new String(Files.readAllBytes(Paths.get(fname)));
		}
		catch(Exception e) { return null ; }
	}
	
	
	@Test
	public void test_pause_resume_stop(){
		// Testing pause, resume, and stop functionality.
		
		// This test uses timeout etc .. not ideal, but at the moment I cant think of
		// a better alternative, short of mocking the entire Thread.sleep mechanism.., which
		// is doable, but is also quite a hassle to do
		
		Logging.attachLogFile("mylog.txt");
		
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent = new AutonomousBasicAgent("agent","sentinel") 
				    . attachState(state) 
				    . setSamplingInterval(100)
				    ;
		
		var a0 = action("a0")
				 . do_((MyState S)->actionState-> { S.counter++ ; return S.counter ; })
				 . lift() ;
		
		var g = goal("g").toSolve((Integer x) -> false).withStrategy(a0) . lift() ;
		
		agent.setGoal(g) ;
		
		// run the agent autonomously:
		new Thread(() -> agent.loop()) . start() ;
		while(state.counter == 0) {
			try {
				Thread.sleep(100);
			}
			catch(Exception e) { }
		}
		
		// command the agent to pause, then to resume, then to stop
		agent.pause() ; sleepx(1000) ;

		agent.resume();
		var cnt0 = state.counter ;
		while(state.counter < cnt0+5) {
			try {
				Thread.sleep(100);
			}
			catch(Exception e) { }
		}
		agent.stop() ; sleepx(1000) ;
		
		String log = readTxtFile("mylog.txt") ;
		assertTrue(log.contains("agent is paused")) ;
		assertTrue(log.contains("agent is stopping")) ;
		
		// clean up
		try { Files.delete(Paths.get("mylog.txt")); }
		catch(Exception e) { }
	}
	
	@Test
	public void test_resume_by_msg(){
		
		Logging.attachLogFile("mylog1.txt");
		
		var comNode = new ComNode() ;
		var state = (MyState) (new MyState().setEnvironment(new ConsoleEnvironment())) ;
		var agent1 = new AutonomousBasicAgent("agent1","sentinel") 
				    . attachState(state) 
				    . setSamplingInterval(100)
				    . registerTo(comNode) ;
		
		var agent2 = new AutonomousBasicAgent("neo","programmer") 
				     .attachState(new MyState().setEnvironment(new ConsoleEnvironment())) 
				     .registerTo(comNode) ;
		
		var a0 = action("a0")
				 . do_((MyState S)->actionState-> { S.counter++ ; return S.counter ; })
				 . lift() ;
		
		var g = goal("g").toSolve((Integer x) -> false).withStrategy(a0) . lift() ;
		
		agent1.setGoal(g) ;
		
		// run the agent autonomously:
				new Thread(() -> agent1.loop()) . start() ;
				while(state.counter == 0) {
					try {
						Thread.sleep(100);
					}
					catch(Exception e) { }
				}
				
		// command the agent to pause, then awaken it with msg
		agent1.pause() ; sleepx(1000) ;
		agent2.messenger().send("neo",0, MsgCastType.SINGLECAST, "agent1","blabla") ;
		assertTrue(agent1.messenger().size() == 1) ;
		var cnt0 = state.counter ;
		while(state.counter < cnt0+5) {
			try {
				Thread.sleep(100);
			}
			catch(Exception e) { }
		}
		agent1.stop() ; sleepx(1000) ;
		
		String log = readTxtFile("mylog1.txt") ;
		assertTrue(log.contains("agent1 is stopping")) ;
		
		// clean up
		try { Files.delete(Paths.get("mylog1.txt")); }
		catch(Exception e) { }
	}

}
