package com.github.onsdigital.thetrain.filters;

import spark.Filter;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;

public class AfterFilter implements Filter {

    @Override
    public void handle(Request request, Response response) throws Exception {
        logBuilder().uri(request.uri())
                .addParameter("method", request.requestMethod())
                .responseStatus(response.status())
                .info("request completed");
    }
}
