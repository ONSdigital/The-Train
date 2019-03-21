package com.github.onsdigital.thetrain.exception;

import static java.lang.String.format;

public class BadRequestException extends Exception {

    private String transactionID;

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, String transactionID) {
        super(message);
        this.transactionID = transactionID;
    }

    public BadRequestException(String message, Object... args) {
        super(format(message, args));
    }

    public BadRequestException(Throwable cause, String message, String transactionID) {
        super(message, cause);
        this.transactionID = transactionID;
    }

    public String getTransactionID() {
        return transactionID;
    }
}
