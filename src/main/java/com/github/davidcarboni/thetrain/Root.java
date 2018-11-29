package com.github.davidcarboni.thetrain;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.davidcarboni.thetrain.helpers.Configuration;
import com.github.davidcarboni.thetrain.storage.Publisher;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.davidcarboni.thetrain.helpers.Configuration.TRANSACTION_STORE;
import static com.github.davidcarboni.thetrain.helpers.Configuration.WEBSITE;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static java.lang.String.format;

public class Root implements Startup {

    static final String CONFIG_MISSING_MSG = "application configuration validation error: value expected but none " +
            "found. It is strongly recommended you investigate and fix this before continuing";

    static final String INVALID_CONFIG_MSG = "application configuration validation error: '%s' directory does not " +
            "exist. It is strongly recommended you investigate and fix this before continuing";

    static final String HEALTHY_MSG = "application configuration validation completed without error";

    static final String TRANSACTIONS_PATH = TRANSACTION_STORE + "_PATH";

    static final String WEBSITE_PATH = WEBSITE + "_PATH";

    static final String PUBLISHING_THREAD_POOL_SIZE = "PUBLISHING_THREAD_POOL_SIZE";

    static final String TRANS_STORE_PATH_INVALID = "transaction store path config invalid";

    static final String WEBSITE_PATH_INVALID = "website store path config invalid";

    @Override
    public void init() {
        logBuilder().info("initialising The-Train");

        try {
            String transactionPath = validateTransactionStorePath();
            String websitePath = validateWebSitePath();
            int threadPoolSize = validateThreadPoolSize();

            logBuilder().clazz(getClass())
                    .addParameter(WEBSITE_PATH, websitePath)
                    .addParameter(TRANSACTIONS_PATH, transactionPath)
                    .addParameter(PUBLISHING_THREAD_POOL_SIZE, threadPoolSize)
                    .debug(HEALTHY_MSG);

            Publisher.init(threadPoolSize);

        } catch (IllegalArgumentException e) {
            logBuilder().warn("application configuration invalid exiting application");
            System.exit(1);
        }
    }

    private String validateTransactionStorePath() {
        String transactionStorePath = Configuration.transactionStore();

        if (StringUtils.isEmpty(transactionStorePath)) {
            logBuilder().clazz(getClass())
                    .addParameter(TRANSACTIONS_PATH, "null")
                    .warn(CONFIG_MISSING_MSG);
            throw new IllegalArgumentException(TRANS_STORE_PATH_INVALID);
        } else if (!Files.isDirectory(Paths.get(transactionStorePath))) {
            logBuilder().clazz(getClass())
                    .addParameter(TRANSACTIONS_PATH, transactionStorePath)
                    .warn(format(INVALID_CONFIG_MSG, TRANSACTION_STORE));
            throw new IllegalArgumentException(TRANS_STORE_PATH_INVALID);
        }

        return transactionStorePath;
    }

    private String validateWebSitePath() {
        String websitePath = Configuration.website();

        if (StringUtils.isEmpty(websitePath)) {
            logBuilder().clazz(getClass())
                    .addParameter(WEBSITE_PATH, "null")
                    .warn(CONFIG_MISSING_MSG);
            throw new IllegalArgumentException(WEBSITE_PATH_INVALID);
        } else if (!Files.isDirectory(Paths.get(websitePath))) {
            logBuilder().clazz(getClass())
                    .addParameter(WEBSITE_PATH, websitePath)
                    .warn(format(INVALID_CONFIG_MSG, WEBSITE));
            throw new IllegalArgumentException(WEBSITE_PATH_INVALID);
        }
        return websitePath;
    }

    private int validateThreadPoolSize() {
        try {
            return Configuration.threadPoolSize();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}
