package com.github.davidcarboni.thetrain.helpers;

import org.apache.commons.lang3.StringUtils;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;

/**
 * Convenience class to get configuration values from {@link System#getProperty(String)} or gracefully fall back to {@link System#getenv()}.
 */
public class Configuration {

    public static final String TRANSACTION_STORE_LEGACY = "thetrain.transactions";
    public static final String TRANSACTION_STORE = "TRANSACTION_STORE";
    public static final String WEBSITE_LEGACY = "thetrain.website";
    public static final String WEBSITE = "WEBSITE";
    public static final String THREAD_POOL_SIZE = "POOL_SIZE";

    /**
     * Gets a configuration value from {@link System#getProperty(String)}, falling back to {@link System#getenv()}
     * if the property comes back blank.
     *
     * @param key The configuration value key.
     * @return A system property or, if that comes back blank, an environment value.
     */
    static String get(String key) {
        return StringUtils.defaultIfBlank(System.getProperty(key), System.getenv(key));
    }

    public static String website() {
        return StringUtils.defaultIfBlank(System.getenv(WEBSITE), get(WEBSITE_LEGACY));
    }

    public static String transactionStore() {
        return StringUtils.defaultIfBlank(System.getenv(TRANSACTION_STORE), get(TRANSACTION_STORE_LEGACY));
    }

    public static int threadPoolSize() {
        try {
            return Integer.parseInt(System.getenv(THREAD_POOL_SIZE));
        } catch (NumberFormatException e) {
            logBuilder()
                    .addParameter("envVar", THREAD_POOL_SIZE)
                    .error(e, "fail to parse environment variable to integer");
            throw e;
        }
    }
}
