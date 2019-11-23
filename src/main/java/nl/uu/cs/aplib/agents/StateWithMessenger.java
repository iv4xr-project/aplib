package nl.uu.cs.aplib.agents;

import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.multiAgentSupport.Messenger;

/**
 * An extension of {@link nl.uu.cs.aplib.mainConcepts.SimpleState} that also has
 * a {@link nl.uu.cs.aplib.multiAgentSupport.Messenger}. Through this Messenger,
 * an agent that owns this state can send and receive messages to/from other
 * agents.
 * 
 * @author Wish
 *
 */
public class StateWithMessenger extends SimpleState {
	
	/**
	 * A {@link nl.uu.cs.aplib.multiAgentSupport.Messenger} buffers incoming
	 * messages {@see Message} for the agent that owns this state. It also provides
	 * messages to inspect and retrieve these messages, as well as methods to send
	 * out messages to other agents.
	 */
	Messenger messenger = new Messenger() ;

	public StateWithMessenger() { super() ; }
	
	/**
	 * Return the messenger associated to this state.
	 */
	public Messenger messenger() {return messenger ; }
}
