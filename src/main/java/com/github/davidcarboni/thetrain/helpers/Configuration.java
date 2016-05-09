package com.github.davidcarboni.thetrain.helpers;

import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.thetrain.logging.Log;
import org.apache.commons.lang3.StringUtils;

/**
 * Convenience class to get configuration values from {@link System#getProperty(String)} or gracefully fall back to {@link System#getenv()}.
 */
public class Configuration {

    public static final String TRANSACTION_STORE = "thetrain.transactions";
    public static final String WEBSITE = "thetrain.website";

    // Commented out as part of temporary fix
    static {
        if (Keys.canUseStrongKeys()) {
            Log.debug("This system is able to use strong AES encryption. " + Keys.SYMMETRIC_KEY_SIZE_UNLIMITED + "-bit keys will be used.");
            // Keys.setSymmetricKeySize(Keys.SYMMETRIC_KEY_SIZE_UNLIMITED);
        } else {
            Log.debug("This system is restricted to standard AES encryption. " + Keys.SYMMETRIC_KEY_SIZE_STANDARD + "-bit keys will be used.");
        }
        Log.debug("Symmetric key size has been set to: " + Keys.getSymmetricKeySize());
    }

    /**
     * Gets a configuration value from {@link System#getProperty(String)}, falling back to {@link System#getenv()}
     * if the property comes back blank.
     *
     * @param key The configuration value key.
     * @return A system property or, if that comes back blank, an environment value.
     */
    public static String get(String key) {
        return StringUtils.defaultIfBlank(System.getProperty(key), System.getenv(key));
    }

    /**
     * Gets a configuration value from {@link System#getProperty(String)}, falling back to {@link System#getenv()}
     * if the property comes back blank, then falling back to the default value.
     *
     * @param key          The configuration value key.
     * @param defaultValue The default to use if neither a property nor an environment value are present.
     * @return The result of {@link #get(String)}, or <code>defaultValue</code> if that result is blank.
     */
    public static String get(String key, String defaultValue) {
        return get(StringUtils.defaultIfBlank(get(key), defaultValue));
    }

}
