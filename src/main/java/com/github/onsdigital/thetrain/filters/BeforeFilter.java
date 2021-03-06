package com.github.onsdigital.thetrain.filters;

import spark.Filter;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;

public class BeforeFilter implements Filter {

    @Override
    public void handle(Request request, Response response) throws Exception {
        info().beginHTTP(request.raw()).log("http request received");
    }
}
