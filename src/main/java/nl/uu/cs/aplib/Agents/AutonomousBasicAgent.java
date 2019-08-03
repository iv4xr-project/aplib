package nl.uu.cs.aplib.Agents;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.function.Predicate;
import java.util.logging.Level;

import nl.uu.cs.aplib.MainConcepts.*;
import nl.uu.cs.aplib.MultiAgentSupport.ComNode;
import nl.uu.cs.aplib.MultiAgentSupport.Message;
import nl.uu.cs.aplib.MultiAgentSupport.Messenger;
import nl.uu.cs.aplib.Utils.Time;

public class AutonomousBasicAgent extends BasicAgent {
	
	protected long samplingInterval = 1000 ; // in ms
	
	public static enum Command { PAUSE, STOP }
	
	protected Command cmd = null ;
	
	protected ComNode comNode = null ;
	
	protected final ReentrantLock lock = new ReentrantLock();	
	protected final Condition triggerArrived = lock.newCondition(); 
	protected final Condition goalConcluded  = lock.newCondition(); 
	protected Thread thisAgentThread = null ;
	
	public AutonomousBasicAgent() {
		super() ;
	}
	
	public AutonomousBasicAgent(String id, String role) {
		super(id,role) ;
	}
	
	Messenger messenger() {
		return ((StateWithMessanger) state).messenger ;
	}
	
	public AutonomousBasicAgent registerTo(ComNode comNode) {
		if (state == null) throw new IllegalArgumentException() ;
		if (! (state instanceof StateWithMessanger)) throw new IllegalArgumentException() ;
		messenger().attachCommuniationNode(comNode);
		this.comNode = comNode ;
		comNode.register(this);
		return this ;
	}
	
	GoalTree shadowg_ ;
	
	@Override
	public AutonomousBasicAgent setGoal(GoalTree g) {
		if (thisAgentThread !=null) {
			// awaken the agent, if it is sleeping
			thisAgentThread.interrupt();
		}
		lock.lock();
		try {
			super.setGoal(g) ;
			shadowg_ = g ;
			triggerArrived.signal(); 
			return this ;
		}
		finally { lock.unlock(); }
	}
	
	@Override
	public AutonomousBasicAgent attachState(SimpleState state) {
		if (! (state instanceof StateWithMessanger)) 
			throw new IllegalArgumentException("You need an instance of StateWithMessanger") ;
		super.attachState(state) ;
		return this ;
	}
	
	public AutonomousBasicAgent attachState(StateWithMessanger state) {
		super.attachState(state) ;
		return this ;
	}
	
	/**
	 * Set the agent sampling interval in ms.
	 */
	public AutonomousBasicAgent setSamplingInterval(long interval) {
		samplingInterval = interval ; return this ;
	}
	
	public void pause() { cmd = Command.PAUSE ; }
	
	private void awakeThisAgentFromSleep() {
		if (thisAgentThread !=null) {
			// awaken the agent, if it is sleeping
			thisAgentThread.interrupt();
		}
	}
	
	public void resume() { 
		awakeThisAgentFromSleep() ;
		lock.lock();
		try {
			cmd = null ; triggerArrived.signal(); 
		}
		finally { lock.unlock(); }
	}
	
	public void stop() { 
		awakeThisAgentFromSleep() ;
		cmd = Command.STOP ; 
	}
	
	public void sendMsgToThisAgent(Message m) {
		awakeThisAgentFromSleep() ;
		lock.lock();
		try {
			if (cmd == Command.STOP) {
				// if the agent has been commanded to stop then discard the msg
				return ;
			}
			messenger().put(m);
			cmd = null ; // set cmd to resume, in case it was on pause
			triggerArrived.signal();  // awaken the agent
		}
		finally { lock.unlock(); }
	}
	/**
	 * When this is called, the calling thread will be paused until the current goal-tree is
	 * concluded (which can be either with success or as fail). The method will return
	 * the goal-tree.
	 */
	public GoalTree waitUntilTheGoalIsConcluded() {
		
		if (shadowg_ == null) return null ;
		
		log(Level.INFO,"Thread " + Thread.currentThread().getId() 
				       + " is waiting for agent " + id + " to close its current goal.") ;
		
		lock.lock();
		try {
			while (shadowg_ == null || shadowg_.getStatus().inProgress()) {
				try { 
					goalConcluded.await(); 
				}
				catch(InterruptedException e) { }
			}
			return shadowg_ ;
		}
		finally { 
			log(Level.INFO,"Thread " + Thread.currentThread().getId() + " acquires a closed goal from agent " + id) ;
			lock.unlock(); 
		}
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
	public void loop() {
		try {
			loopWorker() ;
		}
		catch(Throwable t) {
			log(Level.WARNING,"Agent " + id + " abort its loop due to exception " +  t) ;		
		}
		finally { }
	}
	
	private void loopWorker() {
		
		thisAgentThread = Thread.currentThread() ;
		log(Level.INFO,"Agent " + id + " enters its loop on Thread " +  thisAgentThread.getId()) ;
		
		Time time = new Time() ;

		//repeat forever:		
		while(cmd != Command.STOP) {
			lock.lock();
			try {
				// wait until the goal is not null:
				while(goal == null && cmd != Command.STOP) {
					log(Level.INFO,"Agent " + id + " is blocking, waiting for a goal.") ;
					try { triggerArrived.await(); }
					catch(InterruptedException e) { }
				}
				if (goal != null) 
					log(Level.INFO,"Agent " + id + " identifies a goal and starts working on it. Budget: " + goal.getBudget()) ;
				while(goal != null && cmd != Command.STOP) {
					while(cmd == Command.PAUSE) {
						log(Level.INFO,"Agent " + id + " is paused.") ;
						try { triggerArrived.await(); ; }
						catch(InterruptedException e) { }
					}
					time.sample(); 
					update() ;
					if (goal == null) {
						log(Level.INFO,"Agent " + id + " closed the current goal: " + shadowg_.getStatus() + ".") ;
						// the goal is solved then
						goalConcluded.signalAll();
						break ;
					}
					long sleeptime = samplingInterval - time.elapsedTimeSinceLastSample() ;
					// if needed, just sleep until it is time to do the next sampling:
					if (sleeptime>100) {
						try { 
							//Thread.sleep(sleeptime); 
							triggerArrived.await(sleeptime, TimeUnit.MILLISECONDS) ;
						}  
						catch(InterruptedException e) { }
					}
					System.err.println(">>> update time: " + time.elapsedTimeSinceLastSample()) ;
				}
			}
			finally { 
				//goalConcluded.signalAll();
				lock.unlock(); 
			}	
			// break ;
			/*
			try {
				System.err.println("## starting long sleep...") ;
				Thread.sleep(3600000);
			}
			catch(InterruptedException e) { }
			*/
		}
		log(Level.INFO,"Agent " + id + " is stopping...") ;
		/*
		lock.lock();
		try {
			if(cmd == Command.STOP && goal != null) {
				setTopGoalToFail("The agent is stopping") ;	
				goalConcluded.signalAll();
			} 
		}
		finally { lock.unlock(); }	
		*/
	}

}
