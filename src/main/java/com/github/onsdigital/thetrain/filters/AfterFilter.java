package com.github.onsdigital.thetrain.filters;

import spark.Filter;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

public class AfterFilter implements Filter, QuietFilter {

    @Override
    public void handle(Request request, Response response) throws Exception {
        info().endHTTP(response.raw()).log("request completed");
    }

    public void handleQuietly(Request request, Response response) {
        if (response.status() >= 400) {
            error().endHTTP(response.raw()).log("request unsuccessful");
            return;
        }
        info().endHTTP(response.raw()).log("request completed");
    }

}
