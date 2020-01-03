package com.github.onsdigital.thetrain.json;

import com.google.gson.JsonSyntaxException;
import spark.Request;

@FunctionalInterface
public interface JSONReader {

    <T> T fromRequestBody(Request request, Class<T> tClass) throws JsonSyntaxException;
}
