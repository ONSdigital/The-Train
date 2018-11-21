package com.github.onsdigital.thetrain.filters;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.UUID;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;

public class BeforeFilter implements Filter {

    static final String REQ_ID_KEY = "X-Request-Id";

    @Override
    public void handle(Request request, Response response) throws Exception {
        String requestID = request.raw().getHeader(REQ_ID_KEY);

        if (StringUtils.isEmpty(requestID)) {
            requestID = UUID.randomUUID().toString();
        }

        MDC.put(REQ_ID_KEY, requestID);

        logBuilder().uri(request.uri())
                .addParameter("method", request.requestMethod())
                .addParameter("params", request.raw().getParameterMap())
                .info("inbound request");
    }
}
