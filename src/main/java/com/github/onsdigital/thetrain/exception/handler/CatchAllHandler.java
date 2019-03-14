package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.filters.QuietFilter;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;

public class CatchAllHandler implements ExceptionHandler<Exception> {

    private QuietFilter filter;

    public CatchAllHandler(QuietFilter filter) {
        this.filter = filter;
    }

    @Override
    public void handle(Exception e, Request request, Response response) {
        logBuilder().warn("CATCH ALL");
        LogBuilder log = logBuilder().transactionID(request.raw().getParameter("transactionId"));
        if (e.getCause() != null) {
            log.error(e.getCause(), e.getMessage());
        } else {
            log.error(e.getMessage());
        }

        response.status(500);
        filter.handleQuietly(request, response);
    }
}
