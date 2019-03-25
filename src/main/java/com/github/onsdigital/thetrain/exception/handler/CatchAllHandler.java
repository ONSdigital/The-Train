package com.github.onsdigital.thetrain.exception.handler;

import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;

public class CatchAllHandler implements ExceptionHandler<Exception> {

    @Override
    public void handle(Exception e, Request request, Response response) {
        response.status(500);
        String transactionId = request.raw().getParameter("transactionId");

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
            String message = String.format("internal server error %d/%d: %s", i + 1, nested.size(), ex.getMessage());
            error().transactionID(transactionId).exception(ex).log(message);
        });
    }
}
