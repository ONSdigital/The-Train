package com.github.davidcarboni.thetrain.destination.json;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Information about the transfer of a single file.
 */
public class Timing {

    String uri;
    String start;
    String end;
    String sha;
    long duration;
    /** This is a String rather than an enum to make deserialisation easy. This should be <code>started</code>, <code>uploaded</code> or <code>committed</code>. */
    String status;

    transient Date startDate;
    transient Date endDate;

    public Timing(String uri) {
        this.uri = uri;
        startDate = new Date();
        start = DateConverter.toString(startDate);
        status = "started";
    }

    /**
     * Stops this timing and updates the fields.
     * @return Returns this instance for convenience when passing updates along.
     */
    public Timing stop(String sha) {
        endDate = new Date();
        end = DateConverter.toString(endDate);
        duration = endDate.getTime() - startDate.getTime();
        this.sha = sha;
        if (StringUtils.isNotBlank(sha)) {
            status = "uploaded";
        } else {
            status = "upload failed";
        }
        return this;
    }
}
