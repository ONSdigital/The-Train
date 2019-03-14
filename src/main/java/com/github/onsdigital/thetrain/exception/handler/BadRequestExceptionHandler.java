package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.filters.QuietFilter;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import com.google.gson.Gson;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;

public class BadRequestExceptionHandler implements ExceptionHandler<BadRequestException> {

    private Gson gson;
    private QuietFilter filter;

    public BadRequestExceptionHandler(QuietFilter filter) {
        this.gson = new Gson();
        this.filter = filter;
    }

    @Override
    public void handle(BadRequestException e, Request request, Response response) {
        logBuilder().warn("HANDLING BAD REQUEST EXCEPTION");
        LogBuilder log = logBuilder().transactionID(e.getTransactionID());
        if (e.getCause() != null) {
            log.error(e.getCause(), e.getMessage());
        } else {
            log.error(e.getMessage());
        }

        response.status(400);
        response.body(gson.toJson(new Result(e.getMessage(), true, null)));
        filter.handleQuietly(request, response);
    }

}
