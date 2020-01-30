package com.github.onsdigital.thetrain.filters;

import spark.Filter;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;


public class BeforeFilter implements Filter {

    static final String REQ_ID_KEY = "X-Request-Id";

    @Override
    public void handle(Request request, Response response) throws Exception {
        info().request(request.raw()).log("request received");
    }
}
