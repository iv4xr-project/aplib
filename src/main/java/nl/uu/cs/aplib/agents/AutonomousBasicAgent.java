package nl.uu.cs.aplib.agents;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.logging.Level;

import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.multiAgentSupport.ComNode;
import nl.uu.cs.aplib.multiAgentSupport.Message;
import nl.uu.cs.aplib.multiAgentSupport.Messenger;
import nl.uu.cs.aplib.utils.Time;

/**
 * An extension of {@link nl.uu.cs.aplib.mainConcepts.BasicAgent} that can be
 * run as autonomous agents. Agents of this class moreover have the ability to
 * send messages to other agents.
 * 
 * Do note that an AutonomousBasicAgent requires a state of type {@link State} .
 * 
 * @author Wish
 *
 */
public class AutonomousBasicAgent extends BasicAgent {

    /**
     * The time interval in ms between two ticks. This agent will strive to time the
     * next tick to be invoked {@code samplingInterval}-ms after the start of the
     * current tick. The accuracy of this timing depends on Java implementation of
     * timer. If the current tick executes longer than {@code samplingInterval}-ms
     * the next tick will happen immediately after the current tick ends (but the
     * current tick will not be aborted).
     */
    protected long samplingInterval = 1000; // in ms

    public static enum Command {
        PAUSE, STOP
    }

    protected Command cmd = null;

    protected ComNode comNode = null;

    protected final ReentrantLock lock = new ReentrantLock();
    protected final Condition triggerArrived = lock.newCondition();
    protected final Condition goalConcluded = lock.newCondition();
    protected Thread thisAgentThread = null;

    /**
     * Create a plain instance of AutonomousBasicAgent. To be useful you will need
     * to add few other things to it, e.g. a state and a goal.
     */
    public AutonomousBasicAgent() {
        super();
    }

    /**
     * Create a plain instance of AutonomousBasicAgent with the given id and role.
     * To be useful you will need to add few other things to it, e.g. a state and a
     * goal.
     */
    public AutonomousBasicAgent(String id, String role) {
        super(id, role);
    }

    /**
     * Return the {@link nl.uu.cs.aplib.multiAgentSupport.Messenger} associated to
     * this agent.
     */
    Messenger messenger() {
        return ((State) state).messenger;
    }

    /**
     * Register this agent to the given
     * {@link nl.uu.cs.aplib.multiAgentSupport.ComNode}. Agents need to register to
     * a ComNode first before it is able to send to or receive messages from other
     * agents. The method returns this agent itself, so that it can be used in the
     * Fluent Interface style.
     */
    public AutonomousBasicAgent registerTo(ComNode comNode) {
        if (state == null)
            throw new IllegalArgumentException();
        if (!(state instanceof State))
            throw new IllegalArgumentException();
        messenger().attachCommuniationNode(comNode);
        this.comNode = comNode;
        comNode.register(this);
        return this;
    }

    GoalStructure shadowg_;

    @Override
    public AutonomousBasicAgent setGoal(GoalStructure g) {
        if (thisAgentThread != null) {
            // awaken the agent, if it is sleeping
            thisAgentThread.interrupt();
        }
        lock.lock();
        try {
            super.setGoal(g);
            shadowg_ = g;
            triggerArrived.signal();
            return this;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public AutonomousBasicAgent attachState(SimpleState state) {
        if (!(state instanceof State))
            throw new IllegalArgumentException("You need an instance of StateWithMessanger");
        super.attachState(state);
        return this;
    }

    public AutonomousBasicAgent attachState(State state) {
        super.attachState(state);
        return this;
    }

    /**
     * Set the agent sampling interval in ms. The method returns this agent itself,
     * so that it can be used in the Fluent Interface style.
     */
    public AutonomousBasicAgent setSamplingInterval(long interval) {
        samplingInterval = interval;
        return this;
    }

    /**
     * If the agent is already running autonomously, invoking this method will cause
     * it to pause. While it is paused, it will not trigger any tick. User
     * {@link #resume()} to make the agent resuming its autonomous run. An incoming
     * message will also cause the agent to resume.
     */
    public void pause() {
        cmd = Command.PAUSE;
    }

    private void awakeThisAgentFromSleep() {
        if (thisAgentThread != null) {
            // awaken the agent, if it is sleeping
            thisAgentThread.interrupt();
        }
    }

    /**
     * If this agent is sleeping (e.g. while waiting until it is time to do its next
     * tick), and it was paused ({@link #pause() was called}, this method will cause
     * the agent to resume its work.
     */
    public void resume() {
        awakeThisAgentFromSleep();
        lock.lock();
        try {
            cmd = null;
            triggerArrived.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * If the agent is running autonomously, this command will cause the agent to
     * stop its run. This command will not interrupt the agent if it is currently
     * executing an {@link nl.uu.cs.aplib.mainConcepts.Action}. Once the execution
     * of the Action is finished, the agent will respond to this stop command,
     * causing it to exit its autonomous loop.
     */
    public void stop() {
        awakeThisAgentFromSleep();
        cmd = Command.STOP;
    }

    /**
     * This will send a message to this agent. Invoking this method will cause the
     * agent to be awaken from its sleep, or if it was paused, it will be
     * unpaused/resumed. Normally this method is called by a
     * {@link nl.uu.cs.aplib.multiAgentSupport.ComNode} to send a message to this
     * agent on behalf of another agent. However, since this method is public,
     * whatever the application around this agent can also use it to directly send a
     * message to this agent.
     */
    public void sendMsgToThisAgent(Message m) {
        awakeThisAgentFromSleep();
        lock.lock();
        try {
            if (cmd == Command.STOP) {
                // if the agent has been commanded to stop then discard the msg
                return;
            }
            messenger().put(m);
            cmd = null; // set cmd to resume, in case it was on pause
            triggerArrived.signal(); // awaken the agent
        } finally {
            lock.unlock();
        }
    }

    /**
     * When this is called, the calling thread will be paused until the current
     * goal-tree is concluded (which can be either with success or as fail). The
     * method will return the goal-tree.
     */
    public GoalStructure waitUntilTheGoalIsConcluded() {

        if (shadowg_ == null)
            return null;

        log(Level.INFO, "Thread " + Thread.currentThread().getId() + " is waiting for agent " + id
                + " to close its current goal.");

        lock.lock();
        try {
            while (shadowg_ == null || shadowg_.getStatus().inProgress()) {
                try {
                    goalConcluded.await();
                } catch (InterruptedException e) {
                }
            }
            return shadowg_;
        } finally {
            log(Level.INFO, "Thread " + Thread.currentThread().getId() + " acquires a closed goal from agent " + id);
            lock.unlock();
        }
    }

    /**
     * This will run the agent in an infinite loop. The idea is to run this in a new
     * thread, e.g. as in:
     * 
     * <pre>
     * new Thread(() -> agent.loop()).start();
     * </pre>
     * 
     * The above code essentially cause the agent to run autonomously.
     * 
     * If there is no goal however, this method will pause, until there is one.
     * Then, it will proceed by invoking update() periodically. The time between
     * update is specified by the field samplingInterval. The agent will strive to
     * keep the sampling interval around that specified time, but it does not commit
     * to keep it accurately.
     * 
     * If given a goal, and if after sometime the goal is solved (or failed), the
     * agent will detach the goal. If there are other threads that called
     * waitUntilTheGoalIsConcluded(), these will first be awaken. Then this method
     * launch() will sleep again, waiting for a new goal, and the above cycle
     * repeats again.
     * 
     * There are methods in this class to command the agent to pause, to resume
     * again, and to stop all together.
     */
    public void loop() {
        try {
            loopWorker();
        } catch (Throwable t) {
            log(Level.WARNING, "Agent " + id + " aborts its loop due to exception " + t);
        } finally {
        }
    }

    private void loopWorker() {

        thisAgentThread = Thread.currentThread();
        log(Level.INFO, "Agent " + id + " enters its loop on Thread " + thisAgentThread.getId());

        Time time = new Time();

        // repeat forever:
        while (cmd != Command.STOP) {
            lock.lock();
            try {
                // wait until the goal is not null:
                while (goal == null && cmd != Command.STOP) {
                    log(Level.INFO, "Agent " + id + " is blocking, waiting for a goal.");
                    try {
                        triggerArrived.await();
                    } catch (InterruptedException e) {
                    }
                }
                if (goal != null)
                    log(Level.INFO,
                            "Agent " + id + " identifies a goal and starts working on it. Budget: " + goal.getBudget());
                while (goal != null && cmd != Command.STOP) {
                    while (cmd == Command.PAUSE) {
                        log(Level.INFO, "Agent " + id + " is paused.");
                        try {
                            triggerArrived.await();
                            ;
                        } catch (InterruptedException e) {
                        }
                    }
                    time.sample();
                    update();
                    if (goal == null) {
                        log(Level.INFO, "Agent " + id + " closed the current goal: " + shadowg_.getStatus() + ".");
                        // the goal is solved then
                        goalConcluded.signalAll();
                        break;
                    }
                    long sleeptime = samplingInterval - time.elapsedTimeSinceLastSample();
                    // if needed, just sleep until it is time to do the next sampling:
                    if (sleeptime > 100) {
                        try {
                            // Thread.sleep(sleeptime);
                            triggerArrived.await(sleeptime, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                        }
                    }
                    // System.err.println(">>> update time: " + time.elapsedTimeSinceLastSample()) ;
                }
            } finally {
                // goalConcluded.signalAll();
                lock.unlock();
            }
            // break ;
            /*
             * try { System.err.println("## starting long sleep...") ;
             * Thread.sleep(3600000); } catch(InterruptedException e) { }
             */
        }
        log(Level.INFO, "Agent " + id + " is stopping...");
        /*
         * lock.lock(); try { if(cmd == Command.STOP && goal != null) {
         * setTopGoalToFail("The agent is stopping") ; goalConcluded.signalAll(); } }
         * finally { lock.unlock(); }
         */
    }

}
