package eu.iv4xr.framework.exception;

public class Iv4xrError extends Error {

    private static final long serialVersionUID = 1L;

    public Iv4xrError(String cause) {
        super(cause);
    }
}
