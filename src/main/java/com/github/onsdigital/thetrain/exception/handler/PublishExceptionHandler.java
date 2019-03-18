package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
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
        error().transactionID(e.getTransaction().id())
                .exception(e)
                .endHTTP(response.raw())
                .log("publish exception");
    }
}
