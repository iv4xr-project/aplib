package nl.uu.cs.aplib.agents;

import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.multiAgentSupport.Messenger;

/**
 * An extension of {@link nl.uu.cs.aplib.mainConcepts.SimpleState} that also has
 * a {@link nl.uu.cs.aplib.multiAgentSupport.Messenger} and possibility to
 * attach a Prolog engine. Through the Messenger, an agent that owns this state
 * can send and receive messages to/from other agents. If a Prolog engine is
 * attaches, an agent can add facts and rules to it to do reasoning.
 * 
 * @author Wish
 *
 */
public class State extends SimpleState {

    /**
     * A {@link nl.uu.cs.aplib.multiAgentSupport.Messenger} buffers incoming
     * messages {@see Message} for the agent that owns this state. It also provides
     * messages to inspect and retrieve these messages, as well as methods to send
     * out messages to other agents.
     */
    Messenger messenger = new Messenger();

    /**
     * A {@link nl.uu.cs.aplib.agents.PrologReasoner}, which is a prolog engine. An
     * agent can add facts and rules to this engine, and then use it to do
     * inference. The default to set this field to null.
     */
    PrologReasoner prolog;

    public State() {
        super();
    }

    /**
     * Return the messenger associated to this state.
     */
    public Messenger messenger() {
        return messenger;
    }

    /**
     * Create a Prolog engine and attach it to this state. An agent can add facts
     * and rules to this engine, and then use it to do inference.
     */
    public State attachProlog() {
        prolog = new PrologReasoner();
        return this;
    }

    /**
     * Obtain the prolog-engine attached to this state (if there is one attached,
     * else you get null).
     */
    public PrologReasoner prolog() {
        return prolog;
    }

}
