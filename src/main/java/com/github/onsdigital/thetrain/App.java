package com.github.onsdigital.thetrain;

import com.github.onsdigital.thetrain.configuration.AppConfiguration;
import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.exception.handler.BadRequestExceptionHandler;
import com.github.onsdigital.thetrain.exception.handler.CatchAllHandler;
import com.github.onsdigital.thetrain.exception.handler.PublishExceptionHandler;
import com.github.onsdigital.thetrain.filters.AfterFilter;
import com.github.onsdigital.thetrain.filters.BeforeFilter;
import com.github.onsdigital.thetrain.filters.QuietFilter;
import com.github.onsdigital.thetrain.helpers.FileUploadHelper;
import com.github.onsdigital.thetrain.response.JsonTransformer;
import com.github.onsdigital.thetrain.routes.AddFileToTransaction;
import com.github.onsdigital.thetrain.routes.CommitTransaction;
import com.github.onsdigital.thetrain.routes.GetTransaction;
import com.github.onsdigital.thetrain.routes.OpenTransaction;
import com.github.onsdigital.thetrain.routes.RollbackTransaction;
import com.github.onsdigital.thetrain.routes.SendManifest;
import com.github.onsdigital.thetrain.routes.VerifyTransaction;
import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.PublisherServiceImpl;
import com.github.onsdigital.thetrain.service.TransactionsService;
import com.github.onsdigital.thetrain.service.TransactionsServiceImpl;
import com.github.onsdigital.thetrain.storage.Publisher;
import com.github.onsdigital.thetrain.storage.Transactions;
import spark.ResponseTransformer;
import spark.Route;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.github.onsdigital.thetrain.logging.LogBuilder.logBuilder;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

public class App {


    static Map<String, String> ROUTES;

    public static void main(String[] args) {
        try {
            start();
        } catch (Exception e) {
            logBuilder().error(e, "start up failed");
            System.exit(100);
        }
    }

    private static void start() throws Exception {
        logBuilder().info("starting the-train");

        AppConfiguration config = AppConfiguration.get();

        // init services.
        Publisher.init(config.publishThreadPoolSize());
        Transactions.init(config.transactionStore());

        ROUTES = new HashMap<>();

        port(config.port());

        before("/*", new BeforeFilter());

        AfterFilter afterFilter = new AfterFilter();
        after("/*", afterFilter);

        registerExeptionHandlers(afterFilter);

        // objects needed by routes
        ResponseTransformer transformer = JsonTransformer.get();
        TransactionsService transactionsService = new TransactionsServiceImpl();
        FileUploadHelper fileUploadHelper = new FileUploadHelper();
        PublisherService publisherService = new PublisherServiceImpl(Publisher.getInstance(), config.websitePath());

        registerRoutes(transformer, fileUploadHelper, transactionsService, publisherService, config.websitePath());

        logBuilder().addParameter("routes", ROUTES)
                .addParameter("PORT", config.port())
                .info("registered routes");
    }

    private static void registerExeptionHandlers(QuietFilter afterFilter) {
        exception(BadRequestException.class, (e, req, resp) ->
                new BadRequestExceptionHandler(afterFilter).handle(e, req, resp));

        exception(PublishException.class,
                (e, req, resp) -> new PublishExceptionHandler(afterFilter).handle(e, req, resp));

        exception(Exception.class, (e, req, resp) -> new CatchAllHandler(afterFilter).handle(e, req, resp));
    }

    private static void registerRoutes(ResponseTransformer transformer, FileUploadHelper fileUploadHelper,
                                       TransactionsService transactionsService, PublisherService publisherService,
                                       Path websitePath) {
        Route openTransaction = new OpenTransaction(transactionsService);
        registerPostHandler("/begin", openTransaction, transformer);

        Route addFile = new AddFileToTransaction(transactionsService, publisherService, fileUploadHelper);
        registerPostHandler("/publish", addFile, transformer);

        Route commit = new CommitTransaction(transactionsService, publisherService);
        registerPostHandler("/commit", commit, transformer);

        Route sendManifest = new SendManifest(transactionsService, publisherService, websitePath);
        registerPostHandler("/CommitManifest", sendManifest, transformer);

        Route rollback = new RollbackTransaction(transactionsService, publisherService);
        registerPostHandler("/rollback", rollback, transformer);

        Route getTransaction = new GetTransaction(transactionsService);
        registerGetHandler("/transaction", getTransaction, transformer);

        Route verify = new VerifyTransaction(websitePath);
        registerGetHandler("/veify", verify, transformer);

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
