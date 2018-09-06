package com.github.davidcarboni.thetrain.helpers;

import org.apache.commons.lang3.StringUtils;

/**
 * Convenience class to get configuration values from {@link System#getProperty(String)} or gracefully fall back to {@link System#getenv()}.
 */
public class Configuration {

    static final String TRANSACTION_STORE_LEGACY = "thetrain.transactions";
    static final String TRANSACTION_STORE = "TRANSACTION_STORE";
    static final String WEBSITE_LEGACY = "thetrain.website";
    static final String WEBSITE = "WEBSITE";
    static final String THREAD_POOL_SIZE = "POOL_SIZE";
    static final int DEFAULT_THREAD_POOL_SIZE = 20;

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
            return DEFAULT_THREAD_POOL_SIZE;
        }
    }
}
