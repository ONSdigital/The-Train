package com.github.onsdigital.thetrain.routes;

import com.github.onsdigital.thetrain.exception.BadRequestException;
import com.github.onsdigital.thetrain.json.request.Manifest;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import spark.Request;
import spark.Route;

public abstract class BaseHandler implements Route {

    static final String TRANSACTON_ID_MISSING_ERR = "transactionID required but none provided";
    static final String URI_MISSING_ERR = "uri required but none provided";
    static final String MANIFEST_ERR = "error getting manifest from request body";
    static final String MANIFEST_MISSING_ERR = "manifest required but was null";

    public static final String TRANSACTION_ID_KEY = "transactionId";

    public static final String ZIP_KEY = "zip";

    public static final String URI_KEY = "uri";

    public static final String SHA1_KEY = "sha1";

    protected Gson gson = new Gson();

    protected String getURI(Request request) throws BadRequestException {
        String uri = request.raw().getParameter(URI_KEY);
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException(URI_MISSING_ERR);
        }
        return uri;
    }

    protected boolean isZipped(Request request) {
        return Boolean.valueOf(request.raw().getParameter(ZIP_KEY));
    }

    protected Manifest getManifest(Request request) throws BadRequestException {
        Manifest manifest = null;
        try {
            manifest = gson.fromJson(request.body(), Manifest.class);
        } catch (Exception e) {
            throw new BadRequestException(MANIFEST_ERR);
        }
        if (manifest == null) {
            throw new BadRequestException(MANIFEST_MISSING_ERR);
        }
        return manifest;
    }

}
