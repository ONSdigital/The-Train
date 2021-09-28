package com.github.onsdigital.thetrain.configuration;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.github.onsdigital.thetrain.configuration.ConfigurationUtils.getValue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigurationUtilsTest {

    static final String KEY = "FOO";
    static final String VALUE = "BAR";

    @Test
    public void getValue_shouldReturnSystemValue_ifValueNotEmpty() throws Exception {
        Map<String, String> systemEnvVars = new HashMap() {{
            put(KEY, VALUE);
        }};

        assertThat(getValue(systemEnvVars, new Properties(), KEY), equalTo(VALUE));
    }

    @Test
    public void getValue_shouldPrioritiseSystemValueOverProperties_iffBothExist() throws Exception {
        Map<String, String> systemEnvVars = new HashMap() {{
            put(KEY, VALUE);
        }};

        Properties sysProps = new Properties();
        sysProps.setProperty(KEY, "BOB");

        assertThat(getValue(systemEnvVars, sysProps, KEY), equalTo(VALUE));
    }

    @Test
    public void getValue_shouldReturnPropertiesValue_ifSystemValueEmpty() throws Exception {
        Map<String, String> systemEnvVars = new HashMap();

        Properties sysProps = new Properties();
        sysProps.setProperty(KEY, VALUE);

        assertThat(getValue(systemEnvVars, sysProps, KEY), equalTo(VALUE));
    }

    @Test
    public void getValue_shouldReturnNull_ifValueNotFoundInSysEnvOrSysProps() throws Exception {
        assertThat(getValue(new HashMap<>(), new Properties(), KEY), is(nullValue()));
    }

    @Test(expected = ConfigurationException.class)
    public void getValue_shouldThrowConfigurationEx_ifSysEnvIsNull() throws Exception {
        try {
            assertThat(getValue(null, new Properties(), KEY), is(nullValue()));
        } catch (ConfigurationException ex) {
            assertThat(ex.getMessage(), equalTo("system environment expected but was null"));
            throw ex;
        }
    }

    @Test(expected = ConfigurationException.class)
    public void getValue_shouldThrowConfigurationEx_ifSysProsIsNull() throws Exception {
        try {
            assertThat(getValue(new HashMap<>(), null, KEY), is(nullValue()));
        } catch (ConfigurationException ex) {
            assertThat(ex.getMessage(), equalTo("system properties expected but was null"));
            throw ex;
        }
    }

    @Test
    public void testSetEnvironmentalVariables() {
        // Given
        // Setting
        Map<String,String> thresholdEnvVariable = new HashMap<>();
        thresholdEnvVariable.put("ARCHIVE_TRANSACTION_THRESHOLD", "1d");
    }
}
