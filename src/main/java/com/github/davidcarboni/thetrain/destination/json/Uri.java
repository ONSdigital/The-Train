package com.github.davidcarboni.thetrain.destination.json;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Information about the transfer of a single file.
 */
public class Uri implements Cloneable {

    public static final String STARTED = "started";
    public static final String UPLOADED = "uploaded";
    public static final String UPLOAD_FAILED = "upload failed";
    public static final String COMMITTED = "committed";

    public String uri;
    String start;
    String end;
    String sha;
    long duration;
    /**
     * This is a String rather than an enum to make deserialisation easy. This should be {@value #STARTED}, {@value #UPLOADED}, {@value #UPLOAD_FAILED} or {@value #COMMITTED}.
     */
    String status;

    transient Date startDate;
    transient Date endDate;

    public Uri() {
        // Constructor for serialisation
    }

    public Uri(String uri, Date startDate) {
        this.uri = uri;
        this.startDate = startDate;
        start = DateConverter.toString(startDate);
        status = STARTED;
    }

    /**
     * Clones this instance and stops the timing on the clone.
     *
     * @return Returns the cloned instance.
     */
    public Uri stop(String sha) {
        Uri result = clone();
        result.sha = sha;
        result.stop();
        return result;
    }

    /**
     * Stops this timing (only upload is timed) and updates the relevant fields.
     */
    private void stop() {
        endDate = new Date();
        end = DateConverter.toString(endDate);
        duration = endDate.getTime() - startDate.getTime();
        if (StringUtils.isNotBlank(sha)) {
            status = UPLOADED;
        } else {
            status = UPLOAD_FAILED;
        }
    }

    /**
     * Clones this instance and sets the status of the clone to <code>committed</code>.
     *
     * @return Returns the cloned instance.
     */
    public Uri commit() {
        Uri result = clone();
        result.status = COMMITTED;
        return result;
    }

    /**
     * @return A clone of this instance.
     */
    protected Uri clone() {
        try {
            return (Uri) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

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
                StringUtils.equals(uri, ((Uri) obj).uri);
    }
}
