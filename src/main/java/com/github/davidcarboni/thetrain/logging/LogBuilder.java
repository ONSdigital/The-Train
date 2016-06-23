package com.github.davidcarboni.thetrain.logging;

import ch.qos.logback.classic.Level;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.onsdigital.logging.builder.LogMessageBuilder;

public class LogBuilder extends LogMessageBuilder {

    public LogBuilder(String description, Level logLevel) {
        super(description, logLevel);
    }

    @Override
    public String getLoggerName() {
        return "com.github.onsdigital.thetrain";
    }

    public LogBuilder transaction(Transaction transaction) {
        return (LogBuilder)this.addParameter("transaction", transaction != null ? transaction.id() :"");
    }
}
