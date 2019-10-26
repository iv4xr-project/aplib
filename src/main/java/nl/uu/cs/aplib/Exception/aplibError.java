package nl.uu.cs.aplib.Exception;

public class AplibError extends Error {

	private static final long serialVersionUID = 1L;

	public AplibError(String cause) {
		super(cause) ;
	}
	
}
