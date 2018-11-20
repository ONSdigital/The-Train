package com.github.onsdigital.thetrain.exception.handler;

import com.github.onsdigital.thetrain.exception.InternalServerError;
import com.github.onsdigital.thetrain.filters.QuietFilter;
import com.github.onsdigital.thetrain.logging.LogBuilder;
import com.github.onsdigital.thetrain.response.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;
import spark.Route;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;

public class InternalServerErrorHandler implements Route, ExceptionHandler<InternalServerError> {

    static final Message INTERNAL_SERVER_ERROR = new Message("internal server error");

    private Gson gson;
    private QuietFilter filter;

    public InternalServerErrorHandler(QuietFilter filter) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.filter = filter;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        logBuilder()
                .uri(request.uri())
                .addParameter("method", request.requestMethod())
                .responseStatus(500)
                .error("expected error returning internal server error status");

        response.status(500);
        filter.handleQuietly(request, response);
        return gson.toJson(INTERNAL_SERVER_ERROR);
    }

    @Override
    public void handle(InternalServerError e, Request request, Response response) {
        LogBuilder log = logBuilder().transactionID(e.getTransactionID());
        if (e.getCause() != null) {
            log.error(e.getCause(), e.getMessage());
        } else {
            log.error(e.getMessage());
        }

        response.status(500);
        filter.handleQuietly(request, response);
    }
}
