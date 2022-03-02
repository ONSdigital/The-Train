package com.github.onsdigital.thetrain;

import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.Logger;
import com.github.onsdigital.logging.v2.LoggerImpl;
import com.github.onsdigital.logging.v2.LoggingException;
import com.github.onsdigital.logging.v2.config.Builder;
import com.github.onsdigital.logging.v2.serializer.JacksonLogSerialiser;
import com.github.onsdigital.logging.v2.serializer.LogSerialiser;
import com.github.onsdigital.logging.v2.storage.LogStore;
import com.github.onsdigital.logging.v2.storage.MDCLogStore;
import com.github.onsdigital.thetrain.configuration.AppConfiguration;
import com.github.onsdigital.thetrain.configuration.ConfigurationException;
import com.github.onsdigital.thetrain.configuration.ConfigurationUtils;
import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.exception.handler.BadRequestExceptionHandler;
import com.github.onsdigital.thetrain.exception.handler.CatchAllHandler;
import com.github.onsdigital.thetrain.exception.handler.PublishExceptionHandler;
import com.github.onsdigital.thetrain.filters.AfterFilter;
import com.github.onsdigital.thetrain.filters.BeforeFilter;
import com.github.onsdigital.thetrain.response.Message;
import com.github.onsdigital.thetrain.routes.AddFileToTransaction;
import com.github.onsdigital.thetrain.routes.CommitTransaction;
import com.github.onsdigital.thetrain.routes.GetContentHash;
import com.github.onsdigital.thetrain.routes.GetTransaction;
import com.github.onsdigital.thetrain.routes.OpenTransaction;
import com.github.onsdigital.thetrain.routes.RollbackTransaction;
import com.github.onsdigital.thetrain.routes.SendManifest;
import com.github.onsdigital.thetrain.storage.Publisher;
import com.github.onsdigital.thetrain.storage.Transactions;
import spark.Filter;
import spark.ResponseTransformer;
import spark.Route;

import java.time.Duration;

import static com.github.onsdigital.thetrain.logging.TrainEvent.fatal;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

public class App {


    /**
     * Start The Train.
     * A {@link LoggingException} throw when attempting to init the application is considered fatal and will result
     * in the app exiting with a system exit code of 1 - not being able to log means we are helpless if a publish
     * were to fail.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            startApp();
        } catch (LoggingException ex) {
            System.err.println(ex);
            System.exit(1);
        } catch (Exception e) {
            fatal(e).log("unexpected error while attempting to start the-train");
            System.exit(1);
        }
    }


    private static void startApp() throws Exception {
        initLogging();

        info().log("starting the-train");

        AppConfiguration config = AppConfiguration.get();
        initServices(config);
        port(config.port());
        registerHTTPFilters();
        registerExeptionHandlers();
        registerEndpoints(config);

        info().data("PORT", config.port()).log("train start up completed successfully");
    }

    private static void initLogging() throws LoggingException {
        LogSerialiser serialiser = new JacksonLogSerialiser();
        LogStore store = new MDCLogStore(serialiser);
        Logger logger = new LoggerImpl("the-train");

        DPLogger.init(new Builder()
                .logger(logger)
                .logStore(store)
                .serialiser(serialiser)
                .dataNamespace("train.data")
                .create());
    }

    private static void initServices(AppConfiguration config) throws ConfigurationException {
        Publisher.init(config.publishThreadPoolSize());
        Transactions.init(config.transactionStore());

        final Duration duration = ConfigurationUtils.getDurationEnvVar(AppConfiguration.ARCHIVING_TRANSACTIONS_THRESHOLD_ENV_VAR);
        Long numberArchived = Transactions.archiveTransactions(config.transactionStore(), config.transactionArchivedStore(), duration);

        info().data("count", numberArchived). log("archiving transactions older than threshold");
    }

    private static void registerHTTPFilters() {
        Filter beforeFilter = new BeforeFilter();
        before("/*", beforeFilter);

        AfterFilter afterFilter = new AfterFilter();
        after("/*", afterFilter);
    }

    private static void registerExeptionHandlers() {
        exception(BadRequestException.class, (e, req, resp)
                -> new BadRequestExceptionHandler().handle(e, req, resp));

        exception(PublishException.class, (e, req, resp)
                -> new PublishExceptionHandler().handle(e, req, resp));

        exception(Exception.class, (e, req, resp)
                -> new CatchAllHandler().handle(e, req, resp));
    }

    public static void registerEndpoints(AppConfiguration cfg) {
        Beans beans = new Beans(cfg);

        ResponseTransformer transformer = beans.getResponseTransformer();

        registerPostHandler("/begin", openTransaction(beans), transformer);

        registerPostHandler("/publish", addFiles(beans), transformer);

        registerPostHandler("/commit", commitTransaction(beans), transformer);

        registerPostHandler("/CommitManifest", sendManifest(beans), transformer);

        registerPostHandler("/rollback", rollbackTransaction(beans), transformer);

        registerGetHandler("/transaction", getTransaction(beans), transformer);

        registerGetHandler("/contentHash", getContentHash(beans, cfg.isVerifyPublishEnabled()), transformer);

        // Catch-all for any request not handled by the above routes.
        registerGetHandler("*", getNotFoundHandler(), transformer);
    }

    private static Route openTransaction(Beans beans) {
        return new OpenTransaction(beans.getTransactionsService());
    }

    private static Route addFiles(Beans beans) {
        return new AddFileToTransaction(beans.getTransactionsService(), beans.getPublisherService(),
                beans.getFilePartSupplier());
    }

    private static Route commitTransaction(Beans beans) {
        return new CommitTransaction(beans.getTransactionsService(), beans.getPublisherService());
    }

    private static Route sendManifest(Beans beans) {
        return new SendManifest(beans.getTransactionsService(), beans.getPublisherService(), beans.getWebsitePath());
    }

    private static Route rollbackTransaction(Beans beans) {
        return new RollbackTransaction(beans.getTransactionsService(), beans.getPublisherService());
    }

    private static Route getTransaction(Beans beans) {
        return new GetTransaction(beans.getTransactionsService());
    }

    private static Route getContentHash(Beans beans, boolean isFeatureEnabled) {
        return new GetContentHash(beans.getTransactionsService(), beans.getContentService(), isFeatureEnabled);
    }

    private static Route getNotFoundHandler() {
        return (req, resp) -> {
            resp.status(404);
            return new Message("Not found");
        };
    }

    private static void registerPostHandler(String uri, Route route, ResponseTransformer transformer) {
        post(uri, route, transformer);
    }

    private static void registerGetHandler(String uri, Route route, ResponseTransformer transformer) {
        get(uri, route, transformer);
    }
}
