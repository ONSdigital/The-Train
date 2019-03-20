package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.json.Result;
import com.github.onsdigital.thetrain.json.Transaction;
import com.google.gson.Gson;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;

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

        Transaction transaction = e.getTransaction();

        List<Throwable> nested = new ArrayList<>();
        nested.add(e);

        if (e.getCause() != null) {
            Throwable t = e;
            while (t.getCause() != null) {
                nested.add(t.getCause());
                t = t.getCause();
            }
        }

        nested.stream().forEach(ex -> error().transactionID(transaction).exception(ex).log(ex.getMessage()));
    }
}
