package com.github.onsdigital.thetrain.api.filter;

import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

public class RequestContextFilter {//implements Filter {

    //@Override
    public boolean filter(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        /**
         * TEMP FIX: Zebedee does not currently pass the request ID to the train so generate a new one to allow us to
         * tie requests together.
         */
        MDC.put("X-Request-Id", UUID.randomUUID().toString());
        return true;
    }
}
