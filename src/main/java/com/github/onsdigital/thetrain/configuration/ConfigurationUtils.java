package com.github.onsdigital.thetrain.configuration;

import com.github.onsdigital.dpjavadurationparse.ParseDuration;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;

/**
 * Util class providing helper methods for getting value from the system environment.
 */
public class ConfigurationUtils {

    private ConfigurationUtils() {
        // hide constructor - static methods only.
    }

    /**
     * Get a {@link String} environment variable value.
     *
     * @param varName the name of the environment variable to retrieve.
     * @return the environment variable if it exists otherwise returns null.
     * @throws ConfigurationException problem getting the env var.
     */
    public static String getStringEnvVar(String varName) throws ConfigurationException {
        if (StringUtils.isEmpty(varName)) {
            throw new ConfigurationException("expected env var name but provided value was empty");
        }

        return getValue(System.getenv(), System.getProperties(), varName);
    }

    /**
     * Get a {@link Duration} environment variable value.
     *
     * @param varName the name of the environment variable to retrieve.
     * @return the environment variable if it exists.
     * @throws ConfigurationException problem getting the env var/invalid integer value.
     */
    public static Duration getDurationEnvVar(String varName) throws ConfigurationException {
        if (StringUtils.isEmpty(varName)) {
            throw new ConfigurationException("Expected env var name, but value was empty");
        }
        String strValue = getValue(System.getenv(), System.getProperties(), varName);
        return ParseDuration.parseDuration(strValue);
    }

    /**
     * Get a {@link Integer} environment variable value.
     *
     * @param varName the name of the environment variable to retrieve.
     * @return the environment variable if it exists.
     * @throws ConfigurationException problem getting the env var/invalid integer value.
     */
    public static int getIntegerEnvVar(String varName) throws ConfigurationException {
        if (StringUtils.isEmpty(varName)) {
            throw new ConfigurationException("expected env var name but provided value was empty");
        }

        String value = getValue(System.getenv(), System.getProperties(), varName);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(formatParsingError(varName, value, Integer.class), ex);
        }
    }

    /**
     * Get a {@link Integer} environment variable value.
     *
     * @param varName the name of the environment variable to retrieve.
     * @return the environment variable if it exists.
     * @throws ConfigurationException problem getting the env var/invalid integer value.
     */
    public static long getLongEnvVar(String varName) throws ConfigurationException {
        if (StringUtils.isEmpty(varName)) {
            throw new ConfigurationException("expected env var name but provided value was empty");
        }

        String value = getValue(System.getenv(), System.getProperties(), varName);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(formatParsingError(varName, value, Long.class), ex);
        }
    }

    /**
     * Get a environment variable from the system environment/system properties. System environment takes precedence
     * over system properties. Specifically returns the value in system environment if it is exists and not empty,
     * otherwise returns value from system properties. Returns null if value not present in either.
     *
     * @param sysEnv   the {@link System#getenv()} to use.
     * @param sysProps the {@link System} to use.
     * @param name     the name of the environement variable to find a value for.
     * @return the system environment if it is exists and is not empty, otherwise returns value from system
     * properties. Returns null if value not present in either.
     * @throws ConfigurationException thrown if either sysEnv or sysProps is null.
     */
    public static String getValue(final Map<String, String> sysEnv, final Properties sysProps, final String name)
            throws ConfigurationException {
        if (sysEnv == null) {
            throw new ConfigurationException("system environment expected but was null");
        }

        if (sysProps == null) {
            throw new ConfigurationException("system properties expected but was null");
        }

        return StringUtils.defaultIfEmpty(sysEnv.get(name), sysProps.getProperty(name));
    }


    static final String formatParsingError(String name, String value, Class type) {
        return format("environment variable %s value %s could not be parsed to %s", name, value,
                type.getTypeName());
    }
}
