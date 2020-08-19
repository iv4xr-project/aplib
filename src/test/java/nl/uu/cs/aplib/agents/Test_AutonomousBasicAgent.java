package nl.uu.cs.aplib.agents;

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
import nl.uu.cs.aplib.agents.AutonomousBasicAgent;
import nl.uu.cs.aplib.agents.StateWithMessenger;
import nl.uu.cs.aplib.agents.Test_InterAgentCommunication.MyState;
import nl.uu.cs.aplib.environments.ConsoleEnvironment;
import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.multiAgentSupport.ComNode;
import nl.uu.cs.aplib.multiAgentSupport.Message;
import nl.uu.cs.aplib.multiAgentSupport.Message.MsgCastType;

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
	
	static void createLogFile(String file) {
		try { Files.createFile(Paths.get(file)); }
		catch(Exception e) { }
	}
	
	static void deleteLogFile(String file) {
		try { Files.delete(Paths.get(file)); }
		catch(Exception e) { }
	}
	
	@Test
	public void test_pause_resume_stop(){
		// Testing pause, resume, and stop functionality.
		
		// This test uses timeout etc .. not ideal, but at the moment I cant think of
		// a better alternative, short of mocking the entire Thread.sleep mechanism.., which
		// is doable, but is also quite a hassle to do
		
		Logging.attachFileAsLogHandler("mylog.txt");
		
		var state = (MyState) (new MyState().setEnvironment(new Environment())) ;
		var agent = new AutonomousBasicAgent("agent","sentinel") 
				    . attachState(state) 
				    . setSamplingInterval(100)
				    ;
		
		var a0 = action("a0")
				 . do1((MyState S)-> { S.counter++ ; return S.counter ; })
				 . lift() ;
		
		var g = goal("g").toSolve((Integer x) -> false).withTactic(a0) . lift() ;
		
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
		deleteLogFile("mylog.txt") ;
	}
	
	@Test
	public void test_resume_by_msg(){
		
		//createLogFile("mylog1.txt") ;
		
		Logging.attachFileAsLogHandler("mylog1.txt");
		
		var comNode = new ComNode() ;
		var state = (MyState) (new MyState().setEnvironment(new Environment())) ;
		var agent1 = new AutonomousBasicAgent("agent1","sentinel") 
				    . attachState(state) 
				    . setSamplingInterval(100)
				    . registerTo(comNode) ;
		
		var agent2 = new AutonomousBasicAgent("neo","programmer") 
				     .attachState(new MyState().setEnvironment(new Environment())) 
				     .registerTo(comNode) ;
		
		var a0 = action("a0")
				 . do1((MyState S)-> { S.counter++ ; return S.counter ; })
				 . lift() ;
		
		var g = goal("g").toSolve((Integer x) -> false).withTactic(a0) . lift() ;
		
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
		System.out.println(">>>"  + log) ;
		assertTrue(log.contains("agent1 is stopping")) ;
		
		// clean up
		deleteLogFile("mylog1.txt") ;
	}
	
	@Test
	public void test_setgoal_awaken_suspendedAgent() {
		// test that an agent that is suspended because it has no goal will indeed be awaken
		// by setgoal
        Logging.attachFileAsLogHandler("mylog2.txt");
		
		var state = (MyState) (new MyState().setEnvironment(new Environment())) ;
		var agent = new AutonomousBasicAgent("agent","sentinel") 
				    . attachState(state) 
				    . setSamplingInterval(100)
				    ;
		
		var a0 = action("a0")
				 . do1((MyState S)-> { S.counter++ ; return S.counter ; })
				 . lift() ;
		
		var g = goal("g").toSolve((Integer x) -> false).withTactic(a0) . lift() ;
		
		// run the agent autonomously:
		new Thread(() -> agent.loop()) . start() ;
		
		sleepx(1000) ;
		agent.setGoal(g) ;
		sleepx(1000) ;
		agent.stop() ; 
		sleepx(1000) ;
		
		String log = readTxtFile("mylog2.txt") ;
		assertTrue(log.contains("agent identifies a goal")) ;
		assertTrue(log.contains("agent is stopping")) ;
		
		// cleanup
		deleteLogFile("mylog2.txt") ;
	}
	
	@Test
	public void test_waitUntilTheGoalIsConcluded() {
		
		var state = (MyState) (new MyState().setEnvironment(new Environment())) ;
		var agent = new AutonomousBasicAgent("agent","sentinel") 
				    . attachState(state) 
				    . setSamplingInterval(100)
				    ;
		
		var a0 = action("a0")
				 . do1((MyState S)-> { S.counter++ ; return S.counter ; })
				 . lift() ;
		
		var g = goal("g").toSolve((Integer x) -> x==10).withTactic(a0) . lift() ;
		
		// run the agent autonomously:
		new Thread(() -> agent.loop()) . start() ;
		
		agent.setGoal(g) ;
		
		agent.waitUntilTheGoalIsConcluded() ;
		assertTrue(g.getStatus().success()) ;
		agent.stop() ; 
						
	}
	
	@Test
	public void test_awakening_betweenTicksSleep() {
		// test that when an agent sleeps between ticks, a msg will awaken it
		
		var comNode = new ComNode() ;
		var state = (MyState) (new MyState().setEnvironment(new Environment())) ;
		var agent1 = new AutonomousBasicAgent("agent1","sentinel") 
				    . attachState(state) 
				    . setSamplingInterval(60000) // set a very long between-ticks sleep
				    . registerTo(comNode) ;
		
		var agent2 = new AutonomousBasicAgent("neo","programmer") 
				     .attachState(new MyState().setEnvironment(new Environment())) 
				     .registerTo(comNode) ;
		
		var a0 = action("a0")
				 . do1((MyState S)-> { S.counter++ ; return S.counter ; })
				 . lift() ;
		
		var g = goal("g").toSolve((Integer x) -> x==2).withTactic(a0) . lift() ;
		
		agent1.setGoal(g) ;
		
		// run the agent autonomously, it should enter a long sleep at the end of the first update()
		new Thread(() -> agent1.loop()) . start() ;
		sleepx(1000) ;
		// now test if a msg awaken it:
		agent2.messenger().send("neo",0, MsgCastType.SINGLECAST, "agent1","blabla") ;
		// agent1 should now be awaken, does its 2nd update(), and solve the goal
		sleepx(1000) ;
		assertTrue(g.getStatus().success()) ;
		
		agent1.stop();
	}
	
}
