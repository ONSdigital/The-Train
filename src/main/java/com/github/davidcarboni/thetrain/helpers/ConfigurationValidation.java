package com.github.davidcarboni.thetrain.helpers;

import com.github.davidcarboni.restolino.framework.Startup;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.davidcarboni.thetrain.helpers.Configuration.TRANSACTION_STORE;
import static com.github.davidcarboni.thetrain.helpers.Configuration.WEBSITE;
import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;
import static java.lang.String.format;

public class ConfigurationValidation implements Startup {

    static final String CONFIG_MISSING_MSG = "application configuration validation error: value expected but none " +
            "found. It is strongly recommended you investigate and fix this before continuing";

    static final String INVALID_CONFIG_MSG = "application configuration validation error: '%s' directory does not " +
            "exist. It is strongly recommended you investigate and fix this before continuing";

    static final String HEALTHY_MSG = "application configuration validation completed without error";

    static final String TRANSACTIONS_PATH = TRANSACTION_STORE + "_PATH";

    static final String WEBSITE_PATH = WEBSITE + "_PATH";

    @Override
    public void init() {
        String transactionStorePath = Configuration.transactionStore();
        String websitePath = Configuration.website();
        boolean healthy = true;

        if (StringUtils.isEmpty(transactionStorePath)) {
            logBuilder().clazz(getClass())
                    .addParameter(TRANSACTIONS_PATH, "null")
                    .warn(CONFIG_MISSING_MSG);
            healthy = false;
        } else if (!Files.isDirectory(Paths.get(transactionStorePath))) {
            logBuilder().clazz(getClass())
                    .addParameter(TRANSACTIONS_PATH, transactionStorePath)
                    .warn(format(INVALID_CONFIG_MSG, TRANSACTION_STORE));
            healthy = false;
        }

        if (StringUtils.isEmpty(websitePath)) {
            logBuilder().clazz(getClass())
                    .addParameter(WEBSITE_PATH, "null")
                    .warn(CONFIG_MISSING_MSG);
            healthy = false;
        } else if (!Files.isDirectory(Paths.get(websitePath))) {
            logBuilder().clazz(getClass())
                    .addParameter(WEBSITE_PATH, websitePath)
                    .warn(format(INVALID_CONFIG_MSG, WEBSITE));
            healthy = false;
        }

        if (healthy) {
            logBuilder().clazz(getClass())
                    .addParameter(WEBSITE_PATH, websitePath)
                    .addParameter(TRANSACTIONS_PATH, transactionStorePath)
                    .debug(HEALTHY_MSG);
        }
    }
}
