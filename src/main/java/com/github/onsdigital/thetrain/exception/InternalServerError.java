package com.github.onsdigital.thetrain.exception;

public class InternalServerError extends Exception {

    private String transactionID;

    public InternalServerError(Throwable cause) {
        super(cause);
    }

    public InternalServerError(String message, String transactionID) {
        super(message);
        this.transactionID = transactionID;
    }

    public InternalServerError(String message, Throwable cause, String transactionID) {
        super(message, cause);
        this.transactionID = transactionID;
    }

    public InternalServerError(String message, Throwable cause) {
        super(message, cause);
    }

    public String getTransactionID() {
        return transactionID;
    }
}
