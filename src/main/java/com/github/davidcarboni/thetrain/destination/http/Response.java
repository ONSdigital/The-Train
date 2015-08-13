package com.github.davidcarboni.thetrain.destination.http;

import org.apache.http.StatusLine;

/**
 * A data record to hold the essentials of an API response.
 * Created by david on 26/03/2015.
 */
public class Response<T> {

    public StatusLine statusLine;
    public T body;


    public Response(StatusLine statusLine, T body) {
        this.statusLine = statusLine;
        this.body = body;
    }

    @Override
    public String toString() {
        return statusLine.getStatusCode() + " " + statusLine.getReasonPhrase() + (body==null?"\n[no body]": "\nbody:\n" + body) ;
    }
}
