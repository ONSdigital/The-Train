package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.json.Result;
import com.google.gson.Gson;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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

        List<Throwable> nested = new ArrayList<>();
        nested.add(e);

        if (e.getCause() != null) {
            Throwable t = e;
            while (t.getCause() != null) {
                nested.add(t.getCause());
                t = t.getCause();
            }
        }

        IntStream.range(0, nested.size()).forEach(i -> {
            Throwable ex = nested.get(i);
            String message = String.format("bad request exception %d/%d: %s", i + 1, nested.size(), ex.getMessage());
            error().transactionID(e.getTransactionID()).exception(ex).log(message);
        });
    }
}
