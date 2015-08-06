package com.github.davidcarboni.thetrain.destination.json;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Information about the transfer of a single file.
 */
public class Timing implements Cloneable {

    public String uri;
    String start;
    String end;
    String sha;
    long duration;
    /**
     * This is a String rather than an enum to make deserialisation easy. This should be <code>started</code>, <code>uploaded</code> or <code>committed</code>.
     */
    String status;

    transient Date startDate;
    transient Date endDate;

    public Timing() {
        // Constructor for serialisation
    }

    public Timing(String uri) {
        this.uri = uri;
        startDate = new Date();
        start = DateConverter.toString(startDate);
        status = "started";
    }

    /**
     * Clones this instance and stops the timing on the clone.
     *
     * @return Returns the cloned instance.
     */
    public Timing stop(String sha) {
        Timing result = clone();
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
            status = "uploaded";
        } else {
            status = "upload failed";
        }
    }

    /**
     * Clones this instance and sets the status of the clone to <code>committed</code>.
     *
     * @return Returns the cloned instance.
     */
    public Timing commit() {
        Timing result = clone();
        result.status = "committed";
        return result;
    }

    /**
     * @return A clone of this instance.
     */
    protected Timing clone() {
        try {
            return (Timing) super.clone();
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
                StringUtils.equals(uri, ((Timing) obj).uri);
    }
}
