package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.filters.AfterFilter;
import com.github.onsdigital.thetrain.filters.BeforeFilter;
import com.github.onsdigital.thetrain.handlers.AddFileToTransaction;
import com.github.onsdigital.thetrain.handlers.CommitTransaction;
import com.github.onsdigital.thetrain.handlers.GetTransaction;
import com.github.onsdigital.thetrain.handlers.OpenTransaction;
import com.github.onsdigital.thetrain.handlers.RollbackTransaction;
import com.github.onsdigital.thetrain.handlers.SendManifest;
import com.github.onsdigital.thetrain.handlers.VerifyTransaction;
import com.github.onsdigital.thetrain.response.JsonTransformer;
import com.github.onsdigital.thetrain.storage.Publisher;
import spark.ResponseTransformer;
import spark.Route;

import java.util.HashMap;
import java.util.Map;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

public class App {


    static Map<String, String> ROUTES;

    public static void main(String[] args) {
        start();
    }

    private static void start() {
        logBuilder().info("starting the-train");
        port(8084);

        Publisher.init(100);
        ResponseTransformer transformer = JsonTransformer.get();

        ROUTES = new HashMap<>();

        before("/*", new BeforeFilter());

        after("/*", new AfterFilter());

        registerPostHandler("/begin", new OpenTransaction(), transformer);
        registerPostHandler("/commit", new CommitTransaction(), transformer);
        registerPostHandler("/CommitManifest", new SendManifest(), transformer);
        registerPostHandler("/publish", new AddFileToTransaction(), transformer);
        registerPostHandler("/rollback", new RollbackTransaction(), transformer);
        registerGetHandler("/transaction", new GetTransaction(), transformer);
        registerGetHandler("/veify", new VerifyTransaction(), transformer);

        logBuilder().addParameter("routes", ROUTES)
                .addParameter("PORT", 8084)
                .info("registered routes");
    }

    private static void registerPostHandler(String uri, Route route, ResponseTransformer transformer) {
        ROUTES.put(uri, "POST");
        post(uri, route, transformer);
    }

    private static void registerGetHandler(String uri, Route route, ResponseTransformer transformer) {
        ROUTES.put(uri, "GET");
        get(uri, route, transformer);
    }
}
