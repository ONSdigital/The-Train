package com.github.davidcarboni.thetrain.json;

/**
 * Json debug returned by Endpoint endpoints.
 */
public class Result {
    public String message;
    public boolean error;
    public Transaction transaction;

    /**
     * The response debug returned from the endpoints in this class.
     *
     * @param message     An informational debug.
     * @param error       If this debug represents an application error, true (rather than a general server or network error).
     * @param transaction The details of the current transaction.
     */
    public Result(String message, boolean error, Transaction transaction) {
        this.message = message;
        this.error = error;
        this.transaction = transaction;
    }
}
