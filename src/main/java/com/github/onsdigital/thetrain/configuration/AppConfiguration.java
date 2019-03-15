package com.github.onsdigital.thetrain.configuration;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.thetrain.logging.TrainEvent.info;
import static java.lang.String.format;

/**
 * Object providing access to the application configuration values. AppConfiguration is a lazy loaded singleton - use
 * {{@link #get()}} to load (if not already loaded) and get the config.
 */
public class AppConfiguration {

    private static AppConfiguration INSTANCE = null;

    private static final String TRANSACTION_STORE_ENV_KEY = "TRANSACTION_STORE";
    private static final String WEBSITE_ENV_KEY = "WEBSITE";
    private static final String THREAD_POOL_SIZE_ENV_KEY = "PUBLISHING_THREAD_POOL_SIZE";
    private static final String PORT_ENV_KEY = "PORT";

    private static final String THREAD_POOL_SIZE_PARSE_ERR = format("configuration value %s could not be parsed to " +
            "Integer", THREAD_POOL_SIZE_ENV_KEY);

    private static final String PORT_PARSE_ERR = format("configuration value %s could not be parsed to Integer",
            PORT_ENV_KEY);

    private Path transactionStore;
    private Path websitePath;
    private int publishThreadPoolSize;
    private int port;

    /**
     * @throws ConfigurationException
     */
    private AppConfiguration() throws ConfigurationException {
        this.transactionStore = loadTransactionStoreConfig();
        this.websitePath = loadWebsitePathConfig();
        this.publishThreadPoolSize = loadPublishPoolSizeConfig();
        this.port = loadPortConfig();

        info().data(TRANSACTION_STORE_ENV_KEY, transactionStore)
                .data(WEBSITE_ENV_KEY, websitePath)
                .data(THREAD_POOL_SIZE_ENV_KEY, publishThreadPoolSize)
                .data(PORT_ENV_KEY, port)
                .log("successfully load application configuration");
    }

    /**
     * @return the transaction store directory.
     */
    public Path transactionStore() {
        return transactionStore;
    }

    /**
     * @return the size of the publisher thread pool
     */
    public int publishThreadPoolSize() {
        return publishThreadPoolSize;
    }

    /**
     * @return the content dir path of the the website.
     */
    public Path websitePath() {
        return this.websitePath;
    }

    /**
     * @return the port to run the application on.
     */
    public int port() {
        return port;
    }

    /**
     * Return a singleton instance of the ApplicationConfiguration. Will load the ApplictionConfiguration if it has
     * not already been loaded.
     *
     * @return the application configuration.
     * @throws ConfigurationException any errors while attempting to load the configuration.
     */
    public static AppConfiguration get() throws ConfigurationException {
        if (INSTANCE == null) {
            synchronized (AppConfiguration.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppConfiguration();
                }
            }
        }
        return INSTANCE;
    }

    private static Path loadTransactionStoreConfig() throws ConfigurationException {
        String value = System.getenv(TRANSACTION_STORE_ENV_KEY);

        if (StringUtils.isEmpty(value)) {
            throw new ConfigurationException("transaction store path config is null/empty");
        }

        Path transactionStorePath = Paths.get(value);

        if (Files.notExists(transactionStorePath)) {
            throw new ConfigurationException("configured transaction store path does not exist");
        }

        if (!Files.isDirectory(transactionStorePath)) {
            throw new ConfigurationException("configured transaction store path is not a directory");
        }
        return transactionStorePath;
    }

    private static Path loadWebsitePathConfig() throws ConfigurationException {
        String value = System.getenv(WEBSITE_ENV_KEY);

        if (StringUtils.isEmpty(value)) {
            throw new ConfigurationException("website path config is null/empty");
        }

        Path websitePath = Paths.get(value);

        if (Files.notExists(websitePath)) {
            throw new ConfigurationException("configured website path does not exist");
        }

        if (!Files.isDirectory(websitePath)) {
            throw new ConfigurationException("configured website path is not a directory");
        }
        return websitePath;
    }

    private static int loadPublishPoolSizeConfig() throws ConfigurationException {
        try {
            return Integer.parseInt(System.getenv(THREAD_POOL_SIZE_ENV_KEY));
        } catch (NumberFormatException e) {
            throw new ConfigurationException(THREAD_POOL_SIZE_PARSE_ERR, e);
        }
    }

    private static int loadPortConfig() throws ConfigurationException {
        try {
            return Integer.parseInt(System.getenv(PORT_ENV_KEY));
        } catch (NumberFormatException e) {
            throw new ConfigurationException(PORT_PARSE_ERR, e);
        }
    }
}
