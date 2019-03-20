package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.json.Result;
import com.google.gson.Gson;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;

public class BadRequestExceptionHandler implements ExceptionHandler<BadRequestException> {

    private Gson gson;

    public BadRequestExceptionHandler() {
        this.gson = new Gson();
    }

    @Override
    public void handle(BadRequestException e, Request request, Response response) {
        response.status(400);
        response.body(gson.toJson(new Result(e.getMessage(), true, null)));

        error().transactionID(e.getTransactionID())
                .exception(e)
                .endHTTP(response.raw())
                .log("bad request");
    }
}
