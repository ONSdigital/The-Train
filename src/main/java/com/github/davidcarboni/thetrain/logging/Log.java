package com.github.davidcarboni.thetrain.logging;

import ch.qos.logback.classic.Level;
import com.github.davidcarboni.thetrain.json.Transaction;

public class Log {

    public static void error(Exception exception) {
        new LogBuilder("exception", Level.ERROR).addParameter("exception", exception).log();
    }

    public static void info(String message) {
        new LogBuilder("info", Level.INFO).addMessage(message).log();
    }

    public static void info(Transaction transaction, String message) {
        new LogBuilder("info", Level.INFO)
                .transaction(transaction)
                .addMessage(message)
                .log();
    }
}
