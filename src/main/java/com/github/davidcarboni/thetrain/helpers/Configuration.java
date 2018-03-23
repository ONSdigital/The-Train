package com.github.davidcarboni.thetrain.helpers;

import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.thetrain.logging.LogBuilder;
import org.apache.commons.lang3.StringUtils;

import static com.github.davidcarboni.thetrain.logging.LogBuilder.logBuilder;

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
        LogBuilder log = logBuilder();
        if (Keys.canUseStrongKeys()) {
            log.addParameter("keyBitSize", Keys.SYMMETRIC_KEY_SIZE_UNLIMITED)
                    .info("this system is able to use strong AES encryption");

        } else {
            log.addParameter("keyBitSize", Keys.SYMMETRIC_KEY_SIZE_STANDARD)
                    .info("this system is restricted to standard AES encryption");
        }
        log.addParameter("keyBitSize", Keys.getSymmetricKeySize())
                //.addParameter("random", new java.util.Random().nextInt())
                .info("symmetric key size set");
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

    public static String website() {
        return StringUtils.defaultIfBlank(System.getenv(WEBSITE), get(WEBSITE_LEGACY));
    }

    public static String transactionStore() {
        return StringUtils.defaultIfBlank(System.getenv(TRANSACTION_STORE), get(TRANSACTION_STORE_LEGACY));
    }

}
