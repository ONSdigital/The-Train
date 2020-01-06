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
import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.exception.PublishException;
import com.github.onsdigital.thetrain.exception.handler.BadRequestExceptionHandler;
import com.github.onsdigital.thetrain.exception.handler.CatchAllHandler;
import com.github.onsdigital.thetrain.exception.handler.PublishExceptionHandler;
import com.github.onsdigital.thetrain.filters.AfterFilter;
import com.github.onsdigital.thetrain.filters.BeforeFilter;
import com.github.onsdigital.thetrain.storage.Publisher;
import com.github.onsdigital.thetrain.storage.Transactions;

import static com.github.onsdigital.thetrain.logging.TrainEvent.fatal;
import static com.github.onsdigital.thetrain.logging.TrainEvent.info;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.port;

public class App {

    public static void main(String[] args) {
        initLogging();

        try {
            info().log("starting the-train");
            allAboard();
        } catch (Exception e) {
            fatal(e).log("unexpected error while attempting to start the-train");
            System.exit(1);
        }
    }

    private static void allAboard() throws Exception {
        AppConfiguration config = AppConfiguration.get();

        // init services.
        Publisher.init(config.publishThreadPoolSize());
        Transactions.init(config.transactionStore());

        port(config.port());

        before("/*", new BeforeFilter());

        AfterFilter afterFilter = new AfterFilter();
        after("/*", afterFilter);

        registerExeptionHandlers();

        Beans beans = new Beans(config.websitePath());
        Routes.register(beans);

        info().data("PORT", config.port()).log("registered API routes");
    }

    private static void registerExeptionHandlers() {
        exception(BadRequestException.class, (e, req, resp) -> new BadRequestExceptionHandler().handle(e, req, resp));

        exception(PublishException.class, (e, req, resp) -> new PublishExceptionHandler().handle(e, req, resp));

        exception(Exception.class, (e, req, resp) -> new CatchAllHandler().handle(e, req, resp));
    }

    private static void initLogging() {
        LogSerialiser serialiser = new JacksonLogSerialiser();
        LogStore store = new MDCLogStore(serialiser);
        Logger logger = new LoggerImpl("the-train");

        try {
            DPLogger.init(new Builder()
                    .logger(logger)
                    .logStore(store)
                    .serialiser(serialiser)
                    .dataNamespace("train.data")
                    .create());
        } catch (LoggingException ex) {
            System.err.println(ex);
            System.exit(1);
        }
    }
}
