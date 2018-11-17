package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.filters.AfterFilter;
import com.github.onsdigital.thetrain.filters.BeforeFilter;
import com.github.onsdigital.thetrain.handlers.CommitTransaction;
import com.github.onsdigital.thetrain.handlers.OpenTransaction;
import com.github.onsdigital.thetrain.handlers.SendManifest;
import com.github.onsdigital.thetrain.response.JsonTransformer;
import spark.ResponseTransformer;
import spark.Route;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.port;
import static spark.Spark.post;

public class App {

    public static void main(String[] args) {
        start();
    }

    private static void start() {
        logBuilder().info("starting the-train");
        port(8084);

        ResponseTransformer transformer = JsonTransformer.get();

        before("/*", new BeforeFilter());

        after("/*", new AfterFilter());

        registerHandler("/Begin", new OpenTransaction(), transformer);
        registerHandler("/Commit", new CommitTransaction(), transformer);
        registerHandler("/CommitManifest", new SendManifest(), transformer);
    }

    private static void registerHandler(String uri, Route route, ResponseTransformer transformer) {
        logBuilder().uri(uri)
                .addParameter("method", "POST")
                .info("registered request handler");

        post(uri, route, transformer);
    }
}
