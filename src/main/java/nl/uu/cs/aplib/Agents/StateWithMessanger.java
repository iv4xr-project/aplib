package nl.uu.cs.aplib.Agents;

import nl.uu.cs.aplib.MainConcepts.SimpleState;
import nl.uu.cs.aplib.MultiAgentSupport.Messenger;

public class StateWithMessanger extends SimpleState {
	
	/**
	 * A {@link Messenger} buffers incoming messages {@see Message} for the agent that
	 * owns this state. It also provides messages to inspect and retrieve these messages,
	 * as well as methods to send out messages to other agents.
	 */
	Messenger messenger = new Messenger() ;

	
	/**
	 * Return the messenger associated to this state.
	 * @return
	 */
	public Messenger messanger() {return messenger ; }
}
