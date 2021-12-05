package nl.uu.cs.aplib.multiAgentSupport;

//import java.util.Comparator;
import java.util.Date;

/**
 * The class defines the messages that agents send to each other.
 *
 * @author Wish
 */
public class Message {

    /**
     * Different types of {@link Message}: SINGLECASR, ROLECAST, BROADCAST. A
     * singlecast message is to be sent to single target agent. A broadcast message
     * is to be sent to all agents (registered to the same {@link ComNode}, and
     * rolecast message is to be sent to all agents of the specified role.
     */
    static public enum MsgCastType {
        SINGLECAST, ROLECAST, BROADCAST
    }

    String idSource;
    String idTarget;
    MsgCastType castTy;

    Date timeStamp = null;

    // payload:
    String msgName;
    Object[] args;

    /**
     * Message priority; currently not used.
     */
    int priority;

    // boolean markedAsRead = false ;

    /**
     * Construct a new Message.
     * 
     * @param idSource The id of the sending agent.
     * @param priority The priority of the message. Higher number means higher
     *                 priority. Currently not used.
     * @param castType Either SINGLECAST, ROLECAST, or BROADCAST.
     * @param idTarget The id of the target agent if the message is SINGLECAST, and
     *                 the role name if the message is ROLECAST. It is ignored if
     *                 the message is BROADCAST.
     * @param msgName  The 'name' of the message. We leave it unspecified what this
     *                 represents. E.g. it can be used to identify categories of the
     *                 messages.
     * @param args     The arguments of the message.
     */
    public Message(String idSource, int priority, MsgCastType castType, String idTarget, String msgName,
            Object... args) {
        this.idSource = idSource;
        this.priority = priority;
        castTy = castType;
        this.idTarget = idTarget;
        this.msgName = msgName;
        this.args = args;
    }

    /**
     * Assign a time-stamp to this message.
     */
    public Message timestamp() {
        timeStamp = new Date();
        return this;
    }

    public String getIdSource() {
        return idSource;
    }

    public String getIdTarget() {
        return idTarget;
    }

    public MsgCastType getCastTy() {
        return castTy;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public String getMsgName() {
        return msgName;
    }

    public Object[] getArgs() {
        return args;
    }

    // public boolean isRead() { return markedAsRead ;}
    // public void markAsRead() { markedAsRead = true ; }

}
