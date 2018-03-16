package com.github.davidcarboni.thetrain.logging;

import ch.qos.logback.classic.Level;
import com.github.davidcarboni.thetrain.json.Transaction;
import com.github.onsdigital.logging.builder.LogMessageBuilder;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.List;

public class LogBuilder extends LogMessageBuilder {

    public static LogBuilder error(Exception exception, String message) {
        return new LogBuilder(exception, message);
    }

    public static LogBuilder warn(String message) {
        return new LogBuilder(message, Level.WARN);
    }

    public static LogBuilder info(String message) {
        return new LogBuilder(message, Level.INFO);
    }


    public LogBuilder(String description, Level logLevel) {
        super(description, logLevel);
    }

    public LogBuilder(Throwable t, String description) {
        super(t, description);
    }

    @Override
    public String getLoggerName() {
        return "com.github.onsdigital.thetrain";
    }

    public LogBuilder transaction(Transaction transaction) {
        return (LogBuilder) this.addParameter("transaction", transaction != null ? transaction.id() : "");
    }

    public LogBuilder transactionID(String transactionID) {
        if (!StringUtils.isEmpty(transactionID)) {
            addParameter("transactionID", transactionID);
        }
        return this;
    }

    public LogBuilder uri(String uri) {
        if (!StringUtils.isEmpty(uri)) {
            addParameter("uri", uri);
        }
        return this;
    }

    public LogBuilder errors(List<String> errors) {
        if (errors != null && !errors.isEmpty()) {
            addParameter("transactionID", String.join(", ", errors));
        }
        return this;
    }

    public LogBuilder websitePath(Path websitePath) {
        if (websitePath == null || !StringUtils.isEmpty(websitePath.toString())) {
            addParameter("websitePath", websitePath.toString());
        }
        return this;
    }

    @Override
    public LogBuilder addParameter(String key, Object value) {
        return (LogBuilder) super.addParameter(key, value);
    }
}
