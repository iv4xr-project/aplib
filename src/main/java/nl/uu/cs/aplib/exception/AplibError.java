package nl.uu.cs.aplib.exception;

public class AplibError extends Error {

    private static final long serialVersionUID = 1L;

    public AplibError(String cause) {
        super(cause);
    }

}
