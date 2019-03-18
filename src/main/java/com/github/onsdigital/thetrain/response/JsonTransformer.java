package com.github.onsdigital.thetrain.response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {

    private static JsonTransformer INSTANCE = null;

    private Gson gson;

    private JsonTransformer() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        if (true) {
            gsonBuilder.setPrettyPrinting();
        }

        this.gson = gsonBuilder.create();
    }

    @Override
    public String render(Object o) throws Exception {
        return gson.toJson(o);
    }

    public static JsonTransformer get() {
        if (INSTANCE == null) {
            synchronized (JsonTransformer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new JsonTransformer();
                }
            }
        }
        return INSTANCE;
    }
}
