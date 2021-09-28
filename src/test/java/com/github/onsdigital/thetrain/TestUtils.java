package com.github.onsdigital.thetrain;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

public class TestUtils {
  /*
  * Set a {@link String} environment variable value in the JVM for testing purposes
  * Taken from https://stackoverflow.com/a/7201825 with license https://creativecommons.org/licenses/by-sa/4.0/
  * @param newenv the map of Environment Variable and its associated value.
  * @throws ClassNotFoundException if class java.lang.ProcessEnvironment isn't dynamically created successfully.
  * @throws NoSuchFieldException if the declared field 'm' doesn't exist
  * @throws IllegalAccessException if the JVM cannot locate the class field name.
  */
    public static void setEnvironmentalVariables(Map<String, String> newenv) throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }
}
