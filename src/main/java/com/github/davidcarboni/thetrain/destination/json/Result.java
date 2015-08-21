package com.github.davidcarboni.thetrain.destination.json;

/**
 * Json message returned by API endpoints.
 */
public class Result {
    public String message;
    public boolean error;
    public Transaction transaction;

    /**
     * The response message returned from the endpoints in this class.
     *
     * @param message     An informational message.
     * @param error       If this message represents an application error, true (rather than a general server or network error).
     * @param transaction The details of the current transaction.
     */
    public Result(String message, boolean error, Transaction transaction) {
        this.message = message;
        this.error = error;
        this.transaction = transaction;
    }
}
