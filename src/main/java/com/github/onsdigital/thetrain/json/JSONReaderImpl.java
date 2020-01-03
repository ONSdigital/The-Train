package com.github.onsdigital.thetrain.json;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import spark.Request;

public class JSONReaderImpl implements JSONReader {

    private Gson gson;

    public JSONReaderImpl() {
        this.gson = new Gson();
    }

    @Override
    public <T> T fromRequestBody(Request request, Class<T> tClass) throws JsonSyntaxException {
        return gson.fromJson(request.body(), tClass);
    }
}
