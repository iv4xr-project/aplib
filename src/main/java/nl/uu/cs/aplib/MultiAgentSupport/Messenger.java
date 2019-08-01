package nl.uu.cs.aplib.MultiAgentSupport;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Providing incoming message queue for agents, methods to inspect and retrieve messages
 * from this queue, and also methods to send out messages to a ComNode.
 * 
 * @author Wish
 *
 */
public class Messenger {

	List<Message> incomingMsgs = new LinkedList<Message>() ;
	ComNode comNode ;
	
	public Messenger() { } 
	
	public void attachCommuniationNode(ComNode comNode) {
		this.comNode = comNode ;
	}
	
	synchronized public Message find(Predicate<Message> p) {
		for (Message m : incomingMsgs) {
			if (p.test(m)) return m ;
		}
		return null ;
	}
	
	synchronized public boolean test(Predicate<Message> p) {
		return find(p) != null ;
	}
	
	synchronized public Message findOneAndRetrieve(Predicate<Message> p) {
		Message m = find(p) ;
		if (m != null) incomingMsgs.remove(m) ;
		return m ;
	}
	
	/**
	 * Put the message m in the input message queue.
	 */
	synchronized public void put(Message m) { 
		incomingMsgs.add(m) ;
	}
	
	/**
	 * Send out this message to the {@link ComNode} associated to this Messenger.
	 */
	public Acknowledgement send(Message m) {
		return comNode.send(m) ;
	}
	
}
