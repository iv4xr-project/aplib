package nl.uu.cs.aplib.multiAgentSupport;

/**
 * Representing an acknowledgement sent back by the ComNode when an agent sends
 * a message to it. The Acknowledgement is either SUCCESS (if the message is not
 * rejected) or REJECTED (if the message cannot be delivered).
 * 
 * @author Wish
 *
 */
public class Acknowledgement {

    static public enum AckType {
        SUCCESS, REJECTED
    }

    AckType ackTy;
    String info;

    public Acknowledgement(AckType ty, String info) {
        ackTy = ty;
        this.info = info;
    }

    public AckType getAckTy() {
        return ackTy;
    }

    /**
     * True if this Acknowledgement is a SUCCESS.
     */
    public boolean success() {
        return ackTy == AckType.SUCCESS;
    }

    public String getInfo() {
        return info;
    }

}
