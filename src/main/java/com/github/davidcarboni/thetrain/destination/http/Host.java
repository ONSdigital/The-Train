package com.github.davidcarboni.thetrain.destination.http;

import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by david on 25/03/2015.
 */
public class Host {

    URI url;

    public Host(String baseUrl)  {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            url = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Er, reealy? baseUrl: "+baseUrl);
        }
    }
}
