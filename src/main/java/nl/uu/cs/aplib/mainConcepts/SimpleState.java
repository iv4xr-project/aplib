package nl.uu.cs.aplib.mainConcepts;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the root class for representing agents' states (an 'agent' is
 * an instance of {@link BasicAgent} or its subclasses). For most uses, you
 * will need to <b>extend/subclass</b> this class to enable your agents to track
 * whatever domain specific information that your agents need to track.
 * 
 * <p>
 * An instance of this class represents the semantical part of an agent's state.
 * By 'semantical' we mean that the instance is intended to contain information
 * that is relevant for solving the agent's goals. In particular, instances of
 * this class do not expose information relevant only for the agent's internal
 * control of its execution. The latter information is, and should be, hidden
 * from you.
 * 
 * <p>
 * As the root class, an instance of this class actually contains no domain
 * specific semantical information. This may sound contradictive with what we
 * just said above, but this is actually reasonable: there is no way to know
 * upfront what kind of problems you want to solve with agents, therefore we
 * cannot provide none either. If you need information that the agent should
 * track, you need to extend this class to introduce the needed fields to hold
 * that information. The exception to this is the following. Any instance S of
 * this class does get the following:
 * 
 * <ol>
 * <li>A message queue containing incoming messages (instances of
 * {@link nl.uu.cs.aplib.multiAgentSupport}) for the agent that owns S. The
 * queue itself is not exposed. Instead, the agent can access it through a
 * number of exposed methods, e.g. to check if a message of a certain type is
 * present in the queue and to pop it out.
 * 
 * <li>A pointer to the {@link nl.uu.cs.aplib.mainConcepts.Environment} used by
 * the agent, thus allowing the agent to inspect the Environment, and to send
 * commands to it.
 * </ol>
 * 
 * 
 *
 * @author wish
 *
 */
public class SimpleState {

    /**
     * A pointer to the {@link Environment} associated to the agent that owns this
     * state.
     */
    Environment env;

    /**
     * A logger. Don't set this logger yourself. When you attach this state to an
     * agent, the method {@link BasicAgent#attachState(SimpleState) will set this to
     * the right logger.
     */
    Logger logger;

    public SimpleState() {
    }

    /**
     * Every instance of this class requires an instance to the {@link Environment}
     * that is used by the agent that owns this state. This methods allows you to
     * set this state's pointer to the given environment.
     * 
     * @param env The Environment we want to associate with this state.
     * @return The method simply returns this state to allow it to be used in the
     *         Fluent Interface style.
     */
    public SimpleState setEnvironment(Environment env) {
        this.env = env;
        return this;
    }

    /**
     * This method should send an observe() command to the {@link Environment} 
     * (that is attached to this state) to send back a new observation from the
     * perspective of the specified agent. This method subsequently uses
     * this fresh observation to update itself.
     * 
     * <p>Since this SimpleState does not actually has any state information,
     * being just a 'simple' state, this method updateState() does not actually
     * update anything. Override this method to implement a more elaborate scheme of
     * updating state.
     */
    public void updateState(String agentId) {
        env.observe(agentId);
    };

    /**
     * Return the {@link Environment} associated with the state.
     */
    public Environment env() {
        return env;
    }

    /**
     * Write the string to the logger attached to this state, with the specified
     * logging level.
     */
    protected void log(Level level, String s) {
        if (logger == null)
            return;
        logger.log(level, s);
    }

}
