package nl.uu.cs.aplib.multiAgentSupport;

import java.util.*;

import nl.uu.cs.aplib.agents.AutonomousBasicAgent;
import nl.uu.cs.aplib.multiAgentSupport.Acknowledgement.AckType;

/**
 * This class provides a 'communication node' for agents. It provides a simple
 * message routing between agents. To use it, agents first need to register
 * themselves to a ComNode. The ComNode maintains an address book that maps
 * agents' names to pointers to these agents. A plain
 * {@link nl.uu.cs.aplib.mainConcepts.BasicAgent} has no method to register to
 * ComNode. However, {@link nl.uu.cs.aplib.agents.AutonomousBasicAgent} can
 * register to a ComNode, but keep in mind that an AutonomousBasicAgent can only
 * be a member of at most one ComNode. Access to this ComNode is provided
 * through its agent state.
 * 
 * <p>
 * When an agent A wants to send a message to agent B, A only needs to know B's
 * name. A would then send the message to its ComNode, and the ComNode will take
 * care that the message is delivered to B. Note that without a ComNode every
 * agent will have to know the pointers to every other agents it needs to send
 * messages to. Furthermore, a ComNode allows a convenient way for an agent to
 * broadcast a message to all other agents (which are registered to the same
 * ComNode), or to all other agents with the same role.
 * 
 * @author Wish
 *
 */

public class ComNode {

    /**
     * Maping agents' names to their references/pointers.
     */
    Map<String, AutonomousBasicAgent> idMap = new HashMap<String, AutonomousBasicAgent>();

    /**
     * Mapping role-names to the set of agents with the same role.
     */
    Map<String, Set<AutonomousBasicAgent>> roleMap = new HashMap<String, Set<AutonomousBasicAgent>>();

    public ComNode() {
    }

    /**
     * Register the agent to this ComNode.
     */
    synchronized public void register(AutonomousBasicAgent agent) {
        idMap.put(agent.getId(), agent);
        var role = agent.getRole();
        var brothers = roleMap.get(role);
        if (brothers == null) {
            brothers = new HashSet<AutonomousBasicAgent>();
            brothers.add(agent);
            roleMap.put(role, brothers);
            return;
        }
        brothers.add(agent);
    }

    /**
     * Remove the agent from this ComNode.
     */
    synchronized public void deregister(AutonomousBasicAgent agent) {
        idMap.remove(agent);
        var role = agent.getRole();
        var brothers = roleMap.get(role);
        if (brothers == null)
            return;
        brothers.remove(agent);
    }

    /**
     * This is invoked by an agent to send a message to other agent(s), depending on
     * the message type. If the type is SINGLECAST this ComNode will forward it to
     * its specified target agent (just one can be targetted in a SINGLECAST). If it
     * is a BROADCAST the message will be forwarded to all agents registered to this
     * ComNode. If it is a ROLECAST the message will be forwarded to all agents with
     * the same role as the target role specified in the message.
     * 
     * @param msg The message to send.
     * @return An {@link nl.uu.cs.aplib.multiAgentSupport.Acknowledgement}. It is a
     *         negative acknowledgement (REJECTED) if the message is a SINGLECAST
     *         and its target does not exists. In all other cases the
     *         acknowledgement should be positive (SUCCESS).
     * 
     */
    public Acknowledgement send(Message msg) {
        String senderId = msg.idSource;
        var sender = idMap.get(senderId);
        if (sender == null) {
            // unknown sender!
            return new Acknowledgement(AckType.REJECTED, "Sender is not registered.");
        }
        switch (msg.castTy) {
        case SINGLECAST:
            var receiver = idMap.get(msg.idTarget);
            if (receiver == null) {
                return new Acknowledgement(AckType.REJECTED, "Receiver is not registered.");
            }
            receiver.sendMsgToThisAgent(msg);
            return new Acknowledgement(AckType.SUCCESS, null);
        case BROADCAST:
            for (AutonomousBasicAgent B : idMap.values()) {
                if (B != sender)
                    B.sendMsgToThisAgent(msg);
            }
            return new Acknowledgement(AckType.SUCCESS, null);
        case ROLECAST:
            var receivers = roleMap.get(msg.idTarget);
            if (receivers != null) {
                for (AutonomousBasicAgent B : receivers) {
                    if (B != sender)
                        B.sendMsgToThisAgent(msg);
                }
            }
            return new Acknowledgement(AckType.SUCCESS, null);
        }
        // should not happen
        return null;
    }

}
