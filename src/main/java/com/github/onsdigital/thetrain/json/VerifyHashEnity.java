package com.github.onsdigital.thetrain.json;

public class VerifyHashEnity {

    private String uri;
    private String hash;

    public VerifyHashEnity(String uri, String hash) {
        this.uri = uri;
        this.hash = hash;
    }

    public String getUri() {
        return uri;
    }

    public String getHash() {
        return hash;
    }
}
