package nl.uu.cs.aplib.mainConcepts;

/**
 * A class that can be used to represent rough status of some 'computation'
 * (whatever this computation is). There are three statuses represented: (1) the
 * computation is still in progress, (2) the computation has completed
 * successfully, and (3) the computation has completed in failure.
 * 
 * @author wish
 *
 */
public class ProgressStatus {

    static enum ProgressStatus_ {
        INPROGRESS, SUCCESS, FAILED
    }

    ProgressStatus_ status = ProgressStatus_.INPROGRESS;
    String info;

    /**
     * Create an instance of this class, with null info and status initialized to
     * INPROGRESS.
     */
    public ProgressStatus() {
    }

    /**
     * Return the info string stored in this instance. Note that this info can be
     * null.
     */
    public String getInfo() {
        return info;
    }

    /**
     * True if the status represented by this instance is SUCCESS.
     */
    public boolean success() {
        return status == ProgressStatus_.SUCCESS;
    }

    /**
     * Trye if the status represented by this instance if FAILED.
     */
    public boolean failed() {
        return status == ProgressStatus_.FAILED;
    }

    /**
     * True if the status represented by this instance is INPROGRESS.
     */
    public boolean inProgress() {
        return status == ProgressStatus_.INPROGRESS;
    }

    /**
     * Set the status represented by this instance to SUCCESS, and set its info to
     * the given string. Don't open this to public to prevent user to freely mark a
     * goal as success.
     */
    void setToSuccess(String info) {
        status = ProgressStatus_.SUCCESS;
        this.info = info;
    }

    /**
     * Set the status represented by this instance to SUCCESS, with no info string
     * (it is set to null).
     */
    public void setToSuccess() {
        setToSuccess(null);
    }

    /**
     * Set the status represented by this instance to INPROGRESS. The info string is
     * set to null.
     */
    public void resetToInProgress() {
        status = ProgressStatus_.INPROGRESS;
        info = null;
    }

    /**
     * Set the status represented by this instance to FAILED, and set its info to
     * the given string. You can use the info the describe the reason of the
     * failure.
     */
    void setToFail(String info) {
        status = ProgressStatus_.FAILED;
        this.info = info;
    }

    /**
     * Set the status represented by this instance to FAILED.
     */
    void setToFail() {
        setToFail(null);
    }

    @Override
    public String toString() {
        String s = "" + status;
        if (info != null)
            s += ". " + info;
        return s;
    }

}
