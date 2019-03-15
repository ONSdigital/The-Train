package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.filters.QuietFilter;
import com.github.onsdigital.thetrain.json.Result;
import com.google.gson.Gson;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;

public class BadRequestExceptionHandler implements ExceptionHandler<BadRequestException> {

    private Gson gson;
    private QuietFilter filter;

    public BadRequestExceptionHandler(QuietFilter filter) {
        this.gson = new Gson();
        this.filter = filter;
    }

    @Override
    public void handle(BadRequestException e, Request request, Response response) {
        error().transactionID(e.getTransactionID()).logException(e, "bad request exception");
        response.status(400);
        response.body(gson.toJson(new Result(e.getMessage(), true, null)));
        filter.handleQuietly(request, response);
    }

}
