package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.filters.QuietFilter;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;

public class CatchAllHandler implements ExceptionHandler<Exception> {

    private QuietFilter filter;

    public CatchAllHandler(QuietFilter filter) {
        this.filter = filter;
    }

    @Override
    public void handle(Exception e, Request request, Response response) {
        error().transactionID(request.raw().getParameter("transactionId")).logException(e, e.getMessage());
        response.status(500);
        filter.handleQuietly(request, response);
    }
}
