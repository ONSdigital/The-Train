package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.filters.AfterFilter;
import com.github.onsdigital.thetrain.filters.BeforeFilter;
import com.github.onsdigital.thetrain.handlers.CommitTransactionHandler;
import com.github.onsdigital.thetrain.handlers.OpenTransactionHandler;
import com.github.onsdigital.thetrain.handlers.SendManifestHandler;
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

        Route openTransaction = new OpenTransactionHandler();
        post("/Begin", openTransaction, transformer);

        Route commitTransaction = new CommitTransactionHandler();
        post("/Commit", commitTransaction, transformer);

        Route sendManifest = new SendManifestHandler();
        post("/CommitManifest", sendManifest, transformer);
    }
}
