package nl.uu.cs.aplib.MainConcepts;

public class ProgressStatus {
	static enum ProgressStatus_ {
       INPROGRESS,
       SUCCESS,
       FAILED }
	
	ProgressStatus_ status = ProgressStatus_.INPROGRESS ;
	String info ;
	
	public ProgressStatus() { }
	public String getInfo() { return info ; }
	
	public boolean sucess() { return status == ProgressStatus_.SUCCESS ; }
	public boolean failed() { return status == ProgressStatus_.FAILED ; }
	public boolean inProgress() { return status == ProgressStatus_.INPROGRESS ; }
	
	public void setToSuccess(String info) { 
		status = ProgressStatus_.SUCCESS ;
		this.info = info ; 
	}
	public void setToSuccess() { setToSuccess(null) ; }
		
	public void setToFail(String info) { 
		status = ProgressStatus_.FAILED ;
		this.info = info ; 
	}
	public void setToFail() {setToFail(null) ; }
	
	@Override
	public String toString() {
		String s = "" + status ;
		if (info != null) s += ". " + info ;
		return s ;
	}
	
}
