package com.github.onsdigital.thetrain.filters;

import com.github.onsdigital.thetrain.logging.LogBuilder;
import spark.Filter;
import spark.Request;
import spark.Response;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;

public class AfterFilter implements Filter, QuietFilter {

    @Override
    public void handle(Request request, Response response) throws Exception {
        logBuilder().uri(request.uri())
                .addParameter("method", request.requestMethod())
                .responseStatus(response.status())
                .transactionID(request.raw().getParameter("transactionId"))
                .debug("request completed");
    }

    public void handleQuietly(Request request, Response response) {
        LogBuilder log = logBuilder().uri(request.uri())
                .addParameter("method", request.requestMethod())
                .responseStatus(response.status())
                .transactionID(request.raw().getParameter("transactionId"));

        if (response.status() >= 400) {
            log.error("request unsuccessful");
            return;
        }

        log.debug("request completed successfully");
    }

}
