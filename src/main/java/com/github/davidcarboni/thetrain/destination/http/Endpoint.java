package com.github.davidcarboni.thetrain.destination.http;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by david on 25/03/2015.
 */
public class Endpoint {
    Host host;
    List<String> path;
    Map<String, String> parameters = new TreeMap<>();

    /**
     * Creates and endpoint for the given {@code host} and {@code path}.
     *
     * @param host The base URL, e.g. {@code http://localhost:8080/api/}.
     * @param path The relative path of the endpoint under the base URL, e.g. {@code /login}
     *             (this would result in {@code http://localhost:8080/api/login}, regardless of leading slash).
     */
    public Endpoint(Host host, String path) {
        this.host = host;
        parseUri(path);
    }

    /**
     * Creates and endpoint for the given {@code host} string and {@code path}.
     *
     * @param host The base URL as a string, e.g. {@code http://localhost:8080/api/}.
     *             This will be used to instantiate a {@link Host}.
     * @param path The relative path of the endpoint under the base URL, e.g. {@code /login}
     *             (this would result in {@code http://localhost:8080/api/login}, regardless of leading slash).
     */
    public Endpoint(String host, String path) {
        this.host = new Host(host);
        parseUri(path);
    }

    /**
     * Creates an endpoint where the {@link Host} is instantiated with the string {@code http://localhost:8080/}.
     * This is a convenience for quick local testing.
     *
     * @param path
     */
    public Endpoint(String path) {
        host = new Host("http://localhost:8080/");
        parseUri(path);
    }

    Endpoint(Endpoint source) {
        host = source.host;
        path = new ArrayList<>(source.path);
        parameters = new TreeMap<>(source.parameters);
    }

    /**
     * Creates a new {@link Endpoint} instance with an additional GET parameter.
     * <<<<<<< Updated upstream
     * <p/>
     * Typical usage is to add a request-specific parameter to an endpoint, which is why this method returns a new instance, rather than modifying the existing one.
     * This allows you add different parameters/values at different times without affecting the original instance.
     * <p/>
     * =======
     * <p/>
     * Typical usage is to add a request-specific parameter to an endpoint, which is why this method returns a new instance, rather than modifying the existing one.
     * This allows you add different parameters/values at different times without affecting the original instance.
     * >>>>>>> Stashed changes
     *
     * @param name
     * @param value
     * @return
     */
    public Endpoint setParameter(String name, Object value) {
        Endpoint configured = new Endpoint(this);
        if (StringUtils.isNotBlank(name) && value != null) {
            configured.parameters.put(name, value.toString());
        }
        return configured;
    }

    /**
     * Creates a new {@link Endpoint} instance with an additional path segment.
     * <<<<<<< Updated upstream
     * <p/>
     * Typical usage is to add an ID to an endpoint path, which is why this method returns a new instance, rather than modifying the existing one.
     * This allows you add different ID at different times without affecting the original instance.
     * <p/>
     * =======
     * <p/>
     * Typical usage is to add an ID to an endpoint path, which is why this method returns a new instance, rather than modifying the existing one.
     * This allows you add different ID at different times without affecting the original instance.
     * >>>>>>> Stashed changes
     *
     * @param segment The segment to be added.
     * @return A copy of this instance, with the additional path segment.
     */
    public Endpoint addPathSegment(String segment) {
        Endpoint configured = new Endpoint(this);
        if (StringUtils.isNotBlank(segment)) {
            configured.path.add(segment);
        }
        return configured;
    }

    private void parseUri(String uri) {
        path = new ArrayList<>();
        path.addAll(split(host.url.getPath()));
        path.addAll(split(uri));
    }

    private List<String> split(String pathSegments) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isNotBlank(pathSegments)) {
            String[] split = StringUtils.split(pathSegments, '/');
            result.addAll(Arrays.asList(split));
        }
        return result;
    }

    private URIBuilder uriBuilder() {

        // Host, etc.
        URIBuilder uriBuilder = new URIBuilder(host.url);

        // Path
        String path = '/' + StringUtils.join(this.path, '/');

        // Parameters
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            uriBuilder.setParameter(parameter.getKey(), parameter.getValue());
        }

        uriBuilder.setPath(path);

        return uriBuilder;
    }

    public URI url() {
        try {
            return uriBuilder().build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Whatev.");
        }
    }

    @Override
    public String toString() {
        return url().toString();
    }
}
