package nl.uu.cs.aplib.MultiAgentSupport;

public class Acknowledgement {
	
	static public enum AckType { SUCCESS, REJECTED }
	
	AckType ackTy ;
	String info ;
	
	public Acknowledgement(AckType ty, String info) {
		ackTy = ty ; this.info = info ;
	}

	public AckType getAckTy() {
		return ackTy;
	}

	public String getInfo() {
		return info;
	}

}
