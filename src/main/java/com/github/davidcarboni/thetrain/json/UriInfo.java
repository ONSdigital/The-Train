package com.github.davidcarboni.thetrain.json;

import com.github.davidcarboni.thetrain.helpers.DateConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Information about the transfer of a single file.
 */
public class UriInfo {

    public static final String STARTED = "started";
    public static final String UPLOADED = "uploaded";
    public static final String UPLOAD_FAILED = "upload failed";
    public static final String COMMIT_FAILED = "commit failed";
    public static final String COMMITTED = "committed";
    public static final String ROLLED_BACK = "rolled back";
    public static final String UNKNOWN = "This URI was not recorded in Transaction info";

    public static final String CREATE = "created";
    public static final String UPDATE = "updated";
    public static final String DELETE = "deleted";

    /**
     * This is a String rather than an enum to make deserialisation lenient.
     * <p/>
     * This should be one of the following constant values defined in this class:
     * <ul>
     * <li>{@value #STARTED}</li>
     * <li>{@value #UPLOADED}</li>
     * <li>{@value #UPLOAD_FAILED}</li>
     * <li>{@value #COMMIT_FAILED}</li>
     * <li>{@value #COMMITTED}</li>
     * <li>{@value #ROLLED_BACK}</li>
     * <li>{@value #UNKNOWN}</li>
     * </ul>
     */
    String status;

    /**
     * This is a String rather than an enum to make deserialisation lenient.
     * <p/>
     * This should be one of the following constant values defined in this class:
     * <ul>
     * <li>{@value #CREATE}</li>
     * <li>{@value #UPDATE}</li>
     * </ul>
     */
    String action;
    String uri;
    String start;
    String end;
    long duration;
    String error;

    transient Date startDate;
    transient Date endDate;

    /**
     * Constructor for serialisation.
     */
    public UriInfo() {
        // Constructor for serialisation
    }

    public String status() {
        return status;
    }


    public String action() {
        return action;
    }

    /**
     * Normal constructor.
     *
     * @param uri       The URI to record information about.
     * @param startDate The point in time this URI started being processed.
     */
    public UriInfo(String uri, Date startDate) {
        this.uri = uri;
        this.startDate = startDate;
        start = DateConverter.toString(startDate);
        status = STARTED;
    }

    /**
     * Fallback constructor in the event that a file exists in a transaction
     * but is not present in the {@link Transaction} object.
     *
     * @param uri The URI to record information about.
     */
    public UriInfo(String uri) {
        this.uri = uri;
        status = UNKNOWN;
        error = UNKNOWN;
    }

    public String uri() {
        return uri;
    }

    /**
     * Stops this timing (only upload is timed) and updates the relevant fields.
     *
     * @return <code>this</code>.
     */
    public void stop() {
        endDate = new Date();
        end = DateConverter.toString(endDate);
        if (startDate != null && endDate != null) {
            duration = endDate.getTime() - startDate.getTime();
        }
        status = UPLOADED;
    }

    /**
     * @param error An error debug to set for this Uri.
     */
    public void fail(String error) {
        status = COMMIT_FAILED;
        this.error = error;
    }

    /**
     * Sets the status of this instance to {@value #COMMITTED}.
     */
    public void commit() {
        status = COMMITTED;
    }

    /**
     * Set the action type
     * @param action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Sets the status of this instance to {@value #ROLLED_BACK}.
     */
    public void rollback() {
        status = ROLLED_BACK;
    }

    /**
     * Sets the status of this instance to {@value #COMMITTED}.
     */
    public String error() {
        return error;
    }

    // The hashCode and equals methods are used to identify this instance in the Set<Uri> in Transaction.

    @Override
    public int hashCode() {
        int result = 0;
        if (uri != null) {
            result = uri.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null &&
                obj.getClass().equals(this.getClass()) &&
                StringUtils.equals(uri, ((UriInfo) obj).uri);
    }

    @Override
    public String toString() {
        return uri() + " (" + status + ")";
    }

}
