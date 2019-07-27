package nl.uu.cs.aplib.MultiAgentSupport;

import java.util.Comparator;
import java.util.Date;

public class Message {
	
	static public enum MsgCastType { SINGLECAST, ROLECAST, BROADCAST }
	
	String idSource ;
	String idTarget ;
	MsgCastType castTy ;
	
	Date timeStamp = null ;
	
	// payload:
	String msgName ; 
	Object[] args ;
	
	int priority ;
	
	boolean markedAsRead = false ;

	public Message(String idSource, int priority, MsgCastType castType, String idTarget, String msgName, Object ... args) {
		this.idSource = idSource ;
		this.priority = priority ;
		castTy = castType ; this.idTarget = idTarget ; this.msgName = msgName ; this.args = args ;
	}
	
	public Message timestamp() {
		timeStamp = new Date() ; return this ;
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
	
	public boolean isRead() { return markedAsRead ;}
	public void markAsRead() { markedAsRead = true ; }
	
}
