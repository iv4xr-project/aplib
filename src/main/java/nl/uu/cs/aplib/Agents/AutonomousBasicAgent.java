package nl.uu.cs.aplib.Agents;

import java.util.concurrent.locks.*;
import java.util.function.Predicate;

import nl.uu.cs.aplib.MainConcepts.*;
import nl.uu.cs.aplib.MultiAgentSupport.ComNode;
import nl.uu.cs.aplib.MultiAgentSupport.Message;
import nl.uu.cs.aplib.Utils.Time;

public class AutonomousBasicAgent extends BasicAgent {
	
	long samplingInterval = 1000 ; // in ms
	
	final Lock lock = new ReentrantLock();	
	final Condition triggerArrived = lock.newCondition(); 
	final Condition goalConcluded  = lock.newCondition(); 
	
	static enum Command { PAUSE, STOP }
	
	Command cmd = null ;
	
	ComNode comNode = null ;

	
	public synchronized void sendMsgToThisAgent(Message m) {
		if (cmd == Command.STOP) {
			// if the agent has been commanded to stop then discard the msg
			return ;
		}
		var msgQueue = getIncomingMsgQueue() ;
		msgQueue.add(m) ;
		cmd = null ; // set cmd to resume, in case it was on pause
		triggerArrived.signal();  // awaken the agent
	}
	
	public AutonomousBasicAgent registerToComNode(ComNode comNode) {
		this.comNode = comNode ;
		comNode.register(this) ;
		return this ;
	}
	
	
	public void pause() { cmd = Command.PAUSE ; }
	
	public void resume() { cmd = null ; triggerArrived.signal(); }
	
	public void stop() { cmd = Command.STOP ; }
	
	
	
	GoalTree shadowg_ ;
	
	public BasicAgent setGoal(GoalTree g) {
		super.setGoal(g) ;
		shadowg_ = g ;
		triggerArrived.signal(); 
		return this ;
	}
	
	/**
	 * When this is called, the calling thread will be paused until the current goal-tree is
	 * concluded (which can be either with success or as fail). The method will return
	 * the goal-tree.
	 */
	public GoalTree waitUntilTheGoalIsConcluded() {
		if (shadowg_ == null) return null ;
		while (shadowg_ == null || shadowg_.getStatus().inProgress()) {
			try { goalConcluded.await(); }
			catch(InterruptedException e) { }
		}
		return shadowg_ ;
	}
	
	/**
	 * This will run the agent in an infinite loop. The idea is run this in a new thread. 
	 * If there is no goal however, this method will pause, until there is one. Then,
	 * it will proceed by invoking update() periodically. The time between update is
	 * specified by the field samplingInterval. The agent will strive to keep the sampling
	 * interval around that specified time, but it does not commit to keep it accurately.
	 * 
	 * If given a goal, and if after sometime the goal is solved (or failed), the agent will
	 * detach the goal. If there are other threads that called 
	 * waitUntilTheGoalIsConcluded(), these will first be awaken. Then this method launch()
	 * will sleep again, waiting for a new goal, and the above cycle repeats again.
	 * 
	 * There are methods in this class to command the agent to pause, to resume again, and
	 * to stop all together. 
	 */
	public void launch() {
		Time time = new Time() ;

		//repeat forever:		
		while(cmd != Command.STOP) {
			// wait until the goal is not null:
			while(goal == null) {
				try { triggerArrived.await(); }
				catch(InterruptedException e) { }
			}
			
			while(cmd != Command.STOP) {
				while(cmd == Command.PAUSE) {
					try { triggerArrived.await(); }
					catch(InterruptedException e) { }
				}
				time.sample(); 
				update() ;
				if (goal == null) {
					// the goal is solved then
					goalConcluded.signalAll();
					break ;
				}
				long sleeptime = samplingInterval - time.elapsedTimeSinceLastSample() ;
				// if needed, just sleep until it is time to do the next sampling:
				if (sleeptime>100) {
					try { Thread.sleep(sleeptime); }  // TRICKY... this is not interruptible by condition.wait() !
					catch(InterruptedException e) {
						// somebody else interrupts the sleep... well ok, we can just as well resume
						// Note also that Condition.signal() will not cause this interruption,
						// since here we are not waiting for any condition.
					}
				}
			}
		}
		if(cmd == Command.STOP && goal != null) {
			setTopGoalToFail("The agent is stopping") ;		
		} 
	}

}
