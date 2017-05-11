package com.github.davidcarboni.thetrain.helpers;

import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.thetrain.logging.Log;
import org.apache.commons.lang3.StringUtils;

/**
 * Convenience class to get configuration values from {@link System#getProperty(String)} or gracefully fall back to {@link System#getenv()}.
 */
public class Configuration {

    static final String TRANSACTION_STORE_LEGACY = "thetrain.transactions";
    static final String TRANSACTION_STORE = "TRANSACTION_STORE";
    static final String WEBSITE_LEGACY = "thetrain.website";
    static final String WEBSITE = "WEBSITE";

    // Commented out as part of temporary fix
    static {
        if (Keys.canUseStrongKeys()) {
            Log.info("This system is able to use strong AES encryption. " + Keys.SYMMETRIC_KEY_SIZE_UNLIMITED + "-bit keys will be used.");
            // Keys.setSymmetricKeySize(Keys.SYMMETRIC_KEY_SIZE_UNLIMITED);
        } else {
            Log.info("This system is restricted to standard AES encryption. " + Keys.SYMMETRIC_KEY_SIZE_STANDARD + "-bit keys will be used.");
        }
        Log.info("Symmetric key size has been set to: " + Keys.getSymmetricKeySize());
    }

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

    public static String Website() {
        return StringUtils.defaultIfBlank(System.getenv(WEBSITE), get(WEBSITE_LEGACY));
    }

    public static String TransactionStore() {
        return StringUtils.defaultIfBlank(System.getenv(TRANSACTION_STORE), get(TRANSACTION_STORE_LEGACY));
    }

}
