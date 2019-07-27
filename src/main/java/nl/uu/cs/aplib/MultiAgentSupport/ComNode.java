package nl.uu.cs.aplib.MultiAgentSupport;

import java.util.* ;

import nl.uu.cs.aplib.Agents.AutonomousBasicAgent;
import nl.uu.cs.aplib.MultiAgentSupport.Acknowledgement.AckType;

public class ComNode {
	
	Map<String,AutonomousBasicAgent> idMap = new HashMap<String,AutonomousBasicAgent>() ;
	Map<String,Set<AutonomousBasicAgent>> roleMap = new HashMap<String,Set<AutonomousBasicAgent>>() ;
	
	public ComNode() {
	}
	
	synchronized public void register(AutonomousBasicAgent agent) {
		idMap.put(agent.getId(), agent) ;
		var role = agent.getRole() ;
		var brothers = roleMap.get(role) ;
		if (brothers == null) {
			brothers = new HashSet<AutonomousBasicAgent>() ;
			brothers.add(agent) ;
			roleMap.put(role,brothers) ;
			return ;
		}
		brothers.add(agent) ;
	}
	
	public Acknowledgement send(Message msg) {
		String senderId = msg.idSource ;
		var sender = idMap.get(senderId) ;
		if (sender == null) {
			// unknown sender!
			return new Acknowledgement(AckType.REJECTED,"Sender is not registered.") ;
		}
		switch(msg.castTy) {
		   case SINGLECAST :
			    var receiver = idMap.get(msg.idTarget) ;
			    if (receiver == null) {
			    	return new Acknowledgement(AckType.REJECTED,"Receiver is not registered.") ;
			    }
			    receiver.sendMsgToThisAgent(msg);
			    return new Acknowledgement(AckType.SUCCESS,null) ; 
		   case BROADCAST :
			    for (AutonomousBasicAgent B : idMap.values()) {
			    	if (B != sender) B.sendMsgToThisAgent(msg);
			    }
			    return new Acknowledgement(AckType.SUCCESS,null) ;
		   case ROLECAST :
			    var receivers = roleMap.get(msg.idTarget) ;
			    if (receivers != null) {
				    for (AutonomousBasicAgent B : receivers) {
				    	if (B != sender) B.sendMsgToThisAgent(msg);
				    }
			    }
			    return new Acknowledgement(AckType.SUCCESS,null) ;
		}
		// should not happen
		return null ;
	}
	

}
