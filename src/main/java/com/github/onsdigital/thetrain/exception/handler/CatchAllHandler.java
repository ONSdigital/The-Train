package com.github.onsdigital.thetrain.exception.handler;

import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;

public class CatchAllHandler implements ExceptionHandler<Exception> {

    @Override
    public void handle(Exception e, Request request, Response response) {
        response.status(500);
        error().transactionID(request.raw().getParameter("transactionId"))
                .endHTTP(response.raw())
                .exception(e)
                .log("internal server error");
    }
}
