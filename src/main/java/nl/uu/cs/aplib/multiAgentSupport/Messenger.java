package nl.uu.cs.aplib.multiAgentSupport;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import nl.uu.cs.aplib.multiAgentSupport.Message.MsgCastType;

/**
 * Providing incoming message queue for agents, methods to inspect and retrieve
 * messages from this queue, and also methods to send out messages to a ComNode.
 * 
 * @author Wish
 *
 */
public class Messenger {

    List<Message> incomingMsgs = new LinkedList<Message>();
    ComNode comNode;

    public Messenger() {
    }

    public void attachCommuniationNode(ComNode comNode) {
        this.comNode = comNode;
    }

    /**
     * Find a message in this Messenger's input queue that satisfies the predicate
     * p. It returns the first one that satisfies it. The message is not removed
     * from the queue. If none is found, null is returned.
     */
    synchronized public Message find(Predicate<Message> p) {
        for (Message m : incomingMsgs) {
            if (p.test(m))
                return m;
        }
        return null;
    }

    /**
     * Check this Messenger's input queue if it contains a message satisfying the
     * predicate p. It returns the first one that satisfies it.
     */
    synchronized public boolean has(Predicate<Message> p) {
        return find(p) != null;
    }

    synchronized public boolean empty() {
        return incomingMsgs.isEmpty();
    }

    synchronized public int size() {
        return incomingMsgs.size();
    }

    /**
     * Find a message in this Messenger's input queue that satisfies the predicate
     * p. It returns the first one that satisfies it <b>and removes</b> it from the
     * queue. If none is found, null is returned.
     */
    synchronized public Message retrieve(Predicate<Message> p) {
        Message m = find(p);
        if (m != null)
            incomingMsgs.remove(m);
        return m;
    }

    /**
     * Put the message m in the input message queue.
     */
    synchronized public void put(Message m) {
        incomingMsgs.add(m);
    }

    /**
     * Send out this message to the {@link ComNode} associated to this Messenger.
     */
    public Acknowledgement send(Message m) {
        return comNode.send(m.timestamp());
    }

    public Acknowledgement send(String idSource, int priority, MsgCastType castType, String idTarget, String msgName,
            Object... args) {
        var m = new Message(idSource, priority, castType, idTarget, msgName, args);
        return send(m);
    }
}
