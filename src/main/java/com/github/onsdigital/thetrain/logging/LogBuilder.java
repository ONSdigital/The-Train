package com.github.onsdigital.thetrain.logging;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;
import com.github.onsdigital.thetrain.json.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a convenient builder pattern interface for logging and adding log parameters.
 */
public class LogBuilder extends LogMessageBuilder {

    static final String NAME_SPACE = "the-train";
    static final String ERROR_CONTEXT = "errorContext";
    static final String CLASS = "class";
    static final String STACK_TRACE = "stackTrace";
    static final String PACKAGE = "com.github.onsdigital.thetrain";
    static final String TRANSACTION_ID = "transactionID";
    static final String URI = "uri";
    static final String WEBSITE_PATH = "websitePath";
    static final String RESPONSE_STATUS = "responseStatus";
    static final String METRICS = "metrics";
    static final String DURATION_MS = "durationMillis";
    static final String EVENT = "event";
    static final String DESCRIPTION = "description";


    public static LogBuilder logBuilder() {
        return new LogBuilder("");
    }

    protected LogBuilder(String eventDescription) {
        super(eventDescription);
        setNamespace(NAME_SPACE);
    }

    /**
     * Log at denbug level.
     *
     * @param message context of the log message.
     */
    public void debug(String message) {
        logLevel = Level.DEBUG;
        description = message;
        log();
    }

    /**
     * Log at info level.
     *
     * @param message context of the log message.
     */
    public void info(String message) {
        logLevel = Level.INFO;
        description = message;
        log();
    }

    /**
     * Log at warn level.
     *
     * @param message context of the log message.
     */
    public void warn(String message) {
        logLevel = Level.WARN;
        description = message;
        log();
    }

    /**
     * Log at error level.
     *
     * @param message context of the log message.
     */
    public void error(Throwable t, String message) {
        logLevel = Level.ERROR;
        description = message;
        addParameter(ERROR_CONTEXT, message);
        addParameter(CLASS, t.getClass().getName());
        parameters.getParameters().put(STACK_TRACE, ExceptionUtils.getStackTrace(t));
        addMessage(message).log();
    }

    public void error(String message) {
        logLevel = Level.ERROR;
        description = message;
        log();
    }

    @Override
    public String getLoggerName() {
        return PACKAGE;
    }

    /**
     * Add transactionID to the log parameters.
     */
    public LogBuilder transactionID(String transactionID) {
        if (!StringUtils.isEmpty(transactionID)) {
            addParameter(TRANSACTION_ID, transactionID);
        }
        return this;
    }

    public LogBuilder transactionID(Transaction transaction) {
        if (transaction != null && !StringUtils.isEmpty(transaction.id())) {
            addParameter(TRANSACTION_ID, transaction.id());
        }
        return this;
    }

    /**
     * Add class name to the log parameters.
     */
    public LogBuilder clazz(Class c) {
        if (c != null) {
            addParameter(CLASS, c.getSimpleName().toLowerCase());
        }
        return this;
    }


    /**
     * Add uri to the log parameters.
     */
    public LogBuilder uri(String uri) {
        if (!StringUtils.isEmpty(uri)) {
            addParameter(URI, uri);
        }
        return this;
    }

    /**
     * Add errors to the log parameters.
     */
    public LogBuilder errors(List<String> errors) {
        if (errors != null && !errors.isEmpty()) {
            addParameter(TRANSACTION_ID, String.join(", ", errors));
        }
        return this;
    }

    /**
     * Add the WebsitePath value to the log parameters.
     */
    public LogBuilder websitePath(Path websitePath) {
        if (websitePath == null || !StringUtils.isEmpty(websitePath.toString())) {
            addParameter(WEBSITE_PATH, websitePath.toString());
        }
        return this;
    }

    /**
     * Add the HTTP response status to the log parameters.
     */
    public LogBuilder responseStatus(int status) {
        addParameter(RESPONSE_STATUS, status);
        return this;
    }

    public LogBuilder metrics(LocalDateTime start, MetricEvents event) {
        addParameter(METRICS, createMetrics(start, event));
        return this;
    }

    private Map<String, Object> createMetrics(LocalDateTime start, MetricEvents event) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put(DURATION_MS, Duration.between(start, LocalDateTime.now()).toMillis());
        metrics.put(EVENT, event.getName());
        metrics.put(DESCRIPTION, event.getDescription());
        return metrics;
    }

    /**
     * Add a parameter.
     */
    @Override
    public LogBuilder addParameter(String key, Object value) {
        return (LogBuilder) super.addParameter(key, value);
    }
}
