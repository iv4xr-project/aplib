package nl.uu.cs.aplib.Agents;

import nl.uu.cs.aplib.MainConcepts.SimpleState;
import nl.uu.cs.aplib.MultiAgentSupport.Messenger;

/**
 * An extension of {@link nl.uu.cs.aplib.MainConcepts.SimpleState} that also has
 * a {@link nl.uu.cs.aplib.MultiAgentSupport.Messenger}. Through this Messenger,
 * an agent that owns this state can send and receive messages to/from other
 * agents.
 * 
 * @author Wish
 *
 */
public class StateWithMessanger extends SimpleState {
	
	/**
	 * A {@link nl.uu.cs.aplib.MultiAgentSupport.Messenger} buffers incoming
	 * messages {@see Message} for the agent that owns this state. It also provides
	 * messages to inspect and retrieve these messages, as well as methods to send
	 * out messages to other agents.
	 */
	Messenger messenger = new Messenger() ;

	public StateWithMessanger() { super() ; }
	
	/**
	 * Return the messenger associated to this state.
	 * @return
	 */
	public Messenger messanger() {return messenger ; }
}
