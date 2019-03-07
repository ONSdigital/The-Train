package com.github.onsdigital.thetrain.filters;

import spark.Filter;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

public class BeforeFilter implements Filter {

    static final String REQ_ID_KEY = "X-Request-Id";

    @Override
    public void handle(Request request, Response response) throws Exception {
        info().beginHTTP(request.raw()).log("request received");
    }
}
