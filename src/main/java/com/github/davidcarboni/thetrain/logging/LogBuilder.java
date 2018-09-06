package com.github.davidcarboni.thetrain.logging;

import ch.qos.logback.classic.Level;
import com.github.davidcarboni.thetrain.api.common.Endpoint;
import com.github.onsdigital.logging.builder.LogMessageBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * Provides a convenient builder pattern interface for logging and adding log parameters.
 */
public class LogBuilder extends LogMessageBuilder {

    public static LogBuilder logBuilder() {
        return new LogBuilder("");
    }

    protected LogBuilder(String eventDescription) {
        super(eventDescription);
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
        addParameter("errorContext", message);
        addParameter("class", t.getClass().getName());
        parameters.getParameters().put("stackTrace", ExceptionUtils.getStackTrace(t));
        addMessage(message).log();
    }

    @Override
    public String getLoggerName() {
        return "com.github.onsdigital.thetrain";
    }

    /**
     * Add transactionID to the log parameters.
     */
    public LogBuilder transactionID(String transactionID) {
        if (!StringUtils.isEmpty(transactionID)) {
            addParameter("transactionID", transactionID);
        }
        return this;
    }

    /**
     * Add API endpoint name to the log parameters.
     */
    public LogBuilder endpoint(Endpoint endpoint) {
        if (endpoint != null) {
            addParameter("endpoint", "/" + endpoint.getClass().getSimpleName().toLowerCase());
        }
        return this;
    }

    /**
     * Add class name to the log parameters.
     */
    public LogBuilder clazz(Class c) {
        if (c != null) {
            addParameter("class", c.getSimpleName().toLowerCase());
        }
        return this;
    }


    /**
     * Add uri to the log parameters.
     */
    public LogBuilder uri(String uri) {
        if (!StringUtils.isEmpty(uri)) {
            addParameter("uri", uri);
        }
        return this;
    }

    /**
     * Add errors to the log parameters.
     */
    public LogBuilder errors(List<String> errors) {
        if (errors != null && !errors.isEmpty()) {
            addParameter("transactionID", String.join(", ", errors));
        }
        return this;
    }

    /**
     * Add the WebsitePath value to the log parameters.
     */
    public LogBuilder websitePath(Path websitePath) {
        if (websitePath == null || !StringUtils.isEmpty(websitePath.toString())) {
            addParameter("websitePath", websitePath.toString());
        }
        return this;
    }

    /**
     * Add the HTTP response status to the log parameters.
     */
    public LogBuilder responseStatus(int status) {
        addParameter("responseStatus", status);
        return this;
    }

    public LogBuilder timeSince(long start) {
        addParameter("timeTaken", System.currentTimeMillis() - start);
        return this;
    }

    /**
     * Add a parameter.
     */
    @Override
    public LogBuilder addParameter(String key, Object value) {
        return (LogBuilder) super.addParameter(key, value);
    }
}
