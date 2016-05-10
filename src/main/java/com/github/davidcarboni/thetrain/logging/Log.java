package com.github.davidcarboni.thetrain.logging;

import com.github.davidcarboni.thetrain.json.Transaction;

public class Log {

    public static void error(Exception exception) {
        new LogBuilder("exception").addParameter("exception", exception).log();
    }

    public static void debug(String message) {
        new LogBuilder("debug").addParameter("debug", message).log();
    }

    public static void debug(Transaction transaction, String message) {
        new LogBuilder("debug")
                .transaction(transaction)
                .addMessage(message)
                .log();
    }
}
