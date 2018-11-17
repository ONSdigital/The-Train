package com.github.onsdigital.thetrain.handlers;

import com.github.onsdigital.thetrain.service.PublisherService;
import com.github.onsdigital.thetrain.service.PublisherServiceImpl;
import com.github.onsdigital.thetrain.service.TransactionsService;
import com.github.onsdigital.thetrain.service.TransactionsServiceImpl;
import com.google.gson.Gson;
import spark.Route;

public abstract class BaseHandler implements Route {

    public static final String TRANSACTION_ID_KEY = "transactionId";

    public static final String ZIP_KEY = "zip";

    public static final String URI_KEY = "uri";

    public static final String SHA1_KEY = "sha1";

    protected Gson gson;

    protected TransactionsService transactionsService;

    protected PublisherService publisherService;

    protected TransactionsService getTransactionsService() {
        if (transactionsService == null) {
            transactionsService = new TransactionsServiceImpl();
        }
        return transactionsService;
    }

    protected PublisherService getPublisherService() {
        if (publisherService == null) {
            publisherService = new PublisherServiceImpl();
        }
        return publisherService;
    }

    protected BaseHandler() {
        this.gson = new Gson();
    }
}
