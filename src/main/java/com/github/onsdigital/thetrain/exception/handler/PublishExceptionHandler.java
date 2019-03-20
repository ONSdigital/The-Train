package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.logging.TrainEvent;
import com.google.gson.Gson;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;

public class PublishExceptionHandler implements ExceptionHandler<PublishException> {

    private Gson gson;

    public PublishExceptionHandler() {
        this.gson = new Gson();
    }

    @Override
    public void handle(PublishException e, Request request, Response response) {
        response.status(e.getStatus());
        response.body(gson.toJson(new Result(e.getMessage(), true, e.getTransaction())));

        TrainEvent err = error().transactionID(e.getTransaction().id()).endHTTP(response.raw());
        if (e.getCause() != null) {
            err.exception(e.getCause()).log("publishing exception: " + e.getCause().getMessage());
        } else {
            err.exception(e).log("publishing exception: " + e.getMessage());
        }
    }
}
