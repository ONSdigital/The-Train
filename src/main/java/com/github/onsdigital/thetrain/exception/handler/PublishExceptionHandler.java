package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.filters.QuietFilter;
import com.github.onsdigital.thetrain.json.Result;
import com.google.gson.Gson;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;

public class PublishExceptionHandler implements ExceptionHandler<PublishException> {

    private Gson gson;
    private QuietFilter filter;

    public PublishExceptionHandler(QuietFilter filter) {
        this.gson = new Gson();
        this.filter = filter;
    }

    @Override
    public void handle(PublishException e, Request request, Response response) {
        error().transactionID(e.getTransaction().id()).logException(e, e.getMessage());
        response.status(e.getStatus());
        response.body(gson.toJson(new Result(e.getMessage(), true, e.getTransaction())));
        filter.handleQuietly(request, response);
    }
}
