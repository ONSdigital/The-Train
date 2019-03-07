package com.github.onsdigital.thetrain.filters;

import spark.Request;
import spark.Response;

@FunctionalInterface
public interface QuietFilter {

    void handleQuietly(Request req, Response resp);
}
