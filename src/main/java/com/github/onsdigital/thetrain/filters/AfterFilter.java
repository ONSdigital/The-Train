package com.github.onsdigital.thetrain.filters;

import spark.Filter;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

public class AfterFilter implements Filter {

    @Override
    public void handle(Request request, Response response) throws Exception {
        info().endHTTP(response.raw()).log("request completed");
    }
}
