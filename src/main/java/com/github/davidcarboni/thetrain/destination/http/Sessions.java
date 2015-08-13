package com.github.davidcarboni.thetrain.destination.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides a way to access multiple {@link Http} instances.
 * This is useful for using the same {@link Http} in multiple tests,
 * or for having separate instances for users that have different privilege levels.
 * Created by david on 08/04/2015.
 */
public class Sessions {

    /**
     * @see <a href="https://ria101.wordpress.com/2011/12/12/concurrenthashmap-avoid-a-common-misuse/"
     * >https://ria101.wordpress.com/2011/12/12/concurrenthashmap-avoid-a-common-misuse/</a>
     */
    static Map<String, Http> https = java.util.Collections.synchronizedMap(new ConcurrentHashMap<String, Http>(8, 0.9f, 1));


    /**
     * The default {@link HttpFactory} generates {@link Http} instances with Javascript enabled.
     */
    public static HttpFactory httpFactory = new HttpFactory() {
        @Override
        public Http newHttp() {
            return new Http();
        }
    };

    /**
     * If you want to use a different {@link Http} setup, implement this interface and assign it to the {@link #httpFactory} field.
     */
    public interface HttpFactory {
        Http newHttp();
    }

    /**
     * @param name A string to identify a particular http.
     * @return An {@link Http} for the given name, creating it if necessary.
     */
    public static Http get(String name) {
        Http http = https.get(name);
        if (http == null) {
            https.put(name, http = new Http());
        }
        return http;
    }

    /**
     * Convenience method to obtain a default {@link Http}.
     *
     * @return A {@link Http}. The same http will be returned each time.
     */
    public static Http get() {
        return get("DEFAULT");
    }

    public static void quit() {
        for (Http http : https.values()) {
            http.close();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        // Last-chance attempt to ensure everything is cleaned up:
        quit();
    }

//    makeAutoCloseable(WebHttp http) {
//        InvocationHandler handler = new InvocationHandler(){};
//        MyInterface proxy = (MyInterface) Proxy.newProxyInstance(
//                MyInterface.class.getClassLoader(),
//                new Class[] { MyInterface.class },
//                handler);
//    }

//    interface CloseableHttp extends WebHttp, AutoCloseable {
//
//    }

}
