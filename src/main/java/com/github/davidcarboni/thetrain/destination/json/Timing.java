package com.github.davidcarboni.thetrain.destination.json;

import java.util.Date;

/**
 * Created by david on 04/08/2015.
 */
public class Timing {

    String uri;
    String start;
    String end;
    String sha;
    long duration;

    transient Date startDate;
    transient Date endDate;

    public Timing(String uri) {
        this.uri = uri;
        startDate = new Date();
        start = DateConverter.toString(startDate);
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
        return this;
    }
}
